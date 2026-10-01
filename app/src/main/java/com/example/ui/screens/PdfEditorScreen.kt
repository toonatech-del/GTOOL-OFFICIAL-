package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Environment
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.PdfTemplate
import com.example.ui.GsdcallWorkspaceViewModel
import com.example.ui.components.AmbientLightingBackground
import com.example.ui.components.DarkFrostedSnackbarHost
import com.example.ui.components.GlassCard
import com.example.ui.theme.AmberWarm
import com.example.ui.theme.GlassStroke
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarmGold
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfEditorScreen(
    onBackClick: () -> Unit,
    viewModel: GsdcallWorkspaceViewModel,
    onSaveSuccess: ((Uri) -> Unit)? = null
) {
    // Intercept hardware/system back button to pop stack safely
    BackHandler {
        onBackClick()
    }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var attachedImageUri by remember { mutableStateOf<Uri?>(null) }
    var attachedImageName by remember { mutableStateOf("") }
    var includeInSemanticSearch by remember { mutableStateOf(true) }
    var isGenerating by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    // Dynamic PDF Template Configuration State
    var selectedTemplate by remember { mutableStateOf(PdfTemplate.PROFESSIONAL) }
    var showTemplateSelectionModal by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            attachedImageUri = uri
            attachedImageName = "Attachment_${System.currentTimeMillis() % 10000}.jpg"
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Image attached to PDF studio 🖼️")
            }
        }
    }

    fun applyFormatting(prefix: String, suffix: String = "") {
        body = if (body.isEmpty()) {
            "$prefix$suffix"
        } else {
            "$body\n$prefix$suffix"
        }
    }

    fun insertDivider() {
        body = if (body.isEmpty()) {
            "──────────────────────────────\n"
        } else {
            "$body\n──────────────────────────────\n"
        }
    }

    AmbientLightingBackground {
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { DarkFrostedSnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                    title = {
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
                                "Create PDF Studio",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 18.sp
                                )
                            )
                        }
                    },
                    actions = {
                        // Quick Template Switch Action in TopBar
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0x25FFB87E))
                                .border(1.dp, AmberWarm.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                                .clickable { showTemplateSelectionModal = true }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.Style,
                                    contentDescription = "Templates",
                                    tint = AmberWarm,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = selectedTemplate.displayName.take(12),
                                    style = TextStyle(
                                        color = AmberWarm,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.Rounded.ArrowBack, "Back", tint = Color.White)
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Section 1: Template Selection Bar (Interactive Strip)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "PDF TEMPLATE & LAYOUT",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xB8C2CCFF),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                letterSpacing = 0.8.sp
                            )
                        )

                        Text(
                            "Change Style",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = AmberWarm,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            ),
                            modifier = Modifier.clickable { showTemplateSelectionModal = true }
                        )
                    }

                    // Horizontal Quick-Picker Pills for Templates
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PdfTemplate.values().forEach { tmpl ->
                            val isSelected = selectedTemplate == tmpl
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        if (isSelected) {
                                            Brush.linearGradient(tmpl.previewGradient.map { it.copy(alpha = 0.35f) })
                                        } else {
                                            Brush.linearGradient(listOf(Color(0x18FFFFFF), Color(0x0CFFFFFF)))
                                        }
                                    )
                                    .border(
                                        if (isSelected) 1.5.dp else 1.dp,
                                        if (isSelected) AmberWarm else Color(0x30FFFFFF),
                                        RoundedCornerShape(16.dp)
                                    )
                                    .clickable { selectedTemplate = tmpl }
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                                    .testTag("template_pill_${tmpl.id}")
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isSelected) AmberWarm else Color.White.copy(alpha = 0.4f)
                                            )
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = tmpl.displayName,
                                            style = TextStyle(
                                                color = if (isSelected) Color.White else TextSecondary,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                fontSize = 13.sp
                                            )
                                        )
                                        Text(
                                            text = tmpl.subtitle,
                                            style = TextStyle(
                                                color = if (isSelected) AmberWarm else TextMuted,
                                                fontSize = 10.sp
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Section 2: PDF Document Title
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "DOCUMENT TITLE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color(0xB8C2CCFF),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            letterSpacing = 0.8.sp
                        )
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0x1CFFFFFF))
                            .border(1.dp, Color(0x35FFFFFF), RoundedCornerShape(16.dp))
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        if (title.isEmpty()) {
                            Text(
                                text = "e.g., Annual Business Report, Project Proposal...",
                                style = TextStyle(color = Color(0x80FFFFFF), fontSize = 15.sp)
                            )
                        }
                        BasicTextField(
                            value = title,
                            onValueChange = { title = it },
                            visualTransformation = remember { com.example.util.MarkdownVisualTransformation(isDarkMode = true) },
                            textStyle = TextStyle(
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            cursorBrush = SolidColor(Color(0xFF00E5FF)),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("pdf_title_input")
                        )
                    }
                }

                // Section 3: Rich Text Formatting & Attachment Action Toolbar
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "FORMATTING & TOOLS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color(0xB8C2CCFF),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            letterSpacing = 0.8.sp
                        )
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0x28FFFFFF))
                            .border(
                                1.dp,
                                Brush.horizontalGradient(
                                    listOf(Color(0x45FFFFFF), Color(0x18FFFFFF))
                                ),
                                RoundedCornerShape(20.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // [B] Bold
                            PdfToolbarButton(
                                label = "B",
                                contentDesc = "Bold Text",
                                onClick = { applyFormatting("**", "**") }
                            )

                            // [/] Italic
                            PdfToolbarButton(
                                label = "/",
                                contentDesc = "Italic Text",
                                isItalic = true,
                                onClick = { applyFormatting("*", "*") }
                            )

                            // [1. 2. 3.] Numbered List
                            PdfToolbarIconButton(
                                icon = Icons.Rounded.FormatListNumbered,
                                contentDesc = "Numbered List",
                                onClick = { applyFormatting("1. ") }
                            )

                            // [•] Bullet List
                            PdfToolbarIconButton(
                                icon = Icons.Rounded.FormatListBulleted,
                                contentDesc = "Bullet List",
                                onClick = { applyFormatting("• ") }
                            )

                            // [—] Horizontal Divider
                            PdfToolbarIconButton(
                                icon = Icons.Rounded.HorizontalRule,
                                contentDesc = "Divider Line",
                                onClick = { insertDivider() }
                            )

                            // [🖼️ Attach Image]
                            Box(
                                modifier = Modifier
                                    .height(38.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0x3500E5FF))
                                    .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                                    .clickable {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    }
                                    .padding(horizontal = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Rounded.AddPhotoAlternate,
                                        contentDescription = "Attach Image",
                                        tint = Color(0xFF80EEFF),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Attach Image",
                                        style = TextStyle(
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                // Section 4: Attached Image Thumbnail Preview Card (if any)
                if (attachedImageUri != null) {
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("pdf_attached_image_preview"),
                        shape = RoundedCornerShape(18.dp),
                        borderStrokeWidth = 1.dp,
                        borderColorList = listOf(Color(0x80FFFFFF), Color(0x22FFFFFF)),
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
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0x20FFFFFF))
                                        .border(1.dp, GlassStroke, RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    AsyncImage(
                                        model = attachedImageUri,
                                        contentDescription = "PDF Attached Image",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column {
                                    Text(
                                        text = "Attached to PDF",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = TextPrimary
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = attachedImageName,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = TextSecondary,
                                            fontSize = 11.sp
                                        )
                                    )
                                    Text(
                                        text = "Will be rendered according to ${selectedTemplate.displayName} layout",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = WarmGold,
                                            fontSize = 10.sp
                                        )
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x25FF5252))
                                    .clickable {
                                        attachedImageUri = null
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar("Attachment removed")
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.DeleteOutline,
                                    contentDescription = "Remove Image",
                                    tint = Color(0xFFFF6B6B),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                // Section 5: PDF Document Body
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "DOCUMENT BODY & CONTENT",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color(0xB8C2CCFF),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            letterSpacing = 0.8.sp
                        )
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 220.dp, max = 380.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0x1CFFFFFF))
                            .border(1.dp, Color(0x35FFFFFF), RoundedCornerShape(20.dp))
                            .padding(16.dp)
                    ) {
                        if (body.isEmpty()) {
                            Text(
                                text = "Start typing your document text...\n• Use toolbar for bullet points\n• Format bold with **text**\n• Add horizontal dividers with [—]",
                                style = TextStyle(
                                    color = Color(0x80FFFFFF),
                                    fontSize = 15.sp,
                                    lineHeight = 22.sp
                                )
                            )
                        }
                        BasicTextField(
                            value = body,
                            onValueChange = { body = it },
                            visualTransformation = remember { com.example.util.MarkdownVisualTransformation(isDarkMode = true) },
                            textStyle = TextStyle(
                                color = Color.White,
                                fontSize = 15.sp,
                                lineHeight = 24.sp,
                                fontFamily = FontFamily.SansSerif
                            ),
                            cursorBrush = SolidColor(Color(0xFF00E5FF)),
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("pdf_body_input")
                        )
                    }
                }

                // Section 6: AI Semantic Search Toggle Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0x1EFFFFFF))
                        .border(1.dp, Color(0x25FFFFFF), RoundedCornerShape(18.dp))
                        .padding(horizontal = 16.dp, vertical = 12.dp)
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
                                    .clip(CircleShape)
                                    .background(Color(0x30FFB87E)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AutoAwesome,
                                    contentDescription = null,
                                    tint = AmberWarm,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Include in AI Semantic Search",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = Color.White
                                )
                            )
                        }

                        Switch(
                            checked = includeInSemanticSearch,
                            onCheckedChange = { includeInSemanticSearch = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFF2E1A0C),
                                checkedTrackColor = AmberWarm,
                                uncheckedThumbColor = Color(0x88FFFFFF),
                                uncheckedTrackColor = Color(0x30FFFFFF)
                            )
                        )
                    }
                }

                // Section 7: Primary Action Button - Generate & Save PDF with Selected Template Layout
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .clip(RoundedCornerShape(32.dp))
                        .background(Color(0x351F1D2B))
                        .border(
                            1.5.dp,
                            Brush.horizontalGradient(selectedTemplate.previewGradient),
                            RoundedCornerShape(32.dp)
                        )
                        .clickable(enabled = !isGenerating && !isSaving) {
                            if (isGenerating || isSaving) return@clickable
                            isGenerating = true
                            isSaving = true
                            try {
                                val finalTitle = if (title.isNotBlank()) title.trim() else "Document_${System.currentTimeMillis() % 10000}"
                                val sanitizedName = finalTitle.replace(Regex("[^a-zA-Z0-9_\\-\\s]"), "").trim().replace("\\s+".toRegex(), "_")
                                val fileName = "$sanitizedName.pdf"

                                // Build Multi-Page/Rich Scaled PDF Document dynamically customized by template
                                val document = PdfDocument()
                                val pageWidth = 595 // Standard A4 width in points
                                val pageHeight = 842 // Standard A4 height in points
                                val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
                                val page = document.startPage(pageInfo)
                                val canvas = page.canvas

                                val primaryColorInt = android.graphics.Color.parseColor(selectedTemplate.primaryColorHex)
                                val accentColorInt = android.graphics.Color.parseColor(selectedTemplate.accentColorHex)
                                val secondaryColorInt = android.graphics.Color.parseColor(selectedTemplate.secondaryColorHex)

                                // Dynamic Typography configuration
                                val titleTypeface = when (selectedTemplate) {
                                    PdfTemplate.PROFESSIONAL -> Typeface.create(Typeface.SERIF, Typeface.BOLD)
                                    PdfTemplate.MODERN -> Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                                    PdfTemplate.SIMPLE -> Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                                    PdfTemplate.CREATIVE -> Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                                }

                                val headerPaint = Paint().apply {
                                    isAntiAlias = true
                                    color = if (selectedTemplate == PdfTemplate.PROFESSIONAL) android.graphics.Color.WHITE else primaryColorInt
                                    textSize = when (selectedTemplate) {
                                        PdfTemplate.PROFESSIONAL -> 22f
                                        PdfTemplate.MODERN -> 24f
                                        PdfTemplate.SIMPLE -> 20f
                                        PdfTemplate.CREATIVE -> 23f
                                    }
                                    typeface = titleTypeface
                                }

                                val metaPaint = Paint().apply {
                                    isAntiAlias = true
                                    color = secondaryColorInt
                                    textSize = 9.5f
                                    typeface = Typeface.DEFAULT
                                }

                                val accentLinePaint = Paint().apply {
                                    isAntiAlias = true
                                    color = accentColorInt
                                    strokeWidth = 2.5f
                                    style = Paint.Style.STROKE
                                }

                                val dividerPaint = Paint().apply {
                                    isAntiAlias = true
                                    color = android.graphics.Color.parseColor("#E2E8F0")
                                    strokeWidth = 1f
                                    style = Paint.Style.STROKE
                                }

                                val bodyTextPaint = TextPaint().apply {
                                    isAntiAlias = true
                                    color = android.graphics.Color.parseColor("#334155")
                                    textSize = 12.5f
                                }

                                val boldTextPaint = TextPaint().apply {
                                    isAntiAlias = true
                                    color = primaryColorInt
                                    textSize = 13f
                                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                                }

                                val italicTextPaint = TextPaint().apply {
                                    isAntiAlias = true
                                    color = secondaryColorInt
                                    textSize = 12.5f
                                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
                                }

                                val marginX = when (selectedTemplate) {
                                    PdfTemplate.CREATIVE -> 50f
                                    PdfTemplate.SIMPLE -> 36f
                                    else -> 40f
                                }
                                val contentWidth = pageWidth - (marginX * 2)
                                var currentY = 50f

                                // Template-Driven Page Background & Decorative Frames
                                if (selectedTemplate.showAccentBorder) {
                                    val borderPaint = Paint().apply {
                                        isAntiAlias = true
                                        color = accentColorInt
                                        strokeWidth = if (selectedTemplate == PdfTemplate.MODERN) 3f else 1.5f
                                        style = Paint.Style.STROKE
                                    }
                                    canvas.drawRoundRect(RectF(16f, 16f, pageWidth - 16f, pageHeight - 16f), 12f, 12f, borderPaint)
                                }

                                if (selectedTemplate == PdfTemplate.CREATIVE) {
                                    // Left vertical color ribbon
                                    val ribbonPaint = Paint().apply {
                                        isAntiAlias = true
                                        color = primaryColorInt
                                        style = Paint.Style.FILL
                                    }
                                    canvas.drawRect(RectF(16f, 16f, 32f, pageHeight - 16f), ribbonPaint)
                                }

                                // 1. Draw Template Header
                                when (selectedTemplate) {
                                    PdfTemplate.PROFESSIONAL -> {
                                        // Top Solid Banner
                                        val bannerPaint = Paint().apply {
                                            isAntiAlias = true
                                            color = primaryColorInt
                                            style = Paint.Style.FILL
                                        }
                                        canvas.drawRect(RectF(marginX - 10f, currentY - 20f, marginX + contentWidth + 10f, currentY + 36f), bannerPaint)
                                        
                                        // Gold accent ribbon
                                        val goldBadge = Paint().apply {
                                            isAntiAlias = true
                                            color = accentColorInt
                                            style = Paint.Style.FILL
                                        }
                                        canvas.drawRect(RectF(marginX - 10f, currentY + 36f, marginX + contentWidth + 10f, currentY + 40f), goldBadge)

                                        canvas.drawText(finalTitle, marginX, currentY + 14f, headerPaint)
                                        currentY += 56f

                                        val dateStr = SimpleDateFormat("MMMM dd, yyyy • hh:mm a", Locale.getDefault()).format(Date())
                                        canvas.drawText("Professional Document • $dateStr", marginX, currentY, metaPaint)
                                        currentY += 24f
                                    }
                                    PdfTemplate.MODERN -> {
                                        // Vibrant Modern Gradient Title
                                        canvas.drawText(finalTitle, marginX, currentY + 10f, headerPaint)
                                        currentY += 24f

                                        val dateStr = SimpleDateFormat("yyyy-MM-dd • hh:mm a", Locale.getDefault()).format(Date())
                                        canvas.drawText("MODERN STUDIO EDITION • $dateStr", marginX, currentY, metaPaint)
                                        currentY += 10f

                                        canvas.drawLine(marginX, currentY, marginX + 110f, currentY, accentLinePaint)
                                        currentY += 24f
                                    }
                                    PdfTemplate.SIMPLE -> {
                                        // Clean Minimalist Title & Full-width light line
                                        canvas.drawText(finalTitle, marginX, currentY, headerPaint)
                                        currentY += 14f

                                        val dateStr = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())
                                        canvas.drawText(dateStr, marginX, currentY, metaPaint)
                                        currentY += 10f

                                        canvas.drawLine(marginX, currentY, marginX + contentWidth, currentY, dividerPaint)
                                        currentY += 20f
                                    }
                                    PdfTemplate.CREATIVE -> {
                                        // Creative Studio Header
                                        canvas.drawText(finalTitle, marginX, currentY, headerPaint)
                                        currentY += 16f

                                        val dateStr = SimpleDateFormat("MMMM yyyy • GTOOL X Studio", Locale.getDefault()).format(Date())
                                        canvas.drawText(dateStr, marginX, currentY, metaPaint)
                                        currentY += 12f

                                        val dotPaint = Paint().apply {
                                            isAntiAlias = true
                                            color = accentColorInt
                                            style = Paint.Style.FILL
                                        }
                                        canvas.drawCircle(marginX + 4f, currentY, 4f, dotPaint)
                                        canvas.drawCircle(marginX + 16f, currentY, 4f, dotPaint)
                                        canvas.drawCircle(marginX + 28f, currentY, 4f, dotPaint)
                                        currentY += 22f
                                    }
                                }

                                // 2. Draw Formatted Body Text
                                val rawLines = body.lines()
                                for (rawLine in rawLines) {
                                    val trimmed = rawLine.trim()
                                    if (trimmed.startsWith("─────") || trimmed.contains("──────")) {
                                        currentY += 6f
                                        canvas.drawLine(marginX, currentY, marginX + contentWidth, currentY, dividerPaint)
                                        currentY += 12f
                                    } else if (trimmed.startsWith("•") || trimmed.startsWith("-")) {
                                        val bulletContent = trimmed.removePrefix("•").removePrefix("-").trim()
                                        
                                        // Draw Bullet with Template Accent Color
                                        val bulletPaint = Paint().apply {
                                            isAntiAlias = true
                                            color = accentColorInt
                                            style = Paint.Style.FILL
                                        }
                                        if (selectedTemplate == PdfTemplate.SIMPLE) {
                                            canvas.drawRect(RectF(marginX + 4f, currentY - 7f, marginX + 8f, currentY - 3f), bulletPaint)
                                        } else {
                                            canvas.drawCircle(marginX + 6f, currentY - 4f, 2.5f, bulletPaint)
                                        }
                                        
                                        val spannedBullet = com.example.util.RichTextHelper.createSpannedText(bulletContent, primaryColorInt, android.graphics.Color.parseColor("#334155"))
                                        val layout = StaticLayout.Builder.obtain(
                                            spannedBullet,
                                            0,
                                            spannedBullet.length,
                                            bodyTextPaint,
                                            (contentWidth - 18).toInt()
                                        ).setAlignment(Layout.Alignment.ALIGN_NORMAL).build()

                                        canvas.save()
                                        canvas.translate(marginX + 16f, currentY - 12f)
                                        layout.draw(canvas)
                                        canvas.restore()
                                        currentY += layout.height + 8f
                                    } else if (trimmed.matches(Regex("""^\d+\..*"""))) {
                                        val numPrefix = trimmed.substringBefore(".").trim() + "."
                                        val itemText = trimmed.substringAfter(".").trim()

                                        canvas.drawText(numPrefix, marginX, currentY, boldTextPaint)
                                        
                                        val spannedItem = com.example.util.RichTextHelper.createSpannedText(itemText, primaryColorInt, android.graphics.Color.parseColor("#334155"))
                                        val layout = StaticLayout.Builder.obtain(
                                            spannedItem,
                                            0,
                                            spannedItem.length,
                                            bodyTextPaint,
                                            (contentWidth - 24).toInt()
                                        ).setAlignment(Layout.Alignment.ALIGN_NORMAL).build()

                                        canvas.save()
                                        canvas.translate(marginX + 22f, currentY - 12f)
                                        layout.draw(canvas)
                                        canvas.restore()
                                        currentY += layout.height + 8f
                                    } else if (trimmed.isNotBlank()) {
                                         // Use unified spanned text creator to handle **bold**, *italic*, and ## heading syntax
                                         val spannedContent = com.example.util.RichTextHelper.createSpannedText(rawLine, primaryColorInt, android.graphics.Color.parseColor("#334155"))
                                         
                                         val layout = StaticLayout.Builder.obtain(
                                             spannedContent,
                                             0,
                                             spannedContent.length,
                                             bodyTextPaint,
                                             contentWidth.toInt()
                                         ).setAlignment(Layout.Alignment.ALIGN_NORMAL).build()

                                         canvas.save()
                                         canvas.translate(marginX, currentY - 12f)
                                         layout.draw(canvas)
                                         canvas.restore()
                                         currentY += layout.height + 6f
                                     } else {
                                         currentY += 10f
                                     }
                                }

                                // 3. Draw Image Attachment (if present)
                                attachedImageUri?.let { imgUri ->
                                    try {
                                        val stream: InputStream? = context.contentResolver.openInputStream(imgUri)
                                        val sourceBitmap = BitmapFactory.decodeStream(stream)
                                        stream?.close()

                                        if (sourceBitmap != null) {
                                            currentY += 14f
                                            val maxImgW = contentWidth
                                            val maxImgH = 250f
                                            val scale = minOf(maxImgW / sourceBitmap.width.toFloat(), maxImgH / sourceBitmap.height.toFloat())
                                            val destW = sourceBitmap.width * scale
                                            val destH = sourceBitmap.height * scale
                                            val destX = marginX + (contentWidth - destW) / 2f

                                            // Draw subtle photo border
                                            if (selectedTemplate.showAccentBorder) {
                                                val photoBorderPaint = Paint().apply {
                                                    isAntiAlias = true
                                                    color = accentColorInt
                                                    strokeWidth = 1f
                                                    style = Paint.Style.STROKE
                                                }
                                                canvas.drawRoundRect(RectF(destX - 2f, currentY - 2f, destX + destW + 2f, currentY + destH + 2f), 6f, 6f, photoBorderPaint)
                                            }

                                            val destRect = RectF(destX, currentY, destX + destW, currentY + destH)
                                            canvas.drawBitmap(sourceBitmap, null, destRect, Paint(Paint.FILTER_BITMAP_FLAG))
                                            currentY += destH + 16f
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }

                                // 4. Footer info
                                val footerPaint = Paint().apply {
                                    isAntiAlias = true
                                    color = android.graphics.Color.parseColor("#94A3B8")
                                    textSize = 8.5f
                                    typeface = Typeface.DEFAULT
                                }
                                canvas.drawText("Page 1 of 1 • Styled with ${selectedTemplate.displayName} Template • GTOOL X", marginX, pageHeight - 28f, footerPaint)

                                // 5. Finish & Write PDF to Documents/GTOOL X/
                                document.finishPage(page)

                                val gtoolDir = com.example.util.SecurityUtils.getSecureVaultDir(context)
                                val file = File(gtoolDir, fileName)
                                val outStream = FileOutputStream(file)
                                document.writeTo(outStream)
                                outStream.flush()
                                outStream.close()
                                document.close()

                                val fileSizeMB = "${String.format(Locale.US, "%.1f", maxOf(0.1, file.length() / (1024.0 * 1024.0)))} MB"
                                val keyPointsList = body.lines().filter { it.startsWith("•") || it.startsWith("-") || it.matches(Regex("""^\d+\..*""")) }

                                // Save to Room Database for instant display in PDF tab & Recent items
                                viewModel.savePdfDocument(
                                    title = finalTitle,
                                    fileName = fileName,
                                    fileSize = fileSizeMB,
                                    summary = body.take(160).ifBlank { "Custom created PDF document with ${selectedTemplate.displayName} formatting and attachments." },
                                    keyPoints = keyPointsList,
                                    pdfUri = Uri.fromFile(file),
                                    includeInSearch = includeInSemanticSearch,
                                    fullTranscript = body
                                )

                                Toast.makeText(context, "PDF saved with ${selectedTemplate.displayName} style! 📄", Toast.LENGTH_LONG).show()
                                if (onSaveSuccess != null) {
                                    onSaveSuccess(Uri.fromFile(file))
                                } else {
                                    onBackClick()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "Error saving PDF: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                            } finally {
                                isGenerating = false
                                isSaving = false
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (isGenerating || isSaving) {
                        CircularProgressIndicator(
                            color = Color(0xFF00E5FF),
                            modifier = Modifier.size(28.dp),
                            strokeWidth = 3.dp
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.PictureAsPdf,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Generate & Save PDF (${selectedTemplate.displayName})",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 17.sp,
                                    color = Color.White
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // Modal Bottom Sheet / Dialog for Full Template Selection
    if (showTemplateSelectionModal) {
        ModalBottomSheet(
            onDismissRequest = { showTemplateSelectionModal = false },
            containerColor = Color(0xFF14121A),
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(vertical = 10.dp)
                        .width(40.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(Color(0x40FFFFFF))
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Select Document Template",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        Text(
                            text = "Adjusts colors, headers, typography & borders",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                        )
                    }

                    IconButton(onClick = { showTemplateSelectionModal = false }) {
                        Icon(Icons.Rounded.Close, "Close", tint = TextMuted)
                    }
                }

                PdfTemplate.values().forEach { tmpl ->
                    TemplateCard(
                        template = tmpl,
                        isSelected = selectedTemplate == tmpl,
                        onSelect = {
                            selectedTemplate = tmpl
                            showTemplateSelectionModal = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PdfToolbarButton(
    label: String,
    contentDesc: String,
    onClick: () -> Unit,
    isItalic: Boolean = false
) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x20FFFFFF))
            .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = TextStyle(
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                fontStyle = if (isItalic) androidx.compose.ui.text.font.FontStyle.Italic else androidx.compose.ui.text.font.FontStyle.Normal
            )
        )
    }
}

@Composable
private fun PdfToolbarIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDesc: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x20FFFFFF))
            .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDesc,
            tint = Color.White,
            modifier = Modifier.size(18.dp)
        )
    }
}
