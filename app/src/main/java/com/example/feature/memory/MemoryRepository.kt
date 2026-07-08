package com.example.feature.memory

import com.example.core.database.MemoryDao
import com.example.core.database.MemoryItem

import kotlinx.coroutines.flow.Flow

class MemoryRepository(private val memoryDao: MemoryDao) {
    val allMemories: Flow<List<MemoryItem>> = memoryDao.getAllMemoryItems()

    suspend fun addMemory(content: String) {
        val id = java.util.UUID.randomUUID().toString()
        val item = MemoryItem(id, content)
        memoryDao.insertMemoryItem(item)
    }

    suspend fun deleteMemory(id: String) {
        memoryDao.deleteMemoryItemById(id)
    }

    suspend fun deleteAllMemories() {
        memoryDao.deleteAllMemories()
    }
}
