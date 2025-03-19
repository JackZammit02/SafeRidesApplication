package com.example.backend.user

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class User(
    val id: Int,
    val role: UserRole,
    var authCode: String? = null // Default to null, set it later
) {
    init {
        if (role == UserRole.DRIVER) {
            authCode = UUID.randomUUID().toString()
        }
    }
}

@Serializable
enum class UserRole {
    DRIVER, PASSENGER
}
