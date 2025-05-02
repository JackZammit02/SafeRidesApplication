package com.example.backend.driver

import kotlinx.serialization.Serializable

@Serializable
data class Driver(
    val id: Int,
    var status: String
)

