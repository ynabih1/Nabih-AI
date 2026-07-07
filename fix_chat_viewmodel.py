import re

with open('app/src/main/java/com/example/ui/viewmodel/ChatViewModel.kt', 'r') as f:
    content = f.read()

# Replace AiModel.GEMINI_FLASH with settingsRepository.settings.value.defaultModel
content = content.replace(
    'private val _selectedModel = MutableStateFlow(AiModel.GEMINI_FLASH)',
    'private val _selectedModel = MutableStateFlow(settingsRepository.settings.value.defaultModel)'
)

with open('app/src/main/java/com/example/ui/viewmodel/ChatViewModel.kt', 'w') as f:
    f.write(content)
