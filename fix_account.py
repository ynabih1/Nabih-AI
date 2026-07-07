import re

with open('app/src/main/java/com/example/ui/screen/SettingsScreen.kt', 'r') as f:
    content = f.read()

content = content.replace('settings.userName.ifEmpty { "User" }', 'settings.userName.ifEmpty { settings.userEmail.substringBefore("@") }')
content = content.replace('settings.userEmail.ifEmpty { "user@example.com" }', 'settings.userEmail')
content = content.replace('settings.userName.take(1).uppercase().ifEmpty { "U" }', 'if (settings.userName.isNotEmpty()) settings.userName.take(1).uppercase() else if (settings.userEmail.isNotEmpty()) settings.userEmail.take(1).uppercase() else ""')

with open('app/src/main/java/com/example/ui/screen/SettingsScreen.kt', 'w') as f:
    f.write(content)
