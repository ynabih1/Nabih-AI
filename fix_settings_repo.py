import re

with open('app/src/main/java/com/example/data/repository/SettingsRepository.kt', 'r') as f:
    code = f.read()

code = re.sub(r'deepseekApiKey = prefs\.getString\("deepseekApiKey", ""\) \?: "",\n', '', code)
code = re.sub(r'putString\("deepseekApiKey", settings\.deepseekApiKey\)\n', '', code)

with open('app/src/main/java/com/example/data/repository/SettingsRepository.kt', 'w') as f:
    f.write(code)

