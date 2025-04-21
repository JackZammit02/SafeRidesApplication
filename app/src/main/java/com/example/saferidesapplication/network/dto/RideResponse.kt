package com.example.saferidesapplication.network.dto

data class RideResponse(
    val rideId: String = "",
    val passengerId: String = "",
    val pickupLocation: String = "",
    val dropoffLocation: String = "",
    val passengerCount: Int = 1,
    val status: String = "",
    val timestamp: Long = 0L,
    val driverId: String = ""
)
