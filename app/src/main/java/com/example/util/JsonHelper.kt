package com.example.util

import com.example.model.CustomerDetails
import com.example.model.InvoiceItem
import com.example.model.SellerDetails
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

object JsonHelper {

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    fun toJson(seller: SellerDetails): String {
        return moshi.adapter(SellerDetails::class.java).toJson(seller)
    }

    fun fromSellerJson(json: String): SellerDetails {
        return try {
            moshi.adapter(SellerDetails::class.java).fromJson(json) ?: SellerDetails()
        } catch (e: Exception) {
            SellerDetails()
        }
    }

    fun toJson(customer: CustomerDetails): String {
        return moshi.adapter(CustomerDetails::class.java).toJson(customer)
    }

    fun fromCustomerJson(json: String): CustomerDetails {
        return try {
            moshi.adapter(CustomerDetails::class.java).fromJson(json) ?: CustomerDetails()
        } catch (e: Exception) {
            CustomerDetails()
        }
    }

    fun toJson(items: List<InvoiceItem>): String {
        val type = Types.newParameterizedType(List::class.java, InvoiceItem::class.java)
        return moshi.adapter<List<InvoiceItem>>(type).toJson(items)
    }

    fun fromItemsJson(json: String): List<InvoiceItem> {
        return try {
            val type = Types.newParameterizedType(List::class.java, InvoiceItem::class.java)
            moshi.adapter<List<InvoiceItem>>(type).fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
