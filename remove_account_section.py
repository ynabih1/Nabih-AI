import re

with open('app/src/main/java/com/example/ui/screen/SettingsScreen.kt', 'r') as f:
    content = f.read()

# Remove the call to AccountSection
content = re.sub(r'\s*item \{\s*AccountSection\(settings, settingsViewModel, isArabic, onDeleteAccount\)\s*\}', '', content)

# Remove the function AccountSection
content = re.sub(r'fun AccountSection\(.*?\)\s*\{.*?^}\n', '', content, flags=re.DOTALL | re.MULTILINE)

with open('app/src/main/java/com/example/ui/screen/SettingsScreen.kt', 'w') as f:
    f.write(content)
