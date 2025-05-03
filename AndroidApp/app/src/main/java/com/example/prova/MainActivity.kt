package com.example.prova

import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import com.google.android.material.card.MaterialCardView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import org.json.JSONException
import org.json.JSONObject
import com.google.android.material.bottomappbar.BottomAppBar
import android.content.Intent
import androidx.appcompat.app.AlertDialog
import android.widget.EditText
import android.widget.Toast

class MainActivity : ComponentActivity() {
    private lateinit var mqttClient: MqttClient
    private val flights = mutableListOf<String>()
    private val flightsData = mutableMapOf<String, JSONObject>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        val bottomBar = findViewById<BottomAppBar>(R.id.bottomAppBar)
        bottomBar.setNavigationOnClickListener {
            // This is called whenever you tap the magnifier
            showSearchDialog()
        }

        bottomBar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_news -> {
                    // Fire your NewsActivity
                    startActivity(Intent(this, NewsActivity::class.java))
                    true
                }
                else -> false
            }
        }
        // Star FAB opens link
        findViewById<FloatingActionButton>(R.id.fabStar).setOnClickListener {
            val url = "http://192.168.71.147:8000/mouseketool.mp4"
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }
        connectAndSubscribe()
    }

    private fun connectAndSubscribe() {
        val brokerUrl = "tcp://192.168.71.147:18830"
        val clientId = MqttClient.generateClientId()

        try {
            mqttClient = MqttClient(brokerUrl, clientId, MemoryPersistence())
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
                    if (!flights.contains(flightId)) {
                        flights.add(flightId)
                    }
                    flightsData[flightId] = info
                    Log.d("MQTT", "Added flight $flightId: $info")

                    // Refresh UI
                    runOnUiThread { renderFlightCards() }

                } catch (e: JSONException) {
                    Log.e("MQTT", "Invalid JSON: ${e.message}")
                }
            }

        } catch (e: MqttException) {
            Log.e("MQTT", "MQTT connection error: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun renderFlightCards() {
        val container = findViewById<LinearLayout>(R.id.flightsContainer)
        container.removeAllViews()

        flights.forEach { flightId ->
            val info = flightsData[flightId] ?: return@forEach

            // --- 1) Card root ---
            val card = MaterialCardView(this).apply {
                layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).also {
                    it.setMargins(dp(4), dp(8), dp(4), dp(16))
                }
                radius = dpF(12)
                setCardBackgroundColor(Color.parseColor("#6200EE"))
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    startActivity(
                        Intent(
                            this@MainActivity,
                            FlightDetailActivity::class.java
                        ).apply {
                            putExtra("flight_id", flightId)
                            putExtra("flight_details", info.toString())
                        })
                }
            }

            // --- 2) Vertical inner layout ---
            val vertical = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(16), dp(16), dp(16), dp(16))
            }

            // --- 3) Top row: destination + time ---
            val topRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
            }
            TextView(this).apply {
                text = info.optString("destination")
                setTextColor(Color.WHITE)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 24f)
                layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f)
            }.also { topRow.addView(it) }
            TextView(this).apply {
                text = info.optString("time")
                setTextColor(Color.WHITE)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 24f)
                layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
            }.also { topRow.addView(it) }
            vertical.addView(topRow)

            // --- 4) Bottom row: flight_number pills ---
            val pillsRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).also {
                    it.topMargin = dp(8)
                }
            }
            info.optJSONArray("flight_number")?.let { arr ->
                val total = arr.length()
                val showCount = minOf(total, 3)

                // show up to 3 numbers
                for (i in 0 until showCount) {
                    val num = arr.optString(i)
                    TextView(this).apply {
                        text = num
                        setTextColor(Color.BLACK)
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                        setBackgroundColor(Color.WHITE)
                        setPadding(dp(12), dp(4), dp(12), dp(4))
                        layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).also {
                            it.rightMargin = dp(8)
                        }
                    }.also { pillsRow.addView(it) }
                }
                if (total > 3) {
                    TextView(this).apply {
                        text = "…"
                        setTextColor(Color.BLACK)
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                        setBackgroundColor(Color.WHITE)
                        setPadding(dp(12), dp(4), dp(12), dp(4))
                        layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
                    }.also { pillsRow.addView(it) }
                }
            }
            vertical.addView(pillsRow)

            // --- 5) Assemble ---
            card.addView(vertical)
            container.addView(card)
        }
    }

    private fun showSearchDialog() {
        // Create the input field
        val input = EditText(this).apply {
            hint = "Destination, time or flight#"
            setPadding(dp(16), dp(8), dp(16), dp(8))
        }

        AlertDialog.Builder(this)
            .setTitle("Search flights")
            .setView(input)
            .setPositiveButton("Search") { dialog, _ ->
                val query = input.text.toString().trim()
                if (query.isNotEmpty()) {
                    performSearch(query)
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /**
     * Filters your flightsData by the query, then re‐renders only matching cards.
     */
    private fun performSearch(query: String) {
        // Find all flightIds whose JSON matches the query
        val results = flightsData.filter { (_, json) ->
            json.optString("destination").contains(query, ignoreCase = true) ||
                    json.optString("time").contains(query, ignoreCase = true) ||
                    json.optJSONArray("flight_number")?.let { arr ->
                        (0 until arr.length()).any { i -> arr.optString(i).equals(query, true) }
                    } == true
        }.keys

        if (results.isEmpty()) {
            Toast.makeText(this, "No flights found for “$query”", Toast.LENGTH_SHORT).show()
        } else {
            // Replace your flights list and re‐draw
            flights.clear()
            flights.addAll(results)
            renderFlightCards()
        }
    }

    // Helpers to convert dp to px
    private fun dp(dp: Int): Int =
        (dp * resources.displayMetrics.density).toInt()

    private fun dpF(dp: Int): Float =
        dp * resources.displayMetrics.density
}
