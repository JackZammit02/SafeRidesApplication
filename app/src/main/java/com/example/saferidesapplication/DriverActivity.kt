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
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch

class DriverActivity : ComponentActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var rideQueueAdapter: RideQueueAdapter
    private val driverId: String by lazy {
        getSharedPreferences("SafeRidesPrefs", MODE_PRIVATE).getString("userId", "") ?: ""
    }
    private var rideListenerRegistration: ListenerRegistration? = null
    private var currentRideId: String? = null
    private var currentStage = 0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_driver)

        title = "Driver's Page"

        recyclerView = findViewById(R.id.driverRideQueue)
        recyclerView.layoutManager = LinearLayoutManager(this)

        rideQueueAdapter = RideQueueAdapter(driverId, isDriverView = true)
        recyclerView.adapter = rideQueueAdapter


        listenToRideQueue()

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

    private fun listenToRideQueue() {
        rideListenerRegistration?.remove() // Avoid duplicate listeners

        val db = Firebase.firestore
        rideListenerRegistration = db.collection("rides")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener

                val rides = snapshot.toObjects(com.example.saferidesapplication.network.dto.RideResponse::class.java)
                    .filter {
                        it.status == "queued" || (it.driverId == driverId && it.status in listOf("assigned", "arrived", "in_progress"))
                    }
                    .sortedBy { it.timestamp }

                rideQueueAdapter.updateData(rides)
                rideQueueAdapter.notifyDataSetChanged()

                findViewById<Button>(R.id.hereButton).visibility = if (rides.isNotEmpty()) Button.VISIBLE else Button.GONE
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

    override fun onDestroy() {
        super.onDestroy()
        rideListenerRegistration?.remove()
    }
}
