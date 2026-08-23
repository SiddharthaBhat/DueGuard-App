package com.example.finalproj.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.finalproj.data.Document
import java.util.Calendar

object AlarmScheduler {

    fun scheduleAlarmsForDocument(context: Context, document: Document) {
        if (!document.notificationEnabled) {
            cancelAlarmsForDocument(context, document.id.toLong())
            return
        }

        val prefs = context.getSharedPreferences("notification_prefs", Context.MODE_PRIVATE)
        val windows = mutableListOf<Int>()
        if (prefs.getBoolean("notify_30_days", false)) windows.add(30)
        if (prefs.getBoolean("notify_15_days", false)) windows.add(15)
        if (prefs.getBoolean("notify_7_days", true)) windows.add(7)
        if (prefs.getBoolean("notify_1_day", true)) windows.add(1)
        if (prefs.getBoolean("notify_on_day", true)) windows.add(0)

        for (daysBefore in windows) {
            val triggerTime = calculateTriggerTime(document.expiryDate, daysBefore)
            if (triggerTime > System.currentTimeMillis()) {
                scheduleSingleAlarm(context, document, triggerTime, daysBefore)
            }
        }
    }

    private fun scheduleSingleAlarm(context: Context, document: Document, triggerTime: Long, daysBefore: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("doc_id", document.id.toLong())
            putExtra("doc_title", document.title)
            putExtra("expiry_date", document.expiryDate)
            putExtra("days_before", daysBefore)
        }

        // Unique request code for each document + window combination
        val requestCode = (document.id * 100) + daysBefore

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // ✅ Using setAndAllowWhileIdle instead of setExact.
        // This is much safer for Play Store approval and still works in Doze mode.
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerTime,
            pendingIntent
        )
    }

    private fun calculateTriggerTime(expiryMillis: Long, daysBefore: Int): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = expiryMillis
            add(Calendar.DAY_OF_YEAR, -daysBefore)
            // Set alert time to 9:00 AM
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }
        return calendar.timeInMillis
    }

    fun cancelAlarmsForDocument(context: Context, documentId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val windows = listOf(30, 15, 7, 1, 0)

        for (daysBefore in windows) {
            val requestCode = (documentId.toInt() * 100) + daysBefore
            val intent = Intent(context, NotificationReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        }
    }
}
