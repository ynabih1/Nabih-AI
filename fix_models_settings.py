import re

with open('app/src/main/java/com/example/data/model/Models.kt', 'r') as f:
    code = f.read()

response_style_enum = """
enum class ResponseStyle(val displayName: String) {
    FAST("Fast"),
    BALANCED("Balanced"),
    DETAILED("Detailed")
}
"""

if "enum class ResponseStyle" not in code:
    code = code.replace("enum class FontSize", response_style_enum + "enum class FontSize")

settings_repl = """    val biometricsEnabled: Boolean = false,
    val responseStyle: ResponseStyle = ResponseStyle.BALANCED,
    val memoryEnabled: Boolean = true,
    val saveHistory: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val completionNotifications: Boolean = true
)"""

code = code.replace("    val biometricsEnabled: Boolean = false\n)", settings_repl)

with open('app/src/main/java/com/example/data/model/Models.kt', 'w') as f:
    f.write(code)

with open('app/src/main/java/com/example/data/repository/SettingsRepository.kt', 'r') as f:
    code_repo = f.read()

load_settings_repl = """            userEmail = prefs.getString("user_email", "") ?: "",
            userName = prefs.getString("user_name", "") ?: "",
            biometricsEnabled = prefs.getBoolean("biometrics_enabled", false),
            responseStyle = ResponseStyle.valueOf(prefs.getString("response_style", ResponseStyle.BALANCED.name) ?: ResponseStyle.BALANCED.name),
            memoryEnabled = prefs.getBoolean("memory_enabled", true),
            saveHistory = prefs.getBoolean("save_history", true),
            notificationsEnabled = prefs.getBoolean("notifications_enabled", true),
            completionNotifications = prefs.getBoolean("completion_notifications", true)
        )"""

code_repo = code_repo.replace("""            userEmail = prefs.getString("user_email", "") ?: "",
            userName = prefs.getString("user_name", "") ?: "",
            biometricsEnabled = prefs.getBoolean("biometrics_enabled", false)
        )""", load_settings_repl)

new_repo_funcs = """
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
"""

if "fun updateResponseStyle" not in code_repo:
    code_repo = code_repo.replace("    fun updateHapticFeedback", new_repo_funcs + "    fun updateHapticFeedback")

with open('app/src/main/java/com/example/data/repository/SettingsRepository.kt', 'w') as f:
    f.write(code_repo)


with open('app/src/main/java/com/example/ui/viewmodel/SettingsViewModel.kt', 'r') as f:
    code_vm = f.read()

new_vm_funcs = """
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
"""

if "fun updateResponseStyle" not in code_vm:
    code_vm = code_vm.replace("    fun updateHapticFeedback", new_vm_funcs + "    fun updateHapticFeedback")

with open('app/src/main/java/com/example/ui/viewmodel/SettingsViewModel.kt', 'w') as f:
    f.write(code_vm)

