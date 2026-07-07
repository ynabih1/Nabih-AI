import re

with open('app/src/main/java/com/example/ui/viewmodel/ChatViewModel.kt', 'r') as f:
    code = f.read()

new_func = """
    fun createNewChat(modelId: String) {
        _activeConversationId.value = null
        activeConversationMessagesFlow?.cancel()
        _uiState.value = ChatUiState.Idle
        _currentStreamingResponse.value = ""
        _attachedImageUri.value = null
        _attachedDocumentUri.value = null
        _selectedModel.value = AiModel.fromId(modelId)
    }

    fun selectConversation"""

code = code.replace("    fun selectConversation", new_func)

with open('app/src/main/java/com/example/ui/viewmodel/ChatViewModel.kt', 'w') as f:
    f.write(code)

