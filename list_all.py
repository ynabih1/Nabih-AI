import os
import re

dir_path = 'app/src/main/java/com/example'
file_moves = {
    'app/src/main/java/com/example/ui/viewmodel/ViewModelFactory.kt': 'com.example.core.di',
    'app/src/main/java/com/example/data/di/AppContainer.kt': 'com.example.core.di',
    'app/src/main/java/com/example/data/database/AppDatabase.kt': 'com.example.core.database',
    'app/src/main/java/com/example/data/database/Daos.kt': 'com.example.core.database',
    'app/src/main/java/com/example/data/database/Entities.kt': 'com.example.core.database',
    'app/src/main/java/com/example/data/network/ApiServices.kt': 'com.example.core.network',
    'app/src/main/java/com/example/data/model/Models.kt': 'com.example.core.model',
    'app/src/main/java/com/example/ui/theme/Theme.kt': 'com.example.core.theme',
    'app/src/main/java/com/example/ui/theme/Color.kt': 'com.example.core.theme',
    'app/src/main/java/com/example/ui/theme/Type.kt': 'com.example.core.theme',
    'app/src/main/java/com/example/ui/components/TypingAnimation.kt': 'com.example.core.ui',
    'app/src/main/java/com/example/ui/components/MarkdownRenderer.kt': 'com.example.core.ui',
    'app/src/main/java/com/example/ui/auth/AuthManager.kt': 'com.example.feature.auth',
    'app/src/main/java/com/example/ui/screen/LoginScreen.kt': 'com.example.feature.auth',
    'app/src/main/java/com/example/ui/screen/AccountScreen.kt': 'com.example.feature.auth',
    'app/src/main/java/com/example/ui/screen/MainScreen.kt': 'com.example.feature.chat',
    'app/src/main/java/com/example/ui/screen/VoiceScreen.kt': 'com.example.feature.chat',
    'app/src/main/java/com/example/ui/viewmodel/ChatViewModel.kt': 'com.example.feature.chat',
    'app/src/main/java/com/example/ui/viewmodel/HomeViewModel.kt': 'com.example.feature.chat',
    'app/src/main/java/com/example/data/repository/ChatRepository.kt': 'com.example.feature.chat',
    'app/src/main/java/com/example/ui/screen/SettingsScreen.kt': 'com.example.feature.settings',
    'app/src/main/java/com/example/ui/viewmodel/SettingsViewModel.kt': 'com.example.feature.settings',
    'app/src/main/java/com/example/data/repository/SettingsRepository.kt': 'com.example.feature.settings',
    'app/src/main/java/com/example/ui/screen/FeatureScreens.kt': 'com.example.feature.tools',
    'app/src/main/java/com/example/data/repository/MemoryRepository.kt': 'com.example.feature.memory',
    'app/src/main/java/com/example/MainActivity.kt': 'com.example',
    'app/src/main/java/com/example/NabihApplication.kt': 'com.example'
}

decl_to_pkg = {}

for root, _, files in os.walk(dir_path):
    for file in files:
        if file.endswith('.kt'):
            path = os.path.join(root, file)
            with open(path, 'r') as f:
                content = f.read()
            
            new_pkg = file_moves.get(path, 'com.example')
            
            # Find class, interface, object, enum class
            decls = re.findall(r'^(?:suspend\s+)?(?:data\s+)?(?:sealed\s+)?(?:class|interface|object|enum class)\s+([A-Z][a-zA-Z0-9_]+)', content, re.MULTILINE)
            # Find @Composable fun
            funcs = re.findall(r'^@Composable\s*\n(?:@.*\n)*fun\s+([A-Z][a-zA-Z0-9_]+)', content, re.MULTILINE)
            # Find val/vars
            vals = re.findall(r'^val\s+([A-Z][a-zA-Z0-9_]+)', content, re.MULTILINE)
            
            for d in set(decls + funcs + vals):
                decl_to_pkg[d] = new_pkg

for d, p in decl_to_pkg.items():
    print(f"{d} -> {p}")
