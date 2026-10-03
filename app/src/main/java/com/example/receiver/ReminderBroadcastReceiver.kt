package com.example.receiver

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.data.local.AppDatabase
import com.example.data.local.NotificationEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Authoritative Exact Alarm BroadcastReceiver triggered by Android AlarmManager.
 * Handles Doze mode WakeLock, goAsync background database commits, debouncing,
 * and high-importance heads-up notifications with deep links to MainActivity.
 */
class ReminderBroadcastReceiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_ID = "gtool_x_reminder_channel_v1"
        const val CHANNEL_NAME = "GTOOL X Reminders & Tasks"
        private const val PREFS_NAME = "gtool_reminder_dedup"
        private const val KEY_LAST_TITLE = "last_title"
        private const val KEY_LAST_TIMESTAMP = "last_timestamp"
        private const val DEDUP_WINDOW_MS = 6000L // 6 seconds debounce window
    }

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val action = intent.action
        if (action != null && action != "com.gsd.gtoolx.ACTION_REMINDER_ALARM") {
            pendingResult.finish()
            return
        }

        val rawTitle = intent.getStringExtra("REMINDER_TITLE")
            ?: intent.getStringExtra("description")
            ?: "Task Reminder"

        val message = intent.getStringExtra("REMINDER_MSG")
            ?: intent.getStringExtra("description")
            ?: "Your scheduled task is due now."

        val reminderIdLong = intent.getLongExtra("reminder_id", -1L)
        val reminderIdInt = intent.getIntExtra("REMINDER_ID", reminderIdLong.toInt())
        val finalReminderId = if (reminderIdLong != -1L) reminderIdLong else reminderIdInt.toLong()

        val relatedItemId = intent.getStringExtra("related_item_id")
            ?: intent.getStringExtra("open_item_id")

        val normalizedTitle = rawTitle.trim().lowercase(Locale.ROOT)
        val now = System.currentTimeMillis()

        // Debounce check
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastTitle = prefs.getString(KEY_LAST_TITLE, null)
        val lastTime = prefs.getLong(KEY_LAST_TIMESTAMP, 0L)

        if (normalizedTitle == lastTitle && (now - lastTime) < DEDUP_WINDOW_MS) {
            pendingResult.finish()
            return
        }

        prefs.edit()
            .putString(KEY_LAST_TITLE, normalizedTitle)
            .putLong(KEY_LAST_TIMESTAMP, now)
            .apply()

        // Acquire WakeLock for Doze mode / screen off execution
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val wakeLock = powerManager?.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "GTOOL:ReminderWakeLock"
        )
        wakeLock?.acquire(15 * 1000L)

        val db = AppDatabase.getInstance(context)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (finalReminderId > 0) {
                    db.notificationDao().insertNotification(
                        NotificationEntity(
                            title = "GTOOL X Reminder",
                            content = rawTitle,
                            type = "reminder",
                            timestamp = now,
                            isRead = false,
                            scheduledTime = now,
                            reminderId = finalReminderId
                        )
                    )
                    db.reminderDao().markAsCompleted(finalReminderId)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                showHeadsUpNotification(context, finalReminderId, rawTitle, message, relatedItemId)
                try {
                    wakeLock?.release()
                } catch (_: Exception) {}
                pendingResult.finish()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun showHeadsUpNotification(
        context: Context,
        id: Long,
        title: String,
        message: String,
        relatedItemId: String?
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                .build()

            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "High-importance task reminders and urgent alerts"
                enableLights(true)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 250, 500)
                setSound(soundUri, audioAttributes)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("opened_from_reminder", true)
            putExtra("reminder_title", title)
            if (relatedItemId != null) {
                putExtra("reminder_item_id", relatedItemId)
                putExtra("open_item_id", relatedItemId)
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            id.toInt(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Remove redundant "GTOOL X" prefixes since system header already shows app name
        val cleanTitle = title
            .removePrefix("GTOOL X:")
            .removePrefix("GTOOL X")
            .removePrefix("⏰ GTOOL X:")
            .trim()
            .ifBlank { "Task Reminder" }

        val cleanMessage = when {
            message.startsWith("Scheduled Task:", ignoreCase = true) -> {
                val sub = message.removePrefix("Scheduled Task:").trim()
                if (sub.equals(cleanTitle, ignoreCase = true) || sub.isBlank()) "Task reminder is due now" else sub
            }
            message.equals(cleanTitle, ignoreCase = true) -> "Task reminder is due now"
            else -> message.trim().ifBlank { "Task reminder is due now" }
        }

        val notifTag = "gtool_reminder_${cleanTitle.trim().lowercase(Locale.ROOT)}"

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(com.example.R.mipmap.ic_launcher)
            .setContentTitle(cleanTitle)
            .setContentText(cleanMessage)
            .setStyle(NotificationCompat.BigTextStyle().bigText(cleanMessage))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSound(soundUri)
            .setVibrate(longArrayOf(0, 500, 250, 500))
            .setOnlyAlertOnce(true)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .addAction(0, "Open Task", pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notifTag, 1001, notification)
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }
}
