with open('app/src/main/java/com/example/ui/screen/MainScreen.kt', 'r') as f:
    content = f.read()

content = content.replace("Modifier.padding(horizontal = 24.dp, bottom = 8.dp)", "Modifier.padding(horizontal = 24.dp).padding(bottom = 8.dp)")

with open('app/src/main/java/com/example/ui/screen/MainScreen.kt', 'w') as f:
    f.write(content)
