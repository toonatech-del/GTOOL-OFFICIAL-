package com.example.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBackIos
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Compress
import androidx.compose.material.icons.rounded.Crop
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.AmbientLightingBackground
import com.example.ui.components.DarkFrostedSnackbarHost
import com.example.ui.components.GlassCard
import com.example.ui.components.InteractiveCropper
import com.example.ui.theme.AmberGlow
import com.example.ui.theme.AmberWarm
import com.example.ui.theme.AppTheme
import com.example.ui.theme.GlassStroke
import com.example.ui.theme.SyncGreenGlow
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarmGold
import com.example.util.ImageProcessingUtils
import com.example.util.OutputFormat
import com.example.util.ResizeResult
import kotlinx.coroutines.launch
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.viewmodel.PhotoResizerViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

enum class DimensionUnit(val label: String) {
    PX("px"),
    CM("cm"),
    MM("mm"),
    INCH("inch")
}

data class AspectPreset(
    val name: String,
    val width: Int,
    val height: Int,
    val description: String,
    val minKb: Int = 10,
    val maxKb: Int = 500,
    val label: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoResizerScreen(
    onBackClick: () -> Unit,
    onSaveSuccess: (title: String, imageUri: String, info: String, syncToDrive: Boolean) -> Unit = { _, _, _, _ -> },
    modifier: Modifier = Modifier,
    viewModel: PhotoResizerViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // ... rest of the code will be updated gradually
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var originalBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var originalWidth by remember { mutableIntStateOf(0) }
    var originalHeight by remember { mutableIntStateOf(0) }
    var originalFileSizeKb by remember { mutableLongStateOf(0L) }

    var selectedUnit by remember { mutableStateOf(DimensionUnit.PX) }
    var widthInput by remember { mutableStateOf("1080") }
    var heightInput by remember { mutableStateOf("1080") }
    
    // New fields
    var dpiInput by remember { mutableStateOf("200") }
    var addNameDate by remember { mutableStateOf(false) }
    var candidateName by remember { mutableStateOf("") }
    var dateOfPhoto by remember { mutableStateOf("") }
    
    var isAspectRatioLocked by remember { mutableStateOf(true) }
    var targetSizeKbInput by remember { mutableStateOf("200") }
    var qualitySlider by remember { mutableFloatStateOf(85f) }
    var selectedFormat by remember { mutableStateOf(OutputFormat.JPEG) }
    var isCropping by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }
    var lastResult by remember { mutableStateOf<ResizeResult?>(null) }
    var estimatedFileSizeKb by remember { mutableFloatStateOf(0f) }
    var calculatedQuality by remember { mutableIntStateOf(85) }
    var selectedPreset by remember { mutableStateOf<AspectPreset?>(null) }

    var showPresetsSheet by remember { mutableStateOf(false) }

    val presets = remember {
        listOf(
            AspectPreset("SSC Photo", 413, 531, "3.5x4.5cm, 20-50KB, 200 DPI", 20, 50, "SSC Standard"),
            AspectPreset("SSC Sign", 472, 236, "4.0x2.0cm, 10-20KB, 200 DPI", 10, 20, "SSC Signature"),
            AspectPreset("UPSC Ph/Sig", 413, 413, "350x350px min, 20-300KB, 300 DPI", 20, 300, "UPSC Standard"),
            AspectPreset("Railway Ph", 413, 531, "35x45mm, 30-70KB, 200 DPI", 30, 70, "RRB Standard"),
            AspectPreset("Banking Ph", 531, 413, "4.5x3.5cm, 20-50KB, 200 DPI", 20, 50, "IBPS Standard"),
            AspectPreset("Indian Passport", 413, 531, "3.5x4.5cm, 300 DPI", 10, 100, "Passport"),
            AspectPreset("US Visa", 600, 600, "2x2 inch, 300 DPI", 10, 240, "US Visa"),
            AspectPreset("PAN/DL", 295, 413, "2.5x3.5cm, 200 DPI", 10, 50, "Govt ID"),
            AspectPreset("Custom", 1080, 1080, "Free input", 10, 500, "Custom")
        )
    }

    // Set default preset
    LaunchedEffect(Unit) {
        if (selectedPreset == null) {
            selectedPreset = presets.last()
        }
    }

    // Live calculation effect
    LaunchedEffect(originalBitmap, widthInput, heightInput, targetSizeKbInput, selectedUnit, addNameDate, candidateName, dateOfPhoto, selectedFormat) {
        val bitmap = originalBitmap ?: return@LaunchedEffect
        val w = widthInput.toIntOrNull() ?: 100
        val h = heightInput.toIntOrNull() ?: 100
        val targetKb = targetSizeKbInput.toIntOrNull() ?: 200
        val dpi = dpiInput.toIntOrNull() ?: 200

        // 1. Calculate pixel dimensions
        val pixelWidth = when (selectedUnit) {
            DimensionUnit.PX -> w.coerceIn(10, 4096)
            DimensionUnit.CM -> (w * (dpi / 2.54f)).toInt().coerceIn(10, 4096)
            DimensionUnit.MM -> (w * (dpi / 25.4f)).toInt().coerceIn(10, 4096)
            DimensionUnit.INCH -> (w * dpi).toInt().coerceIn(10, 4096)
        }
        val pixelHeight = when (selectedUnit) {
            DimensionUnit.PX -> h.coerceIn(10, 4096)
            DimensionUnit.CM -> (h * (dpi / 2.54f)).toInt().coerceIn(10, 4096)
            DimensionUnit.MM -> (h * (dpi / 25.4f)).toInt().coerceIn(10, 4096)
            DimensionUnit.INCH -> (h * dpi).toInt().coerceIn(10, 4096)
        }

        val stripHeight = if (addNameDate && (!candidateName.isNullOrBlank() || !dateOfPhoto.isNullOrBlank())) {
            (pixelHeight * 0.12f).toInt()
        } else 0

        // Use a smaller preview bitmap for speed if original is huge
        val scaledBmp = Bitmap.createScaledBitmap(bitmap, pixelWidth, pixelHeight, true)
        val previewBitmap = if (stripHeight > 0) {
            Bitmap.createBitmap(pixelWidth, pixelHeight + stripHeight, Bitmap.Config.ARGB_8888).also {
                val canvas = android.graphics.Canvas(it)
                canvas.drawBitmap(scaledBmp, 0f, 0f, null)
                
                // Draw white strip
                val stripPaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.WHITE
                    style = android.graphics.Paint.Style.FILL
                }
                canvas.drawRect(0f, pixelHeight.toFloat(), pixelWidth.toFloat(), (pixelHeight + stripHeight).toFloat(), stripPaint)
                
                // Draw Name
                if (!candidateName.isNullOrBlank()) {
                    val namePaint = android.graphics.Paint().apply {
                        color = android.graphics.Color.BLACK
                        textSize = (stripHeight * 0.35f)
                        textAlign = android.graphics.Paint.Align.CENTER
                        isAntiAlias = true
                        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                    }
                    canvas.drawText(candidateName.uppercase(), (pixelWidth / 2).toFloat(), (pixelHeight + stripHeight * 0.45f).toFloat(), namePaint)
                }
                
                // Draw Date
                if (!dateOfPhoto.isNullOrBlank()) {
                    val datePaint = android.graphics.Paint().apply {
                        color = android.graphics.Color.BLACK
                        textSize = (stripHeight * 0.35f)
                        textAlign = android.graphics.Paint.Align.CENTER
                        isAntiAlias = true
                        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                    }
                    canvas.drawText(dateOfPhoto, (pixelWidth / 2).toFloat(), (pixelHeight + stripHeight * 0.85f).toFloat(), datePaint)
                }
            }
        } else {
            scaledBmp
        }

        // 2. Find quality for target size
        val bestQuality = ImageProcessingUtils.findQualityForTargetSize(previewBitmap, selectedFormat, targetKb)
        calculatedQuality = bestQuality
        qualitySlider = bestQuality.toFloat()

        // 3. Calculate estimated size
        val sizeBytes = ImageProcessingUtils.calculateCompressedSize(previewBitmap, selectedFormat, bestQuality)
        estimatedFileSizeKb = sizeBytes / 1024f
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            imageUri = uri
            lastResult = null
        }
    }

    // Load bitmap when imageUri changes
    LaunchedEffect(imageUri) {
        if (imageUri != null) {
            val (bmp, bytesSize) = ImageProcessingUtils.decodeBitmapFromUri(context, imageUri!!)
            if (bmp != null) {
                originalBitmap = bmp
                originalWidth = bmp.width
                originalHeight = bmp.height
                originalFileSizeKb = (bytesSize / 1024L).coerceAtLeast(1L)

                widthInput = bmp.width.toString()
                heightInput = bmp.height.toString()
            }
        }
    }

    AmbientLightingBackground {
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { DarkFrostedSnackbarHost(snackbarHostState) },
            modifier = modifier
                .fillMaxSize()
                .testTag("photo_resizer_screen")
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .windowInsetsPadding(WindowInsets.statusBars),
                contentPadding = PaddingValues(
                    bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 32.dp
                )
            ) {
                // Header
                item(key = "header") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(
                            onClick = onBackClick,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0x22FFFFFF))
                                .border(1.dp, GlassStroke, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBackIos,
                                contentDescription = "Back",
                                tint = TextPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Text(
                            text = "Photo Resizer & Compressor",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = TextPrimary
                            )
                        )

                        Spacer(modifier = Modifier.width(40.dp))
                    }
                }

                // Image Selection Dropzone / Preview
                item(key = "image_preview_box") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                    ) {
                        GlassCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .clickable { galleryLauncher.launch("image/*") }
                                .testTag("resizer_image_dropzone"),
                            shape = RoundedCornerShape(26.dp),
                            ambientGlowColor = AmberWarm
                        ) {
                            if (originalBitmap != null) {
                                if (isCropping) {
                                    val currentAspectRatio = remember(widthInput, heightInput) {
                                        val w = widthInput.toFloatOrNull() ?: 1f
                                        val h = heightInput.toFloatOrNull() ?: 1f
                                        if (h != 0f) w / h else null
                                    }
                                    InteractiveCropper(
                                        bitmap = originalBitmap!!,
                                        aspectRatio = currentAspectRatio,
                                        onCropApply = { croppedBmp: Bitmap ->
                                            originalBitmap = croppedBmp
                                            isCropping = false
                                        },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Box(modifier = Modifier.fillMaxSize()) {
                                        Image(
                                            bitmap = originalBitmap!!.asImageBitmap(),
                                            contentDescription = "Selected Photo",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Fit
                                        )
                                        
                                        // Metadata Badge
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopCenter)
                                                .padding(top = 16.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(Color(0xCC0F0E11))
                                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                text = "${widthInput}x${heightInput} ${selectedUnit.label} | ${dpiInput} DPI | Quality: ${calculatedQuality}%",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = WarmGold,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp
                                                )
                                            )
                                        }

                                        // Remove Photo 'X' Button
                                        IconButton(
                                            onClick = {
                                                imageUri = null
                                                originalBitmap = null
                                                lastResult = null
                                            },
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(12.dp)
                                                .size(36.dp)
                                                .background(Color(0x99000000), CircleShape)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Close,
                                                contentDescription = "Remove Photo",
                                                tint = Color.White,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        // Crop button
                                        IconButton(
                                            onClick = { isCropping = true },
                                            modifier = Modifier
                                                .align(Alignment.BottomEnd)
                                                .padding(12.dp)
                                                .background(Color(0x88000000), CircleShape)
                                        ) {
                                            Icon(imageVector = Icons.Rounded.Crop, contentDescription = "Crop", tint = Color.White)
                                        }
                                    }
                                }
                            } else {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
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
                                            imageVector = Icons.Rounded.Image,
                                            contentDescription = "Pick Photo",
                                            tint = Color.White,
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    Text(
                                        text = "Tap to select photo",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp,
                                            color = Color.White
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                // Preset selection dock
                item(key = "preset_dock") {
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 6.dp)
                            .clickable { showPresetsSheet = true },
                        shape = RoundedCornerShape(16.dp),
                        ambientGlowColor = AmberWarm
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Active Preset: ${selectedPreset?.name ?: "Custom"}",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
                                )
                                if (selectedPreset != null && selectedPreset?.name != "Custom") {
                                    Text(
                                        text = "Target: ${selectedPreset!!.minKb} KB - ${selectedPreset!!.maxKb} KB (${selectedPreset!!.label})",
                                        style = MaterialTheme.typography.labelSmall.copy(color = WarmGold, fontWeight = FontWeight.Medium)
                                    )
                                }
                            }
                            Icon(imageVector = Icons.Rounded.PhotoCamera, contentDescription = "Select", tint = Color.White)
                        }
                    }

                    if (showPresetsSheet) {
                        val sheetState = rememberModalBottomSheetState()
                        ModalBottomSheet(
                            onDismissRequest = { showPresetsSheet = false },
                            sheetState = sheetState
                        ) {
                            LazyColumn(modifier = Modifier.padding(16.dp)) {
                                items(presets) { preset ->
                                    ListItem(
                                        headlineContent = { Text(preset.name) },
                                        supportingContent = { Text(preset.description) },
                                        modifier = Modifier.clickable {
                                            selectedPreset = preset
                                            widthInput = preset.width.toString()
                                            heightInput = preset.height.toString()
                                            // Auto-tune to middle of range
                                            targetSizeKbInput = ((preset.minKb + preset.maxKb) / 2).toString()
                                            showPresetsSheet = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Controls Section matching screenshot
                item(key = "resizer_controls") {
                    Spacer(modifier = Modifier.height(8.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Unit, Format controls (No DPI as requested)
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(22.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Dimensions Unit & Format",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                Text(text = "Unit", style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontWeight = FontWeight.Bold))
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf(DimensionUnit.PX, DimensionUnit.CM, DimensionUnit.MM, DimensionUnit.INCH).forEach { unit ->
                                        val isSelected = selectedUnit == unit
                                        Box(
                                            modifier = Modifier
                                                .clip(CircleShape)
                                                .background(if (isSelected) AmberWarm.copy(alpha = 0.25f) else Color(0x12FFFFFF))
                                                .border(1.dp, if (isSelected) AmberWarm else Color.Transparent, CircleShape)
                                                .clickable { selectedUnit = unit }
                                                .padding(horizontal = 14.dp, vertical = 6.dp)
                                        ) {
                                            Text(text = unit.label, color = if (isSelected) AmberWarm else Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                Text(text = "Target Format", style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontWeight = FontWeight.Bold))
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf(OutputFormat.JPEG, OutputFormat.PNG, OutputFormat.WEBP, OutputFormat.SVG).forEach { fmt ->
                                        val isSelected = selectedFormat == fmt
                                        Box(
                                            modifier = Modifier
                                                .clip(CircleShape)
                                                .background(if (isSelected) AmberWarm.copy(alpha = 0.25f) else Color(0x12FFFFFF))
                                                .border(1.dp, if (isSelected) AmberWarm else Color.Transparent, CircleShape)
                                                .clickable { selectedFormat = fmt }
                                                .padding(horizontal = 14.dp, vertical = 6.dp)
                                        ) {
                                            Text(text = fmt.label, color = if (isSelected) AmberWarm else Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }


                        // DOP/DOB Strip toggle
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(22.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Add Name & Date Strip",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
                                    )
                                    androidx.compose.material3.Switch(
                                        checked = addNameDate,
                                        onCheckedChange = { addNameDate = it }
                                    )
                                }
                                if (addNameDate) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedTextField(value = candidateName, onValueChange = { candidateName = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedTextField(value = dateOfPhoto, onValueChange = { dateOfPhoto = it }, label = { Text("Date (e.g., 2026-09-26)") }, modifier = Modifier.fillMaxWidth())
                                }
                            }
                        }


                        // Target Dimensions (Width & Height) with Aspect Ratio Lock matching screenshot
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(22.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Target Dimensions (${selectedUnit.label})",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = Color.White
                                        )
                                    )

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .clickable { isAspectRatioLocked = !isAspectRatioLocked }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isAspectRatioLocked) Icons.Rounded.Lock else Icons.Rounded.LockOpen,
                                            contentDescription = "Lock Ratio",
                                            tint = Color.White,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (isAspectRatioLocked) "Locked" else "Free",
                                            fontSize = 12.sp,
                                            color = Color.White,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // Width input with amber glow border
                                    OutlinedTextField(
                                        value = widthInput,
                                        onValueChange = { newW ->
                                            widthInput = newW
                                            if (isAspectRatioLocked && originalWidth > 0 && originalHeight > 0) {
                                                val wVal = newW.toFloatOrNull() ?: 0f
                                                if (wVal > 0) {
                                                    val calculatedH = (wVal * originalHeight / originalWidth).toInt()
                                                    heightInput = calculatedH.toString()
                                                }
                                            }
                                        },
                                        label = { Text("Width", color = Color(0xB8C2CCFF)) },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = Color(0x1CFFFFFF),
                                            unfocusedContainerColor = Color(0x14FFFFFF),
                                            focusedBorderColor = AmberWarm,
                                            unfocusedBorderColor = GlassStroke,
                                            focusedLabelColor = AmberWarm,
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White
                                        ),
                                        shape = RoundedCornerShape(18.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("input_width")
                                    )

                                    // Height input with cyan glow border
                                    OutlinedTextField(
                                        value = heightInput,
                                        onValueChange = { newH ->
                                            heightInput = newH
                                            if (isAspectRatioLocked && originalWidth > 0 && originalHeight > 0) {
                                                val hVal = newH.toFloatOrNull() ?: 0f
                                                if (hVal > 0) {
                                                    val calculatedW = (hVal * originalWidth / originalHeight).toInt()
                                                    widthInput = calculatedW.toString()
                                                }
                                            }
                                        },
                                        label = { Text("Height", color = Color(0xB8C2CCFF)) },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = Color(0x1CFFFFFF),
                                            unfocusedContainerColor = Color(0x14FFFFFF),
                                            focusedBorderColor = Color(0xFF00E5FF),
                                            unfocusedBorderColor = GlassStroke,
                                            focusedLabelColor = Color(0xFF00E5FF),
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White
                                        ),
                                        shape = RoundedCornerShape(18.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("input_height")
                                    )
                                }
                            }
                        }

                        // Real DPI Setting Card
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(22.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Print Resolution (DPI)",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = Color.White
                                        )
                                    )
                                    Text(
                                        text = "${dpiInput} DPI",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            color = AmberWarm,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf("72", "96", "150", "200", "300").forEach { dpiVal ->
                                        val isSelected = dpiInput == dpiVal
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(CircleShape)
                                                .background(if (isSelected) AmberWarm.copy(alpha = 0.25f) else Color(0x12FFFFFF))
                                                .border(1.dp, if (isSelected) AmberWarm else Color.Transparent, CircleShape)
                                                .clickable { dpiInput = dpiVal }
                                                .padding(vertical = 8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = dpiVal,
                                                color = if (isSelected) AmberWarm else Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Target File Size Compression (KB) / Quality Slider matching screenshot
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(22.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Target File Size & Compression",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = Color.White
                                        )
                                    )
                                    
                                    // Live status indicator (Hidden if no image)
                                    if (originalBitmap != null) {
                                        val targetKbVal = targetSizeKbInput.toFloatOrNull() ?: 200f
                                        val isSafe = if (selectedPreset != null && selectedPreset?.name != "Custom") {
                                            estimatedFileSizeKb >= selectedPreset!!.minKb && estimatedFileSizeKb <= selectedPreset!!.maxKb
                                        } else {
                                            estimatedFileSizeKb <= targetKbVal
                                        }
                                        val statusColor = if (isSafe) Color(0xFF10B981) else Color(0xFFFF4D4D)
                                        val statusText = if (isSafe) "Optimal" else "Out of Range"
                                        
                                        Box(
                                            modifier = Modifier
                                                .clip(CircleShape)
                                                .background(statusColor.copy(alpha = 0.2f))
                                                .border(1.dp, statusColor.copy(alpha = 0.5f), CircleShape)
                                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                        ) {
                                            Text(
                                                text = statusText,
                                                color = statusColor,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))

                                // Interactive Target KB Slider
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Target Max: ${targetSizeKbInput} KB",
                                            style = MaterialTheme.typography.labelMedium.copy(color = TextSecondary)
                                        )
                                        Text(
                                            text = if (originalBitmap == null) "Est: -- KB" else "Est: ${String.format("%.1f", estimatedFileSizeKb)} KB",
                                            style = MaterialTheme.typography.labelMedium.copy(color = WarmGold, fontWeight = FontWeight.Bold)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    val sliderVal = targetSizeKbInput.toFloatOrNull() ?: 200f
                                    Slider(
                                        value = sliderVal,
                                        onValueChange = { targetSizeKbInput = it.toInt().toString() },
                                        valueRange = 10f..500f,
                                        colors = SliderDefaults.colors(
                                            thumbColor = Color.White,
                                            activeTrackColor = Color(0xFF00E5FF),
                                            inactiveTrackColor = Color(0x30FFFFFF)
                                        )
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Quick Compression Preset Buttons
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf("20" to "20 KB", "50" to "50 KB", "100" to "100 KB", "200" to "200 KB", "500" to "500 KB").forEach { (kb, label) ->
                                        val isSelected = targetSizeKbInput == kb
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(CircleShape)
                                                .background(
                                                    if (isSelected) Color(0x3500E5FF) else Color(0x1AFFFFFF)
                                                )
                                                .border(
                                                    1.dp,
                                                    if (isSelected) Color(0xFF00E5FF) else Color(0x30FFFFFF),
                                                    CircleShape
                                                )
                                                .clickable { targetSizeKbInput = kb }
                                                .padding(vertical = 8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = label,
                                                color = Color.White,
                                                fontWeight = FontWeight.Medium,
                                                fontSize = 10.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Success / Result Card (When generated)
                        AnimatedVisibility(
                            visible = lastResult != null,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            if (lastResult != null) {
                                GlassCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(20.dp),
                                    ambientGlowColor = SyncGreenGlow
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.CheckCircle,
                                                contentDescription = "Success",
                                                tint = Color(0xFF10B981),
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Column {
                                                Text(
                                                    text = "Processed: ${lastResult!!.outputWidth}x${lastResult!!.outputHeight} px",
                                                    color = TextPrimary,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp
                                                )
                                                Text(
                                                    text = "${lastResult!!.fileSizeKb} KB • ${lastResult!!.format.label} • ${lastResult!!.compressionRatioText}",
                                                    color = Color(0xFF10B981),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }

                                        IconButton(
                                            onClick = {
                                                ImageProcessingUtils.shareImage(
                                                    context,
                                                    lastResult!!.uri,
                                                    "Share Resized Photo"
                                                )
                                            },
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(Color(0x22FFFFFF))
                                                .border(1.dp, GlassStroke, CircleShape)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Share,
                                                contentDescription = "Share",
                                                tint = TextPrimary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Estimated size pill tag above buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Color(0x28FFFFFF))
                                    .border(1.dp, GlassStroke, CircleShape)
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = if (originalBitmap == null) "Estimated File Size: -- KB" else "Estimated File Size: ${String.format("%.1f", estimatedFileSizeKb)} KB",
                                    color = WarmGold,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Action Buttons: 'Compress & Download' (Cyan glow border) and 'Share' (White glass border)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1.3f)
                                    .height(54.dp)
                                    .clip(CircleShape)
                                    .background(if (originalBitmap != null) Color(0x3500E5FF) else Color(0x18FFFFFF))
                                    .border(
                                        1.5.dp,
                                        if (originalBitmap != null) Brush.horizontalGradient(listOf(Color(0xFF80EEFF), Color(0xFF5CE1E6))) else SolidColor(Color(0x28FFFFFF)),
                                        CircleShape
                                    )
                                    .clickable(enabled = !isProcessing) {
                                        if (originalBitmap == null) {
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar("Please select a photo first! 📸")
                                            }
                                            return@clickable
                                        }

                                        isProcessing = true
                                        coroutineScope.launch {
                                            val targetW = widthInput.toIntOrNull() ?: originalBitmap!!.width
                                            val targetH = heightInput.toIntOrNull() ?: originalBitmap!!.height
                                            val targetKb = targetSizeKbInput.toIntOrNull() ?: 200
                                            val targetDpi = dpiInput.toIntOrNull() ?: 200

                                            val result = ImageProcessingUtils.resizeAndCompress(
                                                context = context,
                                                sourceBitmap = originalBitmap!!,
                                                unit = selectedUnit,
                                                targetWidthVal = targetW,
                                                targetHeightVal = targetH,
                                                targetKb = targetKb,
                                                quality = qualitySlider.toInt(),
                                                format = selectedFormat,
                                                dpi = targetDpi,
                                                candidateName = if (addNameDate) candidateName else null,
                                                candidateDate = if (addNameDate) dateOfPhoto else null
                                            )

                                            if (result != null) {
                                                lastResult = result
                                                val savedPublicUri = ImageProcessingUtils.saveImageToPublicGToolXFolder(
                                                    context = context,
                                                    sourceUri = result.uri,
                                                    fileNamePrefix = "Resized",
                                                    format = selectedFormat
                                                )

                                                val finalUri = savedPublicUri ?: result.uri
                                                val infoText = "Resized to ${result.outputWidth}x${result.outputHeight} px (${targetDpi} DPI) • ${result.fileSizeKb} KB (${result.format.label})"
                                                onSaveSuccess("Resized_${System.currentTimeMillis()}", finalUri.toString(), infoText, false)
                                                isProcessing = false
                                                snackbarHostState.showSnackbar("Saved to Gallery & Vault! 🖼️")
                                            } else {
                                                isProcessing = false
                                                snackbarHostState.showSnackbar("Failed to process image. Check dimensions.")
                                            }
                                        }
                                    }
                                    .testTag("btn_save_to_device"),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isProcessing) {
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Rounded.Download,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                            tint = if (originalBitmap != null) Color.White else Color(0x66FFFFFF)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Compress & Download",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = if (originalBitmap != null) Color.White else Color(0x66FFFFFF)
                                        )
                                    }
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .weight(0.9f)
                                    .height(54.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x1CFFFFFF))
                                    .border(1.dp, Color(0x40FFFFFF), CircleShape)
                                    .clickable(enabled = !isProcessing) {
                                        if (originalBitmap == null) {
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar("Please select a photo first! 📸")
                                            }
                                            return@clickable
                                        }

                                        isProcessing = true
                                        coroutineScope.launch {
                                            val targetW = widthInput.toIntOrNull() ?: originalBitmap!!.width
                                            val targetH = heightInput.toIntOrNull() ?: originalBitmap!!.height
                                            val targetKb = targetSizeKbInput.toIntOrNull() ?: 200
                                            val targetDpi = dpiInput.toIntOrNull() ?: 200

                                            val result = ImageProcessingUtils.resizeAndCompress(
                                                context = context,
                                                sourceBitmap = originalBitmap!!,
                                                unit = selectedUnit,
                                                targetWidthVal = targetW,
                                                targetHeightVal = targetH,
                                                targetKb = targetKb,
                                                quality = qualitySlider.toInt(),
                                                format = selectedFormat,
                                                dpi = targetDpi
                                            )

                                            isProcessing = false
                                            if (result != null) {
                                                lastResult = result
                                                ImageProcessingUtils.shareImage(context, result.uri, "Share Resized Image")
                                            } else {
                                                snackbarHostState.showSnackbar("Failed to process image.")
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Rounded.Share,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = if (originalBitmap != null) Color.White else Color(0x66FFFFFF)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Share",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = if (originalBitmap != null) Color.White else Color(0x66FFFFFF)
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
