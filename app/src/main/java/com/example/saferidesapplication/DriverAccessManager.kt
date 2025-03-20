package com.example.saferidesapplication

import java.util.*

class DriverAccessManager {
    private val validAccessCodes = mutableSetOf<String>()

    // Generate a random access code
    fun generateAccessCode(): String {
        val accessCode : String = UUID.randomUUID().toString().take(8).uppercase()
        validAccessCodes.add(accessCode)
        return accessCode
    }

    // Validate access code
    fun isValidAccessCode(code: String): Boolean {
        return validAccessCodes.contains(code)
    }
}

