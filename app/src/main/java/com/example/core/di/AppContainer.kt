package com.example.core.di

import com.example.core.database.AppDatabase
import com.example.feature.chat.ChatRepository
import com.example.feature.memory.MemoryRepository
import com.example.feature.settings.SettingsRepository

import android.content.Context

interface AppContainer {
    val settingsRepository: SettingsRepository
    val memoryRepository: MemoryRepository
    val chatRepository: ChatRepository
}

class DefaultAppContainer(private val context: Context) : AppContainer {
    private val database: AppDatabase by lazy {
        AppDatabase.getDatabase(context)
    }

    override val settingsRepository: SettingsRepository by lazy {
        SettingsRepository(context)
    }

    override val memoryRepository: MemoryRepository by lazy {
        MemoryRepository(database.memoryDao())
    }

    override val chatRepository: ChatRepository by lazy {
        ChatRepository(
            context = context,
            folderDao = database.folderDao(),
            conversationDao = database.conversationDao(),
            messageDao = database.messageDao(),
            memoryDao = database.memoryDao(),
            settingsRepository = settingsRepository
        )
    }
}
