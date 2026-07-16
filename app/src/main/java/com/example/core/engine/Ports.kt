package com.example.core.engine

import kotlinx.coroutines.flow.Flow
import com.example.core.database.Message

/**
 * Core Domain Models for the Engine
 */
data class UnifiedRequest(
    val context: android.content.Context, val modelId: String,
    val systemPrompt: String,
    val prompt: String,
    val history: List<Message>,
    val attachedBase64Image: String? = null,
    val attachedDocText: String? = null,
    val apiKey: String,
    val temperature: Float = 0.7f
)

sealed class UnifiedResponse {
    data class Success(val text: String) : UnifiedResponse()
    data class Error(val message: String, val cause: Throwable? = null) : UnifiedResponse()
}

/**
 * Ports / Interfaces
 * Represents the boundary between the application layer and external providers.
 */
interface ProviderPort {
    suspend fun execute(request: UnifiedRequest): UnifiedResponse
    fun executeStream(request: UnifiedRequest): Flow<String>
}

interface RequestValidator {
    fun validate(request: UnifiedRequest): Result<Unit>
}

interface ErrorMapper {
    fun map(error: Throwable): UnifiedResponse.Error
}
