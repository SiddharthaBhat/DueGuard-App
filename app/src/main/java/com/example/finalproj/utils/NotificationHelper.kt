package com.example.finalproj.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

object NotificationHelper {

    fun sendWhatsApp(context: Context, phone: String?, message: String) {

        if (phone.isNullOrEmpty()) {
            Toast.makeText(context, "Phone number not available", Toast.LENGTH_SHORT).show()
            return
        }

        val uri = Uri.parse(
            "https://wa.me/$phone?text=" +
                    Uri.encode(message)
        )

        val intent = Intent(Intent.ACTION_VIEW, uri)

        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            sendSMS(context, phone, message)
        }
    }

    fun sendSMS(context: Context, phone: String?, message: String) {

        if (phone.isNullOrEmpty()) {
            Toast.makeText(context, "Phone number not available", Toast.LENGTH_SHORT).show()
            return
        }

        val intent = Intent(Intent.ACTION_SENDTO)
        intent.data = Uri.parse("smsto:$phone")
        intent.putExtra("sms_body", message)

        context.startActivity(intent)
    }

    fun sendEmail(context: Context, email: String?, message: String) {

        if (email.isNullOrEmpty()) {
            Toast.makeText(context, "Email not available", Toast.LENGTH_SHORT).show()
            return
        }

        val intent = Intent(Intent.ACTION_SENDTO)

        intent.data = Uri.parse("mailto:$email")
        intent.putExtra(Intent.EXTRA_SUBJECT, "DueGuard Reminder")
        intent.putExtra(Intent.EXTRA_TEXT, message)

        context.startActivity(intent)
    }
}