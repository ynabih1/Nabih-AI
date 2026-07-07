import re
with open('app/src/main/java/com/example/ui/viewmodel/SettingsViewModel.kt', 'r') as f:
    code = f.read()

code = re.sub(r'fun saveApiKeys\(google: String, openai: String, anthropic: String, deepseek: String\) {[\s\S]*?\}',
              'fun saveApiKeys(google: String, openai: String, anthropic: String) {\n        settingsRepository.updateApiKeys(google, openai, anthropic)\n    }', code)

with open('app/src/main/java/com/example/ui/viewmodel/SettingsViewModel.kt', 'w') as f:
    f.write(code)
