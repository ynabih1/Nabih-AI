package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

// --- Gemini Models ---

data class GeminiPart(
    val text: String? = null,
    @Json(name = "inlineData") val inlineData: GeminiInlineData? = null
)

data class GeminiInlineData(
    @Json(name = "mimeType") val mimeType: String,
    val data: String
)

data class GeminiContent(
    val role: String? = null,
    val parts: List<GeminiPart>
)

data class GeminiRequest(
    val contents: List<GeminiContent>,
    val systemInstruction: GeminiContent? = null,
    val generationConfig: GeminiGenerationConfig? = null
)

data class GeminiImageConfig(
    val aspectRatio: String? = null,
    val imageSize: String? = null
)

data class GeminiGenerationConfig(
    val temperature: Float? = null,
    val maxOutputTokens: Int? = null,
    val responseModalities: List<String>? = null,
    val imageConfig: GeminiImageConfig? = null
)

data class GeminiResponse(
    val candidates: List<GeminiCandidate>?
)

data class GeminiCandidate(
    val content: GeminiContent?
)

data class GeminiModel(
    val name: String,
    val version: String? = null,
    val displayName: String? = null,
    val description: String? = null,
    val supportedGenerationMethods: List<String>? = null
)

data class GeminiListModelsResponse(
    val models: List<GeminiModel>? = null
)

// --- OpenAI Models ---

data class OpenAiMessage(
    val role: String,
    val content: Any // Can be String or List of Content Parts for multimodal
)

data class OpenAiRequest(
    val model: String,
    val messages: List<OpenAiMessage>,
    val temperature: Float = 0.7f,
    val stream: Boolean = false
)

data class OpenAiChoice(
    val message: OpenAiMessageContent?
)

data class OpenAiMessageContent(
    val role: String?,
    val content: String?
)

data class OpenAiResponse(
    val choices: List<OpenAiChoice>?
)

// --- Anthropic Claude Models ---

data class ClaudeMessage(
    val role: String,
    val content: String
)

data class ClaudeRequest(
    val model: String,
    val messages: List<ClaudeMessage>,
    val max_tokens: Int = 4000,
    val system: String? = null,
    val temperature: Float = 0.7f,
    val stream: Boolean = false
)

data class ClaudeContentPart(
    val type: String,
    val text: String?
)

data class ClaudeResponse(
    val content: List<ClaudeContentPart>?
)

// --- Wikipedia Models ---
data class WikipediaSearchResponse(
    val query: WikipediaQuery?
)

data class WikipediaQuery(
    val search: List<WikipediaSearchResult>?
)

data class WikipediaSearchResult(
    val title: String,
    val snippet: String
)

// --- API Service Interfaces ---

interface WikipediaApiService {
    @GET("w/api.php?action=query&list=search&utf8=&format=json")
    suspend fun searchWikipedia(
        @Query("srsearch") query: String
    ): WikipediaSearchResponse
}

interface GeminiApiService {
    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String,
        @Header("x-goog-api-key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse

    @POST("v1beta/models/{model}:streamGenerateContent")
    @Streaming
    suspend fun generateContentStream(
        @Path("model") model: String,
        @Header("x-goog-api-key") apiKey: String,
        @Body request: GeminiRequest
    ): ResponseBody

    @GET("v1beta/models")
    suspend fun listModels(
        @Header("x-goog-api-key") apiKey: String
    ): GeminiListModelsResponse
}

interface OpenAiApiService {
    @POST
    suspend fun generateCompletion(
        @Url url: String,
        @Header("Authorization") authorization: String,
        @Body request: OpenAiRequest
    ): OpenAiResponse

    @POST
    @Streaming
    suspend fun generateCompletionStream(
        @Url url: String,
        @Header("Authorization") authorization: String,
        @Body request: OpenAiRequest
    ): ResponseBody
}

interface ClaudeApiService {
    @POST("v1/messages")
    suspend fun generateMessage(
        @Header("x-api-key") apiKey: String,
        @Header("anthropic-version") anthropicVersion: String = "2023-06-01",
        @Header("content-type") contentType: String = "application/json",
        @Body request: ClaudeRequest
    ): ClaudeResponse

    @POST("v1/messages")
    @Streaming
    suspend fun generateMessageStream(
        @Header("x-api-key") apiKey: String,
        @Header("anthropic-version") anthropicVersion: String = "2023-06-01",
        @Header("content-type") contentType: String = "application/json",
        @Body request: ClaudeRequest
    ): ResponseBody
}

// --- Retrofit Network Client ---

object NetworkClient {
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
        redactHeader("Authorization")
        redactHeader("x-api-key")
        redactHeader("x-goog-api-key")
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(loggingInterceptor)
        .build()

    val geminiService: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://generativelanguage.googleapis.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApiService::class.java)
    }

    val openAiService: OpenAiApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.openai.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(OpenAiApiService::class.java)
    }

    val claudeService: ClaudeApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.anthropic.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(ClaudeApiService::class.java)
    }

    val wikipediaService: WikipediaApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://en.wikipedia.org/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(WikipediaApiService::class.java)
    }
}
