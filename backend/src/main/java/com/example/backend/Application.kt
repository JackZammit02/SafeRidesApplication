package com.example.backend

import com.example.backend.driver.DriverStorage
import com.example.backend.user.User
import com.example.backend.user.UserRole
import com.example.backend.user.UserStorage
import com.example.backend.ride.Ride
import com.example.backend.ride.RideRequest
import com.example.backend.ride.RideStorage
import com.example.backend.RegisterResponse
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*


fun Application.module() {
    install(ContentNegotiation) {
        json()
    }

    routing {
        // Health check route
        get("/") {
            call.respondText("Ktor server is running!")
        }

        // Register as a Passenger
        post("/users/register/passenger") {
            val userId = UserStorage.generateUserId()
            val newUser = User(id = userId, role = UserRole.PASSENGER)
            UserStorage.addUser(newUser)

            val response = RegisterResponse(
                message = "Passenger registered",
                userId = userId
            )
            call.respond(HttpStatusCode.Created, response)
        }

        // Register as a Driver (Requires Access Code)
        post("/users/register/driver") {
            // we still read the request body as a Map so we can check the "access_code"
            val request = call.receive<Map<String, String>>()
            val code = request["access_code"]

            if (code != "DRIVER123") {  // Example access code
                call.respond(HttpStatusCode.Forbidden, "Invalid driver access code")
                return@post
            }

            val userId = UserStorage.generateUserId()
            val newUser = User(id = userId, role = UserRole.DRIVER)
            UserStorage.addUser(newUser)

            // Now use your data class for a properly typed JSON response:
            val response = RegisterResponse(
                message = "Driver registered",
                userId = userId
            )
            call.respond(HttpStatusCode.Created, response)
        }

        // Driver login
        post("/drivers/login") {
            val driverId = call.receive<Map<String, Int>>()["id"]
            val driver = driverId?.let { UserStorage.findUserById(it) }

            if (driver == null || driver.role != UserRole.DRIVER) {
                call.respond(HttpStatusCode.Forbidden, "Only registered drivers can log in")
                return@post
            }

            val driverRecord = DriverStorage.drivers.find { it.id == driverId }
            if (driverRecord != null) {
                driverRecord.status = "ACTIVE"
                call.respond(HttpStatusCode.OK, "Driver $driverId is now ACTIVE")
            } else {
                call.respond(HttpStatusCode.NotFound, "Driver record not found")
            }
        }

        // Driver logout
        post("/drivers/logout") {
            val driverId = call.receive<Map<String, Int>>()["id"]
            val driver = driverId?.let { UserStorage.findUserById(it) }

            if (driver == null || driver.role != UserRole.DRIVER) {
                call.respond(HttpStatusCode.Forbidden, "Only registered drivers can log out")
                return@post
            }

            val driverRecord = DriverStorage.drivers.find { it.id == driverId }
            if (driverRecord != null) {
                driverRecord.status = "OFFLINE"
                call.respond(HttpStatusCode.OK, "Driver $driverId is now OFFLINE")
            } else {
                call.respond(HttpStatusCode.NotFound, "Driver record not found")
            }
        }

        // Driver switching shift
        post("/drivers/switch") {
            val driverId = call.receive<Map<String, Int>>()["id"]
            val driver = driverId?.let { UserStorage.findUserById(it) }

            if (driver == null || driver.role != UserRole.DRIVER) {
                call.respond(HttpStatusCode.Forbidden, "Only registered drivers can switch shifts")
                return@post
            }

            val driverRecord = DriverStorage.drivers.find { it.id == driverId }
            if (driverRecord != null) {
                driverRecord.status = "SWITCHING"
                call.respond(HttpStatusCode.OK, "Driver $driverId is now SWITCHING")
            } else {
                call.respond(HttpStatusCode.NotFound, "Driver record not found")
            }
        }

        // Create a new ride request
        post("/rides/request") {
            val request = call.receive<RideRequest>()
            val passenger = UserStorage.findUserById(request.passengerId)

            if (passenger == null || passenger.role != UserRole.PASSENGER) {
                call.respond(HttpStatusCode.Forbidden, "Only registered passengers can request rides")
                return@post
            }

            val availableDriver = DriverStorage.drivers.find { it.status == "ACTIVE" || it.status == "SWITCHING" }
            if (availableDriver != null) {
                RideStorage.rides.add(Ride(request.passengerId, availableDriver.id))
                call.respond(HttpStatusCode.Created, "Ride request added to queue.")
            } else {
                call.respond(HttpStatusCode.ServiceUnavailable, "No drivers available")
            }
        }

        // Cancel a ride
        post("/rides/cancel") {
            val request = call.receive<Map<String, Int>>()
            val passengerId = request["passengerId"]
            val passenger = passengerId?.let { UserStorage.findUserById(it) }

            if (passenger == null || passenger.role != UserRole.PASSENGER) {
                call.respond(HttpStatusCode.Forbidden, "Only registered passengers can cancel rides")
                return@post
            }

            val ride = RideStorage.rides.find { it.passengerId == passengerId }
            if (ride != null) {
                RideStorage.rides.remove(ride)
                call.respond(HttpStatusCode.OK, "Ride request canceled")
            } else {
                call.respond(HttpStatusCode.NotFound, "No ride request found")
            }
        }
    }
}

fun main() {
    embeddedServer(Netty, port = 8080, module = Application::module).start(wait = true)
}
