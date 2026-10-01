package com.example.ui.components

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.model.ItemType
import com.example.model.WorkspaceItem
import com.example.ui.theme.AmberGlow
import com.example.ui.theme.AmberWarm
import com.example.ui.theme.ImagePurple
import com.example.ui.theme.LocalIsDarkMode
import com.example.ui.theme.NoteYellow
import com.example.ui.theme.PdfRed
import com.example.ui.theme.SyncGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarmGold
import com.example.util.ShareUtils
import kotlinx.coroutines.launch
import java.io.File
import java.util.Calendar

/**
 * Date grouping categories for recent workspace items
 */
enum class DateGroup(val title: String, val badgeColor: Color) {
    TODAY("Today", Color(0xFFFF9E58)),
    YESTERDAY("Yesterday", Color(0xFF00E5FF)),
    THIS_WEEK("This Week", Color(0xFFA78BFA)),
    EARLIER("Earlier", Color(0xFF94A3B8))
}

/**
 * Groups WorkspaceItems into Date buckets (Today, Yesterday, This Week, Earlier)
 */
fun groupWorkspaceItemsByDate(items: List<WorkspaceItem>): List<Pair<DateGroup, List<WorkspaceItem>>> {
    val now = Calendar.getInstance()

    val startOfToday = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    val startOfYesterday = Calendar.getInstance().apply {
        timeInMillis = startOfToday
        add(Calendar.DAY_OF_YEAR, -1)
    }.timeInMillis

    val startOfWeek = Calendar.getInstance().apply {
        timeInMillis = startOfToday
        add(Calendar.DAY_OF_YEAR, -6)
    }.timeInMillis

    val groupsMap = mutableMapOf<DateGroup, MutableList<WorkspaceItem>>()
    DateGroup.values().forEach { groupsMap[it] = mutableListOf() }

    for (item in items) {
        val time = if (item.createdAt > 0) item.createdAt else System.currentTimeMillis()
        val group = when {
            time >= startOfToday -> DateGroup.TODAY
            time >= startOfYesterday -> DateGroup.YESTERDAY
            time >= startOfWeek -> DateGroup.THIS_WEEK
            else -> DateGroup.EARLIER
        }
        groupsMap[group]?.add(item)
    }

    return DateGroup.values().mapNotNull { group ->
        val list = groupsMap[group]
        if (!list.isNullOrEmpty()) Pair(group, list) else null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecentItemsSection(
    items: List<WorkspaceItem>,
    onItemClick: (WorkspaceItem) -> Unit,
    onTogglePin: (WorkspaceItem) -> Unit,
    onDeleteClick: (WorkspaceItem) -> Unit = {},
    onShareClick: ((WorkspaceItem) -> Unit)? = null,
    onMenuClick: (WorkspaceItem) -> Unit = {},
    onAddMemoryClick: () -> Unit = {},
    onSetReminder: (WorkspaceItem) -> Unit = {},
    selectedFilter: ItemType = ItemType.ALL,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDarkMode = LocalIsDarkMode.current
    val headerColor = if (isDarkMode) Color(0xFFE6E5DF) else Color(0xFF000000)

    val effectiveShareClick: (WorkspaceItem) -> Unit = { item ->
        if (onShareClick != null) {
            onShareClick(item)
        } else {
            ShareUtils.shareItem(context, item)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .testTag("recent_items_section")
    ) {
        // Section Header with large clear text & action hint
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Recent items",
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 24.sp,
                        letterSpacing = (-0.4).sp,
                        color = headerColor
                    ),
                    modifier = Modifier.testTag("recent_items_title")
                )
                Text(
                    text = if (items.isNotEmpty()) "Swipe right to Share, swipe left to Delete" else "Your saved memories and indexed documents",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = if (isDarkMode) TextSecondary else Color(0xFF333333),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
            }

            if (items.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x18FFFFFF))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = "${items.size} files",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = WarmGold,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (items.isEmpty()) {
            val emptyTitle = when (selectedFilter) {
                ItemType.PDF -> "No PDFs saved yet"
                ItemType.IMAGE -> "No images saved yet"
                ItemType.NOTE -> "No notes saved yet"
                else -> "No memories saved yet"
            }
            MinimalistEmptyStateCard(
                title = emptyTitle,
                onAddClick = onAddMemoryClick
            )
        } else {
            // Group items by Date (Today, Yesterday, This Week, Earlier)
            val dateGroups = remember(items) { groupWorkspaceItemsByDate(items) }

            Column(
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                dateGroups.forEach { (dateGroup, groupItems) ->
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Date Group Header
                        DateGroupHeader(
                            group = dateGroup,
                            itemCount = groupItems.size,
                            isDarkMode = isDarkMode
                        )

                        // List of Swipeable Translucent Cards in this Date Group
                        groupItems.forEach { item ->
                            SwipeableRecentTranslucentCard(
                                item = item,
                                onClick = { onItemClick(item) },
                                onTogglePin = { onTogglePin(item) },
                                onDeleteClick = { onDeleteClick(item) },
                                onShareClick = { effectiveShareClick(item) },
                                onMenuClick = { onMenuClick(item) },
                                onSetReminder = onSetReminder
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Sticky/Section Header for Date Grouping (Today, Yesterday, This Week, Earlier)
 */
@Composable
private fun DateGroupHeader(
    group: DateGroup,
    itemCount: Int,
    isDarkMode: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(group.badgeColor.copy(alpha = 0.18f))
                    .border(1.dp, group.badgeColor.copy(alpha = 0.45f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (group) {
                        DateGroup.TODAY -> Icons.Rounded.AccessTime
                        DateGroup.YESTERDAY -> Icons.Rounded.CalendarToday
                        DateGroup.THIS_WEEK -> Icons.Rounded.CalendarToday
                        DateGroup.EARLIER -> Icons.Rounded.FolderOpen
                    },
                    contentDescription = null,
                    tint = group.badgeColor,
                    modifier = Modifier.size(13.dp)
                )
            }

            Text(
                text = group.title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    letterSpacing = 0.2.sp,
                    color = if (isDarkMode) Color.White.copy(alpha = 0.92f) else Color(0xFF1E293B)
                )
            )

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isDarkMode) Color(0x14FFFFFF) else Color(0x10000000))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "$itemCount",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = if (isDarkMode) TextMuted else Color(0xFF64748B),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }

        // Subtle glowing divider line
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp)
                .height(0.8.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            group.badgeColor.copy(alpha = 0.35f),
                            Color.Transparent
                        )
                    )
                )
        )
    }
}

/**
 * Swipeable Wrapper with SwipeToDismissBox
 * Left-to-Right = Share (Cyan/Green Glow)
 * Right-to-Left = Delete (Red Glow)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableRecentTranslucentCard(
    item: WorkspaceItem,
    onClick: () -> Unit,
    onTogglePin: (WorkspaceItem) -> Unit,
    onDeleteClick: (WorkspaceItem) -> Unit,
    onShareClick: (WorkspaceItem) -> Unit,
    onMenuClick: (WorkspaceItem) -> Unit,
    onSetReminder: (WorkspaceItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            when (dismissValue) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    // Swiped Right -> Share
                    onShareClick(item)
                    false // Snap back gracefully after opening Share sheet
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    // Swiped Left -> Delete
                    onDeleteClick(item)
                    false // Snap back to allow user confirmation / dialog
                }
                SwipeToDismissBoxValue.Settled -> false
            }
        },
        positionalThreshold = { it * 0.35f }
    )

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp), // Standardized outer padding
        backgroundContent = {
            SwipeActionBackground(
                dismissState = dismissState,
                onDeleteClick = { onDeleteClick(item) },
                onShareClick = { onShareClick(item) }
            )
        },
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = true
    ) {
        RecentTranslucentCard(
            item = item,
            onClick = onClick,
            onTogglePin = onTogglePin,
            onDeleteClick = onDeleteClick,
            onMenuClick = onMenuClick,
            onSetReminder = onSetReminder
        )
    }
}

/**
 * Background rendering for swipe actions (Share on left, Delete on right)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeActionBackground(
    dismissState: androidx.compose.material3.SwipeToDismissBoxState,
    onDeleteClick: () -> Unit = {},
    onShareClick: () -> Unit = {}
) {
    val direction = dismissState.dismissDirection
    val isStartToEnd = direction == SwipeToDismissBoxValue.StartToEnd
    val isEndToStart = direction == SwipeToDismissBoxValue.EndToStart

    val backgroundColor by animateColorAsState(
        targetValue = when (direction) {
            SwipeToDismissBoxValue.StartToEnd -> Color(0xFF00E5FF).copy(alpha = 0.25f)
            SwipeToDismissBoxValue.EndToStart -> Color(0xFFFF3B30).copy(alpha = 0.28f)
            SwipeToDismissBoxValue.Settled -> Color.Transparent
        },
        label = "SwipeBgColor"
    )

    val borderColor by animateColorAsState(
        targetValue = when (direction) {
            SwipeToDismissBoxValue.StartToEnd -> Color(0xFF00E5FF).copy(alpha = 0.6f)
            SwipeToDismissBoxValue.EndToStart -> Color(0xFFFF3B30).copy(alpha = 0.6f)
            SwipeToDismissBoxValue.Settled -> Color.Transparent
        },
        label = "SwipeBorderColor"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(16.dp)) // Matches standardized card shape
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable {
                if (isEndToStart) {
                    onDeleteClick()
                } else if (isStartToEnd) {
                    onShareClick()
                }
            }
            .padding(horizontal = 22.dp),
        contentAlignment = if (isStartToEnd) Alignment.CenterStart else Alignment.CenterEnd
    ) {
        if (isStartToEnd) {
            // Share Action
            Row(
                modifier = Modifier.clickable { onShareClick() },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF00E5FF).copy(alpha = 0.35f))
                        .border(1.dp, Color(0xFF00E5FF), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Share,
                        contentDescription = "Share",
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = "Share",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00E5FF),
                        fontSize = 14.sp
                    )
                )
            }
        } else if (isEndToStart) {
            // Delete Action
            Row(
                modifier = Modifier.clickable { onDeleteClick() },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Delete",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF5252),
                        fontSize = 14.sp
                    )
                )
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFF3B30).copy(alpha = 0.35f))
                        .border(1.dp, Color(0xFFFF5252), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = "Delete",
                        tint = Color(0xFFFF5252),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/**
 * Minimalist frosted glass empty-state card
 */
@Composable
fun MinimalistEmptyStateCard(
    onAddClick: () -> Unit = {},
    title: String = "No memories saved yet",
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("empty_state_card"),
        shape = RoundedCornerShape(24.dp),
        borderStrokeWidth = 1.dp,
        borderColorList = listOf(
            Color(0x55FFFFFF),
            Color(0x18FFFFFF),
            Color(0x35FFFFFF)
        ),
        backgroundGradient = listOf(
            Color(0x22FFFFFF),
            Color(0x12FFFFFF),
            Color(0x0CFFFFFF)
        ),
        ambientGlowColor = AmberWarm
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 36.dp, horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFFFF9500), Color(0xFFFF5E3A))
                        )
                    )
                    .border(
                        1.2.dp,
                        Color.White.copy(alpha = 0.4f),
                        RoundedCornerShape(18.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.FolderOpen,
                        contentDescription = "Empty memory vault",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier
                            .size(13.dp)
                            .align(Alignment.TopEnd)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 19.sp,
                    letterSpacing = (-0.3).sp,
                    fontFamily = FontFamily.SansSerif,
                    color = TextPrimary
                ),
                modifier = Modifier.testTag("empty_state_title")
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Select a tool above to create your first note, PDF, or image.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = TextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    textAlign = TextAlign.Center
                ),
                modifier = Modifier.testTag("empty_state_subtitle")
            )
        }
    }
}

/**
 * Translucent Card with Rich Thumbnail Preview & Information
 */
@Composable
fun RecentTranslucentCard(
    item: WorkspaceItem,
    onClick: () -> Unit,
    onTogglePin: (WorkspaceItem) -> Unit,
    onDeleteClick: (WorkspaceItem) -> Unit = {},
    onMenuClick: (WorkspaceItem) -> Unit = {},
    onSetReminder: (WorkspaceItem) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isDarkMode = LocalIsDarkMode.current
    val (typeGradient, typeIcon) = when (item.type) {
        ItemType.PDF -> Pair(listOf(Color(0xFFFF3B30), Color(0xFFFF2D55)), Icons.Rounded.PictureAsPdf)
        ItemType.NOTE -> Pair(listOf(Color(0xFFFF9500), Color(0xFFFFCC00)), Icons.Rounded.EditNote)
        ItemType.IMAGE -> Pair(listOf(Color(0xFF5856D6), Color(0xFFAF52DE)), Icons.Rounded.Image)
        ItemType.VOICE -> Pair(listOf(Color(0xFFFF2D55), Color(0xFFFF9500)), Icons.Rounded.GraphicEq)
        else -> Pair(listOf(Color(0xFFFF9500), Color(0xFFFF5E3A)), Icons.Rounded.Description)
    }

    val cardTitleColor = if (isDarkMode) TextPrimary else Color(0xFF111111)
    val cardSubtitleColor = if (isDarkMode) TextSecondary else Color(0xFF333333)
    val cardMutedColor = if (isDarkMode) TextMuted else Color(0xFF4A4A4A)

    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 88.dp, max = 110.dp) // Strictly standardized height range
            .testTag("recent_card_${item.id}"),
        shape = RoundedCornerShape(16.dp), // Standardized 16dp radius
        borderStrokeWidth = 1.dp,
        ambientGlowColor = typeGradient.first().copy(alpha = 0.4f),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp) // Standardized internal padding
        ) {
            // Row 1: Top Section (Thumbnail, Info, Actions)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left thumbnail/icon: Fixed size 44.dp
                ItemThumbnailPreview(
                    item = item,
                    typeGradient = typeGradient,
                    typeIcon = typeIcon,
                    modifier = Modifier.size(44.dp)
                )

                // Title & Subtitle Column: weight(1f)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp)
                ) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            letterSpacing = (-0.2).sp,
                            color = cardTitleColor
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("recent_item_title_${item.id}")
                    )

                    Text(
                        text = "${item.sizeText} • ${item.dateModified}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = cardMutedColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Right Action Icons (Pin, Overflow)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    IconButton(
                        onClick = { onTogglePin(item) },
                        modifier = Modifier
                            .size(28.dp)
                            .testTag("btn_pin_${item.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PushPin,
                            contentDescription = if (item.isPinned) "Unpin" else "Pin",
                            tint = if (item.isPinned) AmberWarm else cardMutedColor.copy(alpha = 0.5f),
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    IconButton(
                        onClick = { onMenuClick(item) },
                        modifier = Modifier
                            .size(28.dp)
                            .testTag("btn_menu_${item.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MoreVert,
                            contentDescription = "Options",
                            tint = cardSubtitleColor.copy(alpha = 0.6f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Row 2: Middle Section (Description / Metadata)
            Text(
                text = item.subtitle.ifBlank { "No additional metadata available" },
                style = MaterialTheme.typography.bodySmall.copy(
                    color = cardSubtitleColor,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Normal
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Row 3: Bottom Row (Tags / AI Pills)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Fixed compact tag pill (max 26.dp height)
                Box(
                    modifier = Modifier
                        .height(22.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0x12FFFFFF))
                        .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(6.dp))
                        .padding(horizontal = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "✦",
                            color = WarmGold,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = item.summary.ifBlank { item.type.label },
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = WarmGold,
                                fontSize = 11.sp, // Updated to 11.sp as requested
                                fontWeight = FontWeight.SemiBold
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Optional compact Due Date indicator
                if (item.dueDate != null) {
                    val dateStr = java.text.SimpleDateFormat("dd MMM", java.util.Locale.getDefault()).format(java.util.Date(item.dueDate))
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(5.dp))
                            .background(Color(0x22FF9E58))
                            .clickable { onSetReminder(item) }
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AccessTime,
                            contentDescription = null,
                            tint = WarmGold,
                            modifier = Modifier.size(9.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "Due: $dateStr",
                            color = WarmGold,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    // Modern iOS Chevron
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        contentDescription = null,
                        tint = cardMutedColor.copy(alpha = 0.4f),
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}

/**
 * Renders rich thumbnail previews: image thumbnails, PDF first-page renders, or styled vector icon containers.
 */
@Composable
fun ItemThumbnailPreview(
    item: WorkspaceItem,
    typeGradient: List<Color>,
    typeIcon: ImageVector,
    modifier: Modifier = Modifier
) {
    val hasValidImageUri = !item.imageUri.isNullOrBlank()

    val bgModifier = if (hasValidImageUri) {
        Modifier.background(Color(0x1A000000))
    } else {
        Modifier.background(Brush.linearGradient(typeGradient.map { it.copy(alpha = 0.28f) }))
    }

    Box(
        modifier = modifier
            .size(44.dp) // Standardized fixed size 44.dp
            .clip(RoundedCornerShape(12.dp)) // Compact corner radius for thumbnail
            .then(bgModifier)
            .border(
                1.dp,
                if (hasValidImageUri) typeGradient.first().copy(alpha = 0.5f)
                else Color.White.copy(alpha = 0.35f),
                RoundedCornerShape(12.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        if (hasValidImageUri) {
            Image(
                painter = rememberAsyncImagePainter(model = Uri.parse(item.imageUri)),
                contentDescription = item.title,
                contentScale = ContentScale.Crop, // Prevent expansion, crop to fit 44dp
                modifier = Modifier.fillMaxSize()
            )

            // Mini overlay type badge at bottom right
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(2.dp)
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(typeGradient.first())
                    .border(0.5.dp, Color.White.copy(alpha = 0.7f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = typeIcon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(8.dp)
                )
            }
        } else {
            // Elegant Frosted Icon with subtle gradient glow
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            listOf(
                                typeGradient.first().copy(alpha = 0.3f),
                                Color.Transparent
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = typeIcon,
                    contentDescription = item.type.label,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
