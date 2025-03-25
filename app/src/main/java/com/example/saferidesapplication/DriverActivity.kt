package com.example.saferidesapplication

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.example.saferidesapplication.network.ApiClient
import com.example.saferidesapplication.network.dto.CreateUserRequest
import kotlinx.coroutines.launch

class DriverActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_driver)

        title = "Driver's Page"

        val switchDriversButton: Button = findViewById(R.id.switchingDriversButton)
        switchDriversButton.setOnClickListener {

        }
        val logOffButton: Button = findViewById(R.id.logOffButton)
        logOffButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }


        // If you want to do driver registration from here, you could do:
        // registerDriver()
        // but typically you'd do it earlier in the flow, or from a separate "RegisterDriver" screen.
    }

    private fun logInDriver() {
        lifecycleScope.launch {
            val driverId = "driver123"

            val createReq = CreateUserRequest(
                id = driverId,
                name = "Driver Jack",
                role = "driver",
                onShift = true
            )

            val response = ApiClient.apiService.createUser(createReq)
            if (response.isSuccessful) {
                Toast.makeText(this@DriverActivity, "Logged in!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this@DriverActivity, "Login failed", Toast.LENGTH_LONG).show()
            }
        }
    }


}
