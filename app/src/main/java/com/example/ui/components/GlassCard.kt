package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalIsDarkMode

/**
 * High-fidelity Frosted Glass Card Composable with delicate 1px translucent border stroke,
 * warm dark translucent layering, and built-in tactile zoom-on-press micro-interaction.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    borderStrokeWidth: Dp = 1.dp,
    borderColorList: List<Color> = listOf(
        Color(0x1FFFFFFF), // rgba(255, 255, 255, 0.12)
        Color(0x14FFFFFF),
        Color(0x1CFFFFFF)
    ),
    customBorderBrush: Brush? = null,
    backgroundGradient: List<Color> = listOf(
        Color(0xA62D2A22), // rgba(45, 42, 34, 0.65)
        Color(0x8025231C),
        Color(0x601D1C16)
    ),
    ambientGlowColor: Color? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val isDarkMode = LocalIsDarkMode.current

    val defaultBgGradient = if (isDarkMode) {
        listOf(
            Color(0xA62D2A22), // rgba(45, 42, 34, 0.65)
            Color(0x8025231C),
            Color(0x601D1C16)
        )
    } else {
        listOf(
            Color(0xCDFFFFFF), // rgba(255, 255, 255, 0.80) frosted white
            Color(0xB3FFFFFF),
            Color(0x99FFFFFF)
        )
    }

    val defaultBorderColors = if (isDarkMode) {
        listOf(
            Color(0x1FFFFFFF), // rgba(255, 255, 255, 0.12)
            Color(0x14FFFFFF),
            Color(0x1CFFFFFF)
        )
    } else {
        listOf(
            Color(0xFFFFFFFF), // rgba(255, 255, 255, 1.0)
            Color(0xD0FFFFFF),
            Color(0xAAFFFFFF)
        )
    }

    val actualBg = if (backgroundGradient == listOf(Color(0xA62D2A22), Color(0x8025231C), Color(0x601D1C16))) defaultBgGradient else backgroundGradient
    val actualBorderList = if (borderColorList == listOf(Color(0x1FFFFFFF), Color(0x14FFFFFF), Color(0x1CFFFFFF))) defaultBorderColors else borderColorList

    val borderBrush = customBorderBrush ?: Brush.linearGradient(actualBorderList)

    val baseModifier = modifier
        .clip(shape)
        .then(
            if (!isDarkMode) {
                // Soft blue-indigo box shadow ambient glow for Light Mode white glass
                Modifier.drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0x221F2687), Color.Transparent),
                            radius = size.maxDimension * 0.80f
                        )
                    )
                }
            } else if (ambientGlowColor != null) {
                Modifier.drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(ambientGlowColor.copy(alpha = 0.15f), Color.Transparent),
                            radius = size.maxDimension * 0.75f
                        )
                    )
                }
            } else Modifier
        )
        .background(
            brush = Brush.linearGradient(actualBg),
            shape = shape
        )
        .border(
            border = BorderStroke(borderStrokeWidth, borderBrush),
            shape = shape
        )

    val finalModifier = if (onClick != null) {
        baseModifier.zoomOnPress(
            pressedScale = 0.96f,
            onClick = onClick
        )
    } else {
        baseModifier
    }

    Box(
        modifier = finalModifier,
        content = content
    )
}

/**
 * Capsule button helper with 1px border stroke, translucent glass styling, and tactile zoom micro-interaction.
 */
@Composable
fun GlassCapsule(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(50)
    val backgroundBrush = if (selected) {
        Brush.horizontalGradient(
            listOf(
                Color(0x3500E5FF),
                Color(0x1A00E5FF)
            )
        )
    } else {
        Brush.linearGradient(
            listOf(
                Color(0x20FFFFFF),
                Color(0x0CFFFFFF)
            )
        )
    }

    val borderStroke = if (selected) {
        BorderStroke(
            1.5.dp,
            Brush.horizontalGradient(
                listOf(
                    Color(0xFF00E5FF),
                    Color(0xFF38BDF8),
                    Color(0xFF00E5FF)
                )
            )
        )
    } else {
        BorderStroke(
            1.dp,
            Brush.linearGradient(
                listOf(
                    Color(0x3AFFFFFF),
                    Color(0x12FFFFFF)
                )
            )
        )
    }

    Box(
        modifier = modifier
            .zoomOnPress(
                pressedScale = 0.95f,
                onClick = onClick
            )
            .clip(shape)
            .background(backgroundBrush, shape)
            .border(borderStroke, shape)
            .padding(horizontal = 18.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center,
        content = content
    )
}
