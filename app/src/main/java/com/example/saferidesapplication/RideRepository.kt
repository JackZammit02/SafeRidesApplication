package com.example.saferidesapplication

object RideRepository {
    private val rideRequests = mutableListOf<RideRequest>()
    private var onRideRequestsUpdated: ((List<RideRequest>) -> Unit)? = null

    // Set listener for updates
    fun setOnRideRequestsUpdatedListener(listener: (List<RideRequest>) -> Unit) {
        onRideRequestsUpdated = listener
    }

    // Get all ride requests
    fun getRideRequests(): List<RideRequest> {
        return rideRequests.toList() // Return immutable copy
    }

    // Add a new ride request and notify listener
    fun addRideRequest(rideRequest: RideRequest) {
        rideRequests.add(rideRequest)
        onRideRequestsUpdated?.invoke(rideRequests)
    }

    // Update ride status and notify listener
    fun updateRideStatus(requestId: String, newStatus: String) {
        val index = rideRequests.indexOfFirst { it.requestId == requestId }
        if (index != -1) {
            rideRequests[index] = rideRequests[index].copy(status = newStatus)
            onRideRequestsUpdated?.invoke(rideRequests)
        }
    }
}
