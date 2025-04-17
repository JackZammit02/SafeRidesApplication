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
    private val driverId: String by lazy {
        getSharedPreferences("SafeRidesPrefs", MODE_PRIVATE).getString("userId", "") ?: ""
    }
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


        val endShiftButton: Button = findViewById(R.id.endShiftButton)
        val actionButton: Button = findViewById(R.id.hereButton)



        endShiftButton.setOnClickListener {
            val dialogView = layoutInflater.inflate(R.layout.dialog_end_shift, null)
            val alertDialog = android.app.AlertDialog.Builder(this)
                .setView(dialogView)
                .create()

            dialogView.findViewById<Button>(R.id.switchButton).setOnClickListener {
                alertDialog.dismiss()
                handleEndShift(switching = true)
            }

            dialogView.findViewById<Button>(R.id.logOffButton).setOnClickListener {
                alertDialog.dismiss()
                handleEndShift(switching = false)
            }

            alertDialog.show()
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

                    val hereButton = findViewById<Button>(R.id.hereButton)
                    hereButton.visibility = if (rides.isNotEmpty()) Button.VISIBLE else Button.GONE
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

    private fun handleEndShift(switching: Boolean) {
        lifecycleScope.launch {
            try {
                if (switching) {
                    val switchResponse = apiService.setDriverSwitching(
                        com.example.saferidesapplication.network.dto.DriverSwitchRequest(true)
                    )
                    if (!switchResponse.isSuccessful) {
                        Toast.makeText(this@DriverActivity, "Failed to notify passengers", Toast.LENGTH_SHORT).show()
                        return@launch
                    }
                }

                val shiftOffResponse = apiService.updateDriverShift(driverId, ShiftUpdateRequest(onShift = false))
                if (shiftOffResponse.isSuccessful) {
                    val intent = Intent(this@DriverActivity, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this@DriverActivity, "Failed to end shift", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@DriverActivity, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }





    override fun onDestroy() {
        super.onDestroy()
        pollingJob?.cancel()
    }
}
