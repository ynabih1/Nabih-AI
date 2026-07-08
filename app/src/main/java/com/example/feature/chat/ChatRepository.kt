package com.example.feature.chat

import com.example.core.database.Conversation
import com.example.core.database.ConversationDao
import com.example.core.database.Folder
import com.example.core.database.FolderDao
import com.example.core.database.MemoryDao
import com.example.core.database.Message
import com.example.core.database.MessageDao
import com.example.core.model.AiModel
import com.example.core.model.ApiProvider
import com.example.core.network.ClaudeMessage
import com.example.core.network.ClaudeRequest
import com.example.core.network.GeminiContent
import com.example.core.network.GeminiGenerationConfig
import com.example.core.network.GeminiInlineData
import com.example.core.network.GeminiPart
import com.example.core.network.GeminiRequest
import com.example.core.network.NetworkClient
import com.example.core.network.OpenAiMessage
import com.example.core.network.OpenAiRequest
import com.example.feature.settings.SettingsRepository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.*

class ChatRepository(
    private val context: Context,
    private val folderDao: FolderDao,
    private val conversationDao: ConversationDao,
    private val messageDao: MessageDao,
    private val memoryDao: MemoryDao,
    private val settingsRepository: SettingsRepository
) {
    // --- Room Database Fetchers ---

    val folders: Flow<List<Folder>> = folderDao.getAllFolders()
    val activeConversations: Flow<List<Conversation>> = conversationDao.getActiveConversations()
    val archivedConversations: Flow<List<Conversation>> = conversationDao.getArchivedConversations()

    fun getConversationsInFolder(folderId: String): Flow<List<Conversation>> =
        conversationDao.getConversationsInFolder(folderId)

    fun getMessagesForConversation(conversationId: String): Flow<List<Message>> =
        messageDao.getMessagesForConversation(conversationId)

    // --- Write Database Operations ---

    suspend fun createFolder(name: String): String = withContext(Dispatchers.IO) {
        val id = UUID.randomUUID().toString()
        folderDao.insertFolder(Folder(id, name))
        id
    }

    suspend fun deleteFolder(folder: Folder) = withContext(Dispatchers.IO) {
        folderDao.deleteFolder(folder)
    }

    suspend fun createConversation(title: String, modelId: String, folderId: String? = null, isTemporary: Boolean = false): String = withContext(Dispatchers.IO) {
        val id = if (isTemporary) "temp_" + UUID.randomUUID().toString() else UUID.randomUUID().toString()
        val conversation = Conversation(
            id = id,
            title = if (isTemporary) "Temporary Chat" else title,
            modelId = modelId,
            folderId = folderId
        )
        conversationDao.insertConversation(conversation)
        id
    }

    suspend fun getConversationById(id: String): Conversation? = withContext(Dispatchers.IO) {
        conversationDao.getConversationById(id)
    }

    suspend fun updateConversation(conversation: Conversation) = withContext(Dispatchers.IO) {
        conversationDao.updateConversation(conversation)
    }

    suspend fun deleteConversation(id: String) = withContext(Dispatchers.IO) {
        conversationDao.deleteConversationById(id)
    }

    suspend fun deleteAllConversations() = withContext(Dispatchers.IO) {
        conversationDao.deleteAllConversations()
        messageDao.deleteAllMessages()
    }

    suspend fun insertMessage(message: Message) = withContext(Dispatchers.IO) {
        messageDao.insertMessage(message)
        // Update conversation's updatedAt timestamp
        val conv = conversationDao.getConversationById(message.conversationId)
        if (conv != null) {
            conversationDao.updateConversation(conv.copy(updatedAt = System.currentTimeMillis()))
        }
    }

    suspend fun deleteConversationMessages(conversationId: String) = withContext(Dispatchers.IO) {
        messageDao.deleteMessagesForConversation(conversationId)
    }

    // --- Document Parser Helper ---

    suspend fun copyUriToCache(uri: Uri, onProgress: (Float) -> Unit): Uri? = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            var name = "file_attachment"
            var size = 0L
            
            try {
                contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                    if (cursor.moveToFirst()) {
                        if (nameIndex >= 0) name = cursor.getString(nameIndex) ?: name
                        if (sizeIndex >= 0) size = cursor.getLong(sizeIndex)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("ChatRepository", "Failed to query file metadata", e)
            }
            
            val attachedDir = java.io.File(context.cacheDir, "attached_files").apply { mkdirs() }
            val ext = name.substringAfterLast('.', "")
            val safeName = "attach_${System.currentTimeMillis()}.${if (ext.isNotEmpty()) ext else "bin"}"
            val destFile = java.io.File(attachedDir, safeName)
            
            val inputStream = contentResolver.openInputStream(uri) ?: return@withContext null
            val outputStream = java.io.FileOutputStream(destFile)
            
            val buffer = ByteArray(16 * 1024)
            var bytesRead: Int
            var totalBytesRead = 0L
            
            onProgress(0.05f)
            
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                totalBytesRead += bytesRead
                if (size > 0) {
                    val rawProgress = totalBytesRead.toFloat() / size
                    onProgress(0.05f + rawProgress * 0.9f)
                }
            }
            
            inputStream.close()
            outputStream.flush()
            outputStream.close()
            
            onProgress(1.0f)
            Uri.fromFile(destFile)
        } catch (e: Exception) {
            android.util.Log.e("ChatRepository", "Failed to copy URI to cache", e)
            null
        }
    }

    suspend fun parseAttachedDocument(uri: Uri): Pair<String, String> = withContext(Dispatchers.IO) {
        val fileName = getFileName(uri) ?: "Attached Document"
        val extension = fileName.substringAfterLast('.', "").lowercase()
        var textContent = ""

        try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            if (inputStream != null) {
                if (extension == "txt" || extension == "csv" || extension == "json" || extension == "md") {
                    textContent = inputStream.bufferedReader().use { it.readText() }
                } else {
                    val bytes = inputStream.readBytes()
                    textContent = "[Binary Document Attached: ${extension.uppercase()}, size: ${bytes.size} bytes. The actual file data is provided natively to the model if supported.]"
                }
            }
        } catch (e: Exception) {
            textContent = "[Failed to parse document content: ${e.localizedMessage}]"
        }

        Pair(fileName, textContent)
    }

    fun getFileName(uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (index >= 0) {
                        result = cursor.getString(index)
                    }
                }
            } finally {
                cursor?.close()
            }
        } else if (uri.scheme == "file") {
            result = uri.lastPathSegment
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/')
            if (cut != null && cut != -1) {
                result = result?.substring(cut + 1)
            }
        }
        return result
    }

    fun getFileSizeString(uri: Uri): String {
        return try {
            val path = uri.path ?: ""
            val file = java.io.File(path)
            if (file.exists()) {
                val size = file.length()
                if (size < 1024) "$size B"
                else if (size < 1024 * 1024) String.format(Locale.US, "%.1f KB", size.toFloat() / 1024)
                else String.format(Locale.US, "%.1f MB", size.toFloat() / (1024 * 1024))
            } else {
                ""
            }
        } catch (e: Exception) {
            ""
        }
    }

    fun uriToBase64(uri: Uri): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
            Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
        } catch (e: Exception) {
            null
        }
    }

    fun fileUriToBase64(uri: Uri): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val bytes = inputStream.readBytes()
            inputStream.close()
            Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            null
        }
    }

    // --- Real AI Query Engine with Multi-model & Streaming support ---

    fun streamChatResponse(
        conversationId: String,
        prompt: String,
        attachedImageUri: Uri? = null,
        attachedDocUri: Uri? = null,
        searchEnabled: Boolean = false
    ): Flow<String> = flow {
        val settings = settingsRepository.settings.value
        val conversation = conversationDao.getConversationById(conversationId) ?: return@flow
        val model = AiModel.fromId(conversation.modelId)

        // Auto-rename conversation title if it starts with "New Chat"
        if (conversation.title.startsWith("New Chat")) {
            val words = prompt.trim().split("\\s+".toRegex())
            val automaticTitle = if (words.size > 5) {
                words.take(5).joinToString(" ") + "..."
            } else {
                prompt
            }
            val capitalizedTitle = automaticTitle.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
            conversationDao.updateConversation(conversation.copy(title = capitalizedTitle, updatedAt = System.currentTimeMillis()))
        }

        // Load conversation history for context
        val history = messageDao.getMessagesForConversationSync(conversationId)

        // Compile memory system context to inject into system prompt
        val list = memoryDao.getAllMemoryItems().first()
        var memoriesStr = ""
        if (list.isNotEmpty()) {
            memoriesStr = "Nabih remembered the following personal facts about the user:\n" +
                    list.joinToString("\n") { "- ${it.content}" }
        }
        var searchContext = ""
        if (searchEnabled) {
            try {
                val wikiResponse = NetworkClient.wikipediaService.searchWikipedia(prompt)
                val topResults = wikiResponse.query?.search?.take(3)?.joinToString("\n") {
                    "${it.title}: ${it.snippet.replace(Regex("<[^>]*>"), "")}"
                } ?: "No results found."
                searchContext = "\n[WEB SEARCH RESULTS for '${prompt}':\n$topResults\nProvide the most accurate answer based on these search results.]\n"
            } catch (e: Exception) {
                searchContext = "\n[WEB SEARCH FAILED: ${e.message}]\n"
            }
        }

        val systemPrompt = "You are Nabih AI, a fast, efficient AI assistant. " +
                "You speak Arabic and English natively. " +
                "CRITICAL INSTRUCTIONS: You must provide concise and direct answers. Avoid unnecessary explanations, introductions, or repeated information. Answer exactly what the user asks using the shortest accurate answer possible. Do not use greetings or filler phrases. Do not repeat the user's question. Add details only when explicitly requested. If information is unknown, say so briefly. Provide accurate, structured answers. " +
                "Current local time: ${System.currentTimeMillis()}.\n$memoriesStr\n$searchContext"

        // Build active prompt including attachments if any
        var activePrompt = prompt
        var attachedBase64Image: String? = null
        var attachedDocText: String? = null

        if (attachedImageUri != null) {
            attachedBase64Image = uriToBase64(attachedImageUri)
            activePrompt += "\n[An image attachment is sent. Understand and reference the attached image contents in your response.]"
        }

        if (attachedDocUri != null) {
            val (docName, docText) = parseAttachedDocument(attachedDocUri)
            attachedDocText = docText
            activePrompt += "\n[Document Attached: $docName. Use the contents for reasoning:\n$attachedDocText]"
        }

        var actualModel = model
        var actualModelId = actualModel.id
        var finalApiKey = ""

        when (actualModel.provider) {
            ApiProvider.NABIH -> {
                finalApiKey = com.example.BuildConfig.GEMINI_API_KEY
                // Removed API Key Required exception for Nabih to allow it to work locally/without API as requested.
                actualModelId = "gemini-flash-latest" // use gemini under the hood
            }
            ApiProvider.GOOGLE -> {
                finalApiKey = settings.googleApiKey
                if (finalApiKey.isEmpty()) throw Exception("API Key Required: Google Gemini API Key is missing. Please add it in API Keys settings.")
            }
            ApiProvider.OPENAI -> {
                finalApiKey = settings.openaiApiKey
                if (finalApiKey.isEmpty()) throw Exception("API Key Required: OpenAI API Key is missing.")
            }
            ApiProvider.ANTHROPIC -> {
                finalApiKey = settings.anthropicApiKey
                if (finalApiKey.isEmpty()) throw Exception("API Key Required: Anthropic API Key is missing.")
            }
        }

        // Retry mechanism
        val maxRetries = 3
        var currentTry = 0
        var success = false
        var responseText = ""
        var lastException: Exception? = null

        while (currentTry < maxRetries && !success) {
            try {
                when (actualModel.provider) {
                    ApiProvider.GOOGLE, ApiProvider.NABIH -> {
                        val geminiContents = mutableListOf<GeminiContent>()
                        history.forEach { msg ->
                            val parts = mutableListOf<GeminiPart>()
                            parts.add(GeminiPart(text = msg.content))
                            geminiContents.add(GeminiContent(role = if (msg.role == "user") "user" else "model", parts = parts))
                        }
                        val activeParts = mutableListOf<GeminiPart>()
                        if (attachedBase64Image != null) {
                            activeParts.add(GeminiPart(inlineData = GeminiInlineData("image/jpeg", attachedBase64Image)))
                        }
                        
                        // Check if we have an attached PDF to send to Gemini as inlineData
                        if (attachedDocUri != null) {
                            val fileName = getFileName(attachedDocUri) ?: ""
                            val ext = fileName.substringAfterLast('.', "").lowercase()
                            if (ext == "pdf") {
                                val attachedBase64Doc = fileUriToBase64(attachedDocUri)
                                if (attachedBase64Doc != null) {
                                    activeParts.add(GeminiPart(inlineData = GeminiInlineData("application/pdf", attachedBase64Doc)))
                                }
                            }
                        }
                        
                        activeParts.add(GeminiPart(text = activePrompt))
                        geminiContents.add(GeminiContent(role = "user", parts = activeParts))

                        val req = GeminiRequest(
                            contents = geminiContents,
                            systemInstruction = GeminiContent(parts = listOf(GeminiPart(text = systemPrompt))),
                            generationConfig = GeminiGenerationConfig(temperature = 0.7f)
                        )
                        val response = NetworkClient.geminiService.generateContent(actualModelId, finalApiKey, req)
                        responseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                            ?: "No text response generated by Google Gemini."
                    }
                    ApiProvider.OPENAI -> {
                        val messages = mutableListOf<OpenAiMessage>()
                        messages.add(OpenAiMessage("system", systemPrompt))
                        history.forEach { msg ->
                            messages.add(OpenAiMessage(if (msg.role == "user") "user" else "assistant", msg.content))
                        }
                        messages.add(OpenAiMessage("user", activePrompt))
                        val req = OpenAiRequest(
                            model = actualModelId,
                            messages = messages,
                            temperature = 0.7f
                        )
                        val authHeader = "Bearer $finalApiKey"
                        val service = NetworkClient.openAiService
                        val response = service.generateCompletion(authHeader, req)
                        responseText = response.choices?.firstOrNull()?.message?.content ?: "No text response generated."
                    }
                    ApiProvider.ANTHROPIC -> {
                        val messages = mutableListOf<ClaudeMessage>()
                        history.forEach { msg ->
                            messages.add(ClaudeMessage(if (msg.role == "user") "user" else "assistant", msg.content))
                        }
                        messages.add(ClaudeMessage("user", activePrompt))
                        val req = ClaudeRequest(
                            model = actualModel.id,
                            messages = messages,
                            system = systemPrompt,
                            temperature = 0.7f
                        )
                        val response = NetworkClient.claudeService.generateMessage(apiKey = finalApiKey, request = req)
                        responseText = response.content?.firstOrNull()?.text ?: "No text response generated."
                    }
                }
                success = true
            } catch (e: Exception) {
                lastException = e

                android.util.Log.e("ChatRepository", "API Request failed on try $currentTry", e)
                currentTry++
                if (currentTry < maxRetries) {
                    kotlinx.coroutines.delay(1000L * currentTry) // Exponential backoff
                }
            }
        }

        if (!success) {
            val errorMsg = lastException?.localizedMessage ?: "Failed to connect to AI provider after $maxRetries attempts."
            val fullError = if (model.provider == ApiProvider.NABIH) "Nabih Ultra Service Error: $errorMsg" else errorMsg
            throw Exception(fullError)
        }

        emit(responseText)

    }.flowOn(Dispatchers.IO)

    suspend fun duplicateConversation(originalId: String): String = withContext(Dispatchers.IO) {
        val original = conversationDao.getConversationById(originalId) ?: return@withContext ""
        val newId = UUID.randomUUID().toString()
        val duplicated = original.copy(
            id = newId,
            title = original.title + " (Copy)",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        conversationDao.insertConversation(duplicated)
        
        // Copy messages
        val originalMessages = messageDao.getMessagesForConversationSync(originalId)
        originalMessages.forEach { msg ->
            val newMsg = msg.copy(
                id = UUID.randomUUID().toString(),
                conversationId = newId,
                timestamp = msg.timestamp
            )
            messageDao.insertMessage(newMsg)
        }
        newId
    }

    suspend fun renameConversation(conversationId: String, newTitle: String) = withContext(Dispatchers.IO) {
        val original = conversationDao.getConversationById(conversationId) ?: return@withContext
        conversationDao.updateConversation(original.copy(title = newTitle, updatedAt = System.currentTimeMillis()))
    }
}
