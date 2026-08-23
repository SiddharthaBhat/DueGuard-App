package com.example.finalproj.security

import android.app.AlertDialog
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.finalproj.R
import com.example.finalproj.data.AppDatabase
import com.example.finalproj.data.IntruderLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class AlertsAdapter(
    private var logs: MutableList<IntruderLog>
) : RecyclerView.Adapter<AlertsAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textAttempt: TextView = itemView.findViewById(R.id.textAttempt)
        val textDate: TextView = itemView.findViewById(R.id.textDate)
        val textDevice: TextView = itemView.findViewById(R.id.textDevice)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_intruder_log, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val log = logs[position]

        val pinIcon = if (log.isPinned) "📌 " else ""

        holder.textAttempt.text = "${pinIcon}Failed Attempt ${log.attemptNumber}"

        val formattedDate = SimpleDateFormat(
            "dd MMM yyyy - hh:mm:ss a",
            Locale.getDefault()
        ).format(Date(log.timestamp))

        holder.textDate.text = formattedDate
        holder.textDevice.text = "Device: ${log.deviceModel}"

        // Normal click
        holder.itemView.setOnClickListener {
            val intent = Intent(holder.itemView.context, AlertDetailActivity::class.java)
            intent.putExtra("log_id", log.id)
            holder.itemView.context.startActivity(intent)
        }

        // Long press options
        holder.itemView.setOnLongClickListener {

            val options = arrayOf(
                if (log.isPinned) "Unpin Alert" else "Pin Alert",
                "Delete Permanently"
            )

            AlertDialog.Builder(holder.itemView.context)
                .setTitle("Select Option")
                .setItems(options) { _, which ->

                    when (which) {

                        // Pin / Unpin
                        0 -> {
                            val newPinStatus = !log.isPinned

                            CoroutineScope(Dispatchers.IO).launch {
                                AppDatabase.getDatabase(holder.itemView.context)
                                    .intruderLogDao()
                                    .updatePinStatus(log.id, newPinStatus)
                            }

                            log.isPinned = newPinStatus
                            notifyItemChanged(holder.adapterPosition)
                        }

                        // Delete
                        1 -> {

                            val currentPosition = holder.adapterPosition

                            if (currentPosition != RecyclerView.NO_POSITION) {

                                val logToDelete = logs[currentPosition]

                                CoroutineScope(Dispatchers.IO).launch {
                                    AppDatabase.getDatabase(holder.itemView.context)
                                        .intruderLogDao()
                                        .deleteById(logToDelete.id)
                                }

                                logs.removeAt(currentPosition)
                                notifyItemRemoved(currentPosition)
                            }
                        }
                    }
                }
                .show()

            true
        }
    }

    override fun getItemCount(): Int = logs.size
}