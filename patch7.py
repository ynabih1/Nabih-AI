with open("app/src/main/java/com/example/feature/auth/LoginScreen.kt", "r") as f:
    content = f.read()

content = content.replace("color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f)", "color = MaterialTheme.colorScheme.outlineVariant")
content = content.replace("color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),\n                                        modifier = Modifier.padding(horizontal = 16.dp)", "color = MaterialTheme.colorScheme.onSurfaceVariant,\n                                        modifier = Modifier.padding(horizontal = 16.dp)")

with open("app/src/main/java/com/example/feature/auth/LoginScreen.kt", "w") as f:
    f.write(content)
