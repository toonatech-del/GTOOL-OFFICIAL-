package com.example.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBackIos
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CreditCard
import com.example.util.DocumentSummaryParser
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.ZoomIn
import androidx.compose.material.icons.rounded.ZoomOut
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.ui.components.AmbientLightingBackground
import com.example.ui.components.DarkFrostedSnackbarHost
import com.example.ui.components.GlassCard
import androidx.compose.material.icons.rounded.Warning
import com.example.ui.theme.AmberGlow
import com.example.ui.theme.AmberWarm
import com.example.ui.theme.GlassStroke
import com.example.ui.theme.LocalIsDarkMode
import com.example.ui.theme.SyncGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarmGold
import com.example.util.MlKitTextExtractor
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageMemoryScreen(
    imageUri: String? = null,
    onBackClick: () -> Unit,
    onSaveExtractedMemory: (docName: String, amount: String, dueDate: String, includeInSearch: Boolean, savedUri: String?, fullOcrText: String) -> Unit = { _, _, _, _, _, _ -> },
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDarkMode = LocalIsDarkMode.current
    val clipboardManager = LocalClipboardManager.current

    var currentImageUri by remember(imageUri) { mutableStateOf(imageUri) }
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Editable OCR structured data points
    var docName by remember(imageUri) { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var dueDateText by remember { mutableStateOf("") }
    var extractedFullOcrText by remember { mutableStateOf("") }
    var isOcrExtracting by remember { mutableStateOf(false) }

    // Interactive Photo Viewer zoom and pan
    var zoomScale by remember { mutableFloatStateOf(1f) }
    var panOffsetX by remember { mutableFloatStateOf(0f) }
    var panOffsetY by remember { mutableFloatStateOf(0f) }

    val showSourcePickerSheet = false
    var showFullPhotoModal by remember { mutableStateOf(false) }
    var fieldToEdit by remember { mutableStateOf<Pair<String, String>?>(null) }
    var includeInSemanticSearch by remember { mutableStateOf(true) }
    var detectedExpiryDate by remember { mutableStateOf<String?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Activity result launcher for Gallery Photo Picking
    val galleryPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            currentImageUri = uri.toString()
            capturedBitmap = null
            zoomScale = 1f
            panOffsetX = 0f
            panOffsetY = 0f
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Image selected from gallery 🖼️")
            }
        }
    }

    // Run real on-device Google ML Kit Text Recognition on the image
    LaunchedEffect(currentImageUri, capturedBitmap) {
        if (capturedBitmap != null) {
            isOcrExtracting = true
            try {
                val recognizedText = MlKitTextExtractor.extractTextFromBitmap(capturedBitmap!!)
                if (recognizedText.isNotBlank()) {
                    extractedFullOcrText = recognizedText
                    val lines = recognizedText.lines().map { it.trim() }.filter { it.isNotBlank() }
                    if (lines.isNotEmpty() && docName.isBlank()) {
                        docName = lines.first().take(40)
                    }
                    val amountLine = lines.firstOrNull { it.contains("₹") || it.contains("$") || it.contains("total", ignoreCase = true) || it.contains("amount", ignoreCase = true) }
                    if (amountLine != null && amountText.isBlank()) {
                        amountText = amountLine
                    }
                    val dateLine = lines.firstOrNull { it.contains("date", ignoreCase = true) || it.contains("due", ignoreCase = true) }
                    if (dateLine != null && dueDateText.isBlank()) {
                        dueDateText = dateLine
                    }
                    
                    // Detect Expiry Date
                    detectedExpiryDate = DocumentSummaryParser.extractExpiryDate(recognizedText)
                }
            } catch (_: Exception) {
            } finally {
                isOcrExtracting = false
            }
        } else if (!currentImageUri.isNullOrBlank()) {
            isOcrExtracting = true
            try {
                val recognizedText = MlKitTextExtractor.extractTextFromUri(context, Uri.parse(currentImageUri))
                if (recognizedText.isNotBlank()) {
                    extractedFullOcrText = recognizedText
                    val lines = recognizedText.lines().map { it.trim() }.filter { it.isNotBlank() }
                    if (lines.isNotEmpty() && docName.isBlank()) {
                        docName = lines.first().take(40)
                    }
                    val amountLine = lines.firstOrNull { it.contains("₹") || it.contains("$") || it.contains("total", ignoreCase = true) || it.contains("amount", ignoreCase = true) }
                    if (amountLine != null && amountText.isBlank()) {
                        amountText = amountLine
                    }
                    val dateLine = lines.firstOrNull { it.contains("date", ignoreCase = true) || it.contains("due", ignoreCase = true) }
                    if (dateLine != null && dueDateText.isBlank()) {
                        dueDateText = dateLine
                    }
                    
                    // Detect Expiry Date
                    detectedExpiryDate = DocumentSummaryParser.extractExpiryDate(recognizedText)
                }
            } catch (_: Exception) {
            } finally {
                isOcrExtracting = false
            }
        }
    }

    androidx.activity.compose.BackHandler {
        onBackClick()
    }

    AmbientLightingBackground {
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { DarkFrostedSnackbarHost(snackbarHostState) },
            modifier = modifier
                .fillMaxSize()
                .testTag("image_memory_screen"),
            topBar = {
                ImageMemoryTopBar(
                    onBackClick = onBackClick,
                    onSaveClick = {
                        val finalTitle = if (docName.isNotBlank()) docName else "Captured Image Memory"
                        onSaveExtractedMemory(
                            finalTitle,
                            amountText,
                            dueDateText,
                            includeInSemanticSearch,
                            currentImageUri,
                            extractedFullOcrText
                        )
                    }
                )
            },
            bottomBar = {
                // Bottom Save Memory Button matching reference screenshot
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .clip(RoundedCornerShape(28.dp))
                            .background(Color(0x3500E5FF))
                            .border(
                                1.5.dp,
                                Brush.horizontalGradient(
                                    listOf(Color(0xFF80EEFF), Color(0xFF5CE1E6))
                                ),
                                RoundedCornerShape(28.dp)
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(color = Color.White.copy(alpha = 0.4f)),
                                onClick = {
                                    val finalTitle = if (docName.isNotBlank()) docName else "Captured Image Memory"
                                    onSaveExtractedMemory(
                                        finalTitle,
                                        amountText,
                                        dueDateText,
                                        includeInSemanticSearch,
                                        currentImageUri,
                                        extractedFullOcrText
                                    )
                                }
                            )
                            .testTag("save_extracted_memory_button")
                            .testTag("save_memory_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF00E5FF)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = Color(0xFF0F0E11),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Save Memory",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp,
                                    letterSpacing = 0.2.sp
                                ),
                                modifier = Modifier.testTag("save_extracted_memory_text")
                            )
                        }
                    }
                }
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(
                    start = 20.dp,
                    end = 20.dp,
                    top = 4.dp,
                    bottom = 24.dp
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Expiry Alert Banner
                detectedExpiryDate?.let { date ->
                    item(key = "expiry_alert_banner") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0x30FF9500))
                                .border(
                                    1.dp,
                                    Brush.linearGradient(listOf(Color(0xFFFF9500), Color(0xFFFFCC00))),
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                                .testTag("expiry_alert_banner")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(
                                        imageVector = Icons.Rounded.Warning,
                                        contentDescription = null,
                                        tint = Color(0xFFFF9500),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Expiry Detected: $date",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    )
                                }
                                
                                Text(
                                    text = "Set Reminder",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        color = Color(0xFFFFCC00),
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 12.sp
                                    ),
                                    modifier = Modifier
                                        .clickable { 
                                            val expiryLong = DocumentSummaryParser.parseDateToLong(date)
                                            if (expiryLong != null) {
                                                com.example.util.ReminderManager.scheduleExpiryReminders(
                                                    context,
                                                    docName,
                                                    expiryLong
                                                )
                                                coroutineScope.launch {
                                                    snackbarHostState.showSnackbar("Reminders set for 7 days and 1 day before $date! 🔔")
                                                }
                                            }
                                        }
                                        .padding(start = 8.dp)
                                )
                            }
                        }
                    }
                }
                // Header Title matching screenshot
                item(key = "title_section") {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Image Memories",
                            style = MaterialTheme.typography.displayMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 32.sp,
                                letterSpacing = (-0.5).sp,
                                color = Color.White
                            ),
                            modifier = Modifier.testTag("image_memories_title")
                        )
                    }
                }

                // Interactive Photo Viewer Card / Dropzone matching screenshot
                item(key = "utility_bill_photo_card") {
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("electricity_bill_photo_card"),
                        shape = RoundedCornerShape(26.dp),
                        borderStrokeWidth = 1.5.dp,
                        borderColorList = listOf(Color(0x80FFFFFF), Color(0x25FFFFFF), Color(0x40FFFFFF)),
                        ambientGlowColor = AmberWarm
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            val hasImage = currentImageUri != null || capturedBitmap != null

                            if (hasImage) {
                                // Viewer Controls Bar
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    // Change Photo Button
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color(0x24FFFFFF))
                                            .clickable { galleryPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                            .testTag("change_photo_button")
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Rounded.PhotoCamera,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Change Photo",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp
                                                )
                                            )
                                        }
                                    }

                                    // Zoom Controls
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(30.dp)
                                                .clip(CircleShape)
                                                .background(Color(0x20FFFFFF))
                                                .clickable {
                                                    zoomScale = (zoomScale - 0.25f).coerceAtLeast(0.8f)
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.ZoomOut,
                                                contentDescription = "Zoom Out",
                                                tint = Color.White,
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }

                                        Text(
                                            text = "${(zoomScale * 100).toInt()}%",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = Color.White.copy(alpha = 0.8f),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        )

                                        Box(
                                            modifier = Modifier
                                                .size(30.dp)
                                                .clip(CircleShape)
                                                .background(Color(0x20FFFFFF))
                                                .clickable {
                                                    zoomScale = (zoomScale + 0.25f).coerceAtMost(3.5f)
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.ZoomIn,
                                                contentDescription = "Zoom In",
                                                tint = Color.White,
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }

                                        Box(
                                            modifier = Modifier
                                                .size(30.dp)
                                                .clip(CircleShape)
                                                .background(Color(0x20FFFFFF))
                                                .clickable {
                                                    zoomScale = 1f
                                                    panOffsetX = 0f
                                                    panOffsetY = 0f
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.RestartAlt,
                                                contentDescription = "Reset Zoom",
                                                tint = Color.White,
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }

                                        Box(
                                            modifier = Modifier
                                                .size(30.dp)
                                                .clip(CircleShape)
                                                .background(Color(0x20FFFFFF))
                                                .clickable { showFullPhotoModal = true },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Fullscreen,
                                                contentDescription = "Fullscreen",
                                                tint = Color.White,
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Interactive Viewport with pinch-to-zoom & pan
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(280.dp)
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(Color(0xFF141217))
                                        .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(18.dp))
                                        .pointerInput(Unit) {
                                            detectTransformGestures { _, pan, zoom, _ ->
                                                zoomScale = (zoomScale * zoom).coerceIn(0.8f, 4.0f)
                                                panOffsetX += pan.x
                                                panOffsetY += pan.y
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (capturedBitmap != null) {
                                        Image(
                                            bitmap = capturedBitmap!!.asImageBitmap(),
                                            contentDescription = "Captured Photo",
                                            contentScale = ContentScale.Fit,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .graphicsLayer {
                                                    scaleX = zoomScale
                                                    scaleY = zoomScale
                                                    translationX = panOffsetX
                                                    translationY = panOffsetY
                                                }
                                        )
                                    } else if (!currentImageUri.isNullOrBlank()) {
                                        AsyncImage(
                                            model = currentImageUri,
                                            contentDescription = "Selected Document Photo",
                                            contentScale = ContentScale.Fit,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .graphicsLayer {
                                                    scaleX = zoomScale
                                                    scaleY = zoomScale
                                                    translationX = panOffsetX
                                                    translationY = panOffsetY
                                                }
                                        )
                                    }
                                }
                            } else {
                                // Responsive Clean dropzone matching upload image
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(190.dp)
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(Color(0x1EFFFFFF))
                                        .border(1.5.dp, Color(0x40FFFFFF), RoundedCornerShape(20.dp))
                                        .clickable { galleryPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
                                        .padding(16.dp)
                                        .testTag("tap_to_capture_container"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(52.dp)
                                                .clip(CircleShape)
                                                .background(Color(0x28FFFFFF))
                                                .border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.PhotoLibrary,
                                                contentDescription = "Tap to upload from Gallery",
                                                tint = Color.White,
                                                modifier = Modifier.size(26.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))

                                        Text(
                                            text = "Tap to upload from Gallery",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 17.sp
                                            ),
                                            modifier = Modifier.testTag("tap_to_capture_text")
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Title Input Field matching screenshot
                item(key = "title_input_section") {
                    Text(
                        text = "DOCUMENT TITLE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color(0xB8C2CCFF),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            letterSpacing = 0.8.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(22.dp))
                            .background(Color(0x1CFFFFFF))
                            .border(1.dp, Color(0x35FFFFFF), RoundedCornerShape(22.dp))
                            .padding(horizontal = 18.dp, vertical = 14.dp)
                    ) {
                        if (docName.isEmpty()) {
                            Text(
                                text = "Document Title (e.g., Electricity Bill, Hospital Invoice)",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = Color(0x80FFFFFF),
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 14.sp
                                )
                            )
                        }
                        BasicTextField(
                            value = docName,
                            onValueChange = { docName = it },
                            textStyle = MaterialTheme.typography.titleMedium.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            ),
                            cursorBrush = SolidColor(Color(0xFF00E5FF)),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("image_memory_title_input")
                        )
                    }
                }

                // AI OCR Extraction Card matching screenshot
                item(key = "ai_ocr_extraction_card") {
                    Text(
                        text = "AI OCR EXTRACTION & METADATA",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color(0xB8C2CCFF),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            letterSpacing = 0.8.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    AiOcrExtractionCard(
                        docName = docName,
                        amount = amountText,
                        dueDate = dueDateText,
                        onEditDocName = { fieldToEdit = Pair("Document Name", docName) },
                        onEditAmount = { fieldToEdit = Pair("Amount", amountText) },
                        onEditDueDate = { fieldToEdit = Pair("Due Date", dueDateText) }
                    )
                }

                // Full OCR Raw Text Card (if extracted)
                if (isOcrExtracting) {
                    item(key = "ocr_loading_card") {
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            borderColorList = listOf(Color(0x60FFFFFF), Color(0x20FFFFFF)),
                            ambientGlowColor = AmberWarm
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = AmberWarm,
                                    strokeWidth = 2.5.dp
                                )
                                Spacer(modifier = Modifier.width(14.dp))
                                Column {
                                    Text(
                                        text = "Extracting OCR text...",
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDarkMode) TextPrimary else Color(0xFF111827)
                                        )
                                    )
                                    Text(
                                        text = "Capturing 100% of all blocks, words, numbers & symbols",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = if (isDarkMode) TextSecondary else Color(0xFF6B7280),
                                            fontSize = 11.sp
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                if (extractedFullOcrText.isNotBlank()) {
                    item(key = "ocr_full_text_card") {
                        val wordCount = extractedFullOcrText.split(Regex("""\s+""")).count { it.isNotBlank() }
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            borderColorList = listOf(Color(0x60FFFFFF), Color(0x20FFFFFF)),
                            ambientGlowColor = AmberWarm
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Rounded.ReceiptLong,
                                            contentDescription = null,
                                            tint = AmberWarm,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Full Extracted OCR Text",
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = if (isDarkMode) TextPrimary else Color(0xFF111827)
                                            )
                                        )
                                    }

                                    // Quick Copy Extracted Text Button
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0x28FF9E58))
                                            .border(1.dp, AmberWarm.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                            .clickable {
                                                if (extractedFullOcrText.isNotBlank()) {
                                                    clipboardManager.setText(AnnotatedString(extractedFullOcrText))
                                                    coroutineScope.launch {
                                                        snackbarHostState.showSnackbar("Copied extracted OCR text to clipboard! 📋")
                                                    }
                                                }
                                            }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                            .testTag("copy_extracted_ocr_text_button")
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Rounded.ContentCopy,
                                                contentDescription = "Copy OCR Text",
                                                tint = WarmGold,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "Copy Text",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = WarmGold,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isDarkMode) Color(0x22FFFFFF) else Color(0x15000000))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "$wordCount words • 100% Extracted",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = if (isDarkMode) TextSecondary else Color(0xFF4B5563),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Expandable, scrollable card
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 100.dp, max = 280.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isDarkMode) Color(0x18FFFFFF) else Color(0x10000000))
                                        .border(1.dp, if (isDarkMode) Color(0x25FFFFFF) else Color(0x20000000), RoundedCornerShape(12.dp))
                                        .padding(12.dp)
                                        .verticalScroll(rememberScrollState())
                                ) {
                                    Text(
                                        text = extractedFullOcrText,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = if (isDarkMode) TextSecondary else Color(0xFF374151),
                                            fontSize = 12.sp,
                                            lineHeight = 18.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                // AI Semantic Search Toggle
                item(key = "semantic_search_toggle") {
                    SemanticSearchToggleCard(
                        checked = includeInSemanticSearch,
                        onCheckedChange = { includeInSemanticSearch = it }
                    )
                }
            }
        }
    }

    // Modal Bottom Sheet to choose between Camera or Gallery
    if (false && showSourcePickerSheet) {
        ModalBottomSheet(
            onDismissRequest = { /* showSourcePickerSheet = false */ },
            sheetState = rememberModalBottomSheetState(),
            containerColor = if (isDarkMode) Color(0xFF1B1822) else Color(0xFFFFFFFF),
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(vertical = 10.dp)
                        .size(width = 36.dp, height = 4.dp)
                        .clip(CircleShape)
                        .background(if (isDarkMode) Color(0x44FFFFFF) else Color(0x33000000))
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Select Document Photo",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = if (isDarkMode) TextPrimary else Color(0xFF111827)
                    )
                )

                // Option 1: Take Photo with Camera
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isDarkMode) Color(0x22FFFFFF) else Color(0x10000000))
                        .border(1.dp, GlassStroke, RoundedCornerShape(16.dp))
                        .clickable {
                            // showSourcePickerSheet = false
                            // cameraCaptureLauncher.launch(null)
                        }
                        .padding(16.dp)
                        .testTag("pick_camera_option")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0x28FF9E58)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CameraAlt,
                                contentDescription = "Camera",
                                tint = AmberWarm,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "Take Photo with Camera",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = if (isDarkMode) TextPrimary else Color(0xFF111827)
                                )
                            )
                            Text(
                                text = "Capture physical document, receipt, or invoice",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = if (isDarkMode) TextSecondary else Color(0xFF6B7280),
                                    fontSize = 12.sp
                                )
                            )
                        }
                    }
                }

                // Option 2: Choose from Gallery
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isDarkMode) Color(0x22FFFFFF) else Color(0x10000000))
                        .border(1.dp, GlassStroke, RoundedCornerShape(16.dp))
                        .clickable {
                            /* showSourcePickerSheet = false */
                            galleryPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                        .padding(16.dp)
                        .testTag("pick_gallery_option")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0x2838BDF8)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PhotoLibrary,
                                contentDescription = "Gallery",
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "Choose from Gallery",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = if (isDarkMode) TextPrimary else Color(0xFF111827)
                                )
                            )
                            Text(
                                text = "Pick photo or scan from device storage",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = if (isDarkMode) TextSecondary else Color(0xFF6B7280),
                                    fontSize = 12.sp
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }

    // Inline Field Edit Dialog
    if (fieldToEdit != null) {
        val (fieldName, currentValue) = fieldToEdit!!
        EditDataPointDialog(
            label = fieldName,
            initialValue = currentValue,
            onDismiss = { fieldToEdit = null },
            onSave = { updated ->
                when (fieldName) {
                    "Document Name" -> docName = updated
                    "Amount" -> amountText = updated
                    "Due Date" -> dueDateText = updated
                }
                fieldToEdit = null
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Updated $fieldName to \"$updated\"")
                }
            }
        )
    }

    // Full photo modal
    if (showFullPhotoModal) {
        FullPhotoModal(
            imageUri = currentImageUri,
            bitmap = capturedBitmap,
            docName = docName,
            onDismiss = { showFullPhotoModal = false }
        )
    }
}

@Composable
private fun ImageMemoryTopBar(
    onBackClick: () -> Unit,
    onSaveClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(Color(0x28FFFFFF))
                .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(color = Color.White.copy(alpha = 0.3f)),
                    onClick = onBackClick
                )
                .padding(horizontal = 14.dp, vertical = 8.dp)
                .testTag("image_memory_back_button"),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBackIos,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Back",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                )
            }
        }
    }
}

@Composable
private fun AiOcrExtractionCard(
    docName: String,
    amount: String,
    dueDate: String,
    onEditDocName: () -> Unit,
    onEditAmount: () -> Unit,
    onEditDueDate: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDarkMode = LocalIsDarkMode.current
    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("ai_ocr_extraction_card"),
        shape = RoundedCornerShape(24.dp),
        borderStrokeWidth = 1.dp,
        borderColorList = listOf(Color(0x90FFFFFF), Color(0x25FFFFFF), Color(0x45FFFFFF)),
        ambientGlowColor = AmberWarm
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0x26FF9E58)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = AmberWarm,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Text(
                        text = "EXTRACTED METADATA",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = WarmGold,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            letterSpacing = 0.8.sp
                        )
                    )
                }

                val hasData = docName.isNotBlank() || amount.isNotBlank() || dueDate.isNotBlank()
                if (hasData) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isDarkMode) Color(0x18FFFFFF) else Color(0x12000000))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "OCR Fields Ready",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (isDarkMode) TextSecondary else Color(0xFF374151),
                                fontSize = 10.sp
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (docName.isBlank() && amount.isBlank() && dueDate.isBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No structured text detected yet. Tap an image or photo above.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = if (isDarkMode) TextMuted else Color(0xFF6B7280),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            } else {
                if (docName.isNotBlank()) {
                    StructuredDataRow(
                        fieldLabel = "DOCUMENT TYPE / TITLE",
                        valueText = docName,
                        icon = Icons.Rounded.ReceiptLong,
                        accentColor = AmberWarm,
                        onEditClick = onEditDocName,
                        tagId = "edit_doc_name"
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                if (amount.isNotBlank()) {
                    StructuredDataRow(
                        fieldLabel = "AMOUNT DUE",
                        valueText = if (amount.startsWith("Amount:")) amount else "Amount: $amount",
                        icon = Icons.Rounded.CreditCard,
                        accentColor = WarmGold,
                        onEditClick = onEditAmount,
                        tagId = "edit_amount"
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                if (dueDate.isNotBlank()) {
                    StructuredDataRow(
                        fieldLabel = "PAYMENT DUE DATE",
                        valueText = if (dueDate.startsWith("Due Date:")) dueDate else "Due Date: $dueDate",
                        icon = Icons.Rounded.DateRange,
                        accentColor = Color(0xFF38BDF8),
                        onEditClick = onEditDueDate,
                        tagId = "edit_due_date"
                    )
                }
            }
        }
    }
}

@Composable
private fun StructuredDataRow(
    fieldLabel: String,
    valueText: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    onEditClick: () -> Unit,
    tagId: String
) {
    val isDarkMode = LocalIsDarkMode.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (isDarkMode) Color(0x18FFFFFF) else Color(0x10000000))
            .border(1.dp, if (isDarkMode) GlassStroke else Color(0xFFCBD5E1), RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp)
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
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(accentColor.copy(alpha = 0.15f))
                        .border(1.dp, accentColor.copy(alpha = 0.3f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = fieldLabel,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (isDarkMode) TextMuted else Color(0xFF6B7280),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.6.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = valueText,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            color = if (isDarkMode) TextPrimary else Color(0xFF111827),
                            fontFamily = FontFamily.SansSerif
                        ),
                        modifier = Modifier.testTag("text_$tagId")
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(if (isDarkMode) Color(0x1AFFFFFF) else Color(0x18000000))
                    .border(1.dp, if (isDarkMode) GlassStroke else Color(0xFFCBD5E1), CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(color = AmberWarm.copy(alpha = 0.4f)),
                        onClick = onEditClick
                    )
                    .testTag(tagId),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Edit,
                    contentDescription = "Edit $fieldLabel",
                    tint = AmberWarm,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun SemanticSearchToggleCard(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDarkMode = LocalIsDarkMode.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(if (isDarkMode) Color(0x18FFFFFF) else Color(0x10000000))
            .border(1.dp, if (isDarkMode) GlassStroke else Color(0xFFCBD5E1), RoundedCornerShape(20.dp))
            .padding(16.dp)
            .testTag("semantic_search_toggle_card")
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
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0x26FF9E58)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null,
                        tint = AmberWarm,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "Include in AI Semantic Search",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (isDarkMode) TextPrimary else Color(0xFF111827)
                        ),
                        modifier = Modifier.testTag("semantic_search_label")
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Index text for natural search like 'Find my receipt'",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = if (isDarkMode) TextSecondary else Color(0xFF4B5563),
                            fontSize = 11.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color(0xFF0F0E11),
                    checkedTrackColor = AmberWarm,
                    uncheckedThumbColor = Color(0xFF888888),
                    uncheckedTrackColor = Color(0x33FFFFFF)
                ),
                modifier = Modifier.testTag("semantic_search_switch")
            )
        }
    }
}

@Composable
private fun EditDataPointDialog(
    label: String,
    initialValue: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    val isDarkMode = LocalIsDarkMode.current
    val cleanInitial = initialValue.removePrefix("Amount: ").removePrefix("Due Date: ")
    var textValue by remember { mutableStateOf(cleanInitial) }

    Dialog(onDismissRequest = onDismiss) {
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("edit_data_point_dialog"),
            shape = RoundedCornerShape(24.dp),
            borderColorList = listOf(Color(0x90FFFFFF), Color(0x30FFFFFF)),
            ambientGlowColor = AmberWarm
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp)
            ) {
                Text(
                    text = "Edit $label",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = if (isDarkMode) TextPrimary else Color(0xFF111827)
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = textValue,
                    onValueChange = { textValue = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_field_input"),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = if (isDarkMode) TextPrimary else Color(0xFF111827),
                        unfocusedTextColor = if (isDarkMode) TextPrimary else Color(0xFF111827),
                        focusedBorderColor = AmberWarm,
                        unfocusedBorderColor = if (isDarkMode) Color(0x40FFFFFF) else Color(0xFFCBD5E1),
                        cursorColor = AmberWarm
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDarkMode) Color(0x20FFFFFF) else Color(0x18000000),
                            contentColor = if (isDarkMode) TextPrimary else Color(0xFF111827)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            if (textValue.isNotBlank()) {
                                onSave(textValue.trim())
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AmberWarm,
                            contentColor = Color(0xFF0F0E11)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Save", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun FullPhotoModal(
    imageUri: String? = null,
    bitmap: Bitmap? = null,
    docName: String? = null,
    onDismiss: () -> Unit
) {
    val isDarkMode = LocalIsDarkMode.current
    Dialog(onDismissRequest = onDismiss) {
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("full_photo_modal"),
            shape = RoundedCornerShape(26.dp),
            borderColorList = listOf(Color(0x90FFFFFF), Color(0x30FFFFFF)),
            ambientGlowColor = AmberWarm
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (!docName.isNullOrBlank()) docName else "Document Photo Preview",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isDarkMode) TextPrimary else Color(0xFF111827),
                            fontSize = 15.sp
                        )
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF141217))
                        .border(1.dp, Color(0x33FFB87E), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Full photo preview",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else if (!imageUri.isNullOrBlank()) {
                        AsyncImage(
                            model = imageUri,
                            contentDescription = "Full photo preview",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Rounded.Image,
                                contentDescription = null,
                                tint = AmberWarm,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "NO IMAGE SOURCE",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = WarmGold,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Brush.linearGradient(listOf(AmberWarm, AmberGlow)))
                        .clickable(onClick = onDismiss)
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Close Preview",
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = Color(0xFF0F0E11),
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }
    }
}
