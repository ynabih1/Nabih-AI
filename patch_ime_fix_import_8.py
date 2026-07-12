import sys

filepath = "app/src/main/java/com/example/feature/chat/MainScreen.kt"
content = open(filepath).read()

target = """                            @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
                            val imeVisible = WindowInsets.isImeVisible"""

replacement = """                            val imeVisible = WindowInsets.isImeVisible"""

if target in content:
    content = content.replace(target, replacement)
    print("Patched successfully")
else:
    print("Target not found")

with open(filepath, "w") as f:
    f.write(content)

