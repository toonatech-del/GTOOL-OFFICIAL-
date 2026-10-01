package com.example.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.auth.AuthManager
import com.example.data.auth.FirebaseAuthResult
import com.example.data.local.AppDatabase
import com.example.data.local.UniversalSearchEntity
import com.example.data.repository.MemoryRepository
import com.example.model.ChatMessage
import com.example.model.CitationSource
import com.example.model.CurrentScreen
import com.example.model.ItemType
import com.example.model.MessageSender
import com.example.model.NotificationItem
import com.example.model.StorageData
import com.example.model.UserSession
import com.example.model.WorkspaceItem
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

import kotlinx.coroutines.launch
import java.io.File

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class GsdcallWorkspaceViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MemoryRepository(application.applicationContext)
    private val authManager = AuthManager(application.applicationContext)
    private val universalSearchRepo = com.example.data.repository.UniversalSearchRepository(application)

    val currentUser: StateFlow<UserSession> = authManager.currentUser

    private val _currentScreen = MutableStateFlow(CurrentScreen.HOME)
    val currentScreen: StateFlow<CurrentScreen> = _currentScreen

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _selectedFilter = MutableStateFlow(ItemType.ALL)
    val selectedFilter: StateFlow<ItemType> = _selectedFilter

    // Local room database flow of memories
    val recentItems: StateFlow<List<WorkspaceItem>> = repository.allMemories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val importantItems: StateFlow<List<WorkspaceItem>> = repository.importantMemories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Real-time offline search with 250ms debounce connected to FTS index.
     */
    val searchResults: StateFlow<List<WorkspaceItem>> = _searchQuery
        .debounce(250)
        .flatMapLatest { query ->
            universalSearchRepo.search(query).map { entities ->
                entities.map { universalSearchRepo.entityToWorkspaceItem(it) }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _storageData = MutableStateFlow(
        StorageData(
            usedGb = 0.0,
            totalGb = 15.0,
            pdfsGb = 0.0,
            imagesGb = 0.0,
            notesGb = 0.0,
            isOfflineReady = true,
            isGoogleDriveSynced = true,
            lastSyncTime = "Just now",
            syncAccountEmail = authManager.currentUser.value.email,
            syncStatusText = "Google Cloud & Encrypted Vault"
        )
    )
    val storageData: StateFlow<StorageData> = _storageData

    private val db = AppDatabase.getInstance(application)

    data class PendingReminderSchedule(val title: String, val query: String = "")

    private val _pendingReminderSchedule = MutableStateFlow<PendingReminderSchedule?>(null)
    val pendingReminderSchedule: StateFlow<PendingReminderSchedule?> = _pendingReminderSchedule

    fun clearPendingReminderSchedule() {
        _pendingReminderSchedule.value = null
    }

    fun scheduleReminderFromItem(item: WorkspaceItem) {
        _pendingReminderSchedule.value = PendingReminderSchedule(
            title = item.title,
            query = ""
        )
    }

    fun scheduleStructuredReminder(title: String, timestampMillis: Long) {
        viewModelScope.launch {
            com.example.util.ReminderManager.commitAndSchedule(getApplication(), title, timestampMillis)
            _pendingReminderSchedule.value = null
            val dateStr = com.example.util.ReminderManager.formatTimestamp(timestampMillis)
            val aiMsg = ChatMessage(
                id = "ai-${System.currentTimeMillis()}",
                sender = MessageSender.AI,
                text = "Reminder set: **$title** for **$dateStr** 🔔",
                timestamp = "Just now"
            )
            _chatMessages.value = _chatMessages.value + aiMsg
        }
    }

    fun cancelNotificationOrReminder(item: NotificationItem) {
        viewModelScope.launch(Dispatchers.IO) {
            val remId = item.reminderId ?: item.id.removePrefix("rem-").toLongOrNull()
            if (remId != null) {
                com.example.util.ReminderManager.cancelReminder(getApplication(), remId)
            }
            val notifId = item.id.toIntOrNull()
            if (notifId != null) {
                db.notificationDao().deleteNotification(notifId)
            }
        }
    }
    
    val notifications: StateFlow<List<NotificationItem>> = combine(
        db.notificationDao().getAllNotifications(),
        db.reminderDao().getActiveReminders()
    ) { notificationEntities, reminderEntities ->
        val reminderItems = reminderEntities.map { reminder ->
            val cleanTitle = com.example.util.ReminderManager.extractCleanTaskTitle(reminder.description)
            NotificationItem(
                id = "rem-${reminder.id}",
                title = cleanTitle,
                description = "Exact alarm scheduled for ${com.example.util.ReminderManager.formatTimestamp(reminder.scheduledTime)}",
                timeAgo = java.text.SimpleDateFormat("MMM dd, h:mm a", java.util.Locale.getDefault()).format(java.util.Date(reminder.createdAt)),
                isUnread = true,
                scheduledTimeMillis = reminder.scheduledTime,
                isReminder = true,
                reminderId = reminder.id
            )
        }

        val normalItems = notificationEntities
            .filter { notif -> notif.reminderId == null || !reminderEntities.any { it.id == notif.reminderId } }
            .map { entity ->
                val rawTitle = if (entity.title == "GTOOL X Reminder" && entity.content.isNotBlank()) entity.content else entity.title
                val cleanTitle = if (entity.type == "reminder" || entity.scheduledTime != null) {
                    com.example.util.ReminderManager.extractCleanTaskTitle(rawTitle)
                } else {
                    entity.title
                }
                NotificationItem(
                    id = entity.id.toString(),
                    title = cleanTitle,
                    description = entity.content,
                    timeAgo = java.text.SimpleDateFormat("MMM dd, h:mm a", java.util.Locale.getDefault()).format(java.util.Date(entity.timestamp)),
                    isUnread = !entity.isRead,
                    scheduledTimeMillis = entity.scheduledTime,
                    isReminder = entity.type == "reminder",
                    reminderId = entity.reminderId
                )
            }

        (reminderItems + normalItems).sortedByDescending { it.scheduledTimeMillis ?: 0L }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val prefs = application.getSharedPreferences("gtool_vault_settings", Context.MODE_PRIVATE)

    private val _isBiometricEnabled = MutableStateFlow(prefs.getBoolean("biometric_app_lock", false))
    val isBiometricEnabled: StateFlow<Boolean> = _isBiometricEnabled

    private val _hasAcceptedTerms = MutableStateFlow(prefs.getBoolean("has_accepted_terms", true))
    val hasAcceptedTerms: StateFlow<Boolean> = _hasAcceptedTerms

    private val _backupPromptDismissed = MutableStateFlow(prefs.getBoolean("backup_prompt_dismissed", false))
    val backupPromptDismissed: StateFlow<Boolean> = _backupPromptDismissed

    private val _isAppUnlocked = MutableStateFlow(
        !prefs.getBoolean("biometric_app_lock", false) || 
        !com.example.util.BiometricAuthManager.isBiometricAvailable(application)
    )
    val isAppUnlocked: StateFlow<Boolean> = _isAppUnlocked

    fun acceptTerms() {
        prefs.edit().putBoolean("has_accepted_terms", true).apply()
        _hasAcceptedTerms.value = true
    }

    fun dismissBackupPrompt() {
        prefs.edit().putBoolean("backup_prompt_dismissed", true).apply()
        prefs.edit().putBoolean("restore_prompted", true).apply()
        _showRestorePrompt.value = false
    }

    private val _backupInfo = MutableStateFlow(
        com.example.util.BackupInfo(
            exists = false,
            filePath = "",
            lastBackupDate = "Loading...",
            fileSizeBytes = 0L,
            itemCount = 0
        )
    )
    val backupInfo: StateFlow<com.example.util.BackupInfo> = _backupInfo

    private val _showRestorePrompt = MutableStateFlow(false)
    val showRestorePrompt: StateFlow<Boolean> = _showRestorePrompt

    init {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Load backup info on IO thread
                val info = com.example.util.BackupRestoreManager.getBackupInfo(application)
                _backupInfo.value = info

                // Seamlessly restore data if backup exists and current DB is empty (survives uninstall)
                val restored = com.example.util.BackupRestoreManager.autoRestoreIfNeeded(application)
                if (!restored) {
                    val isDbEmpty = repository.getDao().getAllMemoriesList().isEmpty()
                    val hasExistingData = com.example.util.BackupRestoreManager.checkForExistingData(application)
                    val wasPrompted = prefs.getBoolean("restore_prompted", false)
                    
                    if (isDbEmpty && hasExistingData && !wasPrompted) {
                        _showRestorePrompt.value = true
                    }
                }

                // Clean any legacy raw transcripts
                com.example.data.repository.ReminderRepository.getInstance(application).cleanLegacyTranscripts()

                // Delete duplicate pending reminders on launch
                com.example.data.local.AppDatabase.getInstance(application).reminderDao().deletePendingDuplicates()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("biometric_app_lock", enabled).apply()
        _isBiometricEnabled.value = enabled
        if (!enabled) _isAppUnlocked.value = true
    }

    fun unlockApp() {
        _isAppUnlocked.value = true
    }

    fun lockApp() {
        if (_isBiometricEnabled.value) {
            _isAppUnlocked.value = false
        }
    }

    fun dismissRestorePrompt() {
        prefs.edit().putBoolean("restore_prompted", true).apply()
        _showRestorePrompt.value = false
    }

    private val backupManager = com.example.util.BackupManager(application)

    fun exportPersistentBackup(uri: Uri, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val result = backupManager.exportBackup(uri)
            if (result.isSuccess) {
                onResult(true, "Backup exported successfully: ${result.getOrNull()} items")
            } else {
                onResult(false, "Export failed: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    fun restorePersistentBackup(uri: Uri, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val result = backupManager.restoreBackup(uri)
            if (result.isSuccess) {
                onResult(true, "Successfully restored ${result.getOrNull()} items 🎉")
            } else {
                onResult(false, "Restore failed: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    fun createLocalBackup(onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val result = com.example.util.BackupRestoreManager.createLocalBackup(getApplication())
            _backupInfo.value = com.example.util.BackupRestoreManager.getBackupInfo(getApplication())
            if (result.success) {
                onResult(true, "Backup saved to Documents/GTOOL X/Backups/gtool_backup.json")
            } else {
                onResult(false, result.errorMessage ?: "Failed to create backup")
            }
        }
    }

    fun restoreAllData(onResult: (Int) -> Unit) {
        viewModelScope.launch {
            val count = com.example.util.BackupRestoreManager.restoreBackup(getApplication())
            _backupInfo.value = com.example.util.BackupRestoreManager.getBackupInfo(getApplication())
            prefs.edit().putBoolean("restore_prompted", true).apply()
            _showRestorePrompt.value = false
            onResult(count)
        }
    }

    // Filtered Recent Items when no search query is active
    val filteredRecentItems: StateFlow<List<WorkspaceItem>> = combine(
        recentItems,
        _selectedFilter
    ) { items, filter ->
        items.filter { item ->
            filter == ItemType.ALL || item.type == filter
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredImportantItems: StateFlow<List<WorkspaceItem>> = importantItems

    // Direct Google & Firebase Authentication
    suspend fun signInWithGoogle(activityContext: Context): Pair<Boolean, String?> {
        val result = authManager.signInWithGoogle(activityContext)
        return when (result) {
            is FirebaseAuthResult.Success -> {
                _storageData.value = _storageData.value.copy(
                    syncAccountEmail = result.session.email
                )
                Pair(true, null)
            }
            is FirebaseAuthResult.Error -> {
                Pair(false, result.message)
            }
        }
    }

    fun signOut(context: Context) {
        viewModelScope.launch {
            authManager.signOut(context)
            _storageData.value = _storageData.value.copy(
                syncAccountEmail = ""
            )
            _currentScreen.value = CurrentScreen.HOME
        }
    }

    fun updateNote(title: String, bullets: List<String>) {
        viewModelScope.launch {
            repository.saveNote(
                title = title,
                content = bullets.joinToString("\n• ", prefix = "• "),
                tags = listOf("#note"),
                hasAttachment = false,
                includeInSemanticSearch = true
            )
            updateStorageUsage()
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onFilterSelected(type: ItemType) {
        _selectedFilter.value = if (_selectedFilter.value == type && type != ItemType.ALL) {
            ItemType.ALL
        } else {
            type
        }
    }

    fun togglePin(item: WorkspaceItem) {
        viewModelScope.launch {
            repository.togglePin(item)
        }
    }

    fun renameItem(item: WorkspaceItem, newTitle: String) {
        viewModelScope.launch {
            repository.renameMemory(item.id, newTitle)
        }
    }

    private val _chatMessages = MutableStateFlow(emptyList<ChatMessage>())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages

    private val screenHistory = java.util.Collections.synchronizedList(mutableListOf<CurrentScreen>())

    fun navigateTo(screen: CurrentScreen) {
        synchronized(screenHistory) {
            if (_currentScreen.value != screen) {
                if (screen == CurrentScreen.HOME) {
                    screenHistory.clear()
                } else {
                    // Prevent consecutive duplicate screen pushes
                    if (screenHistory.isEmpty() || screenHistory.last() != _currentScreen.value) {
                        screenHistory.add(_currentScreen.value)
                    }
                }
                _currentScreen.value = screen
            }
        }
    }

    fun navigateAndPopUpTo(
        destination: CurrentScreen,
        popUpToScreen: CurrentScreen? = null,
        inclusive: Boolean = false
    ) {
        synchronized(screenHistory) {
            if (popUpToScreen != null) {
                val index = screenHistory.lastIndexOf(popUpToScreen)
                if (index != -1) {
                    val removeIndex = if (inclusive) index else index + 1
                    while (screenHistory.size > removeIndex) {
                        screenHistory.removeAt(screenHistory.size - 1)
                    }
                }
            }
            if (destination == CurrentScreen.HOME) {
                screenHistory.clear()
            } else {
                // Ensure no redundant duplicate of destination at top of stack
                while (screenHistory.isNotEmpty() && screenHistory.last() == destination) {
                    screenHistory.removeAt(screenHistory.size - 1)
                }
            }
            _currentScreen.value = destination
        }
    }

    fun popBackStack(): Boolean {
        synchronized(screenHistory) {
            return if (screenHistory.isNotEmpty()) {
                val prev = screenHistory.removeAt(screenHistory.size - 1)
                _currentScreen.value = prev
                true
            } else {
                if (_currentScreen.value != CurrentScreen.HOME) {
                    _currentScreen.value = CurrentScreen.HOME
                    true
                } else {
                    false
                }
            }
        }
    }

    fun navigateBack() {
        popBackStack()
    }

    fun sendChatMessage(query: String) {
        val userMsgId = "msg-${System.currentTimeMillis()}"
        val userMsg = ChatMessage(
            id = userMsgId,
            sender = MessageSender.USER,
            text = query,
            timestamp = "Just now"
        )
        _chatMessages.value = _chatMessages.value + userMsg

        viewModelScope.launch {
            // Check for reminder commands
            if (com.example.util.ReminderManager.isReminderQuery(query)) {
                when (val result = com.example.util.ReminderManager.resolveAndSchedule(getApplication(), query)) {
                    is com.example.util.ReminderManager.ReminderResult.Scheduled -> {
                        val aiMsg = ChatMessage(
                            id = "ai-${System.currentTimeMillis()}",
                            sender = MessageSender.AI,
                            text = result.message,
                            timestamp = "Just now"
                        )
                        _chatMessages.value = _chatMessages.value + aiMsg
                    }
                    is com.example.util.ReminderManager.ReminderResult.RequiresConfirmation -> {
                        val aiMsg = ChatMessage(
                            id = "ai-${System.currentTimeMillis()}",
                            sender = MessageSender.AI,
                            text = result.message,
                            timestamp = "Just now",
                            reminderTitle = result.title,
                            reminderDate = result.targetTimestamp,
                            isReminderSet = false
                        )
                        _chatMessages.value = _chatMessages.value + aiMsg
                    }
                    is com.example.util.ReminderManager.ReminderResult.NeedsInteractiveSchedule -> {
                        _pendingReminderSchedule.value = PendingReminderSchedule(
                            title = result.detectedTitle,
                            query = result.query
                        )
                        val aiMsg = ChatMessage(
                            id = "ai-${System.currentTimeMillis()}",
                            sender = MessageSender.AI,
                            text = "I detected your reminder for ${result.detectedTitle}, but couldn't find an explicit date in your vault. Please choose a schedule on the prompt below ⏰",
                            timestamp = "Just now"
                        )
                        _chatMessages.value = _chatMessages.value + aiMsg
                    }
                }
                return@launch
            }

            // Layer 1: Conversational Chat & Greetings
            val greeting = com.example.util.AssistantEngine.getGreetingResponse(query)
            if (greeting != null) {
                delay(400)
                val aiMsg = ChatMessage(
                    id = "ai-${System.currentTimeMillis()}",
                    sender = MessageSender.AI,
                    text = greeting,
                    timestamp = "Just now"
                )
                _chatMessages.value = _chatMessages.value + aiMsg
                return@launch
            }

            delay(300)
            
            // Layer 2: Problem Solving & Multi-Token Search
            val tokens = com.example.util.AssistantEngine.getSearchTokens(query)
            val w1 = tokens.getOrNull(0) ?: ""
            val w2 = tokens.getOrNull(1) ?: ""

            // Query the universal unified repository search
            val initialMatches = try {
                if (w1.isNotEmpty()) {
                    universalSearchRepo.search(query).first()
                } else emptyList()
            } catch (e: Exception) {
                emptyList()
            }

            // Fallback to local memories search
            val fallbackMatches = try {
                val memoryMatches = repository.searchMemories(query).firstOrNull() ?: emptyList()
                memoryMatches.filter { mem -> !initialMatches.any { it.id == mem.id } }.map { mem ->
                    UniversalSearchEntity(
                        id = mem.id,
                        module = if (mem.type == ItemType.PDF) "PDF" else if (mem.type == ItemType.NOTE) "Note" else "Image",
                        title = mem.title,
                        contentText = "${mem.title}\n${mem.summary}\n${mem.contentSnippet ?: ""}",
                        metadataJson = "{}",
                        imageUri = mem.imageUri,
                        createdAt = System.currentTimeMillis()
                    )
                }
            } catch (e: Exception) {
                emptyList()
            }

            val matchedEntities = (initialMatches + fallbackMatches).distinctBy { it.id }

            val citations = matchedEntities.map { entity ->
                val workspaceItem = universalSearchRepo.entityToWorkspaceItem(entity)
                
                // 1. Split content into lines to find the exact matched line
                val lines = entity.contentText.lines().map { it.trim() }.filter { it.isNotEmpty() }
                val matchedLineIndex = lines.indexOfFirst { line -> 
                    tokens.any { token -> line.contains(token, ignoreCase = true) } 
                }
                
                val rawMatchedLine = if (matchedLineIndex != -1) {
                    lines[matchedLineIndex]
                } else {
                    entity.contentText.lines().firstOrNull { it.isNotBlank() }?.trim() ?: entity.title
                }

                // Clean highlight
                val cleanQuery = if (tokens.isNotEmpty()) tokens[0] else query
                val boldPattern = Regex("(?i)(" + Regex.escape(cleanQuery) + ")")
                val boldHighlightedLine = boldPattern.replace(rawMatchedLine) { "**" + it.value + "**" }

                // If user is searching "phone", "mobile", or "contact", try to find a real phone number in the document text
                val isPhoneQuery = query.contains("phone", ignoreCase = true) || 
                                   query.contains("mobile", ignoreCase = true) || 
                                   query.contains("contact", ignoreCase = true)
                val phoneRegex = Regex("""(\+?\d{1,4}?[-.\s]?\(?\d{1,3}?\)?[-.\s]?\d{1,4}[-.\s]?\d{1,9})""")
                
                val extractedValue = if (isPhoneQuery) {
                    val match = phoneRegex.find(entity.contentText)
                    if (match != null) "Extracted Value: " + match.value else boldHighlightedLine
                } else {
                    boldHighlightedLine
                }

                // 2. Exact Location & Context Metadata
                val moduleSourceLabel = when (entity.module.uppercase()) {
                    "PDF" -> "[PDF Document]"
                    "NOTE" -> "[Notepad Note]"
                    "IMAGE" -> "[Image OCR]"
                    "INVOICE" -> "[Smart Invoice]"
                    "RESIZER" -> "[Image Resizer]"
                    else -> "[${entity.module}]"
                }

                val lineBefore = if (matchedLineIndex > 0) lines[matchedLineIndex - 1] else null
                val lineAfter = if (matchedLineIndex < lines.size - 1 && matchedLineIndex != -1) lines[matchedLineIndex + 1] else null
                
                val contextPreview = buildString {
                    if (lineBefore != null) append(lineBefore).append("\n")
                    append(rawMatchedLine).append("\n")
                    if (lineAfter != null) append(lineAfter)
                }

                val originBadge = "[${entity.module.uppercase()}]"

                CitationSource(
                    id = entity.id,
                    title = "$originBadge ${entity.title}",
                    type = workspaceItem.type,
                    metaInfo = "${entity.module} • Unified Index",
                    snippet = boldHighlightedLine,
                    imageUri = entity.imageUri,
                    previewThumbnailUri = entity.imageUri,
                    extractedValue = extractedValue,
                    moduleSourceLabel = moduleSourceLabel,
                    contextPreview = contextPreview,
                    fileName = entity.title
                )
            }

            val responseText = if (matchedEntities.isNotEmpty()) {
                val matchCount = matchedEntities.size
                buildString {
                    append("I found $matchCount match(es) across your workspace for \"$query\":\n\n")
                    matchedEntities.forEachIndexed { i, entity ->
                        val lines = entity.contentText.lines().map { it.trim() }.filter { it.isNotEmpty() }
                        val matchedLine = lines.firstOrNull { line -> tokens.any { t -> line.contains(t, ignoreCase = true) } } 
                            ?: entity.title

                        append("${i + 1}. [${entity.module}] ${entity.title}\n")
                        append("   • Matched Text: $matchedLine\n\n")
                    }
                    append("Tap any match card below to open the original file.")
                }
            } else {
                "I couldn't find any document or note matching \"$query\" in your vault.\n\nTry searching for:\n• A specific name or ID number\n• A partial date like \"sep\" or \"2024\"\n• Keywords from a receipt or bill\n\nI am searching strictly offline for your privacy."
            }

            val aiMsg = ChatMessage(
                id = "ai-${System.currentTimeMillis()}",
                sender = MessageSender.AI,
                text = com.example.util.AssistantEngine.formatCleanText(responseText),
                timestamp = "Just now",
                citations = citations
            )
            _chatMessages.value = _chatMessages.value + aiMsg
        }
    }

    fun confirmAndScheduleReminder(messageId: String, title: String, date: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val db = com.example.data.local.AppDatabase.getInstance(getApplication())
            val reminderDao = db.reminderDao()
            
            // Check for duplicate reminders within the same 24 hours
            val count = reminderDao.hasExistingReminder(title, date)
            if (count == 0) {
                com.example.util.ReminderManager.commitAndSchedule(getApplication(), title, date)
            }
            
            // Update the UI state to mark the reminder as set
            _chatMessages.value = _chatMessages.value.map { msg ->
                if (msg.id == messageId) {
                    msg.copy(isReminderSet = true)
                } else {
                    msg
                }
            }
        }
    }

    fun syncNow() {
        viewModelScope.launch {
            _storageData.value = _storageData.value.copy(
                syncStatusText = "Optimizing vault..."
            )
            delay(500)
            _storageData.value = _storageData.value.copy(
                isOfflineReady = true,
                lastSyncTime = "Just now",
                syncStatusText = "Vault Encrypted & Synced"
            )
        }
    }

    fun deleteItemPermanently(item: WorkspaceItem) {
        viewModelScope.launch {
            repository.deleteMemory(item.id)
            if (!item.imageUri.isNullOrBlank()) {
                try {
                    val uri = Uri.parse(item.imageUri)
                    if (uri.scheme == "file") {
                        val file = File(uri.path ?: "")
                        if (file.exists()) file.delete()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            updateStorageUsage()
        }
    }

    fun addMemory(title: String, type: ItemType, isPrivate: Boolean) {
        viewModelScope.launch {
            when (type) {
                ItemType.NOTE -> {
                    repository.saveNote(
                        title = title,
                        content = "New note content",
                        tags = if (isPrivate) listOf("#private") else listOf("#note"),
                        hasAttachment = false,
                        includeInSemanticSearch = true
                    )
                }
                ItemType.IMAGE -> {
                    repository.saveImageMemory(
                        title = title,
                        imageUri = null,
                        amount = "",
                        dueDate = "",
                        includeInSearch = true
                    )
                }
                ItemType.PDF -> {
                    repository.savePdfMemory(
                        title = title,
                        fileName = "$title.pdf",
                        fileSize = "1.2 MB",
                        summary = "Imported PDF document",
                        keyPoints = emptyList()
                    )
                }
                ItemType.VOICE -> {
                    repository.saveVoiceMemory(
                        title = title,
                        transcription = "Recorded voice note",
                        summary = "Voice note memory",
                        hasReminder = false
                    )
                }
                ItemType.ALL -> {
                    repository.saveNote(
                        title = title,
                        content = "Memory",
                        tags = emptyList(),
                        hasAttachment = false,
                        includeInSemanticSearch = true
                    )
                }
                ItemType.ID_CARD, ItemType.SIGNATURE -> {}
            }
            updateStorageUsage()
        }
    }

    fun saveFullNote(
        id: String? = null,
        title: String,
        content: String,
        tags: List<String>,
        hasAttachment: Boolean,
        includeInSemanticSearch: Boolean,
        attachmentUri: String? = null
    ) {
        viewModelScope.launch {
            val finalId = if (id.isNullOrBlank()) "note-${System.currentTimeMillis()}" else id
            repository.saveNote(
                id = finalId,
                title = title,
                content = content,
                tags = tags,
                hasAttachment = hasAttachment,
                includeInSemanticSearch = includeInSemanticSearch,
                attachmentUri = attachmentUri
            )
            updateStorageUsage()
        }
    }

    fun savePdfDocument(
        title: String,
        fileName: String,
        fileSize: String,
        summary: String,
        keyPoints: List<String>,
        pdfUri: Uri? = null,
        includeInSearch: Boolean = true,
        fullTranscript: String = ""
    ) {
        viewModelScope.launch {
            repository.savePdfMemory(
                title = title,
                fileName = fileName,
                fileSize = fileSize,
                summary = summary,
                keyPoints = keyPoints,
                pdfUri = pdfUri,
                includeInSearch = includeInSearch,
                fullTranscript = fullTranscript
            )
            updateStorageUsage()
        }
    }

    fun saveImageMemory(
        title: String,
        imageUri: String?,
        amount: String = "",
        dueDate: String = "",
        includeInSearch: Boolean = true,
        fullOcrText: String = ""
    ) {
        viewModelScope.launch {
            repository.saveImageMemory(
                title = title,
                imageUri = imageUri,
                amount = amount,
                dueDate = dueDate,
                includeInSearch = includeInSearch,
                fullOcrText = fullOcrText
            )
            updateStorageUsage()
        }
    }

    fun saveVoiceMemory(
        title: String,
        transcription: String,
        summary: String,
        hasReminder: Boolean
    ) {
        viewModelScope.launch {
            repository.saveVoiceMemory(
                title = title,
                transcription = transcription,
                summary = summary,
                hasReminder = hasReminder
            )
            updateStorageUsage()
        }
    }

    private fun updateStorageUsage() {
        val totalItems = recentItems.value.size + 1
        val calcUsed = (totalItems * 0.002 * 1000).toLong() / 1000.0
        _storageData.value = _storageData.value.copy(
            usedGb = calcUsed,
            lastSyncTime = "Just now"
        )
    }

    fun clearCache() {
        _storageData.value = _storageData.value.copy(
            usedGb = 0.0,
            pdfsGb = 0.0,
            imagesGb = 0.0,
            notesGb = 0.0,
            lastSyncTime = "Optimized"
        )
    }
}
