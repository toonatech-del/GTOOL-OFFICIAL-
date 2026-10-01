package com.example.util

import android.content.Context
import com.example.data.repository.ReminderRepository
import java.util.Locale

/**
 * Singleton facade providing backward-compatible access to the ReminderRepository.
 */
object ReminderManager {

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
            val message: String
        ) : ReminderResult()

        data class NeedsInteractiveSchedule(
            val detectedTitle: String,
            val query: String
        ) : ReminderResult()
    }

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

    fun extractCleanTaskTitle(query: String): String {
        val q = query.lowercase(Locale.getDefault())
        return when {
            q.contains("recharge") || q.contains("validity") -> "Recharge Expiry Reminder"
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

    suspend fun resolveAndSchedule(context: Context, query: String): ReminderResult {
        val repo = ReminderRepository.getInstance(context)
        return when (val res = repo.resolveAndSchedule(query)) {
            is ReminderRepository.ReminderResult.Scheduled -> {
                ReminderResult.Scheduled(
                    reminderId = res.reminderId,
                    title = res.title,
                    scheduledTimeMillis = res.scheduledTimeMillis,
                    formattedDate = res.formattedDate,
                    sourceVaultDoc = res.sourceVaultDoc,
                    relatedItemId = res.relatedItemId,
                    message = res.message
                )
            }
            is ReminderRepository.ReminderResult.RequiresConfirmation -> {
                ReminderResult.RequiresConfirmation(
                    title = res.title,
                    targetTimestamp = res.targetTimestamp,
                    formattedDate = res.formattedDate,
                    sourceVaultDoc = res.sourceVaultDoc,
                    relatedItemId = res.relatedItemId,
                    message = res.message
                )
            }
            is ReminderRepository.ReminderResult.NeedsInteractiveSchedule -> {
                ReminderResult.NeedsInteractiveSchedule(
                    detectedTitle = res.detectedTitle,
                    query = res.query
                )
            }
        }
    }

    suspend fun commitAndSchedule(
        context: Context,
        title: String,
        timestampMillis: Long,
        relatedItemId: String? = null
    ): Long {
        val repo = ReminderRepository.getInstance(context)
        return repo.commitAndSchedule(title, timestampMillis, relatedItemId)
    }

    fun scheduleExactAlarm(
        context: Context,
        id: Long,
        title: String,
        timestampMillis: Long,
        relatedItemId: String? = null
    ) {
        val repo = ReminderRepository.getInstance(context)
        repo.scheduleExactAlarm(id, title, timestampMillis, relatedItemId)
    }

    fun scheduleExpiryReminders(context: Context, docName: String, expiryTimeMillis: Long) {
        val repo = ReminderRepository.getInstance(context)
        val cleanName = extractCleanTaskTitle(docName)

        // 7 days before
        val sevenDaysBefore = expiryTimeMillis - (7L * 24 * 60 * 60 * 1000)
        if (sevenDaysBefore > System.currentTimeMillis()) {
            val title = "$cleanName Due in 7 Days"
            val id = (sevenDaysBefore % 1000000) + 7000000L
            repo.scheduleExactAlarm(id, title, sevenDaysBefore)
        }

        // 1 day before
        val oneDayBefore = expiryTimeMillis - (1L * 24 * 60 * 60 * 1000)
        if (oneDayBefore > System.currentTimeMillis()) {
            val title = "$cleanName Expiring Tomorrow"
            val id = (oneDayBefore % 1000000) + 1000000L
            repo.scheduleExactAlarm(id, title, oneDayBefore)
        }
    }

    suspend fun cancelReminder(context: Context, reminderId: Long) {
        val repo = ReminderRepository.getInstance(context)
        repo.cancelReminder(reminderId)
    }

    fun formatTimestamp(timestamp: Long): String {
        val sdf = java.text.SimpleDateFormat("EEE, dd MMM yyyy, h:mm a", Locale.getDefault())
        return sdf.format(java.util.Date(timestamp))
    }
}
