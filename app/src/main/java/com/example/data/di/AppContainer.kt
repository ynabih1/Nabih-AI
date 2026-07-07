package com.example.data.di

import android.content.Context
import com.example.data.database.AppDatabase
import com.example.data.repository.ChatRepository
import com.example.data.repository.MemoryRepository
import com.example.data.repository.SettingsRepository

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
