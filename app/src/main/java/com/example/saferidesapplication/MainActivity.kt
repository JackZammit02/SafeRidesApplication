package com.example.saferidesapplication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.example.saferidesapplication.network.ApiClient
import com.example.saferidesapplication.network.dto.CreateUserRequest
import com.example.saferidesapplication.network.dto.CreateUserResponse
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
}

