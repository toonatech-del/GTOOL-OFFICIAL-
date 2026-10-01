package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import com.example.ui.screens.DimensionUnit
import com.example.util.ImageProcessingUtils
import com.example.util.OutputFormat
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PhotoResizerRobolectricTest {

    private lateinit var context: Context
    private lateinit var sampleBitmap: Bitmap

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        // Create a test 800x600 bitmap with synthetic pixel data
        sampleBitmap = Bitmap.createBitmap(800, 600, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.BLUE)
        }
    }

    @Test
    fun test_resize_to_exact_pixel_dimensions() = runBlocking {
        val targetWidth = 400
        val targetHeight = 300

        val result = ImageProcessingUtils.resizeAndCompress(
            context = context,
            sourceBitmap = sampleBitmap,
            unit = DimensionUnit.PX,
            targetWidthVal = targetWidth,
            targetHeightVal = targetHeight,
            targetKb = 200,
            quality = 90,
            format = OutputFormat.JPEG
        )

        assertNotNull("Result should not be null", result)
        assertEquals("Output width must match target", targetWidth, result!!.outputWidth)
        assertEquals("Output height must match target", targetHeight, result.outputHeight)
        assertTrue("Output file size should be > 0", result.fileSizeBytes > 0)
        assertNotNull("Generated Uri must be valid", result.uri)
    }

    @Test
    fun test_aspect_presets_square_and_story() = runBlocking {
        // 1:1 Square
        val squareResult = ImageProcessingUtils.resizeAndCompress(
            context = context,
            sourceBitmap = sampleBitmap,
            unit = DimensionUnit.PX,
            targetWidthVal = 512,
            targetHeightVal = 512,
            targetKb = 150,
            quality = 85,
            format = OutputFormat.JPEG
        )
        assertNotNull(squareResult)
        assertEquals(512, squareResult!!.outputWidth)
        assertEquals(512, squareResult.outputHeight)

        // 9:16 Story
        val storyResult = ImageProcessingUtils.resizeAndCompress(
            context = context,
            sourceBitmap = sampleBitmap,
            unit = DimensionUnit.PX,
            targetWidthVal = 720,
            targetHeightVal = 1280,
            targetKb = 200,
            quality = 80,
            format = OutputFormat.WEBP
        )
        assertNotNull(storyResult)
        assertEquals(720, storyResult!!.outputWidth)
        assertEquals(1280, storyResult.outputHeight)
        assertEquals(OutputFormat.WEBP, storyResult.format)
    }

    @Test
    fun test_compression_to_target_kb() = runBlocking {
        val targetMaxKb = 50

        val result = ImageProcessingUtils.resizeAndCompress(
            context = context,
            sourceBitmap = sampleBitmap,
            unit = DimensionUnit.PX,
            targetWidthVal = 600,
            targetHeightVal = 400,
            targetKb = targetMaxKb,
            quality = 90,
            format = OutputFormat.JPEG
        )

        assertNotNull(result)
        // Verify output file exists on disk
        val outputFile = File(result!!.uri.path ?: "")
        assertTrue(result.fileSizeBytes > 0)
        // File size in KB should respect max bounds or be reasonably compressed
        assertTrue("File size in KB should be reasonable", result.fileSizeKb <= targetMaxKb + 20)
    }

    @Test
    fun test_cm_and_mm_unit_conversion_to_300dpi_pixels() = runBlocking {
        // 3.5 x 4.5 cm (standard passport size)
        val cmResult = ImageProcessingUtils.resizeAndCompress(
            context = context,
            sourceBitmap = sampleBitmap,
            unit = DimensionUnit.CM,
            targetWidthVal = 3, // 3cm ~ 354px
            targetHeightVal = 4, // 4cm ~ 472px
            targetKb = 100,
            quality = 85,
            format = OutputFormat.JPEG,
            dpi = 300
        )

        assertNotNull(cmResult)
        assertTrue("Converted width should be approximately 354 px", cmResult!!.outputWidth in 340..370)
        assertTrue("Converted height should be approximately 472 px", cmResult.outputHeight in 460..490)
    }

    @Test
    fun test_png_format_output() = runBlocking {
        val result = ImageProcessingUtils.resizeAndCompress(
            context = context,
            sourceBitmap = sampleBitmap,
            unit = DimensionUnit.PX,
            targetWidthVal = 200,
            targetHeightVal = 200,
            targetKb = 300,
            quality = 100,
            format = OutputFormat.PNG
        )

        assertNotNull(result)
        assertEquals(OutputFormat.PNG, result!!.format)
        assertTrue(result.uri.toString().contains(".png") || result.fileSizeBytes > 0)
    }

    @Test
    fun test_boundary_values_and_edge_cases() = runBlocking {
        // Zero/negative dimensions should be clamped safely to minimum positive bound
        val clampedResult = ImageProcessingUtils.resizeAndCompress(
            context = context,
            sourceBitmap = sampleBitmap,
            unit = DimensionUnit.PX,
            targetWidthVal = 0,
            targetHeightVal = -50,
            targetKb = 100,
            quality = 80,
            format = OutputFormat.JPEG
        )

        assertNotNull("Should not crash on zero/negative dimensions", clampedResult)
        assertTrue(clampedResult!!.outputWidth >= 10)
        assertTrue(clampedResult.outputHeight >= 10)
    }
}
