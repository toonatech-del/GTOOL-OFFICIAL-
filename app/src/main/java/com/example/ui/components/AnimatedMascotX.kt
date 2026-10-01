package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sin

@Composable
fun AnimatedMascotX(
    onBellTrigger: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    
    // Animation states
    val offsetX = remember { Animatable(0f) }
    val offsetY = remember { Animatable(0f) }
    val rotationZ = remember { Animatable(0f) }
    val scaleX = remember { Animatable(1f) }
    var isRunning by remember { mutableStateOf(false) }
    
    // Idle animation values
    val infiniteTransition = rememberInfiniteTransition(label = "mascot_idle")
    val idleBounce by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "idle_bounce"
    )
    val idleWobble by infiniteTransition.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "idle_wobble"
    )
    
    // Leg/Hand cycle for running
    val legCycle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = Math.PI.toFloat() * 2,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "leg_cycle"
    )

    // Running logic
    fun startRunSequence() {
        if (isRunning) return
        coroutineScope.launch {
            isRunning = true
            
            // 1. Run to bell (estimated distance, adjusting for header width)
            // We use a safe target since we don't have exact coordinates here easily without local positioning
            // But usually, the bell is about 120-150dp away in a standard layout
            offsetX.animateTo(140f, tween(1200, easing = FastOutSlowInEasing))
            
            // 2. Reach bell: Trigger shake and jump
            onBellTrigger()
            offsetY.animateTo(-16f, tween(200, easing = FastOutSlowInEasing))
            offsetY.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
            
            delay(300)
            
            // 3. Flip and run back
            scaleX.animateTo(-1.1f, tween(200)) // Flip and slight scale up for effect
            offsetX.animateTo(0f, tween(1200, easing = FastOutSlowInEasing))
            
            // 4. Return to idle
            scaleX.animateTo(1f, tween(200))
            isRunning = false
        }
    }

    // Auto-trigger running sequence periodically
    LaunchedEffect(Unit) {
        while (true) {
            delay(18000) // Every 18 seconds
            startRunSequence()
        }
    }

    Box(
        modifier = modifier
            .offset(x = offsetX.value.dp, y = if (isRunning) offsetY.value.dp else idleBounce.dp)
            .graphicsLayer {
                this.rotationZ = if (isRunning) 0f else idleWobble
                this.scaleX = scaleX.value
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                startRunSequence()
            },
        contentAlignment = Alignment.Center
    ) {
        MascotXCharacter(
            isRunning = isRunning,
            legCycle = legCycle,
            waveAngle = idleWobble
        )
    }
}

@Composable
fun MascotXCharacter(
    isRunning: Boolean,
    legCycle: Float,
    waveAngle: Float
) {
    Box(
        modifier = Modifier.size(40.dp),
        contentAlignment = Alignment.Center
    ) {
        // Draw Legs and Arms behind/around the X
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = size.center
            val neonCyan = Color(0xFF00E5FF)
            val neonPink = Color(0xFFFF2D55)
            
            // Draw Legs
            val legY = center.y + 10f
            val legLength = 12f
            
            // Left Leg
            val leftLegAngle = if (isRunning) sin(legCycle) * 0.8f else 0.1f
            val leftLegEnd = Offset(
                x = center.x - 6f + sin(leftLegAngle) * legLength,
                y = legY + kotlin.math.cos(leftLegAngle) * legLength
            )
            drawLine(
                color = neonCyan,
                start = Offset(center.x - 6f, legY),
                end = leftLegEnd,
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )
            
            // Right Leg
            val rightLegAngle = if (isRunning) -sin(legCycle) * 0.8f else -0.1f
            val rightLegEnd = Offset(
                x = center.x + 6f + sin(rightLegAngle) * legLength,
                y = legY + kotlin.math.cos(rightLegAngle) * legLength
            )
            drawLine(
                color = neonCyan,
                start = Offset(center.x + 6f, legY),
                end = rightLegEnd,
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )
            
            // Draw Arms
            val armY = center.y - 2f
            val armLength = 10f
            
            // Left Arm
            val leftArmAngle = if (isRunning) -sin(legCycle) * 0.7f else (sin(waveAngle * 0.1f) * 0.5f - 0.5f)
            drawLine(
                color = neonPink,
                start = Offset(center.x - 12f, armY),
                end = Offset(
                    center.x - 12f + kotlin.math.cos(Math.PI.toFloat() + leftArmAngle) * armLength,
                    armY + sin(Math.PI.toFloat() + leftArmAngle) * armLength
                ),
                strokeWidth = 2.5.dp.toPx(),
                cap = StrokeCap.Round
            )
            
            // Right Arm
            val rightArmAngle = if (isRunning) sin(legCycle) * 0.7f else (sin(waveAngle * 0.1f + 1f) * 0.5f + 0.5f)
            drawLine(
                color = neonPink,
                start = Offset(center.x + 12f, armY),
                end = Offset(
                    center.x + 12f + kotlin.math.cos(rightArmAngle) * armLength,
                    armY + sin(rightArmAngle) * armLength
                ),
                strokeWidth = 2.5.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
        
        // The X Character
        Text(
            text = "X",
            style = MaterialTheme.typography.displayMedium.copy(
                fontFamily = FontFamily.Cursive,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 30.sp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFF9500),
                        Color(0xFF00E5FF)
                    )
                )
            )
        )
    }
}
