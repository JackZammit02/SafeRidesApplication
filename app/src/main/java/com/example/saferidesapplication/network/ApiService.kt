package com.example.saferidesapplication.network

import com.example.saferidesapplication.network.dto.*
import retrofit2.Response
import retrofit2.http.*

const val BASE_URL = "http://138.236.241.15:8080"

interface ApiService {

    // --- USER ROUTES ---

    @POST("/users/create")
    suspend fun createUser(@Body user: CreateUserRequest): Response<CreateUserResponse>

    @GET("/users/{id}")
    suspend fun getUser(@Path("id") id: String): Response<UserResponse>

    @GET("/users/drivers")
    suspend fun getAllDrivers(): Response<List<UserResponse>>

    //driver login/switch/logout
    @POST("/users/drivers/login")
    suspend fun loginDriver(@Body code: AccessCode): Response<String>

    @POST("/users/drivers/switch")
    suspend fun switchDriver(@Query("driverId") driverId: String): Response<String>

    @POST("/users/drivers/logout")
    suspend fun logoutDriver(@Query("driverId") driverId: String): Response<String>

    // --- RIDE ROUTES ---

    @POST("/rides/request")
    suspend fun requestRide(@Body ride: RideRequest): Response<RideResponse>

    @GET("/rides/all")
    suspend fun getAllRides(): Response<List<RideResponse>>

    @POST("/rides/assign")
    suspend fun assignNextRide(@Query("driverId") driverId: String): Response<RideResponse>

    @POST("/rides/{rideId}/cancel")
    suspend fun cancelRide(
        @Path("rideId") rideId: String,
        @Body request: CancelRideRequest
    ): Response<String>

    @POST("/rides/{rideId}/status")
    suspend fun updateRideStatus(
        @Path("rideId") rideId: String,
        @Query("status") status: String
    ): Response<Unit>
}
