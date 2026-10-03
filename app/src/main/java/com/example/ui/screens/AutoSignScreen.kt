package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path as AndroidPath
import android.graphics.Color as AndroidColor
import android.graphics.RectF
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBackIos
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
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
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoSignScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var paths = remember { mutableStateListOf<List<Offset>>() }
    var currentPath = remember { mutableStateListOf<Offset>() }
    
    var savedSignatures by remember { mutableStateOf<List<File>>(emptyList()) }
    var selectedSignature by remember { mutableStateOf<File?>(null) }
    
    var documentToSignUri by remember { mutableStateOf<Uri?>(null) }
    var documentBitmap by remember { mutableStateOf<Bitmap?>(null) }
    
    val signDir = File(context.filesDir, "signatures")
    if (!signDir.exists()) signDir.mkdirs()

    fun loadSignatures() {
        savedSignatures = signDir.listFiles()?.filter { it.extension == "png" }?.toList() ?: emptyList()
    }

    LaunchedEffect(Unit) {
        loadSignatures()
    }

    val docPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            documentToSignUri = uri
            coroutineScope.launch {
                val (bmp, _) = ImageProcessingUtils.decodeBitmapFromUri(context, uri)
                documentBitmap = bmp
            }
        }
    }

    AmbientLightingBackground {
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { DarkFrostedSnackbarHost(snackbarHostState) },
            modifier = Modifier.fillMaxSize().testTag("auto_sign_screen")
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                // ... header same ...
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
                            text = "Auto Sign & Stamp",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                color = TextPrimary
                            )
                        )
                    }
                }

                // Signature Canvas
                item {
                    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                        Text(
                            text = "Draw your signature below:",
                            style = MaterialTheme.typography.labelMedium.copy(color = TextSecondary)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(380.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(Color.White)
                                .border(1.dp, GlassStroke, RoundedCornerShape(24.dp))
                                .pointerInput(Unit) {
                                    detectDragGestures(
                                        onDragStart = { offset ->
                                            currentPath = mutableStateListOf(offset)
                                            paths.add(currentPath)
                                        },
                                        onDrag = { change, _ ->
                                            currentPath.add(change.position)
                                        }
                                    )
                                }
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                paths.forEach { pathPoints ->
                                    if (pathPoints.size > 1) {
                                        val path = Path()
                                        path.moveTo(pathPoints[0].x, pathPoints[0].y)
                                        for (i in 1 until pathPoints.size) {
                                            path.lineTo(pathPoints[i].x, pathPoints[i].y)
                                        }
                                        drawPath(
                                            path = path,
                                            color = Color.Black,
                                            style = Stroke(
                                                width = 6f,
                                                cap = StrokeCap.Round,
                                                join = StrokeJoin.Round
                                            )
                                        )
                                    }
                                }
                            }
                            
                            // Canvas Actions
                            Row(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FloatingActionButton(
                                    onClick = { paths.clear() },
                                    containerColor = Color.LightGray.copy(alpha = 0.8f),
                                    contentColor = Color.Black,
                                    modifier = Modifier.size(40.dp),
                                    shape = CircleShape,
                                    elevation = FloatingActionButtonDefaults.elevation(0.dp)
                                ) {
                                    Icon(Icons.Rounded.DeleteSweep, "Clear", modifier = Modifier.size(20.dp))
                                }
                                
                                FloatingActionButton(
                                    onClick = {
                                        if (paths.isEmpty()) return@FloatingActionButton
                                        coroutineScope.launch {
                                            val bmp = createBitmapFromPoints(paths, 600, 300)
                                            val file = File(signDir, "sign_${System.currentTimeMillis()}.png")
                                            FileOutputStream(file).use { out ->
                                                bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
                                            }
                                            loadSignatures()
                                            paths.clear()
                                            snackbarHostState.showSnackbar("Signature saved to vault! 🖋️")
                                        }
                                    },
                                    containerColor = Color(0xFF4CD964),
                                    contentColor = Color.White,
                                    modifier = Modifier.size(40.dp),
                                    shape = CircleShape,
                                    elevation = FloatingActionButtonDefaults.elevation(0.dp)
                                ) {
                                    Icon(Icons.Rounded.Check, "Save", modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }

                // Signature Vault
                if (savedSignatures.isNotEmpty()) {
                    item {
                        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp)) {
                            Text(
                                text = "Signature Studio",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                savedSignatures.take(4).forEach { file ->
                                    val isSelected = selectedSignature == file
                                    Box(
                                        modifier = Modifier
                                            .size(80.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(if (isSelected) Color(0x334CD964) else Color(0x22FFFFFF))
                                            .border(1.dp, if (isSelected) Color(0xFF4CD964) else GlassStroke, RoundedCornerShape(16.dp))
                                            .clickable { selectedSignature = file }
                                            .padding(8.dp)
                                    ) {
                                        androidx.compose.foundation.Image(
                                            bitmap = android.graphics.BitmapFactory.decodeFile(file.absolutePath).asImageBitmap(),
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Fit
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Apply to Document Section
                item {
                    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                        Text(
                            text = "Quick Self-Attest Document",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        GlassCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clickable { docPicker.launch("image/*") },
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            if (documentBitmap != null) {
                                androidx.compose.foundation.Image(
                                    bitmap = documentBitmap!!.asImageBitmap(),
                                    contentDescription = "Selected Doc",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit
                                )
                            } else {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(Icons.Rounded.AddLink, null, tint = TextSecondary, modifier = Modifier.size(32.dp))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Pick Document/Image", color = TextSecondary)
                                }
                            }
                        }
                        
                        if (documentBitmap != null && selectedSignature != null) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        val docBmp = documentBitmap!!
                                        val signBmp = android.graphics.BitmapFactory.decodeFile(selectedSignature!!.absolutePath)
                                        val result = overlaySignature(docBmp, signBmp)
                                        
                                        val savedUri = ImageProcessingUtils.saveImageToPublicGToolXFolder(
                                            context = context,
                                            sourceUri = ImageProcessingUtils.bitmapToTempUri(context, result),
                                            fileNamePrefix = "Attested",
                                            format = OutputFormat.JPEG
                                        )
                                        if (savedUri != null) {
                                            snackbarHostState.showSnackbar("Self-Attested document saved! ✅")
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CD964))
                            ) {
                                Icon(Icons.Rounded.HistoryEdu, null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Apply Signature & Save", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun createBitmapFromPoints(paths: List<List<Offset>>, width: Int = 800, height: Int = 400): Bitmap {
    val allPoints = paths.flatten()
    if (allPoints.isEmpty()) return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

    val minX = allPoints.minOf { it.x }
    val maxX = allPoints.maxOf { it.x }
    val minY = allPoints.minOf { it.y }
    val maxY = allPoints.maxOf { it.y }

    val boundsW = (maxX - minX).coerceAtLeast(1f)
    val boundsH = (maxY - minY).coerceAtLeast(1f)

    val padding = 24f
    val outWidth = (boundsW + padding * 2).toInt().coerceAtLeast(300)
    val outHeight = (boundsH + padding * 2).toInt().coerceAtLeast(150)

    val bitmap = Bitmap.createBitmap(outWidth, outHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    
    val paint = Paint().apply {
        color = AndroidColor.BLACK
        style = Paint.Style.STROKE
        strokeWidth = 8f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        isAntiAlias = true
    }

    paths.forEach { points ->
        if (points.size > 1) {
            val androidPath = AndroidPath()
            androidPath.moveTo(points[0].x - minX + padding, points[0].y - minY + padding)
            for (i in 1 until points.size) {
                androidPath.lineTo(points[i].x - minX + padding, points[i].y - minY + padding)
            }
            canvas.drawPath(androidPath, paint)
        }
    }
    
    return bitmap
}

// Re-implementing with points for better bitmap capture
private fun overlaySignature(doc: Bitmap, sign: Bitmap): Bitmap {
    val result = doc.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(result)
    
    val paint = Paint().apply { isFilterBitmap = true }
    
    // Draw "Self Attested" text
    val textPaint = Paint().apply {
        color = AndroidColor.BLUE
        textSize = (doc.height * 0.03f)
        isAntiAlias = true
        typeface = android.graphics.Typeface.DEFAULT_BOLD
    }
    
    // Position at bottom right
    val x = doc.width * 0.6f
    val y = doc.height * 0.85f
    
    canvas.drawText("Self Attested", x, y, textPaint)
    
    // Draw signature below text
    val signWidth = (doc.width * 0.3f).toInt()
    val signHeight = (signWidth * sign.height / sign.width)
    val scaledSign = Bitmap.createScaledBitmap(sign, signWidth, signHeight, true)
    
    canvas.drawBitmap(scaledSign, x, y + 10f, paint)
    
    return result
}
