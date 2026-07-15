import re

with open('app/src/main/java/com/example/feature/chat/ChatRepository.kt', 'r') as f:
    content = f.read()

start_marker = "fun streamChatResponse("
end_marker = "suspend fun duplicateConversation(originalId: String): String = withContext(Dispatchers.IO) {"

if start_marker in content and end_marker in content:
    start_idx = content.find(start_marker)
    end_idx = content.find(end_marker)
    
    # We will use the exact parameters to replace
    # But wait, to be safe, we can just replace everything from `fun streamChatResponse(` to the end marker.
    
    new_method = """fun streamChatResponse(
        conversationId: String,
        prompt: String,
        attachedImageUri: Uri? = null,
        attachedDocUri: Uri? = null,
        searchEnabled: Boolean = false
    ): Flow<String> = flow {
        val settings = settingsRepository.settings.value
        val conversation = conversationDao.getConversationById(conversationId) ?: throw Exception("Conversation not found")
        val registryModel = com.example.core.model.ModelRegistry.getModels(context).find { it.id == conversation.modelId }
            ?: throw Exception("Model not found.")
            
        val isArabic = settings.language == com.example.core.model.AppLanguage.ARABIC
        val conversationLock = conversationLocks.getOrPut(conversationId) { kotlinx.coroutines.sync.Mutex() }
        
        conversationLock.lock()
        try {
            val history = messageDao.getMessagesForConversationSync(conversationId)
            var memoriesStr = ""
            if (settings.memoryEnabled) {
                val list = memoryDao.getAllMemoryItems().first()
                if (list.isNotEmpty()) {
                    memoriesStr = "Nabih remembered the following personal facts about the user:\\n" +
                            list.joinToString("\\n") { "- ${it.content}" }
                }
            }
            
            var searchContext = ""
            if (searchEnabled) {
                try {
                    val wikiResponse = com.example.core.network.NetworkClient.wikipediaService.searchWikipedia(prompt)
                    val topResults = wikiResponse.query?.search?.take(3)?.joinToString("\\n") {
                        "${it.title}: ${it.snippet.replace(Regex("<[^>]*>"), "")}"
                    } ?: "No results found."
                    searchContext = "\\n[WEB SEARCH RESULTS for '${prompt}':\\n$topResults\\nProvide the most accurate answer based on these search results.]\\n"
                } catch (e: Exception) {
                    searchContext = "\\n[WEB SEARCH FAILED: ${e.message}]\\n"
                }
            }
            
            val systemPrompt = "You are Nabih AI, a fast, efficient AI assistant. " +
                    "You speak Arabic and English natively. " +
                    "CRITICAL INSTRUCTIONS: You must provide concise and direct answers. Avoid unnecessary explanations, introductions, or repeated information. Answer exactly what the user asks using the shortest accurate answer possible. Do not use greetings or filler phrases. Do not repeat the user's question. Add details only when explicitly requested. If information is unknown, say so briefly. Provide accurate, structured answers. " +
                    "Current local time: ${System.currentTimeMillis()}.\\n$memoriesStr\\n$searchContext"
                    
            var activePrompt = prompt
            var attachedBase64Image: String? = null
            var attachedDocText: String? = null
            
            if (attachedImageUri != null) {
                attachedBase64Image = uriToBase64(attachedImageUri)
                activePrompt += "\\n[An image attachment is sent. Understand and reference the attached image contents in your response.]"
            }
            
            if (attachedDocUri != null) {
                val (docName, docText) = parseAttachedDocument(attachedDocUri)
                attachedDocText = docText
                activePrompt += "\\n[Document Attached: $docName. Use the contents for reasoning:\\n$attachedDocText]"
            }
            
            // Generate cache key
            val cacheKey = "conv_${conversationId}_prompt_${prompt.hashCode()}_img_${attachedBase64Image?.hashCode() ?: 0}_doc_${attachedDocText?.hashCode() ?: 0}_model_${registryModel.id}"
            if (responseCache.containsKey(cacheKey)) {
                val cachedText = responseCache[cacheKey]!!
                var currentLength = 0
                val chunkSize = 4
                while (currentLength < cachedText.length) {
                    currentLength = (currentLength + chunkSize).coerceAtMost(cachedText.length)
                    emit(cachedText.substring(0, currentLength))
                    kotlinx.coroutines.delay(10)
                }
                return@flow
            }
            
            val modelsToTry = mutableListOf<String>()
            val isGeminiRequest = registryModel.provider == com.example.core.model.ApiProvider.GOOGLE || registryModel.provider == com.example.core.model.ApiProvider.NABIH
            
            if (isGeminiRequest) {
                val requestedId = if (registryModel.id == "nabih-ultra") "gemini-2.5-pro" else registryModel.id
                modelsToTry.add(requestedId)
                val candidates = listOf("gemini-2.5-pro", "gemini-2.5-flash", "gemini-1.5-pro", "gemini-1.5-flash", "gemini-1.0-pro")
                candidates.forEach { cand ->
                    if (cand != requestedId && !modelsToTry.contains(cand)) {
                        modelsToTry.add(cand)
                    }
                }
            } else {
                modelsToTry.add(registryModel.id)
            }
            
            var responseText = ""
            var success = false
            var lastException: Exception? = null
            
            for (modelIndex in modelsToTry.indices) {
                val currentModelId = modelsToTry[modelIndex]
                val maxRetries = 3
                var currentTry = 0
                var modelSuccess = false
                
                while (currentTry < maxRetries && !modelSuccess) {
                    try {
                        val currentApiKey = when (registryModel.provider) {
                            com.example.core.model.ApiProvider.NABIH -> settings.nabihApiKey.ifBlank { com.example.BuildConfig.GEMINI_API_KEY }
                            com.example.core.model.ApiProvider.GOOGLE -> settings.googleApiKey.ifBlank { com.example.BuildConfig.GEMINI_API_KEY }
                            com.example.core.model.ApiProvider.OPENAI -> settings.openaiApiKey
                            com.example.core.model.ApiProvider.ANTHROPIC -> settings.anthropicApiKey
                            com.example.core.model.ApiProvider.GROK -> settings.grokApiKey
                            com.example.core.model.ApiProvider.DEEPSEEK -> settings.deepseekApiKey
                            com.example.core.model.ApiProvider.MISTRAL -> settings.mistralApiKey
                            com.example.core.model.ApiProvider.OPENROUTER -> settings.openRouterApiKey
                            com.example.core.model.ApiProvider.OLLAMA -> settings.ollamaEndpoint
                            com.example.core.model.ApiProvider.LMSTUDIO -> settings.lmStudioEndpoint
                        }
                        
                        if (currentApiKey.isEmpty() && registryModel.provider != com.example.core.model.ApiProvider.NABIH && registryModel.provider != com.example.core.model.ApiProvider.GOOGLE) {
                            throw Exception("API Key Required: ${registryModel.provider.displayName} API Key is missing. Please add it in Settings.")
                        }
                        
                        val textResult = when (registryModel.provider) {
                            com.example.core.model.ApiProvider.GOOGLE, com.example.core.model.ApiProvider.NABIH -> {
                                val parts = mutableListOf<com.example.core.network.GeminiPart>()
                                parts.add(com.example.core.network.GeminiPart(text = activePrompt))
                                if (attachedBase64Image != null) {
                                    parts.add(com.example.core.network.GeminiPart(inlineData = com.example.core.network.GeminiInlineData("image/jpeg", attachedBase64Image)))
                                }
                                val contents = mutableListOf<com.example.core.network.GeminiContent>()
                                history.forEach { msg ->
                                    contents.add(com.example.core.network.GeminiContent(
                                        role = if (msg.role == "user") "user" else "model",
                                        parts = listOf(com.example.core.network.GeminiPart(text = msg.content))
                                    ))
                                }
                                contents.add(com.example.core.network.GeminiContent(role = "user", parts = parts))
                                val req = com.example.core.network.GeminiRequest(
                                    contents = contents,
                                    systemInstruction = com.example.core.network.GeminiContent(role = "user", parts = listOf(com.example.core.network.GeminiPart(text = systemPrompt))),
                                    generationConfig = com.example.core.network.GeminiGenerationConfig(temperature = 0.7f)
                                )
                                val response = com.example.core.network.NetworkClient.geminiService.generateContent(currentModelId, currentApiKey, req)
                                response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: throw Exception("No text generated")
                            }
                            com.example.core.model.ApiProvider.OPENAI, com.example.core.model.ApiProvider.GROK, com.example.core.model.ApiProvider.DEEPSEEK,
                            com.example.core.model.ApiProvider.MISTRAL, com.example.core.model.ApiProvider.OPENROUTER, com.example.core.model.ApiProvider.OLLAMA,
                            com.example.core.model.ApiProvider.LMSTUDIO -> {
                                val messages = mutableListOf<com.example.core.network.OpenAiMessage>()
                                messages.add(com.example.core.network.OpenAiMessage("system", systemPrompt))
                                history.forEach { msg ->
                                    messages.add(com.example.core.network.OpenAiMessage(if (msg.role == "user") "user" else "assistant", msg.content))
                                }
                                messages.add(com.example.core.network.OpenAiMessage("user", activePrompt))
                                val req = com.example.core.network.OpenAiRequest(model = currentModelId, messages = messages, temperature = 0.7f)
                                val (endpointUrl, authHeader) = when (registryModel.provider) {
                                    com.example.core.model.ApiProvider.OPENAI -> "https://api.openai.com/v1/chat/completions" to "Bearer $currentApiKey"
                                    com.example.core.model.ApiProvider.GROK -> "https://api.x.ai/v1/chat/completions" to "Bearer $currentApiKey"
                                    com.example.core.model.ApiProvider.DEEPSEEK -> "https://api.deepseek.com/chat/completions" to "Bearer $currentApiKey"
                                    com.example.core.model.ApiProvider.MISTRAL -> "https://api.mistral.ai/v1/chat/completions" to "Bearer $currentApiKey"
                                    com.example.core.model.ApiProvider.OPENROUTER -> "https://openrouter.ai/api/v1/chat/completions" to "Bearer $currentApiKey"
                                    com.example.core.model.ApiProvider.OLLAMA -> {
                                        var base = currentApiKey.trimEnd('/')
                                        base = base.replace("localhost", "10.0.2.2").replace("127.0.0.1", "10.0.2.2")
                                        val url = if (base.endsWith("/v1")) "$base/chat/completions" else "$base/v1/chat/completions"
                                        url to "Bearer ollama"
                                    }
                                    com.example.core.model.ApiProvider.LMSTUDIO -> {
                                        var base = currentApiKey.trimEnd('/')
                                        base = base.replace("localhost", "10.0.2.2").replace("127.0.0.1", "10.0.2.2")
                                        val url = if (base.endsWith("/v1")) "$base/chat/completions" else "$base/v1/chat/completions"
                                        url to "Bearer lmstudio"
                                    }
                                    else -> "https://api.openai.com/v1/chat/completions" to "Bearer $currentApiKey"
                                }
                                val response = com.example.core.network.NetworkClient.openAiService.generateCompletion(endpointUrl, authHeader, req)
                                response.choices?.firstOrNull()?.message?.content ?: throw Exception("No text response generated.")
                            }
                            com.example.core.model.ApiProvider.ANTHROPIC -> {
                                val messages = mutableListOf<com.example.core.network.ClaudeMessage>()
                                history.forEach { msg ->
                                    messages.add(com.example.core.network.ClaudeMessage(if (msg.role == "user") "user" else "assistant", msg.content))
                                }
                                messages.add(com.example.core.network.ClaudeMessage("user", activePrompt))
                                val req = com.example.core.network.ClaudeRequest(model = currentModelId, messages = messages, system = systemPrompt, temperature = 0.7f)
                                val response = com.example.core.network.NetworkClient.claudeService.generateMessage(apiKey = currentApiKey, request = req)
                                response.content?.firstOrNull()?.text ?: throw Exception("No text response generated.")
                            }
                        }
                        
                        responseText = textResult
                        modelSuccess = true
                        
                        var currentLength = 0
                        val chunkSize = 4
                        while (currentLength < responseText.length) {
                            currentLength = (currentLength + chunkSize).coerceAtMost(responseText.length)
                            emit(responseText.substring(0, currentLength))
                            kotlinx.coroutines.delay(12)
                        }
                    } catch (e: Exception) {
                        lastException = e
                        android.util.Log.e("ChatRepository", "API Request failed on try $currentTry for model $currentModelId. Details: ${e.stackTraceToString()}", e)
                        
                        val isAuthError = e is retrofit2.HttpException && (e.code() == 401 || e.code() == 403)
                        if (isAuthError) {
                            break
                        }
                        
                        currentTry++
                        if (currentTry < maxRetries) {
                            val isRateLimit = e.message?.contains("429") == true || (e is retrofit2.HttpException && e.code() == 429)
                            val backoffDelay = if (isRateLimit) {
                                2000L * (1 shl currentTry)
                            } else {
                                1000L * currentTry
                            }
                            kotlinx.coroutines.delay(backoffDelay)
                        }
                    }
                }
                
                if (modelSuccess) {
                    success = true
                    break
                }
            }
            
            if (!success) {
                val errorMsg = lastException?.localizedMessage ?: "Failed to connect to AI provider after candidate fallback."
                throw Exception(errorMsg)
            }
            
            responseCache[cacheKey] = responseText
        } finally {
            conversationLock.unlock()
        }
    }.flowOn(kotlinx.coroutines.Dispatchers.IO)
    
"""
    
    new_content = content[:start_idx] + new_method + content[end_idx:]
    with open('app/src/main/java/com/example/feature/chat/ChatRepository.kt', 'w') as f:
        f.write(new_content)
    print("ChatRepository patched successfully")
else:
    print("Markers not found!")
