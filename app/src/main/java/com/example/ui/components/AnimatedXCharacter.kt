package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

@Composable
fun AnimatedXCharacter(
    brush: Brush,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "x_glide")
    val offsetX by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 100f, // Glide distance
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "x_glide_offset"
    )

    Canvas(
        modifier = modifier
            .size(30.dp)
            .graphicsLayer { translationX = offsetX }
    ) {
        val w = size.width
        val h = size.height

        val xPath = Path().apply {
            moveTo(0f, 0f)
            lineTo(w, h)
            moveTo(w, 0f)
            lineTo(0f, h)
        }
        drawPath(
            path = xPath,
            brush = brush,
            style = Stroke(width = 8f, cap = StrokeCap.Round)
        )
    }
}
