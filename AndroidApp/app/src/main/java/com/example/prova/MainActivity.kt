package com.example.prova

import android.content.BroadcastReceiver
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import com.example.prova.ui.theme.ProvaTheme
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.IMqttToken
import org.eclipse.paho.client.mqttv3.MqttCallback
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import kotlin.printStackTrace

class MainActivity : ComponentActivity() {
    private lateinit var mqttClient: MqttClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        connect(this)
        // publish("test/topic", "Hello from Android!")
    }

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
                Log.d("MQTT", "Messaggio ricevuto su $topic: ${message.toString()}")
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