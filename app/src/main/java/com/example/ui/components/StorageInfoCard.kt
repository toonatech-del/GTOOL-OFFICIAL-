package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.StorageData
import com.example.ui.theme.AmberWarm
import com.example.ui.theme.ImagePurple
import com.example.ui.theme.NoteYellow
import com.example.ui.theme.PdfRed
import com.example.ui.theme.SyncGreen
import com.example.ui.theme.SyncGreenContainer
import com.example.ui.theme.SyncGreenGlow
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarmGold

@Composable
fun StorageInfoCard(
    storageData: StorageData,
    onManageStorageClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val usedRatio = if (storageData.totalGb > 0) (storageData.usedGb / storageData.totalGb).toFloat() else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = usedRatio.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "storageProgress"
    )

    val usedText = when {
        storageData.usedGb <= 0.0001 -> "0 MB"
        storageData.usedGb < 1.0 -> "${maxOf(1, (storageData.usedGb * 1024).toInt())} MB"
        else -> "${"%.1f".format(storageData.usedGb)} GB"
    }
    val totalText = "/ ${storageData.totalGb.toInt()} GB Used"
    val percentageText = when {
        storageData.usedGb <= 0.0001 -> "0% used"
        else -> "${((storageData.usedGb / storageData.totalGb) * 100).toInt()}% used"
    }

    fun formatLegendSize(gb: Double): String = when {
        gb <= 0.0001 -> "0 MB"
        gb < 1.0 -> "${maxOf(1, (gb * 1024).toInt())} MB"
        else -> "${"%.1f".format(gb)} GB"
    }
    val freeGb = maxOf(0.0, storageData.totalGb - storageData.usedGb)

    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .testTag("storage_info_card"),
        shape = RoundedCornerShape(24.dp),
        borderStrokeWidth = 1.dp,
        borderColorList = listOf(
            Color(0x55FFFFFF),
            Color(0x18FFFFFF),
            Color(0x35FFFFFF)
        ),
        backgroundGradient = listOf(
            Color(0x22FFFFFF),
            Color(0x12FFFFFF),
            Color(0x0CFFFFFF)
        ),
        ambientGlowColor = SyncGreen.copy(alpha = 0.5f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header Row: Card Title & Green "☁ Synced" offline-ready status indicator badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Section Title
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0x20FFFFFF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Storage,
                            contentDescription = null,
                            tint = WarmGold,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "Local Phone Storage & Vault",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            letterSpacing = (-0.2).sp,
                            color = TextPrimary
                        ),
                        modifier = Modifier.testTag("storage_info_title")
                    )
                }

                // Green '☁ Synced to Google Drive' status indicator badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(SyncGreenContainer)
                        .border(
                            1.dp,
                            SyncGreen.copy(alpha = 0.5f),
                            RoundedCornerShape(20.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                        .testTag("synced_offline_badge")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Glowing green pulsating dot
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(SyncGreen)
                                .drawBehind {
                                    drawCircle(
                                        brush = Brush.radialGradient(
                                            listOf(SyncGreenGlow, Color.Transparent)
                                        )
                                    )
                                }
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        Text(
                            text = storageData.syncStatusText,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = SyncGreen,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.2.sp
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main Storage Metric: e.g. "0 MB / 5 GB Used" (0% used)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = usedText,
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 28.sp,
                            letterSpacing = (-0.5).sp,
                            fontFamily = FontFamily.SansSerif,
                            color = TextPrimary
                        ),
                        modifier = Modifier.testTag("storage_used_text")
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = totalText,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 16.sp,
                            color = TextSecondary
                        ),
                        modifier = Modifier
                            .padding(bottom = 3.dp)
                            .testTag("storage_total_text")
                    )
                }

                // Percentage indicator
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0x18FFFFFF))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = percentageText,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = WarmGold,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Multi-segment frosted gradient progress bar (empty when 0% used)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color(0x1FFFFFFF))
                    .border(0.5.dp, Color(0x33FFFFFF), RoundedCornerShape(50))
            ) {
                if (animatedProgress > 0.001f && storageData.usedGb > 0.0001) {
                    val totalCategorySum = (storageData.pdfsGb + storageData.imagesGb + storageData.notesGb).toFloat()
                    val pdfWeight = if (totalCategorySum > 0f) (storageData.pdfsGb.toFloat() / totalCategorySum).coerceAtLeast(0.01f) else 0.33f
                    val imgWeight = if (totalCategorySum > 0f) (storageData.imagesGb.toFloat() / totalCategorySum).coerceAtLeast(0.01f) else 0.33f
                    val noteWeight = if (totalCategorySum > 0f) (storageData.notesGb.toFloat() / totalCategorySum).coerceAtLeast(0.01f) else 0.33f

                    Row(
                        modifier = Modifier
                            .fillMaxWidth(animatedProgress)
                            .fillMaxHeight()
                    ) {
                        if (storageData.pdfsGb > 0.0001) {
                            Box(
                                modifier = Modifier
                                    .weight(pdfWeight)
                                    .fillMaxHeight()
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(AmberWarm, AmberWarm.copy(alpha = 0.85f))
                                        )
                                    )
                            )
                        }
                        if (storageData.imagesGb > 0.0001) {
                            Box(
                                modifier = Modifier
                                    .weight(imgWeight)
                                    .fillMaxHeight()
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(ImagePurple, ImagePurple.copy(alpha = 0.85f))
                                        )
                                    )
                            )
                        }
                        if (storageData.notesGb > 0.0001) {
                            Box(
                                modifier = Modifier
                                    .weight(noteWeight)
                                    .fillMaxHeight()
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(WarmGold, WarmGold.copy(alpha = 0.85f))
                                        )
                                    )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Storage Breakdown Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StorageLegendItem(color = AmberWarm, label = "PDFs", size = formatLegendSize(storageData.pdfsGb))
                StorageLegendItem(color = ImagePurple, label = "Images", size = formatLegendSize(storageData.imagesGb))
                StorageLegendItem(color = WarmGold, label = "Notes", size = formatLegendSize(storageData.notesGb))
                StorageLegendItem(color = TextMuted, label = "Free", size = "${"%.1f".format(freeGb)} GB")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Bottom Info Bar with Offline Ready Indicator & Action
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0x12FFFFFF))
                    .border(1.dp, Color(0x20FFFFFF), RoundedCornerShape(14.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(color = Color.White.copy(alpha = 0.2f)),
                        onClick = onManageStorageClick
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp)
                    .testTag("manage_storage_button"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.CloudDone,
                        contentDescription = null,
                        tint = SyncGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "All files cached offline",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    )
                }

                Text(
                    text = "Manage →",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = AmberWarm,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                )
            }
        }
    }
}

@Composable
private fun StorageLegendItem(
    color: Color,
    label: String,
    size: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = TextSecondary,
                    fontSize = 10.sp
                )
            )
            Text(
                text = size,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = TextPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            )
        }
    }
}
