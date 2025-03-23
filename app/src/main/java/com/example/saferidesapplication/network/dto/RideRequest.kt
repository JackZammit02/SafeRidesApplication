package com.example.saferidesapplication.network.dto

data class RideRequest(
    val passengerId: String,
    val pickupLocation: String,
    val dropoffLocation: String,
    val passengerCount: Int
)
