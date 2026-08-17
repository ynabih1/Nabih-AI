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
            description = "النموذج الافتراضي المعتمد عبر Firebase AI SDK",
            version = "v2.0-stable",
            status = ModelStatus.AVAILABLE,
            capabilities = ModelCapabilities(text = true, vision = true, reasoning = true, fileAnalysis = true),
            fallbackModelId = "nabih-ultra"
        )
    )

    private var activeModels: List<ModelMetadata> = baselineModels

    fun getModels(context: Context): List<ModelMetadata> {
        return baselineModels
    }

    fun getModelById(context: Context, id: String): ModelMetadata {
        return baselineModels.first()
    }

    fun checkAndMigrateDeprecated(context: Context, modelId: String): String {
        return "nabih-ultra"
    }

    fun saveModelsToCache(context: Context, models: List<ModelMetadata>) {}

    fun syncAndRefresh(context: Context, onSuccess: () -> Unit = {}, onFailure: (Exception) -> Unit = {}) {
        onSuccess()
    }
}
