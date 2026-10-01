package com.example.ui.components

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AmberGlow
import com.example.ui.theme.AmberWarm
import com.example.ui.theme.GlassStroke
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarmGold
import com.example.util.ReminderManager
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetReminderBottomSheet(
    initialTitle: String,
    onDismiss: () -> Unit,
    onConfirm: (title: String, timestampMillis: Long) -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var taskTitle by remember { mutableStateOf(initialTitle) }

    // Default to tomorrow 9:00 AM
    val defaultCal = remember {
        Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    var selectedTimestamp by remember { mutableLongStateOf(defaultCal.timeInMillis) }
    var selectedPresetLabel by remember { mutableStateOf("Tomorrow 9:00 AM") }

    fun openDateTimePicker() {
        val currentCal = Calendar.getInstance().apply { timeInMillis = selectedTimestamp }
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val timeCal = Calendar.getInstance().apply { timeInMillis = selectedTimestamp }
                TimePickerDialog(
                    context,
                    { _, hourOfDay, minute ->
                        val finalCal = Calendar.getInstance().apply {
                            set(year, month, dayOfMonth, hourOfDay, minute, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        selectedTimestamp = finalCal.timeInMillis
                        selectedPresetLabel = "Custom Date & Time"
                    },
                    timeCal.get(Calendar.HOUR_OF_DAY),
                    timeCal.get(Calendar.MINUTE),
                    false
                ).show()
            },
            currentCal.get(Calendar.YEAR),
            currentCal.get(Calendar.MONTH),
            currentCal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF141318),
        contentColor = TextPrimary,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(44.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0x55FFFFFF))
            )
        },
        modifier = Modifier.testTag("set_reminder_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 8.dp)
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
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color(0x28FF9E58))
                            .border(1.dp, AmberWarm.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Alarm,
                            contentDescription = "Set Reminder",
                            tint = WarmGold,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "Set Reminder Schedule",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        )
                        Text(
                            text = "Select when to notify you",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Detected Intent & Title
            Text(
                text = "TASK / INTENT",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = WarmGold,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 1.sp
                )
            )

            Spacer(modifier = Modifier.height(6.dp))

            OutlinedTextField(
                value = taskTitle,
                onValueChange = { taskTitle = it },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedBorderColor = AmberWarm,
                    unfocusedBorderColor = Color(0x33FFFFFF),
                    cursorColor = AmberWarm
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_reminder_title")
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Quick Preset Chips
            Text(
                text = "QUICK SCHEDULE PRESETS",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = TextSecondary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 1.sp
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Presets row 1: Tomorrow 9:00 AM, In 3 Days
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SchedulePresetChip(
                    label = "Tomorrow 9:00 AM",
                    isSelected = selectedPresetLabel == "Tomorrow 9:00 AM",
                    onClick = {
                        val cal = Calendar.getInstance().apply {
                            add(Calendar.DAY_OF_YEAR, 1)
                            set(Calendar.HOUR_OF_DAY, 9)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        selectedTimestamp = cal.timeInMillis
                        selectedPresetLabel = "Tomorrow 9:00 AM"
                    },
                    modifier = Modifier.weight(1f)
                )

                SchedulePresetChip(
                    label = "In 3 Days",
                    isSelected = selectedPresetLabel == "In 3 Days",
                    onClick = {
                        val cal = Calendar.getInstance().apply {
                            add(Calendar.DAY_OF_YEAR, 3)
                            set(Calendar.HOUR_OF_DAY, 9)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        selectedTimestamp = cal.timeInMillis
                        selectedPresetLabel = "In 3 Days"
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Presets row 2: In 7 Days, Pick Custom Date & Time
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SchedulePresetChip(
                    label = "In 7 Days",
                    isSelected = selectedPresetLabel == "In 7 Days",
                    onClick = {
                        val cal = Calendar.getInstance().apply {
                            add(Calendar.DAY_OF_YEAR, 7)
                            set(Calendar.HOUR_OF_DAY, 9)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        selectedTimestamp = cal.timeInMillis
                        selectedPresetLabel = "In 7 Days"
                    },
                    modifier = Modifier.weight(1f)
                )

                SchedulePresetChip(
                    label = "Custom Date & Time",
                    icon = Icons.Rounded.DateRange,
                    isSelected = selectedPresetLabel == "Custom Date & Time",
                    onClick = {
                        openDateTimePicker()
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Selected Time Badge Preview Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0x18FFFFFF))
                    .border(1.dp, GlassStroke, RoundedCornerShape(18.dp))
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0x2800F5D4)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Schedule,
                            contentDescription = null,
                            tint = Color(0xFF00F5D4),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "TRIGGER TIME",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFF00F5D4),
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = ReminderManager.formatTimestamp(selectedTimestamp),
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = TextPrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Confirm Button
            val canConfirm = taskTitle.isNotBlank() && selectedTimestamp > System.currentTimeMillis() - 60000
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        if (canConfirm) {
                            Brush.horizontalGradient(listOf(AmberWarm, AmberGlow))
                        } else {
                            Brush.horizontalGradient(listOf(Color(0x30FF9E58), Color(0x20FF9E58)))
                        }
                    )
                    .border(
                        1.dp,
                        if (canConfirm) Color.White.copy(alpha = 0.5f) else Color.Transparent,
                        RoundedCornerShape(18.dp)
                    )
                    .clickable(
                        enabled = canConfirm,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(color = Color.White.copy(alpha = 0.35f)),
                        onClick = {
                            onConfirm(taskTitle.trim(), selectedTimestamp)
                            onDismiss()
                        }
                    )
                    .testTag("btn_confirm_reminder"),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        tint = if (canConfirm) Color(0xFF0F0E11) else Color(0x60FFFFFF),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Confirm & Schedule Reminder",
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = if (canConfirm) Color(0xFF0F0E11) else Color(0x60FFFFFF),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SchedulePresetChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    Box(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (isSelected) AmberWarm.copy(alpha = 0.22f) else Color(0x14FFFFFF)
            )
            .border(
                1.dp,
                if (isSelected) AmberWarm else Color(0x22FFFFFF),
                RoundedCornerShape(14.dp)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = AmberWarm.copy(alpha = 0.2f)),
                onClick = onClick
            )
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) WarmGold else TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    color = if (isSelected) WarmGold else TextPrimary,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 12.sp
                )
            )
        }
    }
}
