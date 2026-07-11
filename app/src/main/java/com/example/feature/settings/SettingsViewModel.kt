package com.example.feature.settings

import com.example.core.database.MemoryItem
import com.example.core.model.AiModel
import com.example.core.model.AppLanguage
import com.example.core.model.AppSettings
import com.example.core.model.AppTheme
import com.example.core.model.FontSize
import com.example.core.model.ResponseStyle
import com.example.feature.memory.MemoryRepository

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val memoryRepository: MemoryRepository
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepository.settings

    val memories: StateFlow<List<MemoryItem>> = memoryRepository.allMemories
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun updateTheme(theme: AppTheme) {
        settingsRepository.updateTheme(theme)
    }

    fun updateLoginState(isLoggedIn: Boolean, authType: String, userEmail: String, userName: String) {
        settingsRepository.updateLoginState(isLoggedIn, authType, userEmail, userName)
    }

    fun updateGoogleAccount(email: String, name: String) {
        settingsRepository.updateGoogleAccount(email, name)
    }

    fun updateMicrosoftAccount(email: String, name: String) {
        settingsRepository.updateMicrosoftAccount(email, name)
    }

    fun switchActiveAccount(authType: String) {
        settingsRepository.switchActiveAccount(authType)
    }

    fun updateBiometricsEnabled(enabled: Boolean) {
        settingsRepository.updateBiometricsEnabled(enabled)
    }

    fun logout() {
        settingsRepository.logout()
    }

    fun updateLanguage(language: AppLanguage) {
        settingsRepository.updateLanguage(language)
    }

    fun updateFontSize(fontSize: FontSize) {
        settingsRepository.updateFontSize(fontSize)
    }

    fun updateDefaultModel(model: AiModel) {
        settingsRepository.updateDefaultModel(model)
    }

    fun updateVoiceEnabled(enabled: Boolean) {
        settingsRepository.updateVoiceEnabled(enabled)
    }


    fun updateResponseStyle(style: ResponseStyle) {
        settingsRepository.updateResponseStyle(style)
    }
    fun updateMemoryEnabled(enabled: Boolean) {
        settingsRepository.updateMemoryEnabled(enabled)
    }
    fun updateSaveHistory(enabled: Boolean) {
        settingsRepository.updateSaveHistory(enabled)
    }
    fun updateNotificationsEnabled(enabled: Boolean) {
        settingsRepository.updateNotificationsEnabled(enabled)
    }
    fun updateCompletionNotifications(enabled: Boolean) {
        settingsRepository.updateCompletionNotifications(enabled)
    }
    fun updateHapticFeedback(enabled: Boolean) {
        settingsRepository.updateHapticFeedback(enabled)
    }

    fun saveApiKeys(nabih: String, google: String, openai: String, anthropic: String) {
        settingsRepository.updateApiKeys(nabih, google, openai, anthropic)
    }

    suspend fun validateAndSaveApiKeys(
        nabih: String,
        google: String,
        openai: String,
        anthropic: String,
        isArabic: Boolean
    ): Pair<Boolean, String> {
        if (google.isNotEmpty() && google != settings.value.googleApiKey) {
            val valid = validateGeminiKey(google)
            if (!valid) {
                return Pair(false, if (isArabic) "مفتاح Google Gemini غير صالح! يرجى التحقق منه." else "Invalid Google Gemini Key! Please check it.")
            }
        }
        
        if (nabih.isNotEmpty() && nabih != settings.value.nabihApiKey) {
            val valid = validateGeminiKey(nabih)
            if (!valid) {
                return Pair(false, if (isArabic) "مفتاح Nabih Ultra غير صالح! يرجى التحقق منه." else "Invalid Nabih Ultra Key! Please check it.")
            }
        }

        if (openai.isNotEmpty() && openai != settings.value.openaiApiKey) {
            val valid = validateOpenAiKey(openai)
            if (!valid) {
                return Pair(false, if (isArabic) "مفتاح OpenAI غير صالح! يرجى التحقق منه." else "Invalid OpenAI Key! Please check it.")
            }
        }

        if (anthropic.isNotEmpty() && anthropic != settings.value.anthropicApiKey) {
            if (!anthropic.startsWith("sk-ant-") || anthropic.length < 20) {
                return Pair(false, if (isArabic) "صيغة مفتاح Anthropic Claude غير صالحة!" else "Invalid Anthropic Claude Key format!")
            }
        }

        settingsRepository.updateApiKeys(nabih, google, openai, anthropic)
        return Pair(true, if (isArabic) "تم حفظ المفاتيح بنجاح!" else "Keys saved successfully!")
    }

    private suspend fun validateGeminiKey(key: String): Boolean {
        return try {
            val response = com.example.core.network.NetworkClient.geminiService.generateContent(
                model = "gemini-1.5-flash",
                apiKey = key,
                request = com.example.core.network.GeminiRequest(
                    contents = listOf(
                        com.example.core.network.GeminiContent(
                            parts = listOf(com.example.core.network.GeminiPart(text = "Hello"))
                        )
                    ),
                    generationConfig = com.example.core.network.GeminiGenerationConfig(maxOutputTokens = 1)
                )
            )
            response.candidates != null
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun validateOpenAiKey(key: String): Boolean {
        return try {
            val response = com.example.core.network.NetworkClient.openAiService.generateCompletion(
                authorization = "Bearer $key",
                request = com.example.core.network.OpenAiRequest(
                    model = "gpt-3.5-turbo",
                    messages = listOf(com.example.core.network.OpenAiMessage("user", "Hello")),
                    temperature = 0.7f
                )
            )
            response.choices != null
        } catch (e: Exception) {
            false
        }
    }

    fun addMemory(content: String) {
        viewModelScope.launch {
            if (content.isNotBlank()) {
                memoryRepository.addMemory(content)
            }
        }
    }

    fun deleteMemory(id: String) {
        viewModelScope.launch {
            memoryRepository.deleteMemory(id)
        }
    }
}
