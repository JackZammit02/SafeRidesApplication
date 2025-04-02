package com.example.saferidesapplication.network

import com.example.saferidesapplication.network.dto.*
import retrofit2.Response
import retrofit2.http.*

const val BASE_URL = "http://138.236.201.218:8080"

interface ApiService {

    // --- USER ROUTES ---

    @POST("/users/create")
    suspend fun createUser(@Body user: CreateUserRequest): Response<CreateUserResponse>

    @GET("/users/{id}")
    suspend fun getUser(@Path("id") id: String): Response<UserResponse>

    @POST("/users/{id}/shift")
    suspend fun updateDriverShift(
        @Path("id") id: String,
        @Body request: ShiftUpdateRequest
    ): Response<String>

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

    @POST("/access-code")
    suspend fun  verifyAccessCode(@Body code:AccessCode):Response<String>

    @POST("/rides/{rideId}/status")
    suspend fun updateRideStatus(
        @Path("rideId") rideId: String,
        @Query("status") status: String
    ): Response<Unit>



}
