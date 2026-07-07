with open('app/src/main/java/com/example/data/database/Daos.kt', 'r') as f:
    code = f.read()

code = code.replace(
    'suspend fun deleteConversationById(id: String)',
    'suspend fun deleteConversationById(id: String)\n\n    @Query("DELETE FROM conversations")\n    suspend fun deleteAllConversations()'
)

code = code.replace(
    'suspend fun deleteMessagesForConversation(conversationId: String)',
    'suspend fun deleteMessagesForConversation(conversationId: String)\n\n    @Query("DELETE FROM messages")\n    suspend fun deleteAllMessages()'
)

code = code.replace(
    'suspend fun deleteFolder(folder: Folder)',
    'suspend fun deleteFolder(folder: Folder)\n\n    @Query("DELETE FROM folders")\n    suspend fun deleteAllFolders()'
)

code = code.replace(
    'suspend fun deleteMemoryItemById(id: String)',
    'suspend fun deleteMemoryItemById(id: String)\n\n    @Query("DELETE FROM memory_items")\n    suspend fun deleteAllMemories()'
)

with open('app/src/main/java/com/example/data/database/Daos.kt', 'w') as f:
    f.write(code)

