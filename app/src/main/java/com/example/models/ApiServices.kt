package com.example.models

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

data class GeminiTool(
    @Json(name = "google_search") val google_search: Map<String, String>? = null,
    val googleSearch: Map<String, String> = emptyMap()
)

data class GeminiRequest(
    val contents: List<GeminiContent>,
    val systemInstruction: GeminiContent? = null,
    val generationConfig: GeminiGenerationConfig? = null,
    val tools: List<GeminiTool>? = null
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

data class GeminiGroundingChunkWeb(
    val uri: String? = null,
    val title: String? = null
)

data class GeminiGroundingChunk(
    val web: GeminiGroundingChunkWeb? = null
)

data class GeminiGroundingMetadata(
    val webSearchQueries: List<String>? = null,
    val searchEntryPoint: Map<String, Any>? = null,
    val groundingChunks: List<GeminiGroundingChunk>? = null
)

data class GeminiResponse(
    val candidates: List<GeminiCandidate>?
)

data class GeminiCandidate(
    val content: GeminiContent?,
    val groundingMetadata: GeminiGroundingMetadata? = null
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
        @Query("key") apiKey: String,
        @Body request: GeminiRequest,
        @Header("x-goog-api-key") headerKey: String = apiKey
    ): GeminiResponse

    @POST("v1beta/models/{model}:streamGenerateContent?alt=sse")
    @Streaming
    suspend fun generateContentStream(
        @Path("model") model: String,
        @Query("key") apiKey: String,
        @Body request: GeminiRequest,
        @Header("x-goog-api-key") headerKey: String = apiKey
    ): ResponseBody

    @GET("v1beta/models")
    suspend fun listModels(
        @Query("key") apiKey: String,
        @Header("x-goog-api-key") headerKey: String = apiKey
    ): GeminiListModelsResponse
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
        .addInterceptor { chain ->
            val request = chain.request()
            if (com.example.BuildConfig.DEBUG) {
                android.util.Log.d("NabihUltraDebug", "Full request URL: ${request.url}")
                val buffer = okio.Buffer()
                request.body?.writeTo(buffer)
                android.util.Log.d("NabihUltraDebug", "Request body: ${buffer.readUtf8()}")
            }
            val response = chain.proceed(request)
            if (com.example.BuildConfig.DEBUG) {
                android.util.Log.d("NabihUltraDebug", "Response code: ${response.code}")
                val responseBody = response.peekBody(Long.MAX_VALUE)
                android.util.Log.d("NabihUltraDebug", "Response body: ${responseBody.string()}")
            }
            response
        }
        .build()

    val geminiService: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://generativelanguage.googleapis.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApiService::class.java)
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
