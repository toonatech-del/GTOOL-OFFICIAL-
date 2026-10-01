package com.example.util

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

/**
 * Exact Alarm BroadcastReceiver triggered by Android AlarmManager.
 * Guaranteed to fire even in Doze mode via AlarmManagerCompat.setExactAndAllowWhileIdle()
 * with WakeLock and goAsync() reliability.
 */
open class ReminderBroadcastReceiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_ID = "gtool_reminders_channel"
        const val CHANNEL_NAME = "GTOOL X Reminders"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val reminderId = intent.getLongExtra("reminder_id", System.currentTimeMillis())
        val taskTitle = intent.getStringExtra("description") ?: "Scheduled Task Reminder"
        val relatedItemId = intent.getStringExtra("related_item_id")

        // Acquire temporary WakeLock to guarantee execution during Doze mode / screen-off
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val wakeLock = powerManager?.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "GTOOL:ReminderWakeLock"
        )
        wakeLock?.acquire(15 * 1000L) // 15 seconds max

        // 1. Commit structured record into Room DB & mark reminder completed
        val db = AppDatabase.getInstance(context)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                db.notificationDao().insertNotification(
                    NotificationEntity(
                        title = "GTOOL X Reminder",
                        content = taskTitle,
                        type = "reminder",
                        timestamp = System.currentTimeMillis(),
                        isRead = false,
                        scheduledTime = System.currentTimeMillis(),
                        reminderId = reminderId
                    )
                )
                db.reminderDao().markAsCompleted(reminderId)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                // 2. Trigger high-importance heads-up system notification
                showHeadsUpNotification(context, reminderId, taskTitle, relatedItemId)
                try {
                    wakeLock?.release()
                } catch (_: Exception) {}
                pendingResult.finish()
            }
        }
    }

    @android.annotation.SuppressLint("MissingPermission")
    private fun showHeadsUpNotification(
        context: Context,
        id: Long,
        taskTitle: String,
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
                description = "High-importance contextual task reminders & alerts"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 400, 200, 400)
                setSound(soundUri, audioAttributes)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("opened_from_reminder", true)
            putExtra("reminder_title", taskTitle)
            if (relatedItemId != null) {
                putExtra("reminder_item_id", relatedItemId)
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            id.toInt(),
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("GTOOL X Reminder")
            .setContentText(taskTitle)
            .setStyle(NotificationCompat.BigTextStyle().bigText(taskTitle))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setSound(soundUri)
            .setVibrate(longArrayOf(0, 400, 200, 400))
            .setContentIntent(pendingIntent)
            .addAction(0, "Open in GTOOL X", pendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(id.toInt(), notification)
    }
}
