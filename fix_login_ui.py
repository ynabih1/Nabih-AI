with open('app/src/main/java/com/example/auth/LoginScreen.kt', 'r') as f:
    text = f.read()

text = text.replace('MaterialTheme.colorScheme.surfaceContainer', 'MaterialTheme.colorScheme.surface')

with open('app/src/main/java/com/example/auth/LoginScreen.kt', 'w') as f:
    f.write(text)
