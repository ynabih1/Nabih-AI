class GeminiProvider : AiProvider {
    override suspend fun generateResponse(
        modelId: String,
        apiKey: String,
        systemPrompt: String,
        prompt: String,
        history: List<Message>,
        attachedBase64Image: String?,
        attachedDocText: String?
    ): String {
        val finalPrompt = if (attachedDocText != null) {
            "Document Attached:\n$attachedDocText\n\n$prompt"
        } else prompt

        val contents = mutableListOf<GeminiContent>()
        history.forEach { msg ->
            contents.add(GeminiContent(
                role = if (msg.role == "user") "user" else "model",
                parts = listOf(GeminiPart(text = msg.content))
            ))
        }

        val parts = mutableListOf<GeminiPart>()
        if (attachedBase64Image != null) {
            parts.add(GeminiPart(inlineData = GeminiInlineData(mimeType = "image/jpeg", data = attachedBase64Image)))
        }
        parts.add(GeminiPart(text = finalPrompt))
        contents.add(GeminiContent(role = "user", parts = parts))

        val req = GeminiRequest(
            contents = contents,
            systemInstruction = GeminiContent(parts = listOf(GeminiPart(text = systemPrompt))),
            generationConfig = GeminiGenerationConfig(temperature = 0.7f)
        )

        val response = NetworkClient.geminiService.generateContent(modelId, apiKey, req)
        return response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: throw Exception("رد فارغ من الموديل")
    }

    override fun generateResponseStream(
        modelId: String,
        apiKey: String,
        systemPrompt: String,
        prompt: String,
        history: List<Message>,
        attachedBase64Image: String?,
        attachedDocText: String?
    ): Flow<String> = kotlinx.coroutines.flow.flow {
        val finalPrompt = if (attachedDocText != null) {
            "Document Attached:\n$attachedDocText\n\n$prompt"
        } else prompt

        val contents = mutableListOf<GeminiContent>()
        history.forEach { msg ->
            contents.add(GeminiContent(
                role = if (msg.role == "user") "user" else "model",
                parts = listOf(GeminiPart(text = msg.content))
            ))
        }

        val parts = mutableListOf<GeminiPart>()
        if (attachedBase64Image != null) {
            parts.add(GeminiPart(inlineData = GeminiInlineData(mimeType = "image/jpeg", data = attachedBase64Image)))
        }
        parts.add(GeminiPart(text = finalPrompt))
        contents.add(GeminiContent(role = "user", parts = parts))

        val req = GeminiRequest(
            contents = contents,
            systemInstruction = GeminiContent(parts = listOf(GeminiPart(text = systemPrompt))),
            generationConfig = GeminiGenerationConfig(temperature = 0.7f)
        )

        try {
            val responseBody = NetworkClient.geminiService.generateContentStream(modelId, apiKey, req)
            val reader = responseBody.byteStream().bufferedReader()
            var line: String?
            var receivedAny = false
            while (reader.readLine().also { line = it } != null) {
                var l = line?.trim() ?: continue
                if (l.isEmpty()) continue
                if (l.startsWith("data:")) {
                    l = l.substring(5).trim()
                }

                val textToken = "\"text\":"
                var index = l.indexOf(textToken)
                while (index != -1) {
                    val valQuoteStart = l.indexOf('"', index + textToken.length)
                    if (valQuoteStart != -1) {
                        var valEnd = valQuoteStart + 1
                        var escaped = false
                        while (valEnd < l.length) {
                            val c = l[valEnd]
                            if (escaped) {
                                escaped = false
                            } else if (c == '\\') {
                                escaped = true
                            } else if (c == '"') {
                                break
                            }
                            valEnd++
                        }
                        if (valEnd < l.length) {
                            val rawText = l.substring(valQuoteStart + 1, valEnd)
                            val unescaped = unescapeJsonString(rawText)
                            if (unescaped.isNotEmpty()) {
                                emit(unescaped)
                                receivedAny = true
                            }
                        }
                    }
                    index = l.indexOf(textToken, index + 1)
                }
            }
            if (!receivedAny) {
                val fallback = generateResponse(modelId, apiKey, systemPrompt, prompt, history, attachedBase64Image, attachedDocText)
                emit(fallback)
            }
        } catch (e: Exception) {
            // Graceful typing fallback
            val fallback = generateResponse(modelId, apiKey, systemPrompt, prompt, history, attachedBase64Image, attachedDocText)
            var currentLen = 0
            val chunkSize = 4
            while (currentLen < fallback.length) {
                val nextLen = (currentLen + chunkSize).coerceAtMost(fallback.length)
                emit(fallback.substring(currentLen, nextLen))
                currentLen = nextLen
                kotlinx.coroutines.delay(12)
            }
        }
    }
}
