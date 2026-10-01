package com.example.util

import com.example.model.CustomerDetails
import com.example.model.InvoiceItem
import com.example.model.SellerDetails
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class ParsedOcrInvoice(
    val seller: SellerDetails,
    val customer: CustomerDetails,
    val invoiceNumber: String,
    val invoiceDate: String,
    val dueDate: String,
    val items: List<InvoiceItem>,
    val scannedTotal: Double,
    val calculatedTotal: Double,
    val totalsMismatch: Boolean,
    val confidenceScore: Double, // confidence status out of 100
    val uncertainFields: List<String>,
    val isOcrUncertain: Boolean = false
)

object InvoiceOcrParser {

    /**
     * Parse raw OCR text into structured data.
     */
    fun parseOcrText(rawText: String): ParsedOcrInvoice {
        val lines = rawText.split("\n").map { it.trim() }.filter { it.isNotBlank() }
        
        var sellerName = ""
        var sellerAddress = ""
        var sellerPhone = ""
        var sellerEmail = ""
        var sellerGstin = ""
        var sellerPan = ""
        var sellerUpi = ""
        
        var customerName = ""
        var customerGstin = ""
        
        var invoiceNo = ""
        var invoiceDate = ""
        var dueDate = ""
        
        var scannedTotal = 0.0
        val items = mutableListOf<InvoiceItem>()
        val uncertainFields = mutableListOf<String>()

        if (lines.isEmpty()) {
            return createEmptyInvoice()
        }

        // 1. Match Seller details (typically top of document)
        sellerName = lines.firstOrNull() ?: ""
        if (sellerName.isBlank()) uncertainFields.add("Business Name")

        // Standard Indian GSTIN pattern
        val gstinRegex = Regex("""\b\d{2}[A-Z]{5}\d{4}[A-Z]\d[Z][A-Z\d]\b""")
        // Standard PAN pattern
        val panRegex = Regex("""\b[A-Z]{5}\d{4}[A-Z]\b""")
        // Mobile / Phone pattern
        val phoneRegex = Regex("""(?:\+91[\-\s]?)?[6-9]\d{9}\b|\b\d{3,5}[\-\s]?\d{6,8}\b""")
        // Email pattern
        val emailRegex = Regex("""\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,4}\b""")
        // UPI VPA pattern
        val upiRegex = Regex("""\b[a-zA-Z0-9.-]+@[a-zA-Z]{3,}\b""")

        // Parse lines for key metadata
        val gstinMatches = gstinRegex.findAll(rawText).map { it.value }.toList()
        if (gstinMatches.isNotEmpty()) {
            sellerGstin = gstinMatches.first()
            if (gstinMatches.size > 1) {
                customerGstin = gstinMatches[1]
            }
        }

        val panMatches = panRegex.findAll(rawText).map { it.value }.toList()
        if (panMatches.isNotEmpty()) {
            sellerPan = panMatches.first()
        }

        val emailMatches = emailRegex.findAll(rawText).map { it.value }.toList()
        if (emailMatches.isNotEmpty()) {
            sellerEmail = emailMatches.first()
        }

        val phoneMatches = phoneRegex.findAll(rawText).map { it.value }.toList()
        if (phoneMatches.isNotEmpty()) {
            sellerPhone = phoneMatches.first()
        }

        val upiMatches = upiRegex.findAll(rawText).map { it.value }.toList()
        if (upiMatches.isNotEmpty()) {
            sellerUpi = upiMatches.first()
        }

        // Try mapping seller address
        val addressKeywords = listOf("Street", "Road", "Sector", "Nagar", "Avenue", "Lane", "Bazar", "Market", "Floor", "Block")
        for (i in 1 until minOf(lines.size, 5)) {
            val line = lines[i]
            if (addressKeywords.any { line.contains(it, ignoreCase = true) } || line.contains(Regex("""\b\d{6}\b"""))) {
                sellerAddress = line
                break
            }
        }

        // 2. Invoice Meta data
        val invRegex = Regex("""(?i)(?:inv|invoice|bill|receipt|bill\s*no|invoice\s*no|tax\s*invoice)\s*(?:no|num|\#)?\s*[:.-]?\s*([A-Za-z0-9-/]+)""")
        for (line in lines) {
            val match = invRegex.find(line)
            if (match != null && match.groupValues.size >= 2) {
                invoiceNo = match.groupValues[1]
                break
            }
        }
        if (invoiceNo.isBlank()) {
            invoiceNo = "INV-${(1001..9999).random()}"
            uncertainFields.add("Invoice Number (Generated)")
        }

        // Invoice date mapping
        val dateRegex = Regex("""\b(\d{4}[-/]\d{2}[-/]\d{2}|\d{2}[-/]\d{2}[-/]\d{4})\b""")
        val datesFound = dateRegex.findAll(rawText).map { it.value }.toList()
        if (datesFound.isNotEmpty()) {
            invoiceDate = datesFound.first().replace("/", "-")
            if (datesFound.size > 1) {
                dueDate = datesFound[1].replace("/", "-")
            }
        }
        
        val systemDateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        if (invoiceDate.isBlank()) {
            invoiceDate = systemDateStr
            uncertainFields.add("Invoice Date (Using Current Date)")
        }
        if (dueDate.isBlank()) {
            dueDate = systemDateStr
        }

        // Try mapping Customer Name
        val customerKeywords = listOf("Bill To", "Billed To", "Customer", "Client", "Buyer")
        for (idx in 0 until lines.size) {
            val line = lines[idx]
            if (customerKeywords.any { line.contains(it, ignoreCase = true) } && idx + 1 < lines.size) {
                customerName = lines[idx + 1].trim()
                break
            }
        }
        if (customerName.isBlank()) {
            customerName = "Valued Customer"
            uncertainFields.add("Customer Name")
        }

        // 3. Totals extraction
        val totalRegex = Regex("""(?i)(?:total|grand\s*total|net\s*amount|bill\s*amount|due)\s*[:.-]?\s*(?:rs\.?|inr|₹|usd|\$)?\s*([0-9,]+\.[0-9]{2}|[0-9,]{3,})""")
        for (line in lines.reversed()) {
            val match = totalRegex.find(line)
            if (match != null && match.groupValues.size >= 2) {
                val cleanPrice = match.groupValues[1].replace(",", "")
                scannedTotal = cleanPrice.toDoubleOrNull() ?: 0.0
                break
            }
        }

        // 4. Line Items extraction (Table rows mapping)
        // Find line items by filtering out metadata, headings and matching price numbers at the end
        val priceRegex = Regex("""([0-9,]+\.[0-9]{2})$""")
        for (i in 1 until lines.size) {
            val line = lines[i]
            if (line.contains("total", ignoreCase = true) || 
                line.contains("subtotal", ignoreCase = true) || 
                line.contains("tax", ignoreCase = true) || 
                line.contains("gst", ignoreCase = true) ||
                line.contains("phone", ignoreCase = true) ||
                line.contains("gstin", ignoreCase = true)) {
                continue
            }
            val priceMatch = priceRegex.find(line)
            if (priceMatch != null) {
                val rateStr = priceMatch.groupValues[1].replace(",", "")
                val rateVal = rateStr.toDoubleOrNull() ?: 0.0
                val namePart = line.substring(0, priceMatch.range.first).trim().trimEnd { !it.isLetterOrDigit() }.trim()
                if (namePart.isNotBlank() && namePart.length > 2 && rateVal > 0.0) {
                    // Reconstruct columns: default Qty 1
                    val sub = rateVal * 1.0
                    val tax = sub * 0.18 // default 18% GST
                    items.add(InvoiceItem(
                        id = UUID.randomUUID().toString(),
                        name = namePart,
                        description = "Extracted via AI Scanner",
                        quantity = 1.0,
                        unitPrice = rateVal,
                        gstRatePercent = 18.0,
                        cgstRatePercent = 9.0,
                        sgstRatePercent = 9.0,
                        cgstAmount = tax / 2.0,
                        sgstAmount = tax / 2.0,
                        lineTotal = sub + tax
                    ))
                }
            }
        }

        // Fallback item if none detected
        if (items.isEmpty()) {
            val fallbackPrice = if (scannedTotal > 0.0) scannedTotal / 1.18 else 1000.00
            val tax = fallbackPrice * 0.18
            items.add(InvoiceItem(
                id = UUID.randomUUID().toString(),
                name = "Scanned General Items",
                description = "Manual items extraction fallback",
                quantity = 1.0,
                unitPrice = fallbackPrice,
                gstRatePercent = 18.0,
                cgstRatePercent = 9.0,
                sgstRatePercent = 9.0,
                cgstAmount = tax / 2.0,
                sgstAmount = tax / 2.0,
                lineTotal = fallbackPrice + tax
            ))
            uncertainFields.add("Reconstructed Line Items Table")
        }

        // Calculate Totals independently
        val calcSubtotal = items.sumOf { it.quantity * it.unitPrice }
        val calcCgst = items.sumOf { it.cgstAmount }
        val calcSgst = items.sumOf { it.sgstAmount }
        val calcIgst = items.sumOf { it.igstAmount }
        val calcGrandTotal = calcSubtotal + calcCgst + calcSgst + calcIgst

        if (scannedTotal == 0.0) {
            scannedTotal = calcGrandTotal
        }

        // Check validation
        val totalDiff = Math.abs(scannedTotal - calcGrandTotal)
        val totalsMismatch = totalDiff > 2.0 // allow small round-off

        // Calculate confidence score out of 100
        val baseScore = 100.0
        val penalty = uncertainFields.size * 12.0
        val confidenceScore = (baseScore - penalty).coerceIn(10.0, 100.0)

        return ParsedOcrInvoice(
            seller = SellerDetails(
                name = sellerName,
                address = sellerAddress,
                gstin = sellerGstin,
                pan = sellerPan,
                phone = sellerPhone,
                email = sellerEmail,
                upiId = sellerUpi
            ),
            customer = CustomerDetails(
                name = customerName,
                gstin = customerGstin
            ),
            invoiceNumber = invoiceNo,
            invoiceDate = invoiceDate,
            dueDate = dueDate,
            items = items,
            scannedTotal = scannedTotal,
            calculatedTotal = calcGrandTotal,
            totalsMismatch = totalsMismatch,
            confidenceScore = confidenceScore,
            uncertainFields = uncertainFields
        )
    }

    private fun createEmptyInvoice(): ParsedOcrInvoice {
        return ParsedOcrInvoice(
            seller = SellerDetails(),
            customer = CustomerDetails(),
            invoiceNumber = "",
            invoiceDate = "",
            dueDate = "",
            items = emptyList(),
            scannedTotal = 0.0,
            calculatedTotal = 0.0,
            totalsMismatch = false,
            confidenceScore = 0.0,
            uncertainFields = listOf("Empty Document")
        )
    }
}
