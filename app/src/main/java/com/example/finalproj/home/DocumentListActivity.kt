
package com.example.finalproj.home

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.finalproj.R
import com.example.finalproj.data.AppDatabase
import com.example.finalproj.data.Document
import com.example.finalproj.data.DocumentDao
import com.example.finalproj.data.FamilyMember
import com.example.finalproj.utils.MessageHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class DocumentListActivity : AppCompatActivity() {

    private lateinit var documentDao: DocumentDao
    private lateinit var adapter: DocumentAdapter
    private var memberId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_document_list)

        memberId = intent.getIntExtra("memberId", -1)

        if (memberId == -1) {
            Toast.makeText(this, "Invalid member", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val db = AppDatabase.getDatabase(this)
        documentDao = db.documentDao()

        adapter = DocumentAdapter(

            onClick = { document ->
                openDetailScreen(document)
            },

            onMenuClick = { document, action ->

                when (action) {

                    "Delete" -> deleteDocument(document)

                    "Renew" -> showRenewOptions(document)

                    "Notify" -> sendReminder(document)

                    "Renewal History" -> showRenewalHistory(document)
                }
            }
        )

        recyclerView.adapter = adapter

        lifecycleScope.launch {
            documentDao.getDocumentsForMember(memberId)
                .collectLatest { documents ->
                    adapter.updateList(documents)
                }
        }
    }

    private fun openDetailScreen(document: Document) {

        val intent = Intent(this, DocumentDetailActivity::class.java)
        intent.putExtra("documentId", document.id)
        startActivity(intent)
    }

    private fun showRenewOptions(document: Document) {

        val options = arrayOf("Manual Edit", "Upload New Document")

        AlertDialog.Builder(this)
            .setTitle("Renew Document")
            .setItems(options) { _, which ->

                when (which) {

                    0 -> {
                        val intent = Intent(this, EditDocumentActivity::class.java)
                        intent.putExtra("documentId", document.id)
                        intent.putExtra("memberId", document.memberId)
                        startActivity(intent)
                    }

                    1 -> {
                        val intent = Intent(this, UploadActivity::class.java)
                        intent.putExtra("memberId", document.memberId)
                        startActivity(intent)
                    }
                }
            }
            .show()
    }

    private fun deleteDocument(document: Document) {

        lifecycleScope.launch(Dispatchers.IO) {

            documentDao.delete(document)

            withContext(Dispatchers.Main) {

                Toast.makeText(
                    this@DocumentListActivity,
                    "Document deleted",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun sendReminder(document: Document) {

        val formatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

        val message = """
Reminder from DueGuard:

Your ${document.title}
will expire on ${formatter.format(Date(document.expiryDate))}

Please renew it soon.
""".trimIndent()

        val db = AppDatabase.getDatabase(this)

        lifecycleScope.launch {

            val member = withContext(Dispatchers.IO) {
                db.familyMemberDao().getMemberById(document.memberId)
            }

            if (member == null) {
                Toast.makeText(this@DocumentListActivity, "Member not found", Toast.LENGTH_SHORT).show()
                return@launch
            }

            if (member.notifyChannel.isNullOrEmpty()) {
                // If no channel is selected, ask the user
                showChannelSelectionDialog(member, message)
            } else {
                performNotification(member, message)
            }
        }
    }

    private fun showChannelSelectionDialog(member: FamilyMember, message: String) {
        val channels = arrayOf("WhatsApp", "Email", "SMS")
        AlertDialog.Builder(this)
            .setTitle("Select Notification Channel")
            .setItems(channels) { _, which ->
                val selectedChannel = channels[which]
                
                // Update member in database
                lifecycleScope.launch(Dispatchers.IO) {
                    val updatedMember = member.copy(notifyChannel = selectedChannel)
                    AppDatabase.getDatabase(this@DocumentListActivity).familyMemberDao().insert(updatedMember)
                    
                    withContext(Dispatchers.Main) {
                        performNotification(updatedMember, message)
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performNotification(member: FamilyMember, message: String) {
        when (member.notifyChannel?.lowercase()) {
            "whatsapp" -> {
                if (member.phone.isNullOrEmpty()) {
                    Toast.makeText(this, "Phone number missing", Toast.LENGTH_SHORT).show()
                } else {
                    MessageHelper.sendWhatsApp(this, member.phone, message)
                }
            }
            "email" -> {
                if (member.email.isNullOrEmpty()) {
                    Toast.makeText(this, "Email address missing", Toast.LENGTH_SHORT).show()
                } else {
                    MessageHelper.sendEmail(this, member.email, message)
                }
            }
            "sms" -> {
                if (member.phone.isNullOrEmpty()) {
                    Toast.makeText(this, "Phone number missing", Toast.LENGTH_SHORT).show()
                } else {
                    MessageHelper.sendSMS(this, member.phone, message)
                }
            }
            else -> {
                Toast.makeText(this, "Invalid notification channel", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showRenewalHistory(document: Document) {

        lifecycleScope.launch {

            val history = AppDatabase
                .getDatabase(this@DocumentListActivity)
                .renewalHistoryDao()
                .getHistoryForDocument(document.id)

            if (history.isEmpty()) {

                Toast.makeText(
                    this@DocumentListActivity,
                    "No renewal history found",
                    Toast.LENGTH_SHORT
                ).show()

                return@launch
            }

            val formatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

            val historyText = history.joinToString("\n\n") {

                "Renewed on: ${formatter.format(Date(it.renewalDate))}\nPrevious Expiry: ${
                    formatter.format(Date(it.oldExpiryDate))
                }"
            }

            AlertDialog.Builder(this@DocumentListActivity)
                .setTitle("Renewal History")
                .setMessage(historyText)
                .setPositiveButton("OK", null)
                .show()
        }
    }
}
