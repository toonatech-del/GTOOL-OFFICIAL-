package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ItemType
import com.example.ui.theme.AmberGlow
import com.example.ui.theme.AmberWarm
import com.example.ui.theme.GlassStroke
import com.example.ui.theme.PdfRed
import com.example.ui.theme.SyncGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarmGold

data class MemoryActionOption(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val gradientColors: List<Color>,
    val type: ItemType
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMemoryModal(
    onDismiss: () -> Unit,
    onMemoryCreated: (title: String, type: ItemType, isPrivate: Boolean) -> Unit,
    onPdfPicked: (Uri) -> Unit = {},
    onImagePicked: (Uri) -> Unit = {},
    onOpenScanner: () -> Unit = {},
    onOpenNoteEditor: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isPrivateDoc by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Native file picker launcher for PDF (accept: application/pdf)
    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            onDismiss()
            onPdfPicked(uri)
        }
    }

    // Native photo/gallery picker launcher for Image (accept: image/*)
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            onDismiss()
            onImagePicked(uri)
        }
    }

    // Re-balanced options: 4 distinct choices with modern iOS SF gradients
    val actionOptions = listOf(
        MemoryActionOption(
            id = "note",
            title = "📝 Note",
            subtitle = "Rich text & markdown",
            icon = Icons.Rounded.EditNote,
            gradientColors = listOf(Color(0xFFFF9500), Color(0xFFFFCC00)),
            type = ItemType.NOTE
        ),
        MemoryActionOption(
            id = "pdf",
            title = "📄 PDF",
            subtitle = "Native file import",
            icon = Icons.Rounded.PictureAsPdf,
            gradientColors = listOf(Color(0xFFFF3B30), Color(0xFFFF2D55)),
            type = ItemType.PDF
        ),
        MemoryActionOption(
            id = "image",
            title = "🖼️ Image",
            subtitle = "Gallery photo & OCR",
            icon = Icons.Rounded.Image,
            gradientColors = listOf(Color(0xFF5856D6), Color(0xFFAF52DE)),
            type = ItemType.IMAGE
        ),
        MemoryActionOption(
            id = "scan",
            title = "📷 Scan Document",
            subtitle = "Auto-crop & enhance",
            icon = Icons.Rounded.DocumentScanner,
            gradientColors = listOf(Color(0xFF007AFF), Color(0xFF5AC8FA)),
            type = ItemType.PDF
        )
    )

    androidx.activity.compose.BackHandler {
        onDismiss()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.Transparent,
        dragHandle = null,
        modifier = modifier.testTag("add_memory_modal")
    ) {
        // Frosted Modal Container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xF218151D),
                            Color(0xF8141217),
                            Color(0xFF0F0E11)
                        )
                    )
                )
                .border(
                    1.dp,
                    Brush.verticalGradient(
                        listOf(
                            Color(0x80FFFFFF),
                            Color(0x22FFFFFF),
                            Color(0x10FFFFFF)
                        )
                    ),
                    RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
                )
                .padding(horizontal = 22.dp, vertical = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Top drag pill
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .width(42.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(Color(0x40FFFFFF))
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Header with title 'Add Memory' in large bold font
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Add Memory",
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 28.sp,
                                letterSpacing = (-0.5).sp,
                                fontFamily = FontFamily.SansSerif,
                                color = TextPrimary
                            ),
                            modifier = Modifier.testTag("add_memory_modal_title")
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Preserve documents & notes into your AI workspace",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Prominent frosted primary card with text '+ Add New Memory'
                ProminentPrimaryAddCard(
                    onClick = {
                        onDismiss()
                        onOpenNoteEditor()
                    }
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Section Label
                Text(
                    text = "CHOOSE MEMORY TYPE",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Balanced 2x2 grid of translucent frosted glass action buttons:
                // '📝 Note', '📄 PDF', '🖼️ Image', and '📷 Scan Document'
                ActionButtonsGrid(
                    options = actionOptions,
                    onOptionClick = { option ->
                        when (option.id) {
                            "pdf" -> {
                                // Immediately trigger device's native document/file picker for PDF
                                pdfPickerLauncher.launch(arrayOf("application/pdf"))
                            }
                            "image" -> {
                                // Immediately trigger device's photo/gallery picker for images
                                imagePickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                            "note" -> {
                                onDismiss()
                                onOpenNoteEditor()
                            }
                            "scan" -> {
                                onDismiss()
                                onOpenScanner()
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.height(22.dp))

                // Bottom quick-toggle switch labeled 'Private Document (Lock with Biometrics)'
                PrivateDocumentToggleCard(
                    isPrivate = isPrivateDoc,
                    onToggleChange = { isPrivateDoc = it }
                )

                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

/**
 * Prominent frosted primary card with text '+ Add New Memory'
 */
@Composable
private fun ProminentPrimaryAddCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = AmberWarm.copy(alpha = 0.35f)),
                onClick = onClick
            )
            .testTag("primary_add_new_memory_card"),
        shape = RoundedCornerShape(24.dp),
        borderStrokeWidth = 1.dp,
        borderColorList = listOf(
            Color(0x80FFB87E),
            Color(0x35FFA767),
            Color(0x70FFB87E)
        ),
        backgroundGradient = listOf(
            Color(0x45E8833A),
            Color(0x30B45309),
            Color(0x251C151B)
        ),
        ambientGlowColor = AmberWarm
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Glowing iOS Squircle + Icon
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFFFF9500), Color(0xFFFF5E3A))
                            )
                        )
                        .border(1.dp, Color.White.copy(alpha = 0.45f), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = "Add New Memory",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = "+ Add New Memory",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 17.sp,
                            color = TextPrimary
                        ),
                        modifier = Modifier.testTag("primary_add_memory_text")
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "Instant neural analysis & automatic tag indexing",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFFF1D8C4),
                            fontSize = 11.sp
                        )
                    )
                }
            }

            // AI badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0x24FFFFFF))
                    .border(1.dp, Color(0x40FFFFFF), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 5.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = WarmGold,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "AI 3.8",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = WarmGold,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }
    }
}

/**
 * Balanced 2x2 grid of translucent frosted glass action buttons:
 * Row 1: '📝 Note' and '📄 PDF'
 * Row 2: '🖼️ Image' and '📷 Scan Document'
 * Each option card features smooth 24px rounded corners, subtle inner glow, and a clean 1px translucent border stroke.
 */
@Composable
private fun ActionButtonsGrid(
    options: List<MemoryActionOption>,
    onOptionClick: (MemoryActionOption) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Row 1: '📝 Note' and '📄 PDF'
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FrostedActionCard(
                option = options[0],
                onClick = { onOptionClick(options[0]) },
                modifier = Modifier.weight(1f)
            )
            FrostedActionCard(
                option = options[1],
                onClick = { onOptionClick(options[1]) },
                modifier = Modifier.weight(1f)
            )
        }

        // Row 2: '🖼️ Image' and '📷 Scan Document'
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FrostedActionCard(
                option = options[2],
                onClick = { onOptionClick(options[2]) },
                modifier = Modifier.weight(1f)
            )
            FrostedActionCard(
                option = options[3],
                onClick = { onOptionClick(options[3]) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Single frosted action card with smooth 24px rounded corners, subtle inner glow, and clean 1px border stroke.
 */
@Composable
private fun FrostedActionCard(
    option: MemoryActionOption,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(24.dp)

    Box(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(
                        Color(0x28FFFFFF),
                        Color(0x15FFFFFF),
                        Color(0x0CFFFFFF)
                    )
                )
            )
            .border(
                1.dp,
                Brush.linearGradient(
                    listOf(
                        Color(0x60FFFFFF),
                        Color(0x18FFFFFF),
                        Color(0x35FFFFFF)
                    )
                ),
                shape
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = option.gradientColors.first().copy(alpha = 0.3f)),
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 18.dp)
            .testTag("action_button_${option.id}")
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.Center
        ) {
            // Modern iOS Squircle Icon with Apple System Gradient
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Brush.linearGradient(option.gradientColors))
                    .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = option.icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = option.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = TextPrimary
                ),
                modifier = Modifier.testTag("action_title_${option.id}")
            )

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = option.subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = TextSecondary,
                    fontSize = 11.sp
                ),
                maxLines = 1
            )
        }
    }
}

/**
 * Bottom quick-toggle switch labeled 'Private Document (Lock with Biometrics)'
 */
@Composable
private fun PrivateDocumentToggleCard(
    isPrivate: Boolean,
    onToggleChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        Color(0x28FFFFFF),
                        Color(0x14FFFFFF),
                        Color(0x0CFFFFFF)
                    )
                )
            )
            .border(
                1.dp,
                Brush.linearGradient(
                    listOf(
                        if (isPrivate) Color(0x80FFB87E) else Color(0x40FFFFFF),
                        Color(0x18FFFFFF),
                        if (isPrivate) Color(0x60FFB87E) else Color(0x30FFFFFF)
                    )
                ),
                RoundedCornerShape(24.dp)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = AmberWarm.copy(alpha = 0.25f)),
                onClick = { onToggleChange(!isPrivate) }
            )
            .padding(horizontal = 18.dp, vertical = 14.dp)
            .testTag("private_document_toggle_card")
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
                // Biometrics / Lock icon in frosted circle
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(
                            if (isPrivate) Color(0x33FF9E58) else Color(0x1AFFFFFF)
                        )
                        .border(
                            1.dp,
                            if (isPrivate) Color(0x66FFB87E) else GlassStroke,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPrivate) Icons.Rounded.Lock else Icons.Rounded.Fingerprint,
                        contentDescription = null,
                        tint = if (isPrivate) AmberWarm else TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "Private Document (Lock with Biometrics)",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = TextPrimary
                        ),
                        modifier = Modifier.testTag("private_document_label")
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = if (isPrivate) "Hardware KeyStore encrypted • Touch ID / PIN required" else "Off • Visible in standard search",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = if (isPrivate) WarmGold else TextMuted,
                            fontSize = 11.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Quick-toggle Switch
            Switch(
                checked = isPrivate,
                onCheckedChange = onToggleChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color(0xFF161318),
                    checkedTrackColor = AmberWarm,
                    checkedBorderColor = AmberGlow,
                    uncheckedThumbColor = TextMuted,
                    uncheckedTrackColor = Color(0x2AFFFFFF),
                    uncheckedBorderColor = Color(0x33FFFFFF)
                ),
                modifier = Modifier.testTag("private_document_switch")
            )
        }
    }
}
