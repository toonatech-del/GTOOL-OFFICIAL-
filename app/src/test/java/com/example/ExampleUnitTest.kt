package com.example

import com.example.data.auth.FirebaseConfig
import com.example.model.ChatMessage
import com.example.model.CitationSource
import com.example.model.CurrentScreen
import com.example.model.ItemType
import com.example.model.MessageSender
import com.example.model.NotificationItem
import com.example.model.StorageData
import com.example.model.UserSession
import com.example.model.WorkspaceItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure JVM Unit Tests for Models, Data Classes, and Business Logic
 */
class ExampleUnitTest {

    @Test
    fun verify_math_and_basic_environment() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun verify_user_session_model_defaults_and_immutability() {
        val defaultSession = UserSession()
        assertFalse(defaultSession.isLoggedIn)
        assertEquals("", defaultSession.id)
        assertEquals("", defaultSession.name)
        assertEquals("", defaultSession.email)
        assertNull(defaultSession.avatarUrl)
        assertNull(defaultSession.accessToken)

        val activeSession = defaultSession.copy(
            id = "user_123",
            name = "Test User",
            email = "test@example.com",
            avatarUrl = "https://example.com/photo.jpg",
            isLoggedIn = true
        )
        assertTrue(activeSession.isLoggedIn)
        assertEquals("user_123", activeSession.id)
        assertEquals("Test User", activeSession.name)
        assertEquals("test@example.com", activeSession.email)
    }

    @Test
    fun verify_item_types_and_labels() {
        assertEquals("PDFs", ItemType.PDF.label)
        assertEquals("Images", ItemType.IMAGE.label)
        assertEquals("Notes", ItemType.NOTE.label)
        assertEquals("Voice Notes", ItemType.VOICE.label)
        assertEquals("All", ItemType.ALL.label)

        assertEquals("PDF", ItemType.PDF.extensionLabel)
        assertEquals("IMG", ItemType.IMAGE.extensionLabel)
        assertEquals("Note", ItemType.NOTE.extensionLabel)
        assertEquals("Audio", ItemType.VOICE.extensionLabel)
    }

    @Test
    fun verify_current_screen_navigation_routes() {
        val screens = CurrentScreen.values()
        assertTrue(screens.contains(CurrentScreen.HOME))
        assertTrue(screens.contains(CurrentScreen.NOTE_EDITOR))
        assertTrue(screens.contains(CurrentScreen.DOCUMENT_SCANNER))
        assertTrue(screens.contains(CurrentScreen.IMAGE_MEMORY))
        assertTrue(screens.contains(CurrentScreen.AI_SEARCH))
        assertTrue(screens.contains(CurrentScreen.PHOTO_RESIZER))
        assertTrue(screens.contains(CurrentScreen.VOICE_MEMORY))
        assertTrue(screens.contains(CurrentScreen.PDF_PREVIEW))
    }

    @Test
    fun verify_workspace_item_model_creation() {
        val item = WorkspaceItem(
            id = "mem-101",
            title = "Quarterly Tax Return.pdf",
            subtitle = "Financial • 2.4 MB",
            summary = "Annual tax summary with deduplication receipts.",
            dateModified = "Today, 10:30 AM",
            type = ItemType.PDF,
            sizeText = "2.4 MB",
            tag = "Vault Protected",
            isPinned = true,
            isSyncedToDrive = true
        )

        assertEquals("mem-101", item.id)
        assertEquals("Quarterly Tax Return.pdf", item.title)
        assertEquals(ItemType.PDF, item.type)
        assertTrue(item.isPinned)
        assertTrue(item.isSyncedToDrive)
        assertEquals("Vault Protected", item.tag)
    }

    @Test
    fun verify_storage_data_calculations() {
        val initialStorage = StorageData(
            usedGb = 1.25,
            totalGb = 15.0,
            pdfsGb = 0.8,
            imagesGb = 0.35,
            notesGb = 0.1,
            isOfflineReady = true,
            isGoogleDriveSynced = true,
            lastSyncTime = "Just now",
            syncAccountEmail = "user@example.com",
            syncStatusText = "Google Cloud & Encrypted Vault"
        )

        assertEquals(1.25, initialStorage.usedGb, 0.001)
        assertEquals(15.0, initialStorage.totalGb, 0.001)
        assertTrue(initialStorage.isOfflineReady)
        assertTrue(initialStorage.isGoogleDriveSynced)

        val updatedStorage = initialStorage.copy(
            usedGb = 0.0,
            lastSyncTime = "Optimized"
        )
        assertEquals(0.0, updatedStorage.usedGb, 0.001)
        assertEquals("Optimized", updatedStorage.lastSyncTime)
    }

    @Test
    fun verify_chat_message_and_citations() {
        val citation = CitationSource(
            id = "doc-01",
            title = "Project Specs",
            type = ItemType.NOTE,
            metaInfo = "Notepad Note • Yesterday",
            snippet = "Full-text indexing with SQLite FTS4"
        )

        val userMsg = ChatMessage(
            id = "msg-1",
            sender = MessageSender.USER,
            text = "Search my project specs",
            timestamp = "10:00 AM"
        )

        val aiMsg = ChatMessage(
            id = "msg-2",
            sender = MessageSender.AI,
            text = "Found 1 matching note in your vault.",
            timestamp = "10:01 AM",
            citations = listOf(citation)
        )

        assertEquals(MessageSender.USER, userMsg.sender)
        assertEquals(MessageSender.AI, aiMsg.sender)
        assertEquals(1, aiMsg.citations.size)
        assertEquals("Project Specs", aiMsg.citations.first().title)
    }

    @Test
    fun verify_notification_item_model() {
        val notification = NotificationItem(
            id = "notif-1",
            title = "Sync Complete",
            description = "All local vault documents are backed up",
            timeAgo = "5m ago",
            isUnread = true
        )

        assertTrue(notification.isUnread)
        assertEquals("Sync Complete", notification.title)
        val readNotif = notification.copy(isUnread = false)
        assertFalse(readNotif.isUnread)
    }

    @Test
    fun verify_firebase_config_sanitization_defaults() {
        // Ensure no hardcoded secrets or production project IDs in default constants
        assertEquals("", FirebaseConfig.DEFAULT_API_KEY)
        assertEquals("", FirebaseConfig.DEFAULT_AUTH_DOMAIN)
        assertEquals("", FirebaseConfig.DEFAULT_PROJECT_ID)
        assertEquals("", FirebaseConfig.DEFAULT_APP_ID)
        assertEquals("", FirebaseConfig.DEFAULT_WEB_CLIENT_ID)
    }
}
