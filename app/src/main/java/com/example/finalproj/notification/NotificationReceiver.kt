package com.example.finalproj.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        val documentId = intent.getLongExtra("doc_id", 0L)
        val title = intent.getStringExtra("doc_title") ?: "Document"
        val expiryMillis = intent.getLongExtra("expiry_date", 0L)
        val daysBefore = intent.getIntExtra("days_before", -1)

        val header = when (daysBefore) {
            7 -> "Expires in 7 Days"
            1 -> "Expires Tomorrow"
            0 -> "Expires Today"
            else -> "Document Expiry"
        }

        val date = SimpleDateFormat(
            "dd MMM yyyy",
            Locale.getDefault()
        ).format(Date(expiryMillis))

        NotificationHelper.showDocumentExpiryNotification(
            context = context,
            notificationId = (documentId * 10 + daysBefore).toInt(),
            title = "$header: $title",
            date = date,
            phone = null,
            email = null
        )
    }
}