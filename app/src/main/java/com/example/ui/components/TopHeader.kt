package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.ui.theme.AmberWarm
import com.example.ui.theme.DarkIconTint
import com.example.ui.theme.LocalIsDarkMode
import com.example.ui.theme.SyncGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.WarmGold
import com.example.ui.theme.WhiteAccentBadge
import kotlinx.coroutines.launch

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
    val isDarkMode = LocalIsDarkMode.current
    val coroutineScope = rememberCoroutineScope()
    
    val bellShake = remember { Animatable(0f) }
    
    fun triggerBellShake() {
        coroutineScope.launch {
            repeat(4) {
                bellShake.animateTo(15f, tween(80, easing = LinearEasing))
                bellShake.animateTo(-15f, tween(80, easing = LinearEasing))
            }
            bellShake.animateTo(0f, tween(100))
        }
    }

    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        animProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing)
        )
    }

    val initials = remember(accountName, userEmail) {
        val nameParts = accountName.trim().split(" ")
        if (nameParts.size >= 2) {
            "${nameParts[0].firstOrNull()?.uppercaseChar() ?: ""}${nameParts[1].firstOrNull()?.uppercaseChar() ?: ""}"
        } else if (accountName.isNotBlank() && !accountName.equals("User", ignoreCase = true)) {
            accountName.take(2).uppercase()
        } else if (userEmail.contains("@")) {
            userEmail.take(2).uppercase()
        } else {
            "U"
        }
    }

    val displayName = remember(accountName, userEmail) {
        if (!accountName.equals("User", ignoreCase = true) && accountName.isNotBlank()) {
            accountName
        } else if (userEmail.contains("@")) {
            userEmail.substringBefore("@").replaceFirstChar { it.uppercase() }
        } else {
            "Member"
        }
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
            // Left: Circular Settings Button
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E1E1E))
                    .border(1.dp, Color(0x33FFFFFF), CircleShape)
                    .clickable { onSettingsClick() }
                    .testTag("settings_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Settings,
                    contentDescription = "Settings",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Center: Branding 'GTool' + Animated Mascot X
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "GTOOL",
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontFamily = FontFamily.Cursive,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 32.sp,
                        letterSpacing = (-0.5).sp,
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF00E5FF),
                                Color(0xFFFF2D55)
                            )
                        )
                    )
                )
                Spacer(modifier = Modifier.width(4.dp))
                AnimatedMascotX(
                    onBellTrigger = { triggerBellShake() },
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            // Right: Notification Button
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E1E1E))
                    .border(1.dp, Color(0x33FFFFFF), CircleShape)
                    .clickable { onNotificationClick() }
                    .graphicsLayer {
                        rotationZ = bellShake.value
                    }
                    .testTag("notification_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Notifications,
                    contentDescription = "Notifications",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
                if (unreadCount > 0) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .align(Alignment.TopEnd)
                            .padding(2.dp)
                            .background(Color.Red, CircleShape)
                            .border(1.dp, Color(0xFF1E1E1E), CircleShape)
                    )
                }
            }
        }
    }
}
