package com.example.finalproj.home

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.finalproj.MemberListActivity
import com.example.finalproj.R
import com.example.finalproj.data.AppDatabase
import com.example.finalproj.security.AlertsActivity
import com.example.finalproj.settings.SettingsActivity
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var tvMembersCount: TextView
    private lateinit var tvUpcomingCount: TextView
    private lateinit var tvIntruderStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 🚀 LOCAL ONLY: Login and Auth checks removed.
        setContentView(R.layout.activity_home)

        db = AppDatabase.getDatabase(this)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(true)

        tvMembersCount = findViewById(R.id.tvMembersCount)
        tvUpcomingCount = findViewById(R.id.tvUpcomingCount)
        tvIntruderStatus = findViewById(R.id.tvIntruderStatus)

        val btnMembers = findViewById<View>(R.id.btnMembers)
        val btnAlerts = findViewById<View>(R.id.btnAlerts)
        val btnSettings = findViewById<View>(R.id.btnSettings)

        btnMembers.setOnClickListener {
            startActivity(Intent(this, MemberListActivity::class.java))
        }

        btnAlerts.setOnClickListener {
            startActivity(Intent(this, AlertsActivity::class.java))
        }

        btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        loadDashboardStats()
        updateIntruderStatus()
    }

    private fun updateIntruderStatus() {
        val securityPrefs = getSharedPreferences("security_prefs", MODE_PRIVATE)
        val isEnabled = securityPrefs.getBoolean("intruder_enabled", false)
        
        if (isEnabled) {
            tvIntruderStatus.text = "System Active"
            tvIntruderStatus.setTextColor(android.graphics.Color.parseColor("#16A34A"))
        } else {
            tvIntruderStatus.text = "System OFF"
            tvIntruderStatus.setTextColor(android.graphics.Color.parseColor("#DC2626"))
        }
    }

    override fun onResume() {
        super.onResume()
        loadDashboardStats()
        updateIntruderStatus()
    }

    private fun loadDashboardStats() {
        lifecycleScope.launch {
            val memberCount = withContext(Dispatchers.IO) {
                db.familyMemberDao().getMemberCount()
            }
            val upcomingCount = withContext(Dispatchers.IO) {
                val now = System.currentTimeMillis()
                val sevenDaysLater = now + (7L * 24 * 60 * 60 * 1000)
                db.documentDao().getUpcomingRenewalsCount(now, sevenDaysLater)
            }
            tvMembersCount.text = memberCount.toString()
            tvUpcomingCount.text = upcomingCount.toString()
        }
    }
}
