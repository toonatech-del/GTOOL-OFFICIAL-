package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Universal iOS spring-bounce micro-interaction modifier.
 * Uses authentic Apple iOS spring physics (dampingRatio = 0.72f, stiffness = 380f).
 * Compresses smoothly to [pressedScale] (default 0.94f) on touch down, and snaps back
 * with an elastic overshoot bounce on release.
 */
fun Modifier.iosBounce(
    pressedScale: Float = 0.94f,
    pressedAlpha: Float = 0.88f,
    rippleColor: Color = Color.White.copy(alpha = 0.15f),
    onClick: (() -> Unit)? = null
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) pressedScale else 1f,
        animationSpec = spring(
            dampingRatio = if (isPressed) 0.85f else 0.68f,
            stiffness = if (isPressed) 600f else 380f
        ),
        label = "ios_bounce_scale"
    )

    val alpha by animateFloatAsState(
        targetValue = if (isPressed) pressedAlpha else 1f,
        animationSpec = spring(
            dampingRatio = 0.8f,
            stiffness = 400f
        ),
        label = "ios_bounce_alpha"
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
            this.alpha = alpha
        }
        .then(
            if (onClick != null) {
                Modifier.clickable(
                    interactionSource = interactionSource,
                    indication = ripple(color = rippleColor, bounded = true),
                    onClick = onClick
                )
            } else Modifier
        )
}

/**
 * Backwards-compatible zoomOnPress mapping directly to iOS spring bounce physics.
 */
fun Modifier.zoomOnPress(
    pressedScale: Float = 0.95f,
    rippleColor: Color = Color.White.copy(alpha = 0.2f),
    onClick: (() -> Unit)? = null
): Modifier = iosBounce(
    pressedScale = pressedScale,
    pressedAlpha = 0.92f,
    rippleColor = rippleColor,
    onClick = onClick
)

/**
 * iOS Card tactile press physics with subtle 3D tilt / elevation drop.
 */
fun Modifier.iosCardPress(
    onClick: (() -> Unit)? = null
): Modifier = iosBounce(
    pressedScale = 0.965f,
    pressedAlpha = 0.95f,
    rippleColor = Color.White.copy(alpha = 0.12f),
    onClick = onClick
)

/**
 * Animated iOS-style Zoom-In / Spring-Rebound container for modals and dialogs.
 * Animates opening with scale 0.88 -> 1.0 with Apple spring physics and crisp opacity fade-in.
 */
@Composable
fun ZoomModalWrapper(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var isVisible by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.88f,
        animationSpec = spring(
            dampingRatio = 0.72f,
            stiffness = 350f
        ),
        label = "modal_ios_spring_scale"
    )

    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = spring(
            dampingRatio = 0.85f,
            stiffness = 450f
        ),
        label = "modal_ios_spring_alpha"
    )

    LaunchedEffect(Unit) {
        isVisible = true
    }

    Box(
        modifier = modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
            this.alpha = alpha
        }
    ) {
        content()
    }
}

/**
 * iOS Dynamic Ambient Glow & Breathing Border Effect.
 */
fun Modifier.iosPulsingGlow(
    glowColor: Color = Color(0xFF00E5FF),
    maxAlpha: Float = 0.35f
): Modifier = composed {
    val infiniteTransition = rememberInfiniteTransition(label = "ios_pulse_glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.10f,
        targetValue = maxAlpha,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    this.drawBehind {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(glowColor.copy(alpha = glowAlpha), Color.Transparent),
                radius = size.maxDimension * 0.95f
            )
        )
    }
}
