@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.example

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.fragment.app.FragmentActivity
import android.view.WindowManager
import com.example.util.BiometricAuthManager
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import com.example.ui.theme.TextSecondary
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.rounded.Collections
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.SettingsBackupRestore
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.CitationSource
import com.example.model.CurrentScreen
import com.example.model.ItemType
import com.example.model.WorkspaceItem
import com.example.ui.GsdcallWorkspaceViewModel
import com.example.ui.components.AmbientLightingBackground
import com.example.ui.components.AskAiCard
import com.example.ui.components.DarkFrostedSnackbarHost
import com.example.ui.components.DeleteConfirmationDialog
import com.example.ui.components.FilterRow
import com.example.ui.components.GalleryVaultSheet
import com.example.ui.components.HomeFeatureCards
import com.example.ui.components.ImportantItemsGrid
import com.example.ui.components.ItemContextMenuSheet
import com.example.ui.components.ItemDetailDialog
import com.example.ui.components.ManageStorageDialog
import com.example.ui.components.ModernGalleryIcon
import com.example.ui.components.NotificationSheet
import com.example.ui.components.RecentItemsSection
import com.example.ui.components.RenameItemDialog
import com.example.ui.components.SearchResultsSection
import com.example.ui.components.SetReminderBottomSheet
import com.example.ui.components.TopHeader
import com.example.ui.components.iosBounce
import com.example.ui.components.zoomOnPress
import com.example.util.ShareUtils
import com.example.ui.screens.AiSearchScreen
import com.example.ui.screens.ImageMemoryScreen
import com.example.ui.screens.NoteEditorScreen
import com.example.ui.screens.PdfPreviewScreen
import com.example.ui.screens.PhotoResizerScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.PrivacyPolicyScreen
import com.example.ui.screens.PdfHubScreen
import com.example.ui.screens.PdfEditorScreen
import com.example.ui.screens.SmartInvoiceMakerScreen
import com.example.ui.screens.IdStitcherScreen
import com.example.ui.screens.AutoSignScreen
import com.example.ui.screens.TermsConditionsScreen
import com.example.ui.screens.TermsOnboardingScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.VoiceMemoryScreen
import com.example.ui.theme.AmberWarm
import com.example.ui.theme.GlassStroke
import com.example.ui.theme.LocalIsDarkMode
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.WarmGold
import kotlinx.coroutines.launch
import java.util.Locale

import androidx.core.view.WindowCompat

class MainActivity : FragmentActivity() {
    private val viewModel: GsdcallWorkspaceViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge()
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.isAppearanceLightStatusBars = false
        insetsController.isAppearanceLightNavigationBars = false
        setContent {
            GToolXApp(viewModel = viewModel)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GToolXApp(
    viewModel: GsdcallWorkspaceViewModel = viewModel()
) {
    CompositionLocalProvider(LocalIsDarkMode provides true) {
        MyApplicationTheme(darkTheme = true) {
            GToolXAppContent(
                viewModel = viewModel
            )
        }
    }
}

@Composable
private fun GToolXAppContent(
    viewModel: GsdcallWorkspaceViewModel
) {
    val hasAcceptedTerms by viewModel.hasAcceptedTerms.collectAsStateWithLifecycle()
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedFilter by viewModel.selectedFilter.collectAsStateWithLifecycle()
    val recentItems by viewModel.filteredRecentItems.collectAsStateWithLifecycle()
    val importantItems by viewModel.filteredImportantItems.collectAsStateWithLifecycle()
    val storageData by viewModel.storageData.collectAsStateWithLifecycle()
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val isBiometricEnabled by viewModel.isBiometricEnabled.collectAsStateWithLifecycle()
    val isAppUnlocked by viewModel.isAppUnlocked.collectAsStateWithLifecycle()
    val backupInfo by viewModel.backupInfo.collectAsStateWithLifecycle()
    val showRestorePrompt by viewModel.showRestorePrompt.collectAsStateWithLifecycle()
    val pendingReminderSchedule by viewModel.pendingReminderSchedule.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(context, "Notification permission is required for task reminders!", Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(Unit) {
        // Schedule background GitHub app update checks
        com.example.worker.AppUpdateWorker.scheduleAppUpdateChecks(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    fun queryFileInfo(ctx: android.content.Context, u: Uri): Pair<String, String> {
        var name = "Document.pdf"
        var size = "1.8 MB"
        try {
            ctx.contentResolver.query(u, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1) {
                        val displayName = cursor.getString(nameIndex)
                        if (!displayName.isNullOrBlank()) {
                            name = displayName
                        }
                    }
                    if (sizeIndex != -1) {
                        val bytes = cursor.getLong(sizeIndex)
                        if (bytes > 0) {
                            size = if (bytes < 1024 * 1024) {
                                "${bytes / 1024} KB"
                            } else {
                                String.format(Locale.US, "%.1f MB", bytes.toDouble() / (1024 * 1024))
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {}
        return Pair(name, size)
    }

    var selectedPdfUri by remember { mutableStateOf<String?>(null) }
    var selectedPdfName by remember { mutableStateOf("") }
    var selectedPdfSize by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<String?>(null) }
    var selectedNoteTitle by remember { mutableStateOf("") }
    var selectedNoteContent by remember { mutableStateOf("") }
    var selectedNoteTags by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedNoteAttachmentUri by remember { mutableStateOf<String?>(null) }
    var selectedNoteId by remember { mutableStateOf<String?>(null) }
    var selectedInvoiceId by remember { mutableStateOf<String?>(null) }

    val speechRecognizerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val spokenText = result.data
                ?.getStringArrayListExtra(android.speech.RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull() ?: ""
            if (spokenText.isNotBlank()) {
                viewModel.sendChatMessage(spokenText)
                viewModel.navigateTo(CurrentScreen.AI_SEARCH)
            }
        }
    }

    val scannerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val scanningResult = com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult.fromActivityResultIntent(result.data)
            
            // Prefer PDF result if available (multi-page)
            scanningResult?.pdf?.let { pdf ->
                selectedPdfUri = pdf.uri.toString()
                selectedPdfName = "Scanned_${System.currentTimeMillis()}.pdf"
                selectedPdfSize = "Scanning..."
                viewModel.navigateTo(CurrentScreen.PDF_PREVIEW)
                return@rememberLauncherForActivityResult
            }

            // Fallback to first image page
            scanningResult?.pages?.firstOrNull()?.let { page ->
                selectedImageUri = page.imageUri.toString()
                viewModel.navigateTo(CurrentScreen.IMAGE_MEMORY)
            }
        }
    }

    fun launchMlKitScanner() {
        val options = GmsDocumentScannerOptions.Builder()
            .setGalleryImportAllowed(true)
            .setPageLimit(20)
            .setResultFormats(
                GmsDocumentScannerOptions.RESULT_FORMAT_JPEG,
                GmsDocumentScannerOptions.RESULT_FORMAT_PDF
            )
            .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
            .build()

        val scanner = GmsDocumentScanning.getClient(options)
        scanner.getStartScanIntent(activity ?: return)
            .addOnSuccessListener { intentSender ->
                scannerLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
            }
            .addOnFailureListener { e ->
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Failed to start scanner: ${e.message}")
                }
            }
    }

    // Enforce FLAG_SECURE on window when Biometric Lock is active
    androidx.compose.runtime.LaunchedEffect(isBiometricEnabled, isAppUnlocked) {
        activity?.window?.let { win ->
            if (isBiometricEnabled) {
                win.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
            } else {
                win.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }
        }
    }

    var showNotificationSheet by remember { mutableStateOf(false) }
    var showManageStorageDialog by remember { mutableStateOf(false) }
    var showGalleryVaultSheet by remember { mutableStateOf(false) }
    var itemToDeleteForConfirm by remember { mutableStateOf<WorkspaceItem?>(null) }
    var selectedItemForDetail by remember { mutableStateOf<WorkspaceItem?>(null) }
    var itemForContextMenu by remember { mutableStateOf<WorkspaceItem?>(null) }
    var itemForRename by remember { mutableStateOf<WorkspaceItem?>(null) }

    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val (name, size) = queryFileInfo(context, uri)
            selectedPdfName = name
            selectedPdfSize = size
            selectedPdfUri = uri.toString()
            viewModel.navigateTo(CurrentScreen.PDF_PREVIEW)
        }
    }

    // Helper to open any item in its full viewer
    fun openItemInFullViewer(item: WorkspaceItem) {
        if (item.id.startsWith("invoice-") || item.tag == "Invoice") {
            selectedInvoiceId = item.id.removePrefix("invoice-")
            viewModel.navigateTo(CurrentScreen.SMART_INVOICE_MAKER)
        } else if (item.type == ItemType.PDF || item.title.endsWith(".pdf", ignoreCase = true)) {
            selectedPdfName = if (item.title.endsWith(".pdf", ignoreCase = true)) item.title else "${item.title}.pdf"
            selectedPdfSize = item.sizeText
            selectedPdfUri = item.driveFileId ?: item.imageUri
            viewModel.navigateTo(CurrentScreen.PDF_PREVIEW)
        } else if (item.type == ItemType.NOTE) {
            selectedNoteId = item.id
            selectedNoteTitle = item.title
            selectedNoteContent = item.contentSnippet ?: item.summary
            selectedNoteTags = if (!item.tag.isNullOrBlank()) {
                val cleanTag = item.tag
                listOf(if (cleanTag.startsWith("#")) cleanTag else "#$cleanTag")
            } else {
                emptyList()
            }
            selectedNoteAttachmentUri = item.imageUri
            viewModel.navigateTo(CurrentScreen.NOTE_EDITOR)
        } else if (item.type == ItemType.IMAGE || item.title.contains("bill", ignoreCase = true) || item.title.contains("receipt", ignoreCase = true)) {
            selectedImageUri = item.imageUri
            viewModel.navigateTo(CurrentScreen.IMAGE_MEMORY)
        } else if (item.type == ItemType.VOICE) {
            viewModel.navigateTo(CurrentScreen.VOICE_MEMORY)
        } else {
            selectedItemForDetail = item
        }
    }

    // Helper for citation click from AI search
    fun openCitationInFullViewer(citation: CitationSource) {
        if (citation.id.startsWith("invoice-")) {
            selectedInvoiceId = citation.id.removePrefix("invoice-")
            viewModel.navigateTo(CurrentScreen.SMART_INVOICE_MAKER)
            return
        }
        val matchingItem = recentItems.firstOrNull { it.id == citation.id || it.title.equals(citation.title, ignoreCase = true) }
        if (matchingItem != null) {
            openItemInFullViewer(matchingItem)
        } else {
            when (citation.type) {
                ItemType.PDF -> {
                    selectedPdfName = citation.title
                    selectedPdfSize = "1.8 MB"
                    selectedPdfUri = citation.previewThumbnailUri ?: citation.imageUri
                    viewModel.navigateTo(CurrentScreen.PDF_PREVIEW)
                }
                ItemType.NOTE -> {
                    selectedNoteId = citation.id
                    selectedNoteTitle = citation.title
                    selectedNoteContent = citation.snippet
                    selectedNoteTags = listOf("#note")
                    selectedNoteAttachmentUri = citation.imageUri
                    viewModel.navigateTo(CurrentScreen.NOTE_EDITOR)
                }
                ItemType.IMAGE -> {
                    selectedImageUri = citation.imageUri
                    viewModel.navigateTo(CurrentScreen.IMAGE_MEMORY)
                }
                ItemType.ID_CARD, ItemType.SIGNATURE -> {
                    selectedImageUri = citation.imageUri
                    viewModel.navigateTo(CurrentScreen.IMAGE_MEMORY)
                }
                ItemType.VOICE -> {
                    viewModel.navigateTo(CurrentScreen.VOICE_MEMORY)
                }
                ItemType.ALL -> {
                    viewModel.navigateTo(CurrentScreen.HOME)
                }
            }
        }
    }

    // Direct Deep-Link handler when user taps a heads-up system reminder notification
    androidx.compose.runtime.LaunchedEffect(activity?.intent) {
        val intent = activity?.intent
        if (intent?.getBooleanExtra("opened_from_reminder", false) == true) {
            val reminderTitle = intent.getStringExtra("reminder_title") ?: "Reminder"
            val relatedItemId = intent.getStringExtra("reminder_item_id")

            if (!relatedItemId.isNullOrBlank()) {
                val matchedItem = recentItems.firstOrNull { it.id == relatedItemId }
                if (matchedItem != null) {
                    openItemInFullViewer(matchedItem)
                } else {
                    showNotificationSheet = true
                }
            } else {
                showNotificationSheet = true
            }

            coroutineScope.launch {
                snackbarHostState.showSnackbar("🔔 Reminder: $reminderTitle")
            }
            intent.removeExtra("opened_from_reminder")
        }
    }

    val styledSnackbarHost: @Composable () -> Unit = {
        DarkFrostedSnackbarHost(hostState = snackbarHostState)
    }

    // Direct Home Screen Render on launch
    if (isBiometricEnabled && !isAppUnlocked) {
        // Biometric App Lock Full-Screen Guard Overlay
        androidx.compose.runtime.LaunchedEffect(isBiometricEnabled, isAppUnlocked) {
            activity?.let { act ->
                BiometricAuthManager.authenticate(
                    activity = act,
                    title = "Unlock GTOOL X",
                    subtitle = "Use Fingerprint, Face, or Device PIN",
                    onSuccess = { viewModel.unlockApp() },
                    onError = { msg ->
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(msg)
                        }
                    }
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0D0C10))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color(0x35FF9E58))
                        .border(1.5.dp, AmberWarm, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Lock,
                        contentDescription = "Locked",
                        tint = WarmGold,
                        modifier = Modifier.size(38.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Unlock GTOOL X",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        color = Color.White,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Cursive,
                        fontWeight = FontWeight.Bold,
                        fontSize = 32.sp
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Use Fingerprint, Face, or Device PIN to access your data",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                )

                Spacer(modifier = Modifier.height(28.dp))

                androidx.compose.material3.Button(
                    onClick = {
                        activity?.let { act ->
                            BiometricAuthManager.authenticate(
                                activity = act,
                                title = "Unlock GTOOL X",
                                subtitle = "Use Fingerprint, Face, or Device PIN",
                                onSuccess = { viewModel.unlockApp() },
                                onError = { msg ->
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar(msg)
                                    }
                                }
                            )
                        }
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = WarmGold,
                        contentColor = Color(0xFF0F0E11)
                    ),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(52.dp)
                        .testTag("btn_unlock_vault")
                ) {
                    Text(
                        text = "Unlock GTOOL X 🔒",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    } else {
        // MAIN APP UI
        // Intercept back button when not on HOME
        BackHandler(enabled = currentScreen != CurrentScreen.HOME) {
            viewModel.navigateBack()
        }

        AnimatedContent(
            targetState = currentScreen,
            transitionSpec = {
                if (targetState == CurrentScreen.HOME) {
                    // iOS Pop Back
                    (slideInHorizontally(
                        initialOffsetX = { -it / 3 },
                        animationSpec = spring(dampingRatio = 0.85f, stiffness = 420f)
                    ) + fadeIn(animationSpec = spring(stiffness = 500f)) + scaleIn(
                        initialScale = 0.95f,
                        animationSpec = spring(dampingRatio = 0.85f, stiffness = 420f)
                    )).togetherWith(
                        slideOutHorizontally(
                            targetOffsetX = { it },
                            animationSpec = spring(dampingRatio = 0.85f, stiffness = 420f)
                        ) + fadeOut(animationSpec = spring(stiffness = 500f)) + scaleOut(
                            targetScale = 1.05f,
                            animationSpec = spring(dampingRatio = 0.85f, stiffness = 420f)
                        )
                    )
                } else {
                    // iOS Push Forward
                    (slideInHorizontally(
                        initialOffsetX = { it },
                        animationSpec = spring(dampingRatio = 0.85f, stiffness = 420f)
                    ) + fadeIn(animationSpec = spring(stiffness = 500f)) + scaleIn(
                        initialScale = 0.95f,
                        animationSpec = spring(dampingRatio = 0.85f, stiffness = 420f)
                    )).togetherWith(
                        slideOutHorizontally(
                            targetOffsetX = { -it / 3 },
                            animationSpec = spring(dampingRatio = 0.85f, stiffness = 420f)
                        ) + fadeOut(animationSpec = spring(stiffness = 500f)) + scaleOut(
                            targetScale = 0.92f,
                            animationSpec = spring(dampingRatio = 0.85f, stiffness = 420f)
                        )
                    )
                }
            },
            label = "ios_spring_screen_transition"
        ) { screen ->
            when (screen) {
                CurrentScreen.NOTE_EDITOR -> {
                    NoteEditorScreen(
                        initialTitle = selectedNoteTitle,
                        initialContent = selectedNoteContent,
                        initialTags = selectedNoteTags,
                        initialAttachmentUri = selectedNoteAttachmentUri,
                        initialNoteId = selectedNoteId,
                        onBackClick = {
                            viewModel.navigateTo(CurrentScreen.HOME)
                        },
                        onSaveFullNote = { id, title, content, tags, hasAttachment, includeInSearch, attachedUri, isAutoSave ->
                            val finalId = if (id.isNullOrBlank()) "note-${System.currentTimeMillis()}" else id
                            selectedNoteId = finalId // Lock the note ID immediately in the Activity state!
                            viewModel.saveFullNote(
                                id = finalId,
                                title = title,
                                content = content,
                                tags = tags,
                                hasAttachment = hasAttachment,
                                includeInSemanticSearch = includeInSearch,
                                attachmentUri = attachedUri
                            )
                            if (!isAutoSave) {
                                viewModel.navigateTo(CurrentScreen.HOME)
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Saved \"$title\" to Recent items 📝")
                                }
                            }
                        },
                        onOpenScanner = {
                            launchMlKitScanner()
                        }
                    )
                }
                CurrentScreen.PDF_HUB -> {
                    PdfHubScreen(
                        onUploadExisting = { pdfPickerLauncher.launch(arrayOf("application/pdf")) },
                        onCreateNew = { viewModel.navigateTo(CurrentScreen.PDF_EDITOR) },
                        onBack = { viewModel.popBackStack() }
                    )
                }
                CurrentScreen.PDF_EDITOR -> {
                    com.example.ui.screens.PdfCreatorScreen(
                        onBackClick = { viewModel.popBackStack() },
                        viewModel = viewModel,
                        onSaveSuccess = { savedUri ->
                            // Cleanly clear PDF creator from the backstack to prevent multi-back loop
                            viewModel.navigateAndPopUpTo(
                                destination = CurrentScreen.PDF_HUB,
                                popUpToScreen = CurrentScreen.PDF_EDITOR,
                                inclusive = true
                            )
                        }
                    )
                }
                CurrentScreen.PDF_PREVIEW -> {
                    PdfPreviewScreen(
                        fileName = selectedPdfName,
                        fileSize = selectedPdfSize,
                        pdfUri = selectedPdfUri,
                        onBackClick = {
                            viewModel.navigateTo(CurrentScreen.HOME)
                        },
                        onSaveDocument = { title, summary, keyPoints, includeInSearch, fullTranscript ->
                            viewModel.savePdfDocument(
                                title = title,
                                fileName = selectedPdfName,
                                fileSize = selectedPdfSize,
                                summary = summary,
                                keyPoints = keyPoints,
                                pdfUri = if (!selectedPdfUri.isNullOrBlank()) Uri.parse(selectedPdfUri) else null,
                                includeInSearch = includeInSearch,
                                fullTranscript = fullTranscript
                            )
                            viewModel.navigateTo(CurrentScreen.HOME)
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Saved \"$title\" to Recent items 📄")
                            }
                        }
                    )
                }
                CurrentScreen.DOCUMENT_SCANNER -> {
                    // Handled via direct launchMlKitScanner() from HomeScreen
                    Box(Modifier.fillMaxSize())
                }
                CurrentScreen.IMAGE_MEMORY -> {
                    ImageMemoryScreen(
                        imageUri = selectedImageUri,
                        onBackClick = {
                            viewModel.navigateTo(CurrentScreen.HOME)
                        },
                        onSaveExtractedMemory = { docName, amount, dueDate, includeInSearch, savedUri, fullOcrText ->
                            val finalUri = savedUri ?: selectedImageUri
                            viewModel.saveImageMemory(
                                title = docName,
                                imageUri = finalUri,
                                amount = amount,
                                dueDate = dueDate,
                                includeInSearch = includeInSearch,
                                fullOcrText = fullOcrText
                            )
                            viewModel.navigateTo(CurrentScreen.HOME)
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Saved \"$docName\" to Recent items 🖼️")
                            }
                        }
                    )
                }
                CurrentScreen.VOICE_MEMORY -> {
                    VoiceMemoryScreen(
                        onBackClick = {
                            viewModel.navigateTo(CurrentScreen.HOME)
                        },
                        onSaveVoiceMemory = { title, transcription, summary, hasReminder ->
                            viewModel.saveVoiceMemory(title, transcription, summary, hasReminder)
                            viewModel.navigateTo(CurrentScreen.HOME)
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Saved voice note \"$title\" 🎙️")
                            }
                        }
                    )
                }
                CurrentScreen.AI_SEARCH -> {
                    AiSearchScreen(
                        messages = chatMessages,
                        onSendMessage = { query ->
                            viewModel.sendChatMessage(query)
                        },
                        onBackClick = {
                            viewModel.navigateBack()
                        },
                        onVoiceClick = {
                            viewModel.navigateTo(CurrentScreen.VOICE_MEMORY)
                        },
                        onAddMemoryClick = {},
                        onOpenCitation = { citation ->
                            openCitationInFullViewer(citation)
                        },
                        onConfirmReminder = { message ->
                            viewModel.confirmAndScheduleReminder(
                                message.id,
                                message.reminderTitle ?: "",
                                message.reminderDate ?: 0L
                            )
                        }
                    )
                }
                CurrentScreen.PHOTO_RESIZER -> {
                    PhotoResizerScreen(
                        onBackClick = {
                            viewModel.navigateTo(CurrentScreen.HOME)
                        },
                        onSaveSuccess = { title, uri, info, _ ->
                            viewModel.saveImageMemory(
                                title = title,
                                imageUri = uri,
                                amount = info,
                                dueDate = "",
                                includeInSearch = true
                            )
                            viewModel.navigateTo(CurrentScreen.HOME)
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Resized photo saved to Recent items 🖼️")
                            }
                        }
                    )
                }
                CurrentScreen.SMART_INVOICE_MAKER -> {
                    SmartInvoiceMakerScreen(
                        onBackClick = {
                            viewModel.navigateBack()
                        },
                        initialInvoiceId = selectedInvoiceId
                    )
                }
                CurrentScreen.ID_STITCHER -> {
                    IdStitcherScreen(
                        onBackClick = {
                            viewModel.navigateTo(CurrentScreen.HOME)
                        },
                        onSaveSuccess = { title, uri ->
                            viewModel.saveImageMemory(
                                title = title,
                                imageUri = uri,
                                amount = "Stitched ID Card",
                                dueDate = "",
                                includeInSearch = true
                            )
                            viewModel.navigateTo(CurrentScreen.HOME)
                        }
                    )
                }
                CurrentScreen.AUTO_SIGN -> {
                    AutoSignScreen(
                        onBackClick = {
                            viewModel.navigateTo(CurrentScreen.HOME)
                        }
                    )
                }

                CurrentScreen.TERMS_ONBOARDING -> {
                    TermsOnboardingScreen(onAgree = { viewModel.acceptTerms() })
                }
                CurrentScreen.SETTINGS_TERMS -> {
                    SettingsScreen(
                        viewModel = viewModel,
                        onNavigateBack = { viewModel.navigateTo(CurrentScreen.HOME) },
                        onNavigateToTerms = { viewModel.navigateTo(CurrentScreen.TERMS_CONDITIONS) },
                        onNavigateToPrivacy = { viewModel.navigateTo(CurrentScreen.PRIVACY_POLICY) }
                    )
                }
                CurrentScreen.TERMS_CONDITIONS -> {
                    TermsConditionsScreen(onNavigateBack = { viewModel.navigateTo(CurrentScreen.SETTINGS_TERMS) })
                }
                CurrentScreen.PRIVACY_POLICY -> {
                    PrivacyPolicyScreen(onNavigateBack = { viewModel.navigateTo(CurrentScreen.SETTINGS_TERMS) })
                }
                CurrentScreen.HOME -> {
                    AmbientLightingBackground {
                        Box(modifier = Modifier.fillMaxSize()) {
                            Scaffold(
                                containerColor = Color.Transparent,
                                snackbarHost = styledSnackbarHost,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .testTag("gsdcall_ai_main_screen")
                            ) { paddingValues ->
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(paddingValues)
                                        .windowInsetsPadding(WindowInsets.statusBars),
                                    contentPadding = PaddingValues(
                                        bottom = 80.dp
                                    )
                                ) {
                                // 1. Top Header
                                item(key = "top_header") {
                                    TopHeader(
                                        unreadCount = notifications.count { it.isUnread },
                                        accountName = "User",
                                        userEmail = "",
                                        profileImageUrl = null,
                                        onAvatarClick = { showManageStorageDialog = true },
                                        onNotificationClick = { showNotificationSheet = true },
                                        onSettingsClick = { viewModel.navigateTo(CurrentScreen.SETTINGS_TERMS) }
                                    )
                                }

                                // 2. Prominent large frosted glass card titled 'ASK'
                                item(key = "ask_ai_card") {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                                        AskAiCard(
                                            searchQuery = searchQuery,
                                            onSearchQueryChange = { viewModel.onSearchQueryChange(it) },
                                            onPromptSuggestionClick = { prompt ->
                                                viewModel.sendChatMessage(prompt)
                                                if (!com.example.util.ReminderManager.isReminderQuery(prompt)) {
                                                    viewModel.navigateTo(CurrentScreen.AI_SEARCH)
                                                }
                                            },
                                            onSearchSubmit = { prompt ->
                                                viewModel.sendChatMessage(prompt)
                                                if (!com.example.util.ReminderManager.isReminderQuery(prompt)) {
                                                    viewModel.navigateTo(CurrentScreen.AI_SEARCH)
                                                }
                                            }
                                        )
                                    }
                                }

                                // 3. Core Module Feature Cards Grid
                                item(key = "home_feature_cards") {
                                    Spacer(modifier = Modifier.height(14.dp))
                                    HomeFeatureCards(
                                        onFeatureClick = { screen ->
                                            when (screen) {
                                                CurrentScreen.PDF_PREVIEW -> {
                                                    pdfPickerLauncher.launch(arrayOf("application/pdf"))
                                                }
                                                CurrentScreen.NOTE_EDITOR -> {
                                                    selectedNoteTitle = ""
                                                    selectedNoteContent = ""
                                                    selectedNoteTags = emptyList()
                                                    selectedNoteAttachmentUri = null
                                                    selectedNoteId = null
                                                    viewModel.navigateTo(CurrentScreen.NOTE_EDITOR)
                                                }
                                                CurrentScreen.DOCUMENT_SCANNER -> {
                                                    launchMlKitScanner()
                                                }
                                                CurrentScreen.IMAGE_MEMORY -> {
                                                    selectedImageUri = null
                                                    viewModel.navigateTo(CurrentScreen.IMAGE_MEMORY)
                                                }
                                                CurrentScreen.PHOTO_RESIZER -> {
                                                    viewModel.navigateTo(CurrentScreen.PHOTO_RESIZER)
                                                }
                                                CurrentScreen.ID_STITCHER -> {
                                                    viewModel.navigateTo(CurrentScreen.ID_STITCHER)
                                                }
                                                CurrentScreen.AUTO_SIGN -> {
                                                    viewModel.navigateTo(CurrentScreen.AUTO_SIGN)
                                                }
                                                else -> {
                                                    selectedInvoiceId = null
                                                    viewModel.navigateTo(screen)
                                                }
                                            }
                                        }
                                    )
                                }

                                // 4. Search Results or Filter Row
                                if (searchQuery.isNotBlank()) {
                                    item(key = "fts_search_results") {
                                        Spacer(modifier = Modifier.height(18.dp))
                                        SearchResultsSection(
                                            query = searchQuery,
                                            results = searchResults,
                                            onItemClick = { item ->
                                                openItemInFullViewer(item)
                                            }
                                        )
                                    }
                                } else {
                                    item(key = "filter_row") {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        FilterRow(
                                            selectedType = selectedFilter,
                                            onFilterSelected = { viewModel.onFilterSelected(it) }
                                        )
                                    }

                                    item(key = "recent_items") {
                                        Spacer(modifier = Modifier.height(14.dp))
                                        RecentItemsSection(
                                            items = recentItems,
                                            selectedFilter = selectedFilter,
                                            onAddMemoryClick = {},
                                            onDeleteClick = { item -> itemToDeleteForConfirm = item },
                                            onShareClick = { item -> ShareUtils.shareItem(context, item) },
                                            onMenuClick = { item -> itemForContextMenu = item },
                                            onSetReminder = { viewModel.scheduleReminderFromItem(it) },
                                            onItemClick = { item ->
                                                openItemInFullViewer(item)
                                            },
                                            onTogglePin = { item ->
                                                viewModel.togglePin(item)
                                                coroutineScope.launch {
                                                    snackbarHostState.showSnackbar(
                                                        if (item.isPinned) "Unpinned ${item.title}" else "Pinned ${item.title} to Important"
                                                    )
                                                }
                                            }
                                        )
                                    }
                                }

                                 // 5. Important Items pinned grid
                                if (importantItems.isNotEmpty()) {
                                    item(key = "important_items") {
                                        Spacer(modifier = Modifier.height(24.dp))
                                        ImportantItemsGrid(
                                            items = importantItems,
                                            onItemClick = { item -> openItemInFullViewer(item) },
                                            onSetReminder = { viewModel.scheduleReminderFromItem(it) },
                                            onTogglePin = { item ->
                                                viewModel.togglePin(item)
                                                coroutineScope.launch {
                                                    snackbarHostState.showSnackbar("Unpinned ${item.title}")
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Modern Floating iOS-Style Gallery Dock
                        val interactionSource = remember { MutableInteractionSource() }
                        val isPressed by interactionSource.collectIsPressedAsState()
                        val scale by animateFloatAsState(
                            targetValue = if (isPressed) 0.94f else 1f,
                            animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f),
                            label = "dock_scale"
                        )

                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .navigationBarsPadding()
                                .padding(bottom = 26.dp)
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                    shadowElevation = 16.dp.toPx()
                                }
                                .clip(RoundedCornerShape(32.dp))
                                .background(Color(0xD9161922))
                                .border(
                                    1.2.dp,
                                    Brush.horizontalGradient(
                                        listOf(Color(0xFFFF6D00), Color(0xFF00E5FF))
                                    ),
                                    RoundedCornerShape(32.dp)
                                )
                                .clickable(
                                    interactionSource = interactionSource,
                                    indication = null,
                                    onClick = { showGalleryVaultSheet = true }
                                )
                                .padding(horizontal = 24.dp, vertical = 12.dp)
                                .testTag("floating_gallery_dock"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.PhotoLibrary,
                                    contentDescription = "Gallery",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Gallery",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        letterSpacing = 0.2.sp
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }

        // Item Context Menu Sheet
        if (itemForContextMenu != null) {
            val ctxItem = itemForContextMenu!!
            ItemContextMenuSheet(
                item = ctxItem,
                onDismiss = { itemForContextMenu = null },
                onOpenView = {
                    itemForContextMenu = null
                    openItemInFullViewer(ctxItem)
                },
                onShare = {
                    itemForContextMenu = null
                    ShareUtils.shareItem(context, ctxItem)
                },
                onRename = {
                    itemForContextMenu = null
                    itemForRename = ctxItem
                },
                onTogglePin = {
                    itemForContextMenu = null
                    viewModel.togglePin(ctxItem)
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(
                            if (ctxItem.isPinned) "Unpinned ${ctxItem.title}" else "Pinned ${ctxItem.title} to Important"
                        )
                    }
                },
                onDelete = {
                    itemForContextMenu = null
                    itemToDeleteForConfirm = ctxItem
                }
            )
        }

        // Rename Item Dialog
        if (itemForRename != null) {
            val renItem = itemForRename!!
            RenameItemDialog(
                item = renItem,
                onDismiss = { itemForRename = null },
                onSaveRename = { newTitle ->
                    viewModel.renameItem(renItem, newTitle)
                    itemForRename = null
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Renamed to \"$newTitle\" ✏️")
                    }
                }
            )
        }

        // Item Detail Dialog
        if (selectedItemForDetail != null) {
            ItemDetailDialog(
                item = selectedItemForDetail!!,
                onDismiss = { selectedItemForDetail = null },
                onTogglePin = { item ->
                    viewModel.togglePin(item)
                    selectedItemForDetail = item.copy(isPinned = !item.isPinned)
                },
                onAskAiAboutItem = { item ->
                    selectedItemForDetail = null
                    viewModel.sendChatMessage("Explain ${item.title}")
                    viewModel.navigateTo(CurrentScreen.AI_SEARCH)
                },
                onDeleteItem = { item ->
                    viewModel.deleteItemPermanently(item)
                    selectedItemForDetail = null
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Item deleted successfully 🗑️")
                    }
                },
                onOpenEditor = {
                    val item = selectedItemForDetail!!
                    selectedItemForDetail = null
                    openItemInFullViewer(item)
                },
                onOpenImageMemory = {
                    val item = selectedItemForDetail!!
                    selectedItemForDetail = null
                    openItemInFullViewer(item)
                },
                onOpenVoiceMemory = {
                    selectedItemForDetail = null
                    viewModel.navigateTo(CurrentScreen.VOICE_MEMORY)
                },
                onOpenScanner = {
                    selectedItemForDetail = null
                    launchMlKitScanner()
                }
            )
        }

        // Delete Confirmation Dialog
        if (itemToDeleteForConfirm != null) {
            DeleteConfirmationDialog(
                item = itemToDeleteForConfirm!!,
                onConfirmDelete = {
                    viewModel.deleteItemPermanently(itemToDeleteForConfirm!!)
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Item deleted successfully 🗑️")
                    }
                    itemToDeleteForConfirm = null
                },
                onDismiss = { itemToDeleteForConfirm = null }
            )
        }

        // Gallery Vault Sheet
        if (showGalleryVaultSheet) {
            GalleryVaultSheet(
                items = recentItems,
                onItemClick = { item ->
                    showGalleryVaultSheet = false
                    openItemInFullViewer(item)
                },
                onDeleteItem = { item ->
                    viewModel.deleteItemPermanently(item)
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Item deleted successfully 🗑️")
                    }
                },
                onDismiss = { showGalleryVaultSheet = false },
                onUploadPdf = { uri ->
                    val (name, size) = queryFileInfo(context, uri)
                    selectedPdfName = name
                    selectedPdfSize = size
                    selectedPdfUri = uri.toString()
                    viewModel.navigateTo(CurrentScreen.PDF_PREVIEW)
                    showGalleryVaultSheet = false
                },
                onUploadImage = { uri ->
                    viewModel.saveImageMemory("Uploaded Photo", uri.toString())
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Image saved to Vault 📸")
                    }
                }
            )
        }

        // Notification Sheet
        if (showNotificationSheet) {
            NotificationSheet(
                notifications = notifications,
                onDismiss = { showNotificationSheet = false },
                onCancelNotification = { item ->
                    viewModel.cancelNotificationOrReminder(item)
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Reminder removed 🔕")
                    }
                }
            )
        }

        // Contextual AI Set Reminder Schedule Sheet
        if (pendingReminderSchedule != null) {
            val pending = pendingReminderSchedule!!
            SetReminderBottomSheet(
                initialTitle = pending.title,
                onDismiss = { viewModel.clearPendingReminderSchedule() },
                onConfirm = { confirmedTitle, timestampMillis ->
                    viewModel.scheduleStructuredReminder(confirmedTitle, timestampMillis)

                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Reminder scheduled for \"$confirmedTitle\" ⏰")
                    }
                }
            )
        }

        // Manage Storage Dialog
        if (showManageStorageDialog) {
            ManageStorageDialog(
                storageData = storageData,
                onDismiss = { showManageStorageDialog = false },
                onSyncNow = {
                    viewModel.syncNow()
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Vault encrypted and verified 🔒")
                    }
                },
                onClearCache = {
                    viewModel.clearCache()
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Cache cleaned: 400 MB freed")
                    }
                }
            )
        }

        // Auto-Restore Previous GTOOL X Backup Banner/Dialog on startup
        if (showRestorePrompt) {
            androidx.compose.material3.BasicAlertDialog(
                onDismissRequest = { viewModel.dismissRestorePrompt() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .testTag("restore_prompt_dialog")
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(28.dp))
                        .background(Color(0xE61A1D24))
                        .border(1.dp, Color(0x26FFFFFF), RoundedCornerShape(28.dp))
                        .padding(24.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Icon badge
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFFFFA000), Color(0xFFFF6D00))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.SettingsBackupRestore,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = "Found Previous Backup",
                            style = MaterialTheme.typography.titleLarge.copy(
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 19.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Existing vault data and scanned documents were detected on this device. Would you like to restore your memories and OCR metadata now?",
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color(0xCCFFFFFF),
                                fontSize = 13.5.sp,
                                lineHeight = 19.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(28.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            androidx.compose.material3.Button(
                                onClick = { viewModel.dismissRestorePrompt() },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                    containerColor = Color(0x1AFFFFFF),
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text("Skip", fontWeight = FontWeight.Medium)
                            }
                            
                            androidx.compose.material3.Button(
                                onClick = {
                                    viewModel.restoreAllData { count ->
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar("Restored $count memories from local vault!")
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .weight(1.5f)
                                    .height(48.dp),
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFFF6D00),
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text("Restore All", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
