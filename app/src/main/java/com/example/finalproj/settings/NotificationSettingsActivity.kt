package com.example.finalproj.settings

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.finalproj.R
import com.example.finalproj.data.AppDatabase
import com.example.finalproj.notification.AlarmScheduler
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.switchmaterial.SwitchMaterial
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NotificationSettingsActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notification_settings)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        prefs = getSharedPreferences("notification_prefs", Context.MODE_PRIVATE)
        db = AppDatabase.getDatabase(this)

        setupSwitches()
    }

    private fun setupSwitches() {
        val switch30 = findViewById<SwitchMaterial>(R.id.switch30Days)
        val switch15 = findViewById<SwitchMaterial>(R.id.switch15Days)
        val switch7 = findViewById<SwitchMaterial>(R.id.switch7Days)
        val switch1 = findViewById<SwitchMaterial>(R.id.switch1Day)
        val switchOnDay = findViewById<SwitchMaterial>(R.id.switchOnDay)

        // Load current states
        switch30.isChecked = prefs.getBoolean("notify_30_days", false)
        switch15.isChecked = prefs.getBoolean("notify_15_days", false)
        switch7.isChecked = prefs.getBoolean("notify_7_days", true)
        switch1.isChecked = prefs.getBoolean("notify_1_day", true)
        switchOnDay.isChecked = prefs.getBoolean("notify_on_day", true)

        val listener = { key: String, isChecked: Boolean ->
            prefs.edit().putBoolean(key, isChecked).apply()
            rescheduleAllAlarms()
        }

        switch30.setOnCheckedChangeListener { _, isChecked -> listener("notify_30_days", isChecked) }
        switch15.setOnCheckedChangeListener { _, isChecked -> listener("notify_15_days", isChecked) }
        switch7.setOnCheckedChangeListener { _, isChecked -> listener("notify_7_days", isChecked) }
        switch1.setOnCheckedChangeListener { _, isChecked -> listener("notify_1_day", isChecked) }
        switchOnDay.setOnCheckedChangeListener { _, isChecked -> listener("notify_on_day", isChecked) }
    }

    private fun rescheduleAllAlarms() {
        lifecycleScope.launch {
            val documents = withContext(Dispatchers.IO) {
                db.documentDao().getAllDocumentsForWorker()
            }
            documents.forEach { doc ->
                if (doc.notificationEnabled) {
                    // ✅ FIXED: Call the updated multi-window alarm scheduler
                    AlarmScheduler.scheduleAlarmsForDocument(this@NotificationSettingsActivity, doc)
                }
            }
        }
    }
}