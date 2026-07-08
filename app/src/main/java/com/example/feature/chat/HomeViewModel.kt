package com.example.feature.chat

import com.example.core.database.Conversation
import com.example.core.database.Folder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class HomeViewModel(
    private val chatRepository: ChatRepository
) : ViewModel() {

    val folders: StateFlow<List<Folder>> = chatRepository.folders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeConversations: StateFlow<List<Conversation>> = chatRepository.activeConversations
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val archivedConversations: StateFlow<List<Conversation>> = chatRepository.archivedConversations
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val filteredConversations: StateFlow<List<Conversation>> = combine(
        activeConversations,
        _searchQuery
    ) { conversations, query ->
        if (query.isBlank()) {
            conversations
        } else {
            conversations.filter { it.title.contains(query, ignoreCase = true) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun createFolder(name: String) {
        viewModelScope.launch {
            if (name.isNotBlank()) {
                chatRepository.createFolder(name)
            }
        }
    }

    fun deleteFolder(folder: Folder) {
        viewModelScope.launch {
            chatRepository.deleteFolder(folder)
        }
    }

    fun createConversation(title: String, modelId: String, folderId: String? = null, isTemporary: Boolean = false, onCreated: (String) -> Unit) {
        viewModelScope.launch {
            val id = chatRepository.createConversation(title, modelId, folderId, isTemporary)
            onCreated(id)
        }
    }

    fun renameConversation(id: String, newTitle: String) {
        viewModelScope.launch {
            chatRepository.renameConversation(id, newTitle)
        }
    }

    fun duplicateConversation(id: String, onDuplicated: (String) -> Unit) {
        viewModelScope.launch {
            val newId = chatRepository.duplicateConversation(id)
            if (newId.isNotEmpty()) {
                onDuplicated(newId)
            }
        }
    }

    fun togglePinConversation(conversation: Conversation) {
        viewModelScope.launch {
            chatRepository.updateConversation(conversation.copy(isPinned = !conversation.isPinned))
        }
    }

    fun toggleArchiveConversation(conversation: Conversation) {
        viewModelScope.launch {
            chatRepository.updateConversation(conversation.copy(isArchived = !conversation.isArchived))
        }
    }

    fun moveConversationToFolder(conversation: Conversation, folderId: String?) {
        viewModelScope.launch {
            chatRepository.updateConversation(conversation.copy(folderId = folderId))
        }
    }

    fun deleteConversation(id: String) {
        viewModelScope.launch {
            chatRepository.deleteConversation(id)
            chatRepository.deleteConversationMessages(id)
        }
    }
}
