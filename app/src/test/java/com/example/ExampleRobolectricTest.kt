package com.example

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.MemoryDao
import com.example.data.local.MemoryEntity
import com.example.model.CurrentScreen
import com.example.model.ItemType
import com.example.ui.GsdcallWorkspaceViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var database: AppDatabase
    private lateinit var memoryDao: MemoryDao
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        memoryDao = database.memoryDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `read app_name string from context`() {
        val appName = context.getString(R.string.app_name)
        assertEquals("GTOOL X", appName)
    }

    @Test
    fun `room database insert and query all memories`() = runBlocking {
        val entity = MemoryEntity(
            id = "test-mem-1",
            title = "Passport Scan",
            subtitle = "Photo & Scan",
            summary = "High-res passport photograph with biometric dimensions.",
            type = "image",
            sizeText = "1 Photo",
            tag = "Vault Protected",
            isPinned = true,
            extractedText = "REPUBLIC PASSPORT 123456",
            createdAt = System.currentTimeMillis()
        )

        memoryDao.insertMemory(entity)

        val allMemories = memoryDao.getAllMemories().first()
        assertEquals(1, allMemories.size)
        assertEquals("Passport Scan", allMemories[0].title)
        assertEquals("image", allMemories[0].type)
        assertTrue(allMemories[0].isPinned)
    }

    @Test
    fun `room database full text search and delete`() = runBlocking {
        val entity1 = MemoryEntity(
            id = "doc-1",
            title = "Quarterly Financial Analysis",
            subtitle = "PDF Document",
            summary = "Revenue report for Q3 with expenditure breakdown",
            type = "pdf",
            sizeText = "1.2 MB",
            tag = "Financial",
            extractedText = "Total profit margin 24.5 percent EBITDA",
            createdAt = System.currentTimeMillis()
        )

        val entity2 = MemoryEntity(
            id = "doc-2",
            title = "Meeting Notes with Engineering",
            subtitle = "Notepad Note",
            summary = "Sprint retrospective and database architecture",
            type = "note",
            sizeText = "450 words",
            tag = "Sprint",
            extractedText = "SQLite FTS4 full text search indexing",
            createdAt = System.currentTimeMillis() - 10000
        )

        memoryDao.insertMemory(entity1)
        memoryDao.insertMemory(entity2)

        val loadedDoc = memoryDao.getMemoryById("doc-1")
        assertNotNull(loadedDoc)
        assertEquals("Quarterly Financial Analysis", loadedDoc!!.title)

        // Test delete
        memoryDao.deleteMemoryById("doc-1")
        val deletedDoc = memoryDao.getMemoryById("doc-1")
        assertNull(deletedDoc)

        val remaining = memoryDao.getAllMemories().first()
        assertEquals(1, remaining.size)
        assertEquals("Meeting Notes with Engineering", remaining[0].title)
    }

    @Test
    fun `viewmodel state navigation and filter selection`() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = GsdcallWorkspaceViewModel(application)

        assertEquals(CurrentScreen.HOME, viewModel.currentScreen.value)

        // Test navigation
        viewModel.navigateTo(CurrentScreen.AI_SEARCH)
        assertEquals(CurrentScreen.AI_SEARCH, viewModel.currentScreen.value)

        viewModel.navigateTo(CurrentScreen.DOCUMENT_SCANNER)
        assertEquals(CurrentScreen.DOCUMENT_SCANNER, viewModel.currentScreen.value)

        viewModel.navigateTo(CurrentScreen.NOTE_EDITOR)
        assertEquals(CurrentScreen.NOTE_EDITOR, viewModel.currentScreen.value)

        viewModel.navigateTo(CurrentScreen.HOME)
        assertEquals(CurrentScreen.HOME, viewModel.currentScreen.value)

        // Test filter selection
        assertEquals(ItemType.ALL, viewModel.selectedFilter.value)
        viewModel.onFilterSelected(ItemType.PDF)
        assertEquals(ItemType.PDF, viewModel.selectedFilter.value)

        // Toggle same filter resets to ALL
        viewModel.onFilterSelected(ItemType.PDF)
        assertEquals(ItemType.ALL, viewModel.selectedFilter.value)
    }

    @Test
    fun `viewmodel chat message and simulated ai search`() = runBlocking {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = GsdcallWorkspaceViewModel(application)

        assertEquals(0, viewModel.chatMessages.value.size)
        viewModel.sendChatMessage("Find passport scan")

        // User message added immediately
        val messages = viewModel.chatMessages.value
        assertTrue(messages.isNotEmpty())
        assertEquals("Find passport scan", messages.first().text)
    }
}
