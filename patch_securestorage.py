import re

with open('app/src/main/java/com/example/core/utils/SecureStorage.kt', 'r') as f:
    content = f.read()

content = content.replace("import androidx.security.crypto.MasterKeys", "import androidx.security.crypto.MasterKey")
old_key = """    private val masterKeyAlias = try {
        MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
    } catch (e: Exception) {
        Log.e("SecureStorage", "Failed to get or create MasterKey", e)
        "secured_nabih_key_alias"
    }"""
    
new_key = """    private val masterKey = try {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    } catch (e: Exception) {
        Log.e("SecureStorage", "Failed to get or create MasterKey", e)
        null
    }"""

content = content.replace(old_key, new_key)

old_prefs = """            EncryptedSharedPreferences.create(
                "secured_nabih_prefs",
                masterKeyAlias,
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )"""

new_prefs = """            if (masterKey != null) {
                EncryptedSharedPreferences.create(
                    context,
                    "secured_nabih_prefs",
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )
            } else {
                throw Exception("MasterKey is null")
            }"""

content = content.replace(old_prefs, new_prefs)

with open('app/src/main/java/com/example/core/utils/SecureStorage.kt', 'w') as f:
    f.write(content)
print("SecureStorage patched")
