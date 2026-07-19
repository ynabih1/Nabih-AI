package com.example.core.network

import com.example.core.database.Message
import com.example.core.model.ApiProvider
import com.example.core.model.ModelMetadata
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
            var line: String?
            val stringBuilder = java.lang.StringBuilder()
            var accumulated = ""
            while (reader.readLine().also { line = it } != null) {
                val l = line?.trim() ?: continue
                if (l.isEmpty()) continue
                accumulated += l
                try {
                    val textToken = "\"text\":"
                    var startIndex = 0
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
                            if (unescaped.length > stringBuilder.length) {
                                val diff = unescaped.substring(stringBuilder.length)
                                stringBuilder.append(diff)
                                emit(diff)
                            }
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
                        429 -> "Rate limit exceeded (429). Please wait a moment before trying again."
                        500, 502, 503, 504 -> "Service internal error (${throwable.code()}). Please try again later."
                        else -> "Unexpected provider error (${throwable.code()})."
                    }
                }
                else -> throwable.localizedMessage ?: throwable.message ?: "An unknown error occurred."
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
                } else if (msg.contains("401") || msg.contains("403")) {
                    mapApiErrorToUserMessage(401, provider)
                } else if (msg.contains("400")) {
                    mapApiErrorToUserMessage(400, provider)
                } else if (msg.contains("500") || msg.contains("502") || msg.contains("503") || msg.contains("504")) {
                    mapApiErrorToUserMessage(500, provider)
                } else if (msg.contains("timeout") || msg.contains("Timeout") || msg.contains("SocketTimeout")) {
                    mapApiErrorToUserMessage(-1, provider)
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
        settings: com.example.core.model.AppSettings,
        systemPrompt: String,
        prompt: String,
        history: List<Message>,
        attachedBase64Image: String? = null,
        attachedDocText: String? = null,
        isArabic: Boolean = false
    ): Flow<String> = flow {
        val providerType = registryModel.provider
        val currentApiKey = when (providerType) {
            ApiProvider.NABIH -> settings.nabihApiKey.ifBlank { com.example.BuildConfig.GEMINI_API_KEY }
            ApiProvider.GOOGLE -> settings.googleApiKey.ifBlank { com.example.BuildConfig.GEMINI_API_KEY }
            ApiProvider.OPENAI -> settings.openaiApiKey
            ApiProvider.ANTHROPIC -> settings.anthropicApiKey
        }

        val isGeminiRequest = providerType == ApiProvider.GOOGLE || providerType == ApiProvider.NABIH
        if (isGeminiRequest && (currentApiKey.isBlank() || currentApiKey == "MY_GEMINI_API_KEY" || currentApiKey.contains("YOUR_") || currentApiKey.contains("PLACEHOLDER"))) {
            val missingKeyMsg = if (isArabic) {
                "لم يتم تكوين مفتاح Gemini بشكل صحيح، يرجى مراجعة إعدادات API"
            } else {
                "Gemini API key is not configured correctly, please check your API settings."
            }
            throw Exception(missingKeyMsg)
        }

        if (currentApiKey.isBlank() && !isGeminiRequest) {
            val missingKeyMsg = if (isArabic) {
                "مفتاح API مطلوب: مفتاح ${providerType.displayName} غير موجود. يرجى إضافته في الإعدادات."
            } else {
                "API Key Required: ${providerType.displayName} API Key is missing. Please add it in Settings."
            }
            throw Exception(missingKeyMsg)
        }

        val modelsToTry = mutableListOf<String>()
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

        var success = false
        var lastException: Throwable? = null

        for (modelId in modelsToTry) {
            val maxRetries = 3
            var currentTry = 0
            var modelSuccess = false

            while (currentTry < maxRetries && !modelSuccess) {
                try {
                    val provider = AiProviderFactory.getProvider(providerType)
                    val chunkCollector = mutableListOf<String>()
                    
                    provider.generateResponseStream(
                        modelId = modelId,
                        apiKey = currentApiKey,
                        systemPrompt = systemPrompt,
                        prompt = prompt,
                        history = history,
                        attachedBase64Image = attachedBase64Image,
                        attachedDocText = attachedDocText
                    ).collect { chunk ->
                        chunkCollector.add(chunk)
                        emit(chunkCollector.joinToString(""))
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
            val translatedError = lastException?.let { AiErrorTranslator.translate(throwable = it, isArabic = isArabic) }
                ?: (if (isArabic) "فشلت عملية الاتصال بمزود الذكاء الاصطناعي." else "Failed to connect to AI provider.")
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
