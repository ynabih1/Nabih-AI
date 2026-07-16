package com.example.core.engine

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import retrofit2.HttpException
import java.io.IOException

/**
 * Request Validation
 */
class DefaultRequestValidator : RequestValidator {
    override fun validate(request: UnifiedRequest): Result<Unit> {
        if (request.modelId.isBlank()) {
            return Result.failure(IllegalArgumentException("Model ID cannot be blank"))
        }
        if (request.apiKey.isBlank()) {
            return Result.failure(IllegalArgumentException("API Key is required for model ${request.modelId}"))
        }
        if (request.prompt.isBlank() && request.attachedBase64Image == null && request.attachedDocText == null) {
            return Result.failure(IllegalArgumentException("Request must contain some prompt or attachment"))
        }
        return Result.success(Unit)
    }
}

/**
 * Error Handling
 */
class DefaultErrorMapper : ErrorMapper {
    override fun map(error: Throwable): UnifiedResponse.Error {
        return when (error) {
            is HttpException -> {
                val code = error.code()
                val message = error.response()?.errorBody()?.string() ?: error.message()
                UnifiedResponse.Error("HTTP Error $code: $message", error)
            }
            is IOException -> UnifiedResponse.Error("Network error: Please check your connection", error)
            is IllegalArgumentException -> UnifiedResponse.Error("Validation error: ${error.message}", error)
            else -> UnifiedResponse.Error("An unexpected error occurred: ${error.message}", error)
        }
    }
}

/**
 * Application Layer / Execution Pipeline
 * Combines Validation, Routing, Execution, and Error Mapping into a unified flow.
 */
class ExecutionPipeline(
    private val routingEngine: RoutingEngine = RoutingEngine(),
    private val validator: RequestValidator = DefaultRequestValidator(),
    private val errorMapper: ErrorMapper = DefaultErrorMapper()
) : ProviderPort {

    override suspend fun execute(request: UnifiedRequest): UnifiedResponse {
        return try {
            validator.validate(request).getOrThrow()
            
            val provider = routingEngine.route(request.context, request.modelId)
            
            val resultText = provider.generateResponse(
                modelId = request.modelId,
                apiKey = request.apiKey,
                systemPrompt = request.systemPrompt,
                prompt = request.prompt,
                history = request.history,
                attachedBase64Image = request.attachedBase64Image,
                attachedDocText = request.attachedDocText
            )
            
            UnifiedResponse.Success(resultText)
        } catch (e: Throwable) {
            errorMapper.map(e)
        }
    }

    override fun executeStream(request: UnifiedRequest): Flow<String> = flow {
        try {
            validator.validate(request).getOrThrow()
            
            val provider = routingEngine.route(request.context, request.modelId)
            
            provider.generateResponseStream(
                modelId = request.modelId,
                apiKey = request.apiKey,
                systemPrompt = request.systemPrompt,
                prompt = request.prompt,
                history = request.history,
                attachedBase64Image = request.attachedBase64Image,
                attachedDocText = request.attachedDocText
            ).catch { e ->
                // Delegate to Error Handling
                val mappedError = errorMapper.map(e)
                throw RuntimeException(mappedError.message, e)
            }.collect { chunk ->
                emit(chunk)
            }
        } catch (e: Throwable) {
            val mappedError = errorMapper.map(e)
            throw RuntimeException(mappedError.message, e)
        }
    }
}
