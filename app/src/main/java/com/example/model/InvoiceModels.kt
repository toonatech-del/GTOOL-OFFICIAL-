package com.example.model

import com.squareup.moshi.JsonClass
import java.util.UUID

@JsonClass(generateAdapter = true)
data class SellerDetails(
    val name: String = "",
    val ownerName: String = "",
    val phone: String = "",
    val email: String = "",
    val address: String = "",
    val city: String = "",
    val state: String = "",
    val pinCode: String = "",
    val gstin: String = "",
    val pan: String = "",
    val website: String = "",
    val upiId: String = "",
    val bankName: String = "",
    val accountNumber: String = "",
    val ifsc: String = "",
    val additionalNotes: String = ""
)

@JsonClass(generateAdapter = true)
data class CustomerDetails(
    val name: String = "",
    val companyName: String = "",
    val phone: String = "",
    val email: String = "",
    val billingAddress: String = "",
    val shippingAddress: String = "",
    val gstin: String = "",
    val pan: String = "",
    val notes: String = ""
)

@JsonClass(generateAdapter = true)
data class InvoiceItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val description: String = "",
    val sku: String = "",
    val hsnSac: String = "",
    val quantity: Double = 1.0,
    val unit: String = "PCS",
    val unitPrice: Double = 0.0,
    val discountPercent: Double = 0.0,
    val discountAmount: Double = 0.0,
    val isGstInclusive: Boolean = false,
    val gstRatePercent: Double = 18.0, // e.g. 18.0
    val cgstRatePercent: Double = 9.0,
    val sgstRatePercent: Double = 9.0,
    val igstRatePercent: Double = 0.0,
    val cessRatePercent: Double = 0.0,
    val cgstAmount: Double = 0.0,
    val sgstAmount: Double = 0.0,
    val igstAmount: Double = 0.0,
    val cessAmount: Double = 0.0,
    val lineTotal: Double = 0.0
)

@JsonClass(generateAdapter = true)
data class InvoicePaymentDetails(
    val method: String = "Cash",
    val upiId: String = "",
    val bankName: String = "",
    val accountNumber: String = "",
    val ifsc: String = "",
    val instructions: String = "",
    val terms: String = ""
)
