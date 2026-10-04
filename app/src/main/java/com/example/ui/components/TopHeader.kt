package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

// Elegant Champagne Gold / Bronze tone matching the reference UI
private val HeaderGoldTint = Color(0xFFC7A87D)

// Multi-color editorial gradient matching "GTool X" in reference
private val GToolGradient = Brush.horizontalGradient(
    colors = listOf(
        Color(0xFF1E525E), // Teal 'G'
        Color(0xFF2C6470),
        Color(0xFF7E4A4B), // Rose copper 'T'
        Color(0xFFA15957),
        Color(0xFFB56D60), // Terracotta 'oo'
        Color(0xFFC48263),
        Color(0xFFCEAA68), // Antique gold 'l' and 'X'
        Color(0xFFDEC079),
        Color(0xFFE5CA85)
    )
)

@Composable
fun TopHeader(
    unreadCount: Int,
    accountName: String = "User",
    userEmail: String = "",
    profileImageUrl: String? = null,
    onAvatarClick: () -> Unit,
    onNotificationClick: () -> Unit,
    onSettingsClick: () -> Unit = {},
    onAddClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val bellShake = remember { Animatable(0f) }
    
    fun triggerBellShake() {
        coroutineScope.launch {
            repeat(3) {
                bellShake.animateTo(12f, tween(70, easing = LinearEasing))
                bellShake.animateTo(-12f, tween(70, easing = LinearEasing))
            }
            bellShake.animateTo(0f, tween(80))
        }
    }

    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        animProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing)
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .testTag("app_header")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Minimal flat circular outline Settings Button
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .border(1.25.dp, HeaderGoldTint, CircleShape)
                    .background(Color.Transparent)
                    .clickable { onSettingsClick() }
                    .testTag("settings_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = "Settings",
                    tint = HeaderGoldTint,
                    modifier = Modifier.size(23.dp)
                )
            }

            // Center: Minimal flat aesthetic "GTool X" in editorial serif with horizontal gradient
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "GTool X",
                    style = TextStyle(
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Medium,
                        fontSize = 33.sp,
                        letterSpacing = 0.5.sp,
                        brush = GToolGradient,
                        shadow = Shadow(
                            color = Color(0x66000000),
                            offset = Offset(0f, 2f),
                            blurRadius = 4f
                        )
                    )
                )
            }

            // Right: Minimal flat circular outline Bell/Notification Button
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .border(1.25.dp, HeaderGoldTint, CircleShape)
                    .background(Color.Transparent)
                    .clickable {
                        triggerBellShake()
                        onNotificationClick()
                    }
                    .testTag("notification_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Notifications,
                    contentDescription = "Notifications",
                    tint = HeaderGoldTint,
                    modifier = Modifier
                        .size(23.dp)
                        .graphicsLayer {
                            rotationZ = bellShake.value
                        }
                )
            }
        }
    }
}

