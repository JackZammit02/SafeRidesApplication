package com.example.saferidesapplication

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.saferidesapplication.network.ApiClient.apiService
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class DriverActivity : ComponentActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var rideQueueAdapter: RideQueueAdapter
    private val driverId: String by lazy {
        getSharedPreferences("SafeRidesPrefs", MODE_PRIVATE).getString("driverId", "") ?: ""
    }
    private var rideListenerRegistration: ListenerRegistration? = null
    private var currentRideId: String? = null
    private var currentStage = 0
    private var arrivalCountdownJob: Job? = null
    private lateinit var cancelRideButton: Button

    private var inactivityJob: Job? = null
    private val inactivityTimeoutMs = 10 * 60 * 1000L // 10 minutes



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_driver)

        title = "Driver's Page"

        recyclerView = findViewById(R.id.driverRideQueue)
        recyclerView.layoutManager = LinearLayoutManager(this)

        rideQueueAdapter = RideQueueAdapter(driverId, isDriverView = true)
        recyclerView.adapter = rideQueueAdapter


        listenToRideQueue()
        resetInactivityTimer()


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

        cancelRideButton = findViewById(R.id.cancelRideButton)
        cancelRideButton.text = "Cancel Ride (No Show)"
        cancelRideButton.isEnabled = false
        cancelRideButton.alpha = 0.5f
        cancelRideButton.setOnClickListener {
            cancelCurrentRide()
        }

    }

    private fun listenToRideQueue() {
        rideListenerRegistration?.remove() // Avoid duplicate listeners

        val db = Firebase.firestore
        rideListenerRegistration = db.collection("rides")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    Toast.makeText(this, "Failed to fetch rides", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                val updatedRides = snapshot.toObjects(com.example.saferidesapplication.network.dto.RideResponse::class.java)
                    .filter {
                        it.status == "queued" ||
                                (it.driverId == driverId && it.status in listOf("assigned", "arrived", "in_progress"))
                    }
                    .sortedBy { it.timestamp }

                // Only update adapter if list changed (optional optimization)
                rideQueueAdapter.updateData(updatedRides)
                rideQueueAdapter.notifyDataSetChanged()

                updateActionButtonVisibility(updatedRides)
            }
    }

    private fun updateActionButtonVisibility(rides: List<com.example.saferidesapplication.network.dto.RideResponse>) {
        val hasRelevantRides = rides.any {
            it.status == "queued" ||
                    (it.driverId == driverId && it.status in listOf("assigned", "arrived", "in_progress"))
        }

        findViewById<Button>(R.id.hereButton).visibility =
            if (hasRelevantRides) Button.VISIBLE else Button.GONE
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
                    button.setBackgroundResource(R.drawable.rounded_rectangle)
                    Toast.makeText(this@DriverActivity, "Assigned ride: ${ride.rideId}", Toast.LENGTH_SHORT).show()
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
                        "arrived" -> {
                            button.setBackgroundColor(Color.CYAN)
                            startArrivalCountdown() // Start the 2-minute timer
                        }
                        "picked_up" -> {
                            button.setBackgroundColor(Color.LTGRAY)
                            arrivalCountdownJob?.cancel()
                            cancelRideButton.text = "Cancel Ride (No Show)"
                            cancelRideButton.isEnabled = false
                            cancelRideButton.alpha = 0.5f
                            cancelRideButton.visibility = View.GONE
                        }
                        "completed" -> {
                            button.setBackgroundColor(Color.parseColor("#6200EE"))
                            arrivalCountdownJob?.cancel()
                            cancelRideButton.text = "Cancel Ride (No Show)"
                            cancelRideButton.isEnabled = false
                            cancelRideButton.alpha = 0.5f
                            cancelRideButton.visibility = View.GONE
                        }
                    }

                    if (reset) {
                        currentRideId = null
                        arrivalCountdownJob?.cancel()
                        cancelRideButton.text = "Cancel Ride (No Show)"
                        cancelRideButton.isEnabled = false
                        cancelRideButton.alpha = 0.5f
                        cancelRideButton.visibility = View.GONE
                    }

                } else {
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
                val response = if (switching) {
                    apiService.switchDriver(driverId)
                } else {
                    apiService.logoutDriver(driverId)
                }

                if (response.isSuccessful) {
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

    private fun startArrivalCountdown() {
        arrivalCountdownJob?.cancel()

        cancelRideButton.isEnabled = false
        cancelRideButton.alpha = 0.5f  // visually grayed out

        arrivalCountdownJob = lifecycleScope.launch {
            var secondsLeft = 2 * 60  // 2 minutes

            while (secondsLeft > 0) {
                val minutes = secondsLeft / 60
                val seconds = secondsLeft % 60
                cancelRideButton.text = String.format("Cancel (%d:%02d)", minutes, seconds)
                delay(1000)
                secondsLeft--
            }

            cancelRideButton.text = "Cancel Ride (No Show)"
            cancelRideButton.isEnabled = true
            cancelRideButton.alpha = 1f  // restore normal appearance
        }
    }


    private fun cancelCurrentRide() {
        if (currentStage != 2 || !cancelRideButton.isEnabled) {
            Toast.makeText(this@DriverActivity, "Cannot cancel — ride not eligible for no-show", Toast.LENGTH_SHORT).show()
            cancelRideButton.text = "Cancel Ride (No Show)"
            cancelRideButton.isEnabled = false
            cancelRideButton.alpha = 0.5f
            return
        }

        val rideId = currentRideId ?: return

        lifecycleScope.launch {
            try {
                val response = apiService.cancelRide(
                    rideId,
                    com.example.saferidesapplication.network.dto.CancelRideRequest(driverId, true)
                )
                if (response.isSuccessful) {
                    Toast.makeText(this@DriverActivity, "Ride cancelled due to no-show", Toast.LENGTH_SHORT).show()
                    currentRideId = null
                    currentStage = 0
                    cancelRideButton.text = "Cancel Ride (No Show)"
                    cancelRideButton.isEnabled = false
                    cancelRideButton.alpha = 0.5f
                    cancelRideButton.visibility = View.GONE
                    findViewById<Button>(R.id.hereButton).text = "Next Ride"
                } else {
                    val errorBody = response.errorBody()?.string()
                    android.util.Log.e("CANCEL_RIDE", "Failed with code ${response.code()} — $errorBody")
                    Toast.makeText(this@DriverActivity, "Failed to cancel ride", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@DriverActivity, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        resetInactivityTimer()
    }

    private fun resetInactivityTimer() {
        inactivityJob?.cancel()
        inactivityJob = lifecycleScope.launch {
            delay(inactivityTimeoutMs)
            autoLogoutDriver()
        }
    }

    private fun autoLogoutDriver() {
        lifecycleScope.launch {
            try {
                val response = apiService.logoutDriver(driverId)
                if (response.isSuccessful) {
                    Toast.makeText(this@DriverActivity, "Logged out due to inactivity", Toast.LENGTH_LONG).show()
                    val intent = Intent(this@DriverActivity, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this@DriverActivity, "Auto-logout failed (backend)", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@DriverActivity, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }




    override fun onDestroy() {
        super.onDestroy()
        rideListenerRegistration?.remove()
        arrivalCountdownJob?.cancel()
        inactivityJob?.cancel()

    }
}
