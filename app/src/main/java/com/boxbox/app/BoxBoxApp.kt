package com.boxbox.app

import android.app.Application
import com.google.firebase.FirebaseApp

class BoxBoxApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
    }
}