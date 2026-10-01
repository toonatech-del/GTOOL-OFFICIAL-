package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBackIos
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.AmbientLightingBackground
import com.example.ui.components.DarkFrostedSnackbarHost
import com.example.ui.components.GlassCard
import com.example.ui.theme.AmberGlow
import com.example.ui.theme.AmberWarm
import com.example.ui.theme.GlassStroke
import com.example.ui.theme.SyncGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarmGold
import kotlinx.coroutines.launch
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceMemoryScreen(
    onBackClick: () -> Unit,
    onSaveVoiceMemory: (title: String, transcription: String, summary: String, hasReminder: Boolean) -> Unit = { _, _, _, _ -> },
    modifier: Modifier = Modifier
) {
    // Audio recording state
    var isRecording by remember { mutableStateOf(true) }
    var timestamp by remember { mutableStateOf("00:45") }
    var reminderEnabled by remember { mutableStateOf(true) }

    val transcriptionText = "Kal Rahul se 5 baje meeting hai."
    val summaryText = "Meeting with Rahul tomorrow at 5:00 PM"
    val tags = listOf("#rahul", "#meeting", "#5pm")

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    AmbientLightingBackground {
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { DarkFrostedSnackbarHost(snackbarHostState) },
            modifier = modifier
                .fillMaxSize()
                .testTag("voice_memory_screen"),
            topBar = {
                VoiceMemoryTopBar(onBackClick = onBackClick)
            },
            bottomBar = {
                // Bottom Save Action
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .clip(RoundedCornerShape(27.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(AmberWarm, AmberGlow)
                                )
                            )
                            .border(
                                1.dp,
                                Color.White.copy(alpha = 0.5f),
                                RoundedCornerShape(27.dp)
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(color = Color.White.copy(alpha = 0.4f)),
                                onClick = {
                                    onSaveVoiceMemory(
                                        summaryText,
                                        transcriptionText,
                                        summaryText,
                                        reminderEnabled
                                    )
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar(
                                            if (reminderEnabled) "Voice Memory saved & Reminder set for Tomorrow at 5:00 PM 🔔"
                                            else "Voice Memory saved to Neural Vault ⚡"
                                        )
                                    }
                                }
                            )
                            .testTag("save_voice_memory_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF0F0E11),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Save Voice Memory",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = Color(0xFF0F0E11),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp,
                                    letterSpacing = 0.2.sp
                                ),
                                modifier = Modifier.testTag("save_voice_memory_text")
                            )
                        }
                    }
                }
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(
                    start = 20.dp,
                    end = 20.dp,
                    top = 8.dp,
                    bottom = 24.dp
                )
            ) {
                // Header Title: 'Voice Memories' in large bold font
                item(key = "title_section") {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Realtime Audio Status Pill
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0x28FF9E58))
                                    .border(
                                        1.dp,
                                        Brush.linearGradient(
                                            listOf(AmberWarm.copy(alpha = 0.6f), Color(0x30FFA767))
                                        ),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(if (isRecording) Color(0xFFFF5252) else SyncGreen)
                                            .drawBehind {
                                                if (isRecording) {
                                                    drawCircle(
                                                        color = Color(0xFFFF5252).copy(alpha = 0.5f),
                                                        radius = size.width * 1.6f
                                                    )
                                                }
                                            }
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isRecording) "● Live Audio Recording" else "✓ Recording Complete",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = WarmGold,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                            }

                            Text(
                                text = "Whisper AI v3",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // 'Voice Memories' in large bold font
                        Text(
                            text = "Voice Memories",
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 32.sp,
                                letterSpacing = (-0.8).sp,
                                fontFamily = FontFamily.SansSerif,
                                color = TextPrimary
                            ),
                            modifier = Modifier.testTag("voice_memories_title")
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Real-time speech transcription & intelligent contextual intent extraction",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = TextSecondary,
                                fontSize = 13.sp
                            )
                        )
                    }
                }

                // 1. Center Active Voice Recording Card with dynamic glowing audio waveform and timestamp '00:45'
                item(key = "active_recording_card") {
                    Spacer(modifier = Modifier.height(20.dp))

                    ActiveVoiceRecordingCard(
                        isRecording = isRecording,
                        timestamp = timestamp,
                        onToggleRecording = { isRecording = !isRecording }
                    )
                }

                // 2. Translucent frosted card showing live AI transcription: "Kal Rahul se 5 baje meeting hai."
                item(key = "transcription_card") {
                    Spacer(modifier = Modifier.height(18.dp))

                    LiveAiTranscriptionCard(
                        transcription = transcriptionText,
                        isLive = isRecording
                    )
                }

                // 3. Automated summary card: "Meeting with Rahul tomorrow at 5:00 PM" with tags '#rahul', '#meeting', '#5pm'
                item(key = "summary_card") {
                    Spacer(modifier = Modifier.height(18.dp))

                    AutomatedSummaryCard(
                        summary = summaryText,
                        tags = tags
                    )
                }

                // 4. Bottom smart reminder card displaying: '🔔 Set Reminder: Tomorrow at 5:00 PM'
                item(key = "reminder_card") {
                    Spacer(modifier = Modifier.height(18.dp))

                    SmartReminderCard(
                        reminderText = "🔔 Set Reminder: Tomorrow at 5:00 PM",
                        isEnabled = reminderEnabled,
                        onToggle = { reminderEnabled = it }
                    )
                }
            }
        }
    }
}

/**
 * Top bar with frosted back button
 */
@Composable
private fun VoiceMemoryTopBar(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(Color(0x22FFFFFF))
                .border(1.dp, GlassStroke, CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(color = Color.White.copy(alpha = 0.3f)),
                    onClick = onBackClick
                )
                .testTag("voice_memory_back_button"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBackIos,
                contentDescription = "Back",
                tint = TextPrimary,
                modifier = Modifier.size(18.dp)
            )
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0x1AFFFFFF))
                .border(1.dp, GlassStroke, RoundedCornerShape(20.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.GraphicEq,
                    contentDescription = null,
                    tint = AmberWarm,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Neural Speech Engine",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
        }
    }
}

/**
 * Center active voice recording card with dynamic glowing audio waveform and timestamp '00:45'
 */
@Composable
private fun ActiveVoiceRecordingCard(
    isRecording: Boolean,
    timestamp: String,
    onToggleRecording: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Dynamic waveform animation
    val infiniteTransition = rememberInfiniteTransition(label = "waveform_anim")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.283f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("active_voice_recording_card"),
        shape = RoundedCornerShape(26.dp),
        borderStrokeWidth = 1.dp,
        borderColorList = listOf(
            Color(0x95FFFFFF),
            Color(0x28FFFFFF),
            Color(0x50FFFFFF)
        ),
        backgroundGradient = listOf(
            Color(0x28FFFFFF),
            Color(0x14FFFFFF),
            Color(0x0AFFFFFF)
        ),
        ambientGlowColor = AmberWarm
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Audio Device Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0x26FF9E58)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Mic,
                            contentDescription = null,
                            tint = AmberWarm,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "VOICE INPUT CAPTURE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = WarmGold,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            letterSpacing = 0.8.sp
                        )
                    )
                }

                // Sample rate tag
                Text(
                    text = "48 kHz • Stereo HD",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextMuted,
                        fontSize = 10.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Large digital timestamp '00:45'
            Text(
                text = timestamp,
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 44.sp,
                    letterSpacing = 2.sp,
                    fontFamily = FontFamily.SansSerif,
                    color = TextPrimary
                ),
                modifier = Modifier.testTag("recording_timestamp")
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = if (isRecording) "LISTENING..." else "AUDIO PAUSED",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = if (isRecording) AmberWarm else TextMuted,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                    fontSize = 11.sp
                )
            )

            Spacer(modifier = Modifier.height(22.dp))

            // Dynamic Glowing Audio Waveform
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(84.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0x18000000))
                    .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(16.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("audio_waveform_canvas")
                ) {
                    val barCount = 36
                    val barWidth = (size.width / (barCount * 1.6f)).coerceAtLeast(3f)
                    val spacing = (size.width - (barCount * barWidth)) / (barCount - 1)
                    val centerY = size.height / 2f

                    for (i in 0 until barCount) {
                        val x = i * (barWidth + spacing)
                        // Harmonic wave height math
                        val factor = if (isRecording) {
                            val wave1 = sin((i * 0.35f) + phase)
                            val wave2 = sin((i * 0.7f) - phase * 0.8f)
                            val combined = (wave1 + wave2 * 0.5f).toFloat()
                            val normalized = (combined + 1.5f) / 3f
                            (normalized * 0.85f + 0.15f).coerceIn(0.1f, 1.0f)
                        } else {
                            0.2f
                        }

                        val barHeight = (size.height * 0.85f * factor).coerceAtLeast(6f)
                        val top = centerY - (barHeight / 2f)

                        // Ambient glow draw behind
                        drawRoundRect(
                            color = AmberWarm.copy(alpha = if (isRecording) 0.3f else 0.1f),
                            topLeft = Offset(x - 2f, top - 2f),
                            size = Size(barWidth + 4f, barHeight + 4f),
                            cornerRadius = CornerRadius(6f, 6f)
                        )

                        // Main golden gradient bar
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                listOf(AmberWarm, WarmGold, Color(0xFFFFCC80))
                            ),
                            topLeft = Offset(x, top),
                            size = Size(barWidth, barHeight),
                            cornerRadius = CornerRadius(4f, 4f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // Control Buttons: Mic / Pause / Stop
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Mic / Pause Toggle Button
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                if (isRecording) listOf(AmberWarm, AmberGlow)
                                else listOf(Color(0x33FFFFFF), Color(0x1AFFFFFF))
                            )
                        )
                        .border(
                            1.dp,
                            if (isRecording) Color.White.copy(alpha = 0.6f) else GlassStroke,
                            CircleShape
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(color = Color.White.copy(alpha = 0.4f)),
                            onClick = onToggleRecording
                        )
                        .testTag("toggle_recording_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isRecording) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (isRecording) "Pause" else "Record",
                        tint = if (isRecording) Color(0xFF0F0E11) else TextPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Stop Button
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0x22FFFFFF))
                        .border(1.dp, GlassStroke, CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(color = AmberWarm.copy(alpha = 0.3f)),
                            onClick = onToggleRecording
                        )
                        .testTag("stop_recording_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Stop,
                        contentDescription = "Stop",
                        tint = TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/**
 * Translucent frosted card showing live AI transcription:
 * "Kal Rahul se 5 baje meeting hai."
 */
@Composable
private fun LiveAiTranscriptionCard(
    transcription: String,
    isLive: Boolean,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("transcription_card"),
        shape = RoundedCornerShape(24.dp),
        borderStrokeWidth = 1.dp,
        borderColorList = listOf(
            Color(0x85FFFFFF),
            Color(0x22FFFFFF),
            Color(0x40FFFFFF)
        ),
        backgroundGradient = listOf(
            Color(0x24FFFFFF),
            Color(0x12FFFFFF),
            Color(0x08FFFFFF)
        ),
        ambientGlowColor = AmberWarm
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0x26FF9E58)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.RecordVoiceOver,
                            contentDescription = null,
                            tint = AmberWarm,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Text(
                        text = "LIVE AI TRANSCRIPTION",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = WarmGold,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            letterSpacing = 0.8.sp
                        )
                    )
                }

                // Language detection pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0x1EFFFFFF))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "Hinglish • 99.8% Conf.",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = TextSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Transcription Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0x1CFFFFFF))
                    .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "\"",
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = AmberWarm,
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp
                        )
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    // Exact prompt text: "Kal Rahul se 5 baje meeting hai."
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = transcription,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                lineHeight = 24.sp,
                                color = TextPrimary,
                                fontFamily = FontFamily.SansSerif
                            ),
                            modifier = Modifier.testTag("transcription_text")
                        )

                        if (isLive) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(AmberWarm)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Streaming audio stream...",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = TextMuted,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }
                    }

                    Text(
                        text = "\"",
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = AmberWarm,
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp
                        )
                    )
                }
            }
        }
    }
}

/**
 * Automated summary card:
 * "Meeting with Rahul tomorrow at 5:00 PM" with keyword tags '#rahul', '#meeting', '#5pm'
 */
@Composable
private fun AutomatedSummaryCard(
    summary: String,
    tags: List<String>,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("automated_summary_card"),
        shape = RoundedCornerShape(24.dp),
        borderStrokeWidth = 1.dp,
        borderColorList = listOf(
            Color(0x85FFFFFF),
            Color(0x22FFFFFF),
            Color(0x40FFFFFF)
        ),
        backgroundGradient = listOf(
            Color(0x26FFFFFF),
            Color(0x12FFFFFF),
            Color(0x08FFFFFF)
        ),
        ambientGlowColor = AmberWarm
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0x26FF9E58)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = AmberWarm,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Text(
                        text = "AUTOMATED SUMMARY",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = WarmGold,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            letterSpacing = 0.8.sp
                        )
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0x284ADE80))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "Intent Extracted",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = SyncGreen,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Exact prompt text: "Meeting with Rahul tomorrow at 5:00 PM"
            Text(
                text = summary,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    lineHeight = 22.sp,
                    color = TextPrimary,
                    fontFamily = FontFamily.SansSerif
                ),
                modifier = Modifier.testTag("summary_text")
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Auto-generated keyword tags: '#rahul', '#meeting', '#5pm'
            Text(
                text = "KEYWORD TAGS",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = TextMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.6.sp
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                tags.forEach { tag ->
                    VoiceTagPill(tag = tag)
                }
            }
        }
    }
}

/**
 * Tag pill with 1px border stroke
 */
@Composable
private fun VoiceTagPill(tag: String) {
    val accentColor = when (tag) {
        "#rahul" -> AmberWarm
        "#meeting" -> WarmGold
        "#5pm" -> Color(0xFF38BDF8)
        else -> TextSecondary
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.linearGradient(
                    listOf(Color(0x24FFFFFF), Color(0x0EFFFFFF))
                )
            )
            .border(
                1.dp,
                Brush.linearGradient(
                    listOf(accentColor.copy(alpha = 0.5f), Color(0x20FFFFFF))
                ),
                RoundedCornerShape(18.dp)
            )
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag("tag_pill_${tag.removePrefix("#")}")
    ) {
        Text(
            text = tag,
            style = MaterialTheme.typography.labelSmall.copy(
                color = accentColor,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                fontFamily = FontFamily.SansSerif
            )
        )
    }
}

/**
 * Bottom smart reminder card displaying:
 * '🔔 Set Reminder: Tomorrow at 5:00 PM'
 */
@Composable
private fun SmartReminderCard(
    reminderText: String,
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.linearGradient(
                    listOf(Color(0x2CFFFFFF), Color(0x14FFFFFF), Color(0x0CFFFFFF))
                )
            )
            .border(
                1.dp,
                Brush.linearGradient(
                    listOf(
                        if (isEnabled) AmberWarm.copy(alpha = 0.7f) else Color(0x40FFFFFF),
                        Color(0x20FFFFFF)
                    )
                ),
                RoundedCornerShape(22.dp)
            )
            .padding(18.dp)
            .testTag("smart_reminder_card")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Bell / Notification container
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isEnabled) Color(0x33FF9E58) else Color(0x18FFFFFF)
                        )
                        .border(
                            1.dp,
                            if (isEnabled) AmberWarm.copy(alpha = 0.5f) else Color(0x20FFFFFF),
                            RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.NotificationsActive,
                        contentDescription = null,
                        tint = if (isEnabled) AmberWarm else TextMuted,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    // Exact text requirement: '🔔 Set Reminder: Tomorrow at 5:00 PM'
                    Text(
                        text = reminderText,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            color = TextPrimary
                        ),
                        modifier = Modifier.testTag("smart_reminder_text")
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = if (isEnabled) "Auto-synced with Google Calendar & Alarms" else "Reminder dismissed",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = if (isEnabled) WarmGold else TextMuted,
                            fontSize = 11.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Switch toggle
            Switch(
                checked = isEnabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color(0xFF0F0E11),
                    checkedTrackColor = AmberWarm,
                    uncheckedThumbColor = Color(0xFF888888),
                    uncheckedTrackColor = Color(0x33FFFFFF)
                ),
                modifier = Modifier.testTag("reminder_switch")
            )
        }
    }
}
