import re

with open('app/src/main/java/com/example/ui/screen/SettingsScreen.kt', 'r') as f:
    content = f.read()

# Replace double Composable
content = content.replace('@Composable\n@Composable', '@Composable')

# Remove trailing @Composable at the end of the file
content = re.sub(r'@Composable\s*$', '', content)

with open('app/src/main/java/com/example/ui/screen/SettingsScreen.kt', 'w') as f:
    f.write(content)
