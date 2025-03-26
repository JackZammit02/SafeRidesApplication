package com.example.saferidesapplication

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.saferidesapplication.network.ApiClient
import com.example.saferidesapplication.network.dto.CreateUserRequest
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class DriverActivity : ComponentActivity() {

    private lateinit var recyclerView: RecyclerView
    private val driverId = "driver123" // 👈 update if dynamic later
    private var pollingJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_driver)

        title = "Driver's Page"

        // Setup RecyclerView
        recyclerView = findViewById(R.id.driverRideQueue)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Start polling
        startPollingQueue()

        val switchDriversButton: Button = findViewById(R.id.switchingDriversButton)
        val logOffButton: Button = findViewById(R.id.logOffButton)

        switchDriversButton.setOnClickListener {
            // TODO: Handle driver switch
        }

        logOffButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
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
                if (response.isSuccessful && response.body() != null) {
                    val rides = response.body()!!
                        .filter {
                            it.status == "queued" || (it.status == "assigned" && it.driverId == driverId)
                        }
                        .sortedByDescending { it.timestamp }

                    recyclerView.adapter = RideQueueAdapter(rides, driverId, isDriverView = true)
                } else {
                    Toast.makeText(this@DriverActivity, "Could not load rides", Toast.LENGTH_SHORT).show()
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
