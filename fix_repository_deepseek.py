import re

with open('app/src/main/java/com/example/data/repository/ChatRepository.kt', 'r') as f:
    code = f.read()

# Remove deepseek specific conditions
code = code.replace("ApiProvider.DEEPSEEK -> settings.deepseekApiKey", "")
code = code.replace("ApiProvider.OPENAI, ApiProvider.DEEPSEEK -> {", "ApiProvider.OPENAI -> {")
code = code.replace("val service = if (actualModel.provider == ApiProvider.DEEPSEEK) NetworkClient.deepseekService else NetworkClient.openAiService", "val service = NetworkClient.openAiService")

with open('app/src/main/java/com/example/data/repository/ChatRepository.kt', 'w') as f:
    f.write(code)

