package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AmberWarm
import kotlin.math.max
import kotlin.math.min

/**
 * Advanced Interactive Cropper with 8 touch handles.
 * Allows panning/zooming image underneath a flexible or locked crop frame.
 */
@Composable
fun InteractiveCropper(
    bitmap: Bitmap,
    aspectRatio: Float?, // Locked ratio (e.g. 3.5/4.5). Null for free crop.
    onCropApply: (Bitmap) -> Unit,
    modifier: Modifier = Modifier
) {
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    var imageScale by remember { mutableFloatStateOf(1f) }
    var imageOffset by remember { mutableStateOf(Offset.Zero) }
    
    // Crop Frame Rect (Relative to container)
    var cropRect by remember { mutableStateOf(RectF(100f, 100f, 500f, 700f)) }
    var initialized by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { containerSize = it }
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    imageScale = (imageScale * zoom).coerceIn(0.1f, 10f)
                    imageOffset += pan
                }
            }
    ) {
        if (containerSize != IntSize.Zero) {
            if (!initialized) {
                // Initial crop frame setup
                val w = containerSize.width.toFloat()
                val h = containerSize.height.toFloat()
                val margin = 80f
                if (aspectRatio != null) {
                    val targetW = w - 2 * margin
                    val targetH = targetW / aspectRatio
                    val finalH = if (targetH > h - 2 * margin) {
                        val newH = h - 2 * margin
                        newH
                    } else targetH
                    val finalW = finalH * aspectRatio
                    cropRect = RectF(
                        (w - finalW) / 2,
                        (h - finalH) / 2,
                        (w + finalW) / 2,
                        (h + finalH) / 2
                    )
                } else {
                    cropRect = RectF(margin, margin, w - margin, h - margin)
                }
                
                // Initial image fit
                val imgW = bitmap.width.toFloat()
                val imgH = bitmap.height.toFloat()
                imageScale = min(w / imgW, h / imgH) * 0.8f
                imageOffset = Offset((w - imgW * imageScale) / 2, (h - imgH * imageScale) / 2)
                
                initialized = true
            }

            // Draw Image & Overlay
            Canvas(modifier = Modifier.fillMaxSize()) {
                // 1. Draw Image
                val imgW = bitmap.width.toFloat() * imageScale
                val imgH = bitmap.height.toFloat() * imageScale
                
                drawImage(
                    image = bitmap.asImageBitmap(),
                    dstOffset = androidx.compose.ui.unit.IntOffset(imageOffset.x.toInt(), imageOffset.y.toInt()),
                    dstSize = IntSize(imgW.toInt(), imgH.toInt())
                )

                // 2. Draw Dimmed background outside cropRect
                drawRect(
                    color = Color.Black.copy(alpha = 0.6f),
                    size = Size(size.width, cropRect.top)
                )
                drawRect(
                    color = Color.Black.copy(alpha = 0.6f),
                    topLeft = Offset(0f, cropRect.bottom),
                    size = Size(size.width, size.height - cropRect.bottom)
                )
                drawRect(
                    color = Color.Black.copy(alpha = 0.6f),
                    topLeft = Offset(0f, cropRect.top),
                    size = Size(cropRect.left, cropRect.height())
                )
                drawRect(
                    color = Color.Black.copy(alpha = 0.6f),
                    topLeft = Offset(cropRect.right, cropRect.top),
                    size = Size(size.width - cropRect.right, cropRect.height())
                )

                // 3. Draw Crop Frame Border
                drawRect(
                    color = Color.White,
                    topLeft = Offset(cropRect.left, cropRect.top),
                    size = Size(cropRect.width(), cropRect.height()),
                    style = Stroke(width = 2.dp.toPx())
                )
                
                // 4. Draw Grid Lines
                val thirdW = cropRect.width() / 3
                val thirdH = cropRect.height() / 3
                for (i in 1..2) {
                    drawLine(Color.White.copy(alpha = 0.3f), Offset(cropRect.left + i * thirdW, cropRect.top), Offset(cropRect.left + i * thirdW, cropRect.bottom))
                    drawLine(Color.White.copy(alpha = 0.3f), Offset(cropRect.left, cropRect.top + i * thirdH), Offset(cropRect.right, cropRect.top + i * thirdH))
                }
            }

            // 5. Interaction Handles
            val handleSize = 30.dp
            val handleColor = AmberWarm
            
            // Corners
            CropHandle(Offset(cropRect.left, cropRect.top), handleSize, handleColor) { delta ->
                if (aspectRatio == null) {
                    cropRect.left = (cropRect.left + delta.x).coerceAtMost(cropRect.right - 100f)
                    cropRect.top = (cropRect.top + delta.y).coerceAtMost(cropRect.bottom - 100f)
                } else {
                    // Locked Aspect Ratio logic
                    val dx = delta.x
                    val dy = dx / aspectRatio
                    cropRect.left += dx
                    cropRect.top += dy
                }
            }
            CropHandle(Offset(cropRect.right, cropRect.top), handleSize, handleColor) { delta ->
                if (aspectRatio == null) {
                    cropRect.right = (cropRect.right + delta.x).coerceAtLeast(cropRect.left + 100f)
                    cropRect.top = (cropRect.top + delta.y).coerceAtMost(cropRect.bottom - 100f)
                } else {
                    val dx = delta.x
                    val dy = -dx / aspectRatio
                    cropRect.right += dx
                    cropRect.top += dy
                }
            }
            CropHandle(Offset(cropRect.left, cropRect.bottom), handleSize, handleColor) { delta ->
                if (aspectRatio == null) {
                    cropRect.left = (cropRect.left + delta.x).coerceAtMost(cropRect.right - 100f)
                    cropRect.bottom = (cropRect.bottom + delta.y).coerceAtLeast(cropRect.top + 100f)
                } else {
                    val dx = delta.x
                    val dy = -dx / aspectRatio
                    cropRect.left += dx
                    cropRect.bottom += dy
                }
            }
            CropHandle(Offset(cropRect.right, cropRect.bottom), handleSize, handleColor) { delta ->
                if (aspectRatio == null) {
                    cropRect.right = (cropRect.right + delta.x).coerceAtLeast(cropRect.left + 100f)
                    cropRect.bottom = (cropRect.bottom + delta.y).coerceAtLeast(cropRect.top + 100f)
                } else {
                    val dx = delta.x
                    val dy = dx / aspectRatio
                    cropRect.right += dx
                    cropRect.bottom += dy
                }
            }

            // Edges (only for free crop)
            if (aspectRatio == null) {
                CropHandle(Offset(cropRect.centerX(), cropRect.top), handleSize, handleColor) { delta ->
                    cropRect.top = (cropRect.top + delta.y).coerceAtMost(cropRect.bottom - 100f)
                }
                CropHandle(Offset(cropRect.centerX(), cropRect.bottom), handleSize, handleColor) { delta ->
                    cropRect.bottom = (cropRect.bottom + delta.y).coerceAtLeast(cropRect.top + 100f)
                }
                CropHandle(Offset(cropRect.left, cropRect.centerY()), handleSize, handleColor) { delta ->
                    cropRect.left = (cropRect.left + delta.x).coerceAtMost(cropRect.right - 100f)
                }
                CropHandle(Offset(cropRect.right, cropRect.centerY()), handleSize, handleColor) { delta ->
                    cropRect.right = (cropRect.right + delta.x).coerceAtLeast(cropRect.left + 100f)
                }
            }
        }
        
        // Apply Button (Floating)
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
        ) {
            androidx.compose.material3.FloatingActionButton(
                onClick = {
                    val cropped = performCrop(bitmap, cropRect, imageOffset, imageScale)
                    onCropApply(cropped)
                },
                containerColor = AmberWarm,
                contentColor = Color.Black,
                shape = CircleShape
            ) {
                androidx.compose.material3.Icon(
                    imageVector = androidx.compose.material.icons.Icons.Rounded.Check,
                    contentDescription = "Apply Crop"
                )
            }
        }
    }
}

@Composable
private fun CropHandle(
    position: Offset,
    size: androidx.compose.ui.unit.Dp,
    color: Color,
    onDrag: (Offset) -> Unit
) {
    Box(
        modifier = Modifier
            .size(size)
            .offset(
                x = position.x.dp / androidx.compose.ui.platform.LocalDensity.current.density - size / 2,
                y = position.y.dp / androidx.compose.ui.platform.LocalDensity.current.density - size / 2
            )
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onDrag(dragAmount)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
    }
}

private fun performCrop(
    original: Bitmap,
    cropRect: RectF,
    imageOffset: Offset,
    imageScale: Float
): Bitmap {
    // Convert screen cropRect to bitmap coordinates
    val left = (cropRect.left - imageOffset.x) / imageScale
    val top = (cropRect.top - imageOffset.y) / imageScale
    val right = (cropRect.right - imageOffset.x) / imageScale
    val bottom = (cropRect.bottom - imageOffset.y) / imageScale

    val srcRect = Rect(
        max(0, left.toInt()),
        max(0, top.toInt()),
        min(original.width, right.toInt()),
        min(original.height, bottom.toInt())
    )
    
    if (srcRect.width() <= 0 || srcRect.height() <= 0) return original

    return Bitmap.createBitmap(original, srcRect.left, srcRect.top, srcRect.width(), srcRect.height())
}
