package com.example.prova

import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONException
import org.json.JSONObject
import kotlin.text.append

class FlightDetailActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_flight_detail)

        supportActionBar?.setDisplayShowTitleEnabled(false)

        val flightId = intent.getStringExtra("flight_id") ?: "Unknown"
        val flightDetailsJson = intent.getStringExtra("flight_details") ?: "{}"
        Log.e("FlightDetailActivity", "Flight ID: $flightId")
        Log.e("FlightDetailActivity", "Flight Details JSON: $flightDetailsJson")
        val detailTextView = findViewById<TextView>(R.id.flightDetailTextView)

        try {
            val flightDetails = JSONObject(flightDetailsJson)
            val destination = flightDetails.optString("destination", "Unknown")
            val time = flightDetails.optString("time", "Unknown")
            val gate = flightDetails.optString("gate", "Unknown")
            val airport = flightDetails.optString("airport", "Unknown")

            val flightNumbersArray = flightDetails.optJSONArray("flight_number")
            val flightCount = flightNumbersArray?.length() ?: 0

            val flightNumbers = StringBuilder()
            val detailsText = StringBuilder("Flight to$destination$airport at $time:\n\n")
            for (i in 0 until flightCount) {
                val flightNumber = flightNumbersArray?.optString(i, "Unknown")
                detailsText.append(
                    """
                    $flightNumber
                    """.trimIndent() + "\n"
                )
            }
            detailsText.append(
                """
                Gate: $gate
                """.trimIndent() + "\n"
            )

            detailTextView.text = detailsText
        } catch (e: JSONException) {
            Log.e("FlightDetailActivity", "Invalid JSON: ${e.message}")
            detailTextView.text = "Error loading flight details."
        }
    }
}