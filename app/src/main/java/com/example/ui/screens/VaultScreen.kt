package com.example.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.model.WorkspaceItem
import com.example.ui.components.GalleryVaultSheet
import com.example.util.PreventScreenCapture

@Composable
fun VaultScreen(
    items: List<WorkspaceItem> = emptyList(),
    onItemClick: (WorkspaceItem) -> Unit = {},
    onDeleteItem: (WorkspaceItem) -> Unit = {},
    onDismiss: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Block screenshots, video recording, and recent-task leaks in Vault
    PreventScreenCapture()

    GalleryVaultSheet(
        items = items,
        onItemClick = onItemClick,
        onDeleteItem = onDeleteItem,
        onDismiss = onDismiss,
        modifier = modifier
    )
}
