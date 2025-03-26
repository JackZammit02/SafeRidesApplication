package com.example.saferidesapplication

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.saferidesapplication.network.ApiClient
import com.example.saferidesapplication.network.dto.RideRequest
import kotlinx.coroutines.*
import retrofit2.HttpException

class PassengerActivity : ComponentActivity() {
    private lateinit var driverSwitchTextView: TextView
    private var pollingJob: Job? = null
    private lateinit var passengerId: String
    private lateinit var recyclerView: RecyclerView

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_passenger)

        title = "Passenger Page"

        val pickupSpinner: Spinner = findViewById(R.id.pickupSpinner)
        val dropOffSpinner: Spinner = findViewById(R.id.dropOffSpinner)
        val passengerCountSpinner: Spinner = findViewById(R.id.passengerCountSpinner)
        val requestRideButton: Button = findViewById(R.id.requestRideButton)
        val sharedPreferences = getSharedPreferences("SafeRidesPrefs", MODE_PRIVATE)
        passengerId = sharedPreferences.getString("userId", null) ?: "unknown"

        recyclerView = findViewById(R.id.rideQueueRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Start polling the queue every few seconds
        startPollingQueue()

        val locations = listOf(
            "Norelius Hall", "Three Flags Circle", "North Hall", "7th Street Houses",
            "Rundstrom Hall", "Sohre Hall", "Arbor View Apartments", "Music Building South", "Nobel Hall",
            "Chapel Circle", "International Center", "Lund Center", "College View Apartments", "Chapel View Townhomes"
        )
        val passengerCounts = listOf("1", "2", "3", "4")

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, locations)
        pickupSpinner.adapter = adapter
        dropOffSpinner.adapter = adapter

        val passengerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, passengerCounts)
        passengerCountSpinner.adapter = passengerAdapter

        requestRideButton.setOnClickListener {
            val pickup = pickupSpinner.selectedItem.toString()
            val dropOff = dropOffSpinner.selectedItem.toString()
            val passengers = passengerCountSpinner.selectedItem.toString().toInt()

            if (passengerId == "unknown") {
                Toast.makeText(this, "Error: Passenger ID not found", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val rideRequest = RideRequest(
                passengerId = passengerId,
                pickupLocation = pickup,
                dropoffLocation = dropOff,
                passengerCount = passengers
            )

            lifecycleScope.launch {
                try {
                    val response = ApiClient.apiService.requestRide(rideRequest)
                    if (response.isSuccessful && response.body() != null) {
                        Toast.makeText(
                            this@PassengerActivity,
                            "Ride successfully requested!",
                            Toast.LENGTH_LONG
                        ).show()

                        // Immediately refresh queue
                        loadRideQueue()
                    } else {
                        Toast.makeText(
                            this@PassengerActivity,
                            "Request failed: ${response.code()}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } catch (e: HttpException) {
                    Toast.makeText(
                        this@PassengerActivity,
                        "HTTP error: ${e.message()}",
                        Toast.LENGTH_LONG
                    ).show()
                } catch (e: Exception) {
                    Toast.makeText(
                        this@PassengerActivity,
                        "Network error: ${e.localizedMessage}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

        driverSwitchTextView = findViewById(R.id.driverSwitchTextView)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun startPollingQueue() {
        pollingJob = lifecycleScope.launch {
            while (isActive) {
                loadRideQueue()
                delay(5000) // 5 seconds
            }
        }
    }

    private fun loadRideQueue() {
        lifecycleScope.launch {
            try {
                val response = ApiClient.apiService.getAllRides()
                if (response.isSuccessful && response.body() != null) {
                    val queuedRides = response.body()!!
                        .filter { it.status == "queued" }
                        .sortedByDescending { it.timestamp }
                    recyclerView.adapter = RideQueueAdapter(queuedRides, passengerId, isDriverView = false)
                } else {
                    Toast.makeText(this@PassengerActivity, "Could not load queue", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@PassengerActivity, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        pollingJob?.cancel()
    }

    fun updateDriverSwitchStatus(isSwitching: Boolean) {
        if (isSwitching) {
            driverSwitchTextView.visibility = View.VISIBLE
        } else {
            driverSwitchTextView.visibility = View.GONE
        }
    }
}
