package com.example.finalproj.home

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.finalproj.AddMemberActivity
import com.example.finalproj.R
import com.example.finalproj.data.AppDatabase
import com.example.finalproj.data.Document
import com.example.finalproj.data.FamilyMember
import com.example.finalproj.databinding.ActivityUploadBinding
import com.example.finalproj.notification.AlarmScheduler
import com.example.finalproj.ocr.OCRProcessor
import com.example.finalproj.ocr.OCRResult
import com.example.finalproj.security.LockActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class UploadActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUploadBinding
    private lateinit var db: AppDatabase
    private lateinit var ocrProcessor: OCRProcessor

    private var selectedImageUri: Uri? = null
    private var memberId: Int = -1
    private var allMembers: List<FamilyMember> = emptyList()

    private val galleryPicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { handleSelectedFile(it) }
    }

    private val filePicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { handleSelectedFile(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val securityPrefs = getSharedPreferences("security_prefs", MODE_PRIVATE)
        val isLocked = securityPrefs.getBoolean("lock_enabled", false)
        val isShareIntent = intent?.action == Intent.ACTION_SEND

        if (isShareIntent && isLocked && !intent.getBooleanExtra("unlocked", false)) {
            val lockIntent = Intent(this, LockActivity::class.java)
            lockIntent.putExtra("target_activity", UploadActivity::class.java.name)
            intent.extras?.let { lockIntent.putExtras(it) }
            lockIntent.action = intent.action
            lockIntent.type = intent.type
            startActivity(lockIntent)
            finish()
            return
        }

        binding = ActivityUploadBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = AppDatabase.getDatabase(this)
        ocrProcessor = OCRProcessor(this)

        memberId = intent.getIntExtra("memberId", -1)

        if (memberId != -1) {
            binding.layoutMemberSelection.visibility = View.GONE
        } else {
            binding.layoutMemberSelection.visibility = View.VISIBLE
            loadMembers()
        }

        if (isShareIntent) {
            val uri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
            uri?.let { handleSelectedFile(it) }
        }

        binding.btnUploadScan.setOnClickListener {
            showUploadOptions()
        }

        binding.btnAddManually.setOnClickListener {
            val intent = Intent(this, EditDocumentActivity::class.java)
            intent.putExtra("memberId", if (memberId != -1) memberId else getSelectedMemberId())
            startActivity(intent)
        }
        
        binding.btnAddNewMember.setOnClickListener {
            startActivity(Intent(this, AddMemberActivity::class.java))
        }
    }

    private fun showUploadOptions() {
        val options = arrayOf("Gallery (Images)", "Files (PDF & More)")
        AlertDialog.Builder(this)
            .setTitle("Upload From")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> galleryPicker.launch("image/*")
                    1 -> filePicker.launch(arrayOf("*/*"))
                }
            }
            .show()
    }

    private fun getSelectedMemberId(): Int {
        if (allMembers.isEmpty()) return -1
        return allMembers[binding.spinnerMembers.selectedItemPosition].id
    }

    private fun loadMembers() {
        lifecycleScope.launch {
            allMembers = withContext(Dispatchers.IO) { db.familyMemberDao().getAllMembersList() }
            val adapter = ArrayAdapter(this@UploadActivity, android.R.layout.simple_spinner_dropdown_item, allMembers.map { it.name })
            binding.spinnerMembers.adapter = adapter
        }
    }

    private fun handleSelectedFile(uri: Uri) {
        selectedImageUri = uri
        processOCR(uri)
    }

    private fun processOCR(uri: Uri) {
        lifecycleScope.launch {
            binding.progressBar.visibility = View.VISIBLE
            val result = ocrProcessor.processImage(uri)
            binding.progressBar.visibility = View.GONE

            if (result.expiryDate == null) {
                Toast.makeText(this@UploadActivity, "Expiry date not scanned. Please select manually.", Toast.LENGTH_LONG).show()
                showDatePickerForOCRResult(result)
            } else if (result.expiryDate < System.currentTimeMillis()) {
                AlertDialog.Builder(this@UploadActivity)
                    .setTitle("Document Expired")
                    .setMessage("The detected expiry date (${SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(result.expiryDate))}) has already passed. This document cannot be saved.")
                    .setPositiveButton("OK", null)
                    .show()
            } else {
                showConfirmationDialog(result.title, result.expiryDate, result.rawText)
            }
        }
    }

    private fun showDatePickerForOCRResult(result: OCRResult) {
        val cal = Calendar.getInstance()
        DatePickerDialog(this, { _, y, m, d ->
            val pickedCal = Calendar.getInstance().apply { set(y, m, d, 23, 59, 59) }
            val pickedMillis = pickedCal.timeInMillis
            
            if (pickedMillis < System.currentTimeMillis()) {
                Toast.makeText(this, "Cannot select a past date", Toast.LENGTH_SHORT).show()
            } else {
                showConfirmationDialog(result.title, pickedMillis, result.rawText)
            }
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun showConfirmationDialog(title: String, expiry: Long, rawText: String) {
        val view = layoutInflater.inflate(R.layout.dialog_confirm_document, null)
        val tvTitle = view.findViewById<TextView>(R.id.tvDocumentTitle)
        val tvDate = view.findViewById<TextView>(R.id.tvExpiryDate)

        tvTitle.text = title
        tvDate.text = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(expiry))

        AlertDialog.Builder(this)
            .setTitle("Confirm Extracted Details")
            .setView(view)
            .setPositiveButton("Confirm") { _, _ ->
                val finalMemberId = if (memberId != -1) memberId else getSelectedMemberId()
                val type = extractTypeFromTitle(title)
                saveDocument(finalMemberId, title, type, expiry, rawText)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun extractTypeFromTitle(title: String): String {
        return when {
            title.contains("Licence", true) -> "Licence"
            title.contains("Pollution", true) || title.contains("PUC", true) -> "Pollution"
            title.contains("FD", true) || title.contains("Deposit", true) -> "Bank FD"
            title.contains("RC", true) || title.contains("Registration", true) -> "RC"
            title.contains("Passport", true) -> "Passport"
            title.contains("PAN", true) -> "PAN"
            title.contains("Insurance", true) -> "Insurance"
            title.contains("Fog", true) -> "Fog Test"
            else -> "Other"
        }
    }

    private fun saveDocument(id: Int, title: String, type: String, expiryDate: Long, ocrText: String) {
        if (id == -1) {
            Toast.makeText(this, "Please select a member", Toast.LENGTH_SHORT).show()
            return
        }
        val uri = selectedImageUri ?: return

        lifecycleScope.launch {
            try {
                // ✅ Copy to internal storage for permanent access
                val localPath = withContext(Dispatchers.IO) { saveFileToInternal(uri) } ?: uri.toString()

                val document = Document(
                    memberId = id,
                    title = title,
                    type = type,
                    filePath = localPath,
                    fileUrl = null,
                    expiryDate = expiryDate,
                    notificationEnabled = true,
                    ocrText = ocrText,
                    cloudId = null
                )
                val newId = withContext(Dispatchers.IO) { db.documentDao().insert(document) }
                
                val insertedDocument = document.copy(id = newId.toInt())
                AlarmScheduler.scheduleAlarmsForDocument(this@UploadActivity, insertedDocument)

                Toast.makeText(this@UploadActivity, "Document Saved", Toast.LENGTH_SHORT).show()
                finish()
            } catch (e: Exception) {
                Toast.makeText(this@UploadActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun saveFileToInternal(uri: Uri): String? {
        return try {
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val fileName = "doc_${System.currentTimeMillis()}.jpg"
            val file = File(filesDir, fileName)
            val outputStream = FileOutputStream(file)
            inputStream.copyTo(outputStream)
            inputStream.close()
            outputStream.close()
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
