package com.example.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Draw
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ItemType
import com.example.ui.theme.AmberWarm
import com.example.ui.theme.ImagePurple
import com.example.ui.theme.NoteYellow
import com.example.ui.theme.PdfRed
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VoiceCyan
import com.example.ui.theme.WarmGold

import com.example.ui.theme.LocalIsDarkMode

data class FilterItem(
    val type: ItemType,
    val label: String,
    val icon: ImageVector,
    val tag: String
)

@Composable
fun FilterRow(
    selectedType: ItemType,
    onFilterSelected: (ItemType) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDarkMode = LocalIsDarkMode.current
    val filters = listOf(
        FilterItem(ItemType.ALL, "All", Icons.Rounded.GridView, "filter_all"),
        FilterItem(ItemType.NOTE, "Notes", Icons.Rounded.EditNote, "filter_notes"),
        FilterItem(ItemType.PDF, "PDFs", Icons.Rounded.PictureAsPdf, "filter_pdfs"),
        FilterItem(ItemType.IMAGE, "Images", ModernGalleryIconVector, "filter_images"),
        FilterItem(ItemType.ID_CARD, "ID Cards", Icons.Rounded.Badge, "filter_id_cards"),
        FilterItem(ItemType.SIGNATURE, "Signatures", Icons.Rounded.Draw, "filter_signatures")
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .testTag("filter_capsule_row"),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        filters.forEach { filter ->
            val isSelected = selectedType == filter.type
            val iconTint = if (isSelected) {
                if (isDarkMode) WarmGold else Color.White
            } else {
                when (filter.type) {
                    ItemType.PDF -> PdfRed
                    ItemType.NOTE -> NoteYellow
                    ItemType.IMAGE -> ImagePurple
                    ItemType.VOICE -> VoiceCyan
                    else -> if (isDarkMode) TextSecondary else Color(0xFF111111)
                }
            }

            val textColor = if (isSelected) {
                if (isDarkMode) TextPrimary else Color.White
            } else {
                if (isDarkMode) TextSecondary else Color(0xFF111111)
            }

            GlassCapsule(
                selected = isSelected,
                onClick = { onFilterSelected(filter.type) },
                modifier = Modifier.testTag(filter.tag)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = filter.icon,
                        contentDescription = filter.label,
                        tint = iconTint,
                        modifier = Modifier.size(16.dp)
                    )

                    Spacer(modifier = Modifier.width(7.dp))

                    Text(
                        text = filter.label,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = textColor
                        )
                    )
                }
            }
        }
    }
}
