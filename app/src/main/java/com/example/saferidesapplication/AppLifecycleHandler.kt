package com.example.saferidesapplication

import android.app.Application
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.example.saferidesapplication.network.ApiClient
import com.example.saferidesapplication.network.dto.ShiftUpdateRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AppLifecycleHandler(private val app: Application) : DefaultLifecycleObserver {

    override fun onStop(owner: LifecycleOwner) {
        val sharedPrefs = app.getSharedPreferences("SafeRidesPrefs", Application.MODE_PRIVATE)
        val userId = sharedPrefs.getString("userId", null)

        if (userId != null && userId.length == 6) { // assuming access codes are 6 digits
            Log.d("AppLifecycleHandler", "App moved to background. Logging off driver.")

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val response = ApiClient.apiService.updateDriverShift(userId, ShiftUpdateRequest(onShift = false))
                    if (response.isSuccessful) {
                        Log.d("AppLifecycleHandler", "Driver auto-logged off successfully.")
                    } else {
                        Log.e("AppLifecycleHandler", "Driver logoff failed: ${response.code()}")
                    }
                } catch (e: Exception) {
                    Log.e("AppLifecycleHandler", "Error logging off driver: ${e.localizedMessage}")
                }
            }
        }
    }
}
