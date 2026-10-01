package com.example.ui.screens

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBackIos
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckBox
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.FormatBold
import androidx.compose.material.icons.rounded.Title
import androidx.compose.material.icons.rounded.FormatItalic
import androidx.compose.material.icons.rounded.FormatListBulleted
import androidx.compose.material.icons.rounded.FormatListNumbered
import androidx.compose.material.icons.rounded.FormatQuote
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import com.example.ui.components.DarkFrostedSnackbarHost
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import kotlinx.coroutines.delay
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.ui.components.AmbientLightingBackground
import com.example.ui.components.DarkFrostedSnackbarHost
import com.example.ui.components.FrostedGlassAlertDialog
import com.example.ui.components.GlassCard
import com.example.ui.theme.AmberGlow
import com.example.ui.theme.AmberWarm
import com.example.ui.theme.GlassStroke
import com.example.ui.theme.LocalIsDarkMode
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarmGold
import kotlinx.coroutines.launch
import com.example.util.cleanAsterisks

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
    initialTitle: String = "",
    initialContent: String = "",
    initialTags: List<String> = emptyList(),
    initialAttachmentUri: String? = null,
    initialNoteId: String? = null,
    onBackClick: () -> Unit,
    onSaveSuccess: (title: String, bullets: List<String>) -> Unit = { _, _ -> },
    onSaveFullNote: (id: String?, title: String, content: String, tags: List<String>, hasAttachment: Boolean, includeInSemanticSearch: Boolean, attachmentUri: String?, isAutoSave: Boolean) -> Unit = { _, _, _, _, _, _, _, _ -> },
    onOpenScanner: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isDarkMode = LocalIsDarkMode.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var title by remember(initialTitle) { mutableStateOf(initialTitle) }
    var contentValue by remember(initialContent) {
        mutableStateOf(androidx.compose.ui.text.input.TextFieldValue(initialContent))
    }

    val tags = remember(initialTags) {
        mutableStateListOf<String>().apply {
            addAll(initialTags)
        }
    }

    var noteId by remember(initialNoteId) {
        mutableStateOf(if (initialNoteId.isNullOrBlank()) "note-${System.currentTimeMillis()}" else initialNoteId)
    }

    var hasAttachment by remember(initialAttachmentUri) { mutableStateOf(initialAttachmentUri != null) }
    var attachedImageUri by remember(initialAttachmentUri) { mutableStateOf(initialAttachmentUri) }
    var attachedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var attachedDocName by remember(initialAttachmentUri) {
        mutableStateOf(if (initialAttachmentUri != null) "Attached_Image.jpg" else "")
    }
    var showPhotoPreviewModal by remember { mutableStateOf(false) }
    var showAddTagDialog by remember { mutableStateOf(false) }
    var includeInSemanticSearch by remember { mutableStateOf(true) }

    val takePhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            attachedBitmap = bitmap
            hasAttachment = true
            attachedDocName = "Camera_Capture_${System.currentTimeMillis()}.jpg"
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            attachedImageUri = uri.toString()
            attachedBitmap = null
            hasAttachment = true
            attachedDocName = "Picked_Image_${System.currentTimeMillis()}.jpg"
        }
    }

    fun applyFormatting(prefix: String, suffix: String = "") {
        contentValue = com.example.util.RichTextHelper.applyFormatting(contentValue, prefix, suffix)
    }

    fun performSaveNote() {
        val noteContent = contentValue.text.cleanAsterisks()
        val finalTitle = when {
            title.isNotBlank() -> title.trim().cleanAsterisks()
            noteContent.isNotBlank() -> {
                val firstLine = noteContent.lines().firstOrNull { it.isNotBlank() }?.trim() ?: "Untitled Note"
                if (firstLine.length > 40) firstLine.take(40) + "..." else firstLine
            }
            else -> "Untitled Note"
        }
        onSaveFullNote(
            noteId,
            finalTitle,
            noteContent,
            tags.toList(),
            hasAttachment,
            includeInSemanticSearch,
            attachedImageUri,
            false // isAutoSave = false (explicit save)
        )
    }

    androidx.activity.compose.BackHandler {
        performSaveNote()
    }

    AmbientLightingBackground {
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { DarkFrostedSnackbarHost(snackbarHostState) },
            modifier = modifier
                .fillMaxSize()
                .testTag("note_editor_screen"),
            topBar = {
                NoteEditorTopBar(
                    onBackClick = onBackClick
                )
            },
            bottomBar = {
                NoteEditorBottomActionDeck(
                    onTakePhoto = { takePhotoLauncher.launch(null) },
                    onUploadImage = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    tags = tags,
                    onRemoveTag = { tag -> tags.remove(tag) },
                    onAddTagClick = { showAddTagDialog = true },
                    includeInSemanticSearch = includeInSemanticSearch,
                    onToggleSemanticSearch = { includeInSemanticSearch = it },
                    onSaveNote = { performSaveNote() }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                // Title Field
                Column(modifier = Modifier.fillMaxWidth()) {
                    BasicTextField(
                        value = title,
                        onValueChange = { title = it },
                        visualTransformation = remember { com.example.util.MarkdownVisualTransformation(isDarkMode = true) },
                        textStyle = TextStyle(
                            color = Color.White,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif,
                            lineHeight = 36.sp,
                            letterSpacing = (-0.5).sp
                        ),
                        cursorBrush = SolidColor(Color.White),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("note_title_input"),
                        decorationBox = { innerTextField ->
                            if (title.isEmpty()) {
                                Text(
                                    text = "Note Title...",
                                    style = TextStyle(
                                        color = Color.White.copy(alpha = 0.7f),
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.SansSerif,
                                        letterSpacing = (-0.4).sp
                                    )
                                )
                            }
                            innerTextField()
                        }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Rich-Text Formatting Toolbar
                val isBoldActive = remember(contentValue) {
                    com.example.util.RichTextHelper.isBoldActive(contentValue)
                }
                val isItalicActive = remember(contentValue) {
                    com.example.util.RichTextHelper.isItalicActive(contentValue)
                }

                RichTextToolbar(
                    isBoldActive = isBoldActive,
                    isItalicActive = isItalicActive,
                    onClearFormat = {
                        contentValue = com.example.util.RichTextHelper.clearFormatting(contentValue)
                    },
                    onBold = {
                        contentValue = com.example.util.RichTextHelper.toggleBold(contentValue)
                    },
                    onItalic = {
                        contentValue = com.example.util.RichTextHelper.toggleItalic(contentValue)
                    },
                    onHeader = { applyFormatting("## ") },
                    onBulletList = { applyFormatting("• ") },
                    onNumberList = { applyFormatting("1. ") },
                    onChecklist = { applyFormatting("[ ] ") },
                    onQuote = { applyFormatting("> ") }
                )
                Spacer(modifier = Modifier.height(14.dp))

                // Expansive Blank Editable Textarea Card - stretches seamlessly to fill viewport
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0x28FFFFFF))
                        .border(
                            1.dp,
                            Brush.verticalGradient(
                                listOf(Color(0x45FFFFFF), Color(0x18FFFFFF))
                            ),
                            RoundedCornerShape(24.dp)
                        )
                        .padding(18.dp)
                        .testTag("note_body_card")
                ) {
                    BasicTextField(
                        value = contentValue,
                        onValueChange = { contentValue = it },
                        visualTransformation = remember { com.example.util.MarkdownVisualTransformation(isDarkMode = true) },
                        textStyle = TextStyle(
                            fontWeight = FontWeight.Normal,
                            fontSize = 16.sp,
                            lineHeight = 26.sp,
                            fontFamily = FontFamily.SansSerif,
                            color = Color.White
                        ),
                        cursorBrush = SolidColor(Color.White),
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("note_body_input"),
                        decorationBox = { innerTextField ->
                            if (contentValue.text.isEmpty()) {
                                Text(
                                    text = "Write your note here... Read and edit full rich-text content freely.",
                                    style = TextStyle(
                                        color = Color.White.copy(alpha = 0.6f),
                                        fontSize = 16.sp,
                                        lineHeight = 24.sp,
                                        fontFamily = FontFamily.SansSerif
                                    )
                                )
                            }
                            innerTextField()
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Attached Photo Card
                if (hasAttachment) {
                    AttachedPhotoCard(
                        docName = attachedDocName.ifBlank { "Attached Photo" },
                        bitmap = attachedBitmap,
                        imageUri = attachedImageUri,
                        onPreviewClick = { showPhotoPreviewModal = true },
                        onRemoveClick = {
                            hasAttachment = false
                            attachedBitmap = null
                            attachedImageUri = null
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Attached photo removed")
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                }
            }
        }
    }

    if (showPhotoPreviewModal && hasAttachment) {
        AttachedPhotoPreviewModal(
            fileName = attachedDocName.ifBlank { "Attached Photo" },
            bitmap = attachedBitmap,
            imageUri = attachedImageUri,
            onDismiss = { showPhotoPreviewModal = false }
        )
    }

    if (showAddTagDialog) {
        AddTagDialog(
            onDismiss = { showAddTagDialog = false },
            onAddTag = { newTag ->
                val formatted = if (newTag.startsWith("#")) newTag.trim() else "#${newTag.trim()}"
                if (formatted.length > 1 && !tags.contains(formatted)) {
                    tags.add(formatted)
                }
                showAddTagDialog = false
            }
        )
    }
}

@Composable
private fun RichTextToolbar(
    isBoldActive: Boolean = false,
    isItalicActive: Boolean = false,
    onClearFormat: () -> Unit,
    onBold: () -> Unit,
    onItalic: () -> Unit,
    onHeader: () -> Unit,
    onBulletList: () -> Unit,
    onNumberList: () -> Unit,
    onChecklist: () -> Unit,
    onQuote: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0x28FFFFFF))
            .border(
                1.dp,
                Brush.horizontalGradient(
                    listOf(Color(0x45FFFFFF), Color(0x18FFFFFF))
                ),
                RoundedCornerShape(24.dp)
            )
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ToolbarTextButton(label = "T", contentDesc = "Clear / Format", onClick = onClearFormat)
            ToolbarTextButton(label = "B", contentDesc = "Bold", onClick = onBold, isActive = isBoldActive)
            ToolbarTextButton(label = "I", contentDesc = "Italic", onClick = onItalic, isItalic = true, isActive = isItalicActive)
            ToolbarIconButton(icon = Icons.Rounded.FormatListBulleted, contentDesc = "Bullet List", onClick = onBulletList)
            ToolbarIconButton(icon = Icons.Rounded.FormatListNumbered, contentDesc = "Numbered List", onClick = onNumberList)
            ToolbarIconButton(icon = Icons.Rounded.CheckBox, contentDesc = "Checklist", onClick = onChecklist)
            ToolbarIconButton(icon = Icons.Rounded.FormatQuote, contentDesc = "Quote", onClick = onQuote)
        }
    }
}

@Composable
private fun ToolbarTextButton(
    label: String,
    contentDesc: String,
    onClick: () -> Unit,
    isItalic: Boolean = false,
    isActive: Boolean = false
) {
    val bgColor = if (isActive) Color(0x45FFB87E) else Color(0x20FFFFFF)
    val borderColor = if (isActive) Color(0xFFFFB87E) else Color.White.copy(alpha = 0.25f)
    val textColor = if (isActive) Color(0xFFFFB87E) else Color.White

    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = TextStyle(
                color = textColor,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                fontStyle = if (isItalic) androidx.compose.ui.text.font.FontStyle.Italic else androidx.compose.ui.text.font.FontStyle.Normal
            )
        )
    }
}

@Composable
private fun ToolbarIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDesc: String,
    onClick: () -> Unit,
    isActive: Boolean = false
) {
    val bgColor = if (isActive) Color(0x45FFB87E) else Color(0x20FFFFFF)
    val borderColor = if (isActive) Color(0xFFFFB87E) else Color.White.copy(alpha = 0.25f)
    val iconColor = if (isActive) Color(0xFFFFB87E) else Color.White

    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDesc,
            tint = iconColor,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun NoteEditorTopBar(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
// Note: Back button is removed as per requirements (nav handled by system back).
            // Keeping the header area clean.
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "GT",
                    style = TextStyle(
                        fontFamily = FontFamily.Cursive,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 24.sp,
                        brush = Brush.horizontalGradient(
                            listOf(
                                Color(0xFF00E5FF),
                                Color(0xFFFF2D55),
                                Color(0xFFFF9500)
                            )
                        )
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Notepad",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = Color.White
                    ),
                    modifier = Modifier.testTag("note_editor_header_title")
                )
            }
        }
    }
}

@Composable
private fun NoteEditorBottomActionDeck(
    onTakePhoto: () -> Unit,
    onUploadImage: () -> Unit,
    tags: List<String>,
    onRemoveTag: (String) -> Unit,
    onAddTagClick: () -> Unit,
    includeInSemanticSearch: Boolean,
    onToggleSemanticSearch: (Boolean) -> Unit,
    onSaveNote: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(Color(0xD91A1822))
            .border(
                1.dp,
                Brush.verticalGradient(
                    listOf(Color(0x55FFFFFF), Color(0x1AFFFFFF))
                ),
                RoundedCornerShape(28.dp)
            )
            .testTag("note_bottom_action_deck")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Row 2: Dynamic Tag Area
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("note_tags_row"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(tags, key = { it }) { tag ->
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color(0x35FFB87E))
                                .border(1.dp, Color(0xFFFFB87E), CircleShape)
                                .padding(start = 14.dp, end = 10.dp, top = 8.dp, bottom = 8.dp)
                                .testTag("tag_pill_${tag.removePrefix("#")}")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = tag,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = Color(0xFFFFB87E),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(Color(0x30FFFFFF))
                                        .clickable { onRemoveTag(tag) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                                        contentDescription = "Remove Tag",
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color(0x28FFFFFF))
                                .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(color = Color.White.copy(alpha = 0.3f)),
                                    onClick = onAddTagClick
                                )
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .testTag("add_tag_button")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "+ + Add Tag",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Row 3: Toggle switch card for AI Semantic Search
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0x28FFFFFF))
                    .border(
                        1.dp,
                        Brush.verticalGradient(
                            listOf(Color(0x45FFFFFF), Color(0x18FFFFFF))
                        ),
                        RoundedCornerShape(24.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp)
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
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0x30FFB87E)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = Color(0xFFFFB87E),
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Text(
                            text = "Include in AI Semantic Search",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                                color = Color.White
                            )
                        )
                    }

                    Switch(
                        checked = includeInSemanticSearch,
                        onCheckedChange = onToggleSemanticSearch,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF2E1A0C),
                            checkedTrackColor = Color(0xFFFFB87E),
                            uncheckedThumbColor = Color(0x88FFFFFF),
                            uncheckedTrackColor = Color(0x30FFFFFF)
                        ),
                        modifier = Modifier.testTag("semantic_search_toggle")
                    )
                }
            }

            // Row 4: Save Note Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(CircleShape)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color(0xFFFFD194),
                                Color(0xFFD19153)
                            )
                        )
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.6f), CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(color = Color.White.copy(alpha = 0.35f)),
                        onClick = onSaveNote
                    )
                    .padding(vertical = 16.dp)
                    .testTag("save_note_button"),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        tint = Color(0xFF2E1A0C),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Save Note",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = Color(0xFF2E1A0C),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 17.sp
                        ),
                        modifier = Modifier.testTag("save_note_button_text")
                    )
                }
            }
        }
    }
}

@Composable
private fun AttachedPhotoCard(
    docName: String,
    bitmap: Bitmap?,
    imageUri: String?,
    onPreviewClick: () -> Unit,
    onRemoveClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDarkMode = LocalIsDarkMode.current
    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("note_attached_photo"),
        shape = RoundedCornerShape(18.dp),
        borderStrokeWidth = 1.dp,
        borderColorList = listOf(Color(0x80FFFFFF), Color(0x22FFFFFF), Color(0x40FFFFFF)),
        ambientGlowColor = AmberWarm
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(color = AmberWarm.copy(alpha = 0.2f)),
                        onClick = onPreviewClick
                    )
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 54.dp, height = 54.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0x20FFFFFF))
                        .border(1.dp, GlassStroke, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Attached thumbnail",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else if (imageUri != null) {
                        AsyncImage(
                            model = imageUri,
                            contentDescription = "Attached thumbnail",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.Image,
                            contentDescription = null,
                            tint = AmberWarm,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "Attached Photo",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (isDarkMode) TextPrimary else Color(0xFF111827)
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = docName,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = if (isDarkMode) TextSecondary else Color(0xFF4B5563),
                            fontSize = 11.sp
                        )
                    )
                    Text(
                        text = "Tap to view full preview",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = WarmGold,
                            fontSize = 10.sp
                        )
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(if (isDarkMode) Color(0x1AFFFFFF) else Color(0x18000000))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(color = Color.Red.copy(alpha = 0.3f)),
                        onClick = onRemoveClick
                    )
                    .testTag("remove_attached_photo_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = "Remove Photo",
                    tint = TextMuted,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun AttachedPhotoPreviewModal(
    fileName: String,
    bitmap: Bitmap?,
    imageUri: String?,
    onDismiss: () -> Unit
) {
    val isDarkMode = LocalIsDarkMode.current
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .clip(RoundedCornerShape(24.dp))
                .background(if (isDarkMode) Color(0xFF14121A) else Color(0xFFFFFFFF))
                .border(1.dp, if (isDarkMode) GlassStroke else Color(0xFFCBD5E1), RoundedCornerShape(24.dp))
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Photo Attachment",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isDarkMode) TextPrimary else Color(0xFF111827)
                            )
                        )
                        Text(
                            text = fileName,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = if (isDarkMode) TextSecondary else Color(0xFF4B5563),
                                fontSize = 11.sp
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 200.dp, max = 360.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isDarkMode) Color(0xFF1E1B26) else Color(0xFFF3F4F6))
                        .border(1.dp, GlassStroke, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Full photo preview",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    } else if (imageUri != null) {
                        AsyncImage(
                            model = imageUri,
                            contentDescription = "Full photo preview",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AmberWarm,
                        contentColor = Color(0xFF0F0E11)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Close", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun AddTagDialog(
    onDismiss: () -> Unit,
    onAddTag: (String) -> Unit
) {
    var newTagInput by remember { mutableStateOf("") }

    FrostedGlassAlertDialog(
        onDismissRequest = onDismiss,
        title = "Add Custom Tag",
        confirmButtonText = "Add Tag",
        onConfirm = { 
            if (newTagInput.isNotBlank()) {
                onAddTag(newTagInput.trim())
            }
        },
        icon = Icons.Rounded.Add,
        content = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Organize your notes with custom hashtags for faster searching.",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 12.sp)
                )
                Spacer(modifier = Modifier.height(14.dp))
                OutlinedTextField(
                    value = newTagInput,
                    onValueChange = { newTagInput = it },
                    placeholder = {
                        Text(
                            "e.g. #work, #ideas, #bills",
                            color = TextMuted
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_tag_input_field"),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = AmberWarm,
                        unfocusedBorderColor = Color(0x40FFFFFF),
                        cursorColor = AmberWarm,
                        focusedContainerColor = Color(0x10FFFFFF),
                        unfocusedContainerColor = Color(0x10FFFFFF)
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }
    )
}
