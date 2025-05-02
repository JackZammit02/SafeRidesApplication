package com.example.backend

import kotlinx.serialization.Serializable

@Serializable
data class RegisterResponse(
    val message: String,
    val userId: Int
)
