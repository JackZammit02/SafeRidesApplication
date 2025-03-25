package com.example.saferidesapplication

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.ComponentActivity

class AccessCodeActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_access_code)

        // Initialize the views
        val accessCodeInput: EditText = findViewById(R.id.access_code_input)
        val submitButton: Button = findViewById(R.id.submit_button)
        val exitButton: Button = findViewById(R.id.exit_button)

        exitButton.setOnClickListener {
            finish() //This closes this activity and returns to the previous one
        }


        submitButton.setOnClickListener {
            val accessCode = accessCodeInput.text.toString()

            // Validate the access code
            if (accessCode == "123456") {
                // If the code is correct, navigate to DriverActivity
                val intent = Intent(this, DriverActivity::class.java)
                startActivity(intent)
                finish() // Optionally finish this activity so the user can't navigate back to it
            } else {
                // If the code is incorrect, show a toast message
                Toast.makeText(this, "Invalid Access Code. Please try again.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
