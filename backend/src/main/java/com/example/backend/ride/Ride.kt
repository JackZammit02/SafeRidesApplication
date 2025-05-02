package com.example.backend.ride

import kotlinx.serialization.Serializable

@Serializable
data class Ride(
    val passengerId: Int,
    val driverId: Int
)
