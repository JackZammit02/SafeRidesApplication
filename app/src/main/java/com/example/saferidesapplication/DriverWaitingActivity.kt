package com.example.saferidesapplication

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class DriverWaitingActivity : AppCompatActivity() {

    private lateinit var timerText: TextView
    private lateinit var pickedUpButton: Button
    private lateinit var cancelButton: Button

    private lateinit var countDownTimer: CountDownTimer
    private val totalTime = 4 * 60 * 1000L // 4 minutes in milliseconds

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_driver_waiting)

        timerText = findViewById(R.id.waitingTimerText)
        pickedUpButton = findViewById(R.id.pickedUpButton)
        cancelButton = findViewById(R.id.cancelButton)

        startTimer()

        pickedUpButton.setOnClickListener {
            Toast.makeText(this, "Passenger picked up!", Toast.LENGTH_SHORT).show()
            // Example: go back to main screen or show next step
            finish()
        }

        cancelButton.setOnClickListener {
            Toast.makeText(this, "Pickup canceled", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun startTimer() {
        countDownTimer = object : CountDownTimer(totalTime, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val minutes = (millisUntilFinished / 1000) / 60
                val seconds = (millisUntilFinished / 1000) % 60
                timerText.text = String.format("Waiting time: %d:%02d", minutes, seconds)
            }

            override fun onFinish() {
                timerText.text = "Time's up!"
            }
        }.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer.cancel()
    }
}
