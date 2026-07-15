import sys

filepath = "app/src/main/java/com/example/feature/chat/MainScreen.kt"
content = open(filepath).read()

target = """                            val isImeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
                            LaunchedEffect(filteredMessages.size, streamResponse, isImeVisible, inputText) {
                                if (filteredMessages.isNotEmpty()) {
                                    listState.animateScrollToItem(filteredMessages.lastIndex + (if (isGenerating) 1 else 0))
                                }
                            }"""

replacement = """                            val imeVisible = WindowInsets.isImeVisible
                            LaunchedEffect(filteredMessages.size, streamResponse, imeVisible, inputText) {
                                if (filteredMessages.isNotEmpty()) {
                                    listState.animateScrollToItem(filteredMessages.lastIndex + (if (isGenerating) 1 else 0))
                                }
                            }"""

if target in content:
    content = content.replace(target, replacement)
    print("Patched successfully")
else:
    print("Target not found")

with open(filepath, "w") as f:
    f.write(content)

