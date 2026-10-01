package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import com.example.model.CurrentScreen
import com.example.ui.GsdcallWorkspaceViewModel

/**
 * PdfCreatorScreen - High-performance PDF Creation Studio with Backstack Safety & Debounce.
 * Prevents Jetpack Compose backstack loops by popping the creator route on back or save.
 */
@Composable
fun PdfCreatorScreen(
    onBackClick: () -> Unit,
    viewModel: GsdcallWorkspaceViewModel,
    onSaveSuccess: ((Uri) -> Unit)? = null
) {
    // Explicit BackHandler to cleanly pop stack and prevent looping back into creator
    BackHandler {
        onBackClick()
    }

    PdfEditorScreen(
        onBackClick = onBackClick,
        viewModel = viewModel,
        onSaveSuccess = { savedUri ->
            if (onSaveSuccess != null) {
                onSaveSuccess(savedUri)
            } else {
                // Clear PdfCreator/PdfEditor from the backstack so pressing Back goes directly to Home/Hub
                viewModel.navigateAndPopUpTo(
                    destination = CurrentScreen.PDF_HUB,
                    popUpToScreen = CurrentScreen.PDF_EDITOR,
                    inclusive = true
                )
            }
        }
    )
}
