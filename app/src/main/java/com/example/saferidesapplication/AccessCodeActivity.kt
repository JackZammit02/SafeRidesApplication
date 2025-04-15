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
                    val response = ApiClient.apiService.verifyAccessCode(AccessCode(accessCode))
                    when (response.code()) {
                        201 -> {
                            // Save driver ID in shared preferences
                            getSharedPreferences("SafeRidesPrefs", MODE_PRIVATE)
                                .edit()
                                .putString("userId", accessCode)
                                .apply()

                            // Clear "driver switching" flag on backend
                            lifecycleScope.launch {
                                try {
                                    ApiClient.apiService.setDriverSwitching(DriverSwitchRequest(switching = false))                                } catch (e: Exception) {
                                    // optional: log it, fail silently
                                }
                            }

                            // Navigate to DriverActivity
                            val intent = Intent(this@AccessCodeActivity, DriverActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            finish()
                        }
                        403 -> showToast("Too many drivers are logged in.")
                        409 -> showToast("This driver is already on shift.")
                        404 -> showToast("Access code not recognized.")
                        else -> showToast("Error: ${response.code()}")
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
