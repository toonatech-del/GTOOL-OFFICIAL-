package com.example.data.local

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

// ==========================================
// ROOM ENTITIES
// ==========================================

@Entity(tableName = "invoices")
data class InvoiceEntity(
    @PrimaryKey val id: String,
    val invoiceNumber: String,
    val invoiceDate: String,
    val dueDate: String,
    val paymentStatus: String, // "Paid", "Unpaid", "Partially Paid", "Overdue", "Draft"
    val paymentMethod: String, // "Cash", "UPI", "Bank Transfer", "Card", "Cheque", "Other"
    val currency: String = "₹",
    val placeOfSupply: String = "",
    val reverseCharge: Boolean = false,
    val referenceNo: String = "",
    val sellerDetailsJson: String, // Holds Seller details (Name, Address, phone, GSTIN, Bank, etc.)
    val customerDetailsJson: String, // Holds Customer details (Name, shipping/billing, GSTIN, etc.)
    val itemsJson: String, // Holds JSON array of invoice items
    val notes: String = "",
    val terms: String = "",
    val subtotal: Double = 0.0,
    val totalDiscount: Double = 0.0,
    val taxableAmount: Double = 0.0,
    val cgst: Double = 0.0,
    val sgst: Double = 0.0,
    val igst: Double = 0.0,
    val cess: Double = 0.0,
    val additionalCharges: Double = 0.0,
    val roundOff: Double = 0.0,
    val grandTotal: Double = 0.0,
    val amountPaid: Double = 0.0,
    val balanceDue: Double = 0.0,
    val signaturePath: String? = null,
    val signatureType: String = "none", // "draw", "image", "none"
    val source: String = "Manual", // "Manual" or "OCR"
    val templateName: String = "Classic", // "Classic", "Modern", "Minimal", "Business", "Retail", "Service", "GST-focused"
    val isOcrUncertain: Boolean = false,
    val originalBillImageUri: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "seller_profiles")
data class SellerProfileEntity(
    @PrimaryKey val id: String,
    val name: String,
    val logoUri: String? = null,
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
    val additionalNotes: String = "",
    val isDefault: Boolean = false
)

@Entity(tableName = "customer_profiles")
data class CustomerProfileEntity(
    @PrimaryKey val id: String,
    val name: String,
    val companyName: String = "",
    val phone: String = "",
    val email: String = "",
    val billingAddress: String = "",
    val shippingAddress: String = "",
    val gstin: String = "",
    val pan: String = "",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String = "",
    val sku: String = "",
    val hsnSac: String = "",
    val unitPrice: Double = 0.0,
    val unit: String = "PCS",
    val discountPercent: Double = 0.0,
    val gstRate: Double = 18.0, // e.g. 18.0 for 18%
    val cessRate: Double = 0.0
)

@Entity(tableName = "invoice_drafts")
data class InvoiceDraftEntity(
    @PrimaryKey val id: String = "active_draft",
    val invoiceNumber: String = "",
    val invoiceDate: String = "",
    val dueDate: String = "",
    val paymentStatus: String = "Draft",
    val paymentMethod: String = "Cash",
    val currency: String = "₹",
    val placeOfSupply: String = "",
    val reverseCharge: Boolean = false,
    val referenceNo: String = "",
    val sellerDetailsJson: String = "{}",
    val customerDetailsJson: String = "{}",
    val itemsJson: String = "[]",
    val notes: String = "",
    val terms: String = "",
    val subtotal: Double = 0.0,
    val totalDiscount: Double = 0.0,
    val taxableAmount: Double = 0.0,
    val cgst: Double = 0.0,
    val sgst: Double = 0.0,
    val igst: Double = 0.0,
    val cess: Double = 0.0,
    val additionalCharges: Double = 0.0,
    val roundOff: Double = 0.0,
    val grandTotal: Double = 0.0,
    val amountPaid: Double = 0.0,
    val balanceDue: Double = 0.0,
    val signaturePath: String? = null,
    val signatureType: String = "none",
    val source: String = "Manual",
    val templateName: String = "Classic",
    val originalBillImageUri: String? = null,
    val updatedAt: Long = System.currentTimeMillis()
)

// ==========================================
// DAOS
// ==========================================

@Dao
interface InvoiceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoice(invoice: InvoiceEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertInvoices(invoices: List<InvoiceEntity>)

    @Update
    suspend fun updateInvoice(invoice: InvoiceEntity)

    @Query("SELECT * FROM invoices ORDER BY createdAt DESC")
    fun getAllInvoices(): Flow<List<InvoiceEntity>>

    @Query("SELECT * FROM invoices ORDER BY createdAt DESC")
    suspend fun getAllInvoicesList(): List<InvoiceEntity>

    @Query("SELECT * FROM invoices WHERE id = :id LIMIT 1")
    suspend fun getInvoiceById(id: String): InvoiceEntity?

    @Query("DELETE FROM invoices WHERE id = :id")
    suspend fun deleteInvoiceById(id: String)
}

@Dao
interface SellerProfileDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: SellerProfileEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertProfiles(profiles: List<SellerProfileEntity>)

    @Query("SELECT * FROM seller_profiles ORDER BY name ASC")
    fun getAllProfiles(): Flow<List<SellerProfileEntity>>

    @Query("SELECT * FROM seller_profiles ORDER BY name ASC")
    suspend fun getAllProfilesList(): List<SellerProfileEntity>

    @Query("SELECT * FROM seller_profiles WHERE id = :id LIMIT 1")
    suspend fun getProfileById(id: String): SellerProfileEntity?

    @Query("SELECT * FROM seller_profiles WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefaultProfile(): SellerProfileEntity?

    @Query("UPDATE seller_profiles SET isDefault = 0")
    suspend fun clearDefaultProfiles()

    @Query("UPDATE seller_profiles SET isDefault = 1 WHERE id = :id")
    suspend fun setDefaultProfile(id: String)

    @Query("DELETE FROM seller_profiles WHERE id = :id")
    suspend fun deleteProfileById(id: String)
}

@Dao
interface CustomerProfileDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: CustomerProfileEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCustomers(customers: List<CustomerProfileEntity>)

    @Query("SELECT * FROM customer_profiles ORDER BY name ASC")
    fun getAllCustomers(): Flow<List<CustomerProfileEntity>>

    @Query("SELECT * FROM customer_profiles ORDER BY name ASC")
    suspend fun getAllCustomersList(): List<CustomerProfileEntity>

    @Query("SELECT * FROM customer_profiles WHERE id = :id LIMIT 1")
    suspend fun getCustomerById(id: String): CustomerProfileEntity?

    @Query("DELETE FROM customer_profiles WHERE id = :id")
    suspend fun deleteCustomerById(id: String)
}

@Dao
interface ProductDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertProducts(products: List<ProductEntity>)

    @Query("SELECT * FROM products ORDER BY name ASC")
    fun getAllProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products ORDER BY name ASC")
    suspend fun getAllProductsList(): List<ProductEntity>

    @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
    suspend fun getProductById(id: String): ProductEntity?

    @Query("DELETE FROM products WHERE id = :id")
    suspend fun deleteProductById(id: String)
}

@Dao
interface InvoiceDraftDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveDraft(draft: InvoiceDraftEntity)

    @Query("SELECT * FROM invoice_drafts WHERE id = :id LIMIT 1")
    suspend fun getDraft(id: String = "active_draft"): InvoiceDraftEntity?

    @Query("DELETE FROM invoice_drafts WHERE id = :id")
    suspend fun deleteDraft(id: String = "active_draft")
}

// ==========================================
// ROOM DATABASE
// ==========================================

@Database(
    entities = [
        InvoiceEntity::class,
        SellerProfileEntity::class,
        CustomerProfileEntity::class,
        ProductEntity::class,
        InvoiceDraftEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class InvoiceDatabase : RoomDatabase() {
    abstract fun invoiceDao(): InvoiceDao
    abstract fun sellerProfileDao(): SellerProfileDao
    abstract fun customerProfileDao(): CustomerProfileDao
    abstract fun productDao(): ProductDao
    abstract fun invoiceDraftDao(): InvoiceDraftDao

    companion object {
        @Volatile
        private var INSTANCE: InvoiceDatabase? = null

        fun getInstance(context: Context): InvoiceDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    InvoiceDatabase::class.java,
                    "gtool_invoices.db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
