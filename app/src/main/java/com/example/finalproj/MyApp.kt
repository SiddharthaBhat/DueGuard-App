package com.example.finalproj

import android.app.Application
import com.example.finalproj.utils.ThemeManager

class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize Theme
        ThemeManager.applyTheme(this)
    }
}
