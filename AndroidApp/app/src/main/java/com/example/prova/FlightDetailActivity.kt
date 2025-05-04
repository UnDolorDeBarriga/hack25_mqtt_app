package com.example.prova

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONException
import org.json.JSONObject

class FlightDetailActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_flight_detail)

        supportActionBar?.setDisplayShowTitleEnabled(false)

        // Container for dynamic detail views
        val container = findViewById<LinearLayout>(R.id.flightDetailContainer)
        container.removeAllViews()

        // Parse incoming JSON
        val jsonStr = intent.getStringExtra("flight_details") ?: "{}"
        val flightDetails = try {
            JSONObject(jsonStr)
        } catch (e: JSONException) {
            Log.e("FlightDetailActivity", "Invalid JSON: ${e.message}")
            TextView(this).apply {
                text = "Error loading flight details."
                setTextColor(Color.RED)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
                gravity = Gravity.CENTER_HORIZONTAL
            }.also(container::addView)
            return
        }

        // Extract fields
        val destination = flightDetails.optString("destination", "Unknown")
        val airport     = flightDetails.optString("airport", "Unknown")
        val status      = flightDetails.optString("departure_status", "")
        val time        = flightDetails.optString("time", "Unknown")
        val delay       = flightDetails.optString("delay", "")
        val gate        = flightDetails.optString("gate", "Unknown")
        val numbersArr  = flightDetails.optJSONArray("flight_number")

        // 1) Header: Destination – Airport
        TextView(this).apply {
            text = "$destination – $airport"
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 24f)
            setTypeface(typeface, Typeface.BOLD)
            setPadding(0, dp(8), 0, dp(16))
            gravity = Gravity.CENTER_HORIZONTAL
        }.also(container::addView)

        // 2) If delayed, show bold red "RETARDAT"
        if (status.equals("Retardat", true) && delay.isNotBlank()) {
            TextView(this).apply {
                text = "RETARDAT"
                setTextColor(Color.RED)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
                setTypeface(typeface, Typeface.BOLD)
                setPadding(0, 0, 0, dp(16))
                gravity = Gravity.CENTER_HORIZONTAL
            }.also(container::addView)
        }

        // 3) Time (always) and optional delay in red
        LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        }.also { timeLayout ->
            // Scheduled time
            TextView(this).apply {
                text = time
                setTextColor(Color.WHITE)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            }.also(timeLayout::addView)

            // Delay if present
            if (status.equals("Retardat", true) && delay.isNotBlank()) {
                TextView(this).apply {
                    text = " $delay"
                    setTextColor(Color.RED)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
                }.also(timeLayout::addView)
            }

            container.addView(timeLayout)
        }

        // 4) Flight numbers (bold, centered)
        if (numbersArr != null) {
            for (i in 0 until numbersArr.length()) {
                val num = numbersArr.optString(i, "Unknown")
                TextView(this).apply {
                    text = num
                    setTextColor(Color.WHITE)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
                    setTypeface(typeface, Typeface.BOLD)
                    setPadding(0, 0, 0, dp(8))
                    gravity = Gravity.CENTER_HORIZONTAL
                }.also(container::addView)
            }
        }

        // 5) Gate info
        TextView(this).apply {
            text = "Gate: $gate"
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setPadding(0, dp(16), 0, 0)
            gravity = Gravity.CENTER_HORIZONTAL
        }.also(container::addView)
    }

    // dp → px helper
    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
}
