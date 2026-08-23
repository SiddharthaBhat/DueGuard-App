package com.example.finalproj

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.finalproj.data.AppDatabase
import com.example.finalproj.home.HomeActivity
import com.example.finalproj.notification.ExpiryCheckWorker
import com.example.finalproj.security.LockActivity
import com.example.finalproj.profile.CreateProfileActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val prefs = getSharedPreferences("app_settings", MODE_PRIVATE)
        when (prefs.getString("theme", "light")) {
            "dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }

        super.onCreate(savedInstanceState)

        requestNotificationPermission()
        scheduleDailyExpiryCheck()

        lifecycleScope.launch {
            val profile = withContext(Dispatchers.IO) {
                AppDatabase.getDatabase(applicationContext).userProfileDao().getProfile()
            }

            if (profile == null) {
                val intent = Intent(this@MainActivity, CreateProfileActivity::class.java).apply {
                    putExtra(CreateProfileActivity.EXTRA_MODE, CreateProfileActivity.MODE_CREATE)
                }
                startActivity(intent)
                finish()
            } else {
                val securityPrefs = getSharedPreferences("security_prefs", MODE_PRIVATE)
                val isLockEnabled = securityPrefs.getBoolean("lock_enabled", false)
                
                if (isLockEnabled) {
                    startActivity(Intent(this@MainActivity, LockActivity::class.java))
                } else {
                    startActivity(Intent(this@MainActivity, HomeActivity::class.java))
                }
                finish()
            }
        }
    }

    private fun scheduleDailyExpiryCheck() {
        val workRequest = PeriodicWorkRequestBuilder<ExpiryCheckWorker>(1, TimeUnit.DAYS).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "expiry_check_work",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001)
            }
        }
    }
}
