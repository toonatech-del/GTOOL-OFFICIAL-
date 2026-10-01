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
    onNavigateToPrivacy: () -> Unit
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
                    description = "Disclaimers and user responsibilities",
                    onClick = onNavigateToTerms
                )

                // 6. Privacy Policy Card
                SettingsCard(
                    icon = Icons.Rounded.Security,
                    title = "Privacy Policy",
                    description = "100% offline, zero data collection",
                    onClick = onNavigateToPrivacy
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
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                PolicySection("1. Introduction & Core Philosophy", "GTOOL X is built on a zero-knowledge, strictly offline architecture. We firmly believe your personal documents and identity records belong exclusively to you.")
                PolicySection("2. 100% Local On-Device Processing", "All optical character recognition (OCR), text entity extraction, image resizing, and conversions are executed directly on the user's device hardware. No personal documents, images, or extracted strings are transmitted to remote servers.")
                PolicySection("3. Zero Telemetry & No Third-Party Tracking", "GTOOL X does not embed analytics SDKs, advertising trackers, device identifiers, or telemetry monitoring services. We do not collect behavioral data, usage stats, or error logs containing user data.")
                PolicySection("4. Storage & Vault Security", "Documents and generated photos reside under user-controlled storage (`Documents/GTOOL X/` and `Pictures/GTOOL X/`). The internal metadata database is secured locally using Android platform security standards.")
                PolicySection("5. Camera & File Storage Permissions", "Camera and Storage permissions requested by GTOOL X are strictly utilized in real time to capture photos for resizing, scanning, and saving output locally. Permissions are never exploited for background surveillance or unauthorized access.")
                PolicySection("6. User Rights & Data Ownership", "Users retain complete ownership and full control over their files. Deleting files or clearing app storage deletes the local indexing immediately.")
                PolicySection("7. Policy Revisions & Disclaimers", "This policy remains effective offline. GTOOL X disclaims all liabilities concerning external handling once files are exported outside the app.")
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
                    title = "Acceptance of Terms & App Scope",
                    content = "GTOOL X (Powered by GSD) is an offline-first productivity and document utility application designed for on-device document management, optical character recognition (OCR), photo resizing/compression, and smart invoice creation. By installing, accessing, or using GTOOL X, you agree to be bound by these Terms & Conditions."
                )

                TermsSectionCard(
                    number = "02",
                    icon = Icons.Rounded.Storage,
                    title = "100% Offline Architecture & Data Ownership",
                    content = "GTOOL X operates entirely on-device without remote telemetry or cloud databases. All files, generated invoices, cropped photos, and OCR transcriptions reside strictly within the user's local device storage (Documents/GTOOL X/ and Pictures/GTOOL X/).\n\nYou retain 100% exclusive ownership of your data, files, and documents. GTOOL X has zero access, visibility, or control over your local records."
                )

                TermsSectionCard(
                    number = "03",
                    icon = Icons.Rounded.CropFree,
                    title = "Photo Resizer & Exam Portal Presets Disclaimer",
                    content = "Presets provided within the Photo Resizer module (e.g., SSC, UPSC, Railway/RRB, Banking/IBPS, PAN/Visa) are intended as productivity aids based on standard publicly available specifications.\n\nExamination boards and portal authorities frequently update file dimension, DPI, aspect ratio, and KB limits. The user bears sole responsibility for verifying final file dimensions and file sizes against official notification guidelines prior to submission. GTOOL X is not responsible for any rejected applications or form submission issues."
                )

                TermsSectionCard(
                    number = "04",
                    icon = Icons.Rounded.VerifiedUser,
                    title = "Non-Affiliation with Government Bodies",
                    content = "GTOOL X is an independent offline utility tool and is NOT affiliated, associated, authorized, endorsed by, or in any way officially connected with any government agency, public exam authority, or financial institution."
                )

                TermsSectionCard(
                    number = "05",
                    icon = Icons.Rounded.ReceiptLong,
                    title = "Smart Invoice Maker & Tax Calculations",
                    content = "The Smart Invoice Maker provides digital formatting, OCR bill conversion, and arithmetic calculation assistance. Users are solely responsible for ensuring the accuracy of line items, tax/GST rates, business details, and legal compliance before issuing invoices to clients or authorities."
                )

                TermsSectionCard(
                    number = "06",
                    icon = Icons.Rounded.Lock,
                    title = "Local Backups & Biometric App Lock",
                    content = "Biometric authentication and local backup features depend directly on the host device's Android security subsystem. It is the user's responsibility to maintain device security and secure backup archives."
                )

                TermsSectionCard(
                    number = "07",
                    icon = Icons.Rounded.Gavel,
                    title = "Limitation of Liability & 'As Is' Provision",
                    content = "GTOOL X is provided on an 'AS IS' and 'AS AVAILABLE' basis without warranties of any kind. GTOOL X and its developers shall not be liable for any direct, indirect, incidental, or consequential damages resulting from data loss, device issues, inaccurate calculations, or missed examination deadlines."
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
