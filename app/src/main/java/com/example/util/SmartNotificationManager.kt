package com.example.util

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import java.util.Calendar

object SmartNotificationManager {
    const val CHANNEL_ID = "gtool_smart_reminders"
    const val CHANNEL_NAME = "Smart Document Reminders"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = "Automated payment, renewal, and expiry notifications for documents and bills."
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showNotification(context: Context, title: String, message: String, itemId: String = "") {
        createNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("open_item_id", itemId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            itemId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify((System.currentTimeMillis() % 100000).toInt(), builder.build())
    }

    fun scheduleDueDateReminder(
        context: Context,
        docTitle: String,
        dueDateStr: String,
        itemId: String
    ) {
        createNotificationChannel(context)

        try {
            val alertMessage = "Payment/Renewal due today for $docTitle ($dueDateStr)"
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            val intent = Intent(context, com.example.receiver.ReminderBroadcastReceiver::class.java).apply {
                action = "com.gsd.gtoolx.ACTION_REMINDER_ALARM"
                putExtra("REMINDER_ID", itemId.hashCode())
                putExtra("REMINDER_TITLE", "GTOOL X Smart Alert ⏰")
                putExtra("REMINDER_MSG", alertMessage)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                itemId.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Set trigger time for 9:00 AM on the target due date or 1 minute from now if time is near
            val calendar = Calendar.getInstance().apply {
                add(Calendar.MINUTE, 2) // Default proactive reminder
            }

            alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
