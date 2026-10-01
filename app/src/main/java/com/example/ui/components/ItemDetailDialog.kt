package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import com.example.util.DocumentSummaryParser
import com.example.model.ItemType
import com.example.model.WorkspaceItem
import com.example.ui.theme.AmberWarm
import com.example.ui.theme.GlassStroke
import com.example.ui.theme.ImagePurple
import com.example.ui.theme.NoteYellow
import com.example.ui.theme.PdfRed
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarmGold

private data class DockActionConfig(
    val onClick: () -> Unit,
    val icon: ImageVector,
    val label: String,
    val containerColor: Color,
    val contentColor: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemDetailDialog(
    item: WorkspaceItem,
    onDismiss: () -> Unit,
    onTogglePin: (WorkspaceItem) -> Unit,
    onAskAiAboutItem: (WorkspaceItem) -> Unit,
    onDeleteItem: ((WorkspaceItem) -> Unit)? = null,
    onUploadToDrive: ((WorkspaceItem) -> Unit)? = null,
    onOpenEditor: ((WorkspaceItem) -> Unit)? = null,
    onOpenImageMemory: ((WorkspaceItem) -> Unit)? = null,
    onOpenVoiceMemory: ((WorkspaceItem) -> Unit)? = null,
    onOpenScanner: ((WorkspaceItem) -> Unit)? = null
) {
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    // Native Hardware & Gesture Back Navigation
    BackHandler {
        onDismiss()
    }

    val (typeColor, typeIcon, typeLabel) = when (item.type) {
        ItemType.PDF -> Triple(PdfRed, Icons.Rounded.PictureAsPdf, "PDF Document")
        ItemType.NOTE -> Triple(NoteYellow, Icons.Rounded.Description, "Note")
        ItemType.IMAGE -> Triple(ImagePurple, ModernGalleryIconVector, "Image")
        ItemType.VOICE -> Triple(AmberWarm, Icons.Rounded.GraphicEq, "Voice Note")
        else -> Triple(AmberWarm, Icons.Rounded.Description, "Document")
    }

    if (showDeleteConfirmation) {
        DeleteConfirmationDialog(
            item = item,
            onConfirmDelete = {
                onDeleteItem?.invoke(item)
                onDismiss()
            },
            onDismiss = { showDeleteConfirmation = false }
        )
    }

    BasicAlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        ),
        modifier = Modifier
            .fillMaxSize()
            .testTag("item_detail_dialog")
    ) {
        // FULL SCREEN Surface (width: 100%, height: 100dvh, edge-to-edge dark glassmorphic styling)
        AmbientLightingBackground {
            ZoomModalWrapper(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xF2131118))
                        .windowInsetsPadding(WindowInsets.statusBars)
                ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp)
                ) {
                    Spacer(modifier = Modifier.height(12.dp))

                    // TOP HEADER ROW: Left metadata group & Sync status, Right Delete button (NO 'X' close button!)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Left: Type Icon & Metadata
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(typeColor.copy(alpha = 0.22f))
                                    .border(1.dp, typeColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = typeIcon,
                                    contentDescription = null,
                                    tint = typeColor,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            val wordCount = item.contentSnippet?.split("\\s+".toRegex())?.filter { it.isNotBlank() }?.size ?: 0
                            val subtitleText = if (wordCount > 0) {
                                "$wordCount words • ${item.dateModified}"
                            } else {
                                "${item.sizeText} • ${item.dateModified}"
                            }

                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = typeLabel,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            color = typeColor,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    // Sync Status Pill
                                    // Sync Status Pill removed
                                    /*Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (item.isSyncedToDrive) Color(0x2234D399) else Color(0x22FF9E58))
                                            .border(1.dp, if (item.isSyncedToDrive) Color(0x6634D399) else Color(0x44FF9E58), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = if (item.isSyncedToDrive) "☁ Synced" else "📱 Local",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = if (item.isSyncedToDrive) Color(0xFF6EE7B7) else WarmGold,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                    }*/
                                }

                                Text(
                                    text = subtitleText,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = TextSecondary,
                                        fontSize = 11.sp
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Right: Frosted Red Delete Button (X close button removed permanently)
                        IconButton(
                            onClick = { showDeleteConfirmation = true },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0x33FF5252))
                                .border(1.dp, Color(0x66FF5252), CircleShape)
                                .testTag("btn_delete_detail_header")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DeleteOutline,
                                contentDescription = "Delete",
                                tint = Color(0xFFFF6B6B),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // ITEM TITLE: Full width, bold font (22sp, bold)
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            color = TextPrimary
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // FULL SCREEN CONTENT BODY (Scrollable edge-to-edge area)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // AI Key Insights Card
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(Color(0x1AFF9E58))
                                .border(1.dp, Color(0x33FF9E58), RoundedCornerShape(18.dp))
                                .padding(16.dp)
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Rounded.AutoAwesome,
                                        contentDescription = null,
                                        tint = WarmGold,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "gsdcall AI Key Insights",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            color = WarmGold,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = item.summary,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = TextPrimary,
                                        fontSize = 14.sp,
                                        lineHeight = 20.sp
                                    )
                                )
                            }
                        }

                        // Document / Note Content Preview Card
                        if (!item.contentSnippet.isNullOrBlank()) {
                            Column {
                                Text(
                                    text = when (item.type) {
                                        ItemType.NOTE -> "Note Content Preview"
                                        ItemType.PDF -> "Document Preview"
                                        ItemType.IMAGE -> "OCR Text Preview"
                                        else -> "Content Preview"
                                    },
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = TextSecondary,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp
                                    )
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(Color(0x1AFFFFFF))
                                        .border(1.dp, GlassStroke, RoundedCornerShape(18.dp))
                                        .padding(16.dp)
                                ) {
                                    Text(
                                        text = item.contentSnippet,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = TextPrimary,
                                            fontSize = 14.sp,
                                            lineHeight = 21.sp
                                        )
                                    )
                                }
                            }
                        }

                        // Auto-Extracted Entity Chips with 1-Tap Copy
                        val clipboardManager = LocalClipboardManager.current
                        val parsedSummary = remember(item) {
                            DocumentSummaryParser.parse(item.contentSnippet ?: item.summary, item.title)
                        }

                        if (parsedSummary.extractedEntities.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Column {
                                Text(
                                    text = "Auto-Extracted Identity & Document Fields",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = WarmGold,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    parsedSummary.extractedEntities.forEach { entity ->
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(Color(0x22FFFFFF))
                                                .border(1.dp, GlassStroke, RoundedCornerShape(14.dp))
                                                .padding(horizontal = 12.dp, vertical = 10.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = entity.label,
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            color = TextSecondary,
                                                            fontSize = 11.sp
                                                        )
                                                    )
                                                    Text(
                                                        text = entity.value,
                                                        style = MaterialTheme.typography.bodyMedium.copy(
                                                            color = TextPrimary,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 13.sp
                                                        )
                                                    )
                                                }

                                                Spacer(modifier = Modifier.width(8.dp))

                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(Color(0x35FF9E58))
                                                        .border(1.dp, AmberWarm.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                                        .clickable {
                                                            clipboardManager.setText(AnnotatedString(entity.value))
                                                        }
                                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(
                                                            imageVector = Icons.Rounded.ContentCopy,
                                                            contentDescription = "Copy ${entity.label}",
                                                            tint = WarmGold,
                                                            modifier = Modifier.size(12.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text(
                                                            text = "Copy",
                                                            style = MaterialTheme.typography.labelSmall.copy(
                                                                color = WarmGold,
                                                                fontWeight = FontWeight.Bold,
                                                                fontSize = 11.sp
                                                            )
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Google Drive Upload Card
                        /*if (onUploadToDrive != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(if (item.isSyncedToDrive) Color(0x1A34D399) else Color(0x1A3B82F6))
                                    .border(1.dp, if (item.isSyncedToDrive) Color(0x5534D399) else Color(0x443B82F6), RoundedCornerShape(18.dp))
                                    .clickable { onUploadToDrive(item) }
                                    .padding(16.dp)
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
                                        Text(text = "☁", fontSize = 22.sp)
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = if (item.isSyncedToDrive) "Backup Status: Synced" else "Upload to Google Drive",
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    color = TextPrimary,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp
                                                )
                                            )
                                            Text(
                                                text = if (item.isSyncedToDrive) "Stored in private drive.appdata folder" else "Backup directly to Drive AppData",
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    color = TextSecondary,
                                                    fontSize = 12.sp
                                                )
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (item.isSyncedToDrive) Color(0x3334D399) else Color(0xFF3B82F6))
                                            .padding(horizontal = 14.dp, vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = if (item.isSyncedToDrive) "Synced ✓" else "Upload ☁",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                            }
                        }*/

                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // BOTTOM ACTION DOCK (Safe-area padded horizontal dock)
                    val primaryDockAction = when {
                        onOpenEditor != null && item.type == ItemType.NOTE -> {
                            DockActionConfig({ onOpenEditor(item); onDismiss() }, Icons.Rounded.Edit, "Edit", AmberWarm, Color(0xFF0F0E11))
                        }
                        onOpenImageMemory != null && (item.type == ItemType.IMAGE || item.title.contains("Electricity", ignoreCase = true)) -> {
                            DockActionConfig({ onOpenImageMemory(item); onDismiss() }, Icons.Rounded.DocumentScanner, "OCR", AmberWarm, Color(0xFF0F0E11))
                        }
                        onOpenScanner != null && (item.type == ItemType.IMAGE || item.type == ItemType.PDF || item.title.contains("Bill", ignoreCase = true)) -> {
                            DockActionConfig({ onOpenScanner(item); onDismiss() }, Icons.Rounded.DocumentScanner, "Scan", Color(0xFF4ADE80), Color(0xFF0F0E11))
                        }
                        onOpenVoiceMemory != null && (item.type == ItemType.VOICE || item.title.contains("Voice", ignoreCase = true) || item.title.contains("Rahul", ignoreCase = true)) -> {
                            DockActionConfig({ onOpenVoiceMemory(item); onDismiss() }, Icons.Rounded.GraphicEq, "Voice", AmberWarm, Color(0xFF0F0E11))
                        }
                        else -> {
                            DockActionConfig({ onOpenEditor?.invoke(item); onDismiss() }, Icons.Rounded.Edit, "Edit", AmberWarm, Color(0xFF0F0E11))
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 16.dp)
                            .testTag("action_dock"),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Button 1: Edit / Primary Action
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(primaryDockAction.containerColor)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(color = Color.White.copy(alpha = 0.3f)),
                                    onClick = primaryDockAction.onClick
                                )
                                .padding(horizontal = 6.dp)
                                .testTag("btn_dock_edit"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = primaryDockAction.icon,
                                    contentDescription = null,
                                    tint = primaryDockAction.contentColor,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = primaryDockAction.label,
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        color = primaryDockAction.contentColor,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    ),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }

                        // Button 2: Ask AI
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0x22FFFFFF))
                                .border(1.dp, GlassStroke, RoundedCornerShape(16.dp))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(color = AmberWarm.copy(alpha = 0.3f)),
                                    onClick = {
                                        onAskAiAboutItem(item)
                                        onDismiss()
                                    }
                                )
                                .padding(horizontal = 6.dp)
                                .testTag("btn_dock_ask_ai"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AutoAwesome,
                                    contentDescription = null,
                                    tint = WarmGold,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Ask AI",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        color = WarmGold,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    ),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }

                        // Button 3: Pin
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (item.isPinned) Color(0x33FF9E58) else Color(0x22FFFFFF))
                                .border(
                                    1.dp,
                                    if (item.isPinned) Color(0x66FF9E58) else GlassStroke,
                                    RoundedCornerShape(16.dp)
                                )
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(color = AmberWarm.copy(alpha = 0.3f)),
                                    onClick = { onTogglePin(item) }
                                )
                                .padding(horizontal = 6.dp)
                                .testTag("btn_dock_pin"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = if (item.isPinned) Icons.Rounded.PushPin else Icons.Rounded.PushPin,
                                    contentDescription = null,
                                    tint = if (item.isPinned) AmberWarm else TextPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (item.isPinned) "Pinned" else "Pin",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        color = if (item.isPinned) AmberWarm else TextPrimary,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp
                                    ),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
}
