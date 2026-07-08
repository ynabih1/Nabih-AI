import os
import shutil
import re

dir_path = 'app/src/main/java/com/example'
file_moves = {
    'app/src/main/java/com/example/ui/viewmodel/ViewModelFactory.kt': ('com.example.core.di', 'app/src/main/java/com/example/core/di/ViewModelFactory.kt'),
    'app/src/main/java/com/example/data/di/AppContainer.kt': ('com.example.core.di', 'app/src/main/java/com/example/core/di/AppContainer.kt'),

    'app/src/main/java/com/example/data/database/AppDatabase.kt': ('com.example.core.database', 'app/src/main/java/com/example/core/database/AppDatabase.kt'),
    'app/src/main/java/com/example/data/database/Daos.kt': ('com.example.core.database', 'app/src/main/java/com/example/core/database/Daos.kt'),
    'app/src/main/java/com/example/data/database/Entities.kt': ('com.example.core.database', 'app/src/main/java/com/example/core/database/Entities.kt'),

    'app/src/main/java/com/example/data/network/ApiServices.kt': ('com.example.core.network', 'app/src/main/java/com/example/core/network/ApiServices.kt'),

    'app/src/main/java/com/example/data/model/Models.kt': ('com.example.core.model', 'app/src/main/java/com/example/core/model/Models.kt'),

    'app/src/main/java/com/example/ui/theme/Theme.kt': ('com.example.core.theme', 'app/src/main/java/com/example/core/theme/Theme.kt'),
    'app/src/main/java/com/example/ui/theme/Color.kt': ('com.example.core.theme', 'app/src/main/java/com/example/core/theme/Color.kt'),
    'app/src/main/java/com/example/ui/theme/Type.kt': ('com.example.core.theme', 'app/src/main/java/com/example/core/theme/Type.kt'),

    'app/src/main/java/com/example/ui/components/TypingAnimation.kt': ('com.example.core.ui', 'app/src/main/java/com/example/core/ui/TypingAnimation.kt'),
    'app/src/main/java/com/example/ui/components/MarkdownRenderer.kt': ('com.example.core.ui', 'app/src/main/java/com/example/core/ui/MarkdownRenderer.kt'),

    'app/src/main/java/com/example/ui/auth/AuthManager.kt': ('com.example.feature.auth', 'app/src/main/java/com/example/feature/auth/AuthManager.kt'),
    'app/src/main/java/com/example/ui/screen/LoginScreen.kt': ('com.example.feature.auth', 'app/src/main/java/com/example/feature/auth/LoginScreen.kt'),
    'app/src/main/java/com/example/ui/screen/AccountScreen.kt': ('com.example.feature.auth', 'app/src/main/java/com/example/feature/auth/AccountScreen.kt'),

    'app/src/main/java/com/example/ui/screen/MainScreen.kt': ('com.example.feature.chat', 'app/src/main/java/com/example/feature/chat/MainScreen.kt'),
    'app/src/main/java/com/example/ui/screen/VoiceScreen.kt': ('com.example.feature.chat', 'app/src/main/java/com/example/feature/chat/VoiceScreen.kt'),
    'app/src/main/java/com/example/ui/viewmodel/ChatViewModel.kt': ('com.example.feature.chat', 'app/src/main/java/com/example/feature/chat/ChatViewModel.kt'),
    'app/src/main/java/com/example/ui/viewmodel/HomeViewModel.kt': ('com.example.feature.chat', 'app/src/main/java/com/example/feature/chat/HomeViewModel.kt'),
    'app/src/main/java/com/example/data/repository/ChatRepository.kt': ('com.example.feature.chat', 'app/src/main/java/com/example/feature/chat/ChatRepository.kt'),

    'app/src/main/java/com/example/ui/screen/SettingsScreen.kt': ('com.example.feature.settings', 'app/src/main/java/com/example/feature/settings/SettingsScreen.kt'),
    'app/src/main/java/com/example/ui/viewmodel/SettingsViewModel.kt': ('com.example.feature.settings', 'app/src/main/java/com/example/feature/settings/SettingsViewModel.kt'),
    'app/src/main/java/com/example/data/repository/SettingsRepository.kt': ('com.example.feature.settings', 'app/src/main/java/com/example/feature/settings/SettingsRepository.kt'),

    'app/src/main/java/com/example/ui/screen/FeatureScreens.kt': ('com.example.feature.tools', 'app/src/main/java/com/example/feature/tools/FeatureScreens.kt'),

    'app/src/main/java/com/example/data/repository/MemoryRepository.kt': ('com.example.feature.memory', 'app/src/main/java/com/example/feature/memory/MemoryRepository.kt')
}

decl_to_pkg = {
    'MainActivity': 'com.example',
    'NabihApplication': 'com.example',
    'ViewModelFactory': 'com.example.core.di',
    'ChatUiState': 'com.example.feature.chat',
    'ChatViewModel': 'com.example.feature.chat',
    'ReasoningMode': 'com.example.feature.chat',
    'SettingsViewModel': 'com.example.feature.settings',
    'HomeViewModel': 'com.example.feature.chat',
    'LightOnPrimary': 'com.example.core.theme',
    'DarkOnPrimary': 'com.example.core.theme',
    'DarkOutline': 'com.example.core.theme',
    'NabihBlue': 'com.example.core.theme',
    'DarkOnSurface': 'com.example.core.theme',
    'DarkBackground': 'com.example.core.theme',
    'LightOutline': 'com.example.core.theme',
    'LightSurfaceVariant': 'com.example.core.theme',
    'LightOnSurfaceVariant': 'com.example.core.theme',
    'DarkOnBackground': 'com.example.core.theme',
    'NabihSlate': 'com.example.core.theme',
    'DarkPrimary': 'com.example.core.theme',
    'LightOnSurface': 'com.example.core.theme',
    'NabihRed': 'com.example.core.theme',
    'LightPrimary': 'com.example.core.theme',
    'LightOnBackground': 'com.example.core.theme',
    'NabihBlueLight': 'com.example.core.theme',
    'NabihDarkSlate': 'com.example.core.theme',
    'LightBackground': 'com.example.core.theme',
    'DarkSurface': 'com.example.core.theme',
    'LightSurface': 'com.example.core.theme',
    'DarkSurfaceVariant': 'com.example.core.theme',
    'DarkOnSurfaceVariant': 'com.example.core.theme',
    'Typography': 'com.example.core.theme',
    'NabihTheme': 'com.example.core.theme',
    'TypingAnimation': 'com.example.core.ui',
    'MarkdownBlock': 'com.example.core.ui',
    'MarkdownRenderer': 'com.example.core.ui',
    'CodeBlockLayout': 'com.example.core.ui',
    'ApiKeysSection': 'com.example.feature.settings',
    'PrivacySecuritySection': 'com.example.feature.settings',
    'SettingsSectionCard': 'com.example.feature.settings',
    'LanguageSection': 'com.example.feature.settings',
    'AppearanceSection': 'com.example.feature.settings',
    'SettingsScreen': 'com.example.feature.settings',
    'AccountScreen': 'com.example.feature.auth',
    'OnboardingSlide': 'com.example.feature.auth',
    'LoginScreen': 'com.example.feature.auth',
    'FilesScreen': 'com.example.feature.tools',
    'GenericFeatureScreen': 'com.example.feature.tools',
    'AiToolsScreen': 'com.example.feature.tools',
    'PrivacyScreen': 'com.example.feature.tools',
    'HelpScreen': 'com.example.feature.tools',
    'SavedChatsScreen': 'com.example.feature.tools',
    'VoiceScreen': 'com.example.feature.chat',
    'MainDrawerContent': 'com.example.feature.chat',
    'BottomInputArea': 'com.example.feature.chat',
    'MessageItem': 'com.example.feature.chat',
    'EmptyChatState': 'com.example.feature.chat',
    'MainScreen': 'com.example.feature.chat',
    'AuthManager': 'com.example.feature.auth',
    'FolderDao': 'com.example.core.database',
    'MemoryDao': 'com.example.core.database',
    'ConversationDao': 'com.example.core.database',
    'MessageDao': 'com.example.core.database',
    'Message': 'com.example.core.database',
    'Folder': 'com.example.core.database',
    'MemoryItem': 'com.example.core.database',
    'Conversation': 'com.example.core.database',
    'DefaultAppContainer': 'com.example.core.di',
    'AppContainer': 'com.example.core.di',
    'MemoryRepository': 'com.example.feature.memory',
    'SettingsRepository': 'com.example.feature.settings',
    'ChatRepository': 'com.example.feature.chat',
    'AiModel': 'com.example.core.model',
    'AppLanguage': 'com.example.core.model',
    'AppSettings': 'com.example.core.model',
    'FontSize': 'com.example.core.model',
    'AppTheme': 'com.example.core.model',
    'ResponseStyle': 'com.example.core.model',
    'ApiProvider': 'com.example.core.model',
    'ClaudeMessage': 'com.example.core.network',
    'OpenAiApiService': 'com.example.core.network',
    'ClaudeResponse': 'com.example.core.network',
    'OpenAiResponse': 'com.example.core.network',
    'GeminiCandidate': 'com.example.core.network',
    'OpenAiChoice': 'com.example.core.network',
    'NetworkClient': 'com.example.core.network',
    'GeminiContent': 'com.example.core.network',
    'OpenAiMessage': 'com.example.core.network',
    'GeminiRequest': 'com.example.core.network',
    'GeminiApiService': 'com.example.core.network',
    'GeminiPart': 'com.example.core.network',
    'OpenAiRequest': 'com.example.core.network',
    'GeminiInlineData': 'com.example.core.network',
    'OpenAiMessageContent': 'com.example.core.network',
    'ClaudeApiService': 'com.example.core.network',
    'GeminiGenerationConfig': 'com.example.core.network',
    'ClaudeContentPart': 'com.example.core.network',
    'GeminiResponse': 'com.example.core.network',
    'ClaudeRequest': 'com.example.core.network',
    'AppDatabase': 'com.example.core.database',
    'Daos': 'com.example.core.database'
}

# Add AppDatabase manually just in case
decl_to_pkg['AppDatabase'] = 'com.example.core.database'

# Create directories and move files
for old_path, (new_pkg, new_path) in file_moves.items():
    if os.path.exists(old_path):
        os.makedirs(os.path.dirname(new_path), exist_ok=True)
        shutil.move(old_path, new_path)

# Find all kt files
kt_files = []
for root, _, files in os.walk(dir_path):
    for file in files:
        if file.endswith('.kt'):
            kt_files.append(os.path.join(root, file))

for path in kt_files:
    with open(path, 'r') as f:
        content = f.read()
    
    # 1. Update package
    # Find new package from path
    # e.g. app/src/main/java/com/example/core/di/ViewModelFactory.kt -> com.example.core.di
    rel_path = os.path.relpath(path, 'app/src/main/java')
    new_pkg = os.path.dirname(rel_path).replace('/', '.')
    
    # Replace package
    content = re.sub(r'^package\s+[a-zA-Z0-9_.]+', f'package {new_pkg}', content, count=1, flags=re.MULTILINE)
    
    # 2. Remove all old internal imports
    lines = content.split('\n')
    new_lines = []
    for line in lines:
        if line.startswith('import com.example.ui') or line.startswith('import com.example.data') or line.startswith('import com.example.R'):
            continue # Remove old
        # Also remove any that might match our new packages if they were somehow added, to start clean
        # Wait, keep com.example.R but remove the others? R doesn't move. It's generated in com.example.
        # So we keep import com.example.R
        if line.startswith('import com.example.') and not line.startswith('import com.example.R'):
            continue
        new_lines.append(line)
        
    content = '\n'.join(new_lines)
    
    # 3. Find usages of known declarations
    needed_imports = set()
    for decl, pkg in decl_to_pkg.items():
        # If decl is used in the file as a whole word
        if re.search(r'\b' + decl + r'\b', content):
            if pkg != new_pkg and pkg != 'com.example': # Don't import from same package, and don't import from default root if not necessary (actually we might need to import from com.example if we are in core.di? Kotlin allows com.example.* but it's better to be explicit if it's top level, wait MainActivity is in com.example, we don't import it usually)
                needed_imports.add(f"import {pkg}.{decl}")
                
    # Insert new imports after the package declaration
    if needed_imports:
        pkg_match = re.search(r'^package\s+[a-zA-Z0-9_.]+', content, re.MULTILINE)
        if pkg_match:
            insert_pos = pkg_match.end()
            imports_str = "\n\n" + "\n".join(sorted(list(needed_imports)))
            content = content[:insert_pos] + imports_str + content[insert_pos:]
            
    # Fix import com.example.R if it was deleted
    if 'R.' in content and 'import com.example.R' not in content:
        pkg_match = re.search(r'^package\s+[a-zA-Z0-9_.]+', content, re.MULTILINE)
        if pkg_match:
            insert_pos = pkg_match.end()
            content = content[:insert_pos] + "\n\nimport com.example.R" + content[insert_pos:]

    with open(path, 'w') as f:
        f.write(content)

# Clean up empty directories
for root, dirs, files in os.walk(dir_path, topdown=False):
    for d in dirs:
        d_path = os.path.join(root, d)
        if not os.listdir(d_path):
            os.rmdir(d_path)

print("Refactoring complete.")
