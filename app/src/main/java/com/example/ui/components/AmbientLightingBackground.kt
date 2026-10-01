package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.ui.theme.AmberGlow
import com.example.ui.theme.AmberWarm
import com.example.ui.theme.LocalIsDarkMode
import com.example.ui.theme.OliveBgBottom
import com.example.ui.theme.OliveBgTop

/**
 * Responsive Ambient Lighting Background supporting Dark Mode & Light Mode.
 *
 * Dark Mode: Earthy Warm Olive-Charcoal gradient (#2D2A22 to #1D1C16) with subtle amber glow.
 * Light Mode: Glowing pastel wallpaper gradient matching the uploaded wallpaper
 * (vivid sky blue top, pastel pink/yellow center orb, soft violet/lavender bottom).
 */
@Composable
fun AmbientLightingBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val isDarkMode = LocalIsDarkMode.current

    val topBgColor by animateColorAsState(
        targetValue = if (isDarkMode) OliveBgTop else Color(0xFF3B82F6), // Sky Blue Top
        animationSpec = tween(400),
        label = "bg_top"
    )

    val midBgColor by animateColorAsState(
        targetValue = if (isDarkMode) Color(0xFF25231C) else Color(0xFF93C5FD), // Soft Blue
        animationSpec = tween(400),
        label = "bg_mid"
    )

    val bottomBgColor by animateColorAsState(
        targetValue = if (isDarkMode) OliveBgBottom else Color(0xFF818CF8), // Lavender Bottom
        animationSpec = tween(400),
        label = "bg_bottom"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bottomBgColor)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            if (isDarkMode) {
                // 1. Warm olive-charcoal linear gradient base (#2D2A22 0%, #1D1C16 100%)
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            topBgColor,
                            midBgColor,
                            bottomBgColor
                        ),
                        startY = 0f,
                        endY = height
                    )
                )

                // 2. Soft subtle warm ambient center radial glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0x20303035),
                            Color(0x10151518),
                            Color.Transparent
                        ),
                        center = Offset(width * 0.5f, height * 0.45f),
                        radius = width * 0.95f
                    ),
                    center = Offset(width * 0.5f, height * 0.45f),
                    radius = width * 0.95f
                )

                // 3. Top Accent Golden Warm Glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            AmberWarm.copy(alpha = 0.14f),
                            AmberGlow.copy(alpha = 0.06f),
                            Color.Transparent
                        ),
                        center = Offset(width * 0.8f, height * 0.15f),
                        radius = width * 0.75f
                    ),
                    center = Offset(width * 0.8f, height * 0.15f),
                    radius = width * 0.75f
                )
            } else {
                // LIGHT MODE: Dynamic pastel glowing background matching the Galaxy wallpaper
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF2563EB), // Rich Vivid Blue top
                            Color(0xFF3B82F6), // Bright Sky Blue
                            Color(0xFF818CF8), // Pastel Periwinkle
                            Color(0xFFA855F7)  // Lavender Violet bottom
                        ),
                        startY = 0f,
                        endY = height
                    )
                )

                // Upper Pastel Pink/Yellow Halo Orb
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFFD1DC).copy(alpha = 0.85f), // Soft Pastel Pink
                            Color(0xFFFDE047).copy(alpha = 0.65f), // Warm Yellow Glow
                            Color(0xFFF472B6).copy(alpha = 0.35f),
                            Color.Transparent
                        ),
                        center = Offset(width * 0.5f, height * 0.42f),
                        radius = width * 0.70f
                    ),
                    center = Offset(width * 0.5f, height * 0.42f),
                    radius = width * 0.70f
                )

                // Center Glowing Coral-Pink Sphere
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFB7185).copy(alpha = 0.75f), // Coral Pink
                            Color(0xFFF472B6).copy(alpha = 0.55f),
                            Color(0xFFC084FC).copy(alpha = 0.30f),
                            Color.Transparent
                        ),
                        center = Offset(width * 0.5f, height * 0.58f),
                        radius = width * 0.75f
                    ),
                    center = Offset(width * 0.5f, height * 0.58f),
                    radius = width * 0.75f
                )

                // Bottom Lavender Light Ring
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFE879F9).copy(alpha = 0.60f), // Soft Magenta-Pink
                            Color(0xFFC084FC).copy(alpha = 0.40f),
                            Color.Transparent
                        ),
                        center = Offset(width * 0.5f, height * 0.78f),
                        radius = width * 0.85f
                    ),
                    center = Offset(width * 0.5f, height * 0.78f),
                    radius = width * 0.85f
                )
            }
        }

        content()
    }
}
