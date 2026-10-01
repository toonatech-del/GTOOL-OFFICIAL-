package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.AmbientLightingBackground

@Composable
fun PdfHubScreen(
    onUploadExisting: () -> Unit,
    onCreateNew: () -> Unit,
    onBack: () -> Unit
) {
    AmbientLightingBackground {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "GT pdf maker",
                style = TextStyle(
                    fontFamily = FontFamily.Cursive,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 48.sp,
                    brush = Brush.horizontalGradient(
                        listOf(
                            Color(0xFF00E5FF),
                            Color(0xFFFF2D55),
                            Color(0xFFFF9500)
                        )
                    )
                ),
                modifier = Modifier.padding(bottom = 80.dp)
            )

            PdfHubButton(
                text = "Upload Existing PDF",
                icon = Icons.Rounded.PictureAsPdf,
                onClick = onUploadExisting
            )
            Spacer(modifier = Modifier.height(24.dp))
            PdfHubButton(
                text = "Create New PDF",
                icon = Icons.Rounded.Edit,
                onClick = onCreateNew
            )
        }
    }
}

@Composable
fun PdfHubButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clip(RoundedCornerShape(36.dp))
            .background(Color(0x28FFFFFF))
            .border(
                1.5.dp,
                Brush.horizontalGradient(
                    listOf(
                        Color(0xFF00E5FF),
                        Color(0xFFFF2D55),
                        Color(0xFFFF9500)
                    )
                ),
                RoundedCornerShape(36.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(24.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = Color.White
                )
            )
        }
    }
}
