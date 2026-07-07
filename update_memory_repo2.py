with open('app/src/main/java/com/example/data/repository/MemoryRepository.kt', 'r') as f:
    code = f.read()

code = code.replace(
    'suspend fun deleteMemory(id: String) {\n        memoryDao.deleteMemoryItemById(id)\n    }',
    'suspend fun deleteMemory(id: String) {\n        memoryDao.deleteMemoryItemById(id)\n    }\n\n    suspend fun deleteAllMemories() {\n        memoryDao.deleteAllMemories()\n    }'
)

with open('app/src/main/java/com/example/data/repository/MemoryRepository.kt', 'w') as f:
    f.write(code)
