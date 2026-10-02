package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import java.io.File
import java.io.FileOutputStream

suspend fun <T> com.google.android.gms.tasks.Task<T>.awaitTask(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { result -> cont.resume(result) }
    addOnFailureListener { exception -> cont.resumeWithException(exception) }
}

object PdfTextExtractor {

    private const val TAG = "PdfTextExtractor"

    data class ExtractionResult(
        val fullTranscript: String,
        val pageCount: Int,
        val totalWords: Int,
        val perPageText: Map<Int, String>
    )

    /**
     * Sequentially processes and extracts OCR text from all pages (1 to N) of a PDF document without memory leaks.
     * Reports live progress via onProgress callback.
     */
    suspend fun extractTextFromAllPages(
        context: Context,
        pdfUri: Uri,
        onProgress: (currentPage: Int, totalPages: Int) -> Unit = { _, _ -> }
    ): ExtractionResult = withContext(Dispatchers.IO) {
        val pageTextMap = mutableMapOf<Int, String>()
        val fullBuilder = StringBuilder()
        val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        var tempFile: File? = null
        val pfd: ParcelFileDescriptor? = try {
            context.contentResolver.openFileDescriptor(pdfUri, "r")
        } catch (e: Exception) {
            try {
                val cacheDir = context.cacheDir
                val file = File(cacheDir, "temp_ocr_${System.currentTimeMillis()}.pdf")
                context.contentResolver.openInputStream(pdfUri)?.use { input ->
                    FileOutputStream(file).use { output ->
                        input.copyTo(output)
                    }
                }
                tempFile = file
                ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            } catch (err: Exception) {
                Log.e(TAG, "Error opening PDF file descriptor for OCR: $pdfUri", err)
                null
            }
        }

        if (pfd == null) {
            return@withContext ExtractionResult("", 0, 0, emptyMap())
        }

        var renderer: PdfRenderer? = null
        try {
            renderer = PdfRenderer(pfd)
            val totalPages = renderer.pageCount

            for (index in 0 until totalPages) {
                // Report progress to UI (1-indexed)
                onProgress(index + 1, totalPages)

                var page: PdfRenderer.Page? = null
                var bitmap: Bitmap? = null
                try {
                    page = renderer.openPage(index)
                    // High-res render for maximum ML Kit OCR accuracy
                    val scale = 2
                    val width = (page.width * scale).coerceAtLeast(1)
                    val height = (page.height * scale).coerceAtLeast(1)
                    bitmap = Bitmap.createBitmap(
                        width,
                        height,
                        Bitmap.Config.ARGB_8888
                    )
                    val canvas = android.graphics.Canvas(bitmap)
                    canvas.drawColor(Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                    val image = InputImage.fromBitmap(bitmap, 0)
                    val visionResult = textRecognizer.process(image).awaitTask()
                    val extractedPageText = visionResult.text.trim()

                    if (extractedPageText.isNotEmpty()) {
                        pageTextMap[index + 1] = extractedPageText
                        fullBuilder.append("--- Page ${index + 1} of $totalPages ---\n")
                        fullBuilder.append(extractedPageText)
                        fullBuilder.append("\n\n")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing page $index in PDF", e)
                } finally {
                    try {
                        page?.close()
                    } catch (ignored: Exception) {}
                    try {
                        bitmap?.recycle()
                    } catch (ignored: Exception) {}
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during multi-page PDF OCR extraction", e)
        } finally {
            try {
                renderer?.close()
            } catch (ignored: Exception) {}
            try {
                pfd.close()
            } catch (ignored: Exception) {}
            tempFile?.delete()
        }

        val finalString = fullBuilder.toString().trim()
        val wordCount = if (finalString.isBlank()) 0 else finalString.split(Regex("""\s+""")).count { it.isNotBlank() }

        ExtractionResult(
            fullTranscript = finalString,
            pageCount = if (pageTextMap.isNotEmpty()) pageTextMap.size else 0,
            totalWords = wordCount,
            perPageText = pageTextMap
        )
    }

    /**
     * Backward-compatible helper to extract all pages into a unified string.
     */
    suspend fun extractTextFromPdfUri(context: Context, pdfUri: Uri): String {
        return extractTextFromAllPages(context, pdfUri).fullTranscript
    }
}
