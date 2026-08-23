package com.example.finalproj

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.finalproj.data.AppDatabase
import com.example.finalproj.data.FamilyMember
import com.example.finalproj.home.MemberDashboardActivity
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Calendar
import java.util.concurrent.TimeUnit

class MemberAdapter(
    private var members: MutableList<FamilyMember>,
    private val onDeleteRequested: (FamilyMember, Int) -> Unit
) : RecyclerView.Adapter<MemberAdapter.MemberViewHolder>() {

    class MemberViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.tvMemberName)
        val info: TextView = itemView.findViewById(R.id.tvMemberInfo)
        val photo: CircleImageView = itemView.findViewById(R.id.ivMemberPhoto)
        val card: com.google.android.material.card.MaterialCardView = itemView as com.google.android.material.card.MaterialCardView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemberViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_member, parent, false)
        return MemberViewHolder(view)
    }

    override fun onBindViewHolder(holder: MemberViewHolder, position: Int) {
        val member = members[position]
        val context = holder.itemView.context

        // Reset UI to defaults (White card, Black letters)
        holder.name.text = member.name
        holder.name.setTextColor(Color.BLACK)
        holder.name.setTypeface(null, Typeface.NORMAL)
        holder.info.setTextColor(Color.BLACK)
        holder.card.setCardBackgroundColor(Color.WHITE)
        holder.card.strokeWidth = 8
        holder.card.strokeColor = Color.parseColor("#22C55E") // Initial Green

        // Load Profile Photo
        if (!member.profileImagePath.isNullOrEmpty()) {
            val file = File(member.profileImagePath)
            if (file.exists()) {
                holder.photo.setImageURI(Uri.fromFile(file))
            } else {
                holder.photo.setImageResource(R.drawable.ic_profile_placeholder)
            }
        } else {
            holder.photo.setImageResource(R.drawable.ic_profile_placeholder)
        }

        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.getDatabase(context)
            val nowMs = System.currentTimeMillis()
            val documents = db.documentDao().getDocumentsListForMember(member.id)
            
            withContext(Dispatchers.Main) {
                if (documents.isEmpty()) {
                    holder.card.strokeColor = Color.parseColor("#22C55E") // Green for no docs
                    holder.info.text = "No documents added"
                    return@withContext
                }

                var mostUrgentStatus = 0 // 0: Green, 1: Yellow, 2: Red
                var urgentCount = 0
                var expiresToday = false

                for (doc in documents) {
                    val daysRemaining = getDaysRemaining(nowMs, doc.expiryDate)
                    val isToday = isSameDay(doc.expiryDate, nowMs)
                    
                    if (daysRemaining < 0 || isToday || daysRemaining <= 7) {
                        // Expired, Today, or within 7 days -> RED
                        mostUrgentStatus = 2
                        urgentCount++
                        if (isToday) expiresToday = true
                    } else if (daysRemaining <= 20 && mostUrgentStatus < 2) {
                        // Within 20 days (includes "within 10 days") -> YELLOW
                        mostUrgentStatus = 1
                    }
                }

                when (mostUrgentStatus) {
                    2 -> {
                        holder.card.strokeColor = Color.parseColor("#EF4444") // Red
                        holder.name.setTypeface(null, Typeface.BOLD)
                        if (expiresToday) {
                            holder.info.text = "⚠️ ATTENTION NEEDED: Document expiring today!"
                        } else {
                            val docText = if (urgentCount == 1) "document" else "documents"
                            holder.info.text = "⚠️ ATTENTION NEEDED: $urgentCount $docText expiring soon!"
                        }
                    }
                    1 -> {
                        holder.card.strokeColor = Color.parseColor("#EAB308") // Yellow
                        holder.name.setTypeface(null, Typeface.BOLD)
                        holder.info.text = "Documents expiring soon"
                    }
                    else -> {
                        holder.card.strokeColor = Color.parseColor("#22C55E") // Green
                        holder.name.setTypeface(null, Typeface.NORMAL)
                        holder.info.text = "All documents are up to date"
                    }
                }
            }
        }

        holder.itemView.setOnClickListener {
            val intent = Intent(context, MemberDashboardActivity::class.java)
            intent.putExtra("memberId", member.id)
            intent.putExtra("memberName", member.name)
            context.startActivity(intent)
        }

        holder.itemView.setOnLongClickListener {
            val options = arrayOf("Edit Member", "Delete Member")
            AlertDialog.Builder(context)
                .setTitle(member.name)
                .setItems(options) { _, which ->
                    when (which) {
                        0 -> {
                            // Edit Member
                            val intent = Intent(context, AddMemberActivity::class.java)
                            intent.putExtra("memberId", member.id)
                            context.startActivity(intent)
                        }
                        1 -> {
                            // Delete Member
                            onDeleteRequested(member, holder.adapterPosition)
                        }
                    }
                }
                .show()
            true
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
        return TimeUnit.MILLISECONDS.toDays(diff)
    }

    private fun isSameDay(ms1: Long, ms2: Long): Boolean {
        val cal1 = Calendar.getInstance().apply { timeInMillis = ms1 }
        val cal2 = Calendar.getInstance().apply { timeInMillis = ms2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    override fun getItemCount(): Int = members.size

    fun removeItem(position: Int): FamilyMember {
        val removed = members.removeAt(position)
        notifyItemRemoved(position)
        return removed
    }

    fun restoreItem(member: FamilyMember, position: Int) {
        members.add(position, member)
        notifyItemInserted(position)
    }

    fun updateData(newMembers: List<FamilyMember>) {
        members.clear()
        members.addAll(newMembers)
        notifyDataSetChanged()
    }
}
