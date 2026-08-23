
package com.example.finalproj.navigation

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.finalproj.R
import com.example.finalproj.data.AppDatabase
import com.example.finalproj.data.Document
import com.example.finalproj.data.DocumentDao
import com.example.finalproj.home.DocumentAdapter
import com.example.finalproj.home.DocumentDetailActivity
import com.example.finalproj.home.EditDocumentActivity
import com.example.finalproj.home.UploadActivity
import com.example.finalproj.utils.MessageHelper
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MemberDocumentsFragment : Fragment() {

    private var memberId: Int = -1
    private lateinit var documentDao: DocumentDao
    private lateinit var adapter: DocumentAdapter

    private var allDocuments: List<Document> = emptyList()
    private var filteredDocuments: List<Document> = emptyList()
    
    private var currentSearchQuery = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            memberId = it.getInt("memberId")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.activity_document_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerView)
        val searchBox = view.findViewById<EditText>(R.id.searchDocuments)
        val btnFilter = view.findViewById<ImageButton>(R.id.btnFilterOptions)



        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        val db = AppDatabase.getDatabase(requireContext())
        documentDao = db.documentDao()

        adapter = DocumentAdapter(
            onClick = { document ->
                val intent = Intent(requireContext(), DocumentDetailActivity::class.java)
                intent.putExtra("documentId", document.id)
                startActivity(intent)
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

        // Load documents
        viewLifecycleOwner.lifecycleScope.launch {
            documentDao.getDocumentsForMember(memberId)
                .collectLatest { documents ->
                    allDocuments = documents
                    applyFilters()
                }
        }

        // Search functionality
        searchBox.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                currentSearchQuery = s.toString().lowercase()
                applyFilters()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Sort Button
        btnFilter.setOnClickListener {
            showSortDialog()
        }
    }

    private fun applyFilters() {
        filteredDocuments = allDocuments.filter { doc ->
            doc.title.lowercase().contains(currentSearchQuery) ||
                    (doc.ocrText?.lowercase()?.contains(currentSearchQuery) ?: false) ||
                    (doc.type.lowercase().contains(currentSearchQuery))
        }
        adapter.updateList(filteredDocuments)
    }

    private fun showSortDialog() {
        val options = arrayOf("Sort by Expiry (Near first)", "Sort by Expiry (Far first)", "Sort by Name (A-Z)")
        AlertDialog.Builder(requireContext())
            .setTitle("Sort Documents")
            .setItems(options) { _, which ->
                val sortedList = when (which) {
                    0 -> filteredDocuments.sortedBy { it.expiryDate }
                    1 -> filteredDocuments.sortedByDescending { it.expiryDate }
                    2 -> filteredDocuments.sortedBy { it.title }
                    else -> filteredDocuments
                }
                adapter.updateList(sortedList)
            }
            .show()
    }

    private fun showRenewOptions(document: Document) {
        val options = arrayOf("Manual Edit", "Upload New Document")
        AlertDialog.Builder(requireContext())
            .setTitle("Renew Document")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        val intent = Intent(requireContext(), EditDocumentActivity::class.java)
                        intent.putExtra("documentId", document.id)
                        intent.putExtra("memberId", document.memberId)
                        startActivity(intent)
                    }
                    1 -> {
                        val intent = Intent(requireContext(), UploadActivity::class.java)
                        intent.putExtra("documentId", document.id)
                        intent.putExtra("memberId", document.memberId)
                        startActivity(intent)
                    }
                }
            }
            .show()
    }

    private fun deleteDocument(document: Document) {
        viewLifecycleOwner.lifecycleScope.launch {
            documentDao.delete(document)
        }
    }

    private fun sendReminder(document: Document) {
        val formatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val message = """
Reminder from DueGuard:

${document.title}
will expire on ${formatter.format(Date(document.expiryDate))}

Please renew it soon.
""".trimIndent()

        val db = AppDatabase.getDatabase(requireContext())
        viewLifecycleOwner.lifecycleScope.launch {
            val member = db.familyMemberDao().getMemberById(document.memberId)
            if (member == null) return@launch
            when (member.notifyChannel) {
                "WhatsApp" -> MessageHelper.sendWhatsApp(requireContext(), member.phone ?: "", message)
                "Email" -> MessageHelper.sendEmail(requireContext(), member.email ?: "", message)
                "SMS" -> MessageHelper.sendSMS(requireContext(), member.phone ?: "", message)
            }
        }
    }

    private fun showRenewalHistory(document: Document) {
        viewLifecycleOwner.lifecycleScope.launch {
            val history = AppDatabase.getDatabase(requireContext())
                .renewalHistoryDao()
                .getHistoryForDocument(document.id)

            if (history.isEmpty()) {
                AlertDialog.Builder(requireContext())
                    .setTitle("Renewal History")
                    .setMessage("No renewal history found.")
                    .setPositiveButton("OK", null)
                    .show()
                return@launch
            }

            val formatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            val historyText = history.joinToString("\n\n") {
                "Renewed on: ${formatter.format(Date(it.renewalDate))}\nPrevious Expiry: ${formatter.format(Date(it.oldExpiryDate))}"
            }

            AlertDialog.Builder(requireContext())
                .setTitle("Renewal History")
                .setMessage(historyText)
                .setPositiveButton("OK", null)
                .show()
        }
    }

    companion object {
        fun newInstance(memberId: Int): MemberDocumentsFragment {
            val fragment = MemberDocumentsFragment()
            val bundle = Bundle()
            bundle.putInt("memberId", memberId)
            fragment.arguments = bundle
            return fragment
        }
    }
}
