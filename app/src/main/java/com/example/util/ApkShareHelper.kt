package com.example.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import java.io.File

/**
 * Production-grade utility to share the active GTOOL X APK installer dynamically.
 * Reads the current version name from PackageInfo to generate a professional filename.
 */
object ApkShareHelper {

    fun shareAppApk(context: Context) {
        try {
            val appInfo = context.applicationInfo
            val originalApk = File(appInfo.sourceDir)
            
            // 1. Resolve Dynamic Version Name
            val versionName = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    context.packageManager.getPackageInfo(
                        context.packageName,
                        PackageManager.PackageInfoFlags.of(0)
                    ).versionName
                } else {
                    @Suppress("DEPRECATION")
                    context.packageManager.getPackageInfo(context.packageName, 0).versionName
                }
            } catch (e: Exception) {
                "2.6.0" // Fallback to current major
            }

            // 2. Prepare Cache Destination
            val cacheDir = File(context.cacheDir, "shared_apk")
            if (!cacheDir.exists()) cacheDir.mkdirs()
            
            // Target format: GTOOL_X_vX.Y.Z.apk
            val targetFileName = "GTOOL_X_v$versionName.apk"
            val targetFile = File(cacheDir, targetFileName)
            
            // 3. Copy APK from sourceDir to cache (ensures clean filename for receiver)
            originalApk.copyTo(targetFile, overwrite = true)

            // 4. Generate Content URI via FileProvider
            val authority = "${context.packageName}.fileprovider"
            val apkUri: Uri = FileProvider.getUriForFile(context, authority, targetFile)
            
            // 5. Build and Launch Share Intent
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/vnd.android.package-archive"
                putExtra(Intent.EXTRA_STREAM, apkUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            context.startActivity(Intent.createChooser(shareIntent, "Share GTOOL X via..."))
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
