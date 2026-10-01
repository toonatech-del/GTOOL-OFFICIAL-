package com.example.util

import android.content.Context
import android.os.Environment
import com.example.data.local.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class FullBackupData(
    val memories: List<MemoryEntity> = emptyList(),
    val invoices: List<InvoiceEntity> = emptyList(),
    val sellerProfiles: List<SellerProfileEntity> = emptyList(),
    val customerProfiles: List<CustomerProfileEntity> = emptyList(),
    val products: List<ProductEntity> = emptyList(),
    val reminders: List<ReminderEntity> = emptyList(),
    val notifications: List<NotificationEntity> = emptyList()
)

data class BackupInfo(
    val exists: Boolean,
    val filePath: String,
    val lastBackupDate: String,
    val fileSizeBytes: Long,
    val itemCount: Int
)

data class BackupResult(
    val success: Boolean,
    val filePath: String,
    val itemCount: Int,
    val errorMessage: String? = null
)

object BackupRestoreManager {

    private const val PREFS_NAME = "gtool_backup_prefs"
    private const val KEY_LAST_BACKUP = "last_backup_timestamp"
    private const val KEY_AUTO_RESTORE_DONE = "auto_restore_done"

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val backupAdapter = moshi.adapter(FullBackupData::class.java)

    fun getPublicGToolFolder(context: Context, subFolder: String = ""): File {
        val root = try {
            val docs = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            val gtool = File(docs, "GTOOL X")
            if (!gtool.exists()) gtool.mkdirs()
            gtool
        } catch (e: Exception) {
            val fallback = File(context.getExternalFilesDir(null), "GTOOL X")
            if (!fallback.exists()) fallback.mkdirs()
            fallback
        }

        return if (subFolder.isNotBlank()) {
            val sub = File(root, subFolder)
            if (!sub.exists()) sub.mkdirs()
            sub
        } else root
    }

    private fun getBackupFile(context: Context): File {
        val backupDir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "GTOOL X/Backups")
        if (!backupDir.exists()) backupDir.mkdirs()
        return File(backupDir, "gtool_vault_backup.json")
    }

    private fun getDbBackupFile(context: Context, dbName: String): File {
        val backupDir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "GTOOL X/Backups")
        if (!backupDir.exists()) backupDir.mkdirs()
        return File(backupDir, "$dbName.bak")
    }

    fun getBackupInfo(context: Context): BackupInfo {
        val file = getBackupFile(context)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastTimestamp = prefs.getString(KEY_LAST_BACKUP, "Never") ?: "Never"

        var itemCount = 0
        if (file.exists() && file.length() > 0) {
            try {
                val json = file.readText()
                val data = backupAdapter.fromJson(json)
                itemCount = (data?.memories?.size ?: 0) + (data?.invoices?.size ?: 0)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return BackupInfo(
            exists = file.exists() && file.length() > 0,
            filePath = file.absolutePath,
            lastBackupDate = lastTimestamp,
            fileSizeBytes = if (file.exists()) file.length() else 0L,
            itemCount = itemCount
        )
    }

    fun checkForExistingData(context: Context): Boolean {
        val backupFile = getBackupFile(context)
        if (backupFile.exists() && backupFile.length() > 0) return true

        val docsDir = getPublicGToolFolder(context)
        val hasFiles = docsDir.walkTopDown().any { it.isFile && it.name != ".nomedia" }
        return hasFiles
    }

    suspend fun createLocalBackup(context: Context): BackupResult = withContext(Dispatchers.IO) {
        try {
            val file = getBackupFile(context)
            val vaultDb = AppDatabase.getInstance(context)
            val invoiceDb = InvoiceDatabase.getInstance(context)

            val fullData = FullBackupData(
                memories = vaultDb.memoryDao().getAllMemoriesList(),
                invoices = invoiceDb.invoiceDao().getAllInvoicesList(),
                sellerProfiles = invoiceDb.sellerProfileDao().getAllProfilesList(),
                customerProfiles = invoiceDb.customerProfileDao().getAllCustomersList(),
                products = invoiceDb.productDao().getAllProductsList(),
                reminders = vaultDb.reminderDao().getAllRemindersList(),
                notifications = vaultDb.notificationDao().getAllNotificationsList()
            )

            val json = backupAdapter.toJson(fullData)
            file.writeText(json)

            // Also backup raw .db files for extra safety
            backupRawDb(context, "gsdcall_ai_vault.db")
            backupRawDb(context, "gtool_invoices.db")

            val nowStr = SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault()).format(Date())
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_LAST_BACKUP, nowStr)
                .apply()

            BackupResult(
                success = true,
                filePath = file.absolutePath,
                itemCount = fullData.memories.size + fullData.invoices.size
            )
        } catch (e: Exception) {
            e.printStackTrace()
            BackupResult(
                success = false,
                filePath = getBackupFile(context).absolutePath,
                itemCount = 0,
                errorMessage = e.message ?: "Backup failed"
            )
        }
    }

    private fun backupRawDb(context: Context, dbName: String) {
        try {
            val dbFile = context.getDatabasePath(dbName)
            if (dbFile.exists()) {
                val backupFile = getDbBackupFile(context, dbName)
                dbFile.copyTo(backupFile, overwrite = true)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun restoreBackup(context: Context): Int = withContext(Dispatchers.IO) {
        try {
            val file = getBackupFile(context)
            if (!file.exists()) return@withContext 0

            val json = file.readText()
            val data = backupAdapter.fromJson(json) ?: return@withContext 0

            val vaultDb = AppDatabase.getInstance(context)
            val invoiceDb = InvoiceDatabase.getInstance(context)

            var count = 0

            data.memories.forEach { entity ->
                vaultDb.memoryDao().insertMemory(entity)
                vaultDb.memoryDao().insertFts(MemoryFtsEntity(id = entity.id, title = entity.title, extractedText = entity.extractedText))
                count++
            }

            data.invoices.forEach { invoiceDb.invoiceDao().insertInvoice(it); count++ }
            data.sellerProfiles.forEach { invoiceDb.sellerProfileDao().insertProfile(it) }
            data.customerProfiles.forEach { invoiceDb.customerProfileDao().insertCustomer(it) }
            data.products.forEach { invoiceDb.productDao().insertProduct(it) }
            data.reminders.forEach { vaultDb.reminderDao().insertReminder(it) }
            data.notifications.forEach { vaultDb.notificationDao().insertNotification(it) }

            count
        } catch (e: Exception) {
            e.printStackTrace()
            0
        }
    }

    /**
     * Seamlessly restores data if a backup exists and app was just installed/reset.
     */
    suspend fun autoRestoreIfNeeded(context: Context): Boolean = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_AUTO_RESTORE_DONE, false)) return@withContext false

        val vaultDb = AppDatabase.getInstance(context)
        val memories = vaultDb.memoryDao().getAllMemoriesList()
        
        if (memories.isEmpty() && checkForExistingData(context)) {
            val restoredCount = restoreBackup(context)
            if (restoredCount > 0) {
                prefs.edit().putBoolean(KEY_AUTO_RESTORE_DONE, true).apply()
                return@withContext true
            }
        }
        
        prefs.edit().putBoolean(KEY_AUTO_RESTORE_DONE, true).apply()
        false
    }
}
