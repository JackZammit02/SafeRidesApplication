package com.example.saferidesapplication

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class DriverDrivingActivity : AppCompatActivity() {

    private lateinit var dropOffText: TextView
    private lateinit var atDropOffButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_driver_driving)

        dropOffText = findViewById(R.id.dropOffLocationText)
        atDropOffButton = findViewById(R.id.atDropOffButton)

        // Optional: dynamically set location from intent
        val dropOffLocation = intent.getStringExtra("dropOff") ?: "North Hall"
        dropOffText.text = "Drop-off: $dropOffLocation"

        atDropOffButton.setOnClickListener {
            // Go back to the driver's main activity screen
            val intent = Intent(this, DriverActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}
