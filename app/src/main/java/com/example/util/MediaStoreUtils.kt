package com.example.util

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import java.io.InputStream

object MediaStoreUtils {

    private const val TAG = "MediaStoreUtils"

    /**
     * Saves a file to a public persistent directory using MediaStore.
     * 
     * @param context The context
     * @param inputStream The source data stream
     * @param fileName The desired file name
     * @param relativePath The relative path within the public directory (e.g., "Documents/GTOOL X/Notes")
     * @param mimeType The MIME type of the file
     * @return The Uri of the saved file, or null if failed
     */
    fun saveFileToPublic(
        context: Context,
        inputStream: InputStream,
        fileName: String,
        relativePath: String,
        mimeType: String
    ): Uri? {
        val contentResolver = context.contentResolver

        val collection = when {
            mimeType.startsWith("image/") -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            mimeType.startsWith("audio/") -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            mimeType.startsWith("video/") -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                if (relativePath.startsWith("Download", ignoreCase = true) ||
                    relativePath.startsWith(Environment.DIRECTORY_DOWNLOADS, ignoreCase = true)
                ) {
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI
                } else {
                    MediaStore.Files.getContentUri("external")
                }
            }
            else -> MediaStore.Files.getContentUri("external")
        }

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }

        var uri: Uri? = null
        try {
            uri = contentResolver.insert(collection, contentValues)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to insert into primary collection $collection with relativePath $relativePath. Trying fallback.", e)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, "Download/GTOOL X")
                    uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                } catch (e2: Exception) {
                    Log.e(TAG, "Fallback insert failed", e2)
                }
            }
        }

        if (uri == null) return null

        try {
            contentResolver.openOutputStream(uri)?.use { outputStream ->
                inputStream.copyTo(outputStream)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                contentResolver.update(uri, contentValues, null, null)
            }
            return uri
        } catch (e: Exception) {
            Log.e(TAG, "Failed writing to Uri $uri", e)
            try {
                contentResolver.delete(uri, null, null)
            } catch (_: Exception) {}
            return null
        }
    }

    /**
     * Special case for Documents.
     */
    fun saveDocToPublic(
        context: Context,
        inputStream: InputStream,
        fileName: String,
        subFolder: String, // e.g. "Invoices" or "Notes"
        mimeType: String
    ): Uri? {
        val relativePath = "Documents/GTOOL X/$subFolder"
        return saveFileToPublic(context, inputStream, fileName, relativePath, mimeType)
    }

    fun saveImageToPublic(
        context: Context,
        inputStream: InputStream,
        fileName: String,
        mimeType: String = "image/jpeg"
    ): Uri? {
        val relativePath = "Pictures/GTOOL X"
        return saveFileToPublic(context, inputStream, fileName, relativePath, mimeType)
    }
}
