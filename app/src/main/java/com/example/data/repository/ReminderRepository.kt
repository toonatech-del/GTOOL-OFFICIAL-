package com.example.data.repository

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.AlarmManagerCompat
import com.example.data.local.AppDatabase
import com.example.data.local.MemoryDao
import com.example.data.local.NotificationDao
import com.example.data.local.NotificationEntity
import com.example.data.local.ReminderDao
import com.example.data.local.ReminderEntity
import com.example.data.local.UniversalSearchDao
import com.example.util.QueryExpander
import com.example.util.ReminderBroadcastReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern

/**
 * Repository responsible for contextual AI reminder extraction,
 * vault OCR/FTS keyword resolution, Room DB transactions, and exact alarm dispatching.
 */
class ReminderRepository(
    private val context: Context,
    private val reminderDao: ReminderDao,
    private val notificationDao: NotificationDao,
    private val memoryDao: MemoryDao,
    private val universalSearchDao: UniversalSearchDao
) {

    sealed class ReminderResult {
        data class Scheduled(
            val reminderId: Long,
            val title: String,
            val scheduledTimeMillis: Long,
            val formattedDate: String,
            val sourceVaultDoc: String? = null,
            val relatedItemId: String? = null,
            val message: String
        ) : ReminderResult()

        data class RequiresConfirmation(
            val title: String,
            val targetTimestamp: Long,
            val formattedDate: String,
            val sourceVaultDoc: String,
            val relatedItemId: String? = null,
            val message: String = ""
        ) : ReminderResult()

        data class NeedsInteractiveSchedule(
            val detectedTitle: String,
            val query: String
        ) : ReminderResult()
    }

    companion object {
        @Volatile
        private var INSTANCE: ReminderRepository? = null

        fun getInstance(context: Context): ReminderRepository {
            return INSTANCE ?: synchronized(this) {
                val db = AppDatabase.getInstance(context)
                val instance = ReminderRepository(
                    context.applicationContext,
                    db.reminderDao(),
                    db.notificationDao(),
                    db.memoryDao(),
                    db.universalSearchDao()
                )
                INSTANCE = instance
                instance
            }
        }
    }

    /**
     * Determines whether user text/speech conveys a reminder request.
     */
    fun isReminderQuery(query: String): Boolean {
        val q = query.lowercase(Locale.getDefault())
        val reminderRegex = Regex("(?i).*(remind me|set reminder|yaad dilwa|reminder lagao|reminder|yaad rakhna|alarm lagao|schedule reminder|kab khtm|kab khatam|kab expire|expiry date|valid till|due date).*")
        val isExpiryIntent = (QueryExpander.isRechargeQuery(query) && QueryExpander.isExpiryQuery(query)) ||
                (q.contains("kab") && (q.contains("khtm") || q.contains("khatam") || q.contains("expire") || q.contains("end")))

        return reminderRegex.matches(q) ||
                q.contains("reminder") ||
                q.contains("yaad dilwa") ||
                q.contains("remind me") ||
                q.contains("alarm lagao") ||
                isExpiryIntent
    }

    /**
     * Extracts a concise, professional task title from user query.
     * Prevents raw speech transcripts like "Reminder set: ko pan card FXIPD...".
     */
    fun extractCleanTaskTitle(query: String): String {
        val q = query.lowercase(Locale.getDefault())

        return when {
            q.contains("recharge") || q.contains("validity") || QueryExpander.isRechargeQuery(query) -> "Recharge / Plan Expiry"
            q.contains("bijli") || (q.contains("bill") && q.contains("electricity")) -> "Electricity Bill Due"
            q.contains("water") && q.contains("bill") -> "Water Bill Due"
            q.contains("wifi") || q.contains("broadband") || q.contains("internet") -> "Internet Bill Due"
            q.contains("bill") -> "Bill Payment Due"
            q.contains("pan") || q.contains("pan card") -> "PAN Card Reminder"
            q.contains("passport") -> "Passport Renewal"
            q.contains("license") || q.contains("licence") || q.contains("dl") -> "Driving License Renewal"
            q.contains("insurance") || q.contains("policy") || q.contains("lic") -> "Insurance Premium Due"
            q.contains("emi") || q.contains("loan") || q.contains("installment") -> "EMI Installment Due"
            q.contains("rent") -> "Rent Payment"
            q.contains("medicine") || q.contains("dawa") || q.contains("tablet") -> "Take Medicine"
            q.contains("meeting") || q.contains("call") -> "Meeting Reminder"
            q.contains("birthday") || q.contains("janamdin") -> "Birthday Reminder"
            q.contains("anniversary") -> "Anniversary Reminder"
            else -> {
                val cleaned = query
                    .replace(Regex("(?i)^reminder set:\\s*"), "")
                    .replace(Regex("(?i)^reminding you about:\\s*"), "")
                    .replace(Regex("(?i)(remind me to|set reminder for|set reminder to|set reminder|remind me|yaad dilwa dena|yaad dilwa|reminder lagao|bata dena reminder|bata dena|kis din khatm hoga|kab khatam hoga|kab expire hoga|khatm hoga|khatam hoga|expire hoga|hoga|hai|mera|meri|mere|mujhe|please|plz|ki|ka|ke|ko)\\b"), "")
                    .replace(Regex("[^a-zA-Z0-9\\s]"), " ")
                    .trim()
                    .replace(Regex("\\s+"), " ")

                if (cleaned.length in 3..35) {
                    cleaned.split(" ").joinToString(" ") { word ->
                        word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                    }
                } else {
                    "Task Reminder"
                }
            }
        }
    }

    /**
     * Contextual AI Reminder Resolution Engine:
     * - Step A: Parse user text for explicit relative or absolute dates
     * - Step B: If no explicit date in speech, query Room DB (OCR metadata + Notes FTS4 index) using QueryExpander
     * - Step C: Extract regex date matches from matched note/image OCR text -> schedule at 09:00 AM
     * - Step D: Fallback to Interactive Glassmorphic Sheet when date is absent (no raw cards saved)
     */
    suspend fun resolveAndSchedule(query: String): ReminderResult = withContext(Dispatchers.IO) {
        val taskTitle = extractCleanTaskTitle(query)

        // Step A: Parse user text for relative or absolute dates
        val speechDate = parseDateFromQuery(query)
        if (speechDate != null) {
            val id = commitAndSchedule(taskTitle, speechDate.timeInMillis)
            val dateStr = formatTimestamp(speechDate.timeInMillis)
            return@withContext ReminderResult.Scheduled(
                reminderId = id,
                title = taskTitle,
                scheduledTimeMillis = speechDate.timeInMillis,
                formattedDate = dateStr,
                message = "Reminder scheduled for **$taskTitle** on **$dateStr** 🔔"
            )
        }

        // Step B: Query Room DB using keywords expanded from query (Hinglish + English synonyms)
        val expandedTokens = QueryExpander.expandQuery(query)
        val isRecharge = query.contains("recharge", ignoreCase = true) || QueryExpander.isRechargeQuery(query)
        val isBill = query.contains("bill", ignoreCase = true) || query.contains("bijli", ignoreCase = true)

        val searchKeywords = mutableSetOf<String>().apply {
            addAll(expandedTokens)
            if (isRecharge) {
                addAll(listOf("recharge", "plan", "pack", "validity", "valid", "expires", "expiry", "end", "due", "airtel", "jio", "vi", "bsnl", "telecom"))
            }
            if (isBill) {
                addAll(listOf("bill", "electricity", "due date", "amount due", "power", "water", "gas", "broadband", "wifi", "bijli"))
            }
            if (query.contains("passport", ignoreCase = true)) addAll(listOf("passport", "valid", "expiry", "republic of india"))
            if (query.contains("license", ignoreCase = true) || query.contains("licence", ignoreCase = true)) addAll(listOf("license", "licence", "transport", "validity"))
            if (query.contains("insurance", ignoreCase = true)) addAll(listOf("insurance", "policy", "premium", "lic"))
            if (query.contains("pan", ignoreCase = true)) addAll(listOf("pan", "income tax", "permanent account"))
            if (isEmpty()) add(taskTitle.lowercase(Locale.getDefault()))
        }.toList()

        // RULE: Blacklist Identity Documents from auto-resolution
        val idKeywords = listOf(
            "INCOME TAX DEPARTMENT", "PERMANENT ACCOUNT NUMBER", "PAN CARD",
            "GOVT. OF INDIA", "GOVERNMENT OF INDIA", "FATHER'S NAME",
            "DATE OF BIRTH", "DOB", "AADHAAR", "UIDAI"
        )
        if (idKeywords.count { query.contains(it, ignoreCase = true) } >= 2) {
            return@withContext ReminderResult.NeedsInteractiveSchedule(taskTitle, query)
        }

        // 1. Check local memories & OCR extracted text
        val allMemories = try {
            memoryDao.getAllMemoriesList()
        } catch (_: Exception) {
            emptyList()
        }

        var matchedDocTitle: String? = null
        var matchedItemId: String? = null
        var resolvedVaultTimestamp: Long? = null
        var candidateWithoutDateTitle: String? = null

        val telecomKeywords = setOf("recharge", "plan", "pack", "validity", "valid", "data", "jio", "airtel", "vi", "bsnl", "sim", "telecom")
        val expiryKeywords = setOf("khtm", "khatam", "expire", "expiry", "expires", "end", "ending", "valid till", "due", "last date", "samapt", "over")

        for (item in allMemories) {
            val contentToSearch = "${item.title}\n${item.summary}\n${item.extractedText}".lowercase(Locale.getDefault())

            val hasTelecomMatch = isRecharge && telecomKeywords.any { contentToSearch.contains(it) }
            val hasExpiryMatch = isRecharge && expiryKeywords.any { contentToSearch.contains(it) }
            val hasGenericMatch = searchKeywords.any { kw -> contentToSearch.contains(kw) }

            val matchesKeyword = if (isRecharge) {
                (hasTelecomMatch && hasExpiryMatch) || (hasTelecomMatch) || hasGenericMatch
            } else {
                hasGenericMatch
            }

            if (matchesKeyword) {
                // Step C: Extract exact date and time using SmartDateParser from matched note/image OCR text
                val contentText = "${item.title}\n${item.summary}\n${item.extractedText}"
                val parsedDt = com.example.util.SmartDateParser.parseExpiryDateTime(contentText)
                if (parsedDt != null) {
                    matchedDocTitle = item.title
                    matchedItemId = item.id
                    resolvedVaultTimestamp = parsedDt.timestamp
                    break
                } else if (candidateWithoutDateTitle == null) {
                    candidateWithoutDateTitle = item.title
                }
            }
        }

        // If valid expiry date is resolved from vault: Return RequiresConfirmation instead of auto-scheduling
        if (resolvedVaultTimestamp != null) {
            val dateStr = formatTimestamp(resolvedVaultTimestamp!!)
            val docLabel = matchedDocTitle ?: "Vault Document"

            return@withContext ReminderResult.RequiresConfirmation(
                title = taskTitle,
                targetTimestamp = resolvedVaultTimestamp!!,
                formattedDate = dateStr,
                sourceVaultDoc = docLabel,
                relatedItemId = matchedItemId,
                message = "Found in note '$docLabel': Plan expires on $dateStr. Would you like to set a reminder?"
            )
        }

        // If candidate note matched but no date found
        if (candidateWithoutDateTitle != null) {
            return@withContext ReminderResult.NeedsInteractiveSchedule(
                detectedTitle = taskTitle,
                query = "Found note '$candidateWithoutDateTitle', but no future expiry date was found in it."
            )
        }

        // Step D: Neither user text nor vault contains a valid date -> Launch Interactive Sheet
        ReminderResult.NeedsInteractiveSchedule(
            detectedTitle = taskTitle,
            query = query
        )
    }

    /**
     * Commits a structured reminder to Room DB and arms native exact alarm.
     */
    suspend fun commitAndSchedule(title: String, timestampMillis: Long, relatedItemId: String? = null): Long = withContext(Dispatchers.IO) {
        val cleanTitle = extractCleanTaskTitle(title)

        val reminderEntity = ReminderEntity(
            description = cleanTitle,
            scheduledTime = timestampMillis,
            isCompleted = false,
            createdAt = System.currentTimeMillis()
        )
        val reminderId = reminderDao.insertReminder(reminderEntity)

        notificationDao.insertNotification(
            NotificationEntity(
                title = "GTOOL X Reminder",
                content = cleanTitle,
                type = "reminder",
                timestamp = System.currentTimeMillis(),
                isRead = false,
                scheduledTime = timestampMillis,
                reminderId = reminderId
            )
        )

        scheduleExactAlarm(reminderId, cleanTitle, timestampMillis, relatedItemId)
        reminderId
    }

    /**
     * Schedules native exact alarm via AlarmManagerCompat with Doze mode bypass.
     */
    fun scheduleExactAlarm(id: Long, title: String, timestampMillis: Long, relatedItemId: String? = null) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, com.example.receiver.ReminderBroadcastReceiver::class.java).apply {
            action = "com.gsd.gtoolx.ACTION_REMINDER_ALARM"
            putExtra("REMINDER_ID", id.toInt())
            putExtra("REMINDER_TITLE", title)
            putExtra("REMINDER_MSG", "Scheduled Task: $title")
            if (relatedItemId != null) {
                putExtra("related_item_id", relatedItemId)
            }
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    AlarmManagerCompat.setExactAndAllowWhileIdle(alarmManager, AlarmManager.RTC_WAKEUP, timestampMillis, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timestampMillis, pendingIntent)
                }
            } else {
                AlarmManagerCompat.setExactAndAllowWhileIdle(alarmManager, AlarmManager.RTC_WAKEUP, timestampMillis, pendingIntent)
            }
        } catch (_: SecurityException) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, timestampMillis, pendingIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Cancels scheduled exact alarm from AlarmManager and cleans up database.
     */
    suspend fun cancelReminder(reminderId: Long) = withContext(Dispatchers.IO) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        val intent = Intent(context, ReminderBroadcastReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager?.cancel(pendingIntent)
        pendingIntent.cancel()

        try {
            reminderDao.deleteReminderById(reminderId)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Cleans up any legacy raw speech transcript notifications and invalid OCR reminders.
     */
    suspend fun cleanLegacyTranscripts() = withContext(Dispatchers.IO) {
        try {
            // 1. Clean Notifications
            val list = notificationDao.getAllNotificationsList()
            for (notif in list) {
                val shouldDelete = notif.content.contains("ko pan card", ignoreCase = true) ||
                        notif.title.contains("ko pan card", ignoreCase = true) ||
                        notif.content.contains("Mera recharge", ignoreCase = true) ||
                        notif.content.contains("INCOME TAX DEPARTMENT", ignoreCase = true) ||
                        notif.content.contains("PERMANENT ACCOUNT NUMBER", ignoreCase = true) ||
                        notif.content.contains("AADHAAR", ignoreCase = true) ||
                        (notif.scheduledTime != null && notif.scheduledTime < System.currentTimeMillis())

                if (shouldDelete) {
                    notificationDao.deleteNotification(notif.id)
                }
            }

            // 2. Clean Reminders & Cancel Alarms
            val reminders = reminderDao.getActiveRemindersList()
            for (rem in reminders) {
                if (rem.description.contains("INCOME TAX DEPARTMENT", ignoreCase = true) ||
                    rem.description.contains("PAN CARD", ignoreCase = true) ||
                    rem.description.contains("AADHAAR", ignoreCase = true) ||
                    rem.scheduledTime < System.currentTimeMillis()
                ) {
                    cancelReminder(rem.id)
                }
            }
        } catch (_: Exception) {}
    }

    // ----------------------------------------------------
    // Internal Date Parsing Helpers
    // ----------------------------------------------------

    private fun parseDateFromQuery(query: String): Calendar? {
        val q = query.lowercase(Locale.getDefault())
        val cal = Calendar.getInstance()
        var hasExplicitDate = false

        if (q.contains("tomorrow") || q.contains("kal")) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
            hasExplicitDate = true
        } else if (q.contains("day after tomorrow") || q.contains("parso") || q.contains("parson")) {
            cal.add(Calendar.DAY_OF_YEAR, 2)
            hasExplicitDate = true
        } else if (q.contains("next week") || q.contains("agle hafte")) {
            cal.add(Calendar.DAY_OF_YEAR, 7)
            hasExplicitDate = true
        }

        // Days of the week: "next monday", "monday", "somwar", etc.
        val daysOfWeek = mapOf(
            "monday" to Calendar.MONDAY,
            "somwar" to Calendar.MONDAY,
            "tuesday" to Calendar.TUESDAY,
            "mangalwar" to Calendar.TUESDAY,
            "wednesday" to Calendar.WEDNESDAY,
            "budhwar" to Calendar.WEDNESDAY,
            "thursday" to Calendar.THURSDAY,
            "guruwar" to Calendar.THURSDAY,
            "veervar" to Calendar.THURSDAY,
            "friday" to Calendar.FRIDAY,
            "shukrawar" to Calendar.FRIDAY,
            "saturday" to Calendar.SATURDAY,
            "shaniwar" to Calendar.SATURDAY,
            "sunday" to Calendar.SUNDAY,
            "ravivar" to Calendar.SUNDAY
        )

        for ((dayName, dayConstant) in daysOfWeek) {
            if (q.contains(dayName)) {
                val currentDay = cal.get(Calendar.DAY_OF_WEEK)
                var daysUntil = dayConstant - currentDay
                if (daysUntil <= 0 || q.contains("next $dayName") || q.contains("agle $dayName")) {
                    daysUntil += 7
                }
                cal.add(Calendar.DAY_OF_YEAR, daysUntil)
                hasExplicitDate = true
                break
            }
        }

        // Relative days: "after 3 days", "in 5 days", "3 din baad"
        val daysPattern = Pattern.compile("(?i)(?:after|in)\\s*(\\d{1,2})\\s*days|(\\d{1,2})\\s*din\\s*baad")
        val daysMatcher = daysPattern.matcher(q)
        if (daysMatcher.find()) {
            val num = daysMatcher.group(1) ?: daysMatcher.group(2)
            num?.toIntOrNull()?.let {
                cal.add(Calendar.DAY_OF_YEAR, it)
                hasExplicitDate = true
            }
        }

        // Relative hours: "in 2 hours", "3 ghante baad"
        val hoursPattern = Pattern.compile("(?i)(?:in|after)\\s*(\\d{1,2})\\s*hours?|(\\d{1,2})\\s*ghante?\\s*baad")
        val hoursMatcher = hoursPattern.matcher(q)
        if (hoursMatcher.find()) {
            val num = hoursMatcher.group(1) ?: hoursMatcher.group(2)
            num?.toIntOrNull()?.let {
                cal.add(Calendar.HOUR_OF_DAY, it)
                return cal
            }
        }

        // Explicit dates: "28/10/2026", "28-10-2026", "28 Oct"
        val explicitDatePattern = Pattern.compile("\\b(\\d{1,2})[/-](\\d{1,2})(?:[/-](\\d{2,4}))?\\b")
        val expMatcher = explicitDatePattern.matcher(q)
        if (expMatcher.find()) {
            val d = expMatcher.group(1)?.toIntOrNull() ?: 1
            val m = (expMatcher.group(2)?.toIntOrNull() ?: 1) - 1
            val y = expMatcher.group(3)?.toIntOrNull() ?: cal.get(Calendar.YEAR)
            val fullYear = if (y < 100) 2000 + y else y
            cal.set(fullYear, m, d)
            hasExplicitDate = true
        }

        // Month by name: "28 Oct", "28th October"
        val monthNamePattern = Pattern.compile("\\b(\\d{1,2})(?:st|nd|rd|th)?\\s+(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*(?:\\s+(\\d{2,4}))?\\b", Pattern.CASE_INSENSITIVE)
        val monthMatcher = monthNamePattern.matcher(q)
        if (monthMatcher.find()) {
            val d = monthMatcher.group(1)?.toIntOrNull() ?: 1
            val mStr = monthMatcher.group(2)?.lowercase(Locale.getDefault())?.take(3) ?: "jan"
            val mIdx = when (mStr) {
                "jan" -> 0; "feb" -> 1; "mar" -> 2; "apr" -> 3; "may" -> 4; "jun" -> 5
                "jul" -> 6; "aug" -> 7; "sep" -> 8; "oct" -> 9; "nov" -> 10; "dec" -> 11
                else -> 0
            }
            val y = monthMatcher.group(3)?.toIntOrNull() ?: cal.get(Calendar.YEAR)
            val fullYear = if (y < 100) 2000 + y else y
            cal.set(fullYear, mIdx, d)
            hasExplicitDate = true
        }

        // Time parsing: e.g. "9 am", "10:30 am", "5 pm", "8 baje", "shaam 6 baje"
        val timePattern = Pattern.compile("(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm|baje|subah|shaam|raat)?", Pattern.CASE_INSENSITIVE)
        val timeMatcher = timePattern.matcher(q)
        var hasSpecificTime = false

        while (timeMatcher.find()) {
            val rawHour = timeMatcher.group(1)?.toIntOrNull()
            if (rawHour != null && rawHour in 1..24) {
                var hour = rawHour
                val min = timeMatcher.group(2)?.toIntOrNull() ?: 0
                val modifier = timeMatcher.group(3)?.lowercase(Locale.getDefault())

                if (modifier == "pm" || modifier == "shaam" || modifier == "raat") {
                    if (hour < 12) hour += 12
                } else if (modifier == "am" || modifier == "subah") {
                    if (hour == 12) hour = 0
                }

                cal.set(Calendar.HOUR_OF_DAY, hour)
                cal.set(Calendar.MINUTE, min)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                hasSpecificTime = true
                break
            }
        }

        if (hasExplicitDate) {
            if (!hasSpecificTime) {
                // Default to 09:00 AM on the resolved date
                cal.set(Calendar.HOUR_OF_DAY, 9)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
            }
            return cal
        }

        return if (hasSpecificTime) cal else null
    }

    private fun extractDateFromText(text: String): Long? {
        // Regex 1: Contextual label followed by date: "Valid till: 28/10/2026", "Expires: 28-10-2026"
        val contextPattern = Pattern.compile(
            "(?i)(?:valid\\s+till|validity|expiry\\s+date|expires\\s+on|expires|due\\s+date|end\\s+date|valid\\s+upto)[\\s:=-]*([0-9]{1,2}[/-][0-9]{1,2}(?:[/-][0-9]{2,4})?)",
            Pattern.CASE_INSENSITIVE
        )
        val contextMatcher = contextPattern.matcher(text)
        if (contextMatcher.find()) {
            val dateStr = contextMatcher.group(1)
            parseNumericDate(dateStr)?.let { return it }
        }

        // Regex 2: Contextual label with month name: "Valid till: 28 Oct", "Expires on: 15 November 2026"
        val textMonthPattern = Pattern.compile(
            "(?i)(?:valid\\s+till|validity|expiry|expires|due\\s+date)[\\s:=-]*([0-9]{1,2}(?:st|nd|rd|th)?\\s+(?:jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec)[a-z]*(?:\\s+[0-9]{2,4})?)",
            Pattern.CASE_INSENSITIVE
        )
        val textMatcher = textMonthPattern.matcher(text)
        if (textMatcher.find()) {
            val dateStr = textMatcher.group(1)
            parseTextMonthDate(dateStr)?.let { return it }
        }

        // Regex 3: Standalone date patterns
        val standalonePattern = Pattern.compile("\\b(\\d{1,2})[/-](\\d{1,2})[/-](\\d{2,4})\\b")
        val standaloneMatcher = standalonePattern.matcher(text)
        if (standaloneMatcher.find()) {
            parseNumericDate(standaloneMatcher.group(0))?.let { return it }
        }

        val standaloneMonthPattern = Pattern.compile("\\b(\\d{1,2})(?:st|nd|rd|th)?\\s+(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*(?:\\s+(\\d{2,4}))?\\b", Pattern.CASE_INSENSITIVE)
        val monthMatcher = standaloneMonthPattern.matcher(text)
        if (monthMatcher.find()) {
            parseTextMonthDate(monthMatcher.group(0))?.let { return it }
        }

        return null
    }

    private fun parseNumericDate(raw: String?): Long? {
        if (raw == null) return null
        val parts = raw.split('/', '-')
        if (parts.size >= 2) {
            val d = parts[0].trim().toIntOrNull() ?: return null
            val m = (parts[1].trim().toIntOrNull() ?: return null) - 1
            val y = if (parts.size >= 3) {
                val parsedY = parts[2].trim().toIntOrNull() ?: Calendar.getInstance().get(Calendar.YEAR)
                if (parsedY < 100) 2000 + parsedY else parsedY
            } else {
                Calendar.getInstance().get(Calendar.YEAR)
            }

            val cal = Calendar.getInstance().apply {
                set(y, m, d, 9, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }
            return cal.timeInMillis
        }
        return null
    }

    private fun parseTextMonthDate(raw: String?): Long? {
        if (raw == null) return null
        val clean = raw.replace(Regex("(?i)(st|nd|rd|th)"), "").trim()
        val parts = clean.split("\\s+".toRegex())
        if (parts.size >= 2) {
            val d = parts[0].toIntOrNull() ?: return null
            val monthStr = parts[1].lowercase(Locale.getDefault()).take(3)
            val monthIdx = when (monthStr) {
                "jan" -> 0; "feb" -> 1; "mar" -> 2; "apr" -> 3; "may" -> 4; "jun" -> 5
                "jul" -> 6; "aug" -> 7; "sep" -> 8; "oct" -> 9; "nov" -> 10; "dec" -> 11
                else -> return null
            }
            val y = if (parts.size >= 3) {
                parts[2].toIntOrNull() ?: Calendar.getInstance().get(Calendar.YEAR)
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

    fun formatTimestamp(timestamp: Long): String {
        val sdf = SimpleDateFormat("EEE, dd MMM yyyy, h:mm a", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}
