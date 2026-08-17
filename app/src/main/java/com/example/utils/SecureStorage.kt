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

        try {
            val prefs = encryptedPrefs
            if (prefs != null) {
                prefs.edit().putString(keyName, cleanValue).apply()
                // نجح التشفير: احذف أي نسخة قديمة غير مشفرة متبقية من هذا المفتاح إن وُجدت
                fallbackPrefs.edit().remove(keyName).apply()
                return true
            }
        } catch (e: Exception) {
            Log.e("SecureStorage", "Encrypted save failed, using fallback storage", e)
        }

        // نصل هنا فقط إذا فشل التشفير فعلياً
        return try {
            fallbackPrefs.edit().putString(keyName, cleanValue).commit()
            Log.w("SecureStorage", "Key '$keyName' saved WITHOUT encryption (fallback mode)")
            true
        } catch (e: Exception) {
            Log.e("SecureStorage", "Failed to save key in fallbackPrefs", e)
            false
        }
    }

    fun migratePlaintextKeysToEncrypted() {
        val prefs = encryptedPrefs ?: return
        val allFallbackKeys = fallbackPrefs.all
        if (allFallbackKeys.isEmpty()) return

        val editor = prefs.edit()
        allFallbackKeys.forEach { (key, value) ->
            if (value is String) editor.putString(key, value)
        }
        editor.apply()
        fallbackPrefs.edit().clear().apply()
        Log.i("SecureStorage", "Migrated ${allFallbackKeys.size} plaintext keys to encrypted storage")
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

