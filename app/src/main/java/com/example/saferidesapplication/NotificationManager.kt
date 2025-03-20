package com.example.saferidesapplication

class NotificationManager {
    fun notifyPassenger(requestId: String, message: String) {
        println("Notification for Request [$requestId]: $message")
    }
}