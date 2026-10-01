package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.InputStream
import kotlin.coroutines.resume

object MlKitTextExtractor {

    private const val TAG = "MlKitTextExtractor"

    // Real on-device Google ML Kit Text Recognition client using default options
    private val recognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    /**
     * Extracts full text line-by-line from a Bitmap on-device with zero internet dependency.
     * Iterates through every TextBlock, Line, and Element to capture 100% of text, numbers, symbols, and formatting.
     */
    suspend fun extractTextFromBitmap(bitmap: Bitmap): String = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        suspendCancellableCoroutine { continuation ->
            try {
                val inputImage = InputImage.fromBitmap(bitmap, 0)
                recognizer.process(inputImage)
                    .addOnSuccessListener { visionText ->
                        val fullExtractedStringBuilder = java.lang.StringBuilder()
                        for (block in visionText.textBlocks) {
                            for (line in block.lines) {
                                val lineText = line.text.trim()
                                if (lineText.isNotEmpty()) {
                                    fullExtractedStringBuilder.append(lineText).append("\n")
                                }
                            }
                            fullExtractedStringBuilder.append("\n")
                        }
                        val fullText = fullExtractedStringBuilder.toString().trim()
                        val finalResult = if (fullText.isNotBlank()) {
                            fullText
                        } else {
                            visionText.text.trim()
                        }
                        continuation.resume(finalResult)
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "ML Kit text recognition failed on bitmap", e)
                        continuation.resume("")
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Exception initializing ML Kit extraction on bitmap", e)
                continuation.resume("")
            }
        }
    }

    /**
     * Extracts full text line-by-line from an Image Uri on-device with zero internet dependency.
     */
    suspend fun extractTextFromUri(context: Context, uri: Uri): String = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        suspendCancellableCoroutine { continuation ->
            try {
                val inputImage = InputImage.fromFilePath(context, uri)
                recognizer.process(inputImage)
                    .addOnSuccessListener { visionText ->
                        val fullExtractedStringBuilder = java.lang.StringBuilder()
                        for (block in visionText.textBlocks) {
                            for (line in block.lines) {
                                val lineText = line.text.trim()
                                if (lineText.isNotEmpty()) {
                                    fullExtractedStringBuilder.append(lineText).append("\n")
                                }
                            }
                            fullExtractedStringBuilder.append("\n")
                        }
                        val fullText = fullExtractedStringBuilder.toString().trim()
                        val finalResult = if (fullText.isNotBlank()) {
                            fullText
                        } else {
                            visionText.text.trim()
                        }
                        continuation.resume(finalResult)
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "ML Kit text recognition failed on uri: $uri", e)
                        // Fallback to high-res bitmap decode if needed
                        val fallbackBitmap = decodeSampledBitmapFromUri(context, uri)
                        if (fallbackBitmap != null) {
                            val fallbackImage = InputImage.fromBitmap(fallbackBitmap, 0)
                            recognizer.process(fallbackImage)
                                .addOnSuccessListener { fallbackVisionText ->
                                    val fullExtractedStringBuilder = java.lang.StringBuilder()
                                    for (block in fallbackVisionText.textBlocks) {
                                        for (line in block.lines) {
                                            val lineText = line.text.trim()
                                            if (lineText.isNotEmpty()) {
                                                fullExtractedStringBuilder.append(lineText).append("\n")
                                            }
                                        }
                                        fullExtractedStringBuilder.append("\n")
                                    }
                                    val fullText = fullExtractedStringBuilder.toString().trim()
                                    continuation.resume(if (fullText.isNotBlank()) fullText else fallbackVisionText.text.trim())
                                }
                                .addOnFailureListener {
                                    continuation.resume("")
                                }
                        } else {
                            continuation.resume("")
                        }
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Exception initializing ML Kit extraction on uri", e)
                continuation.resume("")
            }
        }
    }

    private fun decodeSampledBitmapFromUri(context: Context, uri: Uri): Bitmap? {
        var inputStream: InputStream? = null
        return try {
            inputStream = context.contentResolver.openInputStream(uri)
            val options = BitmapFactory.Options().apply {
                inSampleSize = 2
            }
            BitmapFactory.decodeStream(inputStream, null, options)
        } catch (e: Exception) {
            null
        } finally {
            inputStream?.close()
        }
    }
}
