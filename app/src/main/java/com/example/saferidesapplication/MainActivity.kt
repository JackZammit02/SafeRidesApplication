package com.example.saferidesapplication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.example.saferidesapplication.network.ApiClient
import com.example.saferidesapplication.network.dto.CancelRideRequest
import com.example.saferidesapplication.network.dto.CreateUserRequest
import com.example.saferidesapplication.network.dto.CreateUserResponse
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch


class MainActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var passengerId: String
    private var pollingJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewPager = findViewById(R.id.viewPager)
        viewPager.adapter = PassengerPagerAdapter(this) // must come BEFORE


        val driverButton: Button = findViewById(R.id.driverButton)
        val passengerButton: Button = findViewById(R.id.passengerButton)
        val cancelRideButton: Button = findViewById(R.id.cancelRideButton)

        val sharedPreferences = getSharedPreferences("SafeRidesPrefs", MODE_PRIVATE)
        passengerId = sharedPreferences.getString("userId", null) ?: "unknown"

        driverButton.setOnClickListener {
            val intent = Intent(this, AccessCodeActivity::class.java)
            startActivity(intent)
        }

        passengerButton.setOnClickListener {
            val intent = Intent(this, PassengerActivity::class.java)
            startActivity(intent)
        }

        cancelRideButton.setOnClickListener {
            cancelActiveRide()
        }

        startPollingQueue()
    }

    private fun registerPassenger(onSuccess: (() -> Unit)? = null) {
        lifecycleScope.launch {
            try {
                val requestBody = CreateUserRequest(role = "passenger", onShift = false)
                val response = ApiClient.apiService.createUser(requestBody)

                if (response.isSuccessful) {
                    val responseBody = response.body() as CreateUserResponse
                    val userId = responseBody.userId

                    getSharedPreferences("SafeRidesPrefs", MODE_PRIVATE)
                        .edit().putString("userId", userId).apply()

                    passengerId = userId
                    onSuccess?.invoke()

                    val intent = Intent(this@MainActivity, PassengerActivity::class.java)
                    startActivity(intent)
                } else {
                    Toast.makeText(this@MainActivity, "User creation failed", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
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

                    val cancelRequest = CancelRideRequest(userId = passengerId, isDriver = false)
                    val cancelResponse = ApiClient.apiService.cancelRide(myRide.rideId, cancelRequest)

                    if (cancelResponse.isSuccessful) {
                        Toast.makeText(this@MainActivity, "Ride cancelled", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@MainActivity, "Cancel failed", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private suspend fun updateDriverStatusAndRideButton() {
        try {
            val driverStatusTextView: TextView = findViewById(R.id.driverAvailabilityStatusTextView)
            val passengerButton: Button = findViewById(R.id.passengerButton)

            val response = ApiClient.apiService.getAllDrivers()
            if (response.isSuccessful) {
                val drivers = response.body() ?: emptyList()
                val activeDrivers = drivers.filter { it.onShift == true }

                driverStatusTextView.text = "${activeDrivers.size} drivers on shift"

                passengerButton.isEnabled = activeDrivers.isNotEmpty()
                passengerButton.alpha = if (activeDrivers.isNotEmpty()) 1f else 0.5f

            } else {
                driverStatusTextView.text = "Error fetching driver status"
                passengerButton.isEnabled = false
                passengerButton.alpha = 0.5f
            }
        } catch (e: Exception) {
            findViewById<TextView>(R.id.driverAvailabilityStatusTextView).text = "Network error"
            findViewById<Button>(R.id.passengerButton).apply {
                isEnabled = false
                alpha = 0.5f
            }
        }
    }

    private suspend fun updatePassengerQueuePosition() {
        try {
            val response = ApiClient.apiService.getAllRides()
            if (response.isSuccessful) {
                val rides = response.body() ?: emptyList()
                val queuedRides = rides.filter { it.status == "queued" }.sortedBy { it.timestamp }

                val myPosition = queuedRides.indexOfFirst { it.passengerId == passengerId } + 1
                val statusTextView: TextView = findViewById(R.id.passengerRideStatusTextView)

                if (myPosition > 0) {
                    statusTextView.text = "Your ride is in position: $myPosition"
                } else {
                    statusTextView.text = "You are not in the queue"
                }
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Failed to update queue position: ${e.message}")
        }
    }


    private fun startPollingQueue() {
        pollingJob?.cancel()
        pollingJob = lifecycleScope.launch {
            while (isActive) {
                updateDriverStatusAndRideButton()
                updatePassengerQueuePosition()  // 👈 Add this
                delay(5000)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        pollingJob?.cancel()
    }
}
