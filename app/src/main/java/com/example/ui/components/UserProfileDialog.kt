package com.example.ui.components

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Headset
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material.icons.rounded.Mail
import androidx.compose.material.icons.rounded.OpenInFull
import androidx.compose.material.icons.rounded.Policy
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.example.ui.theme.AmberWarm
import com.example.ui.theme.LocalIsDarkMode
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarmGold
import com.example.util.BackupInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileDialog(
    accountName: String = "Google User",
    userEmail: String = "",
    profileImageUrl: String? = null,
    isLoggedIn: Boolean = false,
    isBiometricEnabled: Boolean = false,
    onToggleBiometric: ((Boolean) -> Unit)? = null,
    backupInfo: BackupInfo? = null,
    onCreateBackup: (() -> Unit)? = null,
    onRestoreBackup: (() -> Unit)? = null,
    onGoogleSignIn: (() -> Unit)? = null,
    onSignOut: () -> Unit = {},
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isDarkMode = LocalIsDarkMode.current

    var showContactModal by remember { mutableStateOf(false) }
    var showPrivacyModal by remember { mutableStateOf(false) }
    var contactMessage by remember { mutableStateOf("") }

    BackHandler {
        when {
            showPrivacyModal -> showPrivacyModal = false
            showContactModal -> showContactModal = false
            else -> onDismiss()
        }
    }

    BasicAlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .testTag("user_profile_dialog")
    ) {
        ZoomModalWrapper {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(32.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xF2181B24), Color(0xF8111319))
                        )
                    )
                    .border(
                        1.5.dp,
                        Brush.verticalGradient(
                            listOf(Color(0x9900E5FF), Color(0x33FFFFFF))
                        ),
                        RoundedCornerShape(32.dp)
                    )
                    .padding(24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Settings & Data Vault",
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            color = Color.White
                        ),
                        modifier = Modifier.padding(bottom = 4.dp)
                    )

                    // 1. Biometric App Lock Toggle Tile
                    if (onToggleBiometric != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(22.dp))
                                .background(Color(0x20FFFFFF))
                                .border(1.dp, Color(0x30FFFFFF), RoundedCornerShape(22.dp))
                                .clickable { onToggleBiometric(!isBiometricEnabled) }
                                .padding(16.dp)
                                .testTag("biometric_lock_tile")
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(if (isBiometricEnabled) Color(0x4034D399) else Color(0x20FFFFFF))
                                        .border(1.dp, if (isBiometricEnabled) Color(0xFF34D399) else Color.White.copy(alpha = 0.35f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Lock,
                                        contentDescription = "Biometric Lock",
                                        tint = if (isBiometricEnabled) Color(0xFF34D399) else Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Biometric App Lock",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (isBiometricEnabled) "Enabled • Fingerprint/Face required" else "Require Fingerprint/PIN on launch",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = if (isBiometricEnabled) Color(0xFF34D399) else Color.White.copy(alpha = 0.6f),
                                            fontSize = 11.sp
                                        )
                                    )
                                }

                                Switch(
                                    checked = isBiometricEnabled,
                                    onCheckedChange = { onToggleBiometric(it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = Color(0xFF10B981)
                                    )
                                )
                            }
                        }
                    }

                    // 2. Create Local Backup Tile
                    if (onCreateBackup != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(22.dp))
                                .background(Color(0x20FFFFFF))
                                .border(1.dp, Color(0x30FFFFFF), RoundedCornerShape(22.dp))
                                .clickable { onCreateBackup() }
                                .padding(16.dp)
                                .testTag("local_backup_tile")
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(Color(0x20FFFFFF))
                                        .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Shield,
                                        contentDescription = "Backup",
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Create Local Backup",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Export vault DB to Documents/GTOOL X/Backups/\nLast: ${backupInfo?.lastBackupDate ?: "Never"}",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = Color.White.copy(alpha = 0.6f),
                                            fontSize = 11.sp
                                        )
                                    )
                                }

                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.8f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    // 3. Help & Support
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(22.dp))
                            .background(Color(0x20FFFFFF))
                            .border(1.dp, Color(0x30FFFFFF), RoundedCornerShape(22.dp))
                            .clickable { showContactModal = true }
                            .padding(16.dp)
                            .testTag("contact_support_tile")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x20FFFFFF))
                                    .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Headset,
                                    contentDescription = "Help & Support",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Help & Support",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Submit feedback or report an issue",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Color.White.copy(alpha = 0.6f),
                                        fontSize = 11.sp
                                    )
                                )
                            }

                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // 4. Terms & Privacy Policy
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(22.dp))
                            .background(Color(0x20FFFFFF))
                            .border(1.dp, Color(0x30FFFFFF), RoundedCornerShape(22.dp))
                            .clickable { showPrivacyModal = true }
                            .padding(16.dp)
                            .testTag("privacy_policy_tile")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x20FFFFFF))
                                    .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Description,
                                    contentDescription = "Terms & Conditions",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Terms & Privacy Policy",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Full offline local storage & zero tracking",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Color.White.copy(alpha = 0.6f),
                                        fontSize = 11.sp
                                    )
                                )
                            }

                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Support Modal
    if (showContactModal) {
        BasicAlertDialog(
            onDismissRequest = { showContactModal = false },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("contact_us_dialog")
        ) {
            ZoomModalWrapper {
                GlassCard(
                    shape = RoundedCornerShape(24.dp),
                    borderColorList = listOf(Color(0x66FFFFFF), Color(0x22FFFFFF)),
                    backgroundGradient = listOf(
                        if (isDarkMode) Color(0xF51B1920) else Color(0xF5FFFFFF),
                        if (isDarkMode) Color(0xFA24202C) else Color(0xFAF4F4F5)
                    ),
                    ambientGlowColor = AmberWarm
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(22.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Help & Support",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDarkMode) TextPrimary else Color(0xFF111827)
                                )
                            )
                            IconButton(
                                onClick = { showContactModal = false },
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(if (isDarkMode) Color(0x20FFFFFF) else Color(0x10000000))
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Close",
                                    tint = if (isDarkMode) TextSecondary else Color(0xFF6B7280),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "Write your feedback or issue below:",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = if (isDarkMode) TextSecondary else Color(0xFF4B5563),
                                fontSize = 13.sp
                            )
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = contactMessage,
                            onValueChange = { contactMessage = it },
                            placeholder = { Text("Describe your issue or request...", fontSize = 13.sp) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AmberWarm,
                                unfocusedBorderColor = if (isDarkMode) Color(0x30FFFFFF) else Color(0x20000000)
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        var isSubmitted by remember { mutableStateOf(false) }
                        if (isSubmitted) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0x2200E5FF))
                                    .border(1.dp, Color(0xFF00E5FF), RoundedCornerShape(12.dp))
                                    .padding(12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Feedback submitted! Thank you 🙏", color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        Button(
                            onClick = {
                                if (isSubmitted) {
                                    showContactModal = false
                                } else if (contactMessage.isNotBlank()) {
                                    isSubmitted = true
                                    contactMessage = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AmberWarm,
                                contentColor = Color(0xFF0F0E11)
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (isSubmitted) "Close" else "Submit", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Privacy Policy Modal
    if (showPrivacyModal) {
        BasicAlertDialog(
            onDismissRequest = { showPrivacyModal = false },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("privacy_policy_dialog")
        ) {
            ZoomModalWrapper {
                GlassCard(
                    shape = RoundedCornerShape(24.dp),
                    borderColorList = listOf(Color(0x66FFFFFF), Color(0x22FFFFFF)),
                    backgroundGradient = listOf(
                        if (isDarkMode) Color(0xF51B1920) else Color(0xF5FFFFFF),
                        if (isDarkMode) Color(0xFA24202C) else Color(0xFAF4F4F5)
                    ),
                    ambientGlowColor = AmberWarm
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(22.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Privacy & Security Policy",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDarkMode) TextPrimary else Color(0xFF111827)
                                )
                            )
                            IconButton(
                                onClick = { showPrivacyModal = false },
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(if (isDarkMode) Color(0x20FFFFFF) else Color(0x10000000))
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Close",
                                    tint = if (isDarkMode) TextSecondary else Color(0xFF6B7280),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "• 100% Offline Local Storage: All documents, IDs, scorecards, notes, and OCR texts stay strictly inside your device Room database.\n\n• Public Backup Protection: Backups saved under Documents/GTOOL X/ remain safe even if you uninstall the app.\n\n• Zero Telemetry: No analytics, tracking, or remote data selling.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = if (isDarkMode) TextSecondary else Color(0xFF374151),
                                fontSize = 13.sp,
                                lineHeight = 19.sp
                            )
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        Button(
                            onClick = { showPrivacyModal = false },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AmberWarm,
                                contentColor = Color(0xFF0F0E11)
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Got it", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
