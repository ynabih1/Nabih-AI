with open('app/src/main/java/com/example/ui/viewmodel/ChatViewModel.kt', 'r') as f:
    code = f.read()

old_code = """    private val _activeConversationId = MutableStateFlow<String?>(null)
    val activeConversationId: StateFlow<String?> = _activeConversationId.asStateFlow()"""

new_code = """    val settings: StateFlow<AppSettings> = settingsRepository.settings
    
    private val _activeConversationId = MutableStateFlow<String?>(null)
    val activeConversationId: StateFlow<String?> = _activeConversationId.asStateFlow()"""

code = code.replace(old_code, new_code)

with open('app/src/main/java/com/example/ui/viewmodel/ChatViewModel.kt', 'w') as f:
    f.write(code)

