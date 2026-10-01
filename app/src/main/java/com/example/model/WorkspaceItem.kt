package com.example.model

enum class ItemType(val label: String, val extensionLabel: String) {
    ALL("All", ""),
    NOTE("Notes", "Note"),
    PDF("PDFs", "PDF"),
    IMAGE("Images", "IMG"),
    VOICE("Voice Notes", "Audio"),
    ID_CARD("ID Cards", "IDCard"),
    SIGNATURE("Signatures", "Sign")
}

data class WorkspaceItem(
    val id: String,
    val title: String,
    val type: ItemType,
    val dateModified: String,
    val sizeText: String = "1.2 MB",
    val summary: String,
    val subtitle: String,
    val isPinned: Boolean = false,
    val tag: String? = null,
    val statusText: String? = null,
    val contentSnippet: String? = null,
    val imageUri: String? = null,
    val isSyncedToDrive: Boolean = false,
    val driveFileId: String? = null,
    val dueDate: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

data class StorageData(
    val usedGb: Double = 0.0,
    val totalGb: Double = 15.0,
    val pdfsGb: Double = 0.0,
    val imagesGb: Double = 0.0,
    val notesGb: Double = 0.0,
    val isOfflineReady: Boolean = true,
    val isGoogleDriveSynced: Boolean = false,
    val lastSyncTime: String = "Just now",
    val syncAccountEmail: String = "",
    val syncStatusText: String = "Local Encrypted Storage"
)

data class NotificationItem(
    val id: String,
    val title: String,
    val description: String,
    val timeAgo: String,
    val isUnread: Boolean = true,
    val scheduledTimeMillis: Long? = null,
    val isReminder: Boolean = false,
    val reminderId: Long? = null
)

enum class MessageSender {
    USER,
    AI
}

data class CitationSource(
    val id: String,
    val title: String,
    val type: ItemType,
    val metaInfo: String,
    val snippet: String,
    val imageUri: String? = null,
    val previewThumbnailUri: String? = null,
    val extractedValue: String = "",
    val moduleSourceLabel: String = "",
    val contextPreview: String = "",
    val fileName: String = ""
)

data class ChatMessage(
    val id: String,
    val sender: MessageSender,
    val text: String,
    val timestamp: String,
    val citations: List<CitationSource> = emptyList(),
    val reminderTitle: String? = null,
    val reminderDate: Long? = null,
    val isReminderSet: Boolean = false
)

enum class CurrentScreen {
    HOME,
    AI_SEARCH,
    NOTE_EDITOR,
    IMAGE_MEMORY,
    VOICE_MEMORY,
    DOCUMENT_SCANNER,
    PDF_HUB,
    PDF_PREVIEW,
    PDF_EDITOR,
    PHOTO_RESIZER,
    SMART_INVOICE_MAKER,
    ID_STITCHER,
    AUTO_SIGN,
    TERMS_ONBOARDING,
    SETTINGS_TERMS,
    PRIVACY_POLICY,
    TERMS_CONDITIONS
}
