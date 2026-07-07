import re

with open('app/src/main/java/com/example/data/repository/ChatRepository.kt', 'r') as f:
    content = f.read()

old_routing = """        // 1. Automatic Model & Provider Routing
        var actualModel = model
        var finalApiKey = ""

        if (actualModel.provider == ApiProvider.NABIH) {
            finalApiKey = BuildConfig.GEMINI_API_KEY
            if (finalApiKey.isEmpty() || finalApiKey == "MY_GEMINI_API_KEY" || finalApiKey.startsWith("YOUR_")) {
                emit("Nabih Ultra is currently offline. Please configure the backend API key or try again later.")
                return@flow
            }
            actualModel = AiModel.GEMINI_FLASH // map under the hood
            // Bypass user key requirement for Nabih Ultra
        } else if (actualModel.provider == ApiProvider.GOOGLE) {"""

new_routing = """        // 1. Automatic Model & Provider Routing
        var actualModel = model
        var actualModelId = actualModel.id
        var finalApiKey = ""

        if (actualModel.provider == ApiProvider.NABIH) {
            finalApiKey = BuildConfig.GEMINI_API_KEY
            if (finalApiKey.isEmpty() || finalApiKey == "MY_GEMINI_API_KEY" || finalApiKey.startsWith("YOUR_")) {
                emit("Nabih Ultra is currently offline. Please configure the backend API key or try again later.")
                return@flow
            }
            actualModelId = "gemini-3.1-flash-lite-preview" // Use this for Nabih Ultra
            // Bypass user key requirement for Nabih Ultra
        } else if (actualModel.provider == ApiProvider.GOOGLE) {"""

content = content.replace(old_routing, new_routing)

old_api_call = """                        val req = GeminiRequest(
                            contents = geminiContents,
                            systemInstruction = GeminiContent(parts = listOf(GeminiPart(text = systemPrompt))),
                            generationConfig = GeminiGenerationConfig(temperature = 0.7f)
                        )
                        val response = NetworkClient.geminiService.generateContent(actualModel.id, finalApiKey, req)"""

new_api_call = """                        val req = GeminiRequest(
                            contents = geminiContents,
                            systemInstruction = GeminiContent(parts = listOf(GeminiPart(text = systemPrompt))),
                            generationConfig = GeminiGenerationConfig(temperature = 0.7f)
                        )
                        val response = NetworkClient.geminiService.generateContent(actualModelId, finalApiKey, req)"""

content = content.replace(old_api_call, new_api_call)

old_openai_call = """                        val req = OpenAiRequest(
                            model = actualModel.id,
                            messages = messages,
                            temperature = 0.7f
                        )"""

new_openai_call = """                        val req = OpenAiRequest(
                            model = actualModelId,
                            messages = messages,
                            temperature = 0.7f
                        )"""

content = content.replace(old_openai_call, new_openai_call)

with open('app/src/main/java/com/example/data/repository/ChatRepository.kt', 'w') as f:
    f.write(content)
