package com.example.prova

import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONException
import org.json.JSONObject

class FlightDetailActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_flight_detail)

        val flightId = intent.getStringExtra("flight_id") ?: "Unknown"

        val flightDetailsJson = intent.getStringExtra("flight_details") ?: "{}"

        val detailTextView = findViewById<TextView>(R.id.flightDetailTextView)

        try {
            val flightDetails = JSONObject(flightDetailsJson)
            val destination = flightDetails.optString("destination", "Unknown")
            val time = flightDetails.optString("time", "Unknown")
            val gate = flightDetails.optString("gate", "Unknown")

            val detailsText = """
                Flight: $flightId
                To: $destination
                Time: $time
                Gate: $gate
            """.trimIndent()

            detailTextView.text = detailsText
        } catch (e: JSONException) {
            Log.e("FlightDetailActivity", "Invalid JSON: ${e.message}")
            detailTextView.text = "Error loading flight details."
        }
    }
}