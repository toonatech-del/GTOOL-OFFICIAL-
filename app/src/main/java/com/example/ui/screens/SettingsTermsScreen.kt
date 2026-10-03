package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.interaction.MutableInteractionSource
import com.example.ui.GsdcallWorkspaceViewModel
import com.example.ui.components.AmbientLightingBackground
import com.example.ui.components.DarkFrostedSnackbarHost
import com.example.ui.components.FrostedGlassAlertDialog
import com.example.ui.components.SupportBottomSheet
import com.example.ui.theme.AmberWarm
import com.example.ui.theme.GlassStroke
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.ApkShareHelper
import com.example.util.RichTextHelper
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun PolicySection(title: String, body: String) {
    Column {
        Text(title, style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text(body, style = MaterialTheme.typography.bodyMedium, color = Color.Gray, lineHeight = 20.sp)
    }
}

@Composable
fun SettingsScreen(
    viewModel: GsdcallWorkspaceViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToTerms: () -> Unit,
    onNavigateToPrivacy: () -> Unit,
    onNavigateToAboutApp: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val isBiometricEnabled by viewModel.isBiometricEnabled.collectAsStateWithLifecycle()
    val backupInfo by viewModel.backupInfo.collectAsStateWithLifecycle()

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri: Uri? ->
        uri?.let {
            viewModel.exportPersistentBackup(it) { success, msg ->
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(msg)
                }
            }
        }
    }

    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.restorePersistentBackup(it) { success, msg ->
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(msg)
                }
            }
        }
    }

    AmbientLightingBackground {
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { DarkFrostedSnackbarHost(snackbarHostState) },
            modifier = Modifier.fillMaxSize()
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .statusBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header with Back Button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateBack() }
                        .padding(vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = AmberWarm,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Back to Home",
                        color = AmberWarm,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                }

                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp,
                        color = TextPrimary
                    ),
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // 1. Biometric App Lock Card
                SettingsCard(
                    icon = Icons.Rounded.Fingerprint,
                    title = "Biometric App Lock",
                    description = "Fingerprint / Face / PIN on launch",
                    isToggle = true,
                    toggleState = isBiometricEnabled,
                    onToggleChange = { enabled ->
                        viewModel.setBiometricEnabled(enabled)
                        val msg = if (enabled) "Biometric Lock Enabled 🔒" else "Biometric Lock Disabled"
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(msg)
                        }
                    }
                )

                // 2. Local Backup & Restore Card
                var showBackupDialog by remember { mutableStateOf(false) }
                var showSupportSheet by remember { mutableStateOf(false) }
                SettingsCard(
                    icon = Icons.Rounded.Folder,
                    title = "Local Backup & Restore",
                    description = if (backupInfo.exists) "Last backup: ${backupInfo.lastBackupDate}" else "No backups created yet",
                    onClick = { showBackupDialog = true }
                )

                // 3. Share GTOOL X Card
                SettingsCard(
                    icon = Icons.Rounded.Share,
                    title = "Share GTOOL X",
                    description = "Share app installer with nearby friends offline",
                    onClick = {
                        ApkShareHelper.shareAppApk(context)
                    }
                )

                // 4. Support / Tip Developer Card
                SettingsCard(
                    icon = Icons.Rounded.Favorite,
                    title = "Support GTOOL X",
                    description = "Keep development independent & ad-free with a tip",
                    onClick = {
                        showSupportSheet = true
                    }
                )

                // 5. Terms & Conditions Card
                SettingsCard(
                    icon = Icons.Rounded.Description,
                    title = "Terms & Conditions",
                    description = "Legal agreement, disclaimers & app policies",
                    onClick = onNavigateToTerms
                )

                // 6. Privacy Policy Card
                SettingsCard(
                    icon = Icons.Rounded.Security,
                    title = "Privacy Policy",
                    description = "Local processing & data handling policy",
                    onClick = onNavigateToPrivacy
                )

                // 7. About App Card
                SettingsCard(
                    icon = Icons.Rounded.Info,
                    title = "About App",
                    description = "Version, release details & offline features",
                    onClick = onNavigateToAboutApp
                )

                // 8. Contact Developer Card
                SettingsCard(
                    icon = Icons.Rounded.Email,
                    title = "Contact Developer",
                    description = "Get in touch or report an issue",
                    onClick = {
                        val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:hgdduf93@gmail.com?subject=GTOOL%20X%20Feedback")
                        }
                        try {
                            context.startActivity(emailIntent)
                        } catch (e: Exception) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Developer Contact: hgdduf93@gmail.com")
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Powered by GSD",
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = AmberWarm,
                        fontWeight = FontWeight.Medium
                    ),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                if (showSupportSheet) {
                    SupportBottomSheet(
                        onDismiss = { showSupportSheet = false },
                        onShowSnackbar = { msg ->
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(msg)
                            }
                        }
                    )
                }

                if (showBackupDialog) {
                    Dialog(
                        onDismissRequest = { showBackupDialog = false }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp)
                                .shadow(
                                    elevation = 24.dp,
                                    shape = RoundedCornerShape(28.dp),
                                    spotColor = Color.Black.copy(alpha = 0.5f)
                                )
                                .clip(RoundedCornerShape(28.dp))
                                .background(Color(0xE61A1D24))
                                .border(1.dp, Color(0x26FFFFFF), RoundedCornerShape(28.dp))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp)
                            ) {
                                // Header
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(
                                                    Brush.linearGradient(
                                                        listOf(Color(0xFF3B82F6), Color(0xFF1D4ED8))
                                                    )
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.CloudUpload,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                        Column {
                                            Text(
                                                text = "Backup & Restore",
                                                style = MaterialTheme.typography.titleLarge.copy(
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 18.sp,
                                                    color = Color.White
                                                )
                                            )
                                            Text(
                                                text = "Secure offline data archival",
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    color = Color(0x99FFFFFF),
                                                    fontSize = 12.sp
                                                )
                                            )
                                        }
                                    }

                                    IconButton(
                                        onClick = { showBackupDialog = false },
                                        modifier = Modifier
                                            .size(32.dp)
                                            .background(Color(0x1AFFFFFF), CircleShape)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Close,
                                            contentDescription = "Close",
                                            tint = Color.White.copy(alpha = 0.7f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(20.dp))

                                Text(
                                    text = "Manage your GTOOL X data locally. Persistent backups allow you to save your entire vault to your phone's storage, ensuring your memories are safe even if you reinstall the app.",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = Color(0xCCFFFFFF),
                                        fontSize = 13.sp,
                                        lineHeight = 19.sp
                                    )
                                )

                                Spacer(modifier = Modifier.height(24.dp))

                                // iOS-Style Action Tiles
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    // Export Tile
                                    BackupActionTile(
                                        title = "Export Full Backup",
                                        subtitle = "Saves photos, notes & PDFs to storage",
                                        icon = Icons.Rounded.Share,
                                        iconTint = Color(0xFF00E5FF),
                                        onClick = {
                                            showBackupDialog = false
                                            val timestamp = java.text.SimpleDateFormat("yyyyMMdd_HHmm", java.util.Locale.getDefault()).format(java.util.Date())
                                            exportLauncher.launch("GTOOL_X_Backup_$timestamp.zip")
                                        }
                                    )

                                    // Restore Tile
                                    BackupActionTile(
                                        title = "Restore from File",
                                        subtitle = "Select a saved .zip backup package",
                                        icon = Icons.Rounded.SettingsBackupRestore,
                                        iconTint = Color(0xFFFFA000),
                                        onClick = {
                                            showBackupDialog = false
                                            restoreLauncher.launch(arrayOf("application/zip"))
                                        }
                                    )
                                }

                                if (backupInfo.exists) {
                                    Spacer(modifier = Modifier.height(20.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(Color(0x10FFFFFF))
                                            .padding(14.dp)
                                    ) {
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text(
                                                text = "Last Internal Backup",
                                                color = Color.White.copy(alpha = 0.5f),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 0.5.sp
                                            )
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = backupInfo.lastBackupDate,
                                                    color = Color.White,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                                Text(
                                                    text = "${backupInfo.itemCount} items",
                                                    color = AmberWarm,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(24.dp))

                                // Footer Dismiss
                                Text(
                                    text = "Dismiss",
                                    modifier = Modifier
                                        .align(Alignment.CenterHorizontally)
                                        .clickable { showBackupDialog = false }
                                        .padding(8.dp),
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        color = Color.White.copy(alpha = 0.6f),
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 14.sp
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

@Composable
private fun BackupActionTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0x14FFFFFF))
            .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconTint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0x99FFFFFF),
                        fontSize = 11.sp
                    )
                )
            }

            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = Color(0x33FFFFFF),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun SettingsCard(
    icon: ImageVector,
    title: String,
    description: String,
    isToggle: Boolean = false,
    toggleState: Boolean = false,
    onToggleChange: ((Boolean) -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0x1A2530).copy(alpha = 0.55f))
            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(20.dp))
            .then(
                if (onClick != null) {
                    Modifier.clickable { onClick() }
                } else Modifier
            )
            .padding(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0x28FFFFFF))
                    .border(1.dp, GlassStroke, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = AmberWarm,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = TextPrimary
                    )
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                )
            }

            if (isToggle) {
                Switch(
                    checked = toggleState,
                    onCheckedChange = onToggleChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.Black,
                        checkedTrackColor = AmberWarm,
                        uncheckedThumbColor = Color.Gray,
                        uncheckedTrackColor = Color(0x33FFFFFF)
                    )
                )
            } else if (onClick != null) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Color(0x4DFFFFFF),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun PrivacyPolicyScreen(onNavigateBack: () -> Unit) {
    AmbientLightingBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(24.dp)
        ) {
            // Header with Back Button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateBack() }
                    .padding(vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = AmberWarm,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Back to Settings",
                    color = AmberWarm,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Privacy Policy",
                style = MaterialTheme.typography.headlineMedium.copy(
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp
                )
            )
            Spacer(modifier = Modifier.height(20.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                PolicySection("1. INTRODUCTION", "GTOOL X is a document and productivity utility application designed to help users manage documents, images, notes, OCR text, reminders and related utilities on their Android device.")
                PolicySection("2. INFORMATION THE APP MAY ACCESS", "Depending on the features chosen by the user, GTOOL X may access photos and images selected by the user, PDF and document files selected by the user, camera input when scanning features are used, notes created by the user, OCR text generated from user-selected content, reminder information created by the user, local document metadata, and files selected for backup, restore, import or export.")
                PolicySection("3. LOCAL PROCESSING", "Where functionality is designed to operate locally, documents, images, notes, OCR results, and search indexes are processed and stored on the user's device.")
                PolicySection("4. NO SALE OF USER CONTENT", "GTOOL X does not sell user documents, images, PDFs, notes, or OCR content.")
                PolicySection("5. DEVELOPER-OPERATED SERVERS", "GTOOL X does not operate developer servers to collect or store user documents or personal records. Note: Optional device services such as Google Play Services Document Scanner may interact with Play Services infrastructure as required by the operating system.")
                PolicySection("6. OCR", "OCR is performed locally using the application's supported OCR technology. OCR results may contain errors and should be verified by the user before relying on extracted text.")
                PolicySection("7. SEARCH", "Search indexes are maintained locally on device to allow users to search their notes, documents, and OCR text.")
                PolicySection("8. CAMERA", "Camera access is used only when the user explicitly chooses camera or scanning functionality.")
                PolicySection("9. FILE ACCESS", "The app accesses files selected or provided by the user for features such as viewing, OCR, conversion, organization, import, or export.")
                PolicySection("10. EXPORT AND SHARING", "When the user explicitly chooses Android's share or export functionality, the selected content is provided to the target application chosen by the user. GTOOL X does not control how third-party recipients handle shared content.")
                PolicySection("11. REMINDERS", "Reminder information is used solely to schedule notifications and alarms on the user's local device.")
                PolicySection("12. BACKUP AND RESTORE", "Local backup functionality generates user-initiated archives saved directly on device storage via Android file picker. Backups remain stored on device until moved or deleted by the user.")
                PolicySection("13. DATA RETENTION", "Locally stored data remains on the user's device until the user deletes it, clears application data, uninstalls the app, or otherwise removes it.")
                PolicySection("14. DATA DELETION", "Users can delete app data, individual documents, notes, and backups directly within the app or through Android system settings.")
                PolicySection("15. SECURITY", "GTOOL X employs reasonable security measures including Android app-isolated private storage, biometric unlock protection, and canonical path checks.")
                PolicySection("16. THIRD-PARTY LIBRARIES", "Relevant third-party libraries used in GTOOL X are disclosed in the Open Source Licenses section.")
                PolicySection("17. CHILDREN", "GTOOL X is a general audience document utility app and does not knowingly collect personal information from children.")
                PolicySection("18. POLICY CHANGES", "This Privacy Policy may be updated periodically. Any changes will be reflected with an updated effective date.")
                PolicySection("19. CONTACT", "Developer: GSD\nEmail: hgdduf93@gmail.com")
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Powered by GSD",
                style = MaterialTheme.typography.labelLarge.copy(color = AmberWarm),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
fun TermsSectionCard(
    number: String,
    icon: ImageVector,
    title: String,
    content: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0x14FFFFFF))
            .border(1.dp, Color(0x26FFFFFF), RoundedCornerShape(18.dp))
            .padding(18.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(AmberWarm.copy(alpha = 0.15f))
                        .border(1.dp, AmberWarm.copy(alpha = 0.35f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = AmberWarm,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Column {
                    Text(
                        text = "SECTION $number",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = AmberWarm,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            letterSpacing = 1.sp
                        )
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    )
                }
            }
            Text(
                text = content,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color(0xFFCBD5E1),
                    fontSize = 13.5.sp,
                    lineHeight = 20.sp
                )
            )
        }
    }
}

@Composable
fun TermsConditionsScreen(onNavigateBack: () -> Unit) {
    AmbientLightingBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // Header with Back Button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateBack() }
                    .padding(vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = AmberWarm,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Back to Settings",
                    color = AmberWarm,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Column(modifier = Modifier.padding(bottom = 12.dp)) {
                Text(
                    text = "Terms & Conditions",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp
                    )
                )
                Text(
                    text = "GTOOL X • Legal Agreement & App Policies",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                TermsSectionCard(
                    number = "01",
                    icon = Icons.Rounded.CheckCircle,
                    title = "1. Acceptance of Terms",
                    content = "By installing or using GTOOL X, you agree to be bound by these Terms & Conditions. If you do not agree, please do not use the application."
                )

                TermsSectionCard(
                    number = "02",
                    icon = Icons.Rounded.Storage,
                    title = "2. Description of the Application",
                    content = "GTOOL X provides document, image, PDF, OCR, notes, search, reminder, invoice, and related utility features for Android devices."
                )

                TermsSectionCard(
                    number = "03",
                    icon = Icons.Rounded.VerifiedUser,
                    title = "3. User Responsibility",
                    content = "Users are solely responsible for the content they import, scan, store, process, or share using GTOOL X."
                )

                TermsSectionCard(
                    number = "04",
                    icon = Icons.Rounded.Gavel,
                    title = "4. Copyright and User Content",
                    content = "Users must have appropriate rights or permissions to access, copy, scan, process, or store content they use with the application. GTOOL X does not grant users rights to third-party copyrighted material."
                )

                TermsSectionCard(
                    number = "05",
                    icon = Icons.Rounded.AutoAwesome,
                    title = "5. OCR Disclaimer",
                    content = "OCR and extracted document text may contain recognition errors. Verify important information before relying on extracted text."
                )

                TermsSectionCard(
                    number = "06",
                    icon = Icons.Rounded.Description,
                    title = "6. Document Processing Disclaimer",
                    content = "Processed files, conversions, and resized images should be reviewed for accuracy before being relied upon for formal submissions."
                )

                TermsSectionCard(
                    number = "07",
                    icon = Icons.Rounded.ReceiptLong,
                    title = "7. Invoice / Tax Disclaimer",
                    content = "Invoice creation and calculation assistance is provided for convenience. Users are responsible for verifying applicable tax rates, invoice requirements, and legal obligations."
                )

                TermsSectionCard(
                    number = "08",
                    icon = Icons.Rounded.NotificationsActive,
                    title = "8. Reminder Disclaimer",
                    content = "Reminder functionality relies on device alarm services and should not be relied upon as the sole mechanism for critical or time-sensitive deadlines."
                )

                TermsSectionCard(
                    number = "09",
                    icon = Icons.Rounded.Folder,
                    title = "9. Backup Responsibility",
                    content = "Users are responsible for maintaining backups of important data. Local backups created in GTOOL X remain under user management."
                )

                TermsSectionCard(
                    number = "10",
                    icon = Icons.Rounded.Share,
                    title = "10. Third-Party Applications",
                    content = "When users export or share content to other applications, those applications operate under their own independent policies and terms."
                )

                TermsSectionCard(
                    number = "11",
                    icon = Icons.Rounded.Code,
                    title = "11. Intellectual Property",
                    content = "GTOOL X's original branding, UI, and code remain the property of their respective rights holders. Third-party libraries remain subject to their respective licenses."
                )

                TermsSectionCard(
                    number = "12",
                    icon = Icons.Rounded.Block,
                    title = "12. Prohibited Uses",
                    content = "Users must not use the application for unlawful activities, copyright infringement, fraud, unauthorized data access, malware distribution, or other prohibited activities."
                )

                TermsSectionCard(
                    number = "13",
                    icon = Icons.Rounded.Build,
                    title = "13. Availability",
                    content = "Application features may be modified, updated, suspended, or discontinued when reasonably necessary for maintenance or software improvements."
                )

                TermsSectionCard(
                    number = "14",
                    icon = Icons.Rounded.Lock,
                    title = "14. Security",
                    content = "No software application can guarantee absolute security. GTOOL X employs standard platform security practices to safeguard local data."
                )

                TermsSectionCard(
                    number = "15",
                    icon = Icons.Rounded.Shield,
                    title = "15. Limitation of Liability",
                    content = "To the maximum extent permitted by applicable law, GTOOL X shall not be liable for indirect, incidental, or consequential damages resulting from app usage or data loss."
                )

                TermsSectionCard(
                    number = "16",
                    icon = Icons.Rounded.Info,
                    title = "16. Disclaimer of Warranties",
                    content = "GTOOL X is provided 'AS IS' without express or implied warranties of any kind regarding merchantability or fitness for a particular purpose."
                )

                TermsSectionCard(
                    number = "17",
                    icon = Icons.Rounded.Edit,
                    title = "17. Changes to Terms",
                    content = "These terms may be updated from time to time. Continued use of the application indicates acceptance of any revised terms."
                )

                TermsSectionCard(
                    number = "18",
                    icon = Icons.Rounded.Email,
                    title = "18. Contact",
                    content = "Developer: GSD\nEmail: hgdduf93@gmail.com"
                )

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Powered by GSD",
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = AmberWarm,
                        fontWeight = FontWeight.Medium
                    ),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
