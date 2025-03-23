package com.example.saferidesapplication.network.dto

data class CancelRideRequest(
    val userId: String,
    val isDriver: Boolean
)
