package com.example.ui.navigation

import android.net.Uri

/**
 * Route definitions and navigation utilities for Jetpack Compose.
 */
sealed class Screen(val route: String) {
    object HomeScreen : Screen("home_screen")
    object PdfHub : Screen("pdf_hub")
    object PdfCreator : Screen("pdf_creator")
    object PdfEditor : Screen("pdf_editor")
    object PdfPreview : Screen("pdf_preview")
    object PdfViewer : Screen("pdf_viewer/{pdfUri}") {
        fun createRoute(pdfUri: Uri): String = "pdf_viewer/${Uri.encode(pdfUri.toString())}"
        fun createRoute(pdfUriString: String): String = "pdf_viewer/${Uri.encode(pdfUriString)}"
    }
    object NoteEditor : Screen("note_editor")
    object ImageMemory : Screen("image_memory")
    object VoiceMemory : Screen("voice_memory")
    object AiSearch : Screen("ai_search")
    object PhotoResizer : Screen("photo_resizer")
    object SmartInvoice : Screen("smart_invoice")
}

/**
 * Helper navigation options container to safely handle singleTop and popUpTo inclusive flags.
 */
data class NavigationOptions(
    val popUpToRoute: String? = null,
    val inclusive: Boolean = false,
    val launchSingleTop: Boolean = true
)
