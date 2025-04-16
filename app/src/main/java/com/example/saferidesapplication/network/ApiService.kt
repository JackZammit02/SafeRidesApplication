package com.example.saferidesapplication.network

import com.example.saferidesapplication.network.dto.*
import retrofit2.Response
import retrofit2.http.*

const val BASE_URL = "http://10.0.2.2:8080"

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

    @POST("/users/verify-access-code")
    suspend fun verifyAccessCode(@Body code: AccessCode): Response<String>
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

    @GET("users/drivers")
    suspend fun getAllDrivers(): Response<List<UserResponse>>

    @POST("/rides/{rideId}/status")
    suspend fun updateRideStatus(
        @Path("rideId") rideId: String,
        @Query("status") status: String
    ): Response<Unit>

    @POST("/rides/driver-switch")
    suspend fun setDriverSwitching(@Body request: DriverSwitchRequest): Response<String>

    @GET("/rides/driver-switch/status")
    suspend fun getDriverSwitchingStatus(): Response<DriverSwitchStatusResponse>

}
