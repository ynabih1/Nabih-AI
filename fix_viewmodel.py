with open('app/src/main/java/com/example/ui/viewmodel/ChatViewModel.kt', 'r') as f:
    code = f.read()

old_code = """    val settings: StateFlow<AppSettings> = settingsRepository.settings
    
    private val _activeConversationId = MutableStateFlow<String?>(null)"""

new_code = """    private val _activeConversationId = MutableStateFlow<String?>(null)"""

code = code.replace(old_code, new_code)

with open('app/src/main/java/com/example/ui/viewmodel/ChatViewModel.kt', 'w') as f:
    f.write(code)

