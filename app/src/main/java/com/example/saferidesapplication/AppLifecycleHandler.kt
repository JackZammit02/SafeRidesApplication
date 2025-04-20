// Updated AppLifecycleHandler.kt
package com.example.saferidesapplication

import android.app.Application
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.example.saferidesapplication.network.ApiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AppLifecycleHandler(private val app: Application) : DefaultLifecycleObserver {

    override fun onStop(owner: LifecycleOwner) {
        val sharedPrefs = app.getSharedPreferences("SafeRidesPrefs", Application.MODE_PRIVATE)
        val userId = sharedPrefs.getString("userId", null)

        if (userId != null && userId.length == 6) {
            Log.d("AppLifecycleHandler", "App moved to background. Logging off driver.")

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val response = ApiClient.apiService.logoutDriver(userId)
                    if (response.isSuccessful) {
                        app.getSharedPreferences("SafeRidesPrefs", Application.MODE_PRIVATE)
                            .edit()
                            .remove("userId")
                            .apply()

                        Log.d("AppLifecycleHandler", "Driver auto-logged off and cleared.")
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