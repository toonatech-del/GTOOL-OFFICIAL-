package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBackIos
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Flip
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.AmbientLightingBackground
import com.example.ui.components.DarkFrostedSnackbarHost
import com.example.ui.components.GlassCard
import com.example.ui.theme.AmberWarm
import com.example.ui.theme.GlassStroke
import com.example.ui.theme.SyncGreenGlow
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.ImageProcessingUtils
import com.example.util.OutputFormat
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IdStitcherScreen(
    onBackClick: () -> Unit,
    onSaveSuccess: (title: String, imageUri: String) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var frontBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var backBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isVerticalStitch by remember { mutableStateOf(true) }
    var isProcessing by remember { mutableStateOf(false) }
    var resultBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val frontLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                val (bmp, _) = ImageProcessingUtils.decodeBitmapFromUri(context, uri)
                frontBitmap = bmp
                resultBitmap = null
            }
        }
    }

    val backLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                val (bmp, _) = ImageProcessingUtils.decodeBitmapFromUri(context, uri)
                backBitmap = bmp
                resultBitmap = null
            }
        }
    }

    LaunchedEffect(frontBitmap, backBitmap, isVerticalStitch) {
        if (frontBitmap != null && backBitmap != null) {
            resultBitmap = stitchBitmaps(frontBitmap!!, backBitmap!!, isVerticalStitch)
        }
    }

    AmbientLightingBackground {
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { DarkFrostedSnackbarHost(snackbarHostState) },
            modifier = Modifier.fillMaxSize().testTag("id_stitcher_screen")
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                // Header
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
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

                        Spacer(modifier = Modifier.width(16.dp))

                        Text(
                            text = "ID Card Stitcher",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                color = TextPrimary
                            )
                        )
                    }
                }

                // Step 1: Front Capture
                item {
                    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)) {
                        Text(
                            text = "Step 1: ID Front Side",
                            style = MaterialTheme.typography.labelMedium.copy(color = TextSecondary)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        CaptureSlot(
                            bitmap = frontBitmap,
                            label = "Front Side",
                            onClick = { frontLauncher.launch("image/*") },
                            testTag = "stitcher_front_slot"
                        )
                    }
                }

                // Step 2: Back Capture
                item {
                    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)) {
                        Text(
                            text = "Step 2: ID Back Side",
                            style = MaterialTheme.typography.labelMedium.copy(color = TextSecondary)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        CaptureSlot(
                            bitmap = backBitmap,
                            label = "Back Side",
                            onClick = { backLauncher.launch("image/*") },
                            testTag = "stitcher_back_slot"
                        )
                    }
                }

                // Stitching Preview & Options
                if (frontBitmap != null && backBitmap != null) {
                    item {
                        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Preview & Orientation",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                                )
                                
                                TextButton(
                                    onClick = { isVerticalStitch = !isVerticalStitch },
                                    colors = ButtonDefaults.textButtonColors(contentColor = AmberWarm)
                                ) {
                                    Icon(imageVector = Icons.Rounded.Flip, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = if (isVerticalStitch) "Switch to Horizontal" else "Switch to Vertical")
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            resultBitmap?.let { bmp ->
                                GlassCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(300.dp),
                                    shape = RoundedCornerShape(24.dp)
                                ) {
                                    Image(
                                        bitmap = bmp.asImageBitmap(),
                                        contentDescription = "Stitched Preview",
                                        modifier = Modifier.fillMaxSize().padding(12.dp),
                                        contentScale = ContentScale.Fit
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 10.dp)
                                .height(56.dp)
                                .clip(CircleShape)
                                .background(Brush.horizontalGradient(listOf(Color(0xFF007AFF), Color(0xFF5AC8FA))))
                                .clickable(enabled = !isProcessing) {
                                    isProcessing = true
                                    coroutineScope.launch {
                                        val finalBmp = resultBitmap ?: return@launch
                                        // Store private scanned identity in secure sandbox vault
                                        val vaultFile = java.io.File(com.example.util.SecurityUtils.getSecureVaultDir(context), "ID_Stitched_${System.currentTimeMillis()}.jpg")
                                        try {
                                            vaultFile.outputStream().use { out ->
                                                finalBmp.compress(android.graphics.Bitmap.CompressFormat.JPEG, 95, out)
                                            }
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                        val savedUri = ImageProcessingUtils.saveImageToPublicGToolXFolder(
                                            context = context,
                                            sourceUri = ImageProcessingUtils.bitmapToTempUri(context, finalBmp),
                                            fileNamePrefix = "ID_Stitched",
                                            format = OutputFormat.JPEG
                                        )
                                        if (savedUri != null) {
                                            onSaveSuccess("ID_Stitched_${System.currentTimeMillis()}", savedUri.toString())
                                            snackbarHostState.showSnackbar("Stitched ID saved to Gallery! 🆔")
                                        }
                                        isProcessing = false
                                    }
                                }
                                .testTag("stitcher_save_btn"),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isProcessing) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Rounded.Download, contentDescription = null, tint = Color.White)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Save Stitched ID Card", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CaptureSlot(
    bitmap: Bitmap?,
    label: String,
    onClick: () -> Unit,
    testTag: String
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .clickable { onClick() }
            .testTag(testTag),
        shape = RoundedCornerShape(20.dp),
        ambientGlowColor = if (bitmap != null) Color(0xFF007AFF) else Color.Transparent
    ) {
        if (bitmap != null) {
            Box(modifier = Modifier.fillMaxSize()) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = label,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF007AFF)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Rounded.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(imageVector = Icons.Rounded.AddPhotoAlternate, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(32.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Capture $label", style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary))
            }
        }
    }
}

private fun stitchBitmaps(front: Bitmap, back: Bitmap, vertical: Boolean): Bitmap {
    val width: Int
    val height: Int
    
    if (vertical) {
        width = maxOf(front.width, back.width)
        height = front.height + back.height + 40 // padding
    } else {
        width = front.width + back.width + 40 // padding
        height = maxOf(front.height, back.height)
    }

    val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(result)
    canvas.drawColor(AndroidColor.WHITE)
    
    val paint = Paint().apply { isFilterBitmap = true }
    
    if (vertical) {
        canvas.drawBitmap(front, (width - front.width) / 2f, 0f, paint)
        canvas.drawBitmap(back, (width - back.width) / 2f, (front.height + 40).toFloat(), paint)
    } else {
        canvas.drawBitmap(front, 0f, (height - front.height) / 2f, paint)
        canvas.drawBitmap(back, (front.width + 40).toFloat(), (height - back.height) / 2f, paint)
    }
    
    return result
}
