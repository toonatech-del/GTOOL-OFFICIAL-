package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LocalIsDarkMode
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AskAiCard(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onPromptSuggestionClick: (String) -> Unit = {},
    onSearchSubmit: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isDarkMode = LocalIsDarkMode.current
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current

    // Infinite rotating color phase transition for dynamic animated border
    val infiniteTransition = rememberInfiniteTransition(label = "search_dock_color_cycle")
    
    val rotationProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "border_rotation_progress"
    )

    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dock_pulse_glow"
    )

    // Animated multi-color chromatic spectrum
    val colorAngle = rotationProgress * 2f * Math.PI.toFloat()
    val startX = 0.5f + 0.5f * cos(colorAngle)
    val startY = 0.5f + 0.5f * sin(colorAngle)
    val endX = 0.5f + 0.5f * cos(colorAngle + Math.PI.toFloat())
    val endY = 0.5f + 0.5f * sin(colorAngle + Math.PI.toFloat())

    val animatedBorderBrush = Brush.linearGradient(
        colors = listOf(
            Color(0xFF00F5D4), // Electric Mint Cyan
            Color(0xFF0070F3), // Vivid Blue
            Color(0xFF7928CA), // Deep Neon Violet
            Color(0xFFFF0080), // Cyber Pink
            Color(0xFFFFB800), // Amber Gold
            Color(0xFF00F5D4)  // Seamless loop back to cyan
        ),
        start = Offset(startX * 800f, startY * 200f),
        end = Offset(endX * 800f, endY * 200f)
    )

    val glowHaloBrush = Brush.linearGradient(
        colors = listOf(
            Color(0xFF00F5D4).copy(alpha = pulseGlow * 0.4f),
            Color(0xFF7928CA).copy(alpha = pulseGlow * 0.35f),
            Color(0xFFFF0080).copy(alpha = pulseGlow * 0.3f),
            Color(0xFFFFB800).copy(alpha = pulseGlow * 0.35f)
        ),
        start = Offset(startX * 600f, startY * 150f),
        end = Offset(endX * 600f, endY * 150f)
    )

    val dockBgColor = if (isDarkMode) Color(0x35121218) else Color(0xCCFFFFFF)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                // Animated glowing ambient halo behind search bar dock
                drawRoundRect(
                    brush = glowHaloBrush,
                    size = size,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(34.dp.toPx(), 34.dp.toPx())
                )
            }
            .clip(RoundedCornerShape(32.dp))
            .background(dockBgColor)
            .border(
                width = 1.8.dp,
                brush = animatedBorderBrush,
                shape = RoundedCornerShape(32.dp)
            )
            .padding(horizontal = 18.dp, vertical = 12.dp)
            .testTag("ask_gsdcall_ai_card")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Search Icon with animated subtle gradient
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0x22FFFFFF))
                    .clickable {
                        if (searchQuery.isNotBlank()) {
                            onSearchSubmit(searchQuery)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = "Search",
                    tint = Color(0xFF00F5D4),
                    modifier = Modifier
                        .size(22.dp)
                        .testTag("search_glass_icon")
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Text Input Column
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp), // Fixed height to help with vertical centering
                contentAlignment = Alignment.CenterStart
            ) {
                if (searchQuery.isEmpty()) {
                    Text(
                        text = "Search notes, PDFs, bills...",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }

                BasicTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    textStyle = MaterialTheme.typography.titleMedium.copy(
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.SansSerif
                    ),
                    cursorBrush = SolidColor(Color(0xFF00F5D4)),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        if (searchQuery.isNotBlank()) {
                            onSearchSubmit(searchQuery)
                            keyboardController?.hide()
                            focusManager.clearFocus()
                        }
                    }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_input_field")
                )
            }

            if (searchQuery.isNotEmpty()) {
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(Color(0x33FFFFFF))
                        .iosBounce { onSearchQueryChange("") }
                        .testTag("search_clear_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Clear search",
                        tint = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
