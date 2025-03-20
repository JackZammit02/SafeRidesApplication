package com.example.saferidesapplication

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class AccessCodeActivity : AppCompatActivity() {

    private lateinit var accessCodeInput: EditText
    private lateinit var submitButton: Button
    private val VALID_CODE = "123456" // Replace with actual logic

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_access_code)

        accessCodeInput = findViewById(R.id.access_code_input)
        submitButton = findViewById(R.id.submit_button)

        submitButton.setOnClickListener {
            val enteredCode = accessCodeInput.text.toString().trim()
            if (enteredCode == VALID_CODE) {
                Toast.makeText(this, "Access Granted!", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, DriverDashboardActivity::class.java))
            } else {
                Toast.makeText(this, "Invalid Access Code", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
