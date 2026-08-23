package com.example.finalproj.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.finalproj.utils.MessageHelper

class NotifyMemberReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        val title = intent.getStringExtra("title") ?: return
        val date = intent.getStringExtra("date") ?: return
        val phone = intent.getStringExtra("phone")
        val email = intent.getStringExtra("email")

        val message = """
Reminder from DueGuard:

$title
will expire on $date

Please renew it soon.
""".trimIndent()

        if (!phone.isNullOrEmpty()) {
            MessageHelper.sendWhatsApp(context, phone, message)
        } else {
            MessageHelper.sendEmail(context, email, message)
        }
    }
}