package com.example.saferidesapplication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.saferidesapplication.network.ApiClient
import com.example.saferidesapplication.network.dto.CreateUserRequest
import com.example.saferidesapplication.network.dto.CreateUserResponse
import com.example.saferidesapplication.network.dto.RideResponse
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var passengerId: String
    private var pollingJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val driverButton: Button = findViewById(R.id.driverButton)
        driverButton.setOnClickListener {
            val intent = Intent(this, AccessCodeActivity::class.java)
            startActivity(intent)
        }

        val passengerButton: Button = findViewById(R.id.passengerButton)
        passengerButton.setOnClickListener {
            registerPassenger()
        }

        val sharedPreferences = getSharedPreferences("SafeRidesPrefs", MODE_PRIVATE)

        passengerId = sharedPreferences.getString("userId", null) ?: "unknown"

        recyclerView = findViewById(R.id.rideQueueRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        startPollingQueue()

    }

    private fun registerPassenger() {
        lifecycleScope.launch {
            try {
                val requestBody = CreateUserRequest(
                    // Later: use EditText for dynamic name input
                    role = "passenger",
                    onShift = false

                )

                val response = ApiClient.apiService.createUser(requestBody)

                if (response.isSuccessful) {
                    val responseBody = response.body() as CreateUserResponse
                    val userId = responseBody.userId

                    // Save the generated user ID in SharedPreferences
                    val sharedPreferences = getSharedPreferences("SafeRidesPrefs", MODE_PRIVATE)
                    sharedPreferences.edit().putString("userId", userId).apply()

                    Log.d("API_RESPONSE", "User created with ID: $userId")

                    val intent = Intent(this@MainActivity, PassengerActivity::class.java)
                    startActivity(intent)
                } else {
                    Toast.makeText(this@MainActivity, "User creation failed: ${response.code()}", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                e.printStackTrace()
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
                val response = ApiClient.apiService.getAllRides()
                if (response.isSuccessful) {
                    val rideList = response.body()

                    if (rideList != null) {
                        val queuedRides: List<RideResponse> = rideList.filter { it.status == "queued" }
                            .sortedBy { it.timestamp }

                        recyclerView.adapter = RideQueueAdapter(queuedRides, passengerId, isDriverView = false)

                        updatePassengerRidePosition(queuedRides)
                    }
                } else {
                    Toast.makeText(this@MainActivity, "Could not load queue", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updatePassengerRidePosition(queuedRides: List<RideResponse>) {
        val passengerRideStatusTextView: TextView = findViewById(R.id.passengerRideStatusTextView)

        val passengerRideIndex = queuedRides.indexOfFirst { it.passengerId == passengerId }

        passengerRideStatusTextView.text = if (passengerRideIndex != -1) {
            "Your ride is in position: ${passengerRideIndex + 1}"
        } else {
            "Your ride is not in the queue"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        pollingJob?.cancel()
    }
}

