package com.example.util

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

class PdfPrintDocumentAdapter(
    private val context: Context,
    private val pdfUri: Uri
) : PrintDocumentAdapter() {

    override fun onLayout(
        oldAttributes: PrintAttributes?,
        newAttributes: PrintAttributes,
        cancellationSignal: CancellationSignal,
        callback: LayoutResultCallback,
        extras: Bundle?
    ) {
        if (cancellationSignal.isCanceled) {
            callback.onLayoutCancelled()
            return
        }

        val info = PrintDocumentInfo.Builder("Invoice.pdf")
            .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
            .setPageCount(PrintDocumentInfo.PAGE_COUNT_UNKNOWN)
            .build()

        callback.onLayoutFinished(info, true)
    }

    override fun onWrite(
        pages: Array<out PageRange>?,
        destination: ParcelFileDescriptor,
        cancellationSignal: CancellationSignal,
        callback: WriteResultCallback
    ) {
        var input: FileInputStream? = null
        var output: FileOutputStream? = null

        try {
            val pfd = context.contentResolver.openFileDescriptor(pdfUri, "r") ?: throw IOException("Failed to open Uri")
            input = FileInputStream(pfd.fileDescriptor)
            output = FileOutputStream(destination.fileDescriptor)

            val buf = ByteArray(1024)
            var bytesRead: Int
            while (input.read(buf).also { bytesRead = it } > 0) {
                output.write(buf, 0, bytesRead)
            }

            callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
        } catch (e: Exception) {
            callback.onWriteFailed(e.toString())
        } finally {
            try {
                input?.close()
                output?.close()
            } catch (_: IOException) {}
        }
    }
}
