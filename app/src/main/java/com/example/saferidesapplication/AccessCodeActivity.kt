package com.example.saferidesapplication

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.example.saferidesapplication.network.ApiClient
import com.example.saferidesapplication.network.dto.AccessCode
import com.example.saferidesapplication.network.dto.DriverSwitchRequest
import kotlinx.coroutines.launch

class AccessCodeActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_access_code)

        val accessCodeInput: EditText = findViewById(R.id.access_code_input)
        val submitButton: Button = findViewById(R.id.submit_button)
        val exitButton: Button = findViewById(R.id.exit_button)

        exitButton.setOnClickListener {
            finish()
        }

        submitButton.setOnClickListener {
            val accessCode = accessCodeInput.text.toString().trim()

            if (accessCode.isEmpty()) {
                Toast.makeText(this, "Please enter an access code", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Send access code to backend
            lifecycleScope.launch {
                try {
                    val response = ApiClient.apiService.loginDriver(AccessCode(accessCode))
                    if (response.isSuccessful) {
                        getSharedPreferences("SafeRidesPrefs", MODE_PRIVATE)
                            .edit()
                            .putString("driverId", accessCode)  // 🛡️ Save separately
                            .remove("passengerId")  // 🧹 clear old passengerId if any
                            .apply()


                        val intent = Intent(this@AccessCodeActivity, DriverActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    } else {
                        when (response.code()) {
                            403 -> showToast("Too many drivers are logged in.")
                            404 -> showToast("Access code not recognized.")
                            409 -> showToast("Driver is already logged in.")
                            else -> showToast("Error: ${response.code()}")
                        }
                    }
                } catch (e: Exception) {
                    showToast("Login failed: ${e.localizedMessage}")
                }
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this@AccessCodeActivity, message, Toast.LENGTH_LONG).show()
    }
}
