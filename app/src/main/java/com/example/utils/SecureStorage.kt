package com.example.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import android.util.Log

class SecureStorage(private val context: Context) {
    private val fallbackPrefs: SharedPreferences by lazy {
        context.getSharedPreferences("secured_nabih_prefs_fallback", Context.MODE_PRIVATE)
    }

    private val encryptedPrefs: SharedPreferences? by lazy {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context,
                "secured_nabih_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Log.e("SecureStorage", "EncryptedSharedPreferences creation failed, using fallback", e)
            null
        }
    }

    fun saveKey(keyName: String, keyValue: String): Boolean {
        val cleanValue = keyValue.trim()
        var savedInEncrypted = false
        try {
            if (encryptedPrefs != null) {
                encryptedPrefs?.edit()?.putString(keyName, cleanValue)?.apply()
                savedInEncrypted = true
            }
        } catch (e: Exception) {
            Log.e("SecureStorage", "Failed to save key in encryptedPrefs, saving to fallback", e)
        }

        return try {
            fallbackPrefs.edit().putString(keyName, cleanValue).commit()
            true
        } catch (e: Exception) {
            Log.e("SecureStorage", "Failed to save key in fallbackPrefs", e)
            savedInEncrypted
        }
    }

    fun getKey(keyName: String): String {
        try {
            val encryptedVal = encryptedPrefs?.getString(keyName, "")
            if (!encryptedVal.isNullOrBlank()) {
                return encryptedVal.trim()
            }
        } catch (e: Exception) {
            Log.e("SecureStorage", "Failed to read key from encryptedPrefs, trying fallback", e)
        }

        return try {
            fallbackPrefs.getString(keyName, "")?.trim() ?: ""
        } catch (e: Exception) {
            Log.e("SecureStorage", "Failed to read key from fallbackPrefs", e)
            ""
        }
    }
}

