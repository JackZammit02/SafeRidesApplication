package com.example.saferidesapplication.network.dto

data class RideRequest(
    val rideId: String = "",
    val passengerId: String = "",
    val pickupLocation: String = "",
    val dropoffLocation: String = "",
    val passengerCount: Int = 1,
    val status: String = "queued",
    val driverId: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

