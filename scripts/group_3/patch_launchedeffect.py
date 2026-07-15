import sys

filepath = "app/src/main/java/com/example/feature/chat/MainScreen.kt"
content = open(filepath).read()

target = """                            val listState = rememberLazyListState()
                            val filteredMessages = if (searchQuery.isBlank()) {
                                state.messages
                            } else {
                                state.messages.filter { it.content.contains(searchQuery, ignoreCase = true) }
                            }

                            LaunchedEffect(filteredMessages.size, streamResponse) {"""

replacement = """                            val listState = rememberLazyListState()
                            val filteredMessages = if (searchQuery.isBlank()) {
                                state.messages
                            } else {
                                state.messages.filter { it.content.contains(searchQuery, ignoreCase = true) }
                            }
                            
                            val isImeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0

                            LaunchedEffect(filteredMessages.size, streamResponse, isImeVisible) {"""

if target in content:
    content = content.replace(target, replacement)
    print("Patched successfully")
else:
    print("Target not found")

with open(filepath, "w") as f:
    f.write(content)

