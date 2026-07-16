package com.example.feature.settings

import com.example.core.model.AiModel
import com.example.core.model.AppLanguage
import com.example.core.model.AppSettings
import com.example.core.model.AppTheme
import com.example.core.model.FontSize
import com.example.core.model.ResponseStyle

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SettingsRepository(context: Context) {
    companion object {
        private var isFirstLoad = true
    }

    private val prefs = context.getSharedPreferences("nabih_ai_settings", Context.MODE_PRIVATE)
    private val secureStorage = com.example.core.utils.SecureStorage(context)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<AppSettings> = _settings

    private fun loadSettings(): AppSettings {
        val themeStr = prefs.getString("theme", AppTheme.LIGHT.name) ?: AppTheme.LIGHT.name
        val langStr = prefs.getString("language", AppLanguage.ENGLISH.name) ?: AppLanguage.ENGLISH.name
        val fontStr = prefs.getString("font_size", FontSize.MEDIUM.name) ?: FontSize.MEDIUM.name
        val modelStr = prefs.getString("default_model", AiModel.NABIH_ULTRA.name) ?: AiModel.NABIH_ULTRA.name
        
        var userName = prefs.getString("user_name", "") ?: ""
        if (userName == "Nabih User" || userName == "Nabih Microsoft User" || userName == "Guest User" || userName == "Nabih Core User" || userName == "Secured Passkey Session") {
            userName = ""
            prefs.edit().putString("user_name", userName).apply()
        }
        
        var userEmail = prefs.getString("user_email", "") ?: ""
        if (userEmail == "guest@nabih.ai") {
            userEmail = ""
            prefs.edit().putString("user_email", userEmail).apply()
        }

        val microsoftEmail = prefs.getString("microsoft_email", "") ?: ""
        val microsoftName = prefs.getString("microsoft_name", "") ?: ""
        val profilePictureUri = prefs.getString("profile_picture_uri", "") ?: ""
        val personalInfo = prefs.getString("personal_info", "") ?: ""

        val defaultModelVal = try {
            AiModel.valueOf(modelStr)
        } catch (e: Exception) {
            AiModel.NABIH_ULTRA
        }

        val rememberMe = prefs.getBoolean("remember_me", false)
        var isLoggedInCalculated = prefs.getBoolean("is_logged_in", false)
        if (isLoggedInCalculated && !rememberMe) {
            if (isFirstLoad) {
                isLoggedInCalculated = false
                prefs.edit().putBoolean("is_logged_in", false).apply()
            }
        }
        isFirstLoad = false

        return AppSettings(
            theme = AppTheme.valueOf(themeStr),
            language = AppLanguage.valueOf(langStr),
            fontSize = FontSize.valueOf(fontStr),
            defaultModel = defaultModelVal,
            voiceEnabled = prefs.getBoolean("voice_enabled", true),
            hapticFeedback = prefs.getBoolean("haptic_feedback", true),
            nabihApiKey = secureStorage.getKey("key_nabih"),
            googleApiKey = secureStorage.getKey("key_google"),
            openaiApiKey = secureStorage.getKey("key_openai"),
            anthropicApiKey = secureStorage.getKey("key_anthropic"),
            isLoggedIn = isLoggedInCalculated,
            authType = prefs.getString("auth_type", "") ?: "",
            userEmail = userEmail,
            userName = userName,
            profilePictureUri = profilePictureUri,
            personalInfo = personalInfo,
            microsoftEmail = microsoftEmail,
            microsoftName = microsoftName,
            biometricsEnabled = prefs.getBoolean("biometrics_enabled", false),
            responseStyle = ResponseStyle.valueOf(prefs.getString("response_style", ResponseStyle.BALANCED.name) ?: ResponseStyle.BALANCED.name),
            memoryEnabled = prefs.getBoolean("memory_enabled", true),
            saveHistory = prefs.getBoolean("save_history", true),
            notificationsEnabled = prefs.getBoolean("notifications_enabled", true),
            completionNotifications = prefs.getBoolean("completion_notifications", true)
        )
    }

    fun updateMicrosoftAccount(email: String, name: String) {
        prefs.edit()
            .putString("microsoft_email", email)
            .putString("microsoft_name", name)
            .apply()
        _settings.value = loadSettings()
    }

    fun switchActiveAccount(authType: String) {
        val email = when (authType) {
            "MICROSOFT" -> prefs.getString("microsoft_email", "") ?: ""
            else -> ""
        }
        val name = when (authType) {
            "MICROSOFT" -> prefs.getString("microsoft_name", "") ?: ""
            else -> ""
        }
        if (email.isNotEmpty()) {
            prefs.edit()
                .putBoolean("is_logged_in", true)
                .putString("auth_type", authType)
                .putString("user_email", email)
                .putString("user_name", name)
                .apply()
            _settings.value = loadSettings()
        }
    }

    fun updateLoginState(isLoggedIn: Boolean, authType: String, userEmail: String, userName: String, rememberMe: Boolean = false) {
        val currentEmail = prefs.getString("user_email", "") ?: ""
        
        prefs.edit()
            .putBoolean("is_logged_in", isLoggedIn)
            .putString("auth_type", authType)
            .putBoolean("remember_me", rememberMe)
            .apply()
        
        if (isLoggedIn) {
            // Prevent duplicates: link with existing user if the email matches case-insensitively
            val targetEmail = if (currentEmail.isNotEmpty() && currentEmail.lowercase().trim() == userEmail.lowercase().trim()) {
                currentEmail
            } else {
                userEmail
            }
            
            val targetName = if (userName.isNotEmpty()) userName else (prefs.getString("user_name", "") ?: "")
            
            prefs.edit()
                .putString("user_email", targetEmail)
                .putString("user_name", targetName)
                .apply()
            
            when (authType) {
                "MICROSOFT" -> {
                    prefs.edit()
                        .putString("microsoft_email", userEmail)
                        .putString("microsoft_name", userName)
                        .apply()
                }
            }
        }
        _settings.value = loadSettings()
    }

    fun updateBiometricsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("biometrics_enabled", enabled).apply()
        _settings.value = loadSettings()
    }

    fun logout() {
        prefs.edit()
            .putBoolean("is_logged_in", false)
            .putString("auth_type", "")
            .putString("user_email", "")
            .putString("user_name", "")
            .apply()
        _settings.value = loadSettings()
    }

    fun updateTheme(theme: AppTheme) {
        prefs.edit().putString("theme", theme.name).apply()
        _settings.value = loadSettings()
    }

    suspend fun updateProfile(name: String, pictureUri: String, info: String, newEmail: String = "") {
        val oldEmail = prefs.getString("user_email", "") ?: ""
        
        prefs.edit()
            .putString("user_name", name.trim())
            .putString("profile_picture_uri", pictureUri)
            .putString("personal_info", info.trim())
            .apply()
            
        if (newEmail.isNotBlank() && oldEmail.isNotBlank() && oldEmail.lowercase() != newEmail.lowercase()) {
            val trimmedNewEmail = newEmail.trim().lowercase()
            userAccountDao.updateUserEmail(oldEmail.trim().lowercase(), trimmedNewEmail)
            prefs.edit().putString("user_email", trimmedNewEmail).apply()
        }
        
        val currentEmail = prefs.getString("user_email", "") ?: ""
        if (currentEmail.isNotEmpty()) {
            val trimmedEmail = currentEmail.trim().lowercase()
            userAccountDao.updateUserName(trimmedEmail, name.trim())
        }
        
        val authType = prefs.getString("auth_type", "") ?: ""
        when (authType) {
            "MICROSOFT" -> {
                prefs.edit()
                    .putString("microsoft_name", name.trim())
                    .apply()
            }
        }
        _settings.value = loadSettings()
    }

    suspend fun updatePassword(newPasswordHash: String): Boolean {
        val email = prefs.getString("user_email", "") ?: ""
        if (email.isNotEmpty()) {
            userAccountDao.updateUserPassword(email.trim().lowercase(), newPasswordHash)
            return true
        }
        return false
    }

    fun updateLanguage(language: AppLanguage) {
        prefs.edit().putString("language", language.name).apply()
        _settings.value = loadSettings()
    }

    fun updateFontSize(fontSize: FontSize) {
        prefs.edit().putString("font_size", fontSize.name).apply()
        _settings.value = loadSettings()
    }

    fun updateDefaultModel(model: AiModel) {
        prefs.edit().putString("default_model", model.name).apply()
        _settings.value = loadSettings()
    }

    fun updateVoiceEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("voice_enabled", enabled).apply()
        _settings.value = loadSettings()
    }


    fun updateResponseStyle(style: ResponseStyle) {
        prefs.edit().putString("response_style", style.name).apply()
        _settings.value = loadSettings()
    }
    fun updateMemoryEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("memory_enabled", enabled).apply()
        _settings.value = loadSettings()
    }
    fun updateSaveHistory(enabled: Boolean) {
        prefs.edit().putBoolean("save_history", enabled).apply()
        _settings.value = loadSettings()
    }
    fun updateNotificationsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("notifications_enabled", enabled).apply()
        _settings.value = loadSettings()
    }
    fun updateCompletionNotifications(enabled: Boolean) {
        prefs.edit().putBoolean("completion_notifications", enabled).apply()
        _settings.value = loadSettings()
    }
    fun updateHapticFeedback(enabled: Boolean) {
        prefs.edit().putBoolean("haptic_feedback", enabled).apply()
        _settings.value = loadSettings()
    }

    fun updateApiKeys(
        nabih: String,
        google: String,
        openai: String,
        anthropic: String
    ) {
        secureStorage.saveKey("key_nabih", nabih)
        secureStorage.saveKey("key_google", google)
        secureStorage.saveKey("key_openai", openai)
        secureStorage.saveKey("key_anthropic", anthropic)
        _settings.value = loadSettings()
    }

    fun getExtraApiKey(keyName: String): String {
        return secureStorage.getKey(keyName)
    }

    fun saveExtraApiKey(keyName: String, value: String) {
        secureStorage.saveKey(keyName, value)
    }

    private val userAccountDao by lazy {
        com.example.core.database.AppDatabase.getDatabase(context).userAccountDao()
    }

    suspend fun getUserByEmail(email: String): com.example.core.database.UserAccount? {
        val trimmedEmail = email.trim().lowercase()
        return userAccountDao.getUserByEmail(trimmedEmail)
    }

    suspend fun registerUser(email: String, name: String, passwordHash: String): Boolean {
        val trimmedEmail = email.trim().lowercase()
        val existing = userAccountDao.getUserByEmail(trimmedEmail)
        if (existing != null) {
            return false
        }
        val newUser = com.example.core.database.UserAccount(
            email = trimmedEmail,
            name = name.trim(),
            passwordHash = passwordHash
        )
        userAccountDao.insertUser(newUser)
        return true
    }
}
