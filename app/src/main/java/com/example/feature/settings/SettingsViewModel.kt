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

    fun saveApiKeys(
        nabih: String,
        google: String,
        openai: String,
        anthropic: String
    ) {
        settingsRepository.updateApiKeys(
            nabih.trim(),
            google.trim(),
            openai.trim(),
            anthropic.trim()
        )
    }

    suspend fun validateAndSaveApiKeys(
        nabih: String,
        google: String,
        openai: String,
        anthropic: String,
        isArabic: Boolean
    ): Pair<Boolean, String> {
        val trimmedGoogle = google.trim()
        val trimmedNabih = nabih.trim()
        val trimmedOpenai = openai.trim()
        val trimmedAnthropic = anthropic.trim()

        if (trimmedGoogle.isNotEmpty() && trimmedGoogle != settings.value.googleApiKey) {
            val (valid, errorMsg) = validateGeminiKey(trimmedGoogle, "Google Gemini", isArabic)
            if (!valid) return Pair(false, errorMsg)
        }
        
        if (trimmedNabih.isNotEmpty() && trimmedNabih != settings.value.nabihApiKey) {
            val (valid, errorMsg) = validateGeminiKey(trimmedNabih, "Nabih Ultra", isArabic)
            if (!valid) return Pair(false, errorMsg)
        }

        if (trimmedOpenai.isNotEmpty() && trimmedOpenai != settings.value.openaiApiKey) {
            val (valid, errorMsg) = validateOpenAiKey(trimmedOpenai, isArabic)
            if (!valid) return Pair(false, errorMsg)
        }

        if (trimmedAnthropic.isNotEmpty() && trimmedAnthropic != settings.value.anthropicApiKey) {
            val (valid, errorMsg) = validateAnthropicKey(trimmedAnthropic, isArabic)
            if (!valid) return Pair(false, errorMsg)
        }

        settingsRepository.updateApiKeys(
            trimmedNabih,
            trimmedGoogle,
            trimmedOpenai,
            trimmedAnthropic
        )
        return Pair(true, if (isArabic) "تم حفظ الإعدادات بنجاح!" else "Settings saved successfully!")
    }

    private suspend fun validateGeminiKey(key: String, providerName: String, isArabic: Boolean): Pair<Boolean, String> {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent"
        android.util.Log.d("ApiValidation", "Validating $providerName Key. Request URL: $url")
        return try {
            val response = com.example.core.network.NetworkClient.geminiService.generateContent(
                model = "gemini-1.5-flash",
                apiKey = key,
                request = com.example.core.network.GeminiRequest(
                    contents = listOf(
                        com.example.core.network.GeminiContent(
                            parts = listOf(com.example.core.network.GeminiPart(text = "Ping"))
                        )
                    ),
                    generationConfig = com.example.core.network.GeminiGenerationConfig(maxOutputTokens = 1)
                )
            )
            val successMsg = if (response.candidates != null) "Success" else "No Candidates"
            android.util.Log.d("ApiValidation", "$providerName Key Validation. Response status code: 200, Status: $successMsg")
            Pair(true, "")
        } catch (e: retrofit2.HttpException) {
            val code = e.code()
            val errorBody = e.response()?.errorBody()?.string() ?: ""
            android.util.Log.e("ApiValidation", "$providerName Key Validation failed. URL: $url. Status: $code. Error: $errorBody")
            val errorDetail = parseJsonError(errorBody) ?: errorBody
            val errorMsg = if (isArabic) {
                "فشل التحقق من مفتاح $providerName (HTTP $code): $errorDetail"
            } else {
                "$providerName Key validation failed (HTTP $code): $errorDetail"
            }
            Pair(false, errorMsg)
        } catch (e: Exception) {
            android.util.Log.e("ApiValidation", "$providerName Key Validation failed with exception", e)
            val errorMsg = if (isArabic) {
                "فشل الاتصال بـ $providerName: ${e.localizedMessage}"
            } else {
                "Connection to $providerName failed: ${e.localizedMessage}"
            }
            Pair(false, errorMsg)
        }
    }

    private suspend fun validateOpenAiKey(key: String, isArabic: Boolean): Pair<Boolean, String> {
        val url = "https://api.openai.com/v1/chat/completions"
        android.util.Log.d("ApiValidation", "Validating OpenAI Key. Request URL: $url")
        return try {
            val response = com.example.core.network.NetworkClient.openAiService.generateCompletion(
                authorization = "Bearer $key",
                request = com.example.core.network.OpenAiRequest(
                    model = "gpt-4o-mini",
                    messages = listOf(com.example.core.network.OpenAiMessage("user", "Ping")),
                    temperature = 0.7f
                )
            )
            android.util.Log.d("ApiValidation", "OpenAI Key Validation success. Response status code: 200")
            Pair(true, "")
        } catch (e: retrofit2.HttpException) {
            val code = e.code()
            val errorBody = e.response()?.errorBody()?.string() ?: ""
            android.util.Log.e("ApiValidation", "OpenAI Key Validation failed. URL: $url. Status: $code. Error: $errorBody")
            val errorDetail = parseJsonError(errorBody) ?: errorBody
            val errorMsg = if (isArabic) {
                "فشل التحقق من مفتاح OpenAI (HTTP $code): $errorDetail"
            } else {
                "OpenAI Key validation failed (HTTP $code): $errorDetail"
            }
            Pair(false, errorMsg)
        } catch (e: Exception) {
            android.util.Log.e("ApiValidation", "OpenAI Key Validation failed with exception", e)
            val errorMsg = if (isArabic) {
                "فشل الاتصال بـ OpenAI: ${e.localizedMessage}"
            } else {
                "Connection to OpenAI failed: ${e.localizedMessage}"
            }
            Pair(false, errorMsg)
        }
    }

    private suspend fun validateAnthropicKey(key: String, isArabic: Boolean): Pair<Boolean, String> {
        val url = "https://api.anthropic.com/v1/messages"
        android.util.Log.d("ApiValidation", "Validating Anthropic Key. Request URL: $url")
        return try {
            val response = com.example.core.network.NetworkClient.claudeService.generateMessage(
                apiKey = key,
                request = com.example.core.network.ClaudeRequest(
                    model = "claude-3-haiku-20240307",
                    messages = listOf(com.example.core.network.ClaudeMessage("user", "Ping")),
                    max_tokens = 1
                )
            )
            android.util.Log.d("ApiValidation", "Anthropic Key Validation success. Response status code: 200")
            Pair(true, "")
        } catch (e: retrofit2.HttpException) {
            val code = e.code()
            val errorBody = e.response()?.errorBody()?.string() ?: ""
            android.util.Log.e("ApiValidation", "Anthropic Key Validation failed. URL: $url. Status: $code. Error: $errorBody")
            val errorDetail = parseJsonError(errorBody) ?: errorBody
            val errorMsg = if (isArabic) {
                "فشل التحقق من مفتاح Anthropic (HTTP $code): $errorDetail"
            } else {
                "Anthropic Key validation failed (HTTP $code): $errorDetail"
            }
            Pair(false, errorMsg)
        } catch (e: Exception) {
            android.util.Log.e("ApiValidation", "Anthropic Key Validation failed with exception", e)
            val errorMsg = if (isArabic) {
                "فشل الاتصال بـ Anthropic: ${e.localizedMessage}"
            } else {
                "Connection to Anthropic failed: ${e.localizedMessage}"
            }
            Pair(false, errorMsg)
        }
    }

    private fun parseJsonError(json: String): String? {
        return try {
            val jsonObject = org.json.JSONObject(json)
            if (jsonObject.has("error")) {
                val errorObj = jsonObject.get("error")
                if (errorObj is org.json.JSONObject) {
                    if (errorObj.has("message")) {
                        return errorObj.getString("message")
                    }
                } else if (errorObj is String) {
                    return errorObj
                }
            }
            null
        } catch (e: Exception) {
            null
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
