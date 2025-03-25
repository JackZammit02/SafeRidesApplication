package com.example.saferidesapplication.network.dto

data class CreateUserRequest(
    val role: String,
    val onShift: Boolean = false
)
