// Updated MainActivity.kt
package com.example.saferidesapplication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.saferidesapplication.network.ApiClient
import com.example.saferidesapplication.network.dto.CancelRideRequest
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

        lifecycleScope.launch {
            try {
                val response = ApiClient.apiService.getAllRides()
                if (response.isSuccessful) {
                    Log.d("BACKEND_TEST", "✅ Connected! Rides count: ${response.body()?.size}")
                } else {
                    Log.e("BACKEND_TEST", "❌ Response failed: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("BACKEND_TEST", "❌ Connection failed: ${e.localizedMessage}")
            }
        }

        val driverButton: Button = findViewById(R.id.driverButton)
        val passengerButton: Button = findViewById(R.id.passengerButton)
        val cancelRideButton: Button = findViewById(R.id.cancelRideButton)
        val driverId: String by lazy {
            getSharedPreferences("SafeRidesPrefs", MODE_PRIVATE).getString("userId", "") ?: ""
        }

        driverButton.setOnClickListener {
            val intent = Intent(this, AccessCodeActivity::class.java)
            startActivity(intent)
        }

        passengerButton.setOnClickListener {
            registerPassenger()
        }

        cancelRideButton.setOnClickListener {
            cancelActiveRide()
        }

        val sharedPreferences = getSharedPreferences("SafeRidesPrefs", MODE_PRIVATE)
        passengerId = sharedPreferences.getString("userId", null) ?: "unknown"

        recyclerView = findViewById(R.id.rideQueueRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        startPollingQueue()
    }

    override fun onResume() {
        super.onResume()

        val sharedPreferences = getSharedPreferences("SafeRidesPrefs", MODE_PRIVATE)
        passengerId = sharedPreferences.getString("userId", null) ?: "unknown"
        Log.d("PASSENGER_ID", "Refreshed in onResume(): $passengerId")

        loadRideQueue()

        if (pollingJob == null || pollingJob?.isActive == false) {
            startPollingQueue()
        }
    }

    private fun registerPassenger() {
        lifecycleScope.launch {
            try {
                val requestBody = CreateUserRequest(
                    role = "passenger",
                    onShift = false
                )

                val response = ApiClient.apiService.createUser(requestBody)

                if (response.isSuccessful) {
                    val responseBody = response.body() as CreateUserResponse
                    val userId = responseBody.userId

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
        pollingJob?.cancel()
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
                val driversResponse = ApiClient.apiService.getAllDrivers()
                val activeDrivers = driversResponse.body()?.count { it.onShift } ?: 0

                val ridesResponse = ApiClient.apiService.getAllRides()
                if (ridesResponse.isSuccessful) {
                    val rideList = ridesResponse.body()

                    if (rideList != null) {
                        val queuedRides = rideList.filter { it.status == "queued" }
                            .sortedBy { it.timestamp }

                        recyclerView.adapter = RideQueueAdapter(queuedRides, passengerId, isDriverView = false)

                        updatePassengerRidePosition(queuedRides)

                        val hasActiveRide = rideList.any {
                            it.passengerId == passengerId && it.status in listOf("queued", "assigned", "arrived")
                        }

                        val requestButton = findViewById<Button>(R.id.passengerButton)
                        val cancelButton = findViewById<Button>(R.id.cancelRideButton)
                        val driverButton = findViewById<Button>(R.id.driverButton)
                        val driverStatusTextView = findViewById<TextView>(R.id.driverAvailabilityStatusTextView)

                        val driverStatus = "$activeDrivers driver${if (activeDrivers != 1) "s" else ""} active"
                        driverStatusTextView.text = driverStatus

                        requestButton.isEnabled = activeDrivers > 0 && !hasActiveRide
                        requestButton.alpha = if (requestButton.isEnabled) 1.0f else 0.5f

                        cancelButton.visibility = if (hasActiveRide) Button.VISIBLE else Button.GONE

                        driverButton.isEnabled = !hasActiveRide
                        driverButton.alpha = if (hasActiveRide) 0.5f else 1.0f
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

    private fun cancelActiveRide() {
        lifecycleScope.launch {
            try {
                val response = ApiClient.apiService.getAllRides()
                if (response.isSuccessful) {
                    val myRide = response.body()?.find {
                        it.passengerId == passengerId && it.status in listOf("queued", "assigned", "arrived")
                    }

                    if (myRide == null) {
                        Toast.makeText(this@MainActivity, "No cancellable ride found", Toast.LENGTH_SHORT).show()
                        return@launch
                    }

                    val cancelRequest = CancelRideRequest(
                        userId = passengerId,
                        isDriver = false
                    )

                    val cancelResponse = ApiClient.apiService.cancelRide(myRide.rideId, cancelRequest)
                    if (cancelResponse.isSuccessful) {
                        Toast.makeText(this@MainActivity, "Ride cancelled", Toast.LENGTH_SHORT).show()
                        loadRideQueue()
                    } else {
                        Toast.makeText(this@MainActivity, "Cancel failed: ${cancelResponse.code()}", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        pollingJob?.cancel()
    }
}
