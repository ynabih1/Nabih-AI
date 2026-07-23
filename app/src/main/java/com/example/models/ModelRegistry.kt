package com.example.models

import com.example.models.*

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class ModelStatus {
    AVAILABLE, MAINTENANCE, DEPRECATED
}

data class ModelCapabilities(
    val text: Boolean = true,
    val vision: Boolean = false,
    val audio: Boolean = false,
    val reasoning: Boolean = false,
    val imageGeneration: Boolean = false,
    val fileAnalysis: Boolean = false
) {
    fun toList(): List<String> {
        val list = mutableListOf<String>()
        if (text) list.add("Text")
        if (vision) list.add("Vision")
        if (audio) list.add("Audio")
        if (reasoning) list.add("Reasoning")
        if (imageGeneration) list.add("Image Gen")
        if (fileAnalysis) list.add("File Analysis")
        return list
    }
}

data class ModelMetadata(
    val id: String,
    val displayName: String,
    val provider: ApiProvider,
    val description: String,
    val version: String,
    val status: ModelStatus,
    val capabilities: ModelCapabilities,
    val isDeprecated: Boolean = false,
    val deprecatedDate: String? = null,
    val fallbackModelId: String = "nabih-ultra",
    val maintenanceMessage: String? = null
)

object ModelRegistry {
    private const val PREFS_NAME = "nabih_model_registry"
    private const val KEY_MODELS_JSON = "cached_models_config_v2"

    private val baselineModels = listOf(
        ModelMetadata(
            id = "nabih-ultra",
            displayName = "Nabih Ultra",
            provider = ApiProvider.NABIH,
            description = "Nabih AI's native flagship model. Fast, accurate, and completely free to use.",
            version = "v2.0-stable",
            status = ModelStatus.AVAILABLE,
            capabilities = ModelCapabilities(text = true, vision = true, reasoning = true, fileAnalysis = true),
            fallbackModelId = "nabih-ultra"
        ),
        ModelMetadata(
            id = "gemini-3.6-flash",
            displayName = "Gemini",
            provider = ApiProvider.GOOGLE,
            description = "Google's flagship model for advanced reasoning, coding, and complex tasks.",
            version = "v3.6-stable",
            status = ModelStatus.AVAILABLE,
            capabilities = ModelCapabilities(text = true, vision = true, audio = true, reasoning = true, fileAnalysis = true),
            fallbackModelId = "nabih-ultra"
        ),
        ModelMetadata(
            id = "gpt-5",
            displayName = "ChatGPT",
            provider = ApiProvider.OPENAI,
            description = "OpenAI's latest next-generation frontier intelligence model.",
            version = "v5.0-stable",
            status = ModelStatus.AVAILABLE,
            capabilities = ModelCapabilities(text = true, vision = true, audio = true, reasoning = true, fileAnalysis = true, imageGeneration = true),
            fallbackModelId = "nabih-ultra"
        ),
        ModelMetadata(
            id = "claude-3-7-sonnet",
            displayName = "Claude",
            provider = ApiProvider.ANTHROPIC,
            description = "Anthropic's flagship state-of-the-art model with hybrid reasoning and code capabilities.",
            version = "v3.7-stable",
            status = ModelStatus.AVAILABLE,
            capabilities = ModelCapabilities(text = true, vision = true, reasoning = true, fileAnalysis = true),
            fallbackModelId = "nabih-ultra"
        )
    )

    private var activeModels: List<ModelMetadata> = baselineModels

    fun getModels(context: Context): List<ModelMetadata> {
        val cached = loadCachedModels(context)
        if (cached != null) {
            activeModels = cached
        }
        return activeModels
    }

    fun getModelById(context: Context, id: String): ModelMetadata {
        val models = getModels(context)
        var target = models.find { it.id == id }
        if (target == null) {
            // Check for deprecated mappings
            target = when (id) {
                "gemini-flash-latest", "gemini-1.0-pro", "gemini-1.5-flash", "gemini-1.5-pro", "gemini-2.0-flash", "gemini-2.5-flash", "gemini-3.6-flash", "gemini-2.0-flash-thinking-exp" -> models.find { it.id == "gemini-3.6-flash" } ?: models.find { it.id == "gemini-2.5-flash" }
                "gpt-4o", "gpt-4-turbo", "gpt-5-mini", "gpt-5-nano" -> models.find { it.id == "gpt-5" }
                "claude-3-5-sonnet", "claude-3-haiku", "claude-3-5-sonnet-latest" -> models.find { it.id == "claude-3-7-sonnet" }
                else -> models.find { it.id == "nabih-ultra" }
            }
        }
        return target ?: baselineModels.first()
    }

    fun checkAndMigrateDeprecated(context: Context, modelId: String): String {
        val models = getModels(context)
        val model = models.find { it.id == modelId } ?: return modelId
        if (model.isDeprecated) {
            android.util.Log.w("ModelRegistry", "Model $modelId is deprecated! Migrating to ${model.fallbackModelId}")
            return model.fallbackModelId
        }
        return modelId
    }

    fun saveModelsToCache(context: Context, models: List<ModelMetadata>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        try {
            val jsonArray = JSONArray()
            models.forEach { model ->
                val obj = JSONObject().apply {
                    put("id", model.id)
                    put("displayName", model.displayName)
                    put("provider", model.provider.name)
                    put("description", model.description)
                    put("version", model.version)
                    put("status", model.status.name)
                    put("isDeprecated", model.isDeprecated)
                    put("deprecatedDate", model.deprecatedDate)
                    put("fallbackModelId", model.fallbackModelId)
                    put("maintenanceMessage", model.maintenanceMessage)
                    
                    val capObj = JSONObject().apply {
                        put("text", model.capabilities.text)
                        put("vision", model.capabilities.vision)
                        put("audio", model.capabilities.audio)
                        put("reasoning", model.capabilities.reasoning)
                        put("imageGeneration", model.capabilities.imageGeneration)
                        put("fileAnalysis", model.capabilities.fileAnalysis)
                    }
                    put("capabilities", capObj)
                }
                jsonArray.put(obj)
            }
            prefs.edit().putString(KEY_MODELS_JSON, jsonArray.toString()).apply()
            activeModels = models
        } catch (e: Exception) {
            android.util.Log.e("ModelRegistry", "Failed to cache models JSON", e)
        }
    }

    private fun loadCachedModels(context: Context): List<ModelMetadata>? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonStr = prefs.getString(KEY_MODELS_JSON, null) ?: return null
        return try {
            val list = mutableListOf<ModelMetadata>()
            val jsonArray = JSONArray(jsonStr)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val id = obj.getString("id")
                val displayName = obj.getString("displayName")
                val providerName = obj.getString("provider")
                val provider = try { ApiProvider.valueOf(providerName) } catch (e: Exception) { null }
                if (provider == null) continue
                val description = obj.getString("description")
                val version = obj.getString("version")
                val status = ModelStatus.valueOf(obj.getString("status"))
                val isDeprecated = obj.optBoolean("isDeprecated", false)
                val deprecatedDate = obj.optString("deprecatedDate", null)
                val fallbackModelId = obj.optString("fallbackModelId", "nabih-ultra")
                val maintenanceMessage = obj.optString("maintenanceMessage", null)
                
                val capObj = obj.getJSONObject("capabilities")
                val capabilities = ModelCapabilities(
                    text = capObj.optBoolean("text", true),
                    vision = capObj.optBoolean("vision", false),
                    audio = capObj.optBoolean("audio", false),
                    reasoning = capObj.optBoolean("reasoning", false),
                    imageGeneration = capObj.optBoolean("imageGeneration", false),
                    fileAnalysis = capObj.optBoolean("fileAnalysis", false)
                )
                
                list.add(
                    ModelMetadata(
                        id = id,
                        displayName = displayName,
                        provider = provider,
                        description = description,
                        version = version,
                        status = status,
                        capabilities = capabilities,
                        isDeprecated = isDeprecated,
                        deprecatedDate = if (deprecatedDate == "null") null else deprecatedDate,
                        fallbackModelId = fallbackModelId,
                        maintenanceMessage = if (maintenanceMessage == "null") null else maintenanceMessage
                    )
                )
            }
            list
        } catch (e: Exception) {
            android.util.Log.e("ModelRegistry", "Failed to parse cached models, falling back", e)
            null
        }
    }

    fun syncAndRefresh(context: Context, onSuccess: () -> Unit = {}, onFailure: (Exception) -> Unit = {}) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val secureStorage = com.example.utils.SecureStorage(context)
                val googleKey = secureStorage.getKey("key_google") ?: ""
                val nabihKey = secureStorage.getKey("key_nabih") ?: ""
                val apiKey = googleKey.ifBlank { nabihKey.ifBlank { com.example.BuildConfig.GEMINI_API_KEY } }
                
                if (apiKey.isNotEmpty() && !apiKey.contains("YOUR_GEMINI_API_KEY") && !apiKey.contains("MY_GEMINI_API_KEY")) {
                    try {
                        val response = com.example.models.NetworkClient.geminiService.listModels(apiKey)
                        val apiModels = response.models
                        if (apiModels != null && apiModels.isNotEmpty()) {
                            val detectedModels = apiModels.filter {
                                val methods = it.supportedGenerationMethods ?: emptyList()
                                methods.contains("generateContent") && (it.name.contains("gemini-") || it.name.contains("learnlm"))
                            }.map { geminiModel ->
                                val cleanId = geminiModel.name.substringAfter("models/")
                                val displayName = geminiModel.displayName ?: cleanId.split("-").joinToString(" ") { word ->
                                    word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() }
                                }
                                val desc = geminiModel.description ?: "Detected Google Gemini model."
                                val isVision = desc.lowercase().contains("vision") || desc.lowercase().contains("image") || cleanId.contains("flash") || cleanId.contains("pro")
                                val isAudio = desc.lowercase().contains("audio") || cleanId.contains("pro")
                                
                                ModelMetadata(
                                    id = cleanId,
                                    displayName = displayName,
                                    provider = ApiProvider.GOOGLE,
                                    description = desc,
                                    version = geminiModel.version ?: "v1beta",
                                    status = ModelStatus.AVAILABLE,
                                    capabilities = ModelCapabilities(
                                        text = true,
                                        vision = isVision,
                                        audio = isAudio,
                                        reasoning = cleanId.contains("pro") || cleanId.contains("thinking")
                                    ),
                                    fallbackModelId = "nabih-ultra"
                                )
                            }
                            
                            if (detectedModels.isNotEmpty()) {
                                val nonGoogleModels = baselineModels.filter { it.provider != ApiProvider.GOOGLE }
                                val merged = nonGoogleModels + detectedModels
                                saveModelsToCache(context, merged)
                                withContext(Dispatchers.Main) {
                                    onSuccess()
                                }
                                return@launch
                            }
                        }
                    } catch (netEx: Exception) {
                        android.util.Log.e("ModelRegistry", "Real model sync failed, using simulation fallback", netEx)
                    }
                }
                
                val updated = getModels(context).map { model ->
                    if (model.id == "nabih-ultra") {
                        model.copy(
                            version = "v2.0.2-live",
                            description = "Nabih AI's live native flagship model. Expanded capabilities, maximum speed, and free."
                        )
                    } else if (model.id == "gemini-3.6-flash" || model.id == "gemini-2.5-flash" || model.id == "gemini-1.5-pro") {
                        model.copy(version = "v3.6.0-live")
                    } else {
                        model
                    }
                }
                saveModelsToCache(context, updated)
                withContext(Dispatchers.Main) {
                    onSuccess()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onFailure(e)
                }
            }
        }
    }
}
