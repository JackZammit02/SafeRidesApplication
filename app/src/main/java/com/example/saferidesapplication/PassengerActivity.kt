package com.example.saferidesapplication

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.saferidesapplication.R.id.requestRideButton
import androidx.lifecycle.lifecycleScope
import com.example.saferidesapplication.network.ApiClient
import com.example.saferidesapplication.network.dto.RideRequest
import kotlinx.coroutines.launch
import retrofit2.HttpException

class PassengerActivity : ComponentActivity() {
    private lateinit var driverSwitchTextView: TextView

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
        val passengerId = sharedPreferences.getString("userId", null) ?: "unknown"


        // Sample locations
        val locations = listOf("Norelius Hall", "Three Flags Circle", "North Hall", "7th Street Houses",
            "Rundstrom Hall", "Sohre Hall", "Arbor View Apartments", "Music Building South", "Nobel Hall",
            "Chapel Circle", "International Center", "Lund Center", "College View Apartments", "Chapel View Townhomes")
        val passengerCounts = listOf("1", "2", "3", "4")

        // Populate dropdowns
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, locations)
        pickupSpinner.adapter = adapter
        dropOffSpinner.adapter = adapter

        val passengerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, passengerCounts)
        passengerCountSpinner.adapter = passengerAdapter

        // Inside onCreate, where you already have:
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

                        val createdRide = response.body()!!
                        println("Created Ride ID: ${createdRide.rideId}")
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


        // Initialize the TextView
        driverSwitchTextView = findViewById(R.id.driverSwitchTextView)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Example: Call this function when you detect a driver switch
        //updateDriverSwitchStatus(t)  // Set to 'true' when drivers are switching
    }

    public fun updateDriverSwitchStatus(isSwitching: Boolean) {
        if (isSwitching) {
            driverSwitchTextView.visibility = View.VISIBLE  // Show message
        } else {
            driverSwitchTextView.visibility = View.GONE  // Hide message
        }
    }
}
