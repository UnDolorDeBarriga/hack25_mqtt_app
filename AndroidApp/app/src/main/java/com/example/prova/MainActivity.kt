package com.example.prova

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import com.google.android.material.bottomappbar.BottomAppBar
import com.google.android.material.card.MaterialCardView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import org.json.JSONException
import org.json.JSONObject

class MainActivity : ComponentActivity() {
    private lateinit var mqttClient: MqttClient
    private val flights = mutableListOf<String>()
    private val flightsData = mutableMapOf<String, JSONObject>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // 1) Grab views
        val bottomBar = findViewById<BottomAppBar>(R.id.bottomAppBar)
        val searchInput = findViewById<EditText>(R.id.searchEditText)

        // 2) Show inline search bar & keyboard on magnifier tap
        bottomBar.setNavigationOnClickListener {
            searchInput.visibility = View.VISIBLE
            searchInput.requestFocus()
            (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                .showSoftInput(searchInput, InputMethodManager.SHOW_IMPLICIT)
        }

        // 3) Handle “news” menu-item
        bottomBar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_news -> {
                    startActivity(Intent(this, NewsActivity::class.java))
                    true
                }
                else -> false
            }
        }

        // 4) Real-time filtering as user types
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                renderFlightCards(filter = s?.toString().orEmpty())
            }
        })

        // 5) Star FAB opens a video link
        findViewById<FloatingActionButton>(R.id.fabStar).setOnClickListener {
            val url = "http://192.168.71.147:8000/mouseketool.mp4"
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }

        // 6) Connect to MQTT
        connectAndSubscribe()
    }

    private fun connectAndSubscribe() {
        val brokerUrl = "tcp://192.168.71.147:1883"
        mqttClient = MqttClient(brokerUrl, MqttClient.generateClientId(), MemoryPersistence())
        try {
            mqttClient.connect(MqttConnectOptions().apply {
                isCleanSession = true
                userName = "user"
                password = "user".toCharArray()
            })
            Log.d("MQTT", "Connected to $brokerUrl")

            mqttClient.subscribe("flights/#") { topic, message ->
                val flightId = topic.substringAfter("flights/").lowercase()
                try {
                    val info = JSONObject(message.toString())
                    if (!flights.contains(flightId)) flights.add(flightId)
                    flightsData[flightId] = info
                    runOnUiThread { renderFlightCards() }
                } catch (e: JSONException) {
                    Log.e("MQTT", "Invalid JSON for $flightId: ${e.message}")
                }
            }

        } catch (e: MqttException) {
            Log.e("MQTT", "Connection error: ${e.message}")
        }
    }

    private fun renderFlightCards(filter: String = "") {
        val container = findViewById<LinearLayout>(R.id.flightsContainer)
        container.removeAllViews()

        // Snapshot to avoid concurrent modification
        val snapshot = flights.toList()

        // Filter if needed
        val toShow = if (filter.isBlank()) {
            snapshot
        } else {
            snapshot.filter { id ->
                flightsData[id]?.let { info ->
                    info.optString("airport").contains(filter, true) ||
                            info.optString("destination").contains(filter, true) ||
                            info.optString("time").contains(filter, true) ||
                            (info.optJSONArray("flight_number")?.let { arr ->
                                (0 until arr.length()).any { i ->
                                    arr.optString(i).equals(filter, true)
                                }
                            } ?: false)
                } ?: false
            }
        }

        // Build cards
        toShow.forEach { flightId ->
            val info = flightsData[flightId]!!

            // Card root
            val card = MaterialCardView(this).apply {
                layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).also {
                    it.setMargins(dp(4), dp(8), dp(4), dp(16))
                }
                radius = dpF(12)
                setCardBackgroundColor(Color.parseColor("#6200EE"))
                isClickable = true
                setOnClickListener {
                    startActivity(
                        Intent(this@MainActivity, FlightDetailActivity::class.java).apply {
                            putExtra("flight_id", flightId)
                            putExtra("flight_details", info.toString())
                        }
                    )
                }
            }

            // Inner vertical stack
            val vertical = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(16), dp(16), dp(16), dp(16))
            }

            // Extract fields
            val airport     = info.optString("airport", "").uppercase()
            val dest        = info.optString("destination", "")
            val status      = info.optString("departure_status", "")
            val normalTime  = info.optString("time", "")
            val delay       = info.optString("delay", "")
            val showTime    = if (status.equals("Retardat", true) && delay.isNotBlank()) delay else normalTime
            val timeColor   = if (status.equals("Retardat", true) && delay.isNotBlank()) Color.RED else Color.WHITE

            // Top row: airport – destination, then time or delay (red if delayed)
            LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)

                TextView(this@MainActivity).apply {
                    text = "$airport – $dest"
                    setTextColor(Color.WHITE)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 24f)
                    layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f)
                }.also(::addView)

                TextView(this@MainActivity).apply {
                    text = showTime
                    setTextColor(timeColor)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 24f)
                }.also(::addView)

            }.also(vertical::addView)

            // Bottom row: flight numbers (≤3 then “…”)
            LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).also {
                    it.topMargin = dp(8)
                }

                info.optJSONArray("flight_number")?.let { arr ->
                    val count = minOf(arr.length(), 3)
                    for (i in 0 until count) {
                        TextView(this@MainActivity).apply {
                            text = arr.optString(i)
                            setTextColor(Color.BLACK)
                            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                            setBackgroundColor(Color.WHITE)
                            setPadding(dp(12), dp(4), dp(12), dp(4))
                            layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
                                .also { it.rightMargin = dp(8) }
                        }.also(::addView)
                    }
                    if (arr.length() > 3) {
                        TextView(this@MainActivity).apply {
                            text = "…"
                            setTextColor(Color.BLACK)
                            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                            setBackgroundColor(Color.WHITE)
                            setPadding(dp(12), dp(4), dp(12), dp(4))
                        }.also(::addView)
                    }
                }
            }.also(vertical::addView)

            card.addView(vertical)
            container.addView(card)
        }
    }

    // dp → px helpers
    private fun dp(value: Int): Int  = (value * resources.displayMetrics.density).toInt()
    private fun dpF(value: Int): Float = value * resources.displayMetrics.density
}
