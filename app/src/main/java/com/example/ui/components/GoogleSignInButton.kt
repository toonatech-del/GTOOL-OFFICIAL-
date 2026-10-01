package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AmberWarm
import com.example.ui.theme.GlassStroke
import com.example.ui.theme.LocalIsDarkMode

@Composable
fun GoogleLogoIcon(modifier: Modifier = Modifier.size(22.dp)) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val center = Offset(w / 2f, h / 2f)
        val radius = w * 0.45f
        val strokeWidth = w * 0.18f

        // Google 4-Color Palette
        val blue = Color(0xFF4285F4)
        val green = Color(0xFF34A853)
        val yellow = Color(0xFFFBBC05)
        val red = Color(0xFFEA4335)

        val arcRect = Rect(
            left = center.x - radius,
            top = center.y - radius,
            right = center.x + radius,
            bottom = center.y + radius
        )

        // Draw arcs for Red, Yellow, Green, Blue
        // Red Top Arc (180 to 300 deg)
        val redPath = Path().apply {
            arcTo(arcRect, 180f, 135f, false)
        }
        drawPath(redPath, red, style = Stroke(width = strokeWidth))

        // Blue Arc & Horizontal Bar (300 to 45 deg)
        val bluePath = Path().apply {
            arcTo(arcRect, 315f, 90f, false)
        }
        drawPath(bluePath, blue, style = Stroke(width = strokeWidth))

        // Green Bottom Arc (45 to 135 deg)
        val greenPath = Path().apply {
            arcTo(arcRect, 45f, 90f, false)
        }
        drawPath(greenPath, green, style = Stroke(width = strokeWidth))

        // Yellow Left Arc (135 to 225 deg)
        val yellowPath = Path().apply {
            arcTo(arcRect, 135f, 90f, false)
        }
        drawPath(yellowPath, yellow, style = Stroke(width = strokeWidth))

        // Blue Crossbar
        val barHeight = strokeWidth * 0.95f
        drawRect(
            color = blue,
            topLeft = Offset(center.x - strokeWidth * 0.1f, center.y - barHeight / 2f),
            size = Size(radius + strokeWidth * 0.1f, barHeight),
            style = Fill
        )
    }
}

@Composable
fun GoogleSignInButton(
    text: String = "Continue with Google",
    isLoading: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDarkMode = LocalIsDarkMode.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .shadow(elevation = 6.dp, shape = RoundedCornerShape(26.dp), spotColor = Color(0x33000000))
            .clip(RoundedCornerShape(26.dp))
            .background(
                if (isDarkMode) {
                    Brush.linearGradient(
                        listOf(Color(0xFFFFFFFF), Color(0xFFF3F4F6))
                    )
                } else {
                    Brush.linearGradient(
                        listOf(Color(0xFFFFFFFF), Color(0xFFFAFAFA))
                    )
                }
            )
            .border(
                1.dp,
                if (isDarkMode) Color(0x33FFFFFF) else Color(0xFFE5E7EB),
                RoundedCornerShape(26.dp)
            )
            .clickable(
                enabled = !isLoading,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = Color(0x22000000)),
                onClick = onClick
            )
            .testTag("google_sign_in_button"),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = Color(0xFF1F2937),
                strokeWidth = 2.5.dp
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                GoogleLogoIcon(modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = Color(0xFF1F2937),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        letterSpacing = 0.1.sp
                    )
                )
            }
        }
    }
}
