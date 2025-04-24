// Updated AppLifecycleHandler.kt
package com.example.saferidesapplication

import android.app.Application
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

class AppLifecycleHandler(private val app: Application) : DefaultLifecycleObserver {

    override fun onStop(owner: LifecycleOwner) {
        // No longer auto-logging out drivers when the app is backgrounded
        Log.d("AppLifecycleHandler", "App moved to background — driver remains logged in.")
    }
}
