import sys

filepath = "app/src/main/java/com/example/feature/chat/MainScreen.kt"
content = open(filepath).read()

target = """                            LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxSize(),"""

replacement = """                            LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxSize().imePadding(),"""

if target in content:
    content = content.replace(target, replacement)
    print("Patched successfully")
else:
    print("Target not found")

with open(filepath, "w") as f:
    f.write(content)

