package com.example.saferidesapplication.network.dto;

data class UserResponse(
    val id: String,
    val name: String,
    val role: String,
    val onShift: Boolean
)
