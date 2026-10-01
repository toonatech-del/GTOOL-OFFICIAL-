package com.example.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBackIos
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.material.icons.rounded.FitScreen
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Summarize
import androidx.compose.material.icons.rounded.ZoomIn
import androidx.compose.material.icons.rounded.ZoomOut
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.AmbientLightingBackground
import com.example.ui.components.GlassCard
import androidx.compose.material.icons.rounded.Warning
import com.example.ui.theme.AmberGlow
import com.example.ui.theme.AmberWarm
import com.example.ui.theme.GlassStroke
import com.example.ui.theme.LocalIsDarkMode
import com.example.ui.theme.PdfRed
import com.example.ui.theme.SyncGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarmGold
import com.example.util.DocumentSummaryParser
import com.example.util.PdfPageRenderer
import com.example.util.PdfTextExtractor
import kotlinx.coroutines.launch

enum class PdfViewMode {
    VISUAL_DOCUMENT,
    SUMMARY_METADATA
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfPreviewScreen(
    fileName: String,
    fileSize: String,
    pdfUri: String? = null,
    onBackClick: () -> Unit,
    onSaveDocument: (title: String, summary: String, keyPoints: List<String>, includeInSearch: Boolean, fullTranscript: String) -> Unit,
    modifier: Modifier = Modifier
) {
    com.example.util.PreventScreenCapture()

    val context = LocalContext.current
    val isDarkMode = LocalIsDarkMode.current
    val clipboardManager = LocalClipboardManager.current

    var documentTitle by remember(fileName) {
        val initial = if (fileName.isNotBlank()) fileName.removeSuffix(".pdf") else "Imported Document"
        mutableStateOf(initial)
    }

    var detectedExpiryDate by remember { mutableStateOf<String?>(null) }

    var summaryText by remember { mutableStateOf("") }
    var fullExtractedTranscript by remember { mutableStateOf("") }
    val keyPoints = remember { mutableStateListOf<String>() }
    var includeInSemanticSearch by remember { mutableStateOf(true) }

    // Multi-page Extraction and Progress State
    var isExtracting by remember { mutableStateOf(false) }
    var extractionProgress by remember { mutableStateOf("Ready") }
    var totalWords by remember { mutableIntStateOf(0) }
    var totalPagesExtracted by remember { mutableIntStateOf(0) }

    // PDF Pages Rendering State
    var pdfPages by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    var isRenderingPages by remember { mutableStateOf(true) }
    var selectedPageIndex by remember { mutableIntStateOf(0) }
    var activeViewMode by remember { mutableStateOf(PdfViewMode.VISUAL_DOCUMENT) }

    // Interactive Zoom and Pan State
    var zoomScale by remember { mutableFloatStateOf(1f) }
    var panOffsetX by remember { mutableFloatStateOf(0f) }
    var panOffsetY by remember { mutableFloatStateOf(0f) }

    var newPointText by remember { mutableStateOf("") }
    var isAddingPoint by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Render PDF Pages into high-res bitmaps
    LaunchedEffect(pdfUri) {
        isRenderingPages = true
        if (!pdfUri.isNullOrBlank()) {
            try {
                val uri = Uri.parse(pdfUri)
                val rendered = PdfPageRenderer.renderPdfPages(context, uri, maxPages = 15)
                pdfPages = rendered
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isRenderingPages = false
            }
        } else {
            isRenderingPages = false
        }
    }

    // Extract on-device PDF text from all pages (1 to N) via PdfRenderer + ML Kit and parse full rich summary
    LaunchedEffect(pdfUri, fileName, fileSize) {
        if (!pdfUri.isNullOrBlank()) {
            try {
                isExtracting = true
                extractionProgress = "Scanning PDF pages..."
                val result = PdfTextExtractor.extractTextFromAllPages(context, Uri.parse(pdfUri)) { current, total ->
                    extractionProgress = "Extracting page $current of $total..."
                }
                fullExtractedTranscript = result.fullTranscript
                totalWords = result.totalWords
                totalPagesExtracted = result.pageCount

                if (result.fullTranscript.isNotBlank()) {
                    val parsed = DocumentSummaryParser.parse(result.fullTranscript, fileName)
                    summaryText = parsed.fullSummaryText
                    keyPoints.clear()
                    
                    // Generate real key bullet points by splitting paragraphs/sentences across all pages
                    val lines = result.fullTranscript.lines()
                        .map { it.trim() }
                        .filter { it.length > 20 && !it.startsWith("--- Page") }
                        .take(5)
                    if (lines.isNotEmpty()) {
                        keyPoints.addAll(lines)
                    } else if (parsed.headings.isNotEmpty()) {
                        keyPoints.addAll(parsed.headings)
                    } else {
                        keyPoints.add("Document successfully indexed (${result.fullTranscript.take(150)}...)")
                    }

                    // Detect Expiry Date across all pages
                    detectedExpiryDate = DocumentSummaryParser.extractExpiryDate(result.fullTranscript)
                } else {
                    summaryText = "PDF document ($fileName) saved for visual reading, search, and offline retrieval."
                    keyPoints.clear()
                    keyPoints.add("No readable text found in document.")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                if (summaryText.isBlank()) {
                    summaryText = "PDF document ($fileName) saved for visual reading, search, and offline retrieval."
                }
            } finally {
                isExtracting = false
            }
        } else {
            isExtracting = false
            if (summaryText.isBlank()) {
                summaryText = "PDF document ($fileName) saved for visual reading, search, and offline retrieval."
            }
        }
    }

    AmbientLightingBackground {
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { DarkFrostedSnackbarHost(snackbarHostState) },
            modifier = modifier
                .fillMaxSize()
                .testTag("pdf_preview_screen"),
            topBar = {
                PdfPreviewTopBar()
            },
            bottomBar = {
                // Bottom Save Document Button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    val isSaveEnabled = !isExtracting
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .clip(RoundedCornerShape(27.dp))
                            .background(
                                if (isSaveEnabled) {
                                    Brush.horizontalGradient(
                                        listOf(AmberWarm, AmberGlow)
                                    )
                                } else {
                                    Brush.horizontalGradient(
                                        listOf(Color(0xFF374151), Color(0xFF1F2937))
                                    )
                                }
                            )
                            .border(
                                1.dp,
                                if (isSaveEnabled) Color.White.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.15f),
                                RoundedCornerShape(27.dp)
                            )
                            .clickable(
                                enabled = isSaveEnabled,
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(color = Color.White.copy(alpha = 0.4f)),
                                onClick = {
                                    val finalTitle = if (documentTitle.isNotBlank()) documentTitle.trim() else fileName
                                    onSaveDocument(
                                        finalTitle,
                                        summaryText,
                                        keyPoints.toList(),
                                        includeInSemanticSearch,
                                        fullExtractedTranscript
                                    )
                                }
                            )
                            .testTag("save_pdf_document_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isExtracting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = AmberWarm,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = extractionProgress,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        color = Color.White.copy(alpha = 0.85f),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        letterSpacing = 0.2.sp
                                    ),
                                    modifier = Modifier.testTag("save_pdf_document_text")
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF0F0E11),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Save Document",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        color = Color(0xFF0F0E11),
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 16.sp,
                                        letterSpacing = 0.2.sp
                                    ),
                                    modifier = Modifier.testTag("save_pdf_document_text")
                                )
                            }
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
                    top = 8.dp,
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
                                        text = "Document Expiry Detected: $date",
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
                                                    documentTitle,
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

                // Header badge & title
                item(key = "pdf_header_badge") {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0x28FF9E58))
                                    .border(
                                        1.dp,
                                        Brush.linearGradient(
                                            listOf(AmberWarm.copy(alpha = 0.6f), Color(0x30FFA767))
                                        ),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(SyncGreen)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (pdfPages.isNotEmpty()) "PDF Viewer (${pdfPages.size} ${if (pdfPages.size == 1) "Page" else "Pages"})" else "PDF Document",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = WarmGold,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                            }

                            Text(
                                text = fileSize,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = if (isDarkMode) TextMuted else Color(0xFF4B5563),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "PDF Preview & Reader",
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 30.sp,
                                letterSpacing = (-0.8).sp,
                                fontFamily = FontFamily.SansSerif,
                                color = if (isDarkMode) TextPrimary else Color(0xFF111827)
                            ),
                            modifier = Modifier.testTag("pdf_preview_title")
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Interactive visual document reading with pinch-to-zoom and AI synthesis",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = if (isDarkMode) TextSecondary else Color(0xFF4B5563),
                                fontSize = 13.sp
                            )
                        )
                    }
                }

                // View Mode Switcher: 'Visual Document' vs 'Summary & Metadata'
                item(key = "view_mode_toggle") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isDarkMode) Color(0x22000000) else Color(0x15000000))
                            .border(1.dp, GlassStroke, RoundedCornerShape(14.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Visual Document Tab
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (activeViewMode == PdfViewMode.VISUAL_DOCUMENT) {
                                        Brush.horizontalGradient(listOf(AmberWarm, AmberGlow))
                                    } else {
                                        Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                                    }
                                )
                                .clickable { activeViewMode = PdfViewMode.VISUAL_DOCUMENT }
                                .padding(vertical = 8.dp)
                                .testTag("tab_visual_document"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.MenuBook,
                                    contentDescription = null,
                                    tint = if (activeViewMode == PdfViewMode.VISUAL_DOCUMENT) Color(0xFF0F0E11) else if (isDarkMode) TextSecondary else Color(0xFF374151),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Visual Document",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = if (activeViewMode == PdfViewMode.VISUAL_DOCUMENT) Color(0xFF0F0E11) else if (isDarkMode) TextSecondary else Color(0xFF374151),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                )
                            }
                        }

                        // Summary & Metadata Tab
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (activeViewMode == PdfViewMode.SUMMARY_METADATA) {
                                        Brush.horizontalGradient(listOf(AmberWarm, AmberGlow))
                                    } else {
                                        Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                                    }
                                )
                                .clickable { activeViewMode = PdfViewMode.SUMMARY_METADATA }
                                .padding(vertical = 8.dp)
                                .testTag("tab_pdf_metadata"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.Summarize,
                                    contentDescription = null,
                                    tint = if (activeViewMode == PdfViewMode.SUMMARY_METADATA) Color(0xFF0F0E11) else if (isDarkMode) TextSecondary else Color(0xFF374151),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Summary & Meta",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = if (activeViewMode == PdfViewMode.SUMMARY_METADATA) Color(0xFF0F0E11) else if (isDarkMode) TextSecondary else Color(0xFF374151),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                )
                            }
                        }
                    }
                }

                // EMBEDDED INTERACTIVE PDF VIEWER SECTION
                if (activeViewMode == PdfViewMode.VISUAL_DOCUMENT) {
                    item(key = "interactive_pdf_viewer_card") {
                        GlassCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("interactive_pdf_viewer"),
                            shape = RoundedCornerShape(24.dp),
                            borderStrokeWidth = 1.dp,
                            borderColorList = listOf(Color(0x80FFFFFF), Color(0x25FFFFFF), Color(0x40FFFFFF)),
                            ambientGlowColor = PdfRed
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                // Viewer Header Controls: Page Indicator, Zoom In, Zoom Out, Reset
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    // Page Number Pill
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isDarkMode) Color(0x25FFFFFF) else Color(0x15000000))
                                            .padding(horizontal = 10.dp, vertical = 5.dp)
                                    ) {
                                        Text(
                                            text = if (pdfPages.isNotEmpty()) "Page ${selectedPageIndex + 1} of ${pdfPages.size}" else "Page 1 of 1",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = if (isDarkMode) TextPrimary else Color(0xFF111827),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp
                                            ),
                                            modifier = Modifier.testTag("pdf_page_indicator")
                                        )
                                    }

                                    // Zoom Controls Row
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        // Zoom Out
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .background(if (isDarkMode) Color(0x22FFFFFF) else Color(0x18000000))
                                                .clickable {
                                                    zoomScale = (zoomScale - 0.25f).coerceAtLeast(0.75f)
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.ZoomOut,
                                                contentDescription = "Zoom Out",
                                                tint = AmberWarm,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }

                                        // Zoom Scale Display
                                        Text(
                                            text = "${(zoomScale * 100).toInt()}%",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = if (isDarkMode) TextSecondary else Color(0xFF374151),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        )

                                        // Zoom In
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .background(if (isDarkMode) Color(0x22FFFFFF) else Color(0x18000000))
                                                .clickable {
                                                    zoomScale = (zoomScale + 0.25f).coerceAtMost(3.0f)
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.ZoomIn,
                                                contentDescription = "Zoom In",
                                                tint = AmberWarm,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }

                                        // Reset Zoom
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .background(if (isDarkMode) Color(0x22FFFFFF) else Color(0x18000000))
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
                                                tint = WarmGold,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Interactive Viewport for Current PDF Page
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(380.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color(0xFFEAEAEA))
                                        .border(1.dp, Color(0x33000000), RoundedCornerShape(16.dp))
                                        .pointerInput(Unit) {
                                            detectTransformGestures { _, pan, zoom, _ ->
                                                zoomScale = (zoomScale * zoom).coerceIn(0.75f, 3.5f)
                                                panOffsetX += pan.x
                                                panOffsetY += pan.y
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isRenderingPages) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            CircularProgressIndicator(
                                                color = AmberWarm,
                                                modifier = Modifier.size(32.dp),
                                                strokeWidth = 3.dp
                                            )
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Text(
                                                text = "Rendering PDF pages...",
                                                color = Color(0xFF333333),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    } else if (pdfPages.isNotEmpty()) {
                                        val currentPageBitmap = pdfPages.getOrNull(selectedPageIndex) ?: pdfPages.first()
                                        Image(
                                            bitmap = currentPageBitmap.asImageBitmap(),
                                            contentDescription = "PDF Page ${selectedPageIndex + 1}",
                                            contentScale = ContentScale.Fit,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(6.dp)
                                                .graphicsLayer {
                                                    scaleX = zoomScale
                                                    scaleY = zoomScale
                                                    translationX = panOffsetX
                                                    translationY = panOffsetY
                                                }
                                        )
                                    } else {
                                        // Visual fallback rendering for local sample PDF
                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(20.dp)
                                                .background(Color.White)
                                                .padding(16.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.PictureAsPdf,
                                                    contentDescription = null,
                                                    tint = PdfRed,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = fileName.ifBlank { "Document.pdf" },
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp,
                                                    color = Color(0xFF111827)
                                                )
                                            }
                                            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFE5E7EB)))
                                            Text(
                                                text = summaryText.ifBlank { "Executive document text preview rendered and indexed for semantic search." },
                                                fontSize = 12.sp,
                                                color = Color(0xFF374151),
                                                lineHeight = 18.sp
                                            )
                                            if (keyPoints.isNotEmpty()) {
                                                keyPoints.forEach { pt ->
                                                    Text(
                                                        text = "• $pt",
                                                        fontSize = 11.sp,
                                                        color = Color(0xFF4B5563)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                // Page Thumbnails Strip (if multi-page)
                                if (pdfPages.size > 1) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    LazyRow(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        itemsIndexed(pdfPages) { idx, pageBmp ->
                                            val isSelected = idx == selectedPageIndex
                                            Box(
                                                modifier = Modifier
                                                    .size(width = 48.dp, height = 64.dp)
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(Color.White)
                                                    .border(
                                                        width = if (isSelected) 2.dp else 1.dp,
                                                        color = if (isSelected) AmberWarm else Color(0x33000000),
                                                        shape = RoundedCornerShape(6.dp)
                                                    )
                                                    .clickable {
                                                        selectedPageIndex = idx
                                                        zoomScale = 1f
                                                        panOffsetX = 0f
                                                        panOffsetY = 0f
                                                    }
                                                    .padding(2.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Image(
                                                    bitmap = pageBmp.asImageBitmap(),
                                                    contentDescription = "Page ${idx + 1}",
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // File Information & Display Title
                item(key = "file_info_card") {
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("pdf_file_info_card"),
                        shape = RoundedCornerShape(24.dp),
                        borderStrokeWidth = 1.dp,
                        borderColorList = listOf(Color(0x60FFFFFF), Color(0x20FFFFFF), Color(0x40FFFFFF)),
                        ambientGlowColor = PdfRed
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0x33EF4444))
                                        .border(1.dp, Color(0x66EF4444), RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.PictureAsPdf,
                                        contentDescription = "PDF File",
                                        tint = PdfRed,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "FILE NAME",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = if (isDarkMode) TextMuted else Color(0xFF6B7280),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.5.sp
                                        )
                                    )
                                    Text(
                                        text = fileName,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            color = if (isDarkMode) TextPrimary else Color(0xFF111827),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        ),
                                        modifier = Modifier.testTag("pdf_file_name_display")
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = "DISPLAY TITLE",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = WarmGold,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.6.sp
                                )
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isDarkMode) Color(0x20FFFFFF) else Color(0x10000000))
                                    .border(1.dp, if (isDarkMode) GlassStroke else Color(0xFFCBD5E1), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                BasicTextField(
                                    value = documentTitle,
                                    onValueChange = { documentTitle = it },
                                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                                        color = if (isDarkMode) TextPrimary else Color(0xFF111827),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    cursorBrush = SolidColor(AmberWarm),
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("pdf_title_input")
                                )
                            }
                        }
                    }
                }

                // Frosted Loading Card during Extraction
                if (isExtracting) {
                    item(key = "ocr_extracting_progress_card") {
                        GlassCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("ocr_extracting_progress_card"),
                            shape = RoundedCornerShape(16.dp),
                            borderStrokeWidth = 1.dp,
                            borderColorList = listOf(Color(0x80FF9E58), Color(0x30FFA767)),
                            ambientGlowColor = AmberWarm
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = AmberWarm,
                                    strokeWidth = 2.5.dp
                                )
                                Spacer(modifier = Modifier.width(14.dp))
                                Column {
                                    Text(
                                        text = "Multi-Page OCR Engine Active",
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            color = AmberWarm,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = extractionProgress,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = if (isDarkMode) TextSecondary else Color(0xFF4B5563),
                                            fontSize = 12.sp
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                // Summary & Key Extracted Points Card
                item(key = "document_summary_card") {
                    Text(
                        text = "DOCUMENT SUMMARY & INSIGHTS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = WarmGold,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            letterSpacing = 0.8.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("pdf_summary_card"),
                        shape = RoundedCornerShape(24.dp),
                        borderStrokeWidth = 1.dp,
                        borderColorList = listOf(Color(0x60FFFFFF), Color(0x20FFFFFF), Color(0x40FFFFFF)),
                        ambientGlowColor = AmberWarm
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Rounded.AutoAwesome,
                                        contentDescription = null,
                                        tint = AmberWarm,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Executive Summary",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = if (isDarkMode) TextPrimary else Color(0xFF111827)
                                        )
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0x28FF9E58))
                                            .border(1.dp, AmberWarm.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                            .clickable {
                                                if (summaryText.isNotBlank()) {
                                                    clipboardManager.setText(AnnotatedString(summaryText))
                                                    coroutineScope.launch {
                                                        snackbarHostState.showSnackbar("Executive summary copied to clipboard! 📋")
                                                    }
                                                }
                                            }
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                            .testTag("copy_summary_button")
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Rounded.ContentCopy,
                                                contentDescription = "Copy Summary",
                                                tint = WarmGold,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "Copy All",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = WarmGold,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(6.dp))

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isDarkMode) Color(0x20FFFFFF) else Color(0x15000000))
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = "Editable",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = if (isDarkMode) TextSecondary else Color(0xFF4B5563),
                                                fontSize = 10.sp
                                            )
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Editable Summary Box
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isDarkMode) Color(0x18FFFFFF) else Color(0x10000000))
                                    .border(1.dp, if (isDarkMode) GlassStroke else Color(0xFFCBD5E1), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 14.dp, vertical = 12.dp)
                            ) {
                                BasicTextField(
                                    value = summaryText,
                                    onValueChange = { summaryText = it },
                                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                                        color = if (isDarkMode) TextPrimary else Color(0xFF111827),
                                        fontSize = 13.sp,
                                        lineHeight = 20.sp
                                    ),
                                    cursorBrush = SolidColor(AmberWarm),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("pdf_summary_input")
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Key Points",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = if (isDarkMode) TextPrimary else Color(0xFF111827)
                                )
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Bullet Points
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                keyPoints.forEachIndexed { _, point ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isDarkMode) Color(0x12FFFFFF) else Color(0x0C000000))
                                            .border(0.5.dp, if (isDarkMode) Color(0x20FFFFFF) else Color(0x20000000), RoundedCornerShape(12.dp))
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .padding(top = 4.dp)
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(AmberWarm)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = point,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = if (isDarkMode) TextPrimary else Color(0xFF1F2937),
                                                fontSize = 12.sp,
                                                lineHeight = 18.sp
                                            ),
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Add point button or input
                            if (isAddingPoint) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isDarkMode) Color(0x20FFFFFF) else Color(0x10000000))
                                            .border(1.dp, GlassStroke, RoundedCornerShape(12.dp))
                                            .padding(horizontal = 12.dp, vertical = 10.dp)
                                    ) {
                                        BasicTextField(
                                            value = newPointText,
                                            onValueChange = { newPointText = it },
                                            textStyle = MaterialTheme.typography.bodySmall.copy(
                                                color = if (isDarkMode) TextPrimary else Color(0xFF111827),
                                                fontSize = 12.sp
                                            ),
                                            cursorBrush = SolidColor(AmberWarm),
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Brush.horizontalGradient(listOf(AmberWarm, AmberGlow)))
                                            .clickable {
                                                if (newPointText.isNotBlank()) {
                                                    keyPoints.add(newPointText.trim())
                                                    newPointText = ""
                                                    isAddingPoint = false
                                                }
                                            }
                                            .padding(horizontal = 12.dp, vertical = 10.dp)
                                    ) {
                                        Text(
                                            text = "Add",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = Color(0xFF0F0E11),
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                    }
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isDarkMode) Color(0x18FFFFFF) else Color(0x10000000))
                                        .clickable { isAddingPoint = true }
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Rounded.Add,
                                            contentDescription = null,
                                            tint = AmberWarm,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "+ Add Point",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = WarmGold,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 11.sp
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Full Multi-Page Extracted Transcript Card (100% Text Capture)
                if (fullExtractedTranscript.isNotBlank()) {
                    item(key = "pdf_full_transcript_card") {
                        val wordCount = fullExtractedTranscript.split(Regex("""\s+""")).count { it.isNotBlank() }
                        GlassCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("pdf_full_transcript_card"),
                            shape = RoundedCornerShape(24.dp),
                            borderStrokeWidth = 1.dp,
                            borderColorList = listOf(Color(0x60FFFFFF), Color(0x20FFFFFF), Color(0x40FFFFFF)),
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
                                        Icon(
                                            imageVector = Icons.Rounded.ReceiptLong,
                                            contentDescription = null,
                                            tint = AmberWarm,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Full Multi-Page Transcript",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                color = if (isDarkMode) TextPrimary else Color(0xFF111827)
                                            )
                                        )
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color(0x28FF9E58))
                                                .border(1.dp, AmberWarm.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                                .clickable {
                                                    clipboardManager.setText(AnnotatedString(fullExtractedTranscript))
                                                    coroutineScope.launch {
                                                        snackbarHostState.showSnackbar("Copied full multi-page PDF transcript! 📋")
                                                    }
                                                }
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                                .testTag("copy_pdf_full_transcript_button")
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Rounded.ContentCopy,
                                                    contentDescription = "Copy Full Transcript",
                                                    tint = WarmGold,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "Copy Transcript",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        color = WarmGold,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                val displayPageCount = if (totalPagesExtracted > 0) totalPagesExtracted else (if (pdfPages.isNotEmpty()) pdfPages.size else 1)
                                val displayWordCount = if (totalWords > 0) totalWords else wordCount
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isDarkMode) Color(0x22FFFFFF) else Color(0x15000000))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "$displayWordCount words • Extracted from $displayPageCount ${if (displayPageCount == 1) "page" else "pages"}",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = if (isDarkMode) TextSecondary else Color(0xFF4B5563),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Scrollable Full Text Area
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 120.dp, max = 320.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isDarkMode) Color(0x18FFFFFF) else Color(0x10000000))
                                        .border(1.dp, if (isDarkMode) GlassStroke else Color(0xFFCBD5E1), RoundedCornerShape(12.dp))
                                        .padding(12.dp)
                                        .verticalScroll(rememberScrollState())
                                ) {
                                    Text(
                                        text = fullExtractedTranscript,
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
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("pdf_semantic_search_toggle_card"),
                        shape = RoundedCornerShape(20.dp),
                        borderStrokeWidth = 1.dp,
                        borderColorList = listOf(Color(0x60FFFFFF), Color(0x20FFFFFF), Color(0x40FFFFFF)),
                        ambientGlowColor = AmberWarm
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Include in AI Semantic Search",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = if (isDarkMode) TextPrimary else Color(0xFF111827)
                                    )
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Enable natural language Q&A across this PDF document",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = if (isDarkMode) TextSecondary else Color(0xFF4B5563),
                                        fontSize = 11.sp
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Switch(
                                checked = includeInSemanticSearch,
                                onCheckedChange = { includeInSemanticSearch = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color(0xFF161318),
                                    checkedTrackColor = AmberWarm,
                                    checkedBorderColor = AmberGlow,
                                    uncheckedThumbColor = TextMuted,
                                    uncheckedTrackColor = Color(0x2AFFFFFF),
                                    uncheckedBorderColor = Color(0x33FFFFFF)
                                ),
                                modifier = Modifier.testTag("pdf_semantic_search_switch")
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PdfPreviewTopBar(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Back and Save buttons removed as per requirements.
    }
}
