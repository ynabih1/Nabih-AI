package com.example.di

import com.example.data.repository.ChatRepository
import com.example.data.repository.SettingsRepository
import com.example.data.repository.MemoryRepository
import com.example.data.local.AppDatabase
import android.content.Context
import com.example.utils.NetworkMonitor

interface AppContainer {
    val settingsRepository: SettingsRepository
    val memoryRepository: MemoryRepository
    val chatRepository: ChatRepository
    val notificationHelper: com.example.utils.NotificationHelper
    val networkMonitor: NetworkMonitor
}

class DefaultAppContainer(private val context: Context) : AppContainer {
    private val database: AppDatabase by lazy {
        AppDatabase.getDatabase(context)
    }

    override val networkMonitor: NetworkMonitor by lazy {
        NetworkMonitor(context)
    }

    override val settingsRepository: SettingsRepository by lazy {
        SettingsRepository(context)
    }

    override val memoryRepository: MemoryRepository by lazy {
        MemoryRepository(database.memoryDao())
    }

    override val notificationHelper: com.example.utils.NotificationHelper by lazy {
        com.example.utils.NotificationHelper(context)
    }

    override val chatRepository: ChatRepository by lazy {
        ChatRepository(
            context = context,
            folderDao = database.folderDao(),
            conversationDao = database.conversationDao(),
            messageDao = database.messageDao(),
            memoryDao = database.memoryDao(),
            errorLogDao = database.errorLogDao(),
            settingsRepository = settingsRepository
        )
    }
}
