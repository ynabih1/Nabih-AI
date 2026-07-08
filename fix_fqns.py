import os
import re

dir_path = 'app/src/main/java/com/example'

# Map old FQNs directly to their new FQNs or just strip them
replacements = {
    r'com\.example\.ui\.viewmodel\.ChatViewModel': 'com.example.feature.chat.ChatViewModel',
    r'com\.example\.ui\.viewmodel\.HomeViewModel': 'com.example.feature.chat.HomeViewModel',
    r'com\.example\.ui\.viewmodel\.SettingsViewModel': 'com.example.feature.settings.SettingsViewModel',
    r'com\.example\.ui\.viewmodel\.ViewModelFactory': 'com.example.core.di.ViewModelFactory',
    r'com\.example\.data\.model\.AppTheme': 'com.example.core.model.AppTheme',
    r'com\.example\.data\.model\.AppLanguage': 'com.example.core.model.AppLanguage',
    r'com\.example\.data\.model\.AiModel': 'com.example.core.model.AiModel',
    r'com\.example\.data\.model\.ResponseStyle': 'com.example.core.model.ResponseStyle',
    r'com\.example\.data\.database\.Conversation': 'com.example.core.database.Conversation',
    r'com\.example\.data\.database\.Message': 'com.example.core.database.Message',
    r'com\.example\.data\.database\.Folder': 'com.example.core.database.Folder',
    r'com\.example\.data\.database\.MemoryItem': 'com.example.core.database.MemoryItem',
    # And catch-all for any remaining ones:
    r'com\.example\.data\.[a-z]+\.': '',
    r'com\.example\.ui\.[a-z]+\.': '',
}

for root, _, files in os.walk(dir_path):
    for file in files:
        if file.endswith('.kt'):
            path = os.path.join(root, file)
            with open(path, 'r') as f:
                content = f.read()
                
            new_content = content
            for old, new in replacements.items():
                new_content = re.sub(old, new, new_content)
                
            if new_content != content:
                with open(path, 'w') as f:
                    f.write(new_content)

print("FQNs fixed")
