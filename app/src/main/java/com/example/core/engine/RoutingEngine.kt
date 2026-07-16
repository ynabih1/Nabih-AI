package com.example.core.engine

import com.example.core.model.ApiProvider
import com.example.core.model.ModelRegistry
import com.example.core.network.AiProvider
import com.example.core.network.ClaudeProvider
import com.example.core.network.GeminiProvider
import com.example.core.network.OpenAiProvider

/**
 * Routing Engine
 * Resolves the incoming request to the correct provider implementation based on Model Metadata.
 */
class RoutingEngine {
    private val providers = mutableMapOf<ApiProvider, AiProvider>()

    init {
        // Initialize Provider Architecture adapters
        providers[ApiProvider.GOOGLE] = GeminiProvider()
        providers[ApiProvider.OPENAI] = OpenAiProvider(ApiProvider.OPENAI)
        providers[ApiProvider.ANTHROPIC] = ClaudeProvider()
        providers[ApiProvider.NABIH] = GeminiProvider() // Assuming Nabih Ultra uses Gemini backend
    }

    fun route(context: android.content.Context, modelId: String): AiProvider {
        // Look up model metadata from the existing ModelRegistry
        val metadata = ModelRegistry.getModelById(context, modelId)
            ?: throw IllegalArgumentException("Model $modelId not found in ModelRegistry")
        
        return providers[metadata.provider]
            ?: throw IllegalStateException("Provider adapter for ${metadata.provider} not registered")
    }
}
