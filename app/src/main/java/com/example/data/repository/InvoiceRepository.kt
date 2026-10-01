package com.example.data.repository

import android.content.Context
import com.example.data.local.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONObject

class InvoiceRepository(context: Context) {

    private val db = InvoiceDatabase.getInstance(context)
    private val invoiceDao = db.invoiceDao()
    private val sellerProfileDao = db.sellerProfileDao()
    private val customerProfileDao = db.customerProfileDao()
    private val productDao = db.productDao()
    private val invoiceDraftDao = db.invoiceDraftDao()
    private val universalSearchRepo = UniversalSearchRepository(context)

    // Invoices
    val allInvoices: Flow<List<InvoiceEntity>> = invoiceDao.getAllInvoices()

    private suspend fun indexInvoice(invoice: InvoiceEntity) {
        val sellerObj = try { JSONObject(invoice.sellerDetailsJson) } catch(e: Exception) { JSONObject() }
        val buyerObj = try { JSONObject(invoice.customerDetailsJson) } catch(e: Exception) { JSONObject() }
        val itemsArray = try { org.json.JSONArray(invoice.itemsJson) } catch(e: Exception) { org.json.JSONArray() }

        val sellerName = sellerObj.optString("name", "")
        val buyerName = buyerObj.optString("name", "")
        val sellerGstin = sellerObj.optString("gstin", "")
        val buyerGstin = buyerObj.optString("gstin", "")
        val sellerPhone = sellerObj.optString("phone", "")
        val buyerPhone = buyerObj.optString("phone", "")

        val itemsSummary = StringBuilder()
        for (i in 0 until itemsArray.length()) {
            val item = itemsArray.optJSONObject(i) ?: continue
            if (i > 0) itemsSummary.append(", ")
            itemsSummary.append(item.optString("name", ""))
            val hsn = item.optString("hsnSac", "")
            if (hsn.isNotBlank()) {
                itemsSummary.append(" (HSN: ").append(hsn).append(")")
            }
        }

        val content = buildString {
            append("Invoice Number: ").append(invoice.invoiceNumber).append("\n")
            append("Seller: ").append(sellerName).append("\n")
            append("Buyer: ").append(buyerName).append("\n")
            append("Seller GSTIN: ").append(sellerGstin).append("\n")
            append("Buyer GSTIN: ").append(buyerGstin).append("\n")
            append("Seller Phone: ").append(sellerPhone).append("\n")
            append("Buyer Phone: ").append(buyerPhone).append("\n")
            append("Items: ").append(itemsSummary.toString()).append("\n")
            append("Grand Total: ").append(invoice.grandTotal).append("\n")
            append("Balance Due: ").append(invoice.balanceDue).append("\n")
            if (invoice.notes.isNotBlank()) append("Notes: ").append(invoice.notes).append("\n")
            if (invoice.terms.isNotBlank()) append("Terms: ").append(invoice.terms).append("\n")
        }

        universalSearchRepo.insertOrUpdate(
            id = "invoice-${invoice.id}",
            module = "Invoice",
            title = "Invoice #${invoice.invoiceNumber} - $buyerName",
            contentText = content,
            metadataJson = JSONObject().apply {
                put("invoice_number", invoice.invoiceNumber)
                put("seller_name", sellerName)
                put("buyer_name", buyerName)
                put("grand_total", invoice.grandTotal)
                put("seller_gstin", sellerGstin)
                put("buyer_gstin", buyerGstin)
                put("seller_phone", sellerPhone)
                put("buyer_phone", buyerPhone)
            }.toString(),
            imageUri = invoice.originalBillImageUri
        )
    }

    suspend fun saveInvoice(invoice: InvoiceEntity) = withContext(Dispatchers.IO) {
        invoiceDao.insertInvoice(invoice)
        try { indexInvoice(invoice) } catch (e: Exception) { e.printStackTrace() }
    }

    suspend fun updateInvoice(invoice: InvoiceEntity) = withContext(Dispatchers.IO) {
        invoiceDao.updateInvoice(invoice)
        try { indexInvoice(invoice) } catch (e: Exception) { e.printStackTrace() }
    }

    suspend fun getInvoiceById(id: String): InvoiceEntity? = withContext(Dispatchers.IO) {
        invoiceDao.getInvoiceById(id)
    }

    suspend fun deleteInvoiceById(id: String) = withContext(Dispatchers.IO) {
        invoiceDao.deleteInvoiceById(id)
        universalSearchRepo.delete("invoice-$id")
    }

    suspend fun getAllInvoicesList(): List<InvoiceEntity> = withContext(Dispatchers.IO) {
        invoiceDao.getAllInvoicesList()
    }

    // Seller Profiles
    val allSellerProfiles: Flow<List<SellerProfileEntity>> = sellerProfileDao.getAllProfiles()

    suspend fun saveSellerProfile(profile: SellerProfileEntity) = withContext(Dispatchers.IO) {
        if (profile.isDefault) {
            sellerProfileDao.clearDefaultProfiles()
        }
        sellerProfileDao.insertProfile(profile)
    }

    suspend fun getSellerProfileById(id: String): SellerProfileEntity? = withContext(Dispatchers.IO) {
        sellerProfileDao.getProfileById(id)
    }

    suspend fun getDefaultSellerProfile(): SellerProfileEntity? = withContext(Dispatchers.IO) {
        sellerProfileDao.getDefaultProfile()
    }

    suspend fun setDefaultSellerProfile(id: String) = withContext(Dispatchers.IO) {
        sellerProfileDao.clearDefaultProfiles()
        sellerProfileDao.setDefaultProfile(id)
    }

    suspend fun deleteSellerProfileById(id: String) = withContext(Dispatchers.IO) {
        sellerProfileDao.deleteProfileById(id)
    }

    // Customer Profiles
    val allCustomers: Flow<List<CustomerProfileEntity>> = customerProfileDao.getAllCustomers()

    suspend fun saveCustomer(customer: CustomerProfileEntity) = withContext(Dispatchers.IO) {
        customerProfileDao.insertCustomer(customer)
    }

    suspend fun getCustomerById(id: String): CustomerProfileEntity? = withContext(Dispatchers.IO) {
        customerProfileDao.getCustomerById(id)
    }

    suspend fun getAllCustomersList(): List<CustomerProfileEntity> = withContext(Dispatchers.IO) {
        customerProfileDao.getAllCustomersList()
    }

    suspend fun deleteCustomerById(id: String) = withContext(Dispatchers.IO) {
        customerProfileDao.deleteCustomerById(id)
    }

    // Products
    val allProducts: Flow<List<ProductEntity>> = productDao.getAllProducts()

    suspend fun saveProduct(product: ProductEntity) = withContext(Dispatchers.IO) {
        productDao.insertProduct(product)
    }

    suspend fun getProductById(id: String): ProductEntity? = withContext(Dispatchers.IO) {
        productDao.getProductById(id)
    }

    suspend fun getAllProductsList(): List<ProductEntity> = withContext(Dispatchers.IO) {
        productDao.getAllProductsList()
    }

    suspend fun deleteProductById(id: String) = withContext(Dispatchers.IO) {
        productDao.deleteProductById(id)
    }

    // Invoice Draft
    suspend fun saveDraft(draft: InvoiceDraftEntity) = withContext(Dispatchers.IO) {
        invoiceDraftDao.saveDraft(draft)
    }

    suspend fun getDraft(): InvoiceDraftEntity? = withContext(Dispatchers.IO) {
        invoiceDraftDao.getDraft()
    }

    suspend fun deleteDraft() = withContext(Dispatchers.IO) {
        invoiceDraftDao.deleteDraft()
    }
}
