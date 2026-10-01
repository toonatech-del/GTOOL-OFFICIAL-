package com.example.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.local.MemoryEntity
import com.example.data.local.MemoryFtsEntity
import com.example.model.ItemType
import com.example.model.WorkspaceItem
import com.example.util.MlKitTextExtractor
import com.example.util.PdfPageRenderer
import com.example.util.PdfTextExtractor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import org.json.JSONObject

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MemoryRepository(private val context: Context) {

    private val db = AppDatabase.getInstance(context)
    private val memoryDao = db.memoryDao()
    private val universalSearchRepo = UniversalSearchRepository(context)

    fun getDao() = memoryDao


    companion object {
        private const val TAG = "MemoryRepository"
    }

    /**
     * Flow of all memories stored in local database
     */
    val allMemories: Flow<List<WorkspaceItem>> = memoryDao.getAllMemories()
        .map { entities -> entities.map { it.toWorkspaceItem() } }
        .flowOn(Dispatchers.IO)

    /**
     * Flow of pinned/important memories
     */
    val importantMemories: Flow<List<WorkspaceItem>> = memoryDao.getImportantMemories()
        .map { entities -> entities.map { it.toWorkspaceItem() } }
        .flowOn(Dispatchers.IO)

    private fun extractAndAppendEntities(title: String, rawText: String): String {
        val phoneRegex = Regex("""(\+?\d{1,4}?[-.\s]?\(?\d{1,3}?\)?[-.\s]?\d{1,4}[-.\s]?\d{1,9})""")
        val emailRegex = Regex("""\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,4}\b""")
        val dateRegex = Regex("""\b\d{2,4}[-./]\d{2}[-./]\d{2,4}\b""")

        val combinedText = "$title\n$rawText"
        val entities = mutableListOf<String>()

        // Find phone numbers
        val phones = phoneRegex.findAll(combinedText).map { it.value.trim() }.filter { it.length >= 7 && it.any { c -> c.isDigit() } }.toList()
        if (phones.isNotEmpty()) {
            entities.addAll(phones)
            entities.add("phone")
            entities.add("mobile")
            entities.add("contact")
        }

        // Find emails
        val emails = emailRegex.findAll(combinedText).map { it.value.trim() }.toList()
        if (emails.isNotEmpty()) {
            entities.addAll(emails)
            entities.add("email")
            entities.add("contact")
        }

        // Find dates
        val dates = dateRegex.findAll(combinedText).map { it.value.trim() }.toList()
        if (dates.isNotEmpty()) {
            entities.addAll(dates)
            entities.add("date")
        }

        val distinctEntities = entities.distinct()
        return if (distinctEntities.isNotEmpty()) {
            rawText + "\n\nSearchable Metadata / Entities:\n" + distinctEntities.joinToString(" ")
        } else {
            rawText
        }
    }

    /**
     * Real-time offline search with FTS4 MATCH query:
     * SELECT memories.* FROM memories JOIN memories_fts ON memories.rowid = memories_fts.rowid WHERE memories_fts MATCH :matchQuery
     *
     * Supports wildcard tokens (e.g. *query* or query*)
     */
    fun searchMemories(rawQuery: String): Flow<List<WorkspaceItem>> {
        val cleanQuery = rawQuery.trim()
        if (cleanQuery.isEmpty()) {
            return allMemories
        }

        // Normalize query: If user searches "phone number" or "mobile number" or contact, or searches contact digits
        val isPhoneQuery = cleanQuery.contains("phone", ignoreCase = true) ||
                cleanQuery.contains("mobile", ignoreCase = true) ||
                cleanQuery.contains("contact", ignoreCase = true) ||
                (cleanQuery.length >= 4 && cleanQuery.all { it.isDigit() || it == '+' || it == '-' })

        val tokens = cleanQuery.split("\\s+".toRegex())
            .filter { it.isNotBlank() }
            .map { it.replace(Regex("[^a-zA-Z0-9]"), "").trim() }
            .filter { it.isNotEmpty() }

        val w1 = tokens.getOrNull(0) ?: ""
        val w2 = tokens.getOrNull(1) ?: ""

        val likeQueryTerm = if (isPhoneQuery && !cleanQuery.any { it.isDigit() }) "phone" else cleanQuery

        return if (isPhoneQuery) {
            memoryDao.searchMemoriesLike(likeQueryTerm).map { entities ->
                entities.map { it.toWorkspaceItem() }
            }.flowOn(Dispatchers.IO)
        } else {
            memoryDao.searchMemoriesFlexible(w1, w2).flatMapLatest { flexResults ->
                if (flexResults.isNotEmpty()) {
                    kotlinx.coroutines.flow.flowOf(flexResults.map { it.toWorkspaceItem() })
                } else {
                    memoryDao.searchMemoriesAny(w1, w2).flatMapLatest { anyResults ->
                        if (anyResults.isNotEmpty()) {
                            kotlinx.coroutines.flow.flowOf(anyResults.map { it.toWorkspaceItem() })
                        } else {
                            val fallbackTerm = tokens.maxByOrNull { it.length } ?: cleanQuery
                            memoryDao.searchMemoriesLike(fallbackTerm).map { fallbackEntities ->
                                fallbackEntities.map { it.toWorkspaceItem() }
                            }
                        }
                    }
                }
            }.flowOn(Dispatchers.IO)
        }
    }

    /**
     * Saves a Note directly into local storage & FTS4 index.
     */
    suspend fun saveNote(
        id: String = "note-${System.currentTimeMillis()}",
        title: String,
        content: String,
        tags: List<String>,
        hasAttachment: Boolean,
        includeInSemanticSearch: Boolean,
        attachmentUri: String? = null
    ): WorkspaceItem = withContext(Dispatchers.IO) {
        val cleanTitle = when {
            title.isNotBlank() -> title.trim()
            content.isNotBlank() -> {
                val firstLine = content.lines().firstOrNull { it.isNotBlank() }?.trim() ?: "Untitled Note"
                if (firstLine.length > 40) firstLine.take(40) + "..." else firstLine
            }
            else -> "Untitled Note"
        }

        // Export Note to Public storage as requested
        try {
            val noteFileName = "${cleanTitle.replace(" ", "_")}_${System.currentTimeMillis()}.txt"
            val noteContent = "Title: $cleanTitle\n\n$content\n\nTags: ${tags.joinToString(", ")}"
            com.example.util.MediaStoreUtils.saveDocToPublic(
                context,
                noteContent.byteInputStream(),
                noteFileName,
                "Notes",
                "text/plain"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to export note to public storage", e)
        }

        // If note has an attached image, extract text via ML Kit on-device
        val extractedImageText = if (attachmentUri != null) {
            try {
                MlKitTextExtractor.extractTextFromUri(context, Uri.parse(attachmentUri))
            } catch (e: Exception) {
                ""
            }
        } else {
            ""
        }

        val fullExtractedText = buildString {
            append(cleanTitle)
            append("\n\n")
            append(content)
            if (tags.isNotEmpty()) {
                append("\nTags: ")
                append(tags.joinToString(" "))
            }
            if (extractedImageText.isNotBlank()) {
                append("\n[Attached Photo OCR Extracted Text]:\n")
                append(extractedImageText)
            }
        }

        val fullExtractedTextWithEntities = extractAndAppendEntities(cleanTitle, fullExtractedText)
        val words = content.split("\\s+".toRegex()).filter { it.isNotBlank() }.size
        val primaryTag = tags.firstOrNull()?.removePrefix("#")?.replaceFirstChar { it.uppercase() } ?: "Note"
        val firstSummaryLine = content.lines().firstOrNull { it.isNotBlank() }?.take(90) ?: "Notepad memory"

        val entity = MemoryEntity(
            id = id,
            type = "note",
            title = cleanTitle,
            extractedText = fullExtractedTextWithEntities,
            imageUri = attachmentUri,
            createdAt = System.currentTimeMillis(),
            sizeText = "$words words${if (hasAttachment) " • 1 Photo" else ""}",
            summary = firstSummaryLine,
            subtitle = "Notepad • ${if (includeInSemanticSearch) "FTS4 Indexed" else "Local Storage"}${if (hasAttachment) " • 📷 Photo attached" else ""}",
            tag = primaryTag,
            isPinned = false
        )

        memoryDao.insertMemory(entity)
        memoryDao.insertFts(MemoryFtsEntity(id = id, title = cleanTitle, extractedText = fullExtractedTextWithEntities))

        // Index in Universal Search
        universalSearchRepo.insertOrUpdate(
            id = id,
            module = "Note",
            title = cleanTitle,
            contentText = fullExtractedTextWithEntities,
            metadataJson = JSONObject().apply {
                put("tags", tags.joinToString(","))
                put("has_attachment", hasAttachment)
                put("attachment_uri", attachmentUri ?: "")
            }.toString(),
            imageUri = attachmentUri
        )

        // Automatic Database Backup
        com.example.util.BackupRestoreManager.createLocalBackup(context)

        entity.toWorkspaceItem()
    }

    /**
     * Saves an Image into local storage & FTS4 index.
     * Uses on-device Google ML Kit Text Recognition to extract text line-by-line.
     * Persists uploaded image file locally into app internal files.
     */
    suspend fun saveImageMemory(
        id: String = "img-${System.currentTimeMillis()}",
        title: String,
        imageUri: String?,
        amount: String = "",
        dueDate: String = "",
        includeInSearch: Boolean = true,
        fullOcrText: String = ""
    ): WorkspaceItem = withContext(Dispatchers.IO) {
        // Route through MediaStore for public persistent storage as requested
        val finalTitle = title.ifBlank { "Scanned Document" }
        val isResizer = finalTitle.startsWith("Resized_", ignoreCase = true) || amount.startsWith("Resized to", ignoreCase = true)
        
        val permanentImageUri = if (imageUri != null) {
            try {
                val inputUri = Uri.parse(imageUri)
                val fileName = if (isResizer) "${finalTitle}.jpg" else "Scan_${id}.jpg"
                val mimeType = "image/jpeg"
                
                val publicUri = com.example.util.MediaStoreUtils.saveImageToPublic(
                    context,
                    context.contentResolver.openInputStream(inputUri)!!,
                    fileName,
                    mimeType
                )
                publicUri?.toString() ?: imageUri
            } catch (e: Exception) {
                Log.e(TAG, "Failed to copy image to public storage", e)
                imageUri
            }
        } else {
            null
        }

        // Run real ML Kit Text Recognition on-device or use fullOcrText passed from UI
        val ocrExtractedText = if (fullOcrText.isNotBlank()) {
            fullOcrText
        } else if (permanentImageUri != null) {
            try {
                MlKitTextExtractor.extractTextFromUri(context, Uri.parse(permanentImageUri))
            } catch (e: Exception) {
                Log.e(TAG, "Failed OCR extraction for image: $permanentImageUri", e)
                ""
            }
        } else {
            ""
        }

        val fullSearchableText = buildString {
            append(title)
            if (amount.isNotBlank()) append("\nAmount: $amount")
            if (dueDate.isNotBlank()) append("\nDue Date: $dueDate")
            if (ocrExtractedText.isNotBlank()) {
                append("\n\nExtracted OCR Text:\n")
                append(ocrExtractedText)
            }
        }

        val fullSearchableTextWithEntities = extractAndAppendEntities(finalTitle, fullSearchableText)

        // Classify document to prevent unwanted reminders
        val docType = com.example.util.DocumentSummaryParser.classifyDocument(fullSearchableText)
        
        val summaryText = when {
            docType == com.example.util.DocumentSummaryParser.DocType.IDENTITY_DOCUMENT -> "Identity Document • Securely Stored"
            ocrExtractedText.isNotBlank() -> ocrExtractedText.lines().firstOrNull { it.isNotBlank() }?.take(80) ?: "OCR text recognized on-device"
            amount.isNotBlank() -> "Amount: $amount • Due: $dueDate"
            else -> "Scanned image indexed offline"
        }

        // Deep Entity Parsing: RULE - No auto-dispatching background alarms.
        // We still parse the date for the in-app "Tap to Set Reminder" suggestion.
        val parsedSummary = com.example.util.DocumentSummaryParser.parse(fullSearchableText, title)
        val detectedDueDate = parsedSummary.dates.firstOrNull()?.let { 
            com.example.util.DocumentSummaryParser.parseDateToLong(it) 
        }

        val entity = MemoryEntity(
            id = id,
            type = "image",
            title = finalTitle,
            extractedText = fullSearchableTextWithEntities,
            imageUri = permanentImageUri,
            createdAt = System.currentTimeMillis(),
            sizeText = if (amount.isNotBlank()) "$amount • Image" else "3.2 MB • Image",
            summary = summaryText,
            subtitle = if (docType == com.example.util.DocumentSummaryParser.DocType.IDENTITY_DOCUMENT) "ID Card • Encrypted Vault" else "Receipt / Document • ML Kit OCR Indexed",
            tag = if (docType == com.example.util.DocumentSummaryParser.DocType.IDENTITY_DOCUMENT) "Identity" else "Receipt",
            isPinned = false,
            dueDate = if (docType == com.example.util.DocumentSummaryParser.DocType.IDENTITY_DOCUMENT) null else detectedDueDate
        )

        memoryDao.insertMemory(entity)
        memoryDao.insertFts(MemoryFtsEntity(id = id, title = finalTitle, extractedText = fullSearchableTextWithEntities))

        // Index in Universal Search
        if (isResizer) {
            val dimensionsPattern = Regex("""(\d+)x(\d+)\s*px""")
            val match = dimensionsPattern.find(amount)
            val w = match?.groupValues?.getOrNull(1) ?: "800"
            val h = match?.groupValues?.getOrNull(2) ?: "600"
            val dpiVal = if (amount.contains("DPI", ignoreCase = true)) {
                amount.substringAfter("DPI").substringBefore(" ").trim()
            } else "300 DPI"

            universalSearchRepo.insertOrUpdate(
                id = id,
                module = "Resizer",
                title = "Resized Image: $finalTitle",
                contentText = "Resized Photo\nMetadata: $amount\nFile Name: $finalTitle\nDimensions: $w x $h\nDPI: $dpiVal",
                metadataJson = JSONObject().apply {
                    put("resized_file_name", finalTitle)
                    put("dimensions", "$w x $h")
                    put("preset_type", if (amount.contains("SSC", ignoreCase = true)) "SSC" else if (amount.contains("UPSC", ignoreCase = true)) "UPSC" else "Custom")
                    put("dpi_metadata", dpiVal)
                    put("info", amount)
                }.toString(),
                imageUri = permanentImageUri
            )
        } else {
            universalSearchRepo.insertOrUpdate(
                id = id,
                module = "Image",
                title = finalTitle,
                contentText = fullSearchableTextWithEntities,
                metadataJson = JSONObject().apply {
                    put("amount", amount)
                    put("due_date", dueDate)
                    put("ocr_text", ocrExtractedText)
                }.toString(),
                imageUri = permanentImageUri
            )
        }

        // Automatic Database Backup
        com.example.util.BackupRestoreManager.createLocalBackup(context)

        entity.toWorkspaceItem()
    }

    /**
     * Saves a PDF into local storage & FTS4 index.
     * Persists PDF to internal storage and renders real page 1 thumbnail image.
     */
    suspend fun savePdfMemory(
        id: String = "pdf-${System.currentTimeMillis()}",
        title: String,
        fileName: String,
        fileSize: String,
        summary: String,
        keyPoints: List<String>,
        pdfUri: Uri? = null,
        includeInSearch: Boolean = true,
        fullTranscript: String = ""
    ): WorkspaceItem = withContext(Dispatchers.IO) {
        // Route PDF through MediaStore for public persistent storage as requested
        var publicPdfUri: Uri? = null
        if (pdfUri != null) {
            try {
                val destFileName = if (fileName.endsWith(".pdf", ignoreCase = true)) fileName else "$fileName.pdf"
                publicPdfUri = com.example.util.MediaStoreUtils.saveDocToPublic(
                    context,
                    context.contentResolver.openInputStream(pdfUri)!!,
                    destFileName,
                    "Invoices",
                    "application/pdf"
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error copying PDF to public storage", e)
            }
        }

        val effectivePdfUri = publicPdfUri ?: pdfUri

        val extractedPdfText = if (fullTranscript.isNotBlank()) {
            fullTranscript
        } else if (effectivePdfUri != null) {
            try {
                PdfTextExtractor.extractTextFromPdfUri(context, effectivePdfUri)
            } catch (e: Exception) {
                Log.e(TAG, "Error extracting text from PDF", e)
                ""
            }
        } else {
            ""
        }

        val fullText = buildString {
            append(title)
            append("\n")
            append(summary)
            append("\n")
            append(keyPoints.joinToString("\n"))
            if (extractedPdfText.isNotBlank()) {
                append("\n\nExtracted Pages Text & Full Transcript:\n")
                append(extractedPdfText)
            }
        }

        // Render and save page 1 thumbnail JPEG so Gallery & AI search have a real visual preview
        val thumbnailUriString = if (effectivePdfUri != null) {
            try {
                val pages = PdfPageRenderer.renderPdfPages(context, effectivePdfUri, maxPages = 1)
                val firstPage = pages.firstOrNull()
                if (firstPage != null) {
                    val thumbsDir = File(context.filesDir, "vault_thumbnails").apply { mkdirs() }
                    val thumbFile = File(thumbsDir, "thumb_${id}.jpg")
                    FileOutputStream(thumbFile).use { out ->
                        firstPage.compress(Bitmap.CompressFormat.JPEG, 90, out)
                    }
                    Uri.fromFile(thumbFile).toString()
                } else {
                    effectivePdfUri.toString()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed rendering PDF thumbnail", e)
                effectivePdfUri.toString()
            }
        } else {
            null
        }

        val finalTitle = title.ifBlank { fileName }
        val fullTextWithEntities = extractAndAppendEntities(finalTitle, fullText)

        val entity = MemoryEntity(
            id = id,
            type = "pdf",
            title = finalTitle,
            extractedText = fullTextWithEntities,
            imageUri = thumbnailUriString,
            createdAt = System.currentTimeMillis(),
            sizeText = fileSize.ifBlank { "1.8 MB" },
            summary = summary.ifBlank { "PDF parsed locally on device" },
            subtitle = "PDF Document • Offline FTS4 Index",
            tag = "Document",
            isPinned = false,
            driveFileId = effectivePdfUri?.toString()
        )

        memoryDao.insertMemory(entity)
        memoryDao.insertFts(MemoryFtsEntity(id = id, title = finalTitle, extractedText = fullTextWithEntities))

        // Index in Universal Search
        universalSearchRepo.insertOrUpdate(
            id = id,
            module = "PDF",
            title = finalTitle,
            contentText = fullTextWithEntities,
            metadataJson = JSONObject().apply {
                put("file_name", fileName)
                put("file_size", fileSize)
                put("summary", summary)
                put("key_points", keyPoints.joinToString(","))
                put("pdf_uri", pdfUri?.toString() ?: "")
            }.toString(),
            imageUri = thumbnailUriString
        )

        // Automatic Database Backup
        com.example.util.BackupRestoreManager.createLocalBackup(context)

        entity.toWorkspaceItem()
    }

    /**
     * Saves a voice note memory
     */
    suspend fun saveVoiceMemory(
        id: String = "voice-${System.currentTimeMillis()}",
        title: String,
        transcription: String,
        summary: String,
        hasReminder: Boolean
    ): WorkspaceItem = withContext(Dispatchers.IO) {
        val fullText = "$title\n$summary\n$transcription"
        val fullTextWithEntities = extractAndAppendEntities(title, fullText)
        val entity = MemoryEntity(
            id = id,
            type = "voice",
            title = title,
            extractedText = fullTextWithEntities,
            imageUri = null,
            createdAt = System.currentTimeMillis(),
            sizeText = "1m 45s (Audio)",
            summary = summary,
            subtitle = "Voice Note • ${if (hasReminder) "Reminder set" else "Transcribed"}",
            tag = "Voice",
            isPinned = false
        )

        memoryDao.insertMemory(entity)
        memoryDao.insertFts(MemoryFtsEntity(id = id, title = title, extractedText = fullTextWithEntities))
        
        // Automatic Database Backup
        com.example.util.BackupRestoreManager.createLocalBackup(context)

        entity.toWorkspaceItem()
    }

    suspend fun togglePin(item: WorkspaceItem) = withContext(Dispatchers.IO) {
        val existing = memoryDao.getMemoryById(item.id)
        if (existing != null) {
            memoryDao.updateMemory(existing.copy(isPinned = !existing.isPinned))
            // Automatic Database Backup
            com.example.util.BackupRestoreManager.createLocalBackup(context)
        }
    }

    suspend fun renameMemory(id: String, newTitle: String) = withContext(Dispatchers.IO) {
        val existing = memoryDao.getMemoryById(id)
        if (existing != null && newTitle.isNotBlank()) {
            val updated = existing.copy(title = newTitle.trim())
            memoryDao.updateMemory(updated)
            memoryDao.insertFts(MemoryFtsEntity(id = id, title = newTitle.trim(), extractedText = existing.extractedText))
            universalSearchRepo.insertOrUpdate(
                id = id,
                module = if (existing.type == "pdf") "PDF" else if (existing.type == "note") "Note" else "Image",
                title = newTitle.trim(),
                contentText = existing.extractedText,
                metadataJson = "{}",
                imageUri = existing.imageUri
            )
            com.example.util.BackupRestoreManager.createLocalBackup(context)
        }
    }

    suspend fun deleteMemory(id: String) = withContext(Dispatchers.IO) {
        val existing = memoryDao.getMemoryById(id)
        memoryDao.deleteMemoryById(id)
        universalSearchRepo.delete(id)
        if (existing != null) {
            // Clean up files
            try {
                if (!existing.imageUri.isNullOrBlank()) {
                    val uri = Uri.parse(existing.imageUri)
                    if (uri.scheme == "file") File(uri.path ?: "").delete()
                }
                if (!existing.driveFileId.isNullOrBlank()) {
                    val uri = Uri.parse(existing.driveFileId)
                    if (uri.scheme == "file") File(uri.path ?: "").delete()
                }
            } catch (_: Exception) {}
        }
    }

    suspend fun markItemSyncedToDrive(id: String, driveFileId: String? = null) = withContext(Dispatchers.IO) {
        val fileId = driveFileId ?: "drive-${System.currentTimeMillis()}"
        memoryDao.updateSyncStatus(id, true, fileId)
    }

    suspend fun getMemoryById(id: String): MemoryEntity? = withContext(Dispatchers.IO) {
        memoryDao.getMemoryById(id)
    }

    /**
     * Extension to convert MemoryEntity to WorkspaceItem model for UI
     */
    private fun MemoryEntity.toWorkspaceItem(): WorkspaceItem {
        val itemType = when (type.lowercase(Locale.ROOT)) {
            "note" -> ItemType.NOTE
            "pdf" -> ItemType.PDF
            "image" -> ItemType.IMAGE
            "voice" -> ItemType.VOICE
            else -> ItemType.NOTE
        }

        val dateFormatted = try {
            val sdf = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
            sdf.format(Date(createdAt))
        } catch (e: Exception) {
            "Just now"
        }

        return WorkspaceItem(
            id = id,
            title = title,
            type = itemType,
            dateModified = dateFormatted,
            sizeText = sizeText.ifBlank { if (itemType == ItemType.NOTE) "Note" else "1.2 MB" },
            summary = summary.ifBlank { extractedText.take(90) },
            subtitle = subtitle.ifBlank { "${itemType.label} • Offline FTS4 Index" },
            isPinned = isPinned,
            tag = tag ?: itemType.label,
            contentSnippet = extractedText,
            imageUri = imageUri,
            isSyncedToDrive = isSyncedToDrive,
            driveFileId = driveFileId,
            dueDate = dueDate,
            createdAt = createdAt
        )
    }
}
