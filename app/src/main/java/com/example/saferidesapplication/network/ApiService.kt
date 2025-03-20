package com.example.saferidesapplication.network

import com.example.backend.RegisterResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

const val BASE_URL = "http://10.0.2.2:8080"

// You could define request classes:
data class RegisterRequest(val access_code: String)
data class DriverIdRequest(val id: Int)
data class PassengerIdRequest(val passengerId: Int)

// Reuse your existing classes for responses, e.g. RegisterResponse if needed

interface ApiService {

    // Health check route
    @GET("/")
    suspend fun healthCheck(): Response<String>

    // Register a passenger
    @POST("/users/register/passenger")
    suspend fun registerPassenger(): Response<RegisterResponse>

    // Register a driver
    @POST("/users/register/driver")
    suspend fun registerDriver(@Body request: RegisterRequest): Response<RegisterResponse>

    // Driver login
    @POST("/drivers/login")
    suspend fun driverLogin(@Body body: DriverIdRequest): Response<String>

    // Driver logout
    @POST("/drivers/logout")
    suspend fun driverLogout(@Body body: DriverIdRequest): Response<String>

    // Driver switch
    @POST("/drivers/switch")
    suspend fun driverSwitch(@Body body: DriverIdRequest): Response<String>

    // Create a new ride request
    @POST("/rides/request")
    suspend fun requestRide(@Body body: PassengerIdRequest): Response<String>

    // Cancel a ride
    @POST("/rides/cancel")
    suspend fun cancelRide(@Body body: PassengerIdRequest): Response<String>
}

