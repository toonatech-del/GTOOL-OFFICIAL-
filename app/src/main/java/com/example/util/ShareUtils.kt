package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.model.ItemType
import com.example.model.WorkspaceItem
import java.io.File

object ShareUtils {

    fun shareItem(context: Context, item: WorkspaceItem) {
        try {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                val imageUriStr = item.imageUri
                val driveFileIdStr = item.driveFileId

                when {
                    !imageUriStr.isNullOrBlank() -> {
                        val parsed = Uri.parse(imageUriStr)
                        if (parsed.scheme == "file") {
                            val file = File(parsed.path ?: "")
                            if (file.exists()) {
                                val contentUri = try {
                                    FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        file
                                    )
                                } catch (e: Exception) {
                                    parsed
                                }
                                val mime = if (item.type == ItemType.PDF || item.title.endsWith(".pdf", ignoreCase = true)) "application/pdf" else "image/*"
                                type = mime
                                putExtra(Intent.EXTRA_STREAM, contentUri)
                                putExtra(Intent.EXTRA_SUBJECT, item.title)
                                putExtra(Intent.EXTRA_TEXT, "${item.title}\n\n${item.summary}")
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            } else {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, item.title)
                                putExtra(Intent.EXTRA_TEXT, "${item.title}\n\n${item.contentSnippet ?: item.summary}")
                            }
                        } else if (parsed.scheme == "content") {
                            val mime = if (item.type == ItemType.PDF || item.title.endsWith(".pdf", ignoreCase = true)) "application/pdf" else "image/*"
                            type = mime
                            putExtra(Intent.EXTRA_STREAM, parsed)
                            putExtra(Intent.EXTRA_SUBJECT, item.title)
                            putExtra(Intent.EXTRA_TEXT, "${item.title}\n\n${item.summary}")
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        } else {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, item.title)
                            putExtra(Intent.EXTRA_TEXT, "${item.title}\n\n${item.contentSnippet ?: item.summary}")
                        }
                    }
                    !driveFileIdStr.isNullOrBlank() && (driveFileIdStr.startsWith("file:") || driveFileIdStr.startsWith("content:")) -> {
                        val parsed = Uri.parse(driveFileIdStr)
                        if (parsed.scheme == "file") {
                            val file = File(parsed.path ?: "")
                            if (file.exists()) {
                                val contentUri = try {
                                    FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        file
                                    )
                                } catch (e: Exception) {
                                    parsed
                                }
                                type = "application/pdf"
                                putExtra(Intent.EXTRA_STREAM, contentUri)
                                putExtra(Intent.EXTRA_SUBJECT, item.title)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            } else {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, item.title)
                                putExtra(Intent.EXTRA_TEXT, "${item.title}\n\n${item.contentSnippet ?: item.summary}")
                            }
                        } else {
                            type = "application/pdf"
                            putExtra(Intent.EXTRA_STREAM, parsed)
                            putExtra(Intent.EXTRA_SUBJECT, item.title)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                    }
                    else -> {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, item.title)
                        putExtra(Intent.EXTRA_TEXT, "${item.title}\n\n${item.contentSnippet ?: item.summary}")
                    }
                }
            }

            val chooser = Intent.createChooser(shareIntent, "Share \"${item.title}\"")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
