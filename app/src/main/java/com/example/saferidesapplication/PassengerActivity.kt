package com.example.saferidesapplication

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class PassengerActivity : AppCompatActivity() {
    private lateinit var driverSwitchTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_passenger)

        title = "Passenger Page"

        // Initialize the TextView
        driverSwitchTextView = findViewById(R.id.driverSwitchTextView)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Example: Call this function when you detect a driver switch
        updateDriverSwitchStatus(true)  // Set to 'true' when drivers are switching
    }

    private fun updateDriverSwitchStatus(isSwitching: Boolean) {
        if (isSwitching) {
            driverSwitchTextView.visibility = View.VISIBLE  // Show message
        } else {
            driverSwitchTextView.visibility = View.GONE  // Hide message
        }
    }
}
