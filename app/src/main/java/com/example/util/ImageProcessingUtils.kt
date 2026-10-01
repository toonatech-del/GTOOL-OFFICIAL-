package com.example.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.example.ui.screens.DimensionUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

import android.util.Base64

enum class OutputFormat(val extension: String, val mimeType: String, val label: String) {
    JPEG("jpg", "image/jpeg", "JPEG"),
    PNG("png", "image/png", "PNG"),
    WEBP("webp", "image/webp", "WEBP"),
    SVG("svg", "image/svg+xml", "SVG")
}

data class ResizeResult(
    val uri: Uri,
    val outputWidth: Int,
    val outputHeight: Int,
    val fileSizeBytes: Long,
    val fileSizeKb: Long,
    val format: OutputFormat,
    val compressionRatioText: String
)

object ImageProcessingUtils {

    suspend fun decodeBitmapFromUri(context: Context, uri: Uri): Pair<Bitmap?, Long> = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val bytes = inputStream?.readBytes() ?: return@withContext Pair(null, 0L)
            inputStream.close()

            // Decode dimensions first for memory safety if very large
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)

            // Calculate sample size if over 4096px
            var sampleSize = 1
            while (options.outWidth / sampleSize > 4096 || options.outHeight / sampleSize > 4096) {
                sampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOptions)
            Pair(bmp, bytes.size.toLong())
        } catch (e: Exception) {
            e.printStackTrace()
            Pair(null, 0L)
        }
    }

    suspend fun calculateCompressedSize(
        bitmap: Bitmap,
        format: OutputFormat,
        quality: Int
    ): Long = withContext(Dispatchers.IO) {
        if (format == OutputFormat.SVG) return@withContext 0L // SVG size calculation is complex
        val stream = ByteArrayOutputStream()
        val compressFormat = when (format) {
            OutputFormat.JPEG -> Bitmap.CompressFormat.JPEG
            OutputFormat.PNG -> Bitmap.CompressFormat.PNG
            else -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    Bitmap.CompressFormat.WEBP_LOSSY
                } else {
                    @Suppress("DEPRECATION")
                    Bitmap.CompressFormat.WEBP
                }
            }
        }
        bitmap.compress(compressFormat, quality.coerceIn(1, 100), stream)
        stream.toByteArray().size.toLong()
    }

    suspend fun findQualityForTargetSize(
        bitmap: Bitmap,
        format: OutputFormat,
        targetKb: Int
    ): Int = withContext(Dispatchers.IO) {
        if (format == OutputFormat.PNG || format == OutputFormat.SVG) return@withContext 100
        val targetBytes = targetKb * 1024L
        val compressFormat = when (format) {
            OutputFormat.JPEG -> Bitmap.CompressFormat.JPEG
            else -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    Bitmap.CompressFormat.WEBP_LOSSY
                } else {
                    @Suppress("DEPRECATION")
                    Bitmap.CompressFormat.WEBP
                }
            }
        }

        var low = 1
        var high = 100
        var bestQuality = 85
        val stream = ByteArrayOutputStream()

        while (low <= high) {
            val mid = (low + high) / 2
            stream.reset()
            bitmap.compress(compressFormat, mid, stream)
            val currentSize = stream.size()
            if (currentSize <= targetBytes) {
                bestQuality = mid
                low = mid + 1
            } else {
                high = mid - 1
            }
        }
        bestQuality
    }

    suspend fun resizeAndCompress(
        context: Context,
        sourceBitmap: Bitmap,
        unit: DimensionUnit,
        targetWidthVal: Int,
        targetHeightVal: Int,
        targetKb: Int,
        quality: Int,
        format: OutputFormat,
        dpi: Int = 200,
        candidateName: String? = null,
        candidateDate: String? = null
    ): ResizeResult? = withContext(Dispatchers.IO) {
        try {
            // Convert dimensions based on unit
            val pixelWidth = when (unit) {
                DimensionUnit.PX -> targetWidthVal.coerceIn(10, 8192)
                DimensionUnit.CM -> (targetWidthVal * (dpi / 2.54f)).toInt().coerceIn(10, 8192)
                DimensionUnit.MM -> (targetWidthVal * (dpi / 25.4f)).toInt().coerceIn(10, 8192)
                DimensionUnit.INCH -> (targetWidthVal * dpi).toInt().coerceIn(10, 8192)
            }
            val pixelHeight = when (unit) {
                DimensionUnit.PX -> targetHeightVal.coerceIn(10, 8192)
                DimensionUnit.CM -> (targetHeightVal * (dpi / 2.54f)).toInt().coerceIn(10, 8192)
                DimensionUnit.MM -> (targetHeightVal * (dpi / 25.4f)).toInt().coerceIn(10, 8192)
                DimensionUnit.INCH -> (targetHeightVal * dpi).toInt().coerceIn(10, 8192)
            }

            val finalWidth = pixelWidth
            val stripHeight = if (!candidateName.isNullOrBlank() || !candidateDate.isNullOrBlank()) {
                (pixelHeight * 0.12f).toInt()
            } else 0
            val finalHeight = pixelHeight + stripHeight

            val scaledBmp = Bitmap.createScaledBitmap(sourceBitmap, pixelWidth, pixelHeight, true)
            val outputBitmap = if (stripHeight > 0) {
                val combined = Bitmap.createBitmap(finalWidth, finalHeight, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(combined)
                canvas.drawBitmap(scaledBmp, 0f, 0f, null)
                
                // Draw white strip
                val stripPaint = Paint().apply {
                    color = android.graphics.Color.WHITE
                    style = Paint.Style.FILL
                }
                canvas.drawRect(0f, pixelHeight.toFloat(), finalWidth.toFloat(), finalHeight.toFloat(), stripPaint)
                
                // Draw Name (upper half of strip)
                if (!candidateName.isNullOrBlank()) {
                    val namePaint = Paint().apply {
                        color = android.graphics.Color.BLACK
                        textSize = (stripHeight * 0.35f)
                        textAlign = Paint.Align.CENTER
                        isAntiAlias = true
                        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                    }
                    canvas.drawText(candidateName.uppercase(), (finalWidth / 2).toFloat(), (pixelHeight + stripHeight * 0.45f).toFloat(), namePaint)
                }
                
                // Draw Date (lower half of strip)
                if (!candidateDate.isNullOrBlank()) {
                    val datePaint = Paint().apply {
                        color = android.graphics.Color.BLACK
                        textSize = (stripHeight * 0.35f)
                        textAlign = Paint.Align.CENTER
                        isAntiAlias = true
                        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                    }
                    canvas.drawText(candidateDate, (finalWidth / 2).toFloat(), (pixelHeight + stripHeight * 0.85f).toFloat(), datePaint)
                }
                combined
            } else {
                scaledBmp
            }

            val outputBytes = if (format == OutputFormat.SVG) {
                val jpegStream = ByteArrayOutputStream()
                outputBitmap.compress(Bitmap.CompressFormat.JPEG, 85, jpegStream)
                val base64String = Base64.encodeToString(jpegStream.toByteArray(), Base64.NO_WRAP)
                val svgString = """
                    <svg xmlns="http://www.w3.org/2000/svg" width="$finalWidth" height="$finalHeight">
                      <image href="data:image/jpeg;base64,$base64String" width="$finalWidth" height="$finalHeight"/>
                    </svg>
                """.trimIndent()
                svgString.toByteArray(Charsets.UTF_8)
            } else {
                val compressFormat = when (format) {
                    OutputFormat.JPEG -> Bitmap.CompressFormat.JPEG
                    OutputFormat.PNG -> Bitmap.CompressFormat.PNG
                    else -> {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                            Bitmap.CompressFormat.WEBP_LOSSY
                        } else {
                            @Suppress("DEPRECATION")
                            Bitmap.CompressFormat.WEBP
                        }
                    }
                }

                val stream = ByteArrayOutputStream()
                if (format == OutputFormat.PNG) {
                    outputBitmap.compress(compressFormat, 100, stream)
                } else {
                    var low = 5
                    var high = 100
                    var bestQuality = quality.coerceIn(5, 100)
                    while (low <= high) {
                        val mid = (low + high) / 2
                        stream.reset()
                        outputBitmap.compress(compressFormat, mid, stream)
                        val sizeKb = stream.toByteArray().size / 1024
                        if (sizeKb <= targetKb) {
                            bestQuality = mid
                            low = mid + 1
                        } else {
                            high = mid - 1
                        }
                    }
                    stream.reset()
                    outputBitmap.compress(compressFormat, bestQuality, stream)
                }
                stream.toByteArray()
            }

            val fileName = "resized_${System.currentTimeMillis()}.${format.extension}"
            val outputFile = File(context.cacheDir, fileName)

            FileOutputStream(outputFile).use { fos ->
                fos.write(outputBytes)
                fos.flush()
            }

            val fileUri = try {
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    outputFile
                )
            } catch (_: Exception) {
                Uri.fromFile(outputFile)
            }

            val sizeKb = outputBytes.size / 1024L
            val compressionRatio = if (sourceBitmap.byteCount > 0) {
                val origKb = (sourceBitmap.width * sourceBitmap.height * 4) / 1024L
                val savedPercent = ((origKb - sizeKb).toFloat() / origKb.coerceAtLeast(1) * 100).toInt().coerceIn(0, 99)
                "$savedPercent% reduced"
            } else {
                "Optimized"
            }

            ResizeResult(
                uri = fileUri,
                outputWidth = finalWidth,
                outputHeight = finalHeight,
                fileSizeBytes = outputBytes.size.toLong(),
                fileSizeKb = sizeKb,
                format = format,
                compressionRatioText = compressionRatio
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun shareImage(context: Context, uri: Uri, title: String = "Share Resized Photo") {
        try {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/*"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(shareIntent, title).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun bitmapToTempUri(context: Context, bitmap: Bitmap): Uri = withContext(Dispatchers.IO) {
        val fileName = "temp_${System.currentTimeMillis()}.jpg"
        val file = File(context.cacheDir, fileName)
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
        }
        try {
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) {
            Uri.fromFile(file)
        }
    }

    suspend fun saveImageToPublicGToolXFolder(
        context: Context,
        sourceUri: Uri,
        fileNamePrefix: String = "GTOOL_X",
        format: OutputFormat = OutputFormat.JPEG
    ): Uri? = withContext(Dispatchers.IO) {
        try {
            val timeStamp = System.currentTimeMillis()
            val fileName = "${fileNamePrefix}_$timeStamp.${format.extension}"
            val mimeType = format.mimeType

            val inputStream = context.contentResolver.openInputStream(sourceUri) ?: return@withContext null
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/GTOOL X")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
                val resolver = context.contentResolver
                val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                    ?: return@withContext null

                resolver.openOutputStream(imageUri)?.use { out ->
                    inputStream.copyTo(out)
                }
                inputStream.close()

                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(imageUri, contentValues, null, null)

                // Trigger MediaScanner for instant Gallery update
                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(imageUri.toString()),
                    arrayOf(mimeType),
                    null
                )

                imageUri
            } else {
                val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                val gtoolFolder = File(picturesDir, "GTOOL X")
                if (!gtoolFolder.exists()) {
                    gtoolFolder.mkdirs()
                }
                val imageFile = File(gtoolFolder, fileName)
                FileOutputStream(imageFile).use { out ->
                    inputStream.copyTo(out)
                }
                inputStream.close()

                // Trigger MediaScanner for instant Gallery & File Manager update
                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(imageFile.absolutePath),
                    arrayOf(mimeType),
                    null
                )

                Uri.fromFile(imageFile)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
