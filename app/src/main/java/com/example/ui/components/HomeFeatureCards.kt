package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.Crop
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CurrentScreen
import com.example.ui.theme.LocalIsDarkMode
import com.example.ui.theme.SectionHeaderColor

data class FeatureModule(
    val title: String,
    val icon: ImageVector? = null,
    val useModernGalleryIcon: Boolean = false,
    val gradientColors: List<Color>,
    val targetScreen: CurrentScreen,
    val testTag: String
)

@Composable
fun HomeFeatureCards(
    onFeatureClick: (CurrentScreen) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDarkMode = LocalIsDarkMode.current
    val sectionHeaderColor = if (isDarkMode) SectionHeaderColor else Color(0xFF000000)
    val modules = listOf(
        FeatureModule(
            title = "PDF",
            icon = Icons.Rounded.PictureAsPdf,
            gradientColors = listOf(Color(0xFFFF3B30), Color(0xFFFF2D55)), // iOS Red
            targetScreen = CurrentScreen.PDF_HUB,
            testTag = "feature_card_pdf"
        ),
        FeatureModule(
            title = "Notepad",
            icon = Icons.Rounded.EditNote,
            gradientColors = listOf(Color(0xFFFF9500), Color(0xFFFFCC00)), // iOS Notes Yellow/Amber
            targetScreen = CurrentScreen.NOTE_EDITOR,
            testTag = "feature_card_notepad"
        ),
        FeatureModule(
            title = "Scan & Document",
            icon = Icons.Rounded.DocumentScanner,
            gradientColors = listOf(Color(0xFF007AFF), Color(0xFF5AC8FA)), // iOS Blue/Cyan
            targetScreen = CurrentScreen.DOCUMENT_SCANNER,
            testTag = "feature_card_scan_doc"
        ),
        FeatureModule(
            title = "Image",
            useModernGalleryIcon = true,
            gradientColors = listOf(Color(0xFF5856D6), Color(0xFFAF52DE)), // iOS Photos Indigo/Purple
            targetScreen = CurrentScreen.IMAGE_MEMORY,
            testTag = "feature_card_image"
        ),
        FeatureModule(
            title = "Photo Resizer",
            icon = Icons.Rounded.Crop,
            gradientColors = listOf(Color(0xFF00C7BE), Color(0xFF32ADE6)), // iOS Teal/Mint
            targetScreen = CurrentScreen.PHOTO_RESIZER,
            testTag = "feature_card_photo_resizer"
        ),
        FeatureModule(
            title = "Smart Invoice Maker",
            icon = Icons.Rounded.Description,
            gradientColors = listOf(Color(0xFFFF6482), Color(0xFFFF3B30)), // iOS Coral/Rose
            targetScreen = CurrentScreen.SMART_INVOICE_MAKER,
            testTag = "feature_card_smart_invoice"
        ),
        FeatureModule(
            title = "ID Stitcher",
            icon = Icons.Rounded.Badge,
            gradientColors = listOf(Color(0xFF5AC8FA), Color(0xFF007AFF)),
            targetScreen = CurrentScreen.ID_STITCHER,
            testTag = "feature_card_id_stitcher"
        ),
        FeatureModule(
            title = "Auto Sign",
            icon = Icons.Rounded.EditNote,
            gradientColors = listOf(Color(0xFF4CD964), Color(0xFF28CD41)),
            targetScreen = CurrentScreen.AUTO_SIGN,
            testTag = "feature_card_auto_sign"
        )
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .testTag("home_feature_cards_container")
    ) {
        Text(
            text = "Tools & Modules",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                color = Color.White
            ),
            modifier = Modifier.padding(bottom = 14.dp)
        )

        // 2-column grid layout for capsule tool tiles with iOS spring feedback
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            modules.chunked(2).forEach { rowModules ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    rowModules.forEach { module ->
                        Box(modifier = Modifier.weight(1f)) {
                            FeatureCardItem(
                                module = module,
                                onClick = { onFeatureClick(module.targetScreen) }
                            )
                        }
                    }
                    if (rowModules.size < 2) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun FeatureCardItem(
    module: FeatureModule,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0x991C1E22))
            .border(
                1.dp,
                Brush.verticalGradient(
                    listOf(Color(0x45FFFFFF), Color(0x18FFFFFF))
                ),
                RoundedCornerShape(20.dp)
            )
            .iosBounce(
                pressedScale = 0.935f,
                rippleColor = Color.White.copy(alpha = 0.2f),
                onClick = onClick
            )
            .padding(horizontal = 12.dp, vertical = 12.dp)
            .testTag(module.testTag)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Circular Frosted Icon Container
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0x20FFFFFF))
                    .border(
                        1.dp,
                        Color.White.copy(alpha = 0.35f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (module.useModernGalleryIcon) {
                    ModernGalleryIcon(
                        size = 26.dp,
                        tint = Color.White
                    )
                } else if (module.icon != null) {
                    Icon(
                        imageVector = module.icon,
                        contentDescription = module.title,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = module.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    lineHeight = 18.sp,
                    color = Color.White
                ),
                maxLines = 2,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
