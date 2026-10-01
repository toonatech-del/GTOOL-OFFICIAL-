package com.example.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import java.util.Locale

class ReminderBroadcastReceiver : BroadcastReceiver() {

    companion object {
        private const val PREFS_NAME = "gtool_reminder_dedup"
        private const val KEY_LAST_TITLE = "last_title"
        private const val KEY_LAST_TIMESTAMP = "last_timestamp"
        private const val DEDUP_WINDOW_MS = 6000L // 6 seconds debounce window
    }

    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("REMINDER_TITLE") ?: "Task Reminder"
        val message = intent.getStringExtra("REMINDER_MSG") ?: "Your scheduled task is due now."
        val reminderId = intent.getIntExtra("REMINDER_ID", System.currentTimeMillis().toInt())

        val normalizedTitle = title.trim().lowercase(Locale.ROOT)
        val now = System.currentTimeMillis()

        // Persistent debounce guard across broadcast invocations to prevent duplicate alerts
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastTitle = prefs.getString(KEY_LAST_TITLE, null)
        val lastTime = prefs.getLong(KEY_LAST_TIMESTAMP, 0L)

        if (normalizedTitle == lastTitle && (now - lastTime) < DEDUP_WINDOW_MS) {
            // Deduplicate: same task reminder already alerted within debounce window
            return
        }

        prefs.edit()
            .putString(KEY_LAST_TITLE, normalizedTitle)
            .putLong(KEY_LAST_TIMESTAMP, now)
            .apply()

        val channelId = "gtool_x_reminder_channel_v1"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create High Importance Notification Channel for Android 8.0+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "GTOOL X Reminders & Tasks",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Urgent alerts and task reminder notifications"
                enableLights(true)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 250, 500)
                setBypassDnd(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            reminderId,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        // Using consistent notification tag derived from normalized title to prevent duplicate cards
        val notifTag = "gtool_reminder_${normalizedTitle}"

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("⏰ GTOOL X: $title")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSound(soundUri)
            .setVibrate(longArrayOf(0, 500, 250, 500))
            .setOnlyAlertOnce(true)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notifTag, 1001, notification)
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }
}
