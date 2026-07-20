package com.example.di

import com.example.data.repository.MemoryRepository
import com.example.data.repository.SettingsRepository
import com.example.data.repository.ChatRepository
import com.example.chat.ChatViewModel
import com.example.chat.HomeViewModel
import com.example.settings.SettingsViewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras

class ViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        return when {
            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> {
                SettingsViewModel(container.settingsRepository, container.memoryRepository) as T
            }
            modelClass.isAssignableFrom(HomeViewModel::class.java) -> {
                HomeViewModel(container.chatRepository) as T
            }
            modelClass.isAssignableFrom(ChatViewModel::class.java) -> {
                val savedStateHandle = extras.createSavedStateHandle()
                ChatViewModel(
                    savedStateHandle = savedStateHandle,
                    chatRepository = container.chatRepository,
                    settingsRepository = container.settingsRepository,
                    networkMonitor = container.networkMonitor,
                    notificationHelper = container.notificationHelper
                ) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}