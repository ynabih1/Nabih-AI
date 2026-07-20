package com.example.data.repository

import com.example.data.local.Conversation
import com.example.data.local.ConversationDao
import com.example.data.local.Folder
import com.example.data.local.FolderDao
import com.example.data.local.MemoryDao
import com.example.data.local.Message
import com.example.data.local.MessageDao
import com.example.data.local.AttachmentItem
import com.example.data.local.ErrorLog
import com.example.data.local.ErrorLogDao
import com.example.model.AiModel
import com.example.model.ApiProvider
import com.example.data.remote.ClaudeMessage
import com.example.data.remote.ClaudeRequest
import com.example.data.remote.GeminiContent
import com.example.data.remote.GeminiGenerationConfig
import com.example.data.remote.GeminiInlineData
import com.example.data.remote.GeminiPart
import com.example.data.remote.GeminiRequest
import com.example.data.remote.NetworkClient
import com.example.data.remote.OpenAiMessage
import com.example.data.remote.OpenAiRequest
import com.example.data.repository.SettingsRepository

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
import kotlinx.coroutines.withContext
import kotlinx.coroutines.async
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.*

class ChatRepository(
    private val context: Context,
    private val folderDao: FolderDao,
    private val conversationDao: ConversationDao,
    private val messageDao: MessageDao,
    private val memoryDao: MemoryDao,
    private val errorLogDao: ErrorLogDao,
    private val settingsRepository: SettingsRepository
) {
    private val responseCache = java.util.concurrent.ConcurrentHashMap<String, String>()
    private val activeRequests = java.util.concurrent.ConcurrentHashMap<String, kotlinx.coroutines.Deferred<String>>()
    private val lastRequestTimes = java.util.concurrent.ConcurrentHashMap<String, Long>()
    private val repositoryScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO + kotlinx.coroutines.SupervisorJob())

    // --- Room Database Fetchers ---

    val folders: Flow<List<Folder>> = folderDao.getAllFolders()
    val activeConversations: Flow<List<Conversation>> = conversationDao.getActiveConversations()
    val archivedConversations: Flow<List<Conversation>> = conversationDao.getArchivedConversations()

    fun getConversationsInFolder(folderId: String): Flow<List<Conversation>> =
        conversationDao.getConversationsInFolder(folderId)

    fun getMessagesForConversation(conversationId: String): Flow<List<Message>> =
        messageDao.getMessagesForConversation(conversationId)

    fun getAttachments(): Flow<List<AttachmentItem>> =
        messageDao.getAttachmentsWithConversations()

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

    suspend fun deleteMessageById(id: String) = withContext(Dispatchers.IO) {
        messageDao.deleteMessageById(id)
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
        val textContent = com.example.utils.DocumentParser.parseUri(context, uri)
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

    private val conversationLocks = java.util.concurrent.ConcurrentHashMap<String, kotlinx.coroutines.sync.Mutex>()

    private fun findEndQuoteIndex(s: String): Int {
        var escaped = false
        for (i in 1 until s.length) {
            val c = s[i]
            if (escaped) {
                escaped = false
            } else if (c == '\\') {
                escaped = true
            } else if (c == '"') {
                return i
            }
        }
        return -1
    }

    private fun unescapeJsonString(s: String): String {
        val sb = java.lang.StringBuilder()
        var i = 0
        while (i < s.length) {
            val c = s[i]
            if (c == '\\' && i + 1 < s.length) {
                val next = s[i + 1]
                when (next) {
                    'n' -> sb.append('\n')
                    't' -> sb.append('\t')
                    'r' -> sb.append('\r')
                    'b' -> sb.append('\b')
                    'f' -> sb.append('\u000c')
                    '\\' -> sb.append('\\')
                    '"' -> sb.append('"')
                    '/' -> sb.append('/')
                    'u' -> {
                        if (i + 5 < s.length) {
                            try {
                                val hex = s.substring(i + 2, i + 6)
                                val code = hex.toInt(16)
                                sb.append(code.toChar())
                                i += 4
                            } catch (e: Exception) {
                                sb.append("\\u")
                            }
                        } else {
                            sb.append("\\u")
                        }
                    }
                    else -> sb.append(next)
                }
                i += 2
            } else {
                sb.append(c)
                i++
            }
        }
        return sb.toString()
    }

    private fun extractTextFromLine(line: String): String? {
        val textMarker = "\"text\":"
        val index = line.indexOf(textMarker)
        if (index == -1) return null
        
        val afterMarker = line.substring(index + textMarker.length).trim()
        if (afterMarker.startsWith("\"")) {
            val endIndex = findEndQuoteIndex(afterMarker)
            if (endIndex != -1) {
                val rawValue = afterMarker.substring(1, endIndex)
                return unescapeJsonString(rawValue)
            }
        }
        return null
    }

    fun streamChatResponse(
        conversationId: String,
        prompt: String,
        attachedImageUri: Uri? = null,
        attachedDocUri: Uri? = null,
        searchEnabled: Boolean = false
    ): Flow<String> = flow {
        val settings = settingsRepository.settings.value
        val conversation = conversationDao.getConversationById(conversationId) ?: throw Exception("Conversation not found")
        val registryModel = com.example.model.ModelRegistry.getModels(context).find { it.id == conversation.modelId }
            ?: throw Exception("Model not found.")
            
        val isArabic = settings.language == com.example.model.AppLanguage.ARABIC
        val conversationLock = conversationLocks.getOrPut(conversationId) { kotlinx.coroutines.sync.Mutex() }
        
        conversationLock.lock()
        try {
            val history = messageDao.getMessagesForConversationSync(conversationId)
            var memoriesStr = ""
            if (settings.memoryEnabled) {
                val list = memoryDao.getAllMemoryItems().first()
                if (list.isNotEmpty()) {
                    memoriesStr = "Nabih remembered the following personal facts about the user:\n" +
                            list.joinToString("\n") { "- ${it.content}" }
                }
            }
            
            var searchContext = ""
            if (searchEnabled) {
                try {
                    val wikiResponse = com.example.data.remote.NetworkClient.wikipediaService.searchWikipedia(prompt)
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
            
            // Generate cache key
            val cacheKey = "conv_${conversationId}_prompt_${prompt.hashCode()}_img_${attachedBase64Image?.hashCode() ?: 0}_doc_${attachedDocText?.hashCode() ?: 0}_model_${registryModel.id}"
            if (responseCache.containsKey(cacheKey)) {
                val cachedText = responseCache[cacheKey]!!
                var currentLength = 0
                val chunkSize = 4
                while (currentLength < cachedText.length) {
                    currentLength = (currentLength + chunkSize).coerceAtMost(cachedText.length)
                    emit(cachedText.substring(0, currentLength))
                    kotlinx.coroutines.delay(10)
                }
                return@flow
            }
            
            var responseText = ""
            com.example.data.remote.AiRouter.routeStreaming(
                context = context,
                registryModel = registryModel,
                settings = settings,
                systemPrompt = systemPrompt,
                prompt = activePrompt,
                history = history,
                attachedBase64Image = attachedBase64Image,
                attachedDocText = attachedDocText,
                isArabic = isArabic
            ).collect { currentFullText ->
                responseText = currentFullText
                emit(currentFullText)
            }
            
            responseCache[cacheKey] = responseText
        } finally {
            conversationLock.unlock()
        }
    }.flowOn(kotlinx.coroutines.Dispatchers.IO)
    
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

    suspend fun generateImage(prompt: String, aspectRatio: String): String? = withContext(Dispatchers.IO) {
        val settings = settingsRepository.settings.value
        val finalApiKey = settings.googleApiKey.ifBlank { settings.nabihApiKey.ifBlank { com.example.BuildConfig.GEMINI_API_KEY } }
        if (finalApiKey.isEmpty()) return@withContext null

        val req = com.example.data.remote.GeminiRequest(
            contents = listOf(com.example.data.remote.GeminiContent(parts = listOf(com.example.data.remote.GeminiPart(text = prompt)))),
            generationConfig = com.example.data.remote.GeminiGenerationConfig(
                responseModalities = listOf("TEXT", "IMAGE"),
                imageConfig = com.example.data.remote.GeminiImageConfig(aspectRatio = aspectRatio, imageSize = "1K")
            )
        )
        try {
            val response = com.example.data.remote.NetworkClient.geminiService.generateContent("gemini-2.5-flash-image", finalApiKey, req)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull { it.inlineData != null }?.inlineData?.data
        } catch (e: Exception) {
            android.util.Log.e("ChatRepository", "Image Generation failed", e)
            null
        }
    }

    suspend fun insertErrorLog(errorType: String, provider: String) = withContext(Dispatchers.IO) {
        errorLogDao.insertErrorLog(ErrorLog(errorType = errorType, provider = provider))
    }
}
