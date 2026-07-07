import re
with open('app/src/main/java/com/example/ui/screen/SettingsScreen.kt', 'r') as f:
    content = f.read()

# Remove the call to AboutSection in the LazyColumn
content = re.sub(r'item\s*\{\s*AboutSection\(isArabic\)\s*\}', '', content)

# Also remove the actual function
content = re.sub(r'@Composable\s*fun AboutSection\(.*?\}\s*\}\s*\}', '', content, flags=re.DOTALL)

with open('app/src/main/java/com/example/ui/screen/SettingsScreen.kt', 'w') as f:
    f.write(content)
