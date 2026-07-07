import re

with open('app/src/main/java/com/example/ui/screen/SettingsScreen.kt', 'r') as f:
    content = f.read()

content = content.replace('    onDeleteAccount: () -> Unit = {},\n', '')

with open('app/src/main/java/com/example/ui/screen/SettingsScreen.kt', 'w') as f:
    f.write(content)
