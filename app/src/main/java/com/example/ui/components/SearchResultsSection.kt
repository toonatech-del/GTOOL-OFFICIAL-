package com.example.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.model.WorkspaceItem

/**
 * Empty stub for SearchResultsSection. Per user request, searches must show under chatbox, not at home screen.
 */
@Composable
fun SearchResultsSection(
    query: String,
    results: List<WorkspaceItem>,
    onItemClick: (WorkspaceItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier)
}
