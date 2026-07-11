package com.example.core.utils

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import android.util.Log

class SecureStorage(context: Context) {
    private val masterKeyAlias = try {
        MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
    } catch (e: Exception) {
        Log.e("SecureStorage", "Failed to get or create MasterKey", e)
        "secured_nabih_key_alias"
    }

    private val sharedPrefs by lazy {
        try {
            EncryptedSharedPreferences.create(
                "secured_nabih_prefs",
                masterKeyAlias,
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Log.e("SecureStorage", "EncryptedSharedPreferences creation failed, falling back to private prefs", e)
            context.getSharedPreferences("secured_nabih_prefs_fallback", Context.MODE_PRIVATE)
        }
    }

    fun saveKey(keyName: String, keyValue: String) {
        sharedPrefs.edit().putString(keyName, keyValue).apply()
    }

    fun getKey(keyName: String): String {
        return sharedPrefs.getString(keyName, "") ?: ""
    }
}
