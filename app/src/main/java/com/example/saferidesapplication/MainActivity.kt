package com.example.saferidesapplication

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import com.example.saferidesapplication.network.ApiClient
import com.example.saferidesapplication.network.dto.CreateUserRequest
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // If using Compose, setContent {...}; or if using XML:
        setContentView(R.layout.activity_main)

        // Suppose you have two buttons: driverButton and passengerButton
        val driverButton: Button = findViewById(R.id.driverButton)
        driverButton.setOnClickListener {
            // Navigate to a dedicated DriverActivity
            val intent = Intent(this, DriverActivity::class.java)
            startActivity(intent)
        }

        val passengerButton: Button = findViewById(R.id.passengerButton)
        passengerButton.setOnClickListener {
            // e.g., you might do registerPassenger() here or go to a PassengerActivity
            registerPassenger()
        }
    }

    private fun registerPassenger() {
        lifecycleScope.launch {
            try {
                val requestBody = CreateUserRequest(
                    id = "passenger123",
                    name = "Jack", // could be dynamic input later
                    role = "passenger",
                    onShift = false
                )
                val response = ApiClient.apiService.createUser(requestBody)
                if (response.isSuccessful) {
                    // Navigate to PassengerActivity
                    val intent = Intent(this@MainActivity, PassengerActivity::class.java)
                    startActivity(intent)
                } else {
                    // Show error
                }
            } catch (e: Exception) {
                // Show error
            }
        }
    }

}
