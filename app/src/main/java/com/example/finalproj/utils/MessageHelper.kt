package com.example.finalproj.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import java.net.URLEncoder

object MessageHelper {

    fun sendWhatsApp(context: Context, phone: String?, message: String) {

        if (phone.isNullOrEmpty()) {
            Toast.makeText(context, "Phone number not available", Toast.LENGTH_SHORT).show()
            return
        }

        try {

            // Clean phone number
            var number = phone.replace("[^0-9]".toRegex(), "")

            // Add India code if only 10 digits
            if (number.length == 10) {
                number = "91$number"
            }

            val encodedMessage = URLEncoder.encode(message, "UTF-8")

            val uri = Uri.parse("https://wa.me/$number?text=$encodedMessage")

            val intent = Intent(Intent.ACTION_VIEW, uri)

            context.startActivity(intent)

        } catch (e: Exception) {

            Toast.makeText(context, "WhatsApp not installed. Using SMS.", Toast.LENGTH_SHORT).show()
            sendSMS(context, phone, message)
        }
    }

    fun sendSMS(context: Context, phone: String?, message: String) {

        if (phone.isNullOrEmpty()) {
            Toast.makeText(context, "Phone number not available", Toast.LENGTH_SHORT).show()
            return
        }

        try {

            val uri = Uri.parse("smsto:$phone")

            val intent = Intent(Intent.ACTION_SENDTO, uri).apply {
                putExtra("sms_body", message)
            }

            context.startActivity(intent)

        } catch (e: Exception) {

            Toast.makeText(context, "SMS app not available", Toast.LENGTH_SHORT).show()
        }
    }

    fun sendEmail(context: Context, email: String?, message: String) {

        if (email.isNullOrEmpty()) {
            Toast.makeText(context, "Email not available", Toast.LENGTH_SHORT).show()
            return
        }

        try {

            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:$email")
                putExtra(Intent.EXTRA_SUBJECT, "DueGuard Reminder")
                putExtra(Intent.EXTRA_TEXT, message)
            }

            context.startActivity(Intent.createChooser(intent, "Send Email"))

        } catch (e: Exception) {

            Toast.makeText(context, "No email app installed", Toast.LENGTH_SHORT).show()
        }
    }
}