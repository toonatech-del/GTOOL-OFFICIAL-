package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Collections
import androidx.compose.material.icons.rounded.PermMedia
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Modern high-definition Gallery Icon vector & composable for GTOOL X.
 * Features layered frosted glass photo frames, glowing aperture sun,
 * sleek mountain horizon contours, and vibrant iOS-style color depth.
 */
@Composable
fun ModernGalleryIcon(
    modifier: Modifier = Modifier,
    size: Dp = 28.dp,
    tint: Color? = null,
    isAnimated: Boolean = false
) {
    val infiniteTransition = rememberInfiniteTransition(label = "gallery_icon_pulse")
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_pulse"
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val w = this.size.width
            val h = this.size.height
            val scale = w / 24f

            // 1. Back overlapping tilted/offset frame for depth & layer
            val backFrameColor = tint?.copy(alpha = 0.35f) ?: Color(0x6600E5FF)
            val backFrameBorder = tint?.copy(alpha = 0.5f) ?: Color(0x9900E5FF)
            
            drawRoundRect(
                color = backFrameColor,
                topLeft = Offset(4.5f * scale, 1.5f * scale),
                size = Size(17f * scale, 16f * scale),
                cornerRadius = CornerRadius(4.5f * scale, 4.5f * scale),
                style = Fill
            )
            drawRoundRect(
                color = backFrameBorder,
                topLeft = Offset(4.5f * scale, 1.5f * scale),
                size = Size(17f * scale, 16f * scale),
                cornerRadius = CornerRadius(4.5f * scale, 4.5f * scale),
                style = Stroke(width = 1.2f * scale)
            )

            // 2. Front primary glass frame
            val frontFrameGradient = if (tint != null) {
                Brush.linearGradient(
                    listOf(
                        tint.copy(alpha = 0.95f),
                        tint.copy(alpha = 0.75f)
                    )
                )
            } else {
                Brush.linearGradient(
                    listOf(
                        Color(0xFF00E5FF),
                        Color(0xFF7000FF),
                        Color(0xFFFF007A)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(w, h)
                )
            }

            // Frame Background (translucent dark glass with colorful gradient tint)
            drawRoundRect(
                color = Color(0xD0121218),
                topLeft = Offset(2f * scale, 5.5f * scale),
                size = Size(17.5f * scale, 16.5f * scale),
                cornerRadius = CornerRadius(4.5f * scale, 4.5f * scale),
                style = Fill
            )

            // Dynamic Front Border
            drawRoundRect(
                brush = frontFrameGradient,
                topLeft = Offset(2f * scale, 5.5f * scale),
                size = Size(17.5f * scale, 16.5f * scale),
                cornerRadius = CornerRadius(4.5f * scale, 4.5f * scale),
                style = Stroke(width = 1.6f * scale)
            )

            // 3. Glowing Sun / Aperture in top right of inner frame
            val sunRadius = 2.4f * scale * (if (isAnimated) glowPulse else 1f)
            val sunCenter = Offset(14.5f * scale, 10f * scale)
            val sunBrush = if (tint != null) {
                Brush.radialGradient(
                    colors = listOf(tint, tint.copy(alpha = 0.4f)),
                    center = sunCenter,
                    radius = sunRadius * 1.5f
                )
            } else {
                Brush.radialGradient(
                    colors = listOf(Color(0xFFFFD600), Color(0xFFFF6D00), Color(0x00FF6D00)),
                    center = sunCenter,
                    radius = sunRadius * 1.8f
                )
            }

            drawCircle(
                brush = sunBrush,
                radius = sunRadius,
                center = sunCenter
            )

            // 4. Stylized Mountain / Wave Peaks with smooth gradient curve inside clipping bounds
            val mountainPath = Path().apply {
                moveTo(2.5f * scale, 19.5f * scale)
                lineTo(7.5f * scale, 13.5f * scale)
                cubicTo(
                    8.8f * scale, 12.0f * scale,
                    10.2f * scale, 12.0f * scale,
                    11.5f * scale, 13.8f * scale
                )
                lineTo(13.2f * scale, 16.0f * scale)
                cubicTo(
                    14.0f * scale, 15.0f * scale,
                    15.2f * scale, 15.0f * scale,
                    16.2f * scale, 16.4f * scale
                )
                lineTo(19f * scale, 20f * scale)
                lineTo(19f * scale, 21f * scale)
                lineTo(2.5f * scale, 21f * scale)
                close()
            }

            val mountainBrush = if (tint != null) {
                Brush.verticalGradient(
                    colors = listOf(
                        tint.copy(alpha = 0.85f),
                        tint.copy(alpha = 0.45f)
                    ),
                    startY = 12f * scale,
                    endY = 22f * scale
                )
            } else {
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF00E5FF),
                        Color(0xFF38BDF8),
                        Color(0xFF6366F1)
                    ),
                    start = Offset(2f * scale, 12f * scale),
                    end = Offset(19f * scale, 22f * scale)
                )
            }

            drawPath(path = mountainPath, brush = mountainBrush)

            // 5. Subtle Glass Gloss Reflection Shimmer
            val glassShinePath = Path().apply {
                moveTo(3.5f * scale, 6.5f * scale)
                lineTo(10.5f * scale, 6.5f * scale)
                lineTo(4.5f * scale, 15.5f * scale)
                lineTo(3.5f * scale, 14f * scale)
                close()
            }
            drawPath(
                path = glassShinePath,
                color = Color(0x35FFFFFF)
            )
        }
    }
}

/**
 * Standard ImageVector alias for places that require an ImageVector.
 */
val ModernGalleryIconVector: ImageVector
    get() = Icons.Rounded.Collections
