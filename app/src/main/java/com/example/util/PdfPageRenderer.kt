package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object PdfPageRenderer {

    private const val TAG = "PdfPageRenderer"

    suspend fun renderPdfPages(
        context: Context,
        pdfUri: Uri?,
        maxPages: Int = 20
    ): List<Bitmap> = withContext(Dispatchers.IO) {
        val bitmaps = mutableListOf<Bitmap>()
        if (pdfUri == null) return@withContext bitmaps

        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        var tempFile: File? = null

        try {
            pfd = try {
                context.contentResolver.openFileDescriptor(pdfUri, "r")
            } catch (e: Exception) {
                val cacheDir = context.cacheDir
                val file = File(cacheDir, "pdf_preview_${System.currentTimeMillis()}.pdf")
                context.contentResolver.openInputStream(pdfUri)?.use { input ->
                    FileOutputStream(file).use { output ->
                        input.copyTo(output)
                    }
                }
                tempFile = file
                ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            }

            if (pfd != null) {
                renderer = PdfRenderer(pfd)
                val pageCount = minOf(renderer.pageCount, maxPages)

                for (pageIndex in 0 until pageCount) {
                    val page = renderer.openPage(pageIndex)
                    // High-res rendering scale 2.0 for crisp text reading
                    val width = (page.width * 2).coerceAtLeast(400)
                    val height = (page.height * 2).coerceAtLeast(600)
                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    bitmap.eraseColor(android.graphics.Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    page.close()
                    bitmaps.add(bitmap)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to render PDF pages: $pdfUri", e)
        } finally {
            try {
                renderer?.close()
            } catch (_: Exception) {}
            try {
                pfd?.close()
            } catch (_: Exception) {}
            tempFile?.delete()
        }

        bitmaps
    }
}
