package com.example.saferidesapplication

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.example.saferidesapplication.network.ApiClient
import com.example.saferidesapplication.network.dto.CancelRideRequest
import com.example.saferidesapplication.network.dto.CreateUserRequest
import com.example.saferidesapplication.network.dto.CreateUserResponse
import kotlinx.coroutines.launch
import androidx.core.content.edit
import com.example.saferidesapplication.network.dto.RideResponse
import com.google.firebase.Firebase
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.firestore

class MainActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var passengerId: String

    private val db = Firebase.firestore
    private var rideListener: ListenerRegistration? = null
    private var driverListener: ListenerRegistration? = null

    private var activeDriversCount = 0
    private var hasActiveRide = false
    private var switchingDriversCount = 0



    private lateinit var cancelRideButton: Button
    private lateinit var passengerButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Reset hasActiveRide for debug purposes
        getSharedPreferences("SafeRidesPrefs", MODE_PRIVATE).edit {
            putBoolean("hasActiveRide", false)
        }

        viewPager = findViewById(R.id.viewPager)
        viewPager.adapter = PassengerPagerAdapter(this)

        val driverButton: Button = findViewById(R.id.driverButton)
        passengerButton = findViewById(R.id.passengerButton)
        cancelRideButton = findViewById(R.id.cancelRideButton)

        val sharedPreferences = getSharedPreferences("SafeRidesPrefs", MODE_PRIVATE)
        val existingId = sharedPreferences.getString("userId", null)

        if (existingId == null) {
            registerPassenger()
            return
        } else {
            passengerId = existingId
        }

        Log.d("MainActivity", "passengerId: $passengerId")

        driverButton.setOnClickListener {
            val intent = Intent(this, AccessCodeActivity::class.java)
            startActivity(intent)
        }

        passengerButton.setOnClickListener {
            val intent = Intent(this, PassengerActivity::class.java)
            startActivity(intent)
        }

        cancelRideButton.setOnClickListener {
            cancelActiveRide()
        }

        listenToDrivers()
        listenToRides()
    }

    private fun registerPassenger(onSuccess: (() -> Unit)? = null) {
        lifecycleScope.launch {
            try {
                val requestBody = CreateUserRequest(role = "passenger", onShift = false)
                val response = ApiClient.apiService.createUser(requestBody)

                if (response.isSuccessful) {
                    val responseBody = response.body() as CreateUserResponse
                    val userId = responseBody.userId

                    getSharedPreferences("SafeRidesPrefs", MODE_PRIVATE)
                        .edit { putString("userId", userId) }

                    passengerId = userId
                    onSuccess?.invoke()

                    val intent = Intent(this@MainActivity, PassengerActivity::class.java)
                    startActivity(intent)
                } else {
                    Toast.makeText(this@MainActivity, "User creation failed", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun cancelActiveRide() {
        lifecycleScope.launch {
            try {
                val response = ApiClient.apiService.getAllRides()
                if (response.isSuccessful) {
                    val myRide = response.body()?.find {
                        it.passengerId == passengerId && it.status in listOf("queued", "assigned", "arrived")
                    }

                    if (myRide == null) {
                        Toast.makeText(this@MainActivity, "No cancellable ride found", Toast.LENGTH_SHORT).show()
                        return@launch
                    }

                    val cancelRequest = CancelRideRequest(userId = passengerId, isDriver = false)
                    val cancelResponse = ApiClient.apiService.cancelRide(myRide.rideId, cancelRequest)

                    if (cancelResponse.isSuccessful) {
                        Toast.makeText(this@MainActivity, "Ride cancelled", Toast.LENGTH_SHORT).show()
                        setRideActiveState(false)
                        cancelRideButton.visibility = Button.GONE
                        passengerButton.isEnabled = true
                        passengerButton.alpha = 1f
                    } else {
                        Toast.makeText(this@MainActivity, "Cancel failed", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setRideActiveState(isActive: Boolean) {
        getSharedPreferences("SafeRidesPrefs", MODE_PRIVATE).edit {
            putBoolean("hasActiveRide", isActive)
        }
    }

    private fun getRideActiveState(): Boolean {
        return getSharedPreferences("SafeRidesPrefs", MODE_PRIVATE)
            .getBoolean("hasActiveRide", false)
    }

    private fun listenToDrivers() {
        driverListener?.remove()
        driverListener = db.collection("active_drivers")
            .document("status")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || !snapshot.exists()) return@addSnapshotListener

                val driver1 = snapshot.get("driver1") as? Map<*, *>
                val driver2 = snapshot.get("driver2") as? Map<*, *>

                val drivers = listOfNotNull(driver1, driver2)

                activeDriversCount = drivers.count {
                    val driverId = it["driverId"] as? String
                    val switching = it["switching"] as? Boolean ?: false
                    !driverId.isNullOrBlank() && !switching
                }

                switchingDriversCount = drivers.count {
                    val driverId = it["driverId"] as? String
                    val switching = it["switching"] as? Boolean ?: false
                    !driverId.isNullOrBlank() && switching
                }

                Log.d("MainActivity", "Active drivers: $activeDriversCount, Switching drivers: $switchingDriversCount")

                updateDriverStatusText()
                updatePassengerButtonState()
            }
    }




    private fun listenToRides() {
        rideListener?.remove()
        rideListener = db.collection("rides")
            .addSnapshotListener { snapshot, error -> // Remove whereEqualTo
                if (error != null || snapshot == null) return@addSnapshotListener

                val allRides = snapshot.toObjects(RideResponse::class.java)
                    .filter { it.status == "queued" || it.status == "assigned" || it.status == "arrived" || it.status == "in_progress" }
                    .sortedWith(compareBy<RideResponse> { it.timestamp }.thenBy { it.rideId }) // ✅ Always consistent order

                val statusTextView: TextView = findViewById(R.id.passengerRideStatusTextView)

                val myRide = allRides.find { it.passengerId == passengerId }

                hasActiveRide = myRide != null

                setRideActiveState(hasActiveRide)

                if (hasActiveRide && myRide != null) {
                    val myPosition = allRides.indexOf(myRide) + 1
                    statusTextView.text = "Your ride is in position: $myPosition"
                    cancelRideButton.visibility = Button.VISIBLE
                } else {
                    statusTextView.text = "You are not in the queue"
                    cancelRideButton.visibility = Button.GONE
                }

                updatePassengerButtonState()
            }
    }


    private fun updatePassengerButtonState() {
        val canRequestRide = activeDriversCount > 0 && !hasActiveRide
        passengerButton.isEnabled = canRequestRide
        passengerButton.alpha = if (canRequestRide) 1f else 0.5f
    }

    private fun updateDriverStatusText() {
        val driverStatusTextView: TextView = findViewById(R.id.driverAvailabilityStatusTextView)

        val driverWord = if (activeDriversCount == 1) "driver" else "drivers"
        val switchingWord = if (switchingDriversCount == 1) "driver" else "drivers"

        driverStatusTextView.text = when {
            switchingDriversCount == 0 -> "$activeDriversCount $driverWord on shift"
            switchingDriversCount == 1 -> "$activeDriversCount $driverWord on shift, 1 $switchingWord switching — please expect delays"
            else -> "$activeDriversCount $driverWord on shift, $switchingDriversCount $switchingWord switching — please expect delays"
        }
    }




    override fun onDestroy() {
        super.onDestroy()
        rideListener?.remove()
        driverListener?.remove()
    }
}
