package com.example.models

import com.example.data.local.Message
import com.example.models.ApiProvider
import com.example.models.ModelMetadata
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import com.google.firebase.Firebase
import com.google.firebase.remoteconfig.remoteConfig
import kotlinx.coroutines.tasks.await

private var cachedGeminiModelName: String = "gemini-2.0-flash"

fun getCurrentGeminiModelName(): String {
    return cachedGeminiModelName
}

fun initRemoteConfigAsync() {
    try {
        val remoteConfig = Firebase.remoteConfig
        remoteConfig.setDefaultsAsync(mapOf("gemini_model_name" to "gemini-2.0-flash"))
        remoteConfig.fetchAndActivate().addOnSuccessListener {
            val fetched = remoteConfig.getString("gemini_model_name")
            if (fetched.isNotBlank()) {
                cachedGeminiModelName = fetched
            }
        }
    } catch (e: Exception) {
        android.util.Log.e("RemoteConfig", "Async init failed", e)
    }
}

// --- 1. Provider Interface ---
interface AiProvider {
    suspend fun generateResponse(
        modelId: String,
        apiKey: String,
        systemPrompt: String,
        prompt: String,
        history: List<Message>,
        attachedBase64Image: String? = null,
        attachedDocText: String? = null,
        attachedBase64Audio: String? = null,
        audioMimeType: String? = "audio/m4a"
    ): String

    fun generateResponseStream(
        modelId: String,
        apiKey: String,
        systemPrompt: String,
        prompt: String,
        history: List<Message>,
        attachedBase64Image: String? = null,
        attachedDocText: String? = null,
        attachedBase64Audio: String? = null,
        audioMimeType: String? = "audio/m4a"
    ): Flow<String>
}

// --- 2. Gemini / Nabih Ultra Provider Implementation ---
class GeminiProvider : AiProvider {
    override suspend fun generateResponse(
        modelId: String,
        apiKey: String,
        systemPrompt: String,
        prompt: String,
        history: List<Message>,
        attachedBase64Image: String?,
        attachedDocText: String?,
        attachedBase64Audio: String?,
        audioMimeType: String?
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
        if (attachedBase64Audio != null) {
            parts.add(GeminiPart(inlineData = GeminiInlineData(mimeType = audioMimeType ?: "audio/m4a", data = attachedBase64Audio)))
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
        attachedDocText: String?,
        attachedBase64Audio: String?,
        audioMimeType: String?
    ): Flow<String> = flow {
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
        if (attachedBase64Audio != null) {
            parts.add(GeminiPart(inlineData = GeminiInlineData(mimeType = audioMimeType ?: "audio/m4a", data = attachedBase64Audio)))
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
                val fallback = generateResponse(modelId, apiKey, systemPrompt, prompt, history, attachedBase64Image, attachedDocText, attachedBase64Audio, audioMimeType)
                emit(fallback)
            }
        } catch (e: Exception) {
            val isRateLimitOrAuth = (e is HttpException && (e.code() == 429 || e.code() == 401 || e.code() == 403)) || (e.message?.contains("429") == true)
            if (isRateLimitOrAuth) {
                throw e
            }
            try {
                val fallback = generateResponse(modelId, apiKey, systemPrompt, prompt, history, attachedBase64Image, attachedDocText, attachedBase64Audio, audioMimeType)
                var currentLen = 0
                val chunkSize = 4
                while (currentLen < fallback.length) {
                    val nextLen = (currentLen + chunkSize).coerceAtMost(fallback.length)
                    emit(fallback.substring(currentLen, nextLen))
                    currentLen = nextLen
                    kotlinx.coroutines.delay(12)
                }
            } catch (fallbackEx: Exception) {
                throw fallbackEx
            }
        }
    }
}

// --- 3. Provider Factory ---
object AiProviderFactory {
    fun getProvider(provider: ApiProvider = ApiProvider.NABIH): AiProvider {
        return GeminiProvider()
    }
}

// --- 4. Routing Engine for Nabih Ultra ---
object AiRouter {
    fun routeStreaming(
        context: android.content.Context,
        registryModel: com.example.models.ModelMetadata,
        settings: com.example.models.AppSettings,
        systemPrompt: String,
        prompt: String,
        history: List<com.example.data.local.Message>,
        attachedBase64Image: String? = null,
        attachedDocText: String? = null,
        attachedBase64Audio: String? = null,
        audioMimeType: String? = "audio/m4a",
        isArabic: Boolean = false
    ): Flow<String> = flow {
        val currentApiKey = settings.nabihApiKey
            .ifBlank { settings.googleApiKey }
            .ifBlank { com.example.BuildConfig.GEMINI_API_KEY }
            .trim()

        val isKeyValid = currentApiKey.isNotBlank() && 
                         !currentApiKey.contains("MY_GEMINI_API_KEY") && 
                         !currentApiKey.contains("YOUR_") && 
                         !currentApiKey.contains("PLACEHOLDER")

        if (!isKeyValid) {
            val missingKeyMsg = if (isArabic) {
                "لا يوجد مفتاح API مُفعّل حالياً. يرجى التحقق من إعدادات التطبيق."
            } else {
                "No API Key is currently active. Please check app configuration."
            }
            throw Exception(missingKeyMsg)
        }

        val defaultRemoteModel = getCurrentGeminiModelName()
        val modelsToTry = linkedSetOf(
            defaultRemoteModel,
            "gemini-2.0-flash",
            "gemini-2.5-flash",
            "gemini-1.5-flash",
            "gemini-1.5-pro",
            "gemini-3.7-flash"
        ).toList()

        var success = false
        var lastException: Throwable? = null

        for (modelId in modelsToTry) {
            if (com.example.BuildConfig.DEBUG) android.util.Log.d("NabihUltraDebug", "Model name used: $modelId")
            val maxRetries = 2
            var currentTry = 0
            var modelSuccess = false

            while (currentTry < maxRetries && !modelSuccess) {
                try {
                    val provider = AiProviderFactory.getProvider()
                    val chunkCollector = StringBuilder()
                    var validated = false

                    provider.generateResponseStream(
                        modelId = modelId,
                        apiKey = currentApiKey,
                        systemPrompt = systemPrompt,
                        prompt = prompt,
                        history = history,
                        attachedBase64Image = attachedBase64Image,
                        attachedDocText = attachedDocText,
                        attachedBase64Audio = attachedBase64Audio,
                        audioMimeType = audioMimeType
                    ).collect { chunk ->
                        chunkCollector.append(chunk)
                        val accumulated = chunkCollector.toString()
                        emit(accumulated)
                    }

                    val finalStr = chunkCollector.toString().trim()
                    val lower = finalStr.lowercase()
                    if (finalStr.isEmpty() || lower == "null" || lower == "undefined") {
                        throw Exception("VALIDATION_FAILED: Empty response")
                    }

                    modelSuccess = true
                    success = true
                } catch (e: Throwable) {
                    lastException = e
                    android.util.Log.e("AiRouter", "API Request failed on try $currentTry for model '$modelId'. Error: ${e.message}", e)

                    val isRateLimit = (e is HttpException && e.code() == 429) || (e.message?.contains("429") == true)
                    val isAuthError = e is HttpException && (e.code() == 401 || e.code() == 403)
                    val isNotFound = (e is HttpException && e.code() == 404) || (e.message?.contains("404") == true)

                    if (isAuthError || isNotFound) {
                        break
                    }

                    currentTry++
                    if (currentTry < maxRetries) {
                        val delayTime = if (isRateLimit) 1500L * currentTry else 600L * currentTry
                        kotlinx.coroutines.delay(delayTime)
                    }
                }
            }

            if (modelSuccess) {
                break
            }
            val lastIsAuth = lastException?.let { it is HttpException && (it.code() == 401 || it.code() == 403) } ?: false
            if (lastIsAuth) {
                break
            }
        }

        if (!success) {
            val translatedError = lastException?.let { e -> 
                AiErrorTranslator.translate(throwable = e, isArabic = isArabic)
            } ?: (if (isArabic) "فشلت عملية الاتصال بـ Nabih Ultra." else "Failed to connect to Nabih Ultra.")
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
