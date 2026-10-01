package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.PdfTemplate
import com.example.ui.components.AmbientLightingBackground
import com.example.ui.components.GlassCard
import com.example.ui.theme.AmberWarm
import com.example.ui.theme.GlassStroke
import com.example.ui.theme.SyncGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarmGold

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfTemplateSelectionScreen(
    initialTemplate: PdfTemplate = PdfTemplate.PROFESSIONAL,
    onTemplateSelected: (PdfTemplate) -> Unit,
    onBackClick: () -> Unit
) {
    var selectedTemplate by remember { mutableStateOf(initialTemplate) }

    AmbientLightingBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "GT",
                                style = TextStyle(
                                    fontFamily = FontFamily.Cursive,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 24.sp,
                                    brush = Brush.horizontalGradient(
                                        listOf(
                                            Color(0xFF00E5FF),
                                            Color(0xFFFF2D55),
                                            Color(0xFFFF9500)
                                        )
                                    )
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "PDF Templates",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 18.sp
                                )
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.Rounded.ArrowBack, "Back", tint = Color.White)
                        }
                    }
                )
            },
            bottomBar = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp)
                            .clip(RoundedCornerShape(29.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        Color(0xFF00E5FF),
                                        Color(0xFFFF2D55),
                                        Color(0xFFFF9500)
                                    )
                                )
                            )
                            .clickable {
                                onTemplateSelected(selectedTemplate)
                            }
                            .testTag("apply_selected_template_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Use ${selectedTemplate.displayName} Template",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 16.sp,
                                    color = Color.Black
                                )
                            )
                        }
                    }
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Intro Header
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Choose Document Style",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 22.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Select a layout configuration to dynamically style your exported PDF canvas.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    )
                }

                // Templates Vertical Grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(1),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 10.dp)
                ) {
                    items(PdfTemplate.values()) { template ->
                        val isSelected = selectedTemplate == template
                        TemplateCard(
                            template = template,
                            isSelected = isSelected,
                            onSelect = { selectedTemplate = template }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TemplateCard(
    template: PdfTemplate,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .testTag("pdf_template_card_${template.id}"),
        shape = RoundedCornerShape(22.dp),
        borderStrokeWidth = if (isSelected) 2.dp else 1.dp,
        borderColorList = if (isSelected) {
            listOf(Color(0xFF00E5FF), Color(0xFFFF2D55), Color(0xFFFF9500))
        } else {
            listOf(Color(0x50FFFFFF), Color(0x15FFFFFF))
        },
        ambientGlowColor = if (isSelected) AmberWarm else Color.Transparent
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header row with Icon, Name & Selection badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Brush.linearGradient(template.previewGradient)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (template) {
                                PdfTemplate.PROFESSIONAL -> Icons.Rounded.Work
                                PdfTemplate.MODERN -> Icons.Rounded.AutoAwesome
                                PdfTemplate.SIMPLE -> Icons.Rounded.Article
                                PdfTemplate.CREATIVE -> Icons.Rounded.Palette
                            },
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = template.displayName,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontSize = 16.sp
                            )
                        )
                        Text(
                            text = template.subtitle,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = AmberWarm,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp
                            )
                        )
                    }
                }

                // Selection Radio Pill
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) AmberWarm else Color(0x20FFFFFF))
                        .border(
                            1.5.dp,
                            if (isSelected) AmberWarm else Color(0x60FFFFFF),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = "Selected",
                            tint = Color.Black,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Description text
            Text(
                text = template.description,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
            )

            // Dynamic Miniature Canvas Preview Mockup
            MiniTemplatePreviewMockup(template = template)
        }
    }
}

@Composable
fun MiniTemplatePreviewMockup(template: PdfTemplate) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0D1117))
            .border(1.dp, Color(0x30FFFFFF), RoundedCornerShape(12.dp))
            .padding(10.dp)
    ) {
        when (template) {
            PdfTemplate.PROFESSIONAL -> {
                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                    // Top dark navy banner
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(14.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF1E3A5F))
                            .padding(horizontal = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFFD4AF37)))
                        Spacer(modifier = Modifier.width(4.dp))
                        Box(modifier = Modifier.width(36.dp).height(4.dp).background(Color.White.copy(alpha = 0.8f)))
                    }
                    // Mock content lines
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Box(modifier = Modifier.width(140.dp).height(3.dp).background(Color(0x50FFFFFF)))
                        Box(modifier = Modifier.width(180.dp).height(3.dp).background(Color(0x35FFFFFF)))
                    }
                    // Bottom gold accent line
                    Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(Color(0xFFD4AF37)))
                }
            }
            PdfTemplate.MODERN -> {
                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                    // Top vibrant accent
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .width(60.dp)
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Brush.horizontalGradient(listOf(Color(0xFF00E5FF), Color(0xFFFF9500))))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(modifier = Modifier.width(20.dp).height(4.dp).background(Color.White.copy(alpha = 0.6f)))
                    }
                    // Rounded bullet pills
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(modifier = Modifier.width(40.dp).height(6.dp).clip(RoundedCornerShape(3.dp)).background(Color(0x3000E5FF)))
                        Box(modifier = Modifier.width(50.dp).height(6.dp).clip(RoundedCornerShape(3.dp)).background(Color(0x30FF9500)))
                    }
                    Box(modifier = Modifier.width(120.dp).height(3.dp).background(Color(0x40FFFFFF)))
                }
            }
            PdfTemplate.SIMPLE -> {
                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                    // Clean line title
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.width(70.dp).height(5.dp).background(Color.White.copy(alpha = 0.9f)))
                        Box(modifier = Modifier.width(30.dp).height(3.dp).background(Color.White.copy(alpha = 0.4f)))
                    }
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0x40FFFFFF)))
                    // Minimal lines
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Box(modifier = Modifier.width(160.dp).height(3.dp).background(Color(0x50FFFFFF)))
                        Box(modifier = Modifier.width(120.dp).height(3.dp).background(Color(0x35FFFFFF)))
                    }
                }
            }
            PdfTemplate.CREATIVE -> {
                Row(modifier = Modifier.fillMaxSize()) {
                    // Left purple sidebar
                    Box(
                        modifier = Modifier
                            .width(12.dp)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(3.dp))
                            .background(Brush.verticalGradient(listOf(Color(0xFF7B1FA2), Color(0xFFE040FB))))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                        Box(modifier = Modifier.width(80.dp).height(6.dp).background(Color(0xFFE040FB).copy(alpha = 0.8f)))
                        Box(modifier = Modifier.width(140.dp).height(3.dp).background(Color(0x50FFFFFF)))
                        Box(modifier = Modifier.width(110.dp).height(3.dp).background(Color(0x35FFFFFF)))
                    }
                }
            }
        }
    }
}
