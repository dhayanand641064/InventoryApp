package com.example.shan_inventory

import android.app.Application
import com.google.firebase.FirebaseApp

class PartsInventoryApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
    }
}

