package com.example.prova

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
        val title       = item.optString("Title",       "No Title")
        val time        = item.optString("Time",        "")
        val description = item.optString("Description", "")

        val card = MaterialCardView(this).apply {
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).also {
                it.setMargins(dp(8), dp(8), dp(8), dp(8))
            }
            radius = dpF(8)
            setCardBackgroundColor(0xFFFFFFFF.toInt())
            setContentPadding(dp(16), dp(16), dp(16), dp(16))
        }

        // Title + Time row
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        }
        TextView(this).apply {
            text = title
            textSize = 18f
            setPadding(0, 0, dp(8), 0)
            setTextColor(0xFF000000.toInt())
            layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f)
        }.also { header.addView(it) }
        TextView(this).apply {
            text = time
            textSize = 14f
            setTextColor(0xFF555555.toInt())
            layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
        }.also { header.addView(it) }
        card.addView(header)

        // Description
        TextView(this).apply {
            text = description
            textSize = 16f
            setPadding(0, dp(8), 0, 0)
            setTextColor(0xFF333333.toInt())
        }.also { card.addView(it) }

        parent.addView(card)
    }

    // Helpers to convert dp to px
    private fun dp(dp: Int): Int =
        (dp * resources.displayMetrics.density).toInt()

    private fun dpF(dp: Int): Float =
        dp * resources.displayMetrics.density
}
