package com.example.prova

import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONException
import org.json.JSONObject

class FlightDetailActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_flight_detail)

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(false)
        }

        val container = findViewById<LinearLayout>(R.id.flightDetailContainer)
        container.removeAllViews()

        // Parse JSON payload
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
        val airport     = flightDetails.optString("airport",     "Unknown")
        val status      = flightDetails.optString("departure_status", "")
        val time        = flightDetails.optString("time",        "Unknown")
        val delay       = flightDetails.optString("delay",       "")
        val gate        = flightDetails.optString("gate",        "Unknown")
        val numbersArr  = flightDetails.optJSONArray("flight_number")

        // 1) Header: Destination – Airport with icon
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

        // 2) Separator line
        View(this).apply {
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, dp(1))
                .apply { setMargins(0, dp(8), 0, dp(8)) }
            setBackgroundColor(Color.GRAY)
        }.also(container::addView)

        // Determine state
        val isCanceled = status.equals("Cancelat", true)
        val isDelayed  = status.equals("Retardat", true) && delay.isNotBlank()
        val isDeparted = status.equals("Ha sortit", true)

        // 3) Canceled or delayed header
        if (isCanceled) {
            TextView(this).apply {
                text = "FLIGHT CANCELED"
                setTextColor(Color.RED)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 25f)
                setTypeface(typeface, Typeface.BOLD)
                gravity = Gravity.CENTER_HORIZONTAL
                setPadding(0, dp(8), 0, dp(16))
            }.also(container::addView)

        } else if (isDelayed) {
            TextView(this).apply {
                text = "FLIGHT DELAYED"
                setTextColor(Color.RED)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 25f)
                setTypeface(typeface, Typeface.BOLD)
                gravity = Gravity.CENTER_HORIZONTAL
                setPadding(0, dp(8), 0, dp(16))
            }.also(container::addView)
        } else if (isDeparted){
            TextView(this).apply {
                text = "FLIGHT DEPARTED"
                setTextColor(Color.parseColor("#134b94"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 25f)
                setTypeface(typeface, Typeface.BOLD)
                gravity = Gravity.CENTER_HORIZONTAL
                setPadding(0, dp(8), 0, dp(16))
            }.also(container::addView)
        }

        // 4) Time row (if not canceled)
        if (!isCanceled && !isDeparted) {
            LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
            }.also { timeLayout ->
                // a) Original scheduled time
                TextView(this).apply {
                    text = time
                    setTextColor(Color.WHITE)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 25f)
                    if (isDelayed) {
                        paintFlags = paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                    }
                }.also(timeLayout::addView)

                // b) Delay time in red, if applicable
                if (isDelayed) {
                    TextView(this).apply {
                        text = "  $delay"
                        setTextColor(Color.RED)
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 25f)
                    }.also(timeLayout::addView)
                }

                container.addView(timeLayout)
            }

            // 5) “Flight delayed n minutes” line
            if (isDelayed) {

                val delayMinutes = if (delay.contains(":")) {
                        val (h, m) = delay.split(":", limit = 2)
                        (h.toIntOrNull() ?: 0) * 60 + (m.toIntOrNull() ?: 0)
                } else {
                    delay.toIntOrNull() ?: 0
                }
                val flightMinutes = if (time.contains(":")) {
                    val (h, m) = time.split(":", limit = 2)
                    (h.toIntOrNull() ?: 0) * 60 + (m.toIntOrNull() ?: 0)
                } else {
                    time.toIntOrNull() ?: 0
                }
                val delayedM = delayMinutes - flightMinutes
                // robust parsing: split on “:” then fallback

                TextView(this).apply {
                    text = "Flight delayed $delayMinutes minutes"
                    setTextColor(Color.RED)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
                    gravity = Gravity.CENTER_HORIZONTAL
                    setPadding(0, dp(8), 0, 0)
                }.also(container::addView)
            }
        }

        // 6) Flight numbers
        if (numbersArr != null) {
            for (i in 0 until numbersArr.length()) {
                TextView(this).apply {
                    text = numbersArr.optString(i, "Unknown")
                    setTextColor(Color.LTGRAY)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
                    setTypeface(typeface, Typeface.BOLD)
                    setPadding(0, dp(8), 0, dp(8))
                    gravity = Gravity.CENTER_HORIZONTAL
                }.also(container::addView)
            }
        }

        // 7) Gate info at bottom
        TextView(this).apply {
            text = "Gate: $gate"
            setTextColor(Color.parseColor("#6200EE"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(0, dp(16), 0, 0)
        }.also(container::addView)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (item.itemId == android.R.id.home) {
            finish(); true
        } else super.onOptionsItemSelected(item)
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}
