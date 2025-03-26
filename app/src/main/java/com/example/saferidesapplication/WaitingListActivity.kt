package com.example.saferidesapplication

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.ComponentActivity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity

class WaitingListActivity : ComponentActivity() {

    private lateinit var ridesAheadText: TextView
    private lateinit var estimatedTimeText: TextView
    private lateinit var refreshButton: Button

    // Replace with real values from backend or logic
    private var ridesAhead = 3
    private var estimatedTime = 12 // in minutes

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_waiting_list)

        val cancelButton = findViewById<Button>(R.id.cancel_button)
        cancelButton.setOnClickListener {
            // Optional: cancel the ride with API call here

            // Go back to the main screen
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        ridesAheadText = findViewById(R.id.rides_ahead_text)
        estimatedTimeText = findViewById(R.id.estimated_time_text)
        refreshButton = findViewById(R.id.refresh_button)

        updateQueueInfo()


        refreshButton.setOnClickListener {
            // Simulate update or fetch new values
            ridesAhead--
            estimatedTime -= 3
            updateQueueInfo()
        }
    }

    private fun updateQueueInfo() {
        ridesAheadText.text = "$ridesAhead ride(s) ahead of you"
        estimatedTimeText.text = "Estimated wait time: $estimatedTime minutes"
    }
}
