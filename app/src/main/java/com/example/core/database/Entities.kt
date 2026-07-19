package com.example.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "folders")
data class Folder(
    @PrimaryKey val id: String,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "conversations")
data class Conversation(
    @PrimaryKey val id: String,
    val title: String,
    val folderId: String? = null,
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val modelId: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "messages")
data class Message(
    @PrimaryKey val id: String,
    val conversationId: String,
    val role: String, // "user", "model", "system"
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val imageUri: String? = null,
    val documentUri: String? = null,
    val documentName: String? = null,
    val isVoice: Boolean = false
)

@Entity(tableName = "memory_items")
data class MemoryItem(
    @PrimaryKey val id: String,
    val content: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "user_accounts")
data class UserAccount(
    @PrimaryKey val email: String, // lowercase & trimmed
    val name: String,
    val passwordHash: String
)

data class AttachmentItem(
    val messageId: String,
    val conversationId: String,
    val conversationTitle: String,
    val imageUri: String?,
    val documentUri: String?,
    val documentName: String?,
    val timestamp: Long
)

@Entity(tableName = "error_logs")
data class ErrorLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val errorType: String,
    val provider: String,
    val timestamp: Long = System.currentTimeMillis()
)


