package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.AmbientLightingBackground
import com.example.ui.theme.AmberWarm
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun TermsOnboardingScreen(onAgree: () -> Unit) {
    AmbientLightingBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(AmberWarm.copy(alpha = 0.15f))
                        .border(1.dp, AmberWarm.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "WELCOME TO GTOOL X",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = AmberWarm,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Terms & Conditions",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp
                    )
                )

                Text(
                    text = "Please review our terms of use before entering the workspace.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                )

                Spacer(modifier = Modifier.height(20.dp))

                OnboardingSectionCard(
                    icon = Icons.Rounded.Storage,
                    title = "100% On-Device Offline Architecture",
                    description = "All OCR text extraction, photo resizing, and invoices are generated and stored strictly on your local device storage. Zero external server uploads."
                )

                Spacer(modifier = Modifier.height(12.dp))

                OnboardingSectionCard(
                    icon = Icons.Rounded.CropFree,
                    title = "Exam Portal Presets & Dimensions",
                    description = "Portal presets (SSC, UPSC, RRB, Banking) are productivity aids. Users bear sole responsibility for verifying final file dimensions & KB limits before submitting official applications."
                )

                Spacer(modifier = Modifier.height(12.dp))

                OnboardingSectionCard(
                    icon = Icons.Rounded.VerifiedUser,
                    title = "Independent Utility Tool",
                    description = "GTOOL X is an independent offline utility and is not affiliated with or endorsed by any government agency, public exam authority, or financial institution."
                )

                Spacer(modifier = Modifier.height(12.dp))

                OnboardingSectionCard(
                    icon = Icons.Rounded.ReceiptLong,
                    title = "Smart Invoice & Calculations",
                    description = "Tax and arithmetic calculations are provided for convenience. Users are responsible for verifying business accuracy and compliance."
                )

                Spacer(modifier = Modifier.height(20.dp))
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = onAgree,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AmberWarm),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = "Agree",
                        tint = Color(0xFF111319),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Agree & Enter Workspace",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color(0xFF111319)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Powered by GSD",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = AmberWarm,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }
    }
}

@Composable
private fun OnboardingSectionCard(
    icon: ImageVector,
    title: String,
    description: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0x14FFFFFF))
            .border(1.dp, Color(0x26FFFFFF), RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFFCBD5E1),
                        fontSize = 12.5.sp,
                        lineHeight = 17.sp
                    )
                )
            }
        }
    }
}

