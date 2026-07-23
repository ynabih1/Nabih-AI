package com.example.utils

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import java.io.File
import java.util.Locale

object FileUtils {

    /**
     * Official and reliable method to get file size in bytes from any Uri
     * (content:// from Photo Picker, MediaStore, Google Drive, or file://).
     */
    fun getFileSizeFromUri(context: Context, uri: Uri): Long {
        var size = 0L

        // 1. Primary Method: Query ContentResolver using OpenableColumns.SIZE
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst() && sizeIndex != -1 && !cursor.isNull(sizeIndex)) {
                    size = cursor.getLong(sizeIndex)
                }
            }
        } catch (e: Exception) {
            Log.e("FileUtils", "Error querying size via ContentResolver for $uri", e)
        }

        // 2. Secondary Method: Direct file check if file scheme or valid file path
        if (size <= 0) {
            try {
                if (uri.scheme == "file" || (uri.path != null && uri.path!!.startsWith("/"))) {
                    val file = File(uri.path ?: "")
                    if (file.exists() && file.length() > 0) {
                        size = file.length()
                    }
                }
            } catch (e: Exception) {
                Log.e("FileUtils", "Error getting File length for $uri", e)
            }
        }

        // 3. Fallback Method: ContentResolver openInputStream available bytes
        if (size <= 0) {
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val available = inputStream.available().toLong()
                    if (available > 0) {
                        size = available
                    }
                }
            } catch (e: Exception) {
                Log.e("FileUtils", "Error opening input stream for $uri", e)
            }
        }

        // 4. AssetFileDescriptor fallback for non-standard providers
        if (size <= 0) {
            try {
                context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { afd ->
                    if (afd.length > 0) {
                        size = afd.length
                    }
                }
            } catch (e: Exception) {
                Log.e("FileUtils", "Error opening AssetFileDescriptor for $uri", e)
            }
        }

        return size
    }

    /**
     * Formats bytes into human-readable string (e.g. 1.2 MB, 450 KB, 512 B).
     */
    fun formatFileSize(bytes: Long): String {
        return when {
            bytes <= 0 -> "0 B"
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> String.format(Locale.US, "%.1f KB", bytes / 1024.0)
            else -> String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
        }
    }

    /**
     * Gets a human-readable file size string directly from a Uri.
     */
    fun getUriSizeFormatted(context: Context, uri: Uri?): String {
        if (uri == null) return "0 B"
        val bytes = getFileSizeFromUri(context, uri)
        return formatFileSize(bytes)
    }

    /**
     * Converts any content:// or file:// Uri to base64 encoded string.
     */
    fun uriToBase64(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val bytes = inputStream.readBytes()
                android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
            }
        } catch (e: Exception) {
            Log.e("FileUtils", "Error converting uri to base64: $uri", e)
            null
        }
    }
}
