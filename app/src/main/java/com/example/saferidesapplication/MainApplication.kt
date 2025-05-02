package com.example.saferidesapplication

import android.app.Application
import androidx.lifecycle.ProcessLifecycleOwner

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ProcessLifecycleOwner.get().lifecycle.addObserver(AppLifecycleHandler(this))
    }
}
