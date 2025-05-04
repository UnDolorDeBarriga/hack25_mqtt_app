package com.example.prova

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONException
import org.json.JSONObject
import android.graphics.LinearGradient
import android.graphics.Shader

class FlightDetailActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_flight_detail)

        supportActionBar?.setDisplayShowTitleEnabled(false)

        // Container defined in activity_flight_detail.xml:
        val container = findViewById<LinearLayout>(R.id.flightDetailContainer)
        container.removeAllViews()

        // Pull JSON from intent
        val flightDetailsJson = intent.getStringExtra("flight_details") ?: "{}"
        val flightDetails = try {
            JSONObject(flightDetailsJson)
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

        // Header with icon
        LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, dp(16), 0, dp(16))

            ImageView(this@FlightDetailActivity).apply {
                setImageResource(android.R.drawable.ic_menu_compass)
                setColorFilter(Color.WHITE)
                setPadding(0, 0, dp(8), 0)
            }.also(this::addView)

            TextView(this@FlightDetailActivity).apply {
                text = "$destination – $airport"
                setTextColor(Color.WHITE)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 24f)
                setTypeface(typeface, Typeface.BOLD)
            }.also(this::addView)
        }.also(container::addView)

        // Separator
        View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(1)
            ).apply { setMargins(0, dp(8), 0, dp(8)) }
            setBackgroundColor(Color.GRAY)
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
                    setTextColor(Color.LTGRAY)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
                    setTypeface(typeface, Typeface.BOLD)
                    setPadding(0, 0, 0, dp(8))
                    gravity = Gravity.CENTER_HORIZONTAL
                }.also(container::addView)
            }
        }

        // 3) Gate info (normal, centered)
        TextView(this).apply {
            text = "Gate: $gate"
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setPadding(0, dp(16), 0, 0)
            gravity = Gravity.CENTER_HORIZONTAL

            // Gradient text effect
            paint.shader = LinearGradient(
                0f, 0f, 0f, textSize,
                intArrayOf(Color.CYAN, Color.BLUE),
                null,
                Shader.TileMode.CLAMP
            )
        }.also(container::addView)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}
