package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ItemType
import com.example.model.WorkspaceItem
import com.example.ui.theme.AmberWarm
import com.example.ui.theme.GlassStroke
import com.example.ui.theme.ImagePurple
import com.example.ui.theme.NoteYellow
import com.example.ui.theme.PdfRed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VoiceCyan
import com.example.ui.theme.WarmGold

@Composable
fun ImportantItemsGrid(
    items: List<WorkspaceItem>,
    onItemClick: (WorkspaceItem) -> Unit,
    onTogglePin: (WorkspaceItem) -> Unit,
    onSetReminder: (WorkspaceItem) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .testTag("important_items_section")
    ) {
        // Section Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFFFF9500), Color(0xFFFF5E3A))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PushPin,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(13.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "Important items",
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        letterSpacing = (-0.3).sp,
                        color = TextPrimary
                    ),
                    modifier = Modifier.testTag("important_items_title")
                )
            }

            Text(
                text = "${items.size} pinned",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = TextMuted,
                    fontSize = 11.sp
                )
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 2-Column Grid of Pinned Items
        val chunkedItems = items.chunked(2)
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            chunkedItems.forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    rowItems.forEach { item ->
                        PinnedGridCard(
                            item = item,
                            onClick = { onItemClick(item) },
                            onTogglePin = { onTogglePin(item) },
                            onSetReminder = onSetReminder,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (rowItems.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun PinnedGridCard(
    item: WorkspaceItem,
    onClick: () -> Unit,
    onTogglePin: (WorkspaceItem) -> Unit,
    onSetReminder: (WorkspaceItem) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val (typeGradient, typeIcon) = when (item.type) {
        ItemType.PDF -> Pair(listOf(Color(0xFFFF3B30), Color(0xFFFF2D55)), Icons.Rounded.PictureAsPdf)
        ItemType.NOTE -> Pair(listOf(Color(0xFFFF9500), Color(0xFFFFCC00)), Icons.Rounded.EditNote)
        ItemType.IMAGE -> Pair(listOf(Color(0xFF5856D6), Color(0xFFAF52DE)), ModernGalleryIconVector)
        ItemType.VOICE -> Pair(listOf(Color(0xFFFF2D55), Color(0xFFFF9500)), Icons.Rounded.GraphicEq)
        else -> Pair(listOf(Color(0xFFFF9500), Color(0xFFFF5E3A)), Icons.Rounded.Description)
    }

    GlassCard(
        modifier = modifier
            .testTag("pinned_card_${item.id}"),
        shape = RoundedCornerShape(18.dp),
        borderStrokeWidth = 1.dp,
        borderColorList = listOf(
            Color(0x45FFFFFF),
            Color(0x14FFFFFF),
            Color(0x28FFFFFF)
        ),
        backgroundGradient = listOf(
            Color(0x20FFFFFF),
            Color(0x10FFFFFF),
            Color(0x0AFFFFFF)
        ),
        ambientGlowColor = typeGradient.first(),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(13.dp)
        ) {
            // Card Top: Type badge & Pin icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Brush.linearGradient(typeGradient))
                        .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = typeIcon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = { onTogglePin(item) },
                    modifier = Modifier.size(26.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PushPin,
                        contentDescription = "Unpin",
                        tint = AmberWarm,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Title
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = TextPrimary
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Subtitle
            Text(
                text = item.subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = TextSecondary,
                    fontSize = 11.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Dynamic OCR Reminder Chip
            if (item.dueDate != null) {
                Spacer(modifier = Modifier.height(8.dp))
                val dateStr = java.text.SimpleDateFormat("dd MMM", java.util.Locale.getDefault()).format(java.util.Date(item.dueDate))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0x18FF9E58))
                        .border(1.dp, AmberWarm.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .clickable { onSetReminder(item) }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "⏰ Due: $dateStr | Set",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = WarmGold,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Tag / Status
            if (item.tag != null) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0x14FFFFFF))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = item.tag,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = WarmGold,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
        }
    }
}
