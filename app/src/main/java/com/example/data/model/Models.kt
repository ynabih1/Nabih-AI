package com.example.data.model

enum class ApiProvider(val displayName: String) {
    NABIH("Nabih AI"),
    GOOGLE("Google"),
    OPENAI("OpenAI"),
    ANTHROPIC("Anthropic")
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
    GEMINI_FLASH(
        id = "gemini-3.5-flash",
        displayName = "Gemini 3.5 Flash",
        provider = ApiProvider.GOOGLE,
        description = "Google's fast, multimodal model optimized for speed and general tasks.",
        supportsImages = true,
        supportsDocuments = true
    ),
    GEMINI_PRO(
        id = "gemini-3.1-pro-preview",
        displayName = "Gemini 3.1 Pro",
        provider = ApiProvider.GOOGLE,
        description = "Google's flagship model for advanced reasoning, coding, and complex tasks.",
        supportsImages = true,
        supportsDocuments = true
    ),
    GPT_4O(
        id = "gpt-4o",
        displayName = "GPT-4o",
        provider = ApiProvider.OPENAI,
        description = "OpenAI's versatile high-intelligence multimodal model.",
        supportsImages = true,
        supportsDocuments = true
    ),
    GPT_4O_MINI(
        id = "gpt-4o-mini",
        displayName = "GPT-4o Mini",
        provider = ApiProvider.OPENAI,
        description = "OpenAI's fast, lightweight intelligence model.",
        supportsImages = true,
        supportsDocuments = true
    ),
    CLAUDE_SONNET(
        id = "claude-3-5-sonnet",
        displayName = "Claude 3.5 Sonnet",
        provider = ApiProvider.ANTHROPIC,
        description = "Anthropic's state-of-the-art model for coding, analysis, and deep reasoning.",
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
    val googleApiKey: String = "",
    val openaiApiKey: String = "",
    val anthropicApiKey: String = "",
    val isLoggedIn: Boolean = false,
    val authType: String = "", // "GUEST", "GOOGLE", "MICROSOFT", "EMAIL", "PASSKEY"
    val userEmail: String = "",
    val userName: String = "",
    val biometricsEnabled: Boolean = false,
    val responseStyle: ResponseStyle = ResponseStyle.BALANCED,
    val memoryEnabled: Boolean = true,
    val saveHistory: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val completionNotifications: Boolean = true
)
