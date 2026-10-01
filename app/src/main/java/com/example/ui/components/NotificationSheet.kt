package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.NotificationItem
import com.example.ui.theme.AmberWarm
import com.example.ui.theme.GlassStroke
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarmGold
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSheet(
    notifications: List<NotificationItem>,
    onDismiss: () -> Unit,
    onCancelNotification: (NotificationItem) -> Unit = {}
) {
    androidx.activity.compose.BackHandler {
        onDismiss()
    }

    BasicAlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth(0.92f)
            .wrapContentHeight()
            .testTag("notification_sheet")
    ) {
        ZoomModalWrapper {
            GlassCard(
                shape = RoundedCornerShape(26.dp),
                borderColorList = listOf(Color(0x66FFFFFF), Color(0x22FFFFFF), Color(0x44FFFFFF)),
                backgroundGradient = listOf(Color(0xF5141217), Color(0xFA1D1822)),
                ambientGlowColor = AmberWarm
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .heightIn(max = 500.dp)
                        .padding(20.dp)
                ) {
                    // Header FIX
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x28FF9E58)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Notifications,
                                    contentDescription = null,
                                    tint = AmberWarm,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Text(
                                text = "Task Reminders & Alerts",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    fontSize = 16.sp
                                )
                            )
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    if (notifications.isEmpty()) {
                        // Clean empty state
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(Color(0x10FFFFFF))
                                .border(1.dp, GlassStroke, RoundedCornerShape(18.dp))
                                .padding(vertical = 28.dp, horizontal = 20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Rounded.NotificationsNone,
                                    contentDescription = null,
                                    tint = TextMuted,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "No active reminders or alerts",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        color = TextSecondary,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 14.sp
                                    )
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Reminders set via voice or text will appear here",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = TextMuted,
                                        fontSize = 12.sp
                                    )
                                )
                            }
                        }
                    } else {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight()
                                .verticalScroll(rememberScrollState())
                        ) {
                            notifications.forEach { item ->
                                CompactReminderItemCard(
                                    item = item,
                                    onCancel = { onCancelNotification(item) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactReminderItemCard(
    item: NotificationItem,
    onCancel: () -> Unit
) {
    val scheduledTime = item.scheduledTimeMillis

    val rawText = when {
        item.title.contains("ko pan card", ignoreCase = true) || item.title.startsWith("Reminder set:", ignoreCase = true) -> item.title
        item.description.contains("ko pan card", ignoreCase = true) || item.description.startsWith("Reminder set:", ignoreCase = true) -> item.description
        item.title == "GTOOL X Reminder" && item.description.isNotBlank() && item.description.length <= 40 -> item.description
        item.title.isNotBlank() && item.title != "GTOOL X Reminder" -> item.title
        else -> item.description
    }

    val cleanTitle = com.example.util.ReminderManager.extractCleanTaskTitle(rawText)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0x18FFFFFF))
            .border(1.dp, GlassStroke, RoundedCornerShape(16.dp))
            .padding(12.dp)
            .testTag("notification_card_${item.id}")
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Top Line: Title in bold font
            Text(
                text = cleanTitle,
                style = MaterialTheme.typography.titleMedium.copy(
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Bottom Line: Neat horizontal Row with Due badge and Delete button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val now = System.currentTimeMillis()
                val isExpired = scheduledTime != null && now > scheduledTime
                val badgeText = if (scheduledTime != null) {
                    if (isExpired) "Due: Expired" else formatDueTime(scheduledTime)
                } else {
                    item.timeAgo
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isExpired) Color(0x28FF453A) else Color(0x28FF9E58))
                        .border(1.dp, if (isExpired) Color(0x55FF453A) else Color(0x55FF9E58), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "⏰", fontSize = 11.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (isExpired) Color(0xFFFF6961) else WarmGold,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp
                            ),
                            maxLines = 1
                        )
                    }
                }

                IconButton(
                    onClick = onCancel,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("btn_cancel_notification_${item.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Cancel Reminder",
                        tint = Color(0xFFFF8A65), // Soft red/orange tint
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

private fun formatDueTime(timestamp: Long): String {
    val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
    val now = Calendar.getInstance()

    val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    val timeStr = timeFormat.format(Date(timestamp))

    return if (cal.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
        cal.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)
    ) {
        "Today, $timeStr"
    } else if (cal.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
        cal.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR) + 1
    ) {
        "Tomorrow, $timeStr"
    } else {
        val dateFormat = SimpleDateFormat("dd MMM, h:mm a", Locale.getDefault())
        dateFormat.format(Date(timestamp))
    }
}
