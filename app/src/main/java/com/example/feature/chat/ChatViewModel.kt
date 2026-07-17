package com.example.feature.chat

import com.example.core.database.Message
import com.example.core.model.AiModel
import com.example.feature.settings.SettingsRepository
import androidx.lifecycle.SavedStateHandle

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class ReasoningMode(val displayName: String, val icon: String) {
    AUTO("Auto Strategy", "🪄"),
    FAST("Fast Direct", "⚡"),
    BALANCED("Balanced", "⚖️"),
    DEEP_THINKING("Deep Thinking", "🧠"),
    RESEARCH("Deep Research", "🔍"),
    CREATIVE("Creative Muse", "🎨"),
    CODING("Coding Expert", "💻"),
    TRANSLATION("Translator", "🌐")
}

sealed interface ChatUiState {
    object Idle : ChatUiState
    object Loading : ChatUiState
    data class Success(val messages: List<Message>) : ChatUiState
    data class Error(val message: String) : ChatUiState
}

class ChatViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val chatRepository: ChatRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

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
        if (text.isBlank() && _attachedImageUri.value == null && _attachedDocUri.value == null) return

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
                content = text,
                imageUri = imageUriStr,
                documentUri = docUriStr,
                documentName = docName
            )
            chatRepository.insertMessage(userMsg)

            // Hold current attachment details to send to repository
            val imgUri = _attachedImageUri.value
            val docUri = _attachedDocUri.value
            val search = _searchEnabled.value

            // Build active prompt with Reasoning mode instruction injected safely to the model
            var activePrompt = text
            val currentReasoning = _reasoningMode.value
            when (currentReasoning) {
                ReasoningMode.DEEP_THINKING -> {
                    activePrompt += "\n[REASONING MODE: DEEP_THINKING. You MUST start your response with a thorough step-by-step thinking process wrapped in a <thinking>...</thinking> tag block first, and then provide your final answer.]"
                }
                ReasoningMode.FAST -> {
                    activePrompt += "\n[REASONING MODE: FAST. Provide an extremely direct and rapid response, skipping unnecessary greetings.]"
                }
                ReasoningMode.RESEARCH -> {
                    activePrompt += "\n[REASONING MODE: RESEARCH. Adopt the persona of Research Nabih. Perform exhaustive academic analysis, structure your points logically, cite potential sources, and organize data in comprehensive comparative frameworks.]"
                }
                ReasoningMode.CREATIVE -> {
                    activePrompt += "\n[REASONING MODE: CREATIVE. Adopt the persona of Copy Nabih. Write with rich metaphors, engaging narrative hooks, professional formatting, and persuasive, beautifully stylistic prose.]"
                }
                ReasoningMode.CODING -> {
                    activePrompt += "\n[REASONING MODE: CODING. Adopt the persona of Code Nabih. Write precise, clean, highly optimized, and thoroughly commented code following elite architectural standards and bulletproof error handling.]"
                }
                ReasoningMode.TRANSLATION -> {
                    activePrompt += "\n[REASONING MODE: TRANSLATION. Adopt the persona of Translate Nabih. Provide perfect natural translation, explaining syntactic subtleties, idioms, and grammatical structures comprehensively.]"
                }
                ReasoningMode.AUTO -> {
                    activePrompt += "\n[REASONING MODE: AUTO. Analyze the user's request, select the optimal reasoning strategy (Fast, Deep Thinking, Creative, or Coding) under the hood, and tailor your formatting precisely to match.]"
                }
                else -> {}
            }

            // 2. Start Generating Streaming Response
            _isGenerating.value = true
            _currentStreamingResponse.value = ""

            streamingJob = viewModelScope.launch {
                chatRepository.streamChatResponse(
                    conversationId = convId,
                    prompt = activePrompt,
                    attachedImageUri = imgUri,
                    attachedDocUri = docUri,
                    searchEnabled = search
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
                    _isGenerating.value = false
                    _currentStreamingResponse.value = ""
                    saveMessage(convId, "model", "An error occurred: ${e.localizedMessage}. Please try again.")
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
            if (lastMsg != null && lastMsg.role == "model" && lastMsg.content.startsWith("An error occurred")) {
                // We should actually just delete the last error message from the DB using a new DAO method if we had one. But we don't. We'll just leave it or ignore it.
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
                    }
                    _currentStreamingResponse.value = ""
                }.catch { e ->
                    _isGenerating.value = false
                    _currentStreamingResponse.value = ""
                    saveMessage(convId, "model", "An error occurred: ${e.localizedMessage}. Please try again.")
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

            var activePrompt = newContent
            val currentReasoning = _reasoningMode.value
            when (currentReasoning) {
                ReasoningMode.DEEP_THINKING -> {
                    activePrompt += "\n[REASONING MODE: DEEP_THINKING. You MUST start your response with a thorough step-by-step thinking process wrapped in a <thinking>...</thinking> tag block first, and then provide your final answer.]"
                }
                ReasoningMode.FAST -> {
                    activePrompt += "\n[REASONING MODE: FAST. Provide an extremely direct and rapid response, skipping unnecessary greetings.]"
                }
                ReasoningMode.RESEARCH -> {
                    activePrompt += "\n[REASONING MODE: RESEARCH. Adopt the persona of Research Nabih. Perform exhaustive academic analysis, structure your points logically, cite potential sources, and organize data in comprehensive comparative frameworks.]"
                }
                ReasoningMode.CREATIVE -> {
                    activePrompt += "\n[REASONING MODE: CREATIVE. Adopt the persona of Copy Nabih. Write with rich metaphors, engaging narrative hooks, professional formatting, and persuasive, beautifully stylistic prose.]"
                }
                ReasoningMode.CODING -> {
                    activePrompt += "\n[REASONING MODE: CODING. Adopt the persona of Code Nabih. Write precise, clean, highly optimized, and thoroughly commented code following elite architectural standards and bulletproof error handling.]"
                }
                ReasoningMode.TRANSLATION -> {
                    activePrompt += "\n[REASONING MODE: TRANSLATION. Adopt the persona of Translate Nabih. Provide perfect natural translation, explaining syntactic subtleties, idioms, and grammatical structures comprehensively.]"
                }
                ReasoningMode.AUTO -> {
                    activePrompt += "\n[REASONING MODE: AUTO. Analyze the user's request, select the optimal reasoning strategy (Fast, Deep Thinking, Creative, or Coding) under the hood, and tailor your formatting precisely to match.]"
                }
                else -> {}
            }

            streamingJob = viewModelScope.launch {
                chatRepository.streamChatResponse(
                    conversationId = convId,
                    prompt = activePrompt,
                    attachedImageUri = updatedMsg.imageUri?.let { Uri.parse(it) },
                    attachedDocUri = updatedMsg.documentUri?.let { Uri.parse(it) },
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
                    }
                    _currentStreamingResponse.value = ""
                }.catch { e ->
                    _isGenerating.value = false
                    _currentStreamingResponse.value = ""
                    saveMessage(convId, "model", "An error occurred: ${e.localizedMessage}. Please try again.")
                }.collect { chunk ->
                    _currentStreamingResponse.value = chunk
                }
            }
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
}
