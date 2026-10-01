package com.example.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import com.example.receiver.ReminderBroadcastReceiver

object ReminderScheduler {
    fun scheduleAlarm(
        context: Context,
        reminderId: Int,
        title: String,
        message: String,
        triggerTimeMillis: Long
    ) {
        if (triggerTimeMillis <= System.currentTimeMillis()) {
            Toast.makeText(context, "Cannot schedule reminder for a past time", Toast.LENGTH_SHORT).show()
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = "com.gsd.gtoolx.ACTION_REMINDER_ALARM"
            putExtra("REMINDER_ID", reminderId)
            putExtra("REMINDER_TITLE", title)
            putExtra("REMINDER_MSG", message)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTimeMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTimeMillis,
                    pendingIntent
                )
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTimeMillis,
                pendingIntent
            )
        }
    }
}
