package com.example.prova

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.LinearLayout
import androidx.activity.enableEdgeToEdge
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import org.json.JSONException
import org.json.JSONObject
import androidx.appcompat.app.AppCompatActivity


class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mqttClient: MqttClient
    private lateinit var googleMap: GoogleMap
    val flights = mutableListOf<String>()
    val flightsData = mutableMapOf<String, JSONObject>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        connect(this)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        // Aggiungiamo un marker finto (es: Roma)
        val exampleLocation = LatLng(41.9028, 12.4964)
        googleMap.addMarker(MarkerOptions().position(exampleLocation).title("Example Marker"))
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(exampleLocation, 10f))
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

            mqttClient.connect(options)
            Log.d("MQTT", "Connected to $brokerUrl")

            mqttClient.subscribe("flights/#") { topic, message ->
                val flightId = topic.substringAfter("flights/").lowercase()
                try {
                    val jsonStr = message.toString()
                    val flightInfo = JSONObject(jsonStr)

                    if (!flights.contains(flightId)) {
                        flights.add(flightId)
                    }
                    flightsData[flightId] = flightInfo

                    runOnUiThread {
                        val flightsContainer = findViewById<LinearLayout>(R.id.flightsContainer)
                        flightsContainer.removeAllViews()

                        for (flight in flights) {
                            val button = Button(context).apply {
                                text = flight.uppercase()
                                textSize = 32f
                                setTextColor(Color.WHITE)
                                setBackgroundColor(Color.parseColor("#6200EE"))
                                setPadding(20, 40, 20, 40)

                                setOnClickListener {
                                    val details = flightsData[flight]
                                    val intent = Intent(context, FlightDetailActivity::class.java)
                                    intent.putExtra("flight_id", flight)
                                    intent.putExtra("flight_details", details.toString())
                                    startActivity(intent)
                                }
                            }
                            flightsContainer.addView(button)
                        }
                    }

                } catch (e: JSONException) {
                    Log.e("MQTT", "Invalid JSON: ${e.message}")
                }
            }

        } catch (e: MqttException) {
            Log.e("MQTT", "Connection error: ${e.message}")
        }
    }

    fun publish(topic: String, payload: String) {
        try {
            val message = MqttMessage(payload.toByteArray())
            mqttClient.publish(topic, message)
            Log.d("MQTT", "Published on $topic: $payload")
        } catch (e: MqttException) {
            Log.e("MQTT", "Publish error: ${e.message}")
        }
    }
}
