package com.example.util

import android.content.Context
import android.net.Uri
import com.example.data.local.*
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class BackupManager(private val context: Context) {

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
    private val adapter = moshi.adapter(FullBackupData::class.java)

    suspend fun exportBackup(uri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        try {
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

            val json = adapter.toJson(fullData)

            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                ZipOutputStream(BufferedOutputStream(outputStream)).use { zos ->
                    // 1. Write JSON entry
                    zos.putNextEntry(ZipEntry("backup_data.json"))
                    zos.write(json.toByteArray())
                    zos.closeEntry()

                    // 2. Write Internal Files (thumbnails, etc.)
                    val internalFilesDir = context.filesDir
                    packDirectoryToZip(internalFilesDir, "internal_files/", zos)

                    // 3. Write Media Files (images, PDFs from URIs)
                    fullData.memories.forEach { item ->
                        // Backup Image
                        if (!item.imageUri.isNullOrBlank()) {
                            val uri = Uri.parse(item.imageUri)
                            if (uri.scheme == "content" || uri.scheme == "file") {
                                backupMediaFile(uri, "media/${item.id}_image", zos)
                            }
                        }
                        // Backup PDF
                        if (!item.driveFileId.isNullOrBlank() && (item.type == "pdf" || item.driveFileId?.contains(".pdf") == true)) {
                            val uri = Uri.parse(item.driveFileId)
                            if (uri.scheme == "content" || uri.scheme == "file") {
                                backupMediaFile(uri, "media/${item.id}_pdf", zos)
                            }
                        }
                    }
                }
            } ?: return@withContext Result.failure(Exception("Could not open output stream"))

            val totalItems = fullData.memories.size + fullData.invoices.size
            Result.success(totalItems)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun backupMediaFile(uri: Uri, zipName: String, zos: ZipOutputStream) {
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                zos.putNextEntry(ZipEntry(zipName))
                inputStream.copyTo(zos)
                zos.closeEntry()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun packDirectoryToZip(directory: File, zipPathPrefix: String, zos: ZipOutputStream) {
        directory.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                if (file.name != "vault_media") { // Don't pack the media folder we're restoring to if it exists
                    packDirectoryToZip(file, "$zipPathPrefix${file.name}/", zos)
                }
            } else {
                try {
                    zos.putNextEntry(ZipEntry("$zipPathPrefix${file.name}"))
                    FileInputStream(file).use { fis ->
                        fis.copyTo(zos)
                    }
                    zos.closeEntry()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    suspend fun restoreBackup(uri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        try {
            var totalRestored = 0
            var backupData: FullBackupData? = null
            val restoredMediaPaths = mutableMapOf<String, String>()

            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                ZipInputStream(BufferedInputStream(inputStream)).use { zis ->
                    var entry: ZipEntry? = zis.nextEntry
                    while (entry != null) {
                        when {
                            entry.name == "backup_data.json" -> {
                                val json = zis.bufferedReader().readText()
                                backupData = adapter.fromJson(json)
                            }
                            entry.name.startsWith("internal_files/") -> {
                                val relativePath = entry.name.removePrefix("internal_files/")
                                if (relativePath.isNotEmpty()) {
                                    val destFile = File(context.filesDir, relativePath)
                                    destFile.parentFile?.mkdirs()
                                    FileOutputStream(destFile).use { fos ->
                                        zis.copyTo(fos)
                                    }
                                }
                            }
                            entry.name.startsWith("media/") -> {
                                val fileName = entry.name.removePrefix("media/")
                                if (fileName.isNotEmpty()) {
                                    val mediaDir = File(context.filesDir, "vault_media").apply { mkdirs() }
                                    val destFile = File(mediaDir, fileName)
                                    FileOutputStream(destFile).use { fos ->
                                        zis.copyTo(fos)
                                    }
                                    restoredMediaPaths[entry.name] = Uri.fromFile(destFile).toString()
                                }
                            }
                        }
                        zis.closeEntry()
                        entry = zis.nextEntry
                    }
                }
            } ?: return@withContext Result.failure(Exception("Could not open input stream"))

            val data = backupData ?: return@withContext Result.failure(Exception("Failed to parse backup data from ZIP"))
            
            val vaultDb = AppDatabase.getInstance(context)
            val invoiceDb = InvoiceDatabase.getInstance(context)

            // Restore DB records
            data.memories.forEach { entity ->
                // Update file paths if media was restored
                val updatedEntity = entity.copy(
                    imageUri = restoredMediaPaths["media/${entity.id}_image"] ?: entity.imageUri,
                    driveFileId = restoredMediaPaths["media/${entity.id}_pdf"] ?: entity.driveFileId
                )
                
                vaultDb.memoryDao().insertMemory(updatedEntity)
                vaultDb.memoryDao().insertFts(MemoryFtsEntity(id = entity.id, title = entity.title, extractedText = entity.extractedText))
            }

            invoiceDb.invoiceDao().insertInvoices(data.invoices)
            invoiceDb.sellerProfileDao().insertProfiles(data.sellerProfiles)
            invoiceDb.customerProfileDao().insertCustomers(data.customerProfiles)
            invoiceDb.productDao().insertProducts(data.products)
            vaultDb.reminderDao().insertReminders(data.reminders)
            vaultDb.notificationDao().insertNotifications(data.notifications)
            
            totalRestored = data.memories.size + data.invoices.size
            Result.success(totalRestored)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
