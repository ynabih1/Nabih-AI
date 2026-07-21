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
        val parts = mutableListOf<GeminiPart>()
        parts.add(GeminiPart(text = prompt))
        if (attachedBase64Image != null) {
            parts.add(GeminiPart(inlineData = GeminiInlineData("image/jpeg", attachedBase64Image)))
        }
        val contents = mutableListOf<GeminiContent>()
        history.forEach { msg ->
            contents.add(GeminiContent(
                role = if (msg.role == "user") "user" else "model",
                parts = listOf(GeminiPart(text = msg.content))
            ))
        }
        contents.add(GeminiContent(role = "user", parts = parts))
        val req = GeminiRequest(
            contents = contents,
            systemInstruction = GeminiContent(role = "user", parts = listOf(GeminiPart(text = systemPrompt))),
            generationConfig = GeminiGenerationConfig(temperature = 0.7f)
        )
        val response = NetworkClient.geminiService.generateContent(modelId, apiKey, req)
        return response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            ?: throw Exception("No text generated")
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
        val parts = mutableListOf<GeminiPart>()
        parts.add(GeminiPart(text = prompt))
        if (attachedBase64Image != null) {
            parts.add(GeminiPart(inlineData = GeminiInlineData("image/jpeg", attachedBase64Image)))
        }
        val contents = mutableListOf<GeminiContent>()
        history.forEach { msg ->
            contents.add(GeminiContent(
                role = if (msg.role == "user") "user" else "model",
                parts = listOf(GeminiPart(text = msg.content))
            ))
        }
        contents.add(GeminiContent(role = "user", parts = parts))
        val req = GeminiRequest(
            contents = contents,
            systemInstruction = GeminiContent(role = "user", parts = listOf(GeminiPart(text = systemPrompt))),
            generationConfig = GeminiGenerationConfig(temperature = 0.7f)
        )

        try {
            val responseBody = NetworkClient.geminiService.generateContentStream(modelId, apiKey, req)
            val reader = responseBody.byteStream().bufferedReader()
            val stringBuilder = java.lang.StringBuilder()
            var accumulated = ""
            var previouslyProcessedBlocks = 0
            val buffer = CharArray(4096)
            var readChars: Int
            while (reader.read(buffer).also { readChars = it } != -1) {
                val chunk = String(buffer, 0, readChars)
                accumulated += chunk
                try {
                    val textToken = "\"text\":"
                    var startIndex = 0
                    var parsedBlocks = 0
                    while (true) {
                        val index = accumulated.indexOf(textToken, startIndex)
                        if (index == -1) break
                        val valStart = accumulated.indexOf('"', index + textToken.length)
                        if (valStart == -1) break
                        var valEnd = valStart + 1
                        var escaped = false
                        while (valEnd < accumulated.length) {
                            val c = accumulated[valEnd]
                            if (escaped) {
                                escaped = false
                            } else if (c == '\\') {
                                escaped = true
                            } else if (c == '"') {
                                break
                            }
                            valEnd++
                        }
                        if (valEnd < accumulated.length) {
                            val rawText = accumulated.substring(valStart + 1, valEnd)
                            val unescaped = unescapeJsonString(rawText)
                            if (parsedBlocks >= previouslyProcessedBlocks) {
                                stringBuilder.append(unescaped)
                                emit(unescaped)
                                previouslyProcessedBlocks++
                            }
                            parsedBlocks++
                        }
                        startIndex = index + textToken.length
                    }
                } catch (e: Exception) {
                    // Non-blocking JSON chunk parsing
                }
            }
            if (stringBuilder.isEmpty()) {
                val fallback = generateResponse(modelId, apiKey, systemPrompt, prompt, history, attachedBase64Image, attachedDocText)
                emit(fallback)
            }
        } catch (e: Exception) {
            // Fallback simulated typing stream
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
object AiErrorTranslator {
    fun mapApiErrorToUserMessage(code: Int, provider: String): String {
        val providerNameAr = when (provider.lowercase()) {
            "gemini", "google" -> "Gemini"
            "claude", "anthropic" -> "Claude"
            "chatgpt", "openai" -> "ChatGPT"
            "nabih", "nabih-ultra" -> "Nabih Ultra"
            else -> provider
        }
        return when (code) {
            400 -> "حدث خطأ في تنسيق الطلب، حاول مرة أخرى"
            401, 403 -> "مفتاح API غير صالح أو منتهي الصلاحية، تحقق من إعدادات المفاتيح"
            404 -> "الموديل المطلوب غير متاح حالياً، جرّب موديلاً آخر من القائمة"
            429 -> "تم تجاوز الحد المسموح من الطلبات لهذا المزود، حاول لاحقاً أو استخدم موديل آخر"
            500, 502, 503, 504 -> "خدمة [$providerNameAr] غير متاحة مؤقتاً، حاول مرة أخرى بعد قليل"
            -1 -> "استغرق الرد وقتاً أطول من المتوقع، تحقق من اتصالك وحاول مرة أخرى"
            else -> "حدث خطأ غير متوقع ($code) أثناء الاتصال بـ $providerNameAr، يرجى المحاولة لاحقاً."
        }
    }

    fun translate(throwable: Throwable, provider: String = "الذكاء الاصطناعي", isArabic: Boolean = false): String {
        if (!isArabic) {
            return when (throwable) {
                is SocketTimeoutException -> "Connection timed out. Please try again later."
                is UnknownHostException, is IOException -> "Network failure. Please check your internet connection and try again."
                is HttpException -> {
                    when (throwable.code()) {
                        400 -> "Bad request (400). Please check your input parameters or model configuration."
                        401, 403 -> "Unauthorized or invalid API Key (401/403). Please verify your key in Settings."
                        404 -> "Model not found or currently unavailable (404). Please select a different model from the list."
                        429 -> "Rate limit exceeded (429). Please wait a moment before trying again."
                        500, 502, 503, 504 -> "Service internal error (${throwable.code()}). Please try again later."
                        else -> "Unexpected provider error (${throwable.code()})."
                    }
                }
                else -> {
                    val msg = throwable.localizedMessage ?: throwable.message ?: ""
                    if (msg.contains("VALIDATION_FAILED")) {
                        "Sorry, I couldn't understand your request. Please try rephrasing your question."
                    } else {
                        msg.ifBlank { "An unknown error occurred." }
                    }
                }
            }
        }
        return when (throwable) {
            is SocketTimeoutException -> {
                mapApiErrorToUserMessage(-1, provider)
            }
            is UnknownHostException, is IOException -> {
                "فشل الاتصال بالإنترنت. يرجى التحقق من اتصالك والمحاولة مرة أخرى."
            }
            is HttpException -> {
                mapApiErrorToUserMessage(throwable.code(), provider)
            }
            else -> {
                val msg = throwable.localizedMessage ?: throwable.message ?: ""
                if (msg.contains("429")) {
                    mapApiErrorToUserMessage(429, provider)
                } else if (msg.contains("404")) {
                    mapApiErrorToUserMessage(404, provider)
                } else if (msg.contains("401") || msg.contains("403")) {
                    mapApiErrorToUserMessage(401, provider)
                } else if (msg.contains("400")) {
                    mapApiErrorToUserMessage(400, provider)
                } else if (msg.contains("500") || msg.contains("502") || msg.contains("503") || msg.contains("504")) {
                    mapApiErrorToUserMessage(500, provider)
                } else if (msg.contains("timeout") || msg.contains("Timeout") || msg.contains("SocketTimeout")) {
                    mapApiErrorToUserMessage(-1, provider)
                } else if (msg.contains("VALIDATION_FAILED")) {
                    "عذراً، لم أتمكن من فهم طلبك. يرجى إعادة صياغة السؤال."
                } else {
                    msg.ifBlank { "حدث خطأ غير معروف أثناء الاتصال بالمزود." }
                }
            }
        }
    }
}

// --- 7. Unified Routing Engine ---
object AiRouter {
    fun routeStreaming(
        context: android.content.Context,
        registryModel: ModelMetadata,
        settings: com.example.model.AppSettings,
        systemPrompt: String,
        prompt: String,
        history: List<Message>,
        attachedBase64Image: String? = null,
        attachedDocText: String? = null,
        isArabic: Boolean = false
    ): Flow<String> = flow {
        var actualProviderType = registryModel.provider
        var currentApiKey = ""
        var targetModelId = registryModel.id

        if (actualProviderType == ApiProvider.NABIH) {
            // Nabih Ultra logic: prioritize its own key, then find any available key!
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
                    currentApiKey = settings.googleApiKey
                    targetModelId = "gemini-1.5-pro"
                }
                settings.openaiApiKey.isNotBlank() -> {
                    actualProviderType = ApiProvider.OPENAI
                    currentApiKey = settings.openaiApiKey
                    targetModelId = "gpt-4o"
                }
                settings.anthropicApiKey.isNotBlank() -> {
                    actualProviderType = ApiProvider.ANTHROPIC
                    currentApiKey = settings.anthropicApiKey
                    targetModelId = "claude-3-7-sonnet-20250219"
                }
                com.example.BuildConfig.GEMINI_API_KEY.isNotBlank() && !com.example.BuildConfig.GEMINI_API_KEY.contains("YOUR_") -> {
                    actualProviderType = ApiProvider.GOOGLE
                    currentApiKey = com.example.BuildConfig.GEMINI_API_KEY
                    targetModelId = "gemini-1.5-pro"
                }
            }
        } else {
            currentApiKey = when (actualProviderType) {
                ApiProvider.GOOGLE -> settings.googleApiKey.ifBlank { com.example.BuildConfig.GEMINI_API_KEY }
                ApiProvider.OPENAI -> settings.openaiApiKey
                ApiProvider.ANTHROPIC -> settings.anthropicApiKey
                else -> ""
            }
        }

        val isGeminiRequest = actualProviderType == ApiProvider.GOOGLE
        if (currentApiKey.isBlank() || currentApiKey == "MY_GEMINI_API_KEY" || currentApiKey.contains("YOUR_") || currentApiKey.contains("PLACEHOLDER")) {
            val missingKeyMsg = if (isArabic) {
                "مفتاح API غير متوفر. يرجى إضافة مفتاح API صحيح في الإعدادات للاستمرار."
            } else {
                "API Key is missing or not configured correctly. Please add a valid API key in Settings."
            }
            throw Exception(missingKeyMsg)
        }

        val modelsToTry = mutableListOf<String>()
        if (isGeminiRequest) {
            if (targetModelId != "nabih-ultra" && targetModelId != "gemini") {
                modelsToTry.add(targetModelId)
            }
            val candidates = listOf("gemini-2.0-flash", "gemini-1.5-pro", "gemini-1.5-flash", "gemini-1.0-pro", "gemini-pro")
            candidates.forEach { cand ->
                if (cand != targetModelId && !modelsToTry.contains(cand)) {
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
            
            // Add fallbacks
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
            val maxRetries = 3
            var currentTry = 0
            var modelSuccess = false

            while (currentTry < maxRetries && !modelSuccess) {
                try {
                    val provider = AiProviderFactory.getProvider(actualProviderType)
                    val chunkCollector = StringBuilder()
                    var validated = false
                    
                    provider.generateResponseStream(
                        modelId = modelId,
                        apiKey = currentApiKey,
                        systemPrompt = systemPrompt,
                        prompt = prompt,
                        history = history,
                        attachedBase64Image = attachedBase64Image,
                        attachedDocText = attachedDocText
                    ).collect { chunk ->
                        chunkCollector.append(chunk)
                        val accumulated = chunkCollector.toString()
                        
                        if (!validated) {
                            if (accumulated.length > 25) {
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

                    val isAuthError = e is HttpException && (e.code() == 401 || e.code() == 403 || e.code() == 404)
                    if (isAuthError) {
                        break
                    }

                    currentTry++
                    if (currentTry < maxRetries) {
                        val isRateLimit = e.message?.contains("429") == true || (e is HttpException && e.code() == 429)
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
                break
            }
        }

        if (!success) {
            val translatedError = lastException?.let { e -> 
                var msg = AiErrorTranslator.translate(throwable = e, isArabic = isArabic)
                if (e is retrofit2.HttpException && e.code() == 404) {
                    msg = "$msg (Tried: ${modelsToTry.joinToString()})"
                }
                msg
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
