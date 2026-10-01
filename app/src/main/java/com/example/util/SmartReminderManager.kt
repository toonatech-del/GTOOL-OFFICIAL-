package com.example.util

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.NotificationEntity
import com.example.data.local.ReminderEntity
import com.example.data.repository.ReminderRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern

/**
 * Intelligent Semantic Intent Resolver & Auto-Reminder Scheduler.
 * Processes spoken queries (e.g., "mera recharge kab khtm hoga") by expanding Hinglish/English synonyms,
 * multi-matching across Room Notes & OCR text, extracting dates, and auto-scheduling exact alarms.
 */
object SmartReminderManager {

    sealed class ResolveResult {
        data class Scheduled(
            val reminderId: Long,
            val title: String,
            val targetTimestamp: Long,
            val formattedDate: String,
            val sourceNoteTitle: String,
            val message: String
        ) : ResolveResult()

        data class FoundWithoutDate(
            val noteTitle: String,
            val message: String
        ) : ResolveResult()

        data class NotFound(
            val message: String
        ) : ResolveResult()
    }

    /**
     * Resolves user query against notes & OCR documents, extracts future expiry dates,
     * and automatically schedules an exact alarm.
     */
    suspend fun resolveAndScheduleReminder(context: Context, query: String): ResolveResult = withContext(Dispatchers.IO) {
        val db = AppDatabase.getInstance(context)
        val memoryDao = db.memoryDao()
        val reminderDao = db.reminderDao()
        val notificationDao = db.notificationDao()

        val expandedKeywords = QueryExpander.expandQuery(query)
        val isRechargeQuery = QueryExpander.isRechargeQuery(query) || query.contains("recharge", ignoreCase = true)
        val isBillQuery = query.contains("bill", ignoreCase = true) || query.contains("bijli", ignoreCase = true)

        val telecomKeywords = setOf("recharge", "plan", "pack", "validity", "valid", "data", "jio", "airtel", "vi", "bsnl", "sim", "telecom")
        val expiryKeywords = setOf("khtm", "khatam", "expire", "expiry", "expires", "end", "ending", "valid till", "due", "last date", "samapt", "over")
        val billKeywords = setOf("bill", "bijli", "electricity", "water", "broadband", "wifi", "emi", "installment", "rent", "credit card")

        // 1. Fetch candidate memories / notes / OCR texts
        val allMemories = try {
            memoryDao.getAllMemoriesList()
        } catch (_: Exception) {
            emptyList()
        }

        var matchedNoteTitle: String? = null
        var candidateWithoutDateTitle: String? = null
        var bestFutureTimestamp: Long? = null

        for (item in allMemories) {
            val content = "${item.title}\n${item.summary}\n${item.extractedText}".lowercase(Locale.getDefault())

            // Check multi-match condition:
            // Match telecom/bill keywords AND expiry keywords, or any expanded keywords
            val hasTelecom = telecomKeywords.any { content.contains(it) }
            val hasExpiry = expiryKeywords.any { content.contains(it) }
            val hasBill = billKeywords.any { content.contains(it) }
            val hasGenericExpanded = expandedKeywords.any { kw -> content.contains(kw.lowercase(Locale.getDefault())) }

            val isCandidate = if (isRechargeQuery) {
                (hasTelecom && hasExpiry) || hasTelecom || hasGenericExpanded
            } else if (isBillQuery) {
                (hasBill && hasExpiry) || hasBill || hasGenericExpanded
            } else {
                hasGenericExpanded || (hasTelecom && hasExpiry) || (hasBill && hasExpiry)
            }

            if (isCandidate) {
                val extractedDate = extractBestFutureDate("${item.title}\n${item.summary}\n${item.extractedText}")
                if (extractedDate != null) {
                    matchedNoteTitle = item.title
                    bestFutureTimestamp = extractedDate
                    break
                } else if (candidateWithoutDateTitle == null) {
                    candidateWithoutDateTitle = item.title
                }
            }
        }

        // If matching future date is found:
        if (bestFutureTimestamp != null && matchedNoteTitle != null) {
            val taskTitle = if (isRechargeQuery) "Recharge / Plan Expiry" else if (isBillQuery) "Bill Payment Due" else "Task Reminder"
            val formattedDate = formatDate(bestFutureTimestamp)

            // Auto-insert reminder into Room Database
            val reminderEntity = ReminderEntity(
                description = taskTitle,
                scheduledTime = bestFutureTimestamp,
                isCompleted = false,
                createdAt = System.currentTimeMillis()
            )
            val reminderId = reminderDao.insertReminder(reminderEntity)

            notificationDao.insertNotification(
                NotificationEntity(
                    title = "GTOOL X Reminder",
                    content = taskTitle,
                    type = "reminder",
                    timestamp = System.currentTimeMillis(),
                    isRead = false,
                    scheduledTime = bestFutureTimestamp,
                    reminderId = reminderId
                )
            )

            // Schedule exact alarm via ReminderScheduler
            val reminderMessage = if (isRechargeQuery) {
                "Your plan is ending today. Recharge to continue services."
            } else {
                "Reminder: $taskTitle is due today."
            }
            ReminderScheduler.scheduleAlarm(
                context = context,
                reminderId = reminderId.toInt(),
                title = "$taskTitle Alert",
                message = reminderMessage,
                triggerTimeMillis = bestFutureTimestamp
            )

            val feedbackMsg = "✅ Found in note '$matchedNoteTitle': Plan expires on $formattedDate. Reminder scheduled!"

            return@withContext ResolveResult.Scheduled(
                reminderId = reminderId,
                title = taskTitle,
                targetTimestamp = bestFutureTimestamp,
                formattedDate = formattedDate,
                sourceNoteTitle = matchedNoteTitle,
                message = feedbackMsg
            )
        }

        // If candidate note was found but has no future expiry date
        if (candidateWithoutDateTitle != null) {
            val msg = "Found note '$candidateWithoutDateTitle', but no future expiry date was found in it."
            return@withContext ResolveResult.FoundWithoutDate(
                noteTitle = candidateWithoutDateTitle,
                message = msg
            )
        }

        return@withContext ResolveResult.NotFound(
            message = "No matching plan or bill notes found for \"$query\"."
        )
    }

    /**
     * Automated Date Extraction & Score Ranking:
     * Extracts named months, numeric dates, and expiry phrases.
     * Picks the most relevant future date (timestamp >= System.currentTimeMillis()), defaulting to 09:00 AM.
     */
    fun extractBestFutureDate(text: String): Long? {
        val now = System.currentTimeMillis()
        val foundTimestamps = mutableListOf<Long>()

        // 1. Expiry Phrase regex: (?i)(?:valid till|expires on|expiry date|due|validity|end date|last date)\s*[:\-]?\s*([0-9a-zA-Z\s,]+)
        val expiryPhrasePattern = Pattern.compile(
            "(?i)(?:valid\\s+till|expires\\s+on|expiry\\s+date|expiry|due|validity|end\\s+date|last\\s+date)[\\s:=-]*([0-9]{1,2}(?:st|nd|rd|th)?[\\s/-]+[a-zA-Z0-9]+(?:[\\s/-]+[0-9]{2,4})?)",
            Pattern.CASE_INSENSITIVE
        )
        val expiryMatcher = expiryPhrasePattern.matcher(text)
        while (expiryMatcher.find()) {
            val dateStr = expiryMatcher.group(1) ?: continue
            parseDateString(dateStr)?.let { foundTimestamps.add(it) }
        }

        // 2. Named Month regex: (?i)\b(\d{1,2})\s*(?:st|nd|rd|th)?\s+(Jan(?:uary)?|Feb(?:ruary)?|Mar(?:ch)?|Apr(?:il)?|May|Jun(?:e)?|Jul(?:y)?|Aug(?:ust)?|Sep(?:tember)?|Oct(?:ober)?|Nov(?:ember)?|Dec(?:ember)?)\b(?:\s+(\d{4}))?
        val namedMonthPattern = Pattern.compile(
            "(?i)\\b(\\d{1,2})\\s*(?:st|nd|rd|th)?\\s+(Jan(?:uary)?|Feb(?:ruary)?|Mar(?:ch)?|Apr(?:il)?|May|Jun(?:e)?|Jul(?:y)?|Aug(?:ust)?|Sep(?:tember)?|Oct(?:ober)?|Nov(?:ember)?|Dec(?:ember)?)\\b(?:\\s+(\\d{2,4}))?",
            Pattern.CASE_INSENSITIVE
        )
        val namedMatcher = namedMonthPattern.matcher(text)
        while (namedMatcher.find()) {
            val d = namedMatcher.group(1)?.toIntOrNull() ?: 1
            val mStr = namedMatcher.group(2)?.lowercase(Locale.getDefault())?.take(3) ?: "jan"
            val mIdx = when (mStr) {
                "jan" -> 0; "feb" -> 1; "mar" -> 2; "apr" -> 3; "may" -> 4; "jun" -> 5
                "jul" -> 6; "aug" -> 7; "sep" -> 8; "oct" -> 9; "nov" -> 10; "dec" -> 11
                else -> 0
            }
            val y = namedMatcher.group(3)?.toIntOrNull() ?: Calendar.getInstance().get(Calendar.YEAR)
            val fullYear = if (y < 100) 2000 + y else y

            val cal = Calendar.getInstance().apply {
                set(fullYear, mIdx, d, 9, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }
            foundTimestamps.add(cal.timeInMillis)
        }

        // 3. Numerical Date regex: \b(\d{1,2})[-/.](\d{1,2})[-/.](\d{2,4})\b
        val numericalPattern = Pattern.compile("\\b(\\d{1,2})[-/.](\\d{1,2})[-/.](\\d{2,4})\\b")
        val numMatcher = numericalPattern.matcher(text)
        while (numMatcher.find()) {
            val d = numMatcher.group(1)?.toIntOrNull() ?: continue
            val m = (numMatcher.group(2)?.toIntOrNull() ?: continue) - 1
            val rawY = numMatcher.group(3)?.toIntOrNull() ?: Calendar.getInstance().get(Calendar.YEAR)
            val fullYear = if (rawY < 100) 2000 + rawY else rawY

            val cal = Calendar.getInstance().apply {
                set(fullYear, m, d, 9, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }
            foundTimestamps.add(cal.timeInMillis)
        }

        // Filter for valid future dates, or if within the current year, roll over to future
        val validFutureDates = foundTimestamps.map { ts ->
            if (ts < now) {
                val cal = Calendar.getInstance().apply {
                    timeInMillis = ts
                    add(Calendar.YEAR, 1)
                }
                cal.timeInMillis
            } else {
                ts
            }
        }.filter { it >= now }

        return validFutureDates.minOrNull()
    }

    private fun parseDateString(raw: String): Long? {
        val clean = raw.replace(Regex("(?i)(st|nd|rd|th)"), "").trim()
        val parts = clean.split("[\\s/.-]+".toRegex())
        if (parts.size >= 2) {
            val d = parts[0].toIntOrNull() ?: return null
            val second = parts[1]
            val monthIdx = second.toIntOrNull()?.let { it - 1 } ?: run {
                when (second.lowercase(Locale.getDefault()).take(3)) {
                    "jan" -> 0; "feb" -> 1; "mar" -> 2; "apr" -> 3; "may" -> 4; "jun" -> 5
                    "jul" -> 6; "aug" -> 7; "sep" -> 8; "oct" -> 9; "nov" -> 10; "dec" -> 11
                    else -> null
                }
            } ?: return null

            val y = if (parts.size >= 3) {
                val parsedY = parts[2].toIntOrNull() ?: Calendar.getInstance().get(Calendar.YEAR)
                if (parsedY < 100) 2000 + parsedY else parsedY
            } else {
                Calendar.getInstance().get(Calendar.YEAR)
            }

            val cal = Calendar.getInstance().apply {
                set(y, monthIdx, d, 9, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }
            return cal.timeInMillis
        }
        return null
    }

    fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd MMM yyyy, h:mm a", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}
