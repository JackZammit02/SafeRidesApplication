package com.example.saferidesapplication

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.saferidesapplication.network.ApiClient.apiService
import com.example.saferidesapplication.network.dto.ShiftUpdateRequest
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class DriverActivity : ComponentActivity() {

    private lateinit var recyclerView: RecyclerView
    private val driverId = "driver123" // Update if dynamic later
    private var pollingJob: Job? = null
    private var currentRideId: String? = null
    private var currentStage = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_driver)

        title = "Driver's Page"

        recyclerView = findViewById(R.id.driverRideQueue)
        recyclerView.layoutManager = LinearLayoutManager(this)

        startPollingQueue()

        val switchDriversButton: Button = findViewById(R.id.switchingDriversButton)
        val logOffButton: Button = findViewById(R.id.logOffButton)
        val actionButton: Button = findViewById(R.id.hereButton)

        switchDriversButton.setOnClickListener {
            val request = ShiftUpdateRequest(onShift = false)
            lifecycleScope.launch {
                try {
                    val response = apiService.updateDriverShift(driverId, request)
                    if (response.isSuccessful) {
                        Toast.makeText(this@DriverActivity, "Driver switched successfully", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this@DriverActivity, MainActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(this@DriverActivity, "Failed to switch driver", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@DriverActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        logOffButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        actionButton.setOnClickListener {
            when (currentStage) {
                0 -> assignNextRide(actionButton)
                1 -> updateRideStatus("arrived", actionButton, "Picked Up", 2)
                2 -> updateRideStatus("in_progress", actionButton, "Complete Ride", 3)
                3 -> updateRideStatus("completed", actionButton, "Next Ride", 0, reset = true)
            }
        }
    }

    private fun startPollingQueue() {
        pollingJob = lifecycleScope.launch {
            while (isActive) {
                loadRideQueue()
                delay(5000)
            }
        }
    }

    private fun loadRideQueue() {
        lifecycleScope.launch {
            try {
                val response = apiService.getAllRides()
                if (response.isSuccessful && response.body() != null) {
                    val rides = response.body()!!
                        .filter {
                            it.status == "queued" || (it.driverId == driverId && it.status in listOf("assigned", "arrived", "in_progress"))
                        }
                        .sortedBy { it.timestamp }
                    recyclerView.adapter = RideQueueAdapter(rides, driverId, isDriverView = true)
                } else {
                    Toast.makeText(this@DriverActivity, "Could not load rides", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@DriverActivity, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun assignNextRide(button: Button) {
        button.isEnabled = false
        lifecycleScope.launch {
            try {
                val response = apiService.assignNextRide(driverId)
                if (response.isSuccessful && response.body() != null) {
                    val ride = response.body()!!
                    currentRideId = ride.rideId
                    currentStage = 1
                    button.text = "At Pick-Up"
                    button.setBackgroundColor(Color.YELLOW)
                    Toast.makeText(this@DriverActivity, "Assigned ride: ${ride.rideId}", Toast.LENGTH_SHORT).show()
                    loadRideQueue()
                } else {
                    Toast.makeText(this@DriverActivity, "No rides to assign", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@DriverActivity, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            } finally {
                button.isEnabled = true
            }
        }
    }

    private fun updateRideStatus(
        status: String,
        button: Button,
        nextLabel: String,
        nextStage: Int,
        reset: Boolean = false
    ) {
        val rideId = currentRideId ?: return
        button.isEnabled = false
        lifecycleScope.launch {
            try {
                val response = apiService.updateRideStatus(rideId, status)
                if (response.isSuccessful) {
                    Toast.makeText(this@DriverActivity, "Ride $status", Toast.LENGTH_SHORT).show()
                    button.text = nextLabel
                    currentStage = nextStage
                    when (status) {
                        "arrived" -> button.setBackgroundColor(Color.CYAN)
                        "picked_up" -> button.setBackgroundColor(Color.LTGRAY)
                        "completed" -> button.setBackgroundColor(Color.parseColor("#6200EE"))
                    }
                    if (reset) currentRideId = null
                    loadRideQueue()
                } else {
                    // NEW LOGGING
                    val errorBody = response.errorBody()?.string()
                    Toast.makeText(this@DriverActivity, "Failed to update status: ${response.code()}", Toast.LENGTH_SHORT).show()
                    android.util.Log.e("API_ERROR", "Status update failed: $errorBody")
                }
            } catch (e: Exception) {
                Toast.makeText(this@DriverActivity, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            } finally {
                button.isEnabled = true
            }
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        pollingJob?.cancel()
    }
}
