package com.example.ui.components

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
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.DriveFileRenameOutline
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarmGold

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemContextMenuSheet(
    item: WorkspaceItem,
    onDismiss: () -> Unit,
    onOpenView: () -> Unit,
    onShare: () -> Unit,
    onRename: () -> Unit,
    onTogglePin: () -> Unit,
    onDelete: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val (typeGradient, typeIcon) = when (item.type) {
        ItemType.PDF -> Pair(listOf(Color(0xFFFF3B30), Color(0xFFFF2D55)), Icons.Rounded.PictureAsPdf)
        ItemType.NOTE -> Pair(listOf(Color(0xFFFF9500), Color(0xFFFFCC00)), Icons.Rounded.EditNote)
        ItemType.IMAGE -> Pair(listOf(Color(0xFF5856D6), Color(0xFFAF52DE)), ModernGalleryIconVector)
        ItemType.VOICE -> Pair(listOf(Color(0xFFFF2D55), Color(0xFFFF9500)), Icons.Rounded.GraphicEq)
        else -> Pair(listOf(Color(0xFFFF9500), Color(0xFFFF5E3A)), Icons.Rounded.Description)
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
        modifier = Modifier.testTag("item_context_menu_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Header Info Card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0x18FFFFFF))
                    .border(1.dp, GlassStroke, RoundedCornerShape(18.dp))
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(typeGradient)
                        )
                        .border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = typeIcon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${item.type.label} • ${item.sizeText} • ${item.dateModified}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextSecondary,
                            fontSize = 12.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Options List
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0x10FFFFFF))
                    .border(1.dp, Color(0x18FFFFFF), RoundedCornerShape(20.dp))
            ) {
                ContextMenuOptionRow(
                    icon = Icons.Rounded.OpenInNew,
                    title = "Open / View",
                    subtitle = "Open full preview and viewer",
                    tint = Color(0xFF00F5D4),
                    onClick = {
                        onDismiss()
                        onOpenView()
                    },
                    testTag = "menu_option_open"
                )

                OptionDivider()

                ContextMenuOptionRow(
                    icon = Icons.Rounded.Share,
                    title = "Share File",
                    subtitle = "Export or send to other applications",
                    tint = WarmGold,
                    onClick = {
                        onDismiss()
                        onShare()
                    },
                    testTag = "menu_option_share"
                )

                OptionDivider()

                ContextMenuOptionRow(
                    icon = Icons.Rounded.DriveFileRenameOutline,
                    title = "Rename",
                    subtitle = "Change file or memory title",
                    tint = Color(0xFF60A5FA),
                    onClick = {
                        onDismiss()
                        onRename()
                    },
                    testTag = "menu_option_rename"
                )

                OptionDivider()

                ContextMenuOptionRow(
                    icon = Icons.Rounded.PushPin,
                    title = if (item.isPinned) "Unpin from Important" else "Pin to Important",
                    subtitle = if (item.isPinned) "Remove from pinned section" else "Keep pinned at the top",
                    tint = if (item.isPinned) AmberWarm else Color(0xFFE2E8F0),
                    onClick = {
                        onDismiss()
                        onTogglePin()
                    },
                    testTag = "menu_option_pin"
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Delete Danger Action
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0x1AFF3B30))
                    .border(1.dp, Color(0x44FF3B30), RoundedCornerShape(20.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(color = Color(0xFFFF3B30).copy(alpha = 0.2f)),
                        onClick = {
                            onDismiss()
                            onDelete()
                        }
                    )
                    .padding(horizontal = 16.dp, vertical = 14.dp)
                    .testTag("menu_option_delete")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0x33FF3B30)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = "Delete",
                            tint = Color(0xFFFF453A),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        Text(
                            text = "Delete from Vault",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = Color(0xFFFF453A),
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        )
                        Text(
                            text = "Permanently remove this item and index",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xAAFF453A),
                                fontSize = 11.sp
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ContextMenuOptionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    tint: Color,
    onClick: () -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = tint.copy(alpha = 0.15f)),
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 13.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(tint.copy(alpha = 0.15f))
                .border(1.dp, tint.copy(alpha = 0.3f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = tint,
                modifier = Modifier.size(19.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            )
        }
    }
}

@Composable
private fun OptionDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Color(0x12FFFFFF))
    )
}
