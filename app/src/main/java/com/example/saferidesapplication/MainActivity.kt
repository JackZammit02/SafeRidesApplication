package com.example.saferidesapplication

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.example.saferidesapplication.network.ApiClient
import com.example.saferidesapplication.network.dto.CreateUserRequest
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
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
    }

    private fun registerPassenger() {
        lifecycleScope.launch {
            try {
                val requestBody = CreateUserRequest(
                    id = "passenger123",
                    name = "Jack", // Later: use EditText for dynamic name
                    role = "passenger",
                    onShift = false
                )
                val response = ApiClient.apiService.createUser(requestBody)
                if (response.isSuccessful) {
                    val sharedPreferences = getSharedPreferences("SafeRidesPrefs", MODE_PRIVATE)
                    sharedPreferences.edit().putString("userId", requestBody.id).apply()

                    val intent = Intent(this@MainActivity, PassengerActivity::class.java)
                    startActivity(intent)
                } else {
                    // TODO: Show error Toast or dialog
                }
            } catch (e: Exception) {
                // TODO: Show error Toast or dialog
            }
        }
    }
}
