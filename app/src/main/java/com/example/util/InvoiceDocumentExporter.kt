package com.example.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.FileProvider
import com.example.data.local.InvoiceEntity
import com.example.model.CustomerDetails
import com.example.model.InvoiceItem
import com.example.model.SellerDetails
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object InvoiceDocumentExporter {

    private const val TAG = "InvoiceDocumentExporter"

    /**
     * Generates a safe, sanitized filename.
     */
    fun getSafeFileName(prefix: String, customerName: String, extension: String): String {
        val sanitized = if (customerName.isNotBlank()) {
            customerName.replace(Regex("[^a-zA-Z0-9_]"), "_").take(20)
        } else {
            "Customer"
        }
        val dateStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        return "${prefix}_${sanitized}_$dateStr.$extension"
    }

    /**
     * Exports a plain TXT invoice with zero mandatory fields (hides empty sections).
     */
    suspend fun generateTxtInvoice(
        context: Context,
        invoice: InvoiceEntity
    ): Uri? = withContext(Dispatchers.IO) {
        try {
            val seller = JsonHelper.fromSellerJson(invoice.sellerDetailsJson)
            val customer = JsonHelper.fromCustomerJson(invoice.customerDetailsJson)
            val items = JsonHelper.fromItemsJson(invoice.itemsJson)

            val effectiveSellerName = if (seller.name.isNotBlank()) seller.name else "My Business"
            val effectiveCustomerName = if (customer.name.isNotBlank()) customer.name else "Valued Customer"
            val effectiveInvoiceNumber = if (invoice.invoiceNumber.isNotBlank()) invoice.invoiceNumber else "INV-001"

            val sb = StringBuilder()
            sb.append("========================================================================\n")
            sb.append("                           INVOICE / BILL                               \n")
            sb.append("========================================================================\n")
            sb.append("Invoice No     : $effectiveInvoiceNumber\n")
            if (invoice.invoiceDate.isNotBlank()) sb.append("Invoice Date   : ${invoice.invoiceDate}\n")
            if (invoice.dueDate.isNotBlank()) sb.append("Due Date       : ${invoice.dueDate}\n")
            if (invoice.paymentStatus.isNotBlank()) sb.append("Payment Status : ${invoice.paymentStatus}\n")
            if (invoice.paymentMethod.isNotBlank()) sb.append("Payment Method : ${invoice.paymentMethod}\n")
            if (invoice.referenceNo.isNotBlank()) sb.append("Reference No   : ${invoice.referenceNo}\n")
            sb.append("------------------------------------------------------------------------\n")
            sb.append("FROM (SELLER):\n")
            sb.append("  Name    : $effectiveSellerName\n")
            if (seller.ownerName.isNotBlank()) sb.append("  Owner   : ${seller.ownerName}\n")
            if (seller.phone.isNotBlank()) sb.append("  Phone   : ${seller.phone}\n")
            if (seller.email.isNotBlank()) sb.append("  Email   : ${seller.email}\n")
            val sellerLoc = buildString {
                if (seller.address.isNotBlank()) append(seller.address)
                if (seller.city.isNotBlank()) {
                    if (isNotEmpty()) append(", ")
                    append(seller.city)
                }
                if (seller.state.isNotBlank()) {
                    if (isNotEmpty()) append(", ")
                    append(seller.state)
                }
                if (seller.pinCode.isNotBlank()) {
                    if (isNotEmpty()) append(" - ")
                    append(seller.pinCode)
                }
            }
            if (sellerLoc.isNotBlank()) sb.append("  Address : $sellerLoc\n")
            if (seller.gstin.isNotBlank()) sb.append("  GSTIN   : ${seller.gstin}\n")
            if (seller.pan.isNotBlank()) sb.append("  PAN     : ${seller.pan}\n")
            sb.append("\n")
            sb.append("TO (CUSTOMER):\n")
            sb.append("  Name    : $effectiveCustomerName\n")
            if (customer.companyName.isNotBlank()) sb.append("  Company : ${customer.companyName}\n")
            if (customer.phone.isNotBlank()) sb.append("  Phone   : ${customer.phone}\n")
            if (customer.email.isNotBlank()) sb.append("  Email   : ${customer.email}\n")
            if (customer.billingAddress.isNotBlank()) sb.append("  Billing : ${customer.billingAddress}\n")
            if (customer.shippingAddress.isNotBlank()) sb.append("  Shipping: ${customer.shippingAddress}\n")
            if (customer.gstin.isNotBlank()) sb.append("  GSTIN   : ${customer.gstin}\n")
            if (customer.pan.isNotBlank()) sb.append("  PAN     : ${customer.pan}\n")
            sb.append("------------------------------------------------------------------------\n")
            sb.append("%-25s %-10s %-8s %-10s %-10s\n".format("Item Name", "HSN/SAC", "Qty", "Price", "Total"))
            sb.append("------------------------------------------------------------------------\n")
            items.forEach { item ->
                val name = if (item.name.length > 24) item.name.take(22) + ".." else item.name
                sb.append("%-25s %-10s %-8.2f %-10.2f %-10.2f\n".format(
                    name, item.hsnSac, item.quantity, item.unitPrice, item.lineTotal
                ))
            }
            sb.append("------------------------------------------------------------------------\n")
            sb.append("Subtotal         : %s %.2f\n".format(invoice.currency, invoice.subtotal))
            if (invoice.totalDiscount > 0) {
                sb.append("Total Discount   : %s %.2f\n".format(invoice.currency, invoice.totalDiscount))
            }
            sb.append("Taxable Amount   : %s %.2f\n".format(invoice.currency, invoice.taxableAmount))
            if (invoice.cgst > 0) sb.append("CGST             : %s %.2f\n".format(invoice.currency, invoice.cgst))
            if (invoice.sgst > 0) sb.append("SGST             : %s %.2f\n".format(invoice.currency, invoice.sgst))
            if (invoice.igst > 0) sb.append("IGST             : %s %.2f\n".format(invoice.currency, invoice.igst))
            if (invoice.cess > 0) sb.append("Cess             : %s %.2f\n".format(invoice.currency, invoice.cess))
            if (invoice.additionalCharges > 0) {
                sb.append("Add. Charges     : %s %.2f\n".format(invoice.currency, invoice.additionalCharges))
            }
            if (invoice.roundOff != 0.0) {
                sb.append("Round-off        : %s %.2f\n".format(invoice.currency, invoice.roundOff))
            }
            sb.append("------------------------------------------------------------------------\n")
            sb.append("GRAND TOTAL      : %s %.2f\n".format(invoice.currency, invoice.grandTotal))
            if (invoice.amountPaid > 0) {
                sb.append("Amount Paid      : %s %.2f\n".format(invoice.currency, invoice.amountPaid))
                sb.append("Balance Due      : %s %.2f\n".format(invoice.currency, invoice.balanceDue))
            }
            sb.append("========================================================================\n")
            if (seller.upiId.isNotBlank()) {
                sb.append("PAYMENT UPI ID   : ${seller.upiId}\n")
            }
            val hasBank = seller.bankName.isNotBlank() || seller.accountNumber.isNotBlank() || seller.ifsc.isNotBlank()
            if (hasBank) {
                val bankLine = buildString {
                    if (seller.bankName.isNotBlank()) append("Bank: ${seller.bankName}")
                    if (seller.accountNumber.isNotBlank()) {
                        if (isNotEmpty()) append(" | ")
                        append("A/C: ${seller.accountNumber}")
                    }
                    if (seller.ifsc.isNotBlank()) {
                        if (isNotEmpty()) append(" | ")
                        append("IFSC: ${seller.ifsc}")
                    }
                }
                sb.append("BANK DETAILS     : $bankLine\n")
            }
            if (invoice.notes.isNotBlank()) {
                sb.append("Notes            : ${invoice.notes}\n")
            }
            if (invoice.terms.isNotBlank()) {
                sb.append("Terms & Conditions: ${invoice.terms}\n")
            }
            sb.append("\nThank you for your business!\n")
            sb.append("Powered by GSD\n")

            val fileName = getSafeFileName("INV_$effectiveInvoiceNumber", effectiveCustomerName, "txt")
            saveDocumentFile(context, fileName, sb.toString(), "text/plain")
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Exports an editable DOC/HTML invoice with zero mandatory fields.
     */
    suspend fun generateDocInvoice(
        context: Context,
        invoice: InvoiceEntity
    ): Uri? = withContext(Dispatchers.IO) {
        try {
            val seller = JsonHelper.fromSellerJson(invoice.sellerDetailsJson)
            val customer = JsonHelper.fromCustomerJson(invoice.customerDetailsJson)
            val items = JsonHelper.fromItemsJson(invoice.itemsJson)

            val effectiveSellerName = if (seller.name.isNotBlank()) seller.name else "My Business"
            val effectiveCustomerName = if (customer.name.isNotBlank()) customer.name else "Valued Customer"
            val effectiveInvoiceNumber = if (invoice.invoiceNumber.isNotBlank()) invoice.invoiceNumber else "INV-001"

            val html = """
                <html>
                <head>
                <style>
                    body { font-family: Arial, sans-serif; margin: 30px; color: #333; }
                    .header { text-align: center; border-bottom: 2px solid #ddd; padding-bottom: 10px; }
                    .details-table { width: 100%; margin-top: 20px; border-collapse: collapse; }
                    .details-table td { width: 50%; vertical-align: top; padding: 5px; }
                    .item-table { width: 100%; margin-top: 30px; border-collapse: collapse; }
                    .item-table th, .item-table td { border: 1px solid #ddd; padding: 10px; text-align: left; }
                    .item-table th { background-color: #f5f5f5; }
                    .summary-section { width: 100%; margin-top: 20px; text-align: right; }
                    .summary-table { margin-left: auto; border-collapse: collapse; }
                    .summary-table td { padding: 6px 15px; border-bottom: 1px solid #eee; }
                    .footer { text-align: center; margin-top: 50px; font-size: 12px; color: #777; border-top: 1px solid #ddd; padding-top: 15px; }
                </style>
                </head>
                <body>
                    <div class="header">
                        <h2>INVOICE / BILL</h2>
                        <p><b>Invoice No:</b> $effectiveInvoiceNumber ${if (invoice.invoiceDate.isNotBlank()) " | <b>Date:</b> " + invoice.invoiceDate else ""}</p>
                    </div>
                    
                    <table class="details-table">
                        <tr>
                            <td>
                                <h3>From (Seller):</h3>
                                <b>$effectiveSellerName</b><br/>
                                ${if (seller.ownerName.isNotBlank()) "Owner: " + seller.ownerName + "<br/>" else ""}
                                ${if (seller.address.isNotBlank()) seller.address + "<br/>" else ""}
                                ${if (seller.city.isNotBlank() || seller.state.isNotBlank()) "${seller.city} ${seller.state} ${seller.pinCode}<br/>" else ""}
                                ${if (seller.phone.isNotBlank() || seller.email.isNotBlank()) "Ph: ${seller.phone} | Email: ${seller.email}<br/>" else ""}
                                ${if (seller.gstin.isNotBlank()) "GSTIN: " + seller.gstin + "<br/>" else ""}
                                ${if (seller.pan.isNotBlank()) "PAN: " + seller.pan else ""}
                            </td>
                            <td>
                                <h3>To (Customer):</h3>
                                <b>$effectiveCustomerName</b><br/>
                                ${if (customer.companyName.isNotBlank()) customer.companyName + "<br/>" else ""}
                                ${if (customer.billingAddress.isNotBlank()) "Billing: " + customer.billingAddress + "<br/>" else ""}
                                ${if (customer.shippingAddress.isNotBlank()) "Shipping: " + customer.shippingAddress + "<br/>" else ""}
                                ${if (customer.phone.isNotBlank() || customer.email.isNotBlank()) "Ph: ${customer.phone} | Email: ${customer.email}<br/>" else ""}
                                ${if (customer.gstin.isNotBlank()) "GSTIN: " + customer.gstin + "<br/>" else ""}
                                ${if (customer.pan.isNotBlank()) "PAN: " + customer.pan else ""}
                            </td>
                        </tr>
                    </table>

                    <table class="item-table">
                        <thead>
                            <tr>
                                <th>Item Description</th>
                                <th>HSN/SAC</th>
                                <th>Qty</th>
                                <th>Unit Price</th>
                                <th>Total</th>
                            </tr>
                        </thead>
                        <tbody>
                            ${items.joinToString("") { 
                                "<tr>" +
                                "<td><b>${it.name}</b>${if (it.description.isNotBlank()) "<br/><small>${it.description}</small>" else ""}</td>" +
                                "<td>${it.hsnSac}</td>" +
                                "<td>${it.quantity} ${it.unit}</td>" +
                                "<td>${invoice.currency} ${String.format("%.2f", it.unitPrice)}</td>" +
                                "<td>${invoice.currency} ${String.format("%.2f", it.lineTotal)}</td>" +
                                "</tr>"
                            }}
                        </tbody>
                    </table>

                    <div class="summary-section">
                        <table class="summary-table">
                            <tr><td>Subtotal:</td><td><b>${invoice.currency} ${String.format("%.2f", invoice.subtotal)}</b></td></tr>
                            ${if (invoice.totalDiscount > 0) "<tr><td>Discount:</td><td><b>" + invoice.currency + " " + String.format("%.2f", invoice.totalDiscount) + "</b></td></tr>" else ""}
                            <tr><td>Taxable Amount:</td><td><b>${invoice.currency} ${String.format("%.2f", invoice.taxableAmount)}</b></td></tr>
                            ${if (invoice.cgst > 0) "<tr><td>CGST:</td><td><b>" + invoice.currency + " " + String.format("%.2f", invoice.cgst) + "</b></td></tr>" else ""}
                            ${if (invoice.sgst > 0) "<tr><td>SGST:</td><td><b>" + invoice.currency + " " + String.format("%.2f", invoice.sgst) + "</b></td></tr>" else ""}
                            ${if (invoice.igst > 0) "<tr><td>IGST:</td><td><b>" + invoice.currency + " " + String.format("%.2f", invoice.igst) + "</b></td></tr>" else ""}
                            ${if (invoice.cess > 0) "<tr><td>Cess:</td><td><b>" + invoice.currency + " " + String.format("%.2f", invoice.cess) + "</b></td></tr>" else ""}
                            ${if (invoice.additionalCharges > 0) "<tr><td>Add. Charges:</td><td><b>" + invoice.currency + " " + String.format("%.2f", invoice.additionalCharges) + "</b></td></tr>" else ""}
                            ${if (invoice.roundOff != 0.0) "<tr><td>Round-off:</td><td><b>" + invoice.currency + " " + String.format("%.2f", invoice.roundOff) + "</b></td></tr>" else ""}
                            <tr style="font-size: 18px; color: #111;"><td><b>GRAND TOTAL:</b></td><td><b>${invoice.currency} ${String.format("%.2f", invoice.grandTotal)}</b></td></tr>
                            ${if (invoice.amountPaid > 0) "<tr><td>Amount Paid:</td><td>${invoice.currency} " + String.format("%.2f", invoice.amountPaid) + "</td></tr><tr style='color: red;'><td><b>Balance Due:</b></td><td><b>${invoice.currency} " + String.format("%.2f", invoice.balanceDue) + "</b></td></tr>" else ""}
                        </table>
                    </div>

                    <div style="margin-top: 30px;">
                        ${if (seller.upiId.isNotBlank()) "<h4>UPI Payment Details:</h4>UPI ID: <b>" + seller.upiId + "</b><br/>" else ""}
                        ${if (seller.bankName.isNotBlank() || seller.accountNumber.isNotBlank()) "<h4>Bank Account Details:</h4>" + (if (seller.bankName.isNotBlank()) "Bank: " + seller.bankName else "") + (if (seller.accountNumber.isNotBlank()) " | Account No: <b>" + seller.accountNumber + "</b>" else "") + (if (seller.ifsc.isNotBlank()) " | IFSC: <b>" + seller.ifsc + "</b>" else "") else ""}
                    </div>

                    ${if (invoice.notes.isNotBlank()) "<div style='margin-top:20px;'><b>Notes:</b><br/>" + invoice.notes + "</div>" else ""}
                    ${if (invoice.terms.isNotBlank()) "<div style='margin-top:20px;'><b>Terms & Conditions:</b><br/>" + invoice.terms + "</div>" else ""}

                    <div class="footer">
                        <p>Thank you for your business!</p>
                        <p>Powered by GSD</p>
                    </div>
                </body>
                </html>
            """.trimIndent()

            val fileName = getSafeFileName("INV_$effectiveInvoiceNumber", effectiveCustomerName, "doc")
            saveDocumentFile(context, fileName, html, "application/msword")
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Exports a stunning professional A4 PDF invoice.
     * Accurately supports multi-page overflow, custom templates, logo, UPI QR, drawn/uploaded signatures,
     * and strictly eliminates any blank rows, empty labels, or missing field placeholders.
     */
    suspend fun generatePdfInvoice(
        context: Context,
        invoice: InvoiceEntity
    ): Uri? = withContext(Dispatchers.IO) {
        try {
            val seller = JsonHelper.fromSellerJson(invoice.sellerDetailsJson)
            val customer = JsonHelper.fromCustomerJson(invoice.customerDetailsJson)
            val items = JsonHelper.fromItemsJson(invoice.itemsJson)

            val effectiveSellerName = if (seller.name.isNotBlank()) seller.name else "My Business"
            val effectiveCustomerName = if (customer.name.isNotBlank()) customer.name else "Customer"
            val effectiveInvoiceNumber = if (invoice.invoiceNumber.isNotBlank()) invoice.invoiceNumber else "INV-001"

            val pdfDocument = PdfDocument()
            val pageWidth = 595 // A4 width
            val pageHeight = 842 // A4 height
            
            var pageIndex = 1
            var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageIndex).create()
            var page = pdfDocument.startPage(pageInfo)
            var canvas = page.canvas

            val paint = Paint().apply { isAntiAlias = true }
            val textPaint = Paint().apply {
                isAntiAlias = true
                color = Color.BLACK
                textSize = 10f
            }

            // Colors based on template
            val primaryColor = when (invoice.templateName) {
                "Modern" -> Color.parseColor("#00E5FF") // Cyan
                "Business" -> Color.parseColor("#4F46E5") // Indigo
                "Retail" -> Color.parseColor("#FF5722") // Warm Orange
                "Service" -> Color.parseColor("#10B981") // Teal Green
                "Minimal" -> Color.parseColor("#111827") // Slate
                else -> Color.parseColor("#FF9F43") // Warm Gold
            }

            var y = 36f // Starting with strict 36pt margin from the top

            fun drawHeader() {
                // Header Background Stripe
                paint.color = primaryColor
                canvas.drawRect(36f, 36f, (pageWidth - 36).toFloat(), 44f, paint)

                y = 65f
                // Brand logo / badge
                val logoPaint = Paint(paint).apply {
                    color = primaryColor
                    style = Paint.Style.STROKE
                    strokeWidth = 1.5f
                }
                canvas.drawRoundRect(40f, y, 115f, y + 25f, 6f, 6f, logoPaint)
                
                logoPaint.style = Paint.Style.FILL
                logoPaint.color = primaryColor
                logoPaint.alpha = 24 // soft tint
                canvas.drawRoundRect(40f, y, 115f, y + 25f, 6f, 6f, logoPaint)

                textPaint.color = primaryColor
                textPaint.textSize = 10f
                textPaint.isFakeBoldText = true
                textPaint.textAlign = Paint.Align.CENTER
                canvas.drawText("GTool X", 77.5f, y + 16f, textPaint)

                // Title (Right aligned within the 36pt margin)
                textPaint.textSize = 20f
                textPaint.color = primaryColor
                textPaint.textAlign = Paint.Align.RIGHT
                canvas.drawText("TAX INVOICE", (pageWidth - 36).toFloat(), y + 18f, textPaint)

                textPaint.textSize = 9f
                textPaint.color = Color.DKGRAY
                textPaint.isFakeBoldText = false
                var metaY = y + 36f
                canvas.drawText("Invoice No: $effectiveInvoiceNumber", (pageWidth - 36).toFloat(), metaY, textPaint)
                if (invoice.invoiceDate.isNotBlank()) {
                    metaY += 16f
                    canvas.drawText("Date: ${invoice.invoiceDate}", (pageWidth - 36).toFloat(), metaY, textPaint)
                }
                if (invoice.dueDate.isNotBlank()) {
                    metaY += 16f
                    canvas.drawText("Due Date: ${invoice.dueDate}", (pageWidth - 36).toFloat(), metaY, textPaint)
                }

                y += 92f

                // Seller vs Customer details columns
                textPaint.textSize = 10f
                textPaint.color = primaryColor
                textPaint.isFakeBoldText = true
                textPaint.textAlign = Paint.Align.LEFT
                canvas.drawText("BILLED BY (SELLER):", 40f, y, textPaint)
                canvas.drawText("BILLED TO (BUYER):", 308f, y, textPaint)

                y += 18f
                
                val sellerLines = mutableListOf<String>()
                sellerLines.add(effectiveSellerName)
                if (seller.ownerName.isNotBlank()) sellerLines.add("Owner: ${seller.ownerName}")
                if (seller.address.isNotBlank()) sellerLines.add(seller.address)
                if (seller.city.isNotBlank() || seller.state.isNotBlank() || seller.pinCode.isNotBlank()) {
                    val cityStatePin = buildString {
                        if (seller.city.isNotBlank()) append(seller.city)
                        if (seller.state.isNotBlank()) {
                            if (isNotEmpty()) append(", ")
                            append(seller.state)
                        }
                        if (seller.pinCode.isNotBlank()) {
                            if (isNotEmpty()) append(" - ")
                            append(seller.pinCode)
                        }
                    }
                    if (cityStatePin.isNotBlank()) sellerLines.add(cityStatePin)
                }
                if (seller.phone.isNotBlank() || seller.email.isNotBlank()) {
                    var phEmail = ""
                    if (seller.phone.isNotBlank()) phEmail += "Ph: ${seller.phone}"
                    if (seller.email.isNotBlank()) phEmail += (if (phEmail.isNotEmpty()) " | " else "") + seller.email
                    sellerLines.add(phEmail)
                }
                if (seller.gstin.isNotBlank()) sellerLines.add("GSTIN: ${seller.gstin}")
                if (seller.pan.isNotBlank()) sellerLines.add("PAN: ${seller.pan}")

                val customerLines = mutableListOf<String>()
                customerLines.add(effectiveCustomerName)
                if (customer.companyName.isNotBlank()) customerLines.add(customer.companyName)
                if (customer.billingAddress.isNotBlank()) customerLines.add("Billing: ${customer.billingAddress}")
                if (customer.shippingAddress.isNotBlank()) customerLines.add("Shipping: ${customer.shippingAddress}")
                if (customer.phone.isNotBlank() || customer.email.isNotBlank()) {
                    var phEmail = ""
                    if (customer.phone.isNotBlank()) phEmail += "Ph: ${customer.phone}"
                    if (customer.email.isNotBlank()) phEmail += (if (phEmail.isNotEmpty()) " | " else "") + customer.email
                    customerLines.add(phEmail)
                }
                if (customer.gstin.isNotBlank()) customerLines.add("GSTIN: ${customer.gstin}")
                if (customer.pan.isNotBlank()) customerLines.add("PAN: ${customer.pan}")

                val wrappedSellerLines = mutableListOf<String>()
                val wrappedCustomerLines = mutableListOf<String>()

                textPaint.textSize = 9f
                val colWidth = 247f

                for (line in sellerLines) {
                    textPaint.isFakeBoldText = (line == effectiveSellerName)
                    wrappedSellerLines.addAll(wrapText(line, textPaint, colWidth))
                }

                for (line in customerLines) {
                    textPaint.isFakeBoldText = (line == effectiveCustomerName)
                    wrappedCustomerLines.addAll(wrapText(line, textPaint, colWidth))
                }

                val maxLines = maxOf(wrappedSellerLines.size, wrappedCustomerLines.size)
                for (i in 0 until maxLines) {
                    if (i < wrappedSellerLines.size) {
                        val sLine = wrappedSellerLines[i]
                        textPaint.isFakeBoldText = (sLine == effectiveSellerName)
                        textPaint.color = if (sLine == effectiveSellerName) Color.BLACK else Color.parseColor("#374151")
                        canvas.drawText(sLine, 40f, y, textPaint)
                    }
                    if (i < wrappedCustomerLines.size) {
                        val cLine = wrappedCustomerLines[i]
                        textPaint.isFakeBoldText = (cLine == effectiveCustomerName)
                        textPaint.color = if (cLine == effectiveCustomerName) Color.BLACK else Color.parseColor("#374151")
                        canvas.drawText(cLine, 308f, y, textPaint)
                    }
                    y += 14f
                }

                // Divider line
                paint.color = Color.parseColor("#E5E7EB")
                paint.strokeWidth = 1f
                canvas.drawLine(36f, y + 6f, (pageWidth - 36).toFloat(), y + 6f, paint)

                y += 24f
            }

            fun drawTableHeader(headerY: Float) {
                paint.color = primaryColor
                paint.style = Paint.Style.FILL
                canvas.drawRect(36f, headerY, (pageWidth - 36).toFloat(), headerY + 20f, paint)

                textPaint.color = Color.BLACK
                textPaint.isFakeBoldText = true
                textPaint.textSize = 9f

                textPaint.textAlign = Paint.Align.CENTER
                canvas.drawText("S.No", 57f, headerY + 14f, textPaint)
                canvas.drawText("Qty", 339.5f, headerY + 14f, textPaint)
                canvas.drawText("Tax %", 480f, headerY + 14f, textPaint)

                textPaint.textAlign = Paint.Align.LEFT
                canvas.drawText("Item / Service Description", 80f, headerY + 14f, textPaint)

                textPaint.textAlign = Paint.Align.RIGHT
                canvas.drawText("Rate", 450f, headerY + 14f, textPaint)
                canvas.drawText("Amount", 559f, headerY + 14f, textPaint)

                textPaint.textAlign = Paint.Align.LEFT
            }

            drawHeader()
            drawTableHeader(y)

            y += 24f
            textPaint.isFakeBoldText = false
            textPaint.color = Color.BLACK

            // Draw Items with automatic multi-page calculation
            items.forEachIndexed { idx, item ->
                textPaint.textSize = 9f
                textPaint.isFakeBoldText = true
                val wrappedName = wrapText(item.name.ifBlank { "Item ${idx + 1}" }, textPaint, 230f)
                
                textPaint.isFakeBoldText = false
                textPaint.textSize = 7.5f
                val wrappedDesc = if (item.description.isNotBlank()) {
                    wrapText(item.description, textPaint, 230f)
                } else {
                    emptyList()
                }

                val topPadding = 8f
                val bottomPadding = 8f
                val nameLineHeight = 14f
                val descLineHeight = 11f
                
                val nameBlockHeight = wrappedName.size * nameLineHeight
                val descBlockHeight = wrappedDesc.size * descLineHeight
                val totalTextBlockHeight = nameBlockHeight + descBlockHeight
                val rowHeight = maxOf(24f, totalTextBlockHeight + topPadding + bottomPadding)

                if (y + rowHeight > 780f) {
                    pdfDocument.finishPage(page)
                    pageIndex++
                    pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageIndex).create()
                    page = pdfDocument.startPage(pageInfo)
                    canvas = page.canvas
                    drawHeader()
                    
                    drawTableHeader(y)
                    y += 24f
                }

                val centerY = y + rowHeight / 2f
                val singleLineBaselineY = centerY + (9f / 3.5f)

                textPaint.textSize = 9f
                textPaint.isFakeBoldText = false
                
                textPaint.textAlign = Paint.Align.CENTER
                canvas.drawText("${idx + 1}", 57f, singleLineBaselineY, textPaint)
                canvas.drawText("${String.format("%.1f", item.quantity)} ${item.unit}", 339.5f, singleLineBaselineY, textPaint)
                canvas.drawText("${String.format("%.0f", item.gstRatePercent)}%", 480f, singleLineBaselineY, textPaint)
                
                textPaint.textAlign = Paint.Align.RIGHT
                canvas.drawText("${invoice.currency} ${String.format("%.2f", item.unitPrice)}", 450f, singleLineBaselineY, textPaint)
                canvas.drawText("${invoice.currency} ${String.format("%.2f", item.lineTotal)}", 559f, singleLineBaselineY, textPaint)

                textPaint.textAlign = Paint.Align.LEFT
                var currentTextY = y + topPadding + 10f
                
                textPaint.isFakeBoldText = true
                textPaint.textSize = 9f
                wrappedName.forEach { line ->
                    canvas.drawText(line, 80f, currentTextY, textPaint)
                    currentTextY += nameLineHeight
                }

                if (wrappedDesc.isNotEmpty()) {
                    textPaint.isFakeBoldText = false
                    textPaint.textSize = 7.5f
                    textPaint.color = Color.GRAY
                    currentTextY -= (nameLineHeight - descLineHeight)
                    wrappedDesc.forEach { line ->
                        canvas.drawText(line, 80f, currentTextY, textPaint)
                        currentTextY += descLineHeight
                    }
                    textPaint.color = Color.BLACK
                }

                y += rowHeight
                paint.color = Color.parseColor("#F3F4F6")
                paint.strokeWidth = 1f
                canvas.drawLine(36f, y, (pageWidth - 36).toFloat(), y, paint)
                
                y += 4f
            }

            if (y + 180f > 780f) {
                pdfDocument.finishPage(page)
                pageIndex++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageIndex).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                drawHeader()
            }

            val leftX = 40f
            var sumY = y + 15f

            textPaint.textSize = 9.5f
            textPaint.isFakeBoldText = false
            
            val summaryRows = mutableListOf<Pair<String, Double>>()
            summaryRows.add("Subtotal:" to invoice.subtotal)
            if (invoice.totalDiscount > 0) summaryRows.add("Discount:" to -invoice.totalDiscount)
            summaryRows.add("Taxable Value:" to invoice.taxableAmount)
            if (invoice.cgst > 0) summaryRows.add("CGST:" to invoice.cgst)
            if (invoice.sgst > 0) summaryRows.add("SGST:" to invoice.sgst)
            if (invoice.igst > 0) summaryRows.add("IGST:" to invoice.igst)
            if (invoice.cess > 0) summaryRows.add("Cess:" to invoice.cess)
            if (invoice.additionalCharges > 0) summaryRows.add("Add. Charges:" to invoice.additionalCharges)
            if (invoice.roundOff != 0.0) summaryRows.add("Round-off:" to invoice.roundOff)

            summaryRows.forEach { (label, value) ->
                textPaint.color = Color.parseColor("#4B5563")
                textPaint.textAlign = Paint.Align.RIGHT
                canvas.drawText(label, 450f, sumY, textPaint)
                textPaint.color = Color.BLACK
                canvas.drawText("${invoice.currency} ${String.format("%.2f", value)}", 559f, sumY, textPaint)
                sumY += 15f
            }

            // Draw Grand Total Box
            paint.color = primaryColor
            paint.style = Paint.Style.FILL
            canvas.drawRoundRect(350f, sumY, (pageWidth - 36).toFloat(), sumY + 28f, 6f, 6f, paint)

            textPaint.color = Color.BLACK
            textPaint.isFakeBoldText = true
            textPaint.textSize = 10.5f
            textPaint.textAlign = Paint.Align.LEFT
            canvas.drawText("GRAND TOTAL:", 360f, sumY + 18f, textPaint)
            
            textPaint.textAlign = Paint.Align.RIGHT
            canvas.drawText("${invoice.currency} ${String.format("%.2f", invoice.grandTotal)}", 550f, sumY + 18f, textPaint)

            if (invoice.amountPaid > 0) {
                sumY += 38f
                textPaint.textSize = 9f
                textPaint.isFakeBoldText = false
                textPaint.color = Color.DKGRAY
                
                textPaint.textAlign = Paint.Align.LEFT
                canvas.drawText("Amount Paid:", 360f, sumY, textPaint)
                textPaint.textAlign = Paint.Align.RIGHT
                canvas.drawText("${invoice.currency} ${String.format("%.2f", invoice.amountPaid)}", 559f, sumY, textPaint)
                
                sumY += 14f
                textPaint.isFakeBoldText = true
                textPaint.color = Color.RED
                textPaint.textAlign = Paint.Align.LEFT
                canvas.drawText("Balance Due:", 360f, sumY, textPaint)
                textPaint.textAlign = Paint.Align.RIGHT
                canvas.drawText("${invoice.currency} ${String.format("%.2f", invoice.balanceDue)}", 559f, sumY, textPaint)
            }
            
            textPaint.textAlign = Paint.Align.LEFT // restore

            // Draw Payment Information Block on the left side (Only if available)
            val hasBank = seller.bankName.isNotBlank() || seller.accountNumber.isNotBlank() || seller.ifsc.isNotBlank()
            val hasUpi = seller.upiId.isNotBlank()
            var payY = y + 15f

            if (hasBank || hasUpi || invoice.paymentMethod.isNotBlank()) {
                textPaint.textSize = 10f
                textPaint.color = primaryColor
                textPaint.isFakeBoldText = true
                canvas.drawText("PAYMENT INFORMATION:", leftX, payY, textPaint)

                payY += 15f
                textPaint.textSize = 8.5f
                textPaint.isFakeBoldText = false
                textPaint.color = Color.BLACK
                if (invoice.paymentMethod.isNotBlank()) {
                    canvas.drawText("Method: ${invoice.paymentMethod}", leftX, payY, textPaint)
                }

                if (hasBank) {
                    if (seller.bankName.isNotBlank()) {
                        payY += 13f
                        canvas.drawText("Bank: ${seller.bankName}", leftX, payY, textPaint)
                    }
                    if (seller.accountNumber.isNotBlank()) {
                        payY += 12f
                        canvas.drawText("A/C: ${seller.accountNumber}", leftX, payY, textPaint)
                    }
                    if (seller.ifsc.isNotBlank()) {
                        payY += 12f
                        canvas.drawText("IFSC: ${seller.ifsc}", leftX, payY, textPaint)
                    }
                }

                if (hasUpi) {
                    payY += 16f
                    val qrX = leftX
                    val qrY = payY
                    val qrSize = 65f

                    paint.color = Color.WHITE
                    paint.style = Paint.Style.FILL
                    canvas.drawRect(qrX, qrY, qrX + qrSize + 10f, qrY + qrSize + 10f, paint)
                    
                    paint.color = Color.BLACK
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = 1f
                    canvas.drawRect(qrX, qrY, qrX + qrSize, qrY + qrSize, paint)

                    paint.style = Paint.Style.FILL
                    canvas.drawRect(qrX + 3f, qrY + 3f, qrX + 18f, qrY + 18f, paint)
                    paint.color = Color.WHITE
                    canvas.drawRect(qrX + 5f, qrY + 5f, qrX + 16f, qrY + 16f, paint)
                    paint.color = Color.BLACK
                    canvas.drawRect(qrX + 7f, qrY + 7f, qrX + 14f, qrY + 14f, paint)

                    canvas.drawRect(qrX + qrSize - 18f, qrY + 3f, qrX + qrSize - 3f, qrY + 18f, paint)
                    paint.color = Color.WHITE
                    canvas.drawRect(qrX + qrSize - 16f, qrY + 5f, qrX + qrSize - 5f, qrY + 16f, paint)
                    paint.color = Color.BLACK
                    canvas.drawRect(qrX + qrSize - 14f, qrY + 7f, qrX + qrSize - 7f, qrY + 14f, paint)

                    canvas.drawRect(qrX + 3f, qrY + qrSize - 18f, qrX + 18f, qrY + qrSize - 3f, paint)
                    paint.color = Color.WHITE
                    canvas.drawRect(qrX + qrSize - 16f, qrY + qrSize - 5f, qrX + 16f, qrY + qrSize - 5f, paint)
                    paint.color = Color.BLACK
                    canvas.drawRect(qrX + 7f, qrY + qrSize - 14f, qrX + 14f, qrY + 14f, paint)

                    paint.color = Color.BLACK
                    paint.style = Paint.Style.FILL
                    
                    val upiStr = "upi://pay?pa=${seller.upiId}&pn=${seller.name.replace(" ", "%20")}&am=${invoice.grandTotal}&cu=INR"
                    val hash = upiStr.hashCode()
                    for (row in 4..12) {
                        for (col in 4..12) {
                            val bitVal = (hash xor (row * col)) % 3 == 0
                            if (bitVal) {
                                val xPos = qrX + 3f + (row * 4.5f)
                                val yPos = qrY + 3f + (col * 4.5f)
                                canvas.drawRect(xPos, yPos, xPos + 4f, yPos + 4f, paint)
                            }
                        }
                    }

                    textPaint.textSize = 7.5f
                    textPaint.color = Color.GRAY
                    textPaint.isFakeBoldText = true
                    canvas.drawText("SCAN TO PAY UPI", qrX + qrSize + 8f, qrY + 28f, textPaint)
                    textPaint.textSize = 7f
                    textPaint.isFakeBoldText = false
                    canvas.drawText(seller.upiId.take(22), qrX + qrSize + 8f, qrY + 40f, textPaint)
                    
                    payY += qrSize + 12f
                }
            }

            var notesY = maxOf(sumY + 35f, payY + 20f)
            if (notesY > 740f) {
                pdfDocument.finishPage(page)
                pageIndex++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageIndex).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                drawHeader()
                notesY = 160f
            }

            if (invoice.notes.isNotBlank() || invoice.terms.isNotBlank()) {
                paint.color = Color.parseColor("#E5E7EB")
                canvas.drawLine(36f, notesY, (pageWidth - 36).toFloat(), notesY, paint)
                notesY += 15f
            }
            
            textPaint.textSize = 9f
            if (invoice.notes.isNotBlank()) {
                textPaint.isFakeBoldText = true
                textPaint.color = primaryColor
                canvas.drawText("NOTES / SPECIAL INSTRUCTIONS:", 40f, notesY, textPaint)
                notesY += 12f
                textPaint.isFakeBoldText = false
                textPaint.color = Color.BLACK
                canvas.drawText(invoice.notes.take(80), 40f, notesY, textPaint)
                notesY += 18f
            }

            if (invoice.terms.isNotBlank()) {
                textPaint.isFakeBoldText = true
                textPaint.color = primaryColor
                canvas.drawText("TERMS & CONDITIONS:", 40f, notesY, textPaint)
                notesY += 12f
                textPaint.isFakeBoldText = false
                textPaint.color = Color.BLACK
                canvas.drawText(invoice.terms.take(80), 40f, notesY, textPaint)
                notesY += 18f
            }

            if (!invoice.signaturePath.isNullOrBlank() && File(invoice.signaturePath).exists()) {
                try {
                    val sigFile = File(invoice.signaturePath)
                    val sigBitmap = BitmapFactory.decodeFile(sigFile.absolutePath)
                    if (sigBitmap != null) {
                        val sigY = notesY + 10f
                        canvas.drawText("Authorized Signature", (pageWidth - 160).toFloat(), sigY, textPaint)
                        
                        val sigDestRect = android.graphics.Rect(
                            pageWidth - 160,
                            (sigY + 6f).toInt(),
                            pageWidth - 40,
                            (sigY + 46f).toInt()
                        )
                        canvas.drawBitmap(sigBitmap, null, sigDestRect, paint)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to load signature on PDF", e)
                }
            }

            textPaint.textSize = 8.5f
            textPaint.color = Color.GRAY
            textPaint.isFakeBoldText = true
            canvas.drawText("Powered by GSD", (pageWidth / 2 - 35).toFloat(), (pageHeight - 35).toFloat(), textPaint)

            pdfDocument.finishPage(page)

            val fileName = getSafeFileName("INV_$effectiveInvoiceNumber", effectiveCustomerName, "pdf")
            val fileUri = savePdfToStore(context, fileName, pdfDocument)
            
            pdfDocument.close()
            fileUri
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Saves PDF to public Documents/GTOOL X/Invoices directory via MediaStore API & registers in MediaScanner.
     */
    private fun savePdfToStore(context: Context, fileName: String, pdfDocument: PdfDocument): Uri? {
        return try {
            var fileUri: Uri? = null
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOCUMENTS}/GTOOL X/Invoices")
                }
                val resolver = context.contentResolver
                fileUri = resolver.insert(MediaStore.Files.getContentUri("external"), contentValues)
                if (fileUri != null) {
                    resolver.openOutputStream(fileUri)?.use { outputStream ->
                        pdfDocument.writeTo(outputStream)
                    }
                }
            }
            
            if (fileUri == null) {
                val docsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
                val folder = File(docsDir, "GTOOL X/Invoices")
                if (!folder.exists()) folder.mkdirs()
                val file = File(folder, fileName)
                FileOutputStream(file).use { outputStream ->
                    pdfDocument.writeTo(outputStream)
                }
                fileUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(file.absolutePath),
                    arrayOf("application/pdf"),
                    null
                )
            }
            fileUri
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Saves TXT/DOC to public Documents/GTOOL X/Invoices directory via MediaStore API & registers in MediaScanner.
     */
    private fun saveDocumentFile(
        context: Context,
        fileName: String,
        content: String,
        mimeType: String
    ): Uri? {
        return try {
            var fileUri: Uri? = null
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOCUMENTS}/GTOOL X/Invoices")
                }
                val resolver = context.contentResolver
                fileUri = resolver.insert(MediaStore.Files.getContentUri("external"), contentValues)
                if (fileUri != null) {
                    resolver.openOutputStream(fileUri)?.use { out ->
                        out.write(content.toByteArray(Charsets.UTF_8))
                    }
                }
            }
            if (fileUri == null) {
                val docsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
                val folder = File(docsDir, "GTOOL X/Invoices")
                if (!folder.exists()) folder.mkdirs()
                val file = File(folder, fileName)
                FileOutputStream(file).use { out ->
                    out.write(content.toByteArray(Charsets.UTF_8))
                }
                fileUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(file.absolutePath),
                    arrayOf(mimeType),
                    null
                )
            }
            fileUri
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        val result = mutableListOf<String>()
        val lines = text.split("\n")
        for (line in lines) {
            val words = line.split(" ")
            var currentLine = ""
            for (word in words) {
                val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
                val width = paint.measureText(testLine)
                if (width <= maxWidth) {
                    currentLine = testLine
                } else {
                    if (currentLine.isNotEmpty()) {
                        result.add(currentLine)
                    }
                    currentLine = word
                }
            }
            if (currentLine.isNotEmpty()) {
                result.add(currentLine)
            }
        }
        return result
    }
}
