import re

with open('app/src/main/java/com/example/ui/viewmodel/ChatViewModel.kt', 'r') as f:
    content = f.read()

old_send = """    fun sendMessage(text: String) {
        val convId = _activeConversationId.value ?: return
        if (text.isBlank() && _attachedImageUri.value == null && _attachedDocUri.value == null) return
        viewModelScope.launch {"""
        
new_send = """    fun sendMessage(text: String) {
        val convId = _activeConversationId.value ?: return
        if (text.isBlank() && _attachedImageUri.value == null && _attachedDocUri.value == null) return
        if (_isGenerating.value) return // Prevent duplicate requests
        viewModelScope.launch {"""
        
content = content.replace(old_send, new_send)

with open('app/src/main/java/com/example/ui/viewmodel/ChatViewModel.kt', 'w') as f:
    f.write(content)
