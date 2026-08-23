
package com.example.finalproj.home

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.finalproj.R
import com.example.finalproj.data.AppDatabase
import com.example.finalproj.data.Document
import com.example.finalproj.data.RenewalHistory
import com.example.finalproj.notification.AlarmScheduler
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class EditDocumentActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase

    private lateinit var etTitle: TextInputEditText
    private lateinit var btnDate: MaterialButton
    private lateinit var etOcrText: TextInputEditText
    private lateinit var swNotification: SwitchMaterial
    private lateinit var btnSave: MaterialButton
    private lateinit var btnDelete: MaterialButton

    private var documentId: Int = 0
    private var expiryMillis = 0L
    private var memberId: Int = -1
    private var filePath: String = ""
    private var docType: String = "Other"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_edit_document)

        db = AppDatabase.getDatabase(this)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        etTitle = findViewById(R.id.etTitle)
        btnDate = findViewById(R.id.btnDate)
        etOcrText = findViewById(R.id.etOcrText)
        swNotification = findViewById(R.id.swNotification)
        btnSave = findViewById(R.id.btnSave)
        btnDelete = findViewById(R.id.btnDelete)

        documentId = intent.getIntExtra("documentId", 0)
        memberId = intent.getIntExtra("memberId", -1)

        if (documentId != 0) {
            btnDelete.visibility = View.VISIBLE
            loadDocument()
        }

        btnDate.setOnClickListener { showDatePicker() }
        btnSave.setOnClickListener { saveDocument() }
        btnDelete.setOnClickListener { deleteDocument() }
    }

    private fun loadDocument() {
        lifecycleScope.launch {
            val doc = db.documentDao().getDocumentById(documentId) ?: return@launch
            etTitle.setText(doc.title)
            etOcrText.setText(doc.ocrText)
            swNotification.isChecked = doc.notificationEnabled
            expiryMillis = doc.expiryDate
            btnDate.text = formatDate(expiryMillis)
            memberId = doc.memberId
            filePath = doc.filePath
            docType = doc.type
        }
    }

    private fun showDatePicker() {
        val cal = Calendar.getInstance()
        if (expiryMillis > 0) cal.timeInMillis = expiryMillis

        DatePickerDialog(
            this,
            { _, y, m, d ->
                cal.set(y, m, d, 23, 59, 59)
                expiryMillis = cal.timeInMillis
                btnDate.text = formatDate(expiryMillis)
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun saveDocument() {
        val title = etTitle.text.toString().trim()

        if (title.isEmpty()) {
            etTitle.error = "Title required"
            return
        }
        if (expiryMillis == 0L) {
            Toast.makeText(this, "Select expiry date", Toast.LENGTH_SHORT).show()
            return
        }
        if (memberId == -1) {
            Toast.makeText(this, "Invalid member", Toast.LENGTH_SHORT).show()
            return
        }

        // ✅ VALIDATE EXPIRY DATE
        if (expiryMillis < System.currentTimeMillis()) {
            AlertDialog.Builder(this)
                .setTitle("Document Expired")
                .setMessage("The selected expiry date has already passed. This document cannot be saved.")
                .setPositiveButton("OK", null)
                .show()
            return
        }

        lifecycleScope.launch {
            val oldDoc = if (documentId != 0) db.documentDao().getDocumentById(documentId) else null

            var doc = Document(
                id = documentId,
                memberId = memberId,
                title = title,
                type = docType,
                filePath = filePath,
                expiryDate = expiryMillis,
                notificationEnabled = swNotification.isChecked,
                ocrText = etOcrText.text.toString()
            )

            if (documentId == 0) {
                val newId = db.documentDao().insert(doc).toInt()
                doc = doc.copy(id = newId)
            } else {
                if (oldDoc != null && oldDoc.expiryDate != expiryMillis) {
                    db.renewalHistoryDao().insert(
                        RenewalHistory(
                            documentId = documentId,
                            oldExpiryDate = oldDoc.expiryDate,
                            renewalDate = System.currentTimeMillis()
                        )
                    )
                }
                db.documentDao().update(doc)
            }

            // Reschedule alarms
            AlarmScheduler.cancelAlarmsForDocument(this@EditDocumentActivity, doc.id.toLong())
            if (doc.notificationEnabled) {
                AlarmScheduler.scheduleAlarmsForDocument(this@EditDocumentActivity, doc)
            }

            Toast.makeText(this@EditDocumentActivity, "Saved successfully", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun deleteDocument() {
        lifecycleScope.launch {
            AlarmScheduler.cancelAlarmsForDocument(this@EditDocumentActivity, documentId.toLong())
            db.documentDao().deleteById(documentId)
            Toast.makeText(this@EditDocumentActivity, "Deleted", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun formatDate(ms: Long): String {
        return SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(ms))
    }
}
