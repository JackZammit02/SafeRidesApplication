package com.example.saferidesapplication

data class RideRequest(
    val requestId: String,
    val passengerName: String,
    val pickupLocation: String,
    val destination: String,
    val status: String // e.g., "Pending", "In-Progress", "Completed"
)