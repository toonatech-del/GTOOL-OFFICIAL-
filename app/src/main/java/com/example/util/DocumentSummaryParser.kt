package com.example.util

import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern

data class Entity(val label: String, val value: String)
data class ParsedDocumentSummary(
    val dates: List<String>,
    val extractedEntities: List<Entity>,
    val fullSummaryText: String,
    val headings: List<String>
)

object DocumentSummaryParser {
    enum class DocType {
        GENERIC, IDENTITY_DOCUMENT, BILL_INVOICE
    }

    fun classifyDocument(text: String): DocType {
        val idKeywords = listOf(
            "INCOME TAX DEPARTMENT", "PERMANENT ACCOUNT NUMBER", "PAN CARD",
            "GOVT. OF INDIA", "GOVERNMENT OF INDIA", "FATHER'S NAME",
            "DATE OF BIRTH", "DOB", "MALE", "FEMALE", "AADHAAR", "UIDAI",
            "ELECTION COMMISSION", "DRIVING LICENCE", "PASSPORT"
        )
        val count = idKeywords.count { text.contains(it, ignoreCase = true) }
        if (count >= 2) return DocType.IDENTITY_DOCUMENT
        
        val billKeywords = listOf("TOTAL", "AMOUNT", "DUE DATE", "INVOICE", "BILL", "PAYMENT", "RECEIPT", "TAX INVOICE")
        if (billKeywords.any { text.contains(it, ignoreCase = true) }) return DocType.BILL_INVOICE
        
        return DocType.GENERIC
    }

    fun extractExpiryDate(text: String): String? {
        // Regex to find expiry/validity dates
        val regex = "(?i)(valid\\s*(upto|till|until)|expiry\\s*date|exp\\.?\\s*date|validity|due\\s*date)\\s*[:\\-]?\\s*(\\d{1,2}[\\/\\-\\.]\\d{1,2}[\\/\\-\\.]\\d{2,4})"
        val pattern = Pattern.compile(regex)
        val matcher = pattern.matcher(text)
        
        if (matcher.find()) {
            val dateStr = matcher.group(3)
            val line = text.lines().find { it.contains(dateStr) } ?: ""
            // RULE: If DOB or Birth appears anywhere near the date string, discard it.
            if (line.contains("DOB", ignoreCase = true) || 
                line.contains("Birth", ignoreCase = true) || 
                line.contains("Date of Birth", ignoreCase = true)) {
                return null
            }
            
            val timestamp = parseDateToLong(dateStr) ?: return null
            val currentYear = Calendar.getInstance().get(Calendar.YEAR)
            val dateYear = Calendar.getInstance().apply { timeInMillis = timestamp }.get(Calendar.YEAR)
            
            // RULE: If date is before current year (e.g. 2026), ignore it.
            // RULE: Only consider future dates.
            if (dateYear < currentYear || timestamp <= System.currentTimeMillis()) {
                return null
            }
            
            return dateStr
        }
        return null
    }

    fun parseDateToLong(dateStr: String): Long? {
        val formats = listOf(
            "dd/MM/yyyy", "dd-MM-yyyy", "dd.MM.yyyy",
            "MM/dd/yyyy", "MM-dd-yyyy", "MM.dd.yyyy",
            "yyyy/MM/dd", "yyyy-MM-dd", "yyyy.MM.dd",
            "dd/MM/yy", "dd-MM-yy", "dd.MM.yy"
        )
        for (format in formats) {
            try {
                val sdf = SimpleDateFormat(format, Locale.US)
                sdf.isLenient = false
                return sdf.parse(dateStr)?.time
            } catch (e: Exception) {
                // Try next format
            }
        }
        return null
    }

    fun parse(text: String, title: String): ParsedDocumentSummary {
        val dates = mutableListOf<String>()
        val entities = mutableListOf<Entity>()
        val headings = mutableListOf<String>()
        
        // Use regex to find dates
        val dateRegex = "\\d{1,2}[\\/\\-\\.]\\d{1,2}[\\/\\-\\.]\\d{2,4}".toRegex()
        dateRegex.findAll(text).forEach { dates.add(it.value) }
        
        // Entity Extraction Logic
        val amountRegex = "(?i)(total|amount|rs|₹|\\$)\\s*[:\\-]?\\s*([\\d,.]+)".toRegex()
        amountRegex.find(text)?.let { 
            entities.add(Entity("Transaction Amount", it.groupValues[2]))
        }
        
        val panRegex = "[A-Z]{5}[0-9]{4}[A-Z]".toRegex()
        panRegex.find(text)?.let {
            entities.add(Entity("PAN Card Number", it.value))
        }
        
        val aadhaarRegex = "\\d{4}\\s\\d{4}\\s\\d{4}".toRegex()
        aadhaarRegex.find(text)?.let {
            entities.add(Entity("Aadhaar Number", it.value))
        }

        // Add expiry date as entity if found
        extractExpiryDate(text)?.let {
            entities.add(Entity("Document Expiry", it))
        }

        // Extract headings/key points
        val lines = text.lines().map { it.trim() }.filter { it.length in 5..50 }
        headings.addAll(lines.take(3))

        val summary = if (text.length > 200) text.take(200) + "..." else text

        return ParsedDocumentSummary(dates, entities, summary, headings)
    }
}
