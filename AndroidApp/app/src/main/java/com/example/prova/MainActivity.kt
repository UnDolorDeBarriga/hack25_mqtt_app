package com.example.prova

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
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
import android.view.MotionEvent
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale
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

        val clearIconRes = android.R.drawable.ic_menu_close_clear_cancel
        fun updateClearIcon() {
            val show = searchInput.text.isNotEmpty()
            searchInput.setCompoundDrawablesWithIntrinsicBounds(
                0, 0,
                if (show) clearIconRes else 0,
                0
            )
        }
        // initialize
        updateClearIcon()

        searchInput.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_UP && searchInput.text.isNotEmpty()) {
                // get bounds of the drawable
                val drawable = searchInput.compoundDrawables[2] ?: return@setOnTouchListener false
                if (event.x >= (searchInput.width - searchInput.paddingRight - drawable.bounds.width())) {
                    searchInput.text.clear()
                    renderFlightCards("")      // reset filter
                    updateClearIcon()
                    return@setOnTouchListener true
                }
            }
            false
        }

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
            override fun afterTextChanged(s: Editable?) {
                updateClearIcon()
            }
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

        // ensure bottom padding so last card isn’t hidden
        container.setPadding(
            container.paddingLeft,
            container.paddingTop,
            container.paddingRight,
            dp(80)
        )

        // Snapshot to avoid concurrent modification
        val snapshot = flights.toList()

        // Filter if needed
        val toShow = if (filter.isBlank()) {
            snapshot
        } else {
            snapshot.filter { flightId ->
                flightsData[flightId]?.let { info ->
                    val matchesTextFields =
                        info.optString("airport", "").contains(filter, true) ||
                                info.optString("destination", "").contains(filter, true) ||
                                info.optString("time", "").contains(filter, true)

                    val arr = info.optJSONArray("flight_number")
                    val matchesNumber = (0 until (arr?.length() ?: 0))
                        .any { i -> arr!!.optString(i).contains(filter, true) }

                    matchesTextFields || matchesNumber
                } ?: false
            }
        }

        val sortedFlights = toShow.sortedBy { flightId ->
            flightsData[flightId]?.optString("time")?.let { time ->
                try {
                    val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
                    formatter.isLenient = false
                    formatter.parse(time)?.time
                } catch (e: Exception) {
                    Log.e("RENDER", "Errore nel parsing del tempo: $time")
                    null
                }
            } ?: Long.MAX_VALUE
        }

        // Build cards
        sortedFlights.forEach { flightId ->
            val info = flightsData[flightId]!!

            // Extract fields
            val airport = info.optString("airport", "").uppercase()
            val dest = info.optString("destination", "")
            val status = info.optString("departure_status", "")
            val normalTime = info.optString("time", "")
            val delay = info.optString("delay", "")

            // Determine cancellation or delay
            val isCanceled = status.equals("Cancelat", true)
            val isDelayed = status.equals("Retardat", true) && delay.isNotBlank()
            val isDeparted = status.equals("Ha sortit", true)

            // choose display time
            val showTime = if (isDelayed) delay else normalTime
            val timeColor = if (isDelayed) Color.RED else if (isDeparted) Color.BLACK else Color.WHITE

            // Card root
            val card = MaterialCardView(this).apply {
                layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).also {
                    it.setMargins(dp(4), dp(8), dp(4), dp(16))
                }
                radius = dpF(12)
                // salmon/red if canceled, purple otherwise
                setCardBackgroundColor(
                    if (isCanceled) Color.parseColor("#ad2a51")
                    else if (isDeparted) Color.parseColor("#134b94")
                    else Color.parseColor("#3d3b40")
                )
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
                    if (isCanceled) {
                        paintFlags = paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                    }
                }.also(::addView)

            }.also(vertical::addView)

            // Flight numbers row
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
                            setBackgroundColor(Color.parseColor("#c3bfc9"))
                            setPadding(dp(12), dp(4), dp(12), dp(4))
                            layoutParams =
                                LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).also {
                                    it.rightMargin = dp(8)
                                }
                        }.also(::addView)
                    }
                    if (arr.length() > 3) {
                        TextView(this@MainActivity).apply {
                            text = "…"
                            setTextColor(Color.BLACK)
                            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                            setBackgroundColor(Color.parseColor("#c3bfc9"))
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
    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
    private fun dpF(value: Int): Float = value * resources.displayMetrics.density
}
