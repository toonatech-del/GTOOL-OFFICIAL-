package com.example.ui.components

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AccountBox
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Collections
import androidx.compose.material.icons.rounded.Crop
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.rememberAsyncImagePainter
import com.example.model.ItemType
import com.example.model.WorkspaceItem
import androidx.compose.ui.graphics.SolidColor
import com.example.ui.theme.AmberGlow
import com.example.ui.theme.AmberWarm
import com.example.ui.theme.GlassStroke
import com.example.ui.theme.ImagePurple
import com.example.ui.theme.NoteYellow
import com.example.ui.theme.PdfRed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarmGold
import com.example.util.PdfPageRenderer

enum class GalleryFilter(
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    ALL("All Media", Icons.Rounded.Description),
    PHOTOS("Photos & Scans", Icons.Rounded.Image),
    DOCUMENTS("PDFs", Icons.Rounded.PictureAsPdf),
    NOTES("Notes", Icons.Rounded.EditNote)
}

@Composable
fun GlassyVaultPhotoIcon(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(72.dp)
            .background(
                color = Color(0x1AFFFFFF), // Sleek subtle frosted glass
                shape = RoundedCornerShape(20.dp)
            )
            .border(
                width = 1.dp,
                color = Color(0x2EFFFFFF),
                shape = RoundedCornerShape(20.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.Image,
            contentDescription = "No items",
            tint = Color(0xCCFFFFFF),
            modifier = Modifier.size(32.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryVaultSheet(
    items: List<WorkspaceItem>,
    onItemClick: (WorkspaceItem) -> Unit,
    onDeleteItem: (WorkspaceItem) -> Unit,
    onDismiss: () -> Unit,
    onUploadPdf: (Uri) -> Unit = {},
    onUploadImage: (Uri) -> Unit = {},
    modifier: Modifier = Modifier
) {
    com.example.util.PreventScreenCapture()

    var selectedFilter by remember { mutableStateOf(GalleryFilter.ALL) }
    var galleryQuery by remember { mutableStateOf("") }
    var itemToDelete by remember { mutableStateOf<WorkspaceItem?>(null) }
    var previewingItem by remember { mutableStateOf<WorkspaceItem?>(null) }
    var showUploadModal by remember { mutableStateOf(false) }

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

    androidx.activity.compose.BackHandler {
        onDismiss()
    }

    if (itemToDelete != null) {
        DeleteConfirmationDialog(
            item = itemToDelete!!,
            onConfirmDelete = {
                onDeleteItem(itemToDelete!!)
                itemToDelete = null
            },
            onDismiss = { itemToDelete = null }
        )
    }

    if (previewingItem != null) {
        RealFilePreviewDialog(
            item = previewingItem!!,
            onOpenFullStudio = {
                val selected = previewingItem!!
                previewingItem = null
                onItemClick(selected)
            },
            onDismiss = { previewingItem = null }
        )
    }

    val filteredItems = items.filter { item ->
        val matchesFilter = when (selectedFilter) {
            GalleryFilter.ALL -> true
            GalleryFilter.PHOTOS -> item.type == ItemType.IMAGE
            GalleryFilter.DOCUMENTS -> item.type == ItemType.PDF
            GalleryFilter.NOTES -> item.type == ItemType.NOTE || item.type == ItemType.VOICE
        }
        val matchesQuery = if (galleryQuery.isBlank()) true else {
            item.title.contains(galleryQuery, ignoreCase = true) ||
                    item.summary.contains(galleryQuery, ignoreCase = true) ||
                    item.subtitle.contains(galleryQuery, ignoreCase = true)
        }
        matchesFilter && matchesQuery
    }

    AmbientLightingBackground {
        Box(
            modifier = modifier
                .fillMaxSize()
                .testTag("gallery_vault_sheet")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.statusBars)
            ) {
                // Top Header Row with subtle floating action icons on sides & centered Gallery X title
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    // Back Navigation Button on top left
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0x20FFFFFF))
                            .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                            contentDescription = "Back",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Center Gallery X Cursive Branding
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Gallery X",
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 52.sp,
                                letterSpacing = (-0.5).sp,
                                fontFamily = FontFamily.Cursive,
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xFF5CE1E6), // Cyan
                                        Color(0xFFE052A0), // Magenta / Pink
                                        Color(0xFFF7B070)  // Peach / Gold
                                    )
                                )
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${filteredItems.size} items stored offline & synced",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color(0xB8C2CCFF),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Normal
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Filter Pills Row matching screenshot
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(GalleryFilter.entries.toTypedArray()) { filter ->
                        val isSelected = selectedFilter == filter
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) Color(0x3500E5FF)
                                    else Color(0x1AFFFFFF)
                                )
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    brush = if (isSelected) {
                                        Brush.horizontalGradient(
                                            listOf(Color(0xFF80EEFF), Color(0xFFF7B070))
                                        )
                                    } else {
                                        SolidColor(Color(0x35FFFFFF))
                                    },
                                    shape = CircleShape
                                )
                                .clickable { selectedFilter = filter }
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = filter.icon,
                                    contentDescription = null,
                                    tint = if (isSelected) Color(0xFF80EEFF) else Color.White.copy(alpha = 0.85f),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = filter.label,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = Color.White,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                        fontSize = 13.sp
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Media Grid or Center Empty State matching reference image
                if (filteredItems.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(bottom = 60.dp)
                        ) {
                            GlassyVaultPhotoIcon()
                            Spacer(modifier = Modifier.height(20.dp))
                            Text(
                                text = "No items match filter",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFFE8EEF2),
                                    fontSize = 20.sp
                                )
                            )
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 150.dp),
                        contentPadding = PaddingValues(
                            start = 20.dp,
                            end = 20.dp,
                            top = 8.dp,
                            bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 32.dp
                        ),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        gridItems(filteredItems, key = { it.id }) { item ->
                            GalleryGridCard(
                                item = item,
                                onClick = { previewingItem = item },
                                onPreviewClick = { previewingItem = item },
                                onDeleteClick = { itemToDelete = item }
                            )
                        }
                    }
                }
            }
        }
    }

    // Bottom Sheet for Uploading PDF or Image
    if (showUploadModal) {
        ModalBottomSheet(
            onDismissRequest = { showUploadModal = false },
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
                    text = "Upload Real File",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Uploaded documents are stored offline and indexed with ML Kit OCR.",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 12.sp)
                )
                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Upload PDF Option
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color(0x22FF5252))
                            .border(1.dp, PdfRed.copy(alpha = 0.6f), RoundedCornerShape(18.dp))
                            .clickable {
                                showUploadModal = false
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
                                text = "Upload PDF",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            )
                            Text(
                                text = "Contracts, Bills",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }

                    // Upload Image Option
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color(0x22C084FC))
                            .border(1.dp, ImagePurple.copy(alpha = 0.6f), RoundedCornerShape(18.dp))
                            .clickable {
                                showUploadModal = false
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
                                text = "Upload Photo",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            )
                            Text(
                                text = "Receipts, Scans",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun GalleryGridCard(
    item: WorkspaceItem,
    onClick: () -> Unit,
    onPreviewClick: () -> Unit = onClick,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (typeColor, typeIcon, badgeLabel) = when {
        item.type == ItemType.PDF -> Triple(PdfRed, Icons.Rounded.PictureAsPdf, "PDF")
        item.type == ItemType.NOTE -> Triple(NoteYellow, Icons.Rounded.Description, "Note")
        item.type == ItemType.VOICE -> Triple(AmberWarm, Icons.Rounded.GraphicEq, "Voice")
        item.title.contains("Passport", ignoreCase = true) || item.summary.contains("Passport", ignoreCase = true) -> Triple(WarmGold, Icons.Rounded.Image, "Passport")
        item.title.contains("Resized", ignoreCase = true) || item.summary.contains("Resized", ignoreCase = true) -> Triple(ImagePurple, Icons.Rounded.Image, "Resized")
        item.title.contains("Scan", ignoreCase = true) || item.summary.contains("Scan", ignoreCase = true) -> Triple(Color(0xFF00E5FF), Icons.Rounded.Image, "Scan")
        else -> Triple(ImagePurple, ModernGalleryIconVector, "Photo")
    }

    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("gallery_card_${item.id}"),
        shape = RoundedCornerShape(20.dp),
        onClick = onClick,
        ambientGlowColor = typeColor
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Thumbnail / Icon Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.1f)
                    .background(Color(0x18FFFFFF)),
                contentAlignment = Alignment.Center
            ) {
                val isPdfFile = item.imageUri?.endsWith(".pdf", ignoreCase = true) == true
                if (!item.imageUri.isNullOrBlank() && !isPdfFile) {
                    Image(
                        painter = rememberAsyncImagePainter(model = item.imageUri),
                        contentDescription = item.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(typeColor.copy(alpha = 0.22f))
                            .border(1.dp, typeColor.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = typeIcon,
                            contentDescription = null,
                            tint = typeColor,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                // Type Badge Top-Left
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xEE0F0E11))
                        .border(1.dp, typeColor.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = badgeLabel,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = typeColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    )
                }

                // Delete Button Top-Right
                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color(0xDDFF3B30))
                        .border(1.dp, Color.White.copy(alpha = 0.6f), CircleShape)
                        .testTag("btn_delete_gallery_${item.id}")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = "Delete Item",
                        tint = Color.White,
                        modifier = Modifier.size(15.dp)
                    )
                }

                // Preview Badge Bottom-Right
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xCC000000))
                        .border(1.dp, GlassStroke, RoundedCornerShape(8.dp))
                        .clickable(onClick = onPreviewClick)
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Visibility,
                            contentDescription = "Preview",
                            tint = WarmGold,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "Preview",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = TextPrimary,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }

            // Info Container
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = TextPrimary
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = item.dateModified,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextMuted,
                        fontSize = 11.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))
                // Sync status indicator pill removed
            }
        }
    }
}

/**
 * Interactive Real File Preview Dialog
 * Displays real multi-page PDF rendering and high-res uploaded image viewer
 */
@Composable
fun RealFilePreviewDialog(
    item: WorkspaceItem,
    onOpenFullStudio: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var pdfPages by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    var isLoadingPages by remember { mutableStateOf(item.type == ItemType.PDF) }
    var currentPageIndex by remember { mutableIntStateOf(0) }

    val previewFileUri = item.driveFileId ?: item.imageUri

    LaunchedEffect(item.id, previewFileUri) {
        if (item.type == ItemType.PDF && !previewFileUri.isNullOrBlank()) {
            isLoadingPages = true
            try {
                val pages = PdfPageRenderer.renderPdfPages(context, Uri.parse(previewFileUri), maxPages = 8)
                pdfPages = pages
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoadingPages = false
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xCC000000))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.96f)
                    .fillMaxHeight(0.92f)
                    .clip(RoundedCornerShape(26.dp))
                    .background(Color(0xF51E1B24))
                    .border(1.5.dp, GlassStroke, RoundedCornerShape(26.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {} // prevent dismissing on dialog body click
                    )
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Dialog Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 14.dp),
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
                                    .clip(CircleShape)
                                    .background(Color(0x28FF9E58))
                                    .border(1.dp, AmberWarm.copy(alpha = 0.5f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = when (item.type) {
                                        ItemType.PDF -> Icons.Rounded.PictureAsPdf
                                        ItemType.IMAGE -> Icons.Rounded.Image
                                        ItemType.NOTE -> Icons.Rounded.Description
                                        ItemType.VOICE -> Icons.Rounded.Mic
                                        else -> Icons.Rounded.Description
                                    },
                                    contentDescription = null,
                                    tint = WarmGold,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${item.type.label} • ${item.sizeText} • ${item.dateModified}",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = TextMuted,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0x22FFFFFF))
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                                contentDescription = "Back",
                                tint = TextPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Content Viewer Area
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .background(Color(0x22000000))
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        when (item.type) {
                            ItemType.PDF -> {
                                if (isLoadingPages) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        CircularProgressIndicator(color = AmberWarm, modifier = Modifier.size(36.dp))
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text("Rendering high-res PDF pages...", color = TextSecondary, fontSize = 12.sp)
                                    }
                                } else if (pdfPages.isNotEmpty()) {
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(12.dp))
                                                .border(1.dp, GlassStroke, RoundedCornerShape(12.dp))
                                                .background(Color.White),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            val safeIndex = currentPageIndex.coerceIn(0, pdfPages.size - 1)
                                            Image(
                                                bitmap = pdfPages[safeIndex].asImageBitmap(),
                                                contentDescription = "PDF Page ${safeIndex + 1}",
                                                contentScale = ContentScale.Fit,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }

                                        if (pdfPages.size > 1) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                IconButton(
                                                    onClick = { if (currentPageIndex > 0) currentPageIndex-- },
                                                    enabled = currentPageIndex > 0,
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                                                        contentDescription = "Previous page",
                                                        tint = if (currentPageIndex > 0) TextPrimary else TextMuted
                                                    )
                                                }
                                                Text(
                                                    text = "Page ${currentPageIndex + 1} of ${pdfPages.size}",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        color = WarmGold,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                )
                                                IconButton(
                                                    onClick = { if (currentPageIndex < pdfPages.size - 1) currentPageIndex++ },
                                                    enabled = currentPageIndex < pdfPages.size - 1,
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                                        contentDescription = "Next page",
                                                        tint = if (currentPageIndex < pdfPages.size - 1) TextPrimary else TextMuted
                                                    )
                                                }
                                            }
                                        }
                                    }
                                } else if (!item.imageUri.isNullOrBlank() && !item.imageUri.endsWith(".pdf", ignoreCase = true)) {
                                    // Fallback to page 1 thumbnail
                                    Image(
                                        painter = rememberAsyncImagePainter(model = item.imageUri),
                                        contentDescription = item.title,
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Text("PDF document ready for reading", color = TextSecondary, fontSize = 13.sp)
                                }
                            }
                            ItemType.IMAGE -> {
                                if (!item.imageUri.isNullOrBlank()) {
                                    Image(
                                        painter = rememberAsyncImagePainter(model = item.imageUri),
                                        contentDescription = item.title,
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(14.dp))
                                    )
                                } else {
                                    Text("Image preview not available", color = TextSecondary, fontSize = 13.sp)
                                }
                            }
                            ItemType.NOTE -> {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalScroll(rememberScrollState())
                                        .padding(12.dp)
                                ) {
                                    Text(
                                        text = item.summary,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = TextPrimary,
                                            fontSize = 14.sp,
                                            lineHeight = 22.sp
                                        )
                                    )
                                    if (!item.contentSnippet.isNullOrBlank() && item.contentSnippet != item.summary) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(
                                            text = item.contentSnippet,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = TextSecondary,
                                                fontSize = 12.sp,
                                                lineHeight = 18.sp
                                            )
                                        )
                                    }
                                }
                            }
                            ItemType.VOICE -> {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalScroll(rememberScrollState())
                                        .padding(12.dp)
                                ) {
                                    Text(
                                        text = "Audio Transcription",
                                        style = MaterialTheme.typography.labelSmall.copy(color = WarmGold, fontWeight = FontWeight.Bold)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = item.contentSnippet ?: item.summary,
                                        style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary, fontSize = 14.sp, lineHeight = 20.sp)
                                    )
                                }
                            }
                            else -> {
                                Text(item.summary, color = TextPrimary, fontSize = 13.sp)
                            }
                        }
                    }

                    // Bottom Action Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        /*Text(
                            text = if (item.isSyncedToDrive) "☁ Cloud Synced" else "📱 Local Offline Storage",
                            style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 11.sp)
                        )*/
                        Spacer(modifier = Modifier.weight(1f))

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(Brush.horizontalGradient(listOf(AmberWarm, AmberGlow)))
                                .border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                                .clickable(onClick = onOpenFullStudio)
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.IosShare,
                                    contentDescription = null,
                                    tint = Color(0xFF0F0E11),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Open in Studio",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = Color(0xFF0F0E11),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
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
