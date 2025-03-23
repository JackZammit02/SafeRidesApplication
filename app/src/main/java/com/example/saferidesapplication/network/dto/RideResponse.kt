package com.example.saferidesapplication.network.dto

data class RideResponse(
    val rideId: String,
    val passengerId: String,
    val pickupLocation: String,
    val dropoffLocation: String,
    val passengerCount: Int,
    val status: String,
    val driverId: String?,
    val timestamp: Long
)
