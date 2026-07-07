with open('app/src/main/java/com/example/data/repository/ChatRepository.kt', 'r') as f:
    code = f.read()

code = code.replace(
    'suspend fun deleteConversation(id: String) = withContext(Dispatchers.IO) {\n        conversationDao.deleteConversationById(id)\n    }',
    'suspend fun deleteConversation(id: String) = withContext(Dispatchers.IO) {\n        conversationDao.deleteConversationById(id)\n    }\n\n    suspend fun deleteAllConversations() = withContext(Dispatchers.IO) {\n        conversationDao.deleteAllConversations()\n        messageDao.deleteAllMessages()\n    }'
)

with open('app/src/main/java/com/example/data/repository/ChatRepository.kt', 'w') as f:
    f.write(code)
