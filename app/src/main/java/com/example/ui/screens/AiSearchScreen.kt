package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import java.util.Locale
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBackIos
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Draw
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ripple
import com.example.ui.components.DarkFrostedSnackbarHost
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.model.ChatMessage
import com.example.model.CitationSource
import com.example.model.ItemType
import com.example.model.MessageSender
import com.example.ui.components.AmbientLightingBackground
import com.example.ui.components.GlassCard
import com.example.ui.theme.AmberGlow
import com.example.ui.theme.AmberWarm
import com.example.ui.theme.GlassStroke
import com.example.ui.theme.ImagePurple
import com.example.ui.theme.LocalIsDarkMode
import com.example.ui.theme.NoteYellow
import com.example.ui.theme.PdfRed
import com.example.ui.theme.SyncGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarmGold
import kotlinx.coroutines.launch

@OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class
)
@Composable
fun AiSearchScreen(
    messages: List<ChatMessage>,
    onSendMessage: (String) -> Unit,
    onBackClick: () -> Unit,
    onVoiceClick: () -> Unit = {},
    onAddMemoryClick: () -> Unit = {},
    onOpenCitation: (CitationSource) -> Unit = {},
    onUploadPdf: (Uri) -> Unit = {},
    onUploadImage: (Uri) -> Unit = {},
    onConfirmReminder: ((ChatMessage) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isDarkMode = LocalIsDarkMode.current
    var inputText by remember { mutableStateOf("") }
    var showAttachModal by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            onUploadPdf(uri)
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            onUploadImage(uri)
        }
    }

    val isImeVisible = WindowInsets.isImeVisible
    LaunchedEffect(isImeVisible, messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    AmbientLightingBackground {
        Box(
            modifier = modifier
                .fillMaxSize()
                .testTag("ai_search_screen")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                AiSearchTopBar(
                    onBackClick = onBackClick,
                    onAddMemoryClick = onAddMemoryClick
                )
            if (showAttachModal) {
                ModalBottomSheet(
                    onDismissRequest = { showAttachModal = false },
                    containerColor = Color(0xF51A1820),
                    dragHandle = {
                        Box(
                            modifier = Modifier
                                .padding(vertical = 10.dp)
                                .width(40.dp)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color(0x44FFFFFF))
                        )
                    }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                        Text(
                            text = "Attach Real File to Ask AI",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Upload a PDF or image to immediately analyze and display visual preview.",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 12.sp)
                        )
                        Spacer(modifier = Modifier.height(20.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(Color(0x22FF5252))
                                    .border(1.dp, PdfRed.copy(alpha = 0.6f), RoundedCornerShape(18.dp))
                                    .clickable {
                                        showAttachModal = false
                                        pdfPickerLauncher.launch(arrayOf("application/pdf"))
                                    }
                                    .padding(18.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Rounded.PictureAsPdf,
                                        contentDescription = "PDF",
                                        tint = PdfRed,
                                        modifier = Modifier.size(34.dp)
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "Attach PDF",
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                    )
                                    Text(
                                        text = "Invoice, Document",
                                        style = MaterialTheme.typography.bodySmall.copy(color = TextMuted, fontSize = 11.sp)
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(Color(0x22C084FC))
                                    .border(1.dp, ImagePurple.copy(alpha = 0.6f), RoundedCornerShape(18.dp))
                                    .clickable {
                                        showAttachModal = false
                                        imagePickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    }
                                    .padding(18.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Rounded.Image,
                                        contentDescription = "Image",
                                        tint = ImagePurple,
                                        modifier = Modifier.size(34.dp)
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "Attach Image",
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                    )
                                    Text(
                                        text = "Receipt, Scan",
                                        style = MaterialTheme.typography.bodySmall.copy(color = TextMuted, fontSize = 11.sp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                    if (messages.isEmpty()) {
                        item(key = "empty_chat_state") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                GlassCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("ai_search_empty_state"),
                                    shape = RoundedCornerShape(24.dp),
                                    ambientGlowColor = AmberWarm
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(28.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(54.dp)
                                                .clip(CircleShape)
                                                .background(Color(0x24FF9E58))
                                                .border(1.dp, AmberWarm.copy(alpha = 0.5f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.AutoAwesome,
                                                contentDescription = null,
                                                tint = AmberWarm,
                                                modifier = Modifier.size(26.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                            text = "Neural Memory Assistant",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = if (isDarkMode) TextPrimary else Color(0xFF111827),
                                                fontSize = 17.sp
                                            )
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "Ask anything about your saved memories. Synthesize notes, find key dates, or extract info privately.",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                color = if (isDarkMode) TextSecondary else Color(0xFF4B5563),
                                                fontSize = 13.sp,
                                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }

                    items(messages, key = { it.id }) { message ->
                        when (message.sender) {
                            MessageSender.USER -> {
                                UserChatBubble(
                                    text = message.text,
                                    timestamp = message.timestamp
                                )
                            }
                            MessageSender.AI -> {
                                AiResponseCard(
                                    message = message,
                                    onCitationClick = { citation ->
                                        onOpenCitation(citation)
                                    },
                                    onCopyClick = {
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar("Copied answer to clipboard 📋")
                                        }
                                    },
                                    onConfirmReminder = onConfirmReminder
                                )
                            }
                        }
                    }
                }

                // Docked Input Bar directly below messages flush on top of keyboard
                StickyBottomInputBar(
                    inputText = inputText,
                    onInputTextChange = { inputText = it },
                    onSendClick = {
                        if (inputText.isNotBlank()) {
                            onSendMessage(inputText.trim())
                            inputText = ""
                        }
                    },
                    onVoiceClick = onVoiceClick,
                    onAttachClick = { /* Attachment icon removed in favor of Voice Mic */ },
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 76.dp)
            ) {
                DarkFrostedSnackbarHost(snackbarHostState)
            }
        }
    }
}

@Composable
private fun AiSearchTopBar(
    onBackClick: () -> Unit,
    onAddMemoryClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isDarkMode = LocalIsDarkMode.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp)
            .testTag("ai_search_top_bar")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(if (isDarkMode) Color(0x22FFFFFF) else Color(0xDDFFFFFF))
                    .border(1.dp, if (isDarkMode) GlassStroke else Color(0xFFCBD5E1), CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(color = Color.White.copy(alpha = 0.3f)),
                        onClick = onBackClick
                    )
                    .testTag("back_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBackIos,
                    contentDescription = "Back to Workspace",
                    tint = if (isDarkMode) TextPrimary else Color(0xFF111827),
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "AI Search",
            style = MaterialTheme.typography.displayLarge.copy(
                fontWeight = FontWeight.ExtraBold,
                fontSize = 32.sp,
                letterSpacing = (-0.6).sp,
                fontFamily = FontFamily.SansSerif,
                color = if (isDarkMode) TextPrimary else Color(0xFF111827)
            ),
            modifier = Modifier.testTag("ai_search_title")
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = "Conversational search and document retrieval",
            style = MaterialTheme.typography.bodyMedium.copy(
                color = if (isDarkMode) TextSecondary else Color(0xFF4B5563),
                fontSize = 13.sp
            )
        )
    }
}

@Composable
private fun UserChatBubble(
    text: String,
    timestamp: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("user_chat_bubble_container"),
        horizontalAlignment = Alignment.End
    ) {
        Box(
            modifier = Modifier
                .clip(
                    RoundedCornerShape(
                        topStart = 20.dp,
                        topEnd = 20.dp,
                        bottomStart = 20.dp,
                        bottomEnd = 4.dp
                    )
                )
                .background(
                    Brush.linearGradient(
                        listOf(Color(0x55FF9E58), Color(0x35E8833A), Color(0x4038251A))
                    )
                )
                .border(
                    1.dp,
                    Brush.linearGradient(listOf(Color(0x80FFB87E), Color(0x30FFA767), Color(0x60FFB87E))),
                    RoundedCornerShape(
                        topStart = 20.dp,
                        topEnd = 20.dp,
                        bottomStart = 20.dp,
                        bottomEnd = 4.dp
                    )
                )
                .padding(horizontal = 18.dp, vertical = 14.dp)
                .testTag("user_chat_bubble")
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium.copy(
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 22.sp
                )
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = timestamp,
            style = MaterialTheme.typography.labelSmall.copy(
                color = TextMuted,
                fontSize = 10.sp
            ),
            modifier = Modifier.padding(end = 4.dp)
        )
    }
}

@Composable
private fun AiResponseCard(
    message: ChatMessage,
    onCitationClick: (CitationSource) -> Unit,
    onCopyClick: () -> Unit,
    onConfirmReminder: ((ChatMessage) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isDarkMode = LocalIsDarkMode.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("ai_response_card_container"),
        horizontalAlignment = Alignment.Start
    ) {
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("gsdcall_ai_response_card"),
            shape = RoundedCornerShape(26.dp),
            borderStrokeWidth = 1.dp,
            borderColorList = listOf(Color(0x66FFFFFF), Color(0x1EFFFFFF), Color(0x40FFFFFF)),
            ambientGlowColor = AmberWarm
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
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
                                .size(32.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Brush.radialGradient(listOf(AmberWarm, AmberGlow))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = Color(0xFF0F0E11),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Text(
                                text = "GTOOL X Assistant",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = if (isDarkMode) TextPrimary else Color(0xFF111827)
                                )
                            )
                            Text(
                                text = "Direct File & Memory Retrieval",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (isDarkMode) TextMuted else Color(0xFF6B7280),
                                    fontSize = 10.sp
                                )
                            )
                        }
                    }

                    IconButton(
                        onClick = onCopyClick,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ContentCopy,
                            contentDescription = "Copy text",
                            tint = if (isDarkMode) TextMuted else Color(0xFF4B5563),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = if (isDarkMode) TextPrimary else Color(0xFF1F2937),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Normal,
                        lineHeight = 23.sp,
                        fontFamily = FontFamily.SansSerif
                    ),
                    modifier = Modifier.testTag("ai_answer_text")
                )

                if (message.reminderTitle != null && message.reminderDate != null) {
                    Spacer(modifier = Modifier.height(14.dp))
                    if (message.isReminderSet) {
                        Button(
                            onClick = {},
                            enabled = false,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0x224CAF50),
                                disabledContainerColor = Color(0x224CAF50)
                            ),
                            border = BorderStroke(1.dp, Color(0xFF4CAF50)),
                            modifier = Modifier.testTag("reminder_set_chip")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Reminder Set",
                                color = Color(0xFF4CAF50),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Button(
                            onClick = {
                                onConfirmReminder?.invoke(message)
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6D00)),
                            modifier = Modifier.testTag("confirm_reminder_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Alarm,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            val sdf = java.text.SimpleDateFormat("dd MMM yyyy, h:mm a", java.util.Locale.getDefault())
                            val formattedTimeStr = sdf.format(java.util.Date(message.reminderDate))
                            Text(
                                text = "⏰ Set Reminder for $formattedTimeStr",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                if (message.citations.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(18.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "MATCHED FILES (TAP TO OPEN FULL VIEWER)",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = WarmGold,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp
                            )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(0.5.dp)
                                .background(if (isDarkMode) Color(0x22FFFFFF) else Color(0x22000000))
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.testTag("citation_cards_column")
                    ) {
                        message.citations.forEach { citation ->
                            FrostedCitationCard(
                                citation = citation,
                                onClick = { onCitationClick(citation) }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = message.timestamp,
            style = MaterialTheme.typography.labelSmall.copy(
                color = if (isDarkMode) TextMuted else Color(0xFF6B7280),
                fontSize = 10.sp
            ),
            modifier = Modifier.padding(start = 6.dp)
        )
    }
}

@Composable
private fun FrostedCitationCard(
    citation: CitationSource,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDarkMode = LocalIsDarkMode.current
    val context = LocalContext.current
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    
    val (typeColor, typeIcon) = when (citation.type) {
        ItemType.PDF -> Pair(PdfRed, Icons.Rounded.PictureAsPdf)
        ItemType.NOTE -> Pair(NoteYellow, Icons.Rounded.Description)
        ItemType.IMAGE -> Pair(ImagePurple, Icons.Rounded.Image)
        ItemType.VOICE -> Pair(AmberWarm, Icons.Rounded.Mic)
        ItemType.ID_CARD -> Pair(AmberWarm, Icons.Rounded.Badge)
        ItemType.SIGNATURE -> Pair(AmberWarm, Icons.Rounded.Draw)
        ItemType.ALL -> Pair(AmberWarm, Icons.Rounded.Description)
    }

    val previewUri = citation.previewThumbnailUri ?: citation.imageUri
    val hasVisualPreview = !previewUri.isNullOrBlank() && (citation.type == ItemType.PDF || citation.type == ItemType.IMAGE)
    var isCopied by remember { mutableStateOf(false) }

    LaunchedEffect(isCopied) {
        if (isCopied) {
            kotlinx.coroutines.delay(2000)
            isCopied = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(if (isDarkMode) Color(0x28FFFFFF) else Color(0xFFFFFFFF))
            .border(1.dp, if (isDarkMode) GlassStroke else Color(0xFFCBD5E1), RoundedCornerShape(22.dp))
            .padding(16.dp)
            .testTag("citation_card_${citation.id}")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header: Icon + File Name & Module Source
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
                            .size(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(typeColor.copy(alpha = 0.2f))
                            .border(1.dp, typeColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = typeIcon,
                            contentDescription = citation.type.label,
                            tint = typeColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = citation.fileName.ifBlank { "Untitled File" },
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = if (isDarkMode) TextPrimary else Color(0xFF111827)
                            ),
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = "Source: ${citation.moduleSourceLabel.ifBlank { "[Document]" }}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = WarmGold,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }

                // Mini Source Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0x15FFFFFF))
                        .border(0.5.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = citation.metaInfo.substringBefore(" •"),
                        color = TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 1. Exact Match Highlight & Quick Copy Box (Styled snippet card)
            val cleanValueToCopy = citation.extractedValue
                .removePrefix("Extracted Value: ")
                .removePrefix("Matched Line: ")
                .removePrefix("Extracted Line: ")
                .replace("**", "")
                .trim()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0x18FF9E58))
                    .border(1.dp, AmberWarm.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = citation.extractedValue.ifBlank { "Matched Item" },
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(cleanValueToCopy))
                            isCopied = true
                        },
                        modifier = Modifier
                            .height(32.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isCopied) Color(0x3300E5FF) else Color(0x22FFFFFF))
                            .border(0.5.dp, if (isCopied) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(
                                imageVector = if (isCopied) Icons.Rounded.Check else Icons.Rounded.ContentCopy,
                                contentDescription = "Quick Copy",
                                tint = if (isCopied) Color(0xFF00E5FF) else AmberWarm,
                                modifier = Modifier.size(14.dp)
                            )
                            if (isCopied) {
                                Text("Copied!", color = Color(0xFF00E5FF), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // 2. Exact Location & Context Preview
            if (citation.contextPreview.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x0AFFFFFF))
                        .padding(10.dp)
                ) {
                    Text(
                        text = "CONTEXT PREVIEW",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = TextMuted,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp,
                            letterSpacing = 0.5.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = citation.contextPreview,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextSecondary,
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    )
                }
            }

            // Real visual preview banner for uploaded image or PDF
            val isPdfFile = previewUri?.endsWith(".pdf", ignoreCase = true) == true
            if (hasVisualPreview && !isPdfFile) {
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x15000000))
                        .border(1.dp, typeColor.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = rememberAsyncImagePainter(model = previewUri),
                        contentDescription = "Real preview of ${citation.title}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Overlay badge
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xEE0F0E11))
                            .border(1.dp, typeColor.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = if (citation.type == ItemType.PDF) "📄 Real Uploaded PDF Preview" else "📷 Real Uploaded Image Preview",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = typeColor,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3. Document Deep Link Button
            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0x1A00E5FF),
                    contentColor = Color(0xFF00E5FF)
                ),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Color(0x4400E5FF)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
                    .testTag("citation_open_button_${citation.id}"),
                contentPadding = PaddingValues(0.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Visibility,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Open Full Document",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun StickyBottomInputBar(
    inputText: String,
    onInputTextChange: (String) -> Unit,
    onSendClick: () -> Unit,
    onVoiceClick: () -> Unit,
    onAttachClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDarkMode = LocalIsDarkMode.current
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current

    Box(
        modifier = modifier
            .testTag("sticky_bottom_input_container")
    ) {
        // iOS-Style Floating Pill Container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(Color(0xCC1A1D24))
                .border(1.dp, Color(0x26FFFFFF), RoundedCornerShape(28.dp))
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Center Field: Borderless Text Input
                Box(modifier = Modifier.weight(1f)) {
                    if (inputText.isEmpty()) {
                        Text(
                            text = "Search notes, PDFs, bills...",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = Color(0x80FFFFFF),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Normal
                            )
                        )
                    }

                    BasicTextField(
                        value = inputText,
                        onValueChange = onInputTextChange,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = Color.White,
                            fontSize = 15.sp,
                            fontFamily = FontFamily.SansSerif
                        ),
                        cursorBrush = SolidColor(Color(0xFFFF6D00)),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                if (inputText.isNotBlank()) {
                                    onSendClick()
                                    keyboardController?.hide()
                                }
                            }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("chat_input_field")
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Right Action: Send Button
                val sendEnabled = inputText.isNotBlank()
                IconButton(
                    onClick = onSendClick,
                    enabled = sendEnabled,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (sendEnabled) Color(0xFFFF6D00) else Color(0x14FFFFFF))
                        .testTag("send_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ArrowUpward,
                        contentDescription = "Send",
                        tint = if (sendEnabled) Color.White else Color.White.copy(alpha = 0.3f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
