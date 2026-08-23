
package com.example.finalproj.home

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.finalproj.R
import com.example.finalproj.data.Document
import java.text.SimpleDateFormat
import java.util.*

class DocumentAdapter(
    private val onClick: (Document) -> Unit,
    private val onMenuClick: (Document, String) -> Unit
) : RecyclerView.Adapter<DocumentAdapter.DocumentViewHolder>() {

    private var documents: List<Document> = emptyList()
    private val formatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    inner class DocumentViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {

        val card: CardView = itemView.findViewById(R.id.cardDocument)
        val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        val tvExpiry: TextView = itemView.findViewById(R.id.tvExpiry)
        val tvType: TextView = itemView.findViewById(R.id.tvOcrPreview) // Reusing tvOcrPreview for Type display
        val btnMenu: ImageButton = itemView.findViewById(R.id.btnMenu)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DocumentViewHolder {

        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_document, parent, false)

        return DocumentViewHolder(view)
    }

    override fun onBindViewHolder(holder: DocumentViewHolder, position: Int) {

        val document = documents[position]

        val now = System.currentTimeMillis()
        val daysRemaining = getDaysRemaining(now, document.expiryDate)
        val isToday = isSameDay(document.expiryDate, now)
        val expiryDate = Date(document.expiryDate)

        // Show Document Type
        holder.tvType.text = "Type: ${document.type}"
        holder.tvType.setTextColor(Color.DKGRAY)

        when {
            // RED: Expired, Expiring Today, or within 7 days
            daysRemaining < 0 || isToday || daysRemaining <= 7 -> {
                holder.card.setCardBackgroundColor(Color.parseColor("#FECACA")) 
                holder.tvTitle.text = "🔴 ${document.title}"
                if (isToday) {
                    holder.tvExpiry.text = "Expiring today"
                } else if (daysRemaining < 0) {
                    holder.tvExpiry.text = "Expired on: ${formatter.format(expiryDate)}"
                } else {
                    holder.tvExpiry.text = "Expiry: ${formatter.format(expiryDate)}"
                }
            }

            // YELLOW: Within 20 days
            daysRemaining <= 20 -> {
                holder.card.setCardBackgroundColor(Color.parseColor("#FEF9C3"))
                holder.tvTitle.text = "🟡 ${document.title}"
                holder.tvExpiry.text = "Expiry: ${formatter.format(expiryDate)}"
            }

            // WHITE: Safe
            else -> {
                holder.card.setCardBackgroundColor(Color.WHITE)
                holder.tvTitle.text = "🟢 ${document.title}"
                holder.tvExpiry.text = "Expiry: ${formatter.format(expiryDate)}"
            }
        }

        holder.itemView.setOnClickListener {
            onClick(document)
        }

        holder.btnMenu.setOnClickListener { view ->
            val popup = PopupMenu(view.context, holder.btnMenu)
            popup.menu.add("Delete")
            popup.menu.add("Renew")
            popup.menu.add("Notify")
            popup.menu.add("Renewal History")
            popup.setOnMenuItemClickListener {
                onMenuClick(document, it.title.toString())
                true
            }
            popup.show()
        }
    }

    private fun getDaysRemaining(nowMs: Long, expiryMs: Long): Long {
        val now = Calendar.getInstance().apply { timeInMillis = nowMs }
        now.set(Calendar.HOUR_OF_DAY, 0)
        now.set(Calendar.MINUTE, 0)
        now.set(Calendar.SECOND, 0)
        now.set(Calendar.MILLISECOND, 0)

        val expiry = Calendar.getInstance().apply { timeInMillis = expiryMs }
        expiry.set(Calendar.HOUR_OF_DAY, 0)
        expiry.set(Calendar.MINUTE, 0)
        expiry.set(Calendar.SECOND, 0)
        expiry.set(Calendar.MILLISECOND, 0)

        val diff = expiry.timeInMillis - now.timeInMillis
        return diff / (1000 * 60 * 60 * 24)
    }

    private fun isSameDay(ms1: Long, ms2: Long): Boolean {
        val cal1 = Calendar.getInstance().apply { timeInMillis = ms1 }
        val cal2 = Calendar.getInstance().apply { timeInMillis = ms2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    override fun getItemCount(): Int = documents.size

    fun updateList(newList: List<Document>) {
        documents = newList
        notifyDataSetChanged()
    }
}

class DocumentDiffCallback(
    private val oldList: List<Document>,
    private val newList: List<Document>
) : DiffUtil.Callback() {
    override fun getOldListSize(): Int = oldList.size
    override fun getNewListSize(): Int = newList.size
    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition].id == newList[newItemPosition].id
    }
    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition] == newList[newItemPosition]
    }
}
