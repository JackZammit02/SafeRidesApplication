package com.example.saferidesapplication

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.lifecycle.lifecycleScope
import com.example.saferidesapplication.network.ApiClient
import com.example.saferidesapplication.network.dto.RideRequest
import kotlinx.coroutines.launch

class PassengerActivity : AppCompatActivity() {

    private lateinit var pickupSpinner: Spinner
    private lateinit var dropOffSpinner: Spinner
    private lateinit var passengerCountSpinner: Spinner
    private lateinit var confirmButton: Button
    private lateinit var passengerId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_passenger)

        val sharedPreferences = getSharedPreferences("SafeRidesPrefs", MODE_PRIVATE)
        passengerId = sharedPreferences.getString("passengerId", null) ?: "unknown"


        // Initialize UI elements
        pickupSpinner = findViewById(R.id.pickupSpinner)
        dropOffSpinner = findViewById(R.id.dropOffSpinner)
        passengerCountSpinner = findViewById(R.id.passengerCountSpinner)
        confirmButton = findViewById(R.id.confirmRideButton)

        val locations = listOf(
            "Norelius Hall", "Three Flags Circle", "North Hall", "7th Street Houses",
            "Rundstrom Hall", "Sohre Hall", "Arbor View Apartments", "Music Building South", "Nobel Hall",
            "Chapel Circle", "International Center", "Lund Center", "College View Apartments", "Chapel View Townhomes"
        )
        val passengerCounts = listOf("1", "2", "3", "4")

        pickupSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, locations)
        dropOffSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, locations)
        passengerCountSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, passengerCounts)

        confirmButton.setOnClickListener {
            val pickup = pickupSpinner.selectedItem.toString()
            val dropoff = dropOffSpinner.selectedItem.toString()
            val passengerCount = passengerCountSpinner.selectedItem.toString().toInt()

            if (pickup == dropoff) {
                Toast.makeText(this, "Pickup and drop-off cannot be the same", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val rideRequest = RideRequest(
                passengerId = passengerId,
                pickupLocation = pickup,
                dropoffLocation = dropoff,
                passengerCount = passengerCount
            )

            lifecycleScope.launch {
                try {
                    val response = ApiClient.apiService.requestRide(rideRequest)
                    if (response.isSuccessful) {
                        Toast.makeText(this@PassengerActivity, "Ride successfully requested!", Toast.LENGTH_LONG).show()

                        // ✅ Set local ride state as active
                        setRideActiveState(true)

                        val intent = Intent(this@PassengerActivity, MainActivity::class.java)
                        startActivity(intent)
                        finish()
                    }else {
                        Toast.makeText(this@PassengerActivity, "Request failed: ${response.code()}", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@PassengerActivity, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
        findViewById<Button>(R.id.backButton).setOnClickListener {
            finish()
        }


    }

    private fun setRideActiveState(isActive: Boolean) {
        getSharedPreferences("SafeRidesPrefs", MODE_PRIVATE).edit {
            putBoolean("hasActiveRide", isActive)
        }
    }
}
