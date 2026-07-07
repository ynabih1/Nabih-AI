import re

with open('app/src/main/java/com/example/ui/screen/SettingsScreen.kt', 'r') as f:
    content = f.read()

# Remove the call to DefaultModelSection
content = re.sub(r'\s*item \{\s*DefaultModelSection\(settings, settingsViewModel, isArabic\)\s*\}', '', content)

# Remove the function DefaultModelSection
content = re.sub(r'fun DefaultModelSection\(.*?\)\s*\{.*?^}\n', '', content, flags=re.DOTALL | re.MULTILINE)

with open('app/src/main/java/com/example/ui/screen/SettingsScreen.kt', 'w') as f:
    f.write(content)
