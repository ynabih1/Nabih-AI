import sys

filepath = "app/src/main/java/com/example/feature/chat/MainScreen.kt"
content = open(filepath).read()

target = """                            val isImeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
                            LaunchedEffect(filteredMessages.size, streamResponse, isImeVisible) {"""

replacement = """                            val isImeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
                            LaunchedEffect(filteredMessages.size, streamResponse, isImeVisible, inputText) {"""

if target in content:
    content = content.replace(target, replacement)
    print("Patched successfully")
else:
    print("Target not found")

with open(filepath, "w") as f:
    f.write(content)

