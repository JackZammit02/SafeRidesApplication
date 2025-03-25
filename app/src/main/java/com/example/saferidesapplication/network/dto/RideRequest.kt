package com.example.saferidesapplication.network.dto

data class RideRequest(
    val rideId: String = "",            // leave blank when sending
    val passengerId: String,
    val pickupLocation: String,
    val dropoffLocation: String,
    val passengerCount: Int,
    val status: String = "queued",      // default status
    val driverId: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
