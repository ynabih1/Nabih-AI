package com.example.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.Message
import com.example.data.model.AiModel
import com.example.data.repository.ChatRepository
import com.example.data.repository.SettingsRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class ReasoningMode(val displayName: String, val icon: String) {
    AUTO("Auto", "🪄"),
    FAST("Fast", "⚡"),
    BALANCED("Balanced", "⚖️"),
    DEEP_THINKING("Deep Thinking", "🧠")
}

sealed interface ChatUiState {
    object Idle : ChatUiState
    object Loading : ChatUiState
    data class Success(val messages: List<Message>) : ChatUiState
    data class Error(val message: String) : ChatUiState
}

class ChatViewModel(
    private val chatRepository: ChatRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _activeConversationId = MutableStateFlow<String?>(null)
    val activeConversationId: StateFlow<String?> = _activeConversationId.asStateFlow()

    private val _uiState = MutableStateFlow<ChatUiState>(ChatUiState.Idle)
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

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

    // Internet Search Enable Flag
    private val _searchEnabled = MutableStateFlow(false)
    val searchEnabled: StateFlow<Boolean> = _searchEnabled.asStateFlow()

    private var activeConversationMessagesFlow: Job? = null
    private var streamingJob: Job? = null


    fun createNewChat(modelId: String) {
        _activeConversationId.value = null
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
        _attachedImageUri.value = uri
    }

    fun attachDocument(uri: Uri?) {
        _attachedDocUri.value = uri
        if (uri != null) {
            viewModelScope.launch {
                val (name, _) = chatRepository.parseAttachedDocument(uri)
                _attachedDocName.value = name
            }
        } else {
            _attachedDocName.value = null
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

            // Clear active inputs
            _attachedImageUri.value = null
            _attachedDocUri.value = null
        _attachedDocName.value = null
            _attachedDocName.value = null

            // Build active prompt with Reasoning mode instruction injected safely to the model
            var activePrompt = text
            val currentReasoning = _reasoningMode.value
            if (currentReasoning == ReasoningMode.DEEP_THINKING) {
                activePrompt += "\n[REASONING MODE: DEEP_THINKING. You MUST start your response with a thorough step-by-step thinking process wrapped in a <thinking>...</thinking> tag block first, and then provide your final answer.]"
            } else if (currentReasoning == ReasoningMode.FAST) {
                activePrompt += "\n[REASONING MODE: FAST. Provide an extremely direct and rapid response, skipping unnecessary greetings.]"
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
                ).onCompletion {
                    _isGenerating.value = false
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
            if (lastMsg != null && lastMsg.role == "model") {
                // Delete message flow does not have dedicated DAO yet so we'll just ignore and append,
                // or we can overwrite. Let's send the user prompt again!
            }

            sendMessage(lastUserMsg.content)
        }
    }

    fun continueGeneration() {
        sendMessage("Continue the previous response from where you left off. Do not repeat the previous text, just continue continuously.")
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
