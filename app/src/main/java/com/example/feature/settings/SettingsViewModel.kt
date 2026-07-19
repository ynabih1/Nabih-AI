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

    fun updateProfile(name: String, pictureUri: String, info: String, newEmail: String = "", handle: String = "") {
        viewModelScope.launch {
            settingsRepository.updateProfile(name, pictureUri, info, newEmail, handle)
        }
    }

    suspend fun updatePassword(newPasswordHash: String): Boolean {
        return settingsRepository.updatePassword(newPasswordHash)
    }

    fun updateLoginState(isLoggedIn: Boolean, authType: String, userEmail: String, userName: String, rememberMe: Boolean = false) {
        settingsRepository.updateLoginState(isLoggedIn, authType, userEmail, userName, rememberMe)
    }

    suspend fun getUserByEmail(email: String): com.example.core.database.UserAccount? {
        return settingsRepository.getUserByEmail(email)
    }

    suspend fun registerUser(email: String, name: String, passwordHash: String): Boolean {
        return settingsRepository.registerUser(email, name, passwordHash)
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

    fun getExtraApiKey(keyName: String): String {
        return settingsRepository.getExtraApiKey(keyName)
    }

    fun saveExtraApiKey(keyName: String, value: String) {
        settingsRepository.saveExtraApiKey(keyName, value)
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

    data class ValidationResult(
        val isValid: Boolean,
        val isAuthError: Boolean,
        val message: String
    )

    private suspend fun validateKeyGeneric(
        providerName: String,
        key: String,
        isArabic: Boolean,
        apiCall: suspend () -> Unit
    ): ValidationResult {
        if (key.isBlank()) {
            return ValidationResult(isValid = true, isAuthError = false, message = "")
        }
        return try {
            apiCall()
            ValidationResult(
                isValid = true,
                isAuthError = false,
                message = if (isArabic) "✓ تم التحقق من مفتاح $providerName بنجاح" else "✓ $providerName validated successfully"
            )
        } catch (e: retrofit2.HttpException) {
            val code = e.code()
            val errorBody = e.response()?.errorBody()?.string() ?: ""
            val errorLower = errorBody.lowercase()
            android.util.Log.e("ApiValidation", "$providerName validation failed (HTTP $code). Body: $errorBody")
            
            val isAuth = when (providerName) {
                "Google Gemini", "Nabih Ultra" -> {
                    code == 401 || code == 403 || (code == 400 && (errorLower.contains("api_key_invalid") || errorLower.contains("not valid") || errorLower.contains("invalid") || errorLower.contains("key_invalid")))
                }
                "OpenAI" -> {
                    code == 401 || (code == 400 && errorLower.contains("invalid"))
                }
                "Anthropic Claude" -> {
                    code == 401 || code == 403 || errorLower.contains("api_key")
                }
                else -> code == 401 || code == 403
            }
            
            val isQuota = errorLower.contains("quota") || errorLower.contains("exhausted") || errorLower.contains("balance") || errorLower.contains("billing")
            
            if (isAuth) {
                val detail = parseJsonError(errorBody) ?: (if (isArabic) "مفتاح API غير صالح" else "Invalid API key")
                val msg = if (isArabic) {
                    "❌ مفتاح $providerName غير صالح (HTTP $code): $detail"
                } else {
                    "❌ $providerName API key is invalid (HTTP $code): $detail"
                }
                ValidationResult(isValid = false, isAuthError = true, message = msg)
            } else if (code == 429) {
                if (isQuota) {
                    val msg = if (isArabic) {
                        "⚠️ $providerName: تم تجاوز الحصة (Quota Exceeded). المفتاح صحيح ولكنه يحتاج رصيداً أو تفعيل الدفع."
                    } else {
                        "⚠️ $providerName: Quota Exceeded. The key is valid, but billing is inactive or usage limit reached."
                    }
                    ValidationResult(isValid = true, isAuthError = false, message = msg)
                } else {
                    val msg = if (isArabic) {
                        "⚠️ $providerName: تجاوز حد الطلبات (Rate Limit). يرجى الانتظار قليلاً."
                    } else {
                        "⚠️ $providerName: Rate Limit exceeded. Please try again in a moment."
                    }
                    ValidationResult(isValid = true, isAuthError = false, message = msg)
                }
            } else if (code >= 500) {
                val msg = if (isArabic) {
                    "⚠️ $providerName: خادم الخدمة غير متوفر حالياً (HTTP $code). يرجى المحاولة لاحقاً."
                } else {
                    "⚠️ $providerName: Server unavailable (HTTP $code). Please try again later."
                }
                ValidationResult(isValid = true, isAuthError = false, message = msg)
            } else {
                val detail = parseJsonError(errorBody) ?: errorBody.take(100)
                val msg = if (isArabic) {
                    "⚠️ $providerName: فشل الطلب (HTTP $code): $detail"
                } else {
                    "⚠️ $providerName: Request failed (HTTP $code): $detail"
                }
                ValidationResult(isValid = true, isAuthError = false, message = msg)
            }
        } catch (e: java.io.IOException) {
            android.util.Log.e("ApiValidation", "$providerName validation failed with network exception", e)
            val msg = if (isArabic) {
                "⚠️ $providerName: خطأ في الشبكة. فشل الاتصال بالخادم."
            } else {
                "⚠️ $providerName: Network error. Failed to connect to server."
            }
            ValidationResult(isValid = true, isAuthError = false, message = msg)
        } catch (e: Exception) {
            android.util.Log.e("ApiValidation", "$providerName validation failed with general exception", e)
            val msg = if (isArabic) {
                "⚠️ $providerName: خطأ غير متوقع: ${e.localizedMessage}"
            } else {
                "⚠️ $providerName: Unexpected error: ${e.localizedMessage}"
            }
            ValidationResult(isValid = true, isAuthError = false, message = msg)
        }
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

        val results = mutableListOf<ValidationResult>()

        // 1. Google Gemini Key Validation
        if (trimmedGoogle.isNotEmpty() && trimmedGoogle != settings.value.googleApiKey) {
            val res = validateKeyGeneric("Google Gemini", trimmedGoogle, isArabic) {
                com.example.core.network.NetworkClient.geminiService.generateContent(
                    model = "gemini-1.5-flash",
                    apiKey = trimmedGoogle,
                    request = com.example.core.network.GeminiRequest(
                        contents = listOf(
                            com.example.core.network.GeminiContent(
                                parts = listOf(com.example.core.network.GeminiPart(text = "Ping"))
                            )
                        ),
                        generationConfig = com.example.core.network.GeminiGenerationConfig(maxOutputTokens = 1)
                    )
                )
            }
            results.add(res)
        }

        // 2. Nabih Ultra Key Validation
        if (trimmedNabih.isNotEmpty() && trimmedNabih != settings.value.nabihApiKey) {
            val res = validateKeyGeneric("Nabih Ultra", trimmedNabih, isArabic) {
                com.example.core.network.NetworkClient.geminiService.generateContent(
                    model = "gemini-1.5-flash",
                    apiKey = trimmedNabih,
                    request = com.example.core.network.GeminiRequest(
                        contents = listOf(
                            com.example.core.network.GeminiContent(
                                parts = listOf(com.example.core.network.GeminiPart(text = "Ping"))
                            )
                        ),
                        generationConfig = com.example.core.network.GeminiGenerationConfig(maxOutputTokens = 1)
                    )
                )
            }
            results.add(res)
        }

        // 3. OpenAI Key Validation
        if (trimmedOpenai.isNotEmpty() && trimmedOpenai != settings.value.openaiApiKey) {
            val res = validateKeyGeneric("OpenAI", trimmedOpenai, isArabic) {
                com.example.core.network.NetworkClient.openAiService.generateCompletion(
                    url = "https://api.openai.com/v1/chat/completions",
                    authorization = "Bearer $trimmedOpenai",
                    request = com.example.core.network.OpenAiRequest(
                        model = "gpt-4o-mini",
                        messages = listOf(com.example.core.network.OpenAiMessage("user", "Ping")),
                        temperature = 0.7f
                    )
                )
            }
            results.add(res)
        }

        // 4. Anthropic Claude Key Validation
        if (trimmedAnthropic.isNotEmpty() && trimmedAnthropic != settings.value.anthropicApiKey) {
            val res = validateKeyGeneric("Anthropic Claude", trimmedAnthropic, isArabic) {
                com.example.core.network.NetworkClient.claudeService.generateMessage(
                    apiKey = trimmedAnthropic,
                    request = com.example.core.network.ClaudeRequest(
                        model = "claude-3-haiku-20240307",
                        messages = listOf(com.example.core.network.ClaudeMessage("user", "Ping")),
                        max_tokens = 1
                    )
                )
            }
            results.add(res)
        }

        // Only block saving if a key is explicitly determined to be an INVALID authentication error
        val authFailures = results.filter { !it.isValid && it.isAuthError }
        if (authFailures.isNotEmpty()) {
            val combinedError = authFailures.joinToString("\n") { it.message }
            return Pair(false, combinedError)
        }

        // Update keys (allows quota-exhausted, rate-limited, and offline keys to be successfully edited/saved/deleted)
        settingsRepository.updateApiKeys(
            trimmedNabih,
            trimmedGoogle,
            trimmedOpenai,
            trimmedAnthropic
        )

        // Compile feedback messages
        val feedback = results.map { it.message }.filter { it.isNotEmpty() }
        val successBase = if (isArabic) "تم حفظ الإعدادات بنجاح!" else "Settings saved successfully!"
        val finalMessage = if (feedback.isNotEmpty()) {
            "$successBase\n" + feedback.joinToString("\n")
        } else {
            successBase
        }

        return Pair(true, finalMessage)
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
