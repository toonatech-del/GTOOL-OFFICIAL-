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

    companion object {
        private const val MAX_ENTRIES = 1000
        private const val MAX_FILE_SIZE = 50L * 1024 * 1024 // 50 MB
        private const val MAX_TOTAL_SIZE = 250L * 1024 * 1024 // 250 MB
    }

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
                    val jsonBytes = json.toByteArray(Charsets.UTF_8)
                    val jsonEntry = ZipEntry("backup_data.json").apply {
                        size = jsonBytes.size.toLong()
                    }
                    zos.putNextEntry(jsonEntry)
                    zos.write(jsonBytes)
                    zos.closeEntry()

                    // 2. Write Internal Files (thumbnails, etc.)
                    val internalFilesDir = context.filesDir
                    packDirectoryToZip(internalFilesDir, "internal_files/", zos)

                    // 3. Write Media Files (images, PDFs from URIs)
                    fullData.memories.forEach { item ->
                        // Backup Image
                        if (!item.imageUri.isNullOrBlank()) {
                            val mediaUri = Uri.parse(item.imageUri)
                            if (mediaUri.scheme == "content" || mediaUri.scheme == "file") {
                                backupMediaFile(mediaUri, "media/${sanitizeFilename(item.id)}_image", zos)
                            }
                        }
                        // Backup PDF
                        if (!item.driveFileId.isNullOrBlank() && (item.type == "pdf" || item.driveFileId?.contains(".pdf") == true)) {
                            val mediaUri = Uri.parse(item.driveFileId)
                            if (mediaUri.scheme == "content" || mediaUri.scheme == "file") {
                                backupMediaFile(mediaUri, "media/${sanitizeFilename(item.id)}_pdf", zos)
                            }
                        }
                    }
                }
            } ?: return@withContext Result.failure(Exception("Could not open output stream for backup export"))

            val totalItems = fullData.memories.size + fullData.invoices.size
            Result.success(totalItems)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun sanitizeFilename(input: String): String {
        return input.replace(Regex("[^a-zA-Z0-9_-]"), "_")
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
                if (file.name != "vault_media") {
                    packDirectoryToZip(file, "$zipPathPrefix${file.name}/", zos)
                }
            } else {
                try {
                    val entryName = "$zipPathPrefix${file.name}"
                    zos.putNextEntry(ZipEntry(entryName))
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

    /**
     * Restores backup archive with strict Zip Slip protection, bounds checking, and canonical path validation.
     */
    suspend fun restoreBackup(uri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        try {
            var totalRestored = 0
            var backupData: FullBackupData? = null
            val restoredMediaPaths = mutableMapOf<String, String>()

            val baseDir = context.filesDir.canonicalFile
            val mediaDir = File(baseDir, "vault_media").canonicalFile
            if (!mediaDir.exists()) mediaDir.mkdirs()

            var entryCount = 0
            var totalExtractedBytes = 0L

            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                ZipInputStream(BufferedInputStream(inputStream)).use { zis ->
                    var entry: ZipEntry? = zis.nextEntry
                    while (entry != null) {
                        entryCount++
                        if (entryCount > MAX_ENTRIES) {
                            throw SecurityException("Backup archive exceeds maximum permitted entries limit ($MAX_ENTRIES)")
                        }

                        val entryName = entry.name
                        // Reject directory traversal attempts in entry name
                        if (entryName.contains("..") || entryName.startsWith("/") || entryName.startsWith("\\")) {
                            throw SecurityException("Malicious ZIP entry path detected: $entryName")
                        }

                        when {
                            entryName == "backup_data.json" -> {
                                val json = zis.bufferedReader(Charsets.UTF_8).readText()
                                totalExtractedBytes += json.toByteArray(Charsets.UTF_8).size
                                if (totalExtractedBytes > MAX_TOTAL_SIZE) {
                                    throw SecurityException("Backup archive exceeds maximum total decompressed size")
                                }
                                backupData = adapter.fromJson(json)
                            }
                            entryName.startsWith("internal_files/") -> {
                                val relativePath = entryName.removePrefix("internal_files/")
                                if (relativePath.isNotEmpty() && !entry.isDirectory) {
                                    val destFile = File(baseDir, relativePath).canonicalFile
                                    // Zip Slip check
                                    if (!destFile.path.startsWith(baseDir.path + File.separator) && destFile.path != baseDir.path) {
                                        throw SecurityException("Zip Slip path traversal blocked for: $entryName")
                                    }
                                    destFile.parentFile?.mkdirs()
                                    var fileSize = 0L
                                    FileOutputStream(destFile).use { fos ->
                                        val buffer = ByteArray(8192)
                                        var bytesRead: Int
                                        while (zis.read(buffer).also { bytesRead = it } != -1) {
                                            fileSize += bytesRead
                                            totalExtractedBytes += bytesRead
                                            if (fileSize > MAX_FILE_SIZE || totalExtractedBytes > MAX_TOTAL_SIZE) {
                                                throw SecurityException("Extracted file exceeds safe storage quota")
                                            }
                                            fos.write(buffer, 0, bytesRead)
                                        }
                                    }
                                }
                            }
                            entryName.startsWith("media/") -> {
                                val fileName = sanitizeFilename(entryName.removePrefix("media/"))
                                if (fileName.isNotEmpty() && !entry.isDirectory) {
                                    val destFile = File(mediaDir, fileName).canonicalFile
                                    // Zip Slip check
                                    if (!destFile.path.startsWith(mediaDir.path + File.separator) && destFile.path != mediaDir.path) {
                                        throw SecurityException("Zip Slip path traversal blocked for media: $entryName")
                                    }
                                    var fileSize = 0L
                                    FileOutputStream(destFile).use { fos ->
                                        val buffer = ByteArray(8192)
                                        var bytesRead: Int
                                        while (zis.read(buffer).also { bytesRead = it } != -1) {
                                            fileSize += bytesRead
                                            totalExtractedBytes += bytesRead
                                            if (fileSize > MAX_FILE_SIZE || totalExtractedBytes > MAX_TOTAL_SIZE) {
                                                throw SecurityException("Extracted media exceeds safe storage quota")
                                            }
                                            fos.write(buffer, 0, bytesRead)
                                        }
                                    }
                                    restoredMediaPaths[entryName] = Uri.fromFile(destFile).toString()
                                }
                            }
                        }
                        zis.closeEntry()
                        entry = zis.nextEntry
                    }
                }
            } ?: return@withContext Result.failure(Exception("Could not open input stream for restore"))

            val data = backupData ?: return@withContext Result.failure(Exception("Failed to read valid backup metadata from archive"))

            val vaultDb = AppDatabase.getInstance(context)
            val invoiceDb = InvoiceDatabase.getInstance(context)

            // Restore DB records
            data.memories.forEach { entity ->
                val updatedEntity = entity.copy(
                    imageUri = restoredMediaPaths["media/${sanitizeFilename(entity.id)}_image"] ?: entity.imageUri,
                    driveFileId = restoredMediaPaths["media/${sanitizeFilename(entity.id)}_pdf"] ?: entity.driveFileId
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
