import sys

filepath = "app/src/main/java/com/example/feature/chat/MainScreen.kt"
content = open(filepath).read()

target = """            },
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets.safeDrawing
        ) { paddingValues ->"""

replacement = """            },
            containerColor = MaterialTheme.colorScheme.background
        ) { paddingValues ->"""

if target in content:
    content = content.replace(target, replacement)
    print("Patched successfully")
else:
    print("Target not found")

with open(filepath, "w") as f:
    f.write(content)

