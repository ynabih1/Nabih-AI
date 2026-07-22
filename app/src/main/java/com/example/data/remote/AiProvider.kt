package com.example.data.remote

import com.example.data.local.Message
import com.example.model.ApiProvider
import com.example.model.ModelMetadata
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig

// --- 1. Provider Interface ---
interface AiProvider {
    suspend fun generateResponse(
        modelId: String,
        apiKey: String,
        systemPrompt: String,
        prompt: String,
        history: List<Message>,
        attachedBase64Image: String? = null,
        attachedDocText: String? = null
    ): String

    fun generateResponseStream(
        modelId: String,
        apiKey: String,
        systemPrompt: String,
        prompt: String,
        history: List<Message>,
        attachedBase64Image: String? = null,
        attachedDocText: String? = null
    ): Flow<String>
}

// --- 2. Gemini Provider Implementation ---
/**
 * تم التحول لـ Firebase AI Logic SDK بدلاً من REST calls اليدوية بسبب تغيير جوجل صيغة مفاتيح Gemini API (من AIzaSy إلى AQ.Ab) في يونيو 2026، والذي جعل الاستدعاء اليدوي غير موثوق.
 */
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

// --- 3. OpenAI / ChatGPT / DeepSeek / Mistral Provider Implementation ---
class OpenAiProvider(private val providerType: ApiProvider) : AiProvider {
    override suspend fun generateResponse(
        modelId: String,
        apiKey: String,
        systemPrompt: String,
        prompt: String,
        history: List<Message>,
        attachedBase64Image: String?,
        attachedDocText: String?
    ): String {
        val messages = mutableListOf<OpenAiMessage>()
        messages.add(OpenAiMessage("system", systemPrompt))
        history.forEach { msg ->
            messages.add(OpenAiMessage(if (msg.role == "user") "user" else "assistant", msg.content))
        }
        messages.add(OpenAiMessage("user", prompt))
        val req = OpenAiRequest(model = modelId, messages = messages, temperature = 0.7f)
        val (endpointUrl, authHeader) = getEndpointAndHeader(providerType, apiKey)
        val response = NetworkClient.openAiService.generateCompletion(endpointUrl, authHeader, req)
        return response.choices?.firstOrNull()?.message?.content ?: throw Exception("No text response generated.")
    }

    override fun generateResponseStream(
        modelId: String,
        apiKey: String,
        systemPrompt: String,
        prompt: String,
        history: List<Message>,
        attachedBase64Image: String?,
        attachedDocText: String?
    ): Flow<String> = flow {
        val messages = mutableListOf<OpenAiMessage>()
        messages.add(OpenAiMessage("system", systemPrompt))
        history.forEach { msg ->
            messages.add(OpenAiMessage(if (msg.role == "user") "user" else "assistant", msg.content))
        }
        messages.add(OpenAiMessage("user", prompt))
        val req = OpenAiRequest(model = modelId, messages = messages, temperature = 0.7f, stream = true)
        val (endpointUrl, authHeader) = getEndpointAndHeader(providerType, apiKey)

        try {
            val responseBody = NetworkClient.openAiService.generateCompletionStream(endpointUrl, authHeader, req)
            val reader = responseBody.byteStream().bufferedReader()
            var line: String?
            var receivedAny = false
            while (reader.readLine().also { line = it } != null) {
                var l = line?.trim() ?: continue
                if (l.isEmpty()) continue
                if (l.startsWith("data:")) {
                    l = l.substring(5).trim()
                }
                if (l == "[DONE]") break
                val contentToken = "\"content\":"
                val index = l.indexOf(contentToken)
                if (index != -1) {
                    val valQuoteStart = l.indexOf('"', index + contentToken.length)
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

    private fun getEndpointAndHeader(provider: ApiProvider, apiKey: String): Pair<String, String> {
        return "https://api.openai.com/v1/chat/completions" to "Bearer $apiKey"
    }
}

// --- 4. Anthropic Claude Provider Implementation ---
class ClaudeProvider : AiProvider {
    override suspend fun generateResponse(
        modelId: String,
        apiKey: String,
        systemPrompt: String,
        prompt: String,
        history: List<Message>,
        attachedBase64Image: String?,
        attachedDocText: String?
    ): String {
        val messages = mutableListOf<ClaudeMessage>()
        history.forEach { msg ->
            messages.add(ClaudeMessage(if (msg.role == "user") "user" else "assistant", msg.content))
        }
        messages.add(ClaudeMessage("user", prompt))
        val req = ClaudeRequest(model = modelId, messages = messages, system = systemPrompt, temperature = 0.7f)
        val response = NetworkClient.claudeService.generateMessage(apiKey = apiKey, request = req)
        return response.content?.firstOrNull()?.text ?: throw Exception("No text response generated.")
    }

    override fun generateResponseStream(
        modelId: String,
        apiKey: String,
        systemPrompt: String,
        prompt: String,
        history: List<Message>,
        attachedBase64Image: String?,
        attachedDocText: String?
    ): Flow<String> = flow {
        val messages = mutableListOf<ClaudeMessage>()
        history.forEach { msg ->
            messages.add(ClaudeMessage(if (msg.role == "user") "user" else "assistant", msg.content))
        }
        messages.add(ClaudeMessage("user", prompt))
        val req = ClaudeRequest(model = modelId, messages = messages, system = systemPrompt, temperature = 0.7f, stream = true)

        try {
            val responseBody = NetworkClient.claudeService.generateMessageStream(apiKey = apiKey, request = req)
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
                val index = l.indexOf(textToken)
                if (index != -1) {
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

// --- 5. Provider Factory ---
object AiProviderFactory {
    fun getProvider(provider: ApiProvider): AiProvider {
        return when (provider) {
            ApiProvider.NABIH,
            ApiProvider.GOOGLE -> GeminiProvider()
            
            ApiProvider.OPENAI -> OpenAiProvider(provider)
            
            ApiProvider.ANTHROPIC -> ClaudeProvider()
        }
    }
}

// --- 6. Unified Error Handling System ---

// --- 8. Unified Routing Engine ---
object AiRouter {
    fun routeStreaming(
        context: android.content.Context,
        registryModel: com.example.model.ModelMetadata,
        settings: com.example.model.AppSettings,
        systemPrompt: String,
        prompt: String,
        history: List<com.example.data.local.Message>,
        attachedBase64Image: String? = null,
        attachedDocText: String? = null,
        isArabic: Boolean = false
    ): Flow<String> = flow {
        val tStart = System.currentTimeMillis()
        android.util.Log.d("PerfDebug", "1. routeStreaming started at $tStart")
        val isNabihRequest = registryModel.provider == ApiProvider.NABIH || registryModel.id == "nabih-ultra"
        var actualProviderType = registryModel.provider
        var currentApiKey = ""
        var targetModelId = registryModel.id

        if (actualProviderType == ApiProvider.NABIH) {
            when {
                settings.nabihApiKey.isNotBlank() -> {
                    val key = settings.nabihApiKey.trim()
                    if (key.startsWith("sk-ant-")) {
                        actualProviderType = ApiProvider.ANTHROPIC
                        currentApiKey = key
                        targetModelId = "claude-3-7-sonnet-20250219"
                    } else if (key.startsWith("sk-")) {
                        actualProviderType = ApiProvider.OPENAI
                        currentApiKey = key
                        targetModelId = "gpt-4o"
                    } else {
                        actualProviderType = ApiProvider.GOOGLE
                        currentApiKey = key
                        targetModelId = "gemini-1.5-pro"
                    }
                }
                settings.googleApiKey.isNotBlank() -> {
                    actualProviderType = ApiProvider.GOOGLE
                    currentApiKey = settings.googleApiKey.trim()
                    targetModelId = "gemini-1.5-pro"
                }
                settings.openaiApiKey.isNotBlank() -> {
                    actualProviderType = ApiProvider.OPENAI
                    currentApiKey = settings.openaiApiKey.trim()
                    targetModelId = "gpt-4o"
                }
                settings.anthropicApiKey.isNotBlank() -> {
                    actualProviderType = ApiProvider.ANTHROPIC
                    currentApiKey = settings.anthropicApiKey.trim()
                    targetModelId = "claude-3-7-sonnet-20250219"
                }
                com.example.BuildConfig.GEMINI_API_KEY.isNotBlank() && 
                !com.example.BuildConfig.GEMINI_API_KEY.contains("YOUR_") &&
                !com.example.BuildConfig.GEMINI_API_KEY.contains("MY_GEMINI_") -> {
                    actualProviderType = ApiProvider.GOOGLE
                    currentApiKey = com.example.BuildConfig.GEMINI_API_KEY.trim()
                    targetModelId = "gemini-1.5-pro"
                }
            }
        } else {
            currentApiKey = when (actualProviderType) {
                ApiProvider.GOOGLE -> settings.googleApiKey.ifBlank { com.example.BuildConfig.GEMINI_API_KEY }.trim()
                ApiProvider.OPENAI -> settings.openaiApiKey.trim()
                ApiProvider.ANTHROPIC -> settings.anthropicApiKey.trim()
                else -> ""
            }
        }

        val isKeyValid = currentApiKey.isNotBlank() && 
                         !currentApiKey.contains("MY_GEMINI_API_KEY") && 
                         !currentApiKey.contains("YOUR_") && 
                         !currentApiKey.contains("PLACEHOLDER")

        if (!isKeyValid) {
            val missingKeyMsg = if (isArabic) {
                "لا يوجد مفتاح API مُفعّل حالياً. يرجى إضافة مفتاح من شاشة الإعدادات لاستخدام النماذج."
            } else {
                "No API Key is currently active. Please add an API key in Settings to use the models."
            }
            throw Exception(missingKeyMsg)
        }

        val isGeminiRequest = actualProviderType == ApiProvider.GOOGLE
        val modelsToTry = mutableListOf<String>()
        if (isGeminiRequest) {
            val candidates = listOf("gemini-2.5-flash", "gemini-2.0-flash", "gemini-1.5-flash", "gemini-1.5-pro", "gemini-2.0-flash-exp")
            if (targetModelId.isNotBlank() && targetModelId != "nabih-ultra" && targetModelId != "gemini") {
                modelsToTry.add(targetModelId)
            }
            candidates.forEach { cand ->
                if (!modelsToTry.contains(cand)) {
                    modelsToTry.add(cand)
                }
            }
        } else {
            val mappedModelId = when {
                actualProviderType == ApiProvider.OPENAI && (targetModelId.contains("gpt-5") || targetModelId == "chatgpt" || targetModelId == "gpt-4o" || targetModelId == "nabih-ultra") -> "gpt-4o"
                actualProviderType == ApiProvider.ANTHROPIC && (targetModelId.contains("claude-3-7") || targetModelId == "claude" || targetModelId == "nabih-ultra") -> "claude-3-7-sonnet-20250219"
                else -> targetModelId
            }
            modelsToTry.add(mappedModelId)
            
            if (actualProviderType == ApiProvider.OPENAI) {
                val fallbacks = listOf("gpt-4o", "gpt-4o-mini", "gpt-4-turbo", "gpt-4", "gpt-3.5-turbo")
                fallbacks.forEach { if (!modelsToTry.contains(it)) modelsToTry.add(it) }
            } else if (actualProviderType == ApiProvider.ANTHROPIC) {
                val fallbacks = listOf("claude-3-7-sonnet-20250219", "claude-3-5-sonnet-20241022", "claude-3-haiku-20240307")
                fallbacks.forEach { if (!modelsToTry.contains(it)) modelsToTry.add(it) }
            }
        }

        var success = false
        var lastException: Throwable? = null

        for (modelId in modelsToTry) {
            if (com.example.BuildConfig.DEBUG) android.util.Log.d("NabihUltraDebug", "Model name used: $modelId")
            val maxRetries = 2
            var currentTry = 0
            var modelSuccess = false

            while (currentTry < maxRetries && !modelSuccess) {
                try {
                    val provider = AiProviderFactory.getProvider(actualProviderType)
                    val chunkCollector = StringBuilder()
                    var validated = false
                    
                    val tReq = System.currentTimeMillis()
                    android.util.Log.d("PerfDebug", "2. Before provider.generateResponseStream. modelId=$modelId, try=$currentTry, ms since start=${tReq - tStart}")
                    var isFirstChunk = true

                    provider.generateResponseStream(
                        modelId = modelId,
                        apiKey = currentApiKey,
                        systemPrompt = systemPrompt,
                        prompt = prompt,
                        history = history,
                        attachedBase64Image = attachedBase64Image,
                        attachedDocText = attachedDocText
                    ).collect { chunk ->
                        if (isFirstChunk) {
                            val tFirstChunk = System.currentTimeMillis()
                            android.util.Log.d("PerfDebug", "3. First chunk received after ${tFirstChunk - tReq}ms")
                            isFirstChunk = false
                        }
                        chunkCollector.append(chunk)
                        val accumulated = chunkCollector.toString()
                        
                        if (!validated) {
                            if (accumulated.length > 20) {
                                val testStrLower = accumulated.trimStart().lowercase()
                                val isBad = testStrLower.startsWith("{") || testStrLower.startsWith("[") || 
                                            testStrLower.startsWith("exception:") || testStrLower.startsWith("error:") ||
                                            testStrLower.startsWith("api_error") ||
                                            testStrLower.contains("match result value") || testStrLower.contains("stack trace") || 
                                            testStrLower.contains("debug output") || testStrLower.contains("raw json parsing error")
                                if (isBad) {
                                    throw Exception("VALIDATION_FAILED: Bad response")
                                }
                                validated = true
                                emit(accumulated)
                            }
                        } else {
                            emit(accumulated)
                        }
                    }
                    
                    if (!validated) {
                        val finalStr = chunkCollector.toString().trim()
                        val lower = finalStr.lowercase()
                        if (finalStr.isEmpty() || lower == "null" || lower == "undefined" || lower.startsWith("{") || lower.startsWith("[") || lower.contains("match result value")) {
                            throw Exception("VALIDATION_FAILED: Empty or invalid response")
                        }
                        emit(finalStr)
                    }

                    modelSuccess = true
                    success = true
                } catch (e: Throwable) {
                    lastException = e
                    android.util.Log.e("AiRouter", "API Request failed on try $currentTry for model $modelId", e)

                    val isAuthError = e is HttpException && (e.code() == 401 || e.code() == 403)
                    if (isAuthError) {
                        break
                    }

                    currentTry++
                    if (currentTry < maxRetries) {
                        kotlinx.coroutines.delay(800L * currentTry)
                    }
                }
            }

            if (modelSuccess) {
                break
            }
        }

        if (!success) {
            val translatedError = lastException?.let { e -> 
                AiErrorTranslator.translate(throwable = e, isArabic = isArabic)
            } ?: (if (isArabic) "فشلت عملية الاتصال بمزود الذكاء الاصطناعي." else "Failed to connect to AI provider.")
            throw Exception(translatedError)
        }
    }
}

// --- Helper JSON String Unescaper ---
private fun unescapeJsonString(str: String): String {
    val builder = java.lang.StringBuilder()
    var i = 0
    while (i < str.length) {
        val c = str[i]
        if (c == '\\' && i + 1 < str.length) {
            val next = str[i + 1]
            when (next) {
                'n' -> builder.append('\n')
                'r' -> builder.append('\r')
                't' -> builder.append('\t')
                'b' -> builder.append('\b')
                'f' -> builder.append('\u000C')
                '"' -> builder.append('"')
                '\\' -> builder.append('\\')
                'u' -> {
                    if (i + 5 < str.length) {
                        try {
                            val code = str.substring(i + 2, i + 6).toInt(16)
                            builder.append(code.toChar())
                            i += 4
                        } catch (e: Exception) {
                            builder.append("\\u")
                        }
                    } else {
                        builder.append("\\u")
                    }
                }
                else -> builder.append(next)
            }
            i += 2
        } else {
            builder.append(c)
            i++
        }
    }
    return builder.toString()
}
