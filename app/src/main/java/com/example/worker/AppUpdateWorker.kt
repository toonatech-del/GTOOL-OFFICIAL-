package com.example.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.R
import com.example.receiver.UpdateDismissReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

class AppUpdateWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "AppUpdateWorker"
        const val CHANNEL_ID = "channel_app_updates"
        const val NOTIFICATION_ID = 9001
        const val WORK_NAME_PERIODIC = "GTOOLX_PERIODIC_APP_UPDATE_CHECK"
        const val WORK_NAME_ONETIME = "GTOOLX_ONETIME_APP_UPDATE_CHECK"

        // Default GitHub Repository Configuration
        var GITHUB_OWNER = "toonatech"
        var GITHUB_REPO = "GTOOL-X"

        /**
         * Reliable semantic version comparison ignoring leading 'v' and suffix descriptors.
         */
        fun isNewerVersion(latestTag: String, currentVersion: String): Boolean {
            fun parseParts(v: String): List<Int> {
                val clean = v.lowercase()
                    .trim()
                    .removePrefix("v")
                    .split("-")[0]
                    .split("+")[0]
                return clean.split(".").mapNotNull { it.trim().toIntOrNull() }
            }

            val latestParts = parseParts(latestTag)
            val currentParts = parseParts(currentVersion)

            if (latestParts.isEmpty() || currentParts.isEmpty()) return false

            val maxLen = maxOf(latestParts.size, currentParts.size)
            for (i in 0 until maxLen) {
                val l = latestParts.getOrElse(i) { 0 }
                val c = currentParts.getOrElse(i) { 0 }
                if (l > c) return true
                if (l < c) return false
            }
            return false
        }
        fun scheduleAppUpdateChecks(context: Context) {
            try {
                val appContext = context.applicationContext
                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()

                // 1. Immediate One-Time Check on startup
                val oneTimeRequest = OneTimeWorkRequestBuilder<AppUpdateWorker>()
                    .setConstraints(constraints)
                    .build()

                WorkManager.getInstance(appContext).enqueue(oneTimeRequest)

                // 2. Periodic Check every 12 Hours
                val periodicRequest = PeriodicWorkRequestBuilder<AppUpdateWorker>(12, TimeUnit.HOURS)
                    .setConstraints(constraints)
                    .build()

                WorkManager.getInstance(appContext).enqueueUniquePeriodicWork(
                    WORK_NAME_PERIODIC,
                    ExistingPeriodicWorkPolicy.KEEP,
                    periodicRequest
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to schedule WorkManager update checks", e)
            }
        }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val apiUrl = "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases/latest"
            val url = URL(apiUrl)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                setRequestProperty("User-Agent", "GTOOL-X-Android-App")
                connectTimeout = 10000
                readTimeout = 10000
            }

            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                Log.w(TAG, "GitHub API returned status $responseCode")
                return@withContext Result.success() // Gracefully succeed on rate-limit / 404
            }

            val responseText = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(responseText)

            val latestTag = json.optString("tag_name", "").trim()
            val htmlUrl = json.optString("html_url", "https://github.com/$GITHUB_OWNER/$GITHUB_REPO/releases")
            
            // Prefer direct APK download URL from assets if present
            var downloadUrl = htmlUrl
            val assets = json.optJSONArray("assets")
            if (assets != null && assets.length() > 0) {
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val name = asset.optString("name", "")
                    if (name.endsWith(".apk", ignoreCase = true)) {
                        downloadUrl = asset.optString("browser_download_url", htmlUrl)
                        break
                    }
                }
            }

            if (latestTag.isBlank()) {
                return@withContext Result.success()
            }

            val currentVersionName = getCurrentVersionName(context)
            Log.d(TAG, "Latest GitHub release tag: '$latestTag', Installed app version: '$currentVersionName'")

            if (isNewerVersion(latestTag, currentVersionName)) {
                showUpdateNotification(context, latestTag, downloadUrl)
            }

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error checking for app update from GitHub", e)
            Result.success() // Do not crash or retry aggressively on network failure
        }
    }

    private fun getCurrentVersionName(context: Context): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }
    }

    /**
     * Reliable semantic version comparison ignoring leading 'v' and suffix descriptors.
     */
    internal fun isNewerVersion(latestTag: String, currentVersion: String): Boolean {
        fun parseParts(v: String): List<Int> {
            val clean = v.lowercase()
                .trim()
                .removePrefix("v")
                .split("-")[0]
                .split("+")[0]
            return clean.split(".").mapNotNull { it.trim().toIntOrNull() }
        }

        val latestParts = parseParts(latestTag)
        val currentParts = parseParts(currentVersion)

        if (latestParts.isEmpty() || currentParts.isEmpty()) return false

        val maxLen = maxOf(latestParts.size, currentParts.size)
        for (i in 0 until maxLen) {
            val l = latestParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (l > c) return true
            if (l < c) return false
        }
        return false
    }

    private fun showUpdateNotification(context: Context, versionTag: String, downloadUrl: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "App Updates",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for new GTOOL X app updates and releases"
                enableLights(true)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Action 1: Update Intent
        val updateIntent = Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val updatePendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID + 1,
            updateIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action 2: Dismiss Intent
        val dismissIntent = Intent(context, UpdateDismissReceiver::class.java).apply {
            putExtra(UpdateDismissReceiver.EXTRA_NOTIFICATION_ID, NOTIFICATION_ID)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context,
            NOTIFICATION_ID + 2,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val cleanTag = if (versionTag.startsWith("v", ignoreCase = true)) versionTag else "v$versionTag"

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("New Update Available: $cleanTag")
            .setContentText("A new version of GTOOL X is available to install.")
            .setStyle(NotificationCompat.BigTextStyle().bigText("A new version ($cleanTag) of GTOOL X is available. Tap Update to download the latest features and improvements."))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(updatePendingIntent)
            .addAction(R.mipmap.ic_launcher, "Update", updatePendingIntent)
            .addAction(0, "Dismiss", dismissPendingIntent)

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build())
        } catch (e: SecurityException) {
            Log.e(TAG, "Notification permission missing when attempting to show update alert", e)
        }
    }
}
