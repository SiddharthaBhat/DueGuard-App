package com.example.finalproj.utils

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import com.example.finalproj.R

class ChannelSpinnerAdapter(
    context: Context,
    private val channels: List<NotificationChannel>
) : ArrayAdapter<NotificationChannel>(context, 0, channels) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        return createView(position, convertView, parent)
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        return createView(position, convertView, parent)
    }

    private fun createView(position: Int, convertView: View?, parent: ViewGroup): View {

        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_notification_channel, parent, false)

        val icon = view.findViewById<ImageView>(R.id.icon)
        val text = view.findViewById<TextView>(R.id.text)

        val channel = channels[position]

        icon.setImageResource(channel.icon)
        text.text = channel.name

        return view
    }
}