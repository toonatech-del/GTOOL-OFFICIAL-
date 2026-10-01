package com.example.data.repository

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.UniversalSearchEntity
import com.example.data.local.UniversalSearchFtsEntity
import com.example.model.ItemType
import com.example.model.WorkspaceItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONObject

class UniversalSearchRepository(private val context: Context) {

    private val db = AppDatabase.getInstance(context)
    private val searchDao = db.universalSearchDao()

    companion object {
        private const val TAG = "UniversalSearchRepo"
        private val phoneRegex = Regex("""(\+?\d{1,4}?[-.\s]?\(?\d{1,3}?\)?[-.\s]?\d{1,4}[-.\s]?\d{1,9})""")
    }

    /**
     * Index general or custom data. Automatically performs phone number detection
     * and appends searchable metadata category tags "phone mobile contact" if found.
     */
    suspend fun insertOrUpdate(
        id: String,
        module: String,
        title: String,
        contentText: String,
        metadataJson: String,
        imageUri: String? = null
    ) = withContext(Dispatchers.IO) {
        val mergedText = "$title\n$contentText\n$metadataJson"
        val hasPhone = phoneRegex.containsMatchIn(mergedText)

        val finalContentText = if (hasPhone) {
            "$contentText\n\n[Searchable Metadata Entities]: phone mobile contact"
        } else {
            contentText
        }

        val finalMetadataJson = if (hasPhone) {
            try {
                val obj = JSONObject(metadataJson)
                obj.put("has_phone", true)
                obj.put("entity_tags", "phone mobile contact")
                obj.toString()
            } catch (e: Exception) {
                metadataJson
            }
        } else {
            metadataJson
        }

        val entity = UniversalSearchEntity(
            id = id,
            module = module,
            title = title,
            contentText = finalContentText,
            metadataJson = finalMetadataJson,
            imageUri = imageUri,
            createdAt = System.currentTimeMillis()
        )
        searchDao.insertSearchItem(entity)
        searchDao.insertFts(
            UniversalSearchFtsEntity(
                id = id,
                title = title,
                contentText = finalContentText,
                metadataJson = finalMetadataJson
            )
        )
    }

    suspend fun delete(id: String) = withContext(Dispatchers.IO) {
        searchDao.deleteById(id)
    }

    /**
     * Expose search with FTS prefix fallback to LIKE substring
     */
    fun search(rawQuery: String): Flow<List<UniversalSearchEntity>> {
        val query = rawQuery.trim()
        if (query.isEmpty()) {
            return searchDao.getAllItems()
        }

        val isPhoneQuery = query.contains("phone", ignoreCase = true) ||
                query.contains("mobile", ignoreCase = true) ||
                query.contains("contact", ignoreCase = true) ||
                (query.length >= 4 && query.all { it.isDigit() || it == '+' || it == '-' })

        val tokens = query.split("\\s+".toRegex())
            .filter { it.isNotBlank() }
            .map { it.replace(Regex("[^a-zA-Z0-9]"), "").trim() }
            .filter { it.isNotEmpty() }

        val w1 = tokens.getOrNull(0) ?: ""
        val w2 = tokens.getOrNull(1) ?: ""

        val likeQueryTerm = if (isPhoneQuery && !query.any { it.isDigit() }) "phone" else query

        return kotlinx.coroutines.flow.flow {
            try {
                if (isPhoneQuery) {
                    searchDao.searchLike(likeQueryTerm).collect { emit(it) }
                } else {
                    searchDao.searchLikeFlexible(w1, w2).flatMapLatest { flexResults ->
                        if (flexResults.isNotEmpty()) {
                            flowOf(flexResults)
                        } else {
                            searchDao.searchLikeAny(w1, w2).flatMapLatest { anyResults ->
                                if (anyResults.isNotEmpty()) {
                                    flowOf(anyResults)
                                } else {
                                    val fallbackTerm = tokens.maxByOrNull { it.length } ?: query
                                    searchDao.searchLike(fallbackTerm)
                                }
                            }
                        }
                    }.collect { emit(it) }
                }
            } catch (e: Exception) {
                searchDao.searchLike(likeQueryTerm).collect { emit(it) }
            }
        }.flowOn(Dispatchers.IO)
    }

    /**
     * Convert UniversalSearchEntity to WorkspaceItem for Home Screen search results
     */
    fun entityToWorkspaceItem(entity: UniversalSearchEntity): WorkspaceItem {
        val itemType = when (entity.module.uppercase()) {
            "PDF" -> ItemType.PDF
            "NOTE" -> ItemType.NOTE
            "IMAGE" -> ItemType.IMAGE
            "INVOICE" -> ItemType.IMAGE // displayed as scanned or processed invoice item
            "RESIZER" -> ItemType.IMAGE
            else -> ItemType.NOTE
        }

        val dateFormatted = try {
            val sdf = java.text.SimpleDateFormat("MMM d, h:mm a", java.util.Locale.getDefault())
            sdf.format(java.util.Date(entity.createdAt))
        } catch (e: Exception) {
            "Just now"
        }

        // Summary Snippet Creation with boldness or fallback
        val cleanedSummary = entity.contentText.take(150).replace("\n", " ").trim()

        return WorkspaceItem(
            id = entity.id,
            title = entity.title,
            type = itemType,
            dateModified = dateFormatted,
            sizeText = if (entity.module == "Invoice") "Invoice" else if (entity.module == "Resizer") "Resized" else itemType.extensionLabel,
            summary = cleanedSummary,
            subtitle = "${entity.module} • Unified Index",
            isPinned = false,
            tag = entity.module,
            contentSnippet = entity.contentText,
            imageUri = entity.imageUri
        )
    }
}
