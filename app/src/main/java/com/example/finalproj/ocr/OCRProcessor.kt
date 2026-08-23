package com.example.finalproj.ocr

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.*

data class OCRResult(
    val rawText: String,
    val title: String,
    val expiryDate: Long?
)

class OCRProcessor(private val context: Context) {

    suspend fun processImage(uri: Uri): OCRResult = withContext(Dispatchers.IO) {
        try {
            val image = InputImage.fromFilePath(context, uri)
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            val result = recognizer.process(image).await()
            val rawText = result.text
            
            // Clean common OCR mistakes (e.g., O -> 0, I -> 1 in dates)
            val cleanedText = cleanOcrMistakes(rawText)
            
            OCRResult(
                rawText = rawText,
                title = generateSmartTitle(cleanedText),
                expiryDate = extractExpiryDate(cleanedText)
            )
        } catch (e: Exception) {
            OCRResult(rawText = "OCR failed: ${e.message}", title = "Renewable Document", expiryDate = null)
        }
    }

    private fun cleanOcrMistakes(text: String): String {
        return text.replace(Regex("(?<=\\d)O(?=\\d)"), "0")
            .replace(Regex("(?<=\\d)I(?=\\d)"), "1")
            .replace(Regex("(?<=\\d)l(?=\\d)"), "1")
    }

    private fun generateSmartTitle(text: String): String {
        val lower = text.lowercase()
        return when {
            lower.contains("driving") || lower.contains("licence") || lower.contains("license") || lower.contains("dl") -> "Driving Licence"
            lower.contains("pollution") || lower.contains("puc") || lower.contains("smoke") || lower.contains("emission") -> {
                val vehicleNo = Regex("[A-Z]{2}\\s?[0-9]{1,2}\\s?[A-Z]{1,2}\\s?[0-9]{4}").find(text.uppercase())?.value ?: ""
                "Pollution Certificate $vehicleNo".trim()
            }
            lower.contains("fog") || lower.contains("test") || lower.contains("lamp") -> "Fog Test Report"
            lower.contains("passport") -> "Passport"
            lower.contains("pan") && (lower.contains("income") || lower.contains("tax")) -> {
                val name = extractName(text)
                "PAN Card - $name".trim()
            }
            lower.contains("fd") || lower.contains("fixed deposit") || lower.contains("bank") || lower.contains("maturity") -> {
                val name = extractName(text)
                "Bank FD - $name".trim()
            }
            lower.contains("insurance") || lower.contains("policy") -> "Insurance Policy"
            lower.contains("rc") || lower.contains("registration") || lower.contains("certificate") -> "Vehicle RC"
            else -> "Renewable Document"
        }
    }

    private fun extractName(text: String): String {
        val lines = text.lines().filter { it.isNotBlank() }
        for (line in lines) {
            val uLine = line.uppercase()
            if (uLine.contains("NAME") || uLine.contains("HOLDER") || uLine.contains("CUSTOMER")) {
                return line.split(":").last().trim()
            }
        }
        return ""
    }

    private fun extractExpiryDate(text: String): Long? {
        val now = System.currentTimeMillis()
        val lower = text.lowercase()
        val candidates = mutableListOf<Long>()

        // Keywords that usually precede an expiry date
        val expiryKeywords = listOf("exp", "valid", "until", "ends", "expiry", "thru", "upto")
        
        // 1. Full dates (dd/mm/yyyy, dd-mm-yyyy, dd.mm.yyyy) with flexible separators
        val fullDateRegex = Regex("\\b(\\d{1,2})[\\s./-]*(\\d{1,2})[\\s./-]*(\\d{4}|\\d{2})\\b")
        fullDateRegex.findAll(text).forEach { m ->
            val d1 = m.groupValues[1].toInt()
            val d2 = m.groupValues[2].toInt()
            var yearStr = m.groupValues[3]
            var year = if (yearStr.length == 2) 2000 + yearStr.toInt() else yearStr.toInt()
            
            // Check context for keywords
            val startIndex = maxOf(0, m.range.first - 20)
            val context = lower.substring(startIndex, m.range.first)
            val hasKeyword = expiryKeywords.any { context.contains(it) }

            if (d1 in 1..31 && d2 in 1..12) {
                val date = buildDate(year, d2, d1)
                if (hasKeyword) return date // High confidence
                candidates.add(date)
            }
            if (d2 in 1..31 && d1 in 1..12) {
                val date = buildDate(year, d1, d2)
                if (hasKeyword) return date // High confidence
                candidates.add(date)
            }
        }

        // 2. Month Name + Year formats
        val monthMap = mapOf(
            "jan" to 1, "january" to 1, "feb" to 2, "february" to 2, "mar" to 3, "march" to 3,
            "apr" to 4, "april" to 4, "may" to 5, "jun" to 6, "june" to 6, "jul" to 7, "july" to 7,
            "aug" to 8, "august" to 8, "sep" to 9, "september" to 9, "oct" to 10, "october" to 10,
            "nov" to 11, "november" to 11, "dec" to 12, "december" to 12
        )
        
        val monthNameYearRegex = Regex("\\b(\\d{1,2})?[\\s.-]*(jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec)[a-z]*[\\s,.-]*(\\d{4})\\b")
        monthNameYearRegex.findAll(lower).forEach { m ->
            val day = m.groupValues[1].toIntOrNull()
            val month = monthMap[m.groupValues[2]] ?: return@forEach
            val year = m.groupValues[3].toInt()
            
            val date = if (day != null && day in 1..31) buildDate(year, month, day) else getLastDayOfMonth(year, month)
            candidates.add(date)
        }

        // Pick the closest future date
        return candidates.filter { it > now }.minOrNull()
    }

    private fun getLastDayOfMonth(year: Int, month: Int): Long {
        val cal = Calendar.getInstance()
        cal.set(year, month - 1, 1)
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        return cal.timeInMillis
    }

    private fun buildDate(year: Int, month: Int, day: Int): Long {
        return Calendar.getInstance().apply {
            set(year, month - 1, day, 23, 59, 59)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}
