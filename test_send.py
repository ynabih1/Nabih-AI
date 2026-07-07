import re

with open('app/src/main/java/com/example/ui/viewmodel/ChatViewModel.kt', 'r') as f:
    content = f.read()

# Check if _isGenerating is checked in sendMessage
print("if (_isGenerating.value) return" in content)
