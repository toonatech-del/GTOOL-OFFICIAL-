package com.example.data.repository

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.MemoryEntity
import com.example.util.QueryExpander
import com.example.util.SmartReminderManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Vault repository for semantic synonym queries, multi-match database search,
 * and auto-reminder resolution.
 */
class VaultRepository(private val context: Context) {

    private val db = AppDatabase.getInstance(context)
    private val noteDao = db.noteDao()
    private val memoryDao = db.memoryDao()

    suspend fun resolveSemanticQuery(spokenQuery: String): SmartReminderManager.ResolveResult {
        return SmartReminderManager.resolveAndScheduleReminder(context, spokenQuery)
    }

    suspend fun searchNotesWithExpandedKeywords(spokenQuery: String): List<MemoryEntity> = withContext(Dispatchers.IO) {
        val expanded = QueryExpander.expandQuery(spokenQuery)
        val isRecharge = QueryExpander.isRechargeQuery(spokenQuery) || spokenQuery.contains("recharge", ignoreCase = true)
        
        if (isRecharge) {
            val telecomNotes = noteDao.searchTelecomExpiryNotes()
            if (telecomNotes.isNotEmpty()) return@withContext telecomNotes
        }

        val all = memoryDao.getAllMemoriesList()
        all.filter { item ->
            val content = "${item.title} ${item.summary} ${item.extractedText}".lowercase()
            expanded.any { kw -> content.contains(kw.lowercase()) }
        }
    }
}
