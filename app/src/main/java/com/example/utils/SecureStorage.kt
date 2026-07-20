package com.example.utils

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import android.util.Log

class SecureStorage(context: Context) {
    private val masterKey = try {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    } catch (e: Exception) {
        Log.e("SecureStorage", "Failed to get or create MasterKey", e)
        null
    }

    private val sharedPrefs by lazy {
        try {
            if (masterKey != null) {
                EncryptedSharedPreferences.create(
                    context,
                    "secured_nabih_prefs",
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )
            } else {
                throw Exception("MasterKey is null")
            }
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
