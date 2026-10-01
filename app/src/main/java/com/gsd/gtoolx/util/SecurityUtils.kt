package com.gsd.gtoolx.util

import android.app.Activity
import android.content.Context
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import java.io.File

object SecurityUtils {
    // Return isolated private folder inside internal sandbox (inaccessible to other apps or adb backup)
    fun getSecureVaultDir(context: Context): File {
        return File(context.noBackupFilesDir, "secure_vault").apply {
            if (!exists()) mkdirs()
        }
    }
}

// Composable modifier to block screenshots, video capture, and recent-apps preview leakage
@Composable
fun PreventScreenCapture() {
    val context = LocalContext.current
    val activity = context as? Activity ?: return

    DisposableEffect(activity) {
        runCatching {
            activity.window?.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
            )
        }
        onDispose {
            runCatching {
                activity.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }
        }
    }
}
