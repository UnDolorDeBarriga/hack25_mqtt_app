package com.example.prova

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import com.google.android.material.card.MaterialCardView
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import org.eclipse.paho.client.mqttv3.MqttException
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

class NewsActivity : ComponentActivity() {
    private lateinit var mqttClient: MqttClient

    /** Change only this if your topic is different */
    private val NEWS_TOPIC = "blog/#"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_news)
        subscribeToNews()
    }

    private fun subscribeToNews() {
        val brokerUrl = "tcp://192.168.71.147:18830"  // same broker
        val clientId  = MqttClient.generateClientId()

        try {
            mqttClient = MqttClient(brokerUrl, clientId, MemoryPersistence())
            mqttClient.connect(MqttConnectOptions().apply {
                isCleanSession = true
                userName = "user"
                password = "user".toCharArray()
            })

            mqttClient.subscribe(NEWS_TOPIC) { _, msg ->
                val payload = msg.toString()
                runOnUiThread {
                    renderNewsPosts(payload)
                }
            }

            Log.d("NewsActivity", "Subscribed to $NEWS_TOPIC")

        } catch (e: MqttException) {
            Log.e("NewsActivity", "MQTT error: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * Expects payload to be a JSON array of objects, e.g.:
     * [
     *   {"Title":"...","Time":"...","Description":"..."},
     *   {"Title":"...","Time":"...","Description":"..."}
     * ]
     * Renders them newest-first.
     */
    private fun renderNewsPosts(jsonArrayStr: String) {
        val container = findViewById<LinearLayout>(R.id.newsContainer)
        // Clear old posts if you want only latest batch:
        container.removeAllViews()

        try {
            val arr = JSONArray(jsonArrayStr)
            // Iterate backwards: last element = newest
            for (i in arr.length() - 1 downTo 0) {
                val obj = arr.getJSONObject(i)
                addNewsCard(container, obj)
            }
        } catch (e: JSONException) {
            Log.e("NewsActivity", "Invalid news JSON array: $jsonArrayStr")
        }
    }

    /** Creates and appends a single card for one news item */
    private fun addNewsCard(parent: LinearLayout, item: JSONObject) {
        val title = item.optString("Title", "No Title")
        val time = item.optString("Time", "")
        val description = item.optString("Description", "")

        val card = MaterialCardView(this).apply {
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).also {
                it.setMargins(dp(8), dp(8), dp(8), dp(8))
            }
            radius = dpF(8)
            setCardBackgroundColor(Color.parseColor("#6200EE"))
            setContentPadding(dp(16), dp(24), dp(16), dp(16))
        }

        // Container for title and description
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f)
        }

        // Title
        TextView(this).apply {
            text = title
            textSize = 20f
            setTextColor(0xFFFFFFFF.toInt())
            setPadding(0, 0, 0, dp(4))
        }.also { content.addView(it) }

        // Description
        TextView(this).apply {
            text = description
            textSize = 16f
            setTextColor(0xFFFFFFFF.toInt())
        }.also { content.addView(it) }

        // Header row: content (title + description) and time
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        }
        header.addView(content)
        TextView(this).apply {
            text = time
            textSize = 14f
            setTextColor(0xFFFFFFFF.toInt())
            layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
        }.also { header.addView(it) }

        card.addView(header)
        parent.addView(card)
    }

    // Helpers to convert dp to px
    private fun dp(dp: Int): Int =
        (dp * resources.displayMetrics.density).toInt()

    private fun dpF(dp: Int): Float =
        dp * resources.displayMetrics.density
}
