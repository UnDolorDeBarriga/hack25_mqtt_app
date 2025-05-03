package com.example.prova

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.widget.TextView

class FlightDetailActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_flight_detail)

        val flightId = intent.getStringExtra("flight_id") ?: "Unknown"
        val detailTextView = findViewById<TextView>(R.id.flightDetailTextView)
        detailTextView.text = "Details for flight: $flightId"

        // TODO: You can retrieve more data from the Map or intent extras here
    }
}