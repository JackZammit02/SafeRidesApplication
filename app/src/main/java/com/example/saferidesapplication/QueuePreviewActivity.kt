package com.example.saferidesapplication

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AppCompatActivity

class QueuePreviewActivity : ComponentActivity() {

    private lateinit var ridesText: TextView
    private lateinit var waitTimeText: TextView
    private lateinit var requestRideButton: Button
    private lateinit var backButton: Button

    private var currentRides = 5
    private var estimatedWait = 20 // in minutes

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_queue_preview)

        ridesText = findViewById(R.id.preview_rides_ahead)
        waitTimeText = findViewById(R.id.preview_estimated_time)
        requestRideButton = findViewById(R.id.requestRideButton)
        backButton = findViewById(R.id.backButton)

        ridesText.text = "$currentRides rides currently in the queue"
        waitTimeText.text = "Estimated wait time: $estimatedWait minutes"

        requestRideButton.setOnClickListener {
            val intent = Intent(this, PassengerActivity::class.java)
            startActivity(intent)
            finish()
        }

        backButton.setOnClickListener {
            finish()
        }
    }
}
