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

class MainActivity : ComponentActivity() {
    private lateinit var mqttClient: MqttClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        connect(this)
        // publish("test/topic", "Hello from Android!")
    }

    fun connect(context: android.content.Context) {
        val brokerUrl =
            "tcp://10.0.2.2:8000"  // 10.0.2.2 = localhost dell'host visto dall'emulatore
        val clientId = MqttClient.generateClientId()

        try {
            mqttClient = MqttClient(brokerUrl, clientId, MemoryPersistence())

            val options = MqttConnectOptions().apply {
                isCleanSession = true
            }

            mqttClient.connect(options)
            Log.d("MQTT", "Connesso a $brokerUrl")

            // Sottoscrizione
            mqttClient.subscribe("test/topic") { topic, message ->
                Log.d("MQTT", "Messaggio ricevuto su $topic: ${message.toString()}")
            }

        } catch (e: MqttException) {
            Log.e("MQTT", "Errore nella connessione: ${e.message}")
            e.printStackTrace()
        }
    }

    fun publish(topic: String, payload: String) {
        try {
            val message = MqttMessage(payload.toByteArray())
            mqttClient.publish(topic, message)
            Log.d("MQTT", "Messaggio pubblicato su $topic: $payload")
        } catch (e: MqttException) {
            Log.e("MQTT", "Errore nella pubblicazione: ${e.message}")
        }
    }
}


@Composable
fun ConnectionSuccess(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Connected to broker: $name SUCCESSFULLY", modifier = modifier
    )
}

@Composable
fun ConnectionFail(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Connected to broker: $name FAILED", modifier = modifier
    )
}