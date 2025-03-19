package com.example.saferidesapplication.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

// Replace with your real domain or local dev server IP:port.
// If using the emulator and your Ktor is on port 8080, use 10.0.2.2:8080.
const val BASE_URL = "http://10.0.2.2:8080"

data class RegisterRequest(
    val access_code: String
)

data class RegisterResponse(
    val message: String,
    val userId: Int
)

// Example retrofit interface for a couple endpoints
interface ApiService {

    // Register a passenger
    @POST("/users/register/passenger")
    suspend fun registerPassenger(): Response<RegisterResponse>

    // Register a driver
    @POST("/users/register/driver")
    suspend fun registerDriver(@Body request: RegisterRequest): Response<RegisterResponse>

    // Health check route
    @GET("/")
    suspend fun healthCheck(): Response<String>
}
