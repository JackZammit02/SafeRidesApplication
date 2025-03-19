package com.example.saferidesapplication

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.example.saferidesapplication.network.ApiClient
import com.example.saferidesapplication.network.RegisterRequest
import kotlinx.coroutines.launch

class DriverActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_driver)

        val switchDriversButton: Button = findViewById(R.id.switchingDriversButton)
        switchDriversButton.setOnClickListener {
            // maybe call something like switchDrivers()
        }

        val logOffButton: Button = findViewById(R.id.logOffButton)
        logOffButton.setOnClickListener {
            // maybe call logoutDriver()
        }

        // If you want to do driver registration from here, you could do:
        // registerDriver()
        // but typically you'd do it earlier in the flow, or from a separate "RegisterDriver" screen.
    }

    private fun registerDriver() {
        lifecycleScope.launch {
            try {
                val requestBody = RegisterRequest(access_code = "DRIVER123")
                val response = ApiClient.apiService.registerDriver(requestBody)
                if (response.isSuccessful) {
                    val body = response.body()
                    Toast.makeText(
                        this@DriverActivity,
                        "Driver Created! Id = ${body?.userId}",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        this@DriverActivity,
                        "Failed: ${response.message()}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@DriverActivity,
                    "Error: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
