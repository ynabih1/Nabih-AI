package com.example.core.model

enum class ApiProvider(val displayName: String) {
    NABIH("Nabih AI"),
    GOOGLE("Google Gemini"),
    OPENAI("OpenAI ChatGPT"),
    ANTHROPIC("Anthropic Claude"),
    GROK("xAI Grok"),
    DEEPSEEK("DeepSeek"),
    MISTRAL("Mistral AI"),
    OPENROUTER("OpenRouter"),
    OLLAMA("Ollama"),
    LMSTUDIO("LM Studio")
}

enum class AiModel(
    val id: String,
    val displayName: String,
    val provider: ApiProvider,
    val description: String,
    val supportsImages: Boolean = true,
    val supportsDocuments: Boolean = true
) {
    NABIH_ULTRA(
        id = "nabih-ultra",
        displayName = "Nabih Ultra",
        provider = ApiProvider.NABIH,
        description = "Nabih AI's native flagship model. Fast, accurate, and completely free to use.",
        supportsImages = true,
        supportsDocuments = true
    ),
    GEMINI(
        id = "gemini-2.5-pro",
        displayName = "Gemini",
        provider = ApiProvider.GOOGLE,
        description = "Google's flagship model for advanced reasoning, coding, and complex tasks.",
        supportsImages = true,
        supportsDocuments = true
    ),
    CHATGPT(
        id = "gpt-5",
        displayName = "ChatGPT",
        provider = ApiProvider.OPENAI,
        description = "OpenAI's latest next-generation frontier intelligence model.",
        supportsImages = true,
        supportsDocuments = true
    ),
    CLAUDE(
        id = "claude-3-7-sonnet",
        displayName = "Claude",
        provider = ApiProvider.ANTHROPIC,
        description = "Anthropic's flagship state-of-the-art model with hybrid thinking.",
        supportsImages = true,
        supportsDocuments = true
    );

    companion object {
        fun fromId(id: String): AiModel {
            return values().find { it.id == id } ?: NABIH_ULTRA
        }
    }
}

enum class AppTheme {
    SYSTEM, LIGHT, DARK
}

enum class AppLanguage(val code: String, val displayName: String) {
    ENGLISH("en", "English"),
    ARABIC("ar", "العربية")
}


enum class ResponseStyle(val displayName: String) {
    FAST("Fast"),
    BALANCED("Balanced"),
    DETAILED("Detailed")
}
enum class FontSize(val scale: Float, val displayName: String) {
    SMALL(0.85f, "Small"),
    MEDIUM(1.0f, "Medium"),
    LARGE(1.2f, "Large")
}

data class AppSettings(
    val theme: AppTheme = AppTheme.LIGHT, // white background, flat design is default
    val language: AppLanguage = AppLanguage.ENGLISH,
    val fontSize: FontSize = FontSize.MEDIUM,
    val defaultModel: AiModel = AiModel.NABIH_ULTRA,
    val voiceEnabled: Boolean = true,
    val hapticFeedback: Boolean = true,
    val nabihApiKey: String = "",
    val googleApiKey: String = "",
    val openaiApiKey: String = "",
    val anthropicApiKey: String = "",
    val grokApiKey: String = "",
    val deepseekApiKey: String = "",
    val mistralApiKey: String = "",
    val openRouterApiKey: String = "",
    val ollamaEndpoint: String = "",
    val lmStudioEndpoint: String = "",
    val isLoggedIn: Boolean = false,
    val authType: String = "", // "GUEST", "GOOGLE", "MICROSOFT", "EMAIL", "PASSKEY"
    val userEmail: String = "",
    val userName: String = "",
    val profilePictureUri: String = "",
    val personalInfo: String = "",
    val microsoftEmail: String = "",
    val microsoftName: String = "",
    val googleEmail: String = "",
    val googleName: String = "",
    val biometricsEnabled: Boolean = false,
    val responseStyle: ResponseStyle = ResponseStyle.BALANCED,
    val memoryEnabled: Boolean = true,
    val saveHistory: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val completionNotifications: Boolean = true
)
