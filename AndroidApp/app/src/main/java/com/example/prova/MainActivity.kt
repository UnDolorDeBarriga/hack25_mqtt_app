package com.example.prova

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import org.json.JSONObject
import org.json.JSONException
import kotlin.printStackTrace

class MainActivity : ComponentActivity() {
    private lateinit var mqttClient: MqttClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContentView(R.layout.activity_main)


        connect(this)
        // publish("test/topic", "Hello from Android!")
    }

    val flights = mutableListOf<String>()
    val flightsData = mutableMapOf<String, JSONObject>()

    fun connect(context: android.content.Context) {
        val brokerUrl = "tcp://192.168.71.147:1883"

        val clientId = MqttClient.generateClientId()

        try {
            mqttClient = MqttClient(brokerUrl, clientId, MemoryPersistence())

            val options = MqttConnectOptions().apply {
                isCleanSession = true
                userName = "user"
                password = "user".toCharArray()
            }

            try {
                mqttClient.connect(options)
                Log.d("MQTT", "Connected to $brokerUrl")
            } catch (e: MqttException) {
                Log.e("MQTT", "Connection failed: ${e.message}")
                e.printStackTrace()
            }

            // Subscription
            mqttClient.subscribe("flights/#") { topic, message ->
                val flightId = topic.substringAfter("flights/").lowercase()

                try {
                    val jsonStr = message.toString()
                    val flightInfo = JSONObject(jsonStr)

                    // Add to list if not already present
                    if (!flights.contains(flightId)) {
                        flights.add(flightId)
                    }

                    // Save/Update flight data
                    flightsData[flightId] = flightInfo

                    Log.d("MQTT", "Added flight $flightId: $flightInfo")
                    Log.d("Flights List", flights.toString())
                } catch (e: JSONException) {
                    Log.e("MQTT", "Invlaid JSON: ${e.message}")
                }

            }

        } catch (e: MqttException) {
            Log.e("MQTT", "Error in connecting: ${e.message}")
            e.printStackTrace()
        }
    }

    fun publish(topic: String, payload: String) {
        try {
            val message = MqttMessage(payload.toByteArray())
            mqttClient.publish(topic, message)
            Log.d("MQTT", "Message published on $topic: $payload")
        } catch (e: MqttException) {
            Log.e("MQTT", "Error in connecting: ${e.message}")
        }
    }
}