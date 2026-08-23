package com.example.finalproj.settings

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.finalproj.R
import com.example.finalproj.appinfo.AppInfoActivity
import com.example.finalproj.data.AppDatabase
import com.example.finalproj.profile.ProfileActivity
import com.example.finalproj.security.SecurityActivity

class SettingsActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        supportActionBar?.title = "Settings"

        prefs = getSharedPreferences("app_settings", MODE_PRIVATE)
        db = AppDatabase.getDatabase(this)

        val layoutProfile = findViewById<View>(R.id.layoutProfile)
        val layoutNotifications = findViewById<View>(R.id.layoutNotifications)
        val layoutAppInfo = findViewById<View>(R.id.layoutAppInfo)
        val layoutSecurity = findViewById<View>(R.id.layoutSecurity)
        val layoutLogout = findViewById<View>(R.id.layoutLogout)
        val layoutSync = findViewById<View>(R.id.layoutSync)

        // 🚀 FULL LOCAL: Hide Cloud Sync and Logout permanently
        layoutLogout.visibility = View.GONE
        layoutSync.visibility = View.GONE

        layoutProfile.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        layoutNotifications.setOnClickListener {
            startActivity(Intent(this, NotificationSettingsActivity::class.java))
        }

        layoutAppInfo.setOnClickListener {
            startActivity(Intent(this, AppInfoActivity::class.java))
        }

        layoutSecurity.setOnClickListener {
            startActivity(Intent(this, SecurityActivity::class.java))
        }
    }
}
