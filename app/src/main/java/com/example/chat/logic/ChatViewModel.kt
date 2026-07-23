package com.example.chat.logic

import com.example.data.repository.SettingsRepository
import com.example.data.repository.ChatRepository
import com.example.data.local.Message
import com.example.data.local.AttachmentItem
import com.example.models.AiModel
import com.example.models.ReasoningMode
import com.example.utils.NetworkMonitor
import androidx.lifecycle.SavedStateHandle

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed interface ChatUiState {
    object Idle : ChatUiState
    object Loading : ChatUiState
    data class Success(val messages: List<Message>) : ChatUiState
    data class Error(val message: String) : ChatUiState
}

class ChatViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val chatRepository: ChatRepository,
    private val settingsRepository: SettingsRepository,
    private val networkMonitor: NetworkMonitor,
    private val notificationHelper: com.example.utils.NotificationHelper
) : ViewModel() {

    data class FallbackDialogState(
        val show: Boolean = false,
        val failedModel: AiModel = AiModel.NABIH_ULTRA,
        val suggestedModel: AiModel = AiModel.NABIH_ULTRA,
        val conversationId: String = ""
    )

    private val _fallbackDialogState = MutableStateFlow(FallbackDialogState())
    val fallbackDialogState: StateFlow<FallbackDialogState> = _fallbackDialogState.asStateFlow()

    fun acceptFallback() {
        val state = _fallbackDialogState.value
        if (!state.show) return
        _fallbackDialogState.value = FallbackDialogState()
        
        viewModelScope.launch {
            val convId = state.conversationId
            val conv = chatRepository.getConversationById(convId)
            if (conv != null) {
                chatRepository.updateConversation(conv.copy(modelId = state.suggestedModel.id))
            }
            selectModel(state.suggestedModel)
            retryLastResponse()
        }
    }

    fun dismissFallback() {
        _fallbackDialogState.value = FallbackDialogState()
    }

    val isOnline: StateFlow<Boolean> = networkMonitor.isOnline
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = networkMonitor.isCurrentlyOnline()
        )

    private val _activeConversationId = MutableStateFlow<String?>(null)
    val activeConversationId: StateFlow<String?> = _activeConversationId.asStateFlow()

    private val _uiState = MutableStateFlow<ChatUiState>(ChatUiState.Idle)
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val _currentInputText = MutableStateFlow(savedStateHandle.get<String>("draft_text") ?: "")
    val currentInputText: StateFlow<String> = _currentInputText.asStateFlow()

    fun updateInputText(text: String) {
        _currentInputText.value = text
        savedStateHandle["draft_text"] = text
    }

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _currentStreamingResponse = MutableStateFlow("")
    val currentStreamingResponse: StateFlow<String> = _currentStreamingResponse.asStateFlow()

    // Advanced reasoning and suggestions state
    private val _reasoningMode = MutableStateFlow(ReasoningMode.AUTO)
    val reasoningMode: StateFlow<ReasoningMode> = _reasoningMode.asStateFlow()

    private val _followUpSuggestions = MutableStateFlow<List<String>>(emptyList())
    val followUpSuggestions: StateFlow<List<String>> = _followUpSuggestions.asStateFlow()

    // Selected Model state
    private val _selectedModel = MutableStateFlow(settingsRepository.settings.value.defaultModel)
    val selectedModel: StateFlow<AiModel> = _selectedModel.asStateFlow()

    // Search query inside conversation
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Pinned messages set
    private val _pinnedMessageIds = MutableStateFlow<Set<String>>(emptySet())
    val pinnedMessageIds: StateFlow<Set<String>> = _pinnedMessageIds.asStateFlow()

    // Attachments State
    private val _attachedImageUri = MutableStateFlow<Uri?>(null)
    val attachedImageUri: StateFlow<Uri?> = _attachedImageUri.asStateFlow()

    private val _attachedDocUri = MutableStateFlow<Uri?>(null)
    val attachedDocUri: StateFlow<Uri?> = _attachedDocUri.asStateFlow()

    private val _attachedDocName = MutableStateFlow<String?>(null)
    val attachedDocName: StateFlow<String?> = _attachedDocName.asStateFlow()

    private val _isAttaching = MutableStateFlow(false)
    val isAttaching: StateFlow<Boolean> = _isAttaching.asStateFlow()

    private val _attachProgress = MutableStateFlow(0f)
    val attachProgress: StateFlow<Float> = _attachProgress.asStateFlow()

    private val _attachError = MutableStateFlow<String?>(null)
    val attachError: StateFlow<String?> = _attachError.asStateFlow()

    // Internet Search Enable Flag
    private val _searchEnabled = MutableStateFlow(false)
    val searchEnabled: StateFlow<Boolean> = _searchEnabled.asStateFlow()

    val attachments: StateFlow<List<AttachmentItem>> = chatRepository.getAttachments()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private var activeConversationMessagesFlow: Job? = null
    private var streamingJob: Job? = null

    init {
        val lastConvId = settingsRepository.getLastActiveConversationId()
        if (lastConvId != null) {
            viewModelScope.launch {
                val conv = chatRepository.getConversationById(lastConvId)
                if (conv != null) {
                    selectConversation(lastConvId)
                }
            }
        }
    }


    fun createNewChat(modelId: String) {
        _activeConversationId.value = null
        settingsRepository.saveLastActiveConversationId(null)
        activeConversationMessagesFlow?.cancel()
        _uiState.value = ChatUiState.Idle
        _currentStreamingResponse.value = ""
        _attachedImageUri.value = null
        _attachedDocUri.value = null
        _attachedDocName.value = null
        _selectedModel.value = AiModel.fromId(modelId)
    }

    fun selectConversation(id: String) {
        _activeConversationId.value = id
        settingsRepository.saveLastActiveConversationId(id)
        activeConversationMessagesFlow?.cancel()
        _uiState.value = ChatUiState.Loading

        // Load conversation to read the correct active model ID
        viewModelScope.launch {
            val conv = chatRepository.getConversationById(id)
            if (conv != null) {
                _selectedModel.value = AiModel.fromId(conv.modelId)
            }
        }

        activeConversationMessagesFlow = viewModelScope.launch {
            chatRepository.getMessagesForConversation(id)
                .collect { messages ->
                    _uiState.value = ChatUiState.Success(messages)
                }
        }
    }

    fun selectModel(model: AiModel) {
        _selectedModel.value = model
        settingsRepository.updateDefaultModel(model)
        val convId = _activeConversationId.value ?: return
        viewModelScope.launch {
            val conv = chatRepository.getConversationById(convId)
            if (conv != null) {
                chatRepository.updateConversation(conv.copy(modelId = model.id))
            }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun togglePinMessage(messageId: String) {
        val current = _pinnedMessageIds.value
        _pinnedMessageIds.value = if (current.contains(messageId)) {
            current - messageId
        } else {
            current + messageId
        }
    }

    fun attachImage(uri: Uri?) {
        if (uri == null) {
            _attachedImageUri.value = null
            _attachError.value = null
            return
        }
        processAndAttachFile(uri, "image")
    }

    fun attachDocument(uri: Uri?) {
        if (uri == null) {
            _attachedDocUri.value = null
            _attachedDocName.value = null
            _attachError.value = null
            return
        }
        processAndAttachFile(uri, "document")
    }

    private fun processAndAttachFile(uri: Uri, type: String) {
        viewModelScope.launch {
            _isAttaching.value = true
            _attachProgress.value = 0f
            _attachError.value = null
            try {
                val cachedUri = chatRepository.copyUriToCache(uri) { progress ->
                    _attachProgress.value = progress
                }
                if (cachedUri != null) {
                    val name = chatRepository.getFileName(cachedUri) ?: "File"
                    if (type == "image") {
                        _attachedImageUri.value = cachedUri
                        _attachedDocUri.value = null
                        _attachedDocName.value = null
                    } else {
                        _attachedDocUri.value = cachedUri
                        _attachedDocName.value = name
                        _attachedImageUri.value = null
                    }
                } else {
                    _attachError.value = "Failed to copy file to local cache."
                }
            } catch (e: Exception) {
                _attachError.value = e.localizedMessage ?: "Unknown attachment error"
            } finally {
                _isAttaching.value = false
            }
        }
    }

    fun toggleSearch() {
        _searchEnabled.value = !_searchEnabled.value
    }

    fun getAlternativeModel(failedModel: AiModel = selectedModel.value): AiModel {
        val settings = settingsRepository.settings.value
        val candidates = listOf(AiModel.NABIH_ULTRA, AiModel.GEMINI, AiModel.CHATGPT, AiModel.CLAUDE)
        for (model in candidates) {
            if (model == failedModel) continue
            val isValid = when (model.provider) {
                com.example.models.ApiProvider.NABIH -> true
                com.example.models.ApiProvider.GOOGLE -> settings.googleApiKey.isNotBlank() || settings.nabihApiKey.isNotBlank() || com.example.BuildConfig.GEMINI_API_KEY.isNotBlank()
                com.example.models.ApiProvider.OPENAI -> settings.openaiApiKey.isNotBlank()
                com.example.models.ApiProvider.ANTHROPIC -> settings.anthropicApiKey.isNotBlank()
            }
            if (isValid) return model
        }
        return if (failedModel == AiModel.NABIH_ULTRA) AiModel.GEMINI else AiModel.NABIH_ULTRA
    }

    fun retryWithAlternativeModel(alternativeModel: AiModel) {
        selectModel(alternativeModel)
        retryLastResponse()
    }

    fun stopGeneration() {
        streamingJob?.cancel()
        _isGenerating.value = false
        // Save whatever we got so far as a message
        val streamText = _currentStreamingResponse.value
        val convId = _activeConversationId.value
        if (convId != null && streamText.isNotBlank()) {
            saveMessage(convId, "model", streamText)
        }
        _currentStreamingResponse.value = ""
    }

    fun setReasoningMode(mode: ReasoningMode) {
        _reasoningMode.value = mode
    }

    fun cleanupIfTemporary() {
        val convId = _activeConversationId.value ?: return
        if (convId.startsWith("temp_")) {
            viewModelScope.launch {
                chatRepository.deleteConversation(convId)
                chatRepository.deleteConversationMessages(convId)
            }
        }
    }

    fun sendMessage(text: String) {
        if (_isGenerating.value) return
        val convId = _activeConversationId.value ?: return
        val userText = text.trim()
        if (userText.isBlank() && _attachedImageUri.value == null && _attachedDocUri.value == null) return

        viewModelScope.launch {
            _followUpSuggestions.value = emptyList()

            // 1. Save User Message
            val imageUriStr = _attachedImageUri.value?.toString()
            val docUriStr = _attachedDocUri.value?.toString()
            val docName = _attachedDocName.value

            val userMsg = Message(
                id = java.util.UUID.randomUUID().toString(),
                conversationId = convId,
                role = "user",
                content = userText,
                imageUri = imageUriStr,
                documentUri = docUriStr,
                documentName = docName
            )
            chatRepository.insertMessage(userMsg)

            // Hold current attachment details to send to repository
            val imgUri = _attachedImageUri.value
            val docUri = _attachedDocUri.value
            val search = _searchEnabled.value

            // Pass pure user text as prompt and pass reasoningMode to chatRepository
            val activePrompt = userText
            val currentReasoning = _reasoningMode.value

            // 2. Start Generating Streaming Response
            _isGenerating.value = true
            _currentStreamingResponse.value = ""

            streamingJob = viewModelScope.launch {
                chatRepository.streamChatResponse(
                    conversationId = convId,
                    prompt = activePrompt,
                    attachedImageUri = imgUri,
                    attachedDocUri = docUri,
                    searchEnabled = search,
                    reasoningMode = currentReasoning
                ).onCompletion { err ->
                    _isGenerating.value = false
                    if (err == null) {
                        _currentInputText.value = ""
                        savedStateHandle["draft_text"] = null
                        _attachedImageUri.value = null
                        savedStateHandle["draft_img"] = null
                        _attachedDocUri.value = null
                        savedStateHandle["draft_doc"] = null
                        _attachedDocName.value = null
                        savedStateHandle["draft_doc_name"] = null
                    }
                    val responseText = _currentStreamingResponse.value
                    if (responseText.isNotEmpty()) {
                        saveMessage(convId, "model", responseText)
                        if (settingsRepository.settings.value.completionNotifications) {
                            val title = if (settingsRepository.settings.value.language == com.example.models.AppLanguage.ARABIC) "اكتمل الرد" else "Response Completed"
                            val msg = if (settingsRepository.settings.value.language == com.example.models.AppLanguage.ARABIC) "أنهى Nabih Ultra إجابته." else "Nabih Ultra has finished answering."
                            notificationHelper.showCompletionNotification(title, msg, convId)
                        }
                        
                        _followUpSuggestions.value = when {
                            text.lowercase().contains("code") || text.lowercase().contains("kotlin") || text.lowercase().contains("program") -> listOf(
                                "Can you optimize this code?",
                                "Add unit tests for this",
                                "Explain the architecture"
                            )
                            text.lowercase().contains("write") || text.lowercase().contains("draft") || text.lowercase().contains("explain") || text.lowercase().contains("summary") -> listOf(
                                "Make it more professional",
                                "Summarize into bullet points",
                                "Translate to Arabic"
                            )
                            else -> listOf(
                                "Give me a practical example",
                                "What are the pros and cons?",
                                "Tell me more about this"
                            )
                        }
                    }
                    _currentStreamingResponse.value = ""
                }.catch { e ->
                    handleError(e, convId)
                }.collect { chunk ->
                    _currentStreamingResponse.value = chunk
                }
            }
        }
    }

    fun retryLastResponse() {
        val convId = _activeConversationId.value ?: return
        viewModelScope.launch {
            val messages = chatRepository.getMessagesForConversation(convId).first()
            val lastUserMsg = messages.lastOrNull { it.role == "user" } ?: return@launch

            // Remove last model response if exists
            val lastMsg = messages.lastOrNull()
            if (lastMsg != null && lastMsg.role == "model" && (lastMsg.content.startsWith("An error occurred") || lastMsg.content.startsWith("API_ERROR:") || lastMsg.content.startsWith("حدث خطأ:") || lastMsg.content.startsWith("Error:") || lastMsg.content.startsWith("Sorry,") || lastMsg.content.startsWith("عذراً"))) {
                chatRepository.deleteMessageById(lastMsg.id)
            }
            
            // To properly retry, we will just start generating again with the existing context.
            // But we need to use the lastUserMsg's content and attachments.
            // Since we didn't clear the composer, they can just use the composer. 
            // But if they clicked retry, let's just trigger streaming again manually using the DB history.
            // Wait, if we call sendMessage again, it will duplicate the user message.
            // Better to just call chatRepository.streamChatResponse on the lastUserMsg!
            _isGenerating.value = true
            _currentStreamingResponse.value = ""

            streamingJob = viewModelScope.launch {
                chatRepository.streamChatResponse(
                    conversationId = convId,
                    prompt = lastUserMsg.content,
                    attachedImageUri = lastUserMsg.imageUri?.let { android.net.Uri.parse(it) },
                    attachedDocUri = lastUserMsg.documentUri?.let { android.net.Uri.parse(it) },
                    searchEnabled = _searchEnabled.value
                ).onCompletion { err ->
                    _isGenerating.value = false
                    if (err == null) {
                        _currentInputText.value = ""
                        savedStateHandle["draft_text"] = null
                        _attachedImageUri.value = null
                        savedStateHandle["draft_img"] = null
                        _attachedDocUri.value = null
                        savedStateHandle["draft_doc"] = null
                        _attachedDocName.value = null
                        savedStateHandle["draft_doc_name"] = null
                    }
                    val responseText = _currentStreamingResponse.value
                    if (responseText.isNotEmpty()) {
                        saveMessage(convId, "model", responseText)
                        if (settingsRepository.settings.value.completionNotifications) {
                            val title = if (settingsRepository.settings.value.language == com.example.models.AppLanguage.ARABIC) "اكتمل الرد" else "Response Completed"
                            val msg = if (settingsRepository.settings.value.language == com.example.models.AppLanguage.ARABIC) "أنهى Nabih Ultra إجابته." else "Nabih Ultra has finished answering."
                            notificationHelper.showCompletionNotification(title, msg, convId)
                        }
                    }
                    _currentStreamingResponse.value = ""
                }.catch { e ->
                    handleError(e, convId)
                }.collect { chunk ->
                    _currentStreamingResponse.value = chunk
                }
            }
        }
    }

    fun continueGeneration() {
        sendMessage("Continue the previous response from where you left off. Do not repeat the previous text, just continue continuously.")
    }

    fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            chatRepository.deleteMessageById(messageId)
        }
    }

    fun editUserMessageAndRegenerate(messageId: String, newContent: String) {
        val convId = _activeConversationId.value ?: return
        if (newContent.isBlank()) return
        viewModelScope.launch {
            stopGeneration()
            val messages = chatRepository.getMessagesForConversation(convId).first()
            val targetIndex = messages.indexOfFirst { it.id == messageId }
            if (targetIndex == -1) return@launch

            val targetMessage = messages[targetIndex]
            for (i in targetIndex + 1 until messages.size) {
                chatRepository.deleteMessageById(messages[i].id)
            }

            val updatedMsg = targetMessage.copy(content = newContent, timestamp = System.currentTimeMillis())
            chatRepository.insertMessage(updatedMsg)

            _isGenerating.value = true
            _currentStreamingResponse.value = ""

            val activePrompt = newContent
            val currentReasoning = _reasoningMode.value

            streamingJob = viewModelScope.launch {
                chatRepository.streamChatResponse(
                    conversationId = convId,
                    prompt = activePrompt,
                    attachedImageUri = updatedMsg.imageUri?.let { Uri.parse(it) },
                    attachedDocUri = updatedMsg.documentUri?.let { Uri.parse(it) },
                    searchEnabled = _searchEnabled.value,
                    reasoningMode = currentReasoning
                ).onCompletion { err ->
                    _isGenerating.value = false
                    if (err == null) {
                        _currentInputText.value = ""
                        savedStateHandle["draft_text"] = null
                        _attachedImageUri.value = null
                        savedStateHandle["draft_img"] = null
                        _attachedDocUri.value = null
                        savedStateHandle["draft_doc"] = null
                        _attachedDocName.value = null
                        savedStateHandle["draft_doc_name"] = null
                    }
                    val responseText = _currentStreamingResponse.value
                    if (responseText.isNotEmpty()) {
                        saveMessage(convId, "model", responseText)
                        if (settingsRepository.settings.value.completionNotifications) {
                            val title = if (settingsRepository.settings.value.language == com.example.models.AppLanguage.ARABIC) "اكتمل الرد" else "Response Completed"
                            val msg = if (settingsRepository.settings.value.language == com.example.models.AppLanguage.ARABIC) "أنهى Nabih Ultra إجابته." else "Nabih Ultra has finished answering."
                            notificationHelper.showCompletionNotification(title, msg, convId)
                        }
                    }
                    _currentStreamingResponse.value = ""
                }.catch { e ->
                    handleError(e, convId)
                }.collect { chunk ->
                    _currentStreamingResponse.value = chunk
                }
            }
        }
    }

    private fun handleError(e: Throwable, convId: String) {
        _isGenerating.value = false
        _currentStreamingResponse.value = ""
        val currentModelObj = _selectedModel.value
        
        // Log error locally in Room error_logs
        viewModelScope.launch {
            chatRepository.insertErrorLog(
                errorType = e.javaClass.simpleName ?: "Exception",
                provider = currentModelObj.provider.name
            )
        }

        // Translate/Map API Error code to user friendly message
        val isArabic = settingsRepository.settings.value.language == com.example.models.AppLanguage.ARABIC
        val userMsg = com.example.models.AiErrorTranslator.translate(
            throwable = e,
            provider = currentModelObj.displayName,
            isArabic = isArabic
        )

        // Save with "API_ERROR:" prefix so the UI knows to render it beautifully as an error bubble with retry action
        saveMessage(convId, "model", "API_ERROR: $userMsg")

        // Trigger fallback model proposal dialog if 429 or 500 error occurs
        val isServerOrRateLimit = if (e is retrofit2.HttpException) {
            e.code() == 429 || e.code() in 500..599
        } else {
            val msg = e.localizedMessage ?: e.message ?: ""
            msg.contains("429") || msg.contains("500") || msg.contains("502") || msg.contains("503") || msg.contains("504")
        }

        if (isServerOrRateLimit) {
            val suggestedModel = if (currentModelObj == AiModel.NABIH_ULTRA) AiModel.GEMINI else AiModel.NABIH_ULTRA
            _fallbackDialogState.value = FallbackDialogState(
                show = true,
                failedModel = currentModelObj,
                suggestedModel = suggestedModel,
                conversationId = convId
            )
        }
    }

    private fun saveMessage(conversationId: String, role: String, content: String) {
        viewModelScope.launch {
            val msg = Message(
                id = java.util.UUID.randomUUID().toString(),
                conversationId = conversationId,
                role = role,
                content = content
            )
            chatRepository.insertMessage(msg)
        }
    }

    fun getFileSizeString(uriString: String): String {
        return try {
            chatRepository.getFileSizeString(android.net.Uri.parse(uriString))
        } catch (e: Exception) {
            ""
        }
    }
}
