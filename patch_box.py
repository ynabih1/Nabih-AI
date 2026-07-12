import sys

filepath = "app/src/main/java/com/example/feature/chat/MainScreen.kt"
content = open(filepath).read()

target = """                Box(modifier = Modifier.weight(1f)) {
                    when (val state = chatState) {"""

replacement = """                Box(modifier = Modifier.weight(1f, fill = true)) {
                    when (val state = chatState) {"""

if target in content:
    content = content.replace(target, replacement)
    print("Patched successfully")
else:
    print("Target not found")

with open(filepath, "w") as f:
    f.write(content)

