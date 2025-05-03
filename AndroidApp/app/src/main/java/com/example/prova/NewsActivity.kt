package com.example.prova

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import com.google.android.material.card.MaterialCardView
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

class NewsActivity : ComponentActivity() {
    private lateinit var mqttClient: MqttClient

    // Change this if your broker’s topic differs
    private val NEWS_TOPIC = "news"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_news)
        subscribeToNews()
    }

    private fun subscribeToNews() {
        val brokerUrl = "tcp://192.168.71.147:18830"
        mqttClient = MqttClient(brokerUrl, MqttClient.generateClientId(), MemoryPersistence())
        try {
            mqttClient.connect(MqttConnectOptions().apply {
                isCleanSession = true
                userName = "user"
                password = "user".toCharArray()
            })
            Log.d("NewsActivity", "Connected to $brokerUrl, subscribing to $NEWS_TOPIC")

            mqttClient.subscribe(NEWS_TOPIC) { _, msg ->
                val raw = msg.toString().trim()
                if (raw.isEmpty() || raw == "null") {
                    Log.w("NewsActivity", "Skipping null/empty payload")
                    return@subscribe
                }
                runOnUiThread { renderNewsPosts(raw) }
            }
        } catch (e: MqttException) {
            Log.e("NewsActivity", "MQTT error: ${e.message}", e)
        }
    }

    private fun renderNewsPosts(jsonArrayStr: String) {
        val emptyText = findViewById<TextView>(R.id.emptyNewsText)
        val scroll    = findViewById<ScrollView>(R.id.newsScroll)
        val container = findViewById<LinearLayout>(R.id.newsContainer)

        val arr = try {
            JSONArray(jsonArrayStr)
        } catch (e: JSONException) {
            Log.e("NewsActivity", "Invalid JSON array: $jsonArrayStr", e)
            return
        }

        if (arr.length() == 0) {
            emptyText.visibility = View.VISIBLE
            scroll.visibility    = View.GONE
            return
        }

        emptyText.visibility = View.GONE
        scroll.visibility    = View.VISIBLE
        container.removeAllViews()

        // Render newest-first
        for (i in arr.length() - 1 downTo 0) {
            val obj = try { arr.getJSONObject(i) } catch (e: JSONException) {
                Log.e("NewsActivity", "Bad JSON at index $i", e)
                continue
            }
            addNewsCard(container, obj)
        }
    }

    private fun addNewsCard(parent: LinearLayout, item: JSONObject) {
        val title       = item.optString("Title", "No Title")
        val time        = item.optString("Time", "")
        val description = item.optString("Description", "")

        // Card
        val card = MaterialCardView(this).apply {
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).also {
                it.setMargins(dp(8), dp(8), dp(8), dp(8))
            }
            radius = dpF(8)
            useCompatPadding = true
            setCardBackgroundColor(Color.parseColor("#6200EE"))
            setContentPadding(dp(24), dp(24), dp(24), dp(24))
        }

        // Title – Time (bold)
        TextView(this).apply {
            text = "$title – $time"
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
            setTypeface(typeface, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).also {
                it.bottomMargin = dp(24)   // <-- bump this up for more space
            }
        }.also(card::addView)
        // Description below with more space
        TextView(this).apply {
            text = description
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setPadding(0, dp(28), 0, 0)
        }.also(card::addView)

        parent.addView(card)
    }

    // Helpers
    private fun dp(v: Int): Int  = (v * resources.displayMetrics.density).toInt()
    private fun dpF(v: Int): Float = v * resources.displayMetrics.density
}
