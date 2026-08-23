package com.example.finalproj.home

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.finalproj.R
import com.example.finalproj.data.AppDatabase
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class DocumentDetailActivity : AppCompatActivity() {

    private lateinit var tvTitle: TextView
    private lateinit var tvExpiry: TextView
    private lateinit var tvOcrText: TextView
    private lateinit var ivDocumentPreview: ImageView
    private lateinit var vStatusIndicator: View
    private lateinit var btnCopyText: MaterialButton
    private lateinit var chipType: Chip

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_document_detail)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        tvTitle = findViewById(R.id.tvTitle)
        tvExpiry = findViewById(R.id.tvExpiry)
        tvOcrText = findViewById(R.id.tvOcrText)
        ivDocumentPreview = findViewById(R.id.ivDocumentPreview)
        vStatusIndicator = findViewById(R.id.vStatusIndicator)
        btnCopyText = findViewById(R.id.btnCopyText)
        chipType = findViewById(R.id.chipType)

        val documentId = intent.getIntExtra("documentId", -1)

        if (documentId == -1) {
            finish()
            return
        }

        loadDocumentDetails(documentId)
    }

    private fun loadDocumentDetails(documentId: Int) {
        lifecycleScope.launch {
            val doc = AppDatabase.getDatabase(this@DocumentDetailActivity)
                .documentDao()
                .getDocumentById(documentId)

            doc?.let { document ->
                tvTitle.text = document.title
                chipType.text = document.type
                
                val now = System.currentTimeMillis()
                val formattedDate = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(document.expiryDate))
                
                if (document.expiryDate < now) {
                    tvExpiry.text = "Expired on: $formattedDate"
                    vStatusIndicator.setBackgroundColor(Color.parseColor("#EF4444"))
                } else {
                    tvExpiry.text = "Expiry: $formattedDate"
                    vStatusIndicator.setBackgroundColor(Color.parseColor("#22C55E"))
                }

                tvOcrText.text = if (!document.ocrText.isNullOrBlank()) document.ocrText.trim() else "No data extracted."

                if (document.filePath.isNotEmpty()) {
                    try {
                        ivDocumentPreview.setImageURI(Uri.parse(document.filePath))
                    } catch (e: Exception) {
                        ivDocumentPreview.setImageResource(android.R.drawable.ic_menu_report_image)
                    }
                }

                btnCopyText.setOnClickListener {
                    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("Extracted Info", document.ocrText ?: "")
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(this@DocumentDetailActivity, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}