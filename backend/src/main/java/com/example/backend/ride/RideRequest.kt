package com.example.backend.ride

import kotlinx.serialization.Serializable

@Serializable
data class RideRequest(
    val passengerId: Int
)
