package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.AmberWarm
import com.example.ui.theme.VoiceCyan
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

/**
 * Global Dark Frosted Glass Snackbar Host matching GTOOL X styling.
 * Container: Dark onyx/charcoal with 1dp translucent border (#33FFFFFF).
 * Icon: Amber/Teal accent icon.
 * Typography: Crisp white.
 */
@Composable
fun DarkFrostedSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    SnackbarHost(hostState = hostState, modifier = modifier) { data ->
        val isError = data.visuals.message.contains("fail", ignoreCase = true) ||
                data.visuals.message.contains("error", ignoreCase = true) ||
                data.visuals.message.contains("delete", ignoreCase = true)
        val isSuccess = data.visuals.message.contains("success", ignoreCase = true) ||
                data.visuals.message.contains("saved", ignoreCase = true) ||
                data.visuals.message.contains("restored", ignoreCase = true) ||
                data.visuals.message.contains("copied", ignoreCase = true)

        val accentColor = when {
            isError -> Color(0xFFFF5252)
            isSuccess -> Color(0xFF00E5FF)
            else -> AmberWarm
        }

        val iconVector: ImageVector = when {
            isError -> Icons.Rounded.Warning
            isSuccess -> Icons.Rounded.CheckCircle
            else -> Icons.Rounded.Info
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .shadow(elevation = 16.dp, shape = RoundedCornerShape(16.dp), spotColor = accentColor)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xF2121820)) // Dark onyx/charcoal (alpha = 0.95f)
                .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(16.dp))
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Amber/Teal or Alert Icon Badge
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(accentColor.copy(alpha = 0.85f), accentColor.copy(alpha = 0.4f))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = iconVector,
                        contentDescription = null,
                        tint = if (isError) Color.White else Color.Black,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Text(
                    text = data.visuals.message,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.5.sp,
                        lineHeight = 18.sp
                    ),
                    modifier = Modifier.weight(1f)
                )

                data.visuals.actionLabel?.let { actionLabel ->
                    Text(
                        text = actionLabel,
                        color = accentColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0x22FFFFFF))
                            .clickable { data.performAction() }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

/**
 * Universal Frosted Dark Slate Dialog / Popup
 * - Surface: Frosted dark slate (#121820 / alpha = 0.95f) with matching glow borders
 * - Actions: Neon amber gradient primary action buttons
 */
@Composable
fun FrostedGlassAlertDialog(
    onDismissRequest: () -> Unit,
    title: String,
    content: @Composable () -> Unit,
    confirmButtonText: String? = null,
    onConfirm: (() -> Unit)? = null,
    dismissButtonText: String? = "Cancel",
    onDismiss: (() -> Unit)? = onDismissRequest,
    icon: ImageVector? = null,
    iconTint: Color = AmberWarm,
    properties: DialogProperties = DialogProperties()
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = properties
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .shadow(24.dp, shape = RoundedCornerShape(24.dp), spotColor = iconTint.copy(alpha = 0.4f))
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xF2121820)) // Dark slate surface alpha = 0.95f
                .border(
                    1.dp,
                    Brush.verticalGradient(
                        listOf(
                            Color(0x55FFFFFF),
                            iconTint.copy(alpha = 0.35f),
                            Color(0x22FFFFFF)
                        )
                    ),
                    RoundedCornerShape(24.dp)
                )
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header with Icon & Title
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (icon != null) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(iconTint.copy(alpha = 0.15f))
                                .border(1.dp, iconTint.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = iconTint,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = TextPrimary
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    IconButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close",
                            tint = TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Dialog Body Content
                Box(modifier = Modifier.fillMaxWidth()) {
                    content()
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (dismissButtonText != null && onDismiss != null) {
                        TextButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = dismissButtonText,
                                color = TextSecondary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    if (confirmButtonText != null && onConfirm != null) {
                        Button(
                            onClick = onConfirm,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                            contentPadding = PaddingValues(0.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(
                                                AmberWarm,
                                                Color(0xFFFFB74D)
                                            )
                                        ),
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                    .padding(horizontal = 20.dp, vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = confirmButtonText,
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
