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
import com.example.saferidesapplication.network.dto.RideResponse
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

        //val pickupSpinner: Spinner = findViewById(R.id.pickupSpinner)
        //val dropOffSpinner: Spinner = findViewById(R.id.dropOffSpinner)
        //val passengerCountSpinner: Spinner = findViewById(R.id.passengerCountSpinner)
        val requestRideButton: Button = findViewById(R.id.requestRideButton)
        val cancelRideButton: Button = findViewById(R.id.cancelRideButton)
        cancelRideButton.visibility = View.GONE

        val sharedPreferences = getSharedPreferences("SafeRidesPrefs", MODE_PRIVATE)
        val backButton = findViewById<Button>(R.id.backButton)
        backButton.setOnClickListener {
            finish()
        }

        passengerId = sharedPreferences.getString("userId", null) ?: "unknown"

        recyclerView = findViewById(R.id.rideQueueRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        startPollingQueue()


        requestRideButton.setOnClickListener {
            showRideRequestDialog()
        }


        cancelRideButton.setOnClickListener {
            Toast.makeText(this@PassengerActivity, "Ride cancelled (backend coming soon)", Toast.LENGTH_SHORT).show()
            cancelRideButton.visibility = View.GONE
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
                    Toast.makeText(this@PassengerActivity, "Could not load queue", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@PassengerActivity, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
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

    private fun showRideRequestDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_request_ride, null)

        val pickupSpinner = dialogView.findViewById<Spinner>(R.id.pickupSpinner)
        val dropOffSpinner = dialogView.findViewById<Spinner>(R.id.dropOffSpinner)
        val passengerCountSpinner = dialogView.findViewById<Spinner>(R.id.passengerCountSpinner)
        val confirmButton = dialogView.findViewById<Button>(R.id.requestRideButton)

        val locations = listOf(
            "Norelius Hall", "Three Flags Circle", "North Hall", "7th Street Houses",
            "Rundstrom Hall", "Sohre Hall", "Arbor View Apartments", "Music Building South", "Nobel Hall",
            "Chapel Circle", "International Center", "Lund Center", "College View Apartments", "Chapel View Townhomes"
        )
        val passengerCounts = listOf("1", "2", "3", "4")

        pickupSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, locations)
        dropOffSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, locations)
        passengerCountSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, passengerCounts)

        val dialog = android.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        confirmButton.setOnClickListener {
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
                        Toast.makeText(this@PassengerActivity, "Ride successfully requested!", Toast.LENGTH_LONG).show()
                        findViewById<Button>(R.id.cancelRideButton).visibility = View.VISIBLE
                        loadRideQueue()
                        dialog.dismiss()
                    } else {
                        Toast.makeText(this@PassengerActivity, "Request failed: ${response.code()}", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@PassengerActivity, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }

        dialog.show()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)    }


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
