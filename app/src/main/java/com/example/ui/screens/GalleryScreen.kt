package com.example.ui.screens

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.model.WorkspaceItem
import com.example.ui.components.GalleryVaultSheet
import com.example.util.PreventScreenCapture

@Composable
fun GalleryScreen(
    items: List<WorkspaceItem> = emptyList(),
    onItemClick: (WorkspaceItem) -> Unit = {},
    onDeleteItem: (WorkspaceItem) -> Unit = {},
    onDismiss: () -> Unit = {},
    onUploadPdf: (Uri) -> Unit = {},
    onUploadImage: (Uri) -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Block screenshots, video recording, and recent-task leaks in Gallery
    PreventScreenCapture()

    GalleryVaultSheet(
        items = items,
        onItemClick = onItemClick,
        onDeleteItem = onDeleteItem,
        onDismiss = onDismiss,
        onUploadPdf = onUploadPdf,
        onUploadImage = onUploadImage,
        modifier = modifier
    )
}
