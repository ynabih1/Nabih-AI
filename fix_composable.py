import re

with open('app/src/main/java/com/example/ui/screen/SettingsScreen.kt', 'r') as f:
    content = f.read()

content = content.replace('@Composable\n@Composable', '@Composable')

with open('app/src/main/java/com/example/ui/screen/SettingsScreen.kt', 'w') as f:
    f.write(content)
