package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AmberWarm
import com.example.ui.theme.WarmGold

/**
 * Lightweight 3D/vector mini boy mascot character animation for 'GTOOL X'.
 *
 * Stage 1 (Drop & Bounce): Drops from above onto 'G' with squash & stretch.
 * Stage 2 (Recovery & Run): Stands up, dusts off, and hops across the letters to the right.
 * Stage 3 (Final Pose): Settles next to 'X' in a confident standing & waving pose.
 */
@Composable
fun GtoolMascotCharacter(
    modifier: Modifier = Modifier
) {
    val offsetX = remember { Animatable(0f) }      // 0dp (over G) -> 118dp (next to X)
    val offsetY = remember { Animatable(-55f) }    // -55dp (above screen) -> 0dp
    val scaleX = remember { Animatable(1.0f) }
    val scaleY = remember { Animatable(1.2f) }
    val rotation = remember { Animatable(0f) }

    // Waving hand infinite rotation when settled
    val infiniteTransition = rememberInfiniteTransition(label = "mascot_wave_transition")
    val waveAngle by infiniteTransition.animateFloat(
        initialValue = -12f,
        targetValue = 18f,
        animationSpec = infiniteRepeatable(
            animation = tween(450, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wave_angle"
    )

    LaunchedEffect(Unit) {
        // --- STAGE 1: Drop & Bounce onto 'G' ---
        offsetY.animateTo(0f, tween(380, easing = FastOutSlowInEasing))
        // Squash on impact
        scaleX.snapTo(1.35f)
        scaleY.snapTo(0.65f)
        offsetY.snapTo(4f)
        
        // Bounce recovery
        scaleX.animateTo(0.95f, tween(160))
        scaleY.animateTo(1.1f, tween(160))
        offsetY.animateTo(-6f, tween(160))

        scaleX.animateTo(1.0f, tween(140))
        scaleY.animateTo(1.0f, tween(140))
        offsetY.animateTo(0f, tween(140))

        // --- STAGE 2: Dust off & Hop/Run to 'X' ---
        // Tilt/dust off wobble
        rotation.animateTo(-8f, tween(100))
        rotation.animateTo(8f, tween(100))
        rotation.animateTo(0f, tween(100))

        // Hop 1 (to middle ~60dp)
        offsetY.animateTo(-12f, tween(180, easing = FastOutSlowInEasing))
        offsetX.animateTo(58f, tween(180, easing = LinearEasing))
        offsetY.animateTo(0f, tween(160, easing = FastOutSlowInEasing))

        // Hop 2 (to 'X' position ~118dp)
        offsetY.animateTo(-14f, tween(190, easing = FastOutSlowInEasing))
        offsetX.animateTo(118f, tween(190, easing = LinearEasing))
        offsetY.animateTo(0f, tween(170, easing = FastOutSlowInEasing))

        // --- STAGE 3: Final Pose (Settle near X) ---
        rotation.animateTo(0f, tween(100))
        scaleX.snapTo(1.0f)
        scaleY.snapTo(1.0f)
    }

    Box(
        modifier = modifier
            .offset(x = offsetX.value.dp, y = offsetY.value.dp)
            .graphicsLayer {
                this.scaleX = scaleX.value
                this.scaleY = scaleY.value
                this.rotationZ = rotation.value
            }
            .size(28.dp)
    ) {
        Canvas(modifier = Modifier.size(28.dp)) {
            val w = size.width
            val h = size.height

            // 1. Legs / Sneakers
            drawRoundRect(
                color = Color(0xFF1E1028),
                topLeft = Offset(w * 0.25f, h * 0.72f),
                size = Size(w * 0.2f, h * 0.25f),
                cornerRadius = CornerRadius(4f, 4f)
            )
            drawRoundRect(
                color = Color(0xFF1E1028),
                topLeft = Offset(w * 0.55f, h * 0.72f),
                size = Size(w * 0.2f, h * 0.25f),
                cornerRadius = CornerRadius(4f, 4f)
            )
            // Sneaker white soles
            drawRoundRect(
                color = Color.White,
                topLeft = Offset(w * 0.20f, h * 0.88f),
                size = Size(w * 0.26f, h * 0.1f),
                cornerRadius = CornerRadius(3f, 3f)
            )
            drawRoundRect(
                color = Color.White,
                topLeft = Offset(w * 0.54f, h * 0.88f),
                size = Size(w * 0.26f, h * 0.1f),
                cornerRadius = CornerRadius(3f, 3f)
            )

            // 2. Body / Hoodie (Dark charcoal & Amber trim)
            drawRoundRect(
                color = Color(0xFF2C243B),
                topLeft = Offset(w * 0.2f, h * 0.42f),
                size = Size(w * 0.6f, h * 0.35f),
                cornerRadius = CornerRadius(8f, 8f)
            )
            // Amber Hoodie Stripe
            drawRoundRect(
                color = AmberWarm,
                topLeft = Offset(w * 0.44f, h * 0.42f),
                size = Size(w * 0.12f, h * 0.35f),
                cornerRadius = CornerRadius(2f, 2f)
            )

            // 3. Waving Arm (Right)
            val armPath = Path().apply {
                moveTo(w * 0.75f, h * 0.45f)
                lineTo(w * 0.95f, h * 0.28f)
            }
            drawPath(
                path = armPath,
                color = WarmGold,
                style = Stroke(width = 4f, cap = StrokeCap.Round)
            )
            // Hand (Waving Circle)
            drawCircle(
                color = Color(0xFFFFD1B3),
                radius = 3.5f,
                center = Offset(w * 0.96f, h * 0.24f)
            )

            // 4. Head / Face
            drawCircle(
                color = Color(0xFFFFD1B3), // Warm skin tone
                radius = w * 0.26f,
                center = Offset(w * 0.5f, h * 0.28f)
            )

            // Hair / Cap (Spiky Cool Dark Hair with Gold Accent Visor)
            drawCircle(
                color = Color(0xFF1E1028),
                radius = w * 0.28f,
                center = Offset(w * 0.5f, h * 0.22f)
            )
            // Cap Visor
            drawRoundRect(
                color = WarmGold,
                topLeft = Offset(w * 0.22f, h * 0.16f),
                size = Size(w * 0.56f, h * 0.08f),
                cornerRadius = CornerRadius(4f, 4f)
            )

            // Eyes (Cute dark dots)
            drawCircle(
                color = Color(0xFF130E1B),
                radius = 2f,
                center = Offset(w * 0.42f, h * 0.28f)
            )
            drawCircle(
                color = Color(0xFF130E1B),
                radius = 2f,
                center = Offset(w * 0.6f, h * 0.28f)
            )

            // Cute Smile Arc
            val smilePath = Path().apply {
                moveTo(w * 0.44f, h * 0.35f)
                quadraticTo(w * 0.51f, h * 0.40f, w * 0.58f, h * 0.35f)
            }
            drawPath(
                path = smilePath,
                color = Color(0xFFB85028),
                style = Stroke(width = 2.5f, cap = StrokeCap.Round)
            )
        }
    }
}
