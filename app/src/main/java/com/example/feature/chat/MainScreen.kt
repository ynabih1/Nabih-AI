package com.example.feature.chat

import com.example.R

import com.example.core.database.Conversation
import com.example.core.database.Folder
import com.example.core.database.Message
import com.example.core.model.AiModel
import com.example.core.model.AppLanguage
import com.example.core.model.AppSettings
import com.example.feature.settings.SettingsViewModel
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Check

import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

fun generateAutoTitle(text: String): String {
    val firstLine = text.lineSequence().firstOrNull()?.trim() ?: "New Chat"
    val cleanText = firstLine.replace(Regex("\\[REASONING MODE:.*?\\]"), "").trim()
    if (cleanText.isEmpty()) return "New Chat"
    return if (cleanText.length > 25) {
        val truncated = cleanText.take(25)
        val lastSpace = truncated.lastIndexOf(' ')
        if (lastSpace > 10) {
            truncated.take(lastSpace) + "..."
        } else {
            truncated + "..."
        }
    } else {
        cleanText
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MainScreen(
    homeViewModel: com.example.feature.chat.HomeViewModel,
    chatViewModel: com.example.feature.chat.ChatViewModel,
    settingsViewModel: com.example.feature.settings.SettingsViewModel,
    onNavigateToSettings: () -> Unit,
    onNavigateToVoice: () -> Unit,
    onNavigateToRoute: (String) -> Unit = {}
) {
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
    val isArabic = settings.language == AppLanguage.ARABIC
    val activeConversations by homeViewModel.activeConversations.collectAsStateWithLifecycle(emptyList())
    
    val chatState by chatViewModel.uiState.collectAsStateWithLifecycle()
    val isGenerating by chatViewModel.isGenerating.collectAsStateWithLifecycle()
    val streamResponse by chatViewModel.currentStreamingResponse.collectAsStateWithLifecycle()
    val selectedModel by chatViewModel.selectedModel.collectAsStateWithLifecycle()
    val inputText by chatViewModel.currentInputText.collectAsStateWithLifecycle()
    val searchEnabled by chatViewModel.searchEnabled.collectAsStateWithLifecycle()
    val searchQuery by chatViewModel.searchQuery.collectAsStateWithLifecycle()
    val context = LocalContext.current
    
    
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    LaunchedEffect(selectedModel, settings) {
        if (selectedModel.id != com.example.core.model.AiModel.NABIH_ULTRA.id) {
            val isProviderValid = when (selectedModel.provider) {
                com.example.core.model.ApiProvider.NABIH -> true
                com.example.core.model.ApiProvider.GOOGLE -> settings.googleApiKey.isNotEmpty() || settings.nabihApiKey.isNotEmpty()
                com.example.core.model.ApiProvider.OPENAI -> settings.openaiApiKey.isNotEmpty()
                com.example.core.model.ApiProvider.ANTHROPIC -> settings.anthropicApiKey.isNotEmpty()
                                        com.example.core.model.ApiProvider.GROK -> settings.grokApiKey.isNotEmpty()
                                        com.example.core.model.ApiProvider.DEEPSEEK -> settings.deepseekApiKey.isNotEmpty()
                                        com.example.core.model.ApiProvider.MISTRAL -> settings.mistralApiKey.isNotEmpty()
                                        com.example.core.model.ApiProvider.OPENROUTER -> settings.openRouterApiKey.isNotEmpty()
                                        com.example.core.model.ApiProvider.OLLAMA -> settings.ollamaEndpoint.isNotEmpty()
                                        com.example.core.model.ApiProvider.LMSTUDIO -> settings.lmStudioEndpoint.isNotEmpty()
                com.example.core.model.ApiProvider.GROK -> settings.grokApiKey.isNotEmpty()
                com.example.core.model.ApiProvider.DEEPSEEK -> settings.deepseekApiKey.isNotEmpty()
                com.example.core.model.ApiProvider.MISTRAL -> settings.mistralApiKey.isNotEmpty()
                com.example.core.model.ApiProvider.OPENROUTER -> settings.openRouterApiKey.isNotEmpty()
                com.example.core.model.ApiProvider.OLLAMA -> settings.ollamaEndpoint.isNotEmpty()
                com.example.core.model.ApiProvider.LMSTUDIO -> settings.lmStudioEndpoint.isNotEmpty()
            }
            if (!isProviderValid) {
                chatViewModel.selectModel(com.example.core.model.AiModel.NABIH_ULTRA)
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
                        MainDrawerContent(
                settings = settings,
                selectedModel = selectedModel,
                onSelectModel = { chatViewModel.selectModel(it) },
                conversations = activeConversations,
                onSelectConversation = { id ->
                    chatViewModel.selectConversation(id)
                },
                onNewChat = {
                    chatViewModel.cleanupIfTemporary() // Cleanup current if needed
                    chatViewModel.createNewChat(settings.defaultModel.id)
                },
                onNavigateToSettings = onNavigateToSettings,
                onNavigateTo = { route -> 
                    if (route == "search") {
                        chatViewModel.toggleSearch()
                    } else {
                        onNavigateToRoute(route)
                    }
                },
                onCloseDrawer = {
                    scope.launch { drawerState.close() }
                },
                onRenameConversation = { id, title -> homeViewModel.renameConversation(id, title) },
                onDeleteConversation = { id -> homeViewModel.deleteConversation(id) },
                onTogglePinConversation = { conv -> homeViewModel.togglePinConversation(conv) },
                onToggleArchiveConversation = { conv -> homeViewModel.toggleArchiveConversation(conv) }
            )
        }
    ) {
        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(id = R.drawable.logo),
                                contentDescription = null,
                                tint = androidx.compose.ui.graphics.Color.Unspecified,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Nabih AI",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    },
                    actions = {
                        val hasApiKey = settings.googleApiKey.isNotEmpty() || 
                                        settings.nabihApiKey.isNotEmpty() || 
                                        settings.openaiApiKey.isNotEmpty() || 
                                        settings.anthropicApiKey.isNotEmpty()
                                        
                        if (hasApiKey) {
                            var modelMenuExpanded by remember { mutableStateOf(false) }
                            Box {
                                TextButton(
                                    onClick = { modelMenuExpanded = true },
                                    modifier = Modifier.padding(end = 4.dp),
                                    colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                ) {
                                    Text(text = selectedModel.displayName.split(" ").firstOrNull() ?: selectedModel.displayName, style = MaterialTheme.typography.labelLarge)
                                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
                                }
                                DropdownMenu(
                                expanded = modelMenuExpanded,
                                onDismissRequest = { modelMenuExpanded = false },
                                modifier = Modifier.width(320.dp).background(MaterialTheme.colorScheme.surface)
                            ) {
                                val registryModels = com.example.core.model.ModelRegistry.getModels(context).filter { model ->
                                    when (model.provider) {
                                        com.example.core.model.ApiProvider.NABIH -> true
                                        com.example.core.model.ApiProvider.GOOGLE -> {
                                            settings.googleApiKey.isNotEmpty() || 
                                             settings.nabihApiKey.isNotEmpty()
                                        }
                                        com.example.core.model.ApiProvider.OPENAI -> settings.openaiApiKey.isNotEmpty()
                                        com.example.core.model.ApiProvider.ANTHROPIC -> settings.anthropicApiKey.isNotEmpty()
                                        com.example.core.model.ApiProvider.GROK -> settings.grokApiKey.isNotEmpty()
                                        com.example.core.model.ApiProvider.DEEPSEEK -> settings.deepseekApiKey.isNotEmpty()
                                        com.example.core.model.ApiProvider.MISTRAL -> settings.mistralApiKey.isNotEmpty()
                                        com.example.core.model.ApiProvider.OPENROUTER -> settings.openRouterApiKey.isNotEmpty()
                                        com.example.core.model.ApiProvider.OLLAMA -> settings.ollamaEndpoint.isNotEmpty()
                                        com.example.core.model.ApiProvider.LMSTUDIO -> settings.lmStudioEndpoint.isNotEmpty()
                                    }
                                }
                                
                                // Dynamic Sync Button
                                var isRefreshing by remember { mutableStateOf(false) }
                                val coroutineScope = rememberCoroutineScope()
                                
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            if (isRefreshing) {
                                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                            } else {
                                                Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(if (isArabic) "تحديث النماذج تلقائياً" else "Sync Model Registry", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                        }
                                    },
                                    onClick = {
                                        if (!isRefreshing) {
                                            isRefreshing = true
                                            coroutineScope.launch {
                                                com.example.core.model.ModelRegistry.syncAndRefresh(context,
                                                    onSuccess = {
                                                        isRefreshing = false
                                                        android.widget.Toast.makeText(context, if (isArabic) "تم تحديث النماذج!" else "Model Registry synced!", android.widget.Toast.LENGTH_SHORT).show()
                                                    },
                                                    onFailure = {
                                                        isRefreshing = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                
                                registryModels.forEach { modelMetadata ->
                                    val modelEnum = com.example.core.model.AiModel.values().find { it.id == modelMetadata.id } ?: com.example.core.model.AiModel.NABIH_ULTRA
                                    val isLocked = false
                                    DropdownMenuItem(
                                        text = { 
                                            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    if (isLocked) {
                                                        Icon(Icons.Outlined.Lock, contentDescription = "Locked", modifier = Modifier.size(16.dp).padding(end = 4.dp))
                                                    } else {
                                                        Icon(
                                                            imageVector = if (modelMetadata.isDeprecated) Icons.Outlined.Warning else Icons.Outlined.Check,
                                                            contentDescription = "Status",
                                                            modifier = Modifier.size(16.dp).padding(end = 4.dp),
                                                            tint = if (modelMetadata.isDeprecated) MaterialTheme.colorScheme.error 
                                                                   else if (selectedModel.id == modelMetadata.id) MaterialTheme.colorScheme.primary 
                                                                   else androidx.compose.ui.graphics.Color.Transparent
                                                        )
                                                    }
                                                    Text(
                                                        text = modelMetadata.displayName,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = if (isLocked) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface
                                                    )
                                                    
                                                }
                                                
                                                if (modelMetadata.isDeprecated) {
                                                    Text(
                                                        text = if (isArabic) "سيتوقف قريباً! البديل: ${modelMetadata.fallbackModelId}" else "Deprecated! Migrates to: ${modelMetadata.fallbackModelId}",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.error,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(start = 16.dp)
                                                    )
                                                } else if (modelMetadata.status == com.example.core.model.ModelStatus.MAINTENANCE) {
                                                    Text(
                                                        text = if (isArabic) "صيانة: غير متوفر مؤقتاً" else "Maintenance Mode",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = androidx.compose.ui.graphics.Color(0xFFE65100),
                                                        fontSize = 10.sp,
                                                        modifier = Modifier.padding(start = 16.dp)
                                                    )
                                                }
                                                
                                                Row(
                                                    modifier = Modifier.padding(start = 16.dp, top = 4.dp),
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    val caps = modelMetadata.capabilities
                                                    if (caps.text) Icon(Icons.Outlined.Notes, "Text", modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
                                                    if (caps.vision) Icon(Icons.Outlined.Visibility, "Vision", modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
                                                    if (caps.audio) Icon(Icons.Outlined.Mic, "Audio", modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
                                                    if (caps.reasoning) Icon(Icons.Outlined.Lightbulb, "Reasoning", modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
                                                    if (caps.imageGeneration) Icon(Icons.Outlined.Image, "Image Gen", modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
                                                    if (caps.fileAnalysis) Icon(Icons.Outlined.Description, "File Analysis", modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
                                                }
                                            }
                                        },
                                        onClick = {
                                            if (isLocked) {
                                                android.widget.Toast.makeText(context, if (isArabic) "مفتاح API مطلوب" else "API Key Required", android.widget.Toast.LENGTH_SHORT).show()
                                            } else {
                                                chatViewModel.selectModel(modelEnum)
                                                modelMenuExpanded = false
                                            }
                                        }
                                    )
                                }
                            }
                            }
                        } else {
                            TextButton(
                                onClick = { },
                                modifier = Modifier.padding(end = 4.dp),
                                colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                enabled = false
                            ) {
                                Text(
                                    text = "Nabih Ultra",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    
            ) {
                if (searchEnabled) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            TextField(
                                value = searchQuery,
                                onValueChange = { chatViewModel.setSearchQuery(it) },
                                placeholder = { Text(if (isArabic) "ابحث في المحادثة..." else "Search in conversation...") },
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    disabledContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                ),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { chatViewModel.setSearchQuery("") }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            IconButton(onClick = { 
                                chatViewModel.setSearchQuery("")
                                chatViewModel.toggleSearch() 
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Chat Area
                Box(modifier = Modifier.weight(1f)) {
                    when (val state = chatState) {
                        is ChatUiState.Idle -> {
                            EmptyChatState(
                                isArabic = isArabic,
                                onSuggestionClick = { prompt ->
                                    val currentConvId = chatViewModel.activeConversationId.value
                                    if (currentConvId == null) {
                                        homeViewModel.createConversation(
                                            title = prompt,
                                            modelId = selectedModel.id,
                                            isTemporary = false
                                        ) { newId ->
                                            chatViewModel.selectConversation(newId)
                                            chatViewModel.sendMessage(prompt)
                                        }
                                    } else {
                                        chatViewModel.sendMessage(prompt)
                                    }
                                }
                            )
                        }
                        is ChatUiState.Loading -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        }
                        is ChatUiState.Success -> {
                            val listState = rememberLazyListState()
                            val filteredMessages = if (searchQuery.isBlank()) {
                                state.messages
                            } else {
                                state.messages.filter { it.content.contains(searchQuery, ignoreCase = true) }
                            }
                            val imeVisible = WindowInsets.isImeVisible
                            LaunchedEffect(filteredMessages.size, streamResponse, imeVisible, inputText) {
                                if (filteredMessages.isNotEmpty()) {
                                    listState.animateScrollToItem(filteredMessages.lastIndex + (if (isGenerating) 1 else 0))
                                }
                            }
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(filteredMessages) { message ->
                                    MessageItem(
                                        message = message,
                                        isArabic = isArabic,
                                        onRetry = { chatViewModel.retryLastResponse() },
                                        onDelete = { chatViewModel.deleteMessage(message.id) },
                                        onEdit = { newContent -> chatViewModel.editUserMessageAndRegenerate(message.id, newContent) }
                                    )
                                }
                                if (isGenerating) {
                                    item {
                                        MessageItem(
                                            message = com.example.core.database.Message(
                                                id = "streaming",
                                                conversationId = "",
                                                role = "model",
                                                content = streamResponse.ifEmpty { if (isArabic) "جاري التفكير..." else "Thinking..." }
                                            ),
                                            isArabic = isArabic,
                                            isLoading = streamResponse.isEmpty()
                                        )
                                    }
                                }
                            }
                        }
                        is ChatUiState.Error -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(state.message, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }

                // Input Area
                val attachedImageUri by chatViewModel.attachedImageUri.collectAsStateWithLifecycle()
                val attachedDocUri by chatViewModel.attachedDocUri.collectAsStateWithLifecycle()
                val attachedDocName by chatViewModel.attachedDocName.collectAsStateWithLifecycle()
                val isAttaching by chatViewModel.isAttaching.collectAsStateWithLifecycle()
                val attachProgress by chatViewModel.attachProgress.collectAsStateWithLifecycle()
                val attachError by chatViewModel.attachError.collectAsStateWithLifecycle()

                BottomInputArea(
                    isArabic = isArabic,
                    text = inputText,
                    onTextChange = { chatViewModel.updateInputText(it) },
                    onSend = { text ->
                        val currentConvId = chatViewModel.activeConversationId.value
                        if (currentConvId == null) {
                            val autoTitle = generateAutoTitle(text)
                            homeViewModel.createConversation(
                                title = autoTitle,
                                modelId = selectedModel.id,
                                isTemporary = false
                            ) { newId ->
                                chatViewModel.selectConversation(newId)
                                chatViewModel.sendMessage(text)
                            }
                        } else {
                            chatViewModel.sendMessage(text)
                        }
                    },
                    onVoice = onNavigateToVoice,
                    isGenerating = isGenerating,
                    onStop = { chatViewModel.stopGeneration() },
                    attachedImageUri = attachedImageUri,
                    attachedDocUri = attachedDocUri,
                    attachedDocName = attachedDocName,
                    isAttaching = isAttaching,
                    attachProgress = attachProgress,
                    attachError = attachError,
                    onAttachImage = { uri -> chatViewModel.attachImage(uri) },
                    onAttachDocument = { uri, _ -> chatViewModel.attachDocument(uri) },
                    onRemoveAttachment = {
                        chatViewModel.attachImage(null)
                        chatViewModel.attachDocument(null)
                    }
                )
            }
        }
    }
}

@Composable
fun EmptyChatState(isArabic: Boolean, onSuggestionClick: (String) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painterResource(id = R.drawable.logo),
contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (isArabic) "كيف يمكنني مساعدتك اليوم؟" else "How can I help you today?",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(32.dp))
        
        val suggestions = listOf(
            if (isArabic) "لخص مقالاً طويلاً" else "Summarize a long article",
            if (isArabic) "اكتب رسالة بريد إلكتروني" else "Draft a professional email",
            if (isArabic) "ساعدني في البرمجة" else "Help me write some code"
        )
        
        suggestions.forEach { text ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable { onSuggestionClick(text) },
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Text(
                    text = text,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
fun MessageItem(
    message: com.example.core.database.Message,
    isArabic: Boolean,
    isLoading: Boolean = false,
    onRetry: () -> Unit = {},
    onDelete: () -> Unit = {},
    onEdit: (String) -> Unit = {}
) {
    val isUser = message.role == "user"
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var editMessageText by remember { mutableStateOf(message.content) }

    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text(if (isArabic) "تعديل الرسالة" else "Edit Message") },
            text = {
                OutlinedTextField(
                    value = editMessageText,
                    onValueChange = { editMessageText = it },
                    label = { Text(if (isArabic) "الرسالة" else "Message") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onEdit(editMessageText)
                        showEditDialog = false
                    }
                ) {
                    Text(if (isArabic) "تحديث وإرسال" else "Update & Send")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text(if (isArabic) "إلغاء" else "Cancel")
                }
            }
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(painter = painterResource(id = R.drawable.logo), contentDescription = null, tint = androidx.compose.ui.graphics.Color.Unspecified, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
        }
        
        Surface(
            shape = RoundedCornerShape(
                topStart = 20.dp,
                topEnd = 20.dp,
                bottomStart = if (isUser) 20.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 20.dp
            ),
            color = if (isUser) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent,
            modifier = Modifier.weight(1f, fill = false).clickable(enabled = !isLoading) { showMenu = true }
        ) {
            Box {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (message.imageUri != null) {
                        androidx.compose.foundation.Image(
                            painter = coil.compose.rememberAsyncImagePainter(message.imageUri),
                            contentDescription = "Attached Image",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .padding(bottom = 8.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    }
                    if (message.documentUri != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Outlined.Description, contentDescription = "Document", tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = message.documentName ?: "Attached Document",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                    }
                    if (isLoading) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(message.content, style = MaterialTheme.typography.bodyLarge)
                        }
                    } else {
                        Text(
                            text = message.content,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (message.content.startsWith("An error occurred:")) {
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(onClick = onRetry) {
                                Icon(Icons.Outlined.Refresh, contentDescription = "Retry")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(if (isArabic) "إعادة المحاولة" else "Retry")
                            }
                        }
                    }
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(if (isArabic) "نسخ" else "Copy") },
                        leadingIcon = { Icon(Icons.Outlined.ContentCopy, null) },
                        onClick = {
                            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            val clip = android.content.ClipData.newPlainText("Copied Text", message.content)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, if (isArabic) "تم النسخ" else "Copied to clipboard", Toast.LENGTH_SHORT).show()
                            showMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(if (isArabic) "مشاركة" else "Share") },
                        leadingIcon = { Icon(Icons.Outlined.Share, null) },
                        onClick = {
                            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(android.content.Intent.EXTRA_TEXT, message.content)
                            }
                            context.startActivity(android.content.Intent.createChooser(intent, null))
                            showMenu = false
                        }
                    )
                    if (isUser) {
                        DropdownMenuItem(
                            text = { Text(if (isArabic) "تعديل" else "Edit") },
                            leadingIcon = { Icon(Icons.Outlined.Edit, null) },
                            onClick = {
                                showMenu = false
                                showEditDialog = true
                            }
                        )
                    }
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text(if (isArabic) "حذف" else "Delete", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = { Icon(Icons.Outlined.Delete, null, tint = MaterialTheme.colorScheme.error) },
                        onClick = {
                            showMenu = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun BottomInputArea(
    isArabic: Boolean,
    text: String,
    onTextChange: (String) -> Unit,
    onSend: (String) -> Unit,
    onVoice: () -> Unit,
    isGenerating: Boolean,
    onStop: () -> Unit,
    attachedImageUri: android.net.Uri? = null,
    attachedDocUri: android.net.Uri? = null,
    attachedDocName: String? = null,
    isAttaching: Boolean = false,
    attachProgress: Float = 0f,
    attachError: String? = null,
    onAttachImage: (android.net.Uri?) -> Unit = {},
    onAttachDocument: (android.net.Uri?, String?) -> Unit = { _, _ -> },
    onRemoveAttachment: () -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var showAttachmentMenu by remember { mutableStateOf(false) }



    val docPicker = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            var name = "Document"
            var size = 0L
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex >= 0) name = cursor.getString(nameIndex)
                    if (sizeIndex >= 0) size = cursor.getLong(sizeIndex)
                }
            }
            
            // Validate size (e.g., max 10MB)
            if (size > 10 * 1024 * 1024) {
                android.widget.Toast.makeText(context, if (isArabic) "حجم الملف كبير جداً (الحد الأقصى 10 ميغابايت)" else "File size too large (Max 10MB)", android.widget.Toast.LENGTH_SHORT).show()
            } else {
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri, 
                        android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (e: Exception) {
                    // Ignore if not permitted
                }
                onAttachDocument(uri, name)
            }
        }
        showAttachmentMenu = false
    }

    // Add similar validation for imagePicker
    val imagePicker = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            var size = 0L
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                if (cursor.moveToFirst() && sizeIndex >= 0) {
                    size = cursor.getLong(sizeIndex)
                }
            }
            if (size > 10 * 1024 * 1024) {
                android.widget.Toast.makeText(context, if (isArabic) "حجم الصورة كبير جداً (الحد الأقصى 10 ميغابايت)" else "Image size too large (Max 10MB)", android.widget.Toast.LENGTH_SHORT).show()
            } else {
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri, 
                        android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (e: Exception) {
                    // Ignore if not permitted
                }
                onAttachImage(uri)
            }
        }
        showAttachmentMenu = false
    }

    val cameraLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            val tempFile = java.io.File.createTempFile("photo_", ".jpg", context.cacheDir)
            java.io.FileOutputStream(tempFile).use { out ->
                bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 100, out)
            }
            onAttachImage(androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.provider", tempFile))
        }
        showAttachmentMenu = false
    }

    val cameraPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            cameraLauncher.launch(null)
        } else {
            android.widget.Toast.makeText(context, if (isArabic) "عذرًا، نحتاج إلى إذن الكاميرا" else "Camera permission is required", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    Surface(
        color = MaterialTheme.colorScheme.background,
        shadowElevation = 8.dp
    ) {
        Column {
            // Attachment Preview Area
            if (attachedImageUri != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surface)
                        ) {
                            AsyncImage(
                                model = attachedImageUri,
                                contentDescription = "Image preview",
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Column {
                            Text(
                                text = if (isArabic) "صورة مرفقة" else "Image Attached",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (isArabic) "جاهز للإرسال" else "Ready to send",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                    IconButton(onClick = onRemoveAttachment, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Remove")
                    }
                }
            } else if (attachedDocUri != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(
                            imageVector = Icons.Outlined.Description,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Column {
                            Text(
                                text = attachedDocName ?: "Document",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.widthIn(max = 200.dp)
                            )
                            Text(
                                text = if (isArabic) "مستند مرفق" else "Document Attached",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                    IconButton(onClick = onRemoveAttachment, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Remove")
                    }
                }
            }

            // Attaching Progress Area
            if (isAttaching) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isArabic) "جاري معالجة ورفع الملف..." else "Processing file...",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        LinearProgressIndicator(
                            progress = attachProgress,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        )
                    }
                    Text(
                        text = "${(attachProgress * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Error Area
            if (attachError != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = "Error",
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = attachError ?: "",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }
                    IconButton(onClick = onRemoveAttachment, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Clear", tint = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.size(16.dp))
                    }
                }
            }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .defaultMinSize(minHeight = 56.dp),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier.padding(end = 4.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                Box {
                    IconButton(
                        onClick = { showAttachmentMenu = true }, 
                        modifier = Modifier.padding(bottom = 4.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Add, 
                            contentDescription = "Attach", 
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    
                    DropdownMenu(
                        expanded = showAttachmentMenu,
                        onDismissRequest = { showAttachmentMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(if (isArabic) "صورة من المعرض" else "Image from Gallery") },
                            onClick = { 
                                showAttachmentMenu = false
                                imagePicker.launch(arrayOf("image/*")) 
                            },
                            leadingIcon = { Icon(Icons.Outlined.Image, null) }
                        )
                        DropdownMenuItem(
                            text = { Text(if (isArabic) "مستند / ملف" else "Document / File") },
                            onClick = { 
                                showAttachmentMenu = false
                                docPicker.launch(arrayOf("*/*")) 
                            },
                            leadingIcon = { Icon(Icons.Outlined.Description, null) }
                        )
                        DropdownMenuItem(
                            text = { Text(if (isArabic) "الكاميرا" else "Camera") },
                            onClick = { 
                                showAttachmentMenu = false
                                cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA) 
                            },
                            leadingIcon = { Icon(Icons.Outlined.CameraAlt, null) }
                        )
                    }
                }
                
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 16.dp, horizontal = 4.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (text.isEmpty()) {
                        Text(
                            text = if (isArabic) "اكتب رسالة..." else "Message Nabih AI...",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    BasicTextField(
                        value = text,
                        onValueChange = onTextChange,
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
                        maxLines = 6
                    )
                }
                
                Box(
                    modifier = Modifier.padding(bottom = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedContent(
                        targetState = when {
                            isGenerating -> 0
                            text.isNotBlank() || attachedImageUri != null || attachedDocUri != null -> 1
                            else -> 2
                        },
                        transitionSpec = {
                            scaleIn(animationSpec = tween(150)) togetherWith scaleOut(animationSpec = tween(150))
                        },
                        label = "input_action"
                    ) { state ->
                        when (state) {
                            0 -> {
                                IconButton(
                                    onClick = onStop,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.errorContainer)
                                ) {
                                    Icon(
                                        Icons.Default.Stop, 
                                        contentDescription = "Stop", 
                                        tint = MaterialTheme.colorScheme.onErrorContainer, 
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            1 -> {
                                IconButton(
                                    onClick = { onSend(text) },
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary)
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.Send, 
                                        contentDescription = "Send", 
                                        tint = MaterialTheme.colorScheme.onPrimary, 
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            2 -> {
                                IconButton(
                                    onClick = onVoice,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(androidx.compose.ui.graphics.Color.Transparent)
                                ) {
                                    Icon(
                                        Icons.Default.Mic, 
                                        contentDescription = "Voice", 
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant, 
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        }
    }
}

@Composable
fun SwipeableConversationItem(
    conversation: Conversation,
    isArabic: Boolean,
    onSelectConversation: (String) -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onTogglePin: () -> Unit,
    onToggleArchive: () -> Unit
) {
    var offsetX by remember { mutableStateOf(0f) }
    val animatedOffset by animateFloatAsState(targetValue = offsetX, label = "swipeOffset")

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (offsetX < -80f) {
                            offsetX = -120f
                        } else if (offsetX > 80f) {
                            offsetX = 120f
                        } else {
                            offsetX = 0f
                        }
                    },
                    onDragCancel = {
                        offsetX = 0f
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        offsetX = (offsetX + dragAmount).coerceIn(-140f, 140f)
                    }
                )
            }
    ) {
        Row(
            modifier = Modifier
                .matchParentSize()
                .background(MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp),
            horizontalArrangement = if (offsetX < 0) Arrangement.End else Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (offsetX < 0) {
                IconButton(onClick = {
                    offsetX = 0f
                    onRename()
                }) {
                    Icon(Icons.Default.Edit, contentDescription = "Rename", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = {
                    offsetX = 0f
                    onDelete()
                }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
            } else {
                IconButton(onClick = {
                    offsetX = 0f
                    onTogglePin()
                }) {
                    Icon(
                        imageVector = if (conversation.isPinned) Icons.Default.PushPin else Icons.Outlined.PushPin,
                        contentDescription = "Pin",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = {
                    offsetX = 0f
                    onToggleArchive()
                }) {
                    Icon(
                        imageVector = if (conversation.isArchived) Icons.Default.Unarchive else Icons.Default.Archive,
                        contentDescription = "Archive",
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }

        Surface(
            modifier = Modifier
                .offset(x = animatedOffset.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
            onClick = {
                if (offsetX != 0f) {
                    offsetX = 0f
                } else {
                    onSelectConversation(conversation.id)
                }
            }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (conversation.isPinned) Icons.Default.PushPin else Icons.Outlined.ChatBubbleOutline,
                    contentDescription = null,
                    tint = if (conversation.isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = conversation.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (conversation.isPinned) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Text(
                        text = conversation.modelId.replace("gemini-2.5-", "").replace("gpt-5-", "").replace("-", " ").capitalize(Locale.ROOT),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
                
                var showOptions by remember { mutableStateOf(false) }
                Box {
                    IconButton(
                        onClick = { showOptions = true },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "Options",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    DropdownMenu(
                        expanded = showOptions,
                        onDismissRequest = { showOptions = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(if (isArabic) (if (conversation.isPinned) "إلغاء التثبيت" else "تثبيت المحادثة") else (if (conversation.isPinned) "Unpin" else "Pin")) },
                            leadingIcon = { Icon(Icons.Outlined.PushPin, null) },
                            onClick = {
                                showOptions = false
                                onTogglePin()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(if (isArabic) (if (conversation.isArchived) "إلغاء الأرشفة" else "أرشفة المحادثة") else (if (conversation.isArchived) "Unarchive" else "Archive")) },
                            leadingIcon = { Icon(Icons.Outlined.Archive, null) },
                            onClick = {
                                showOptions = false
                                onToggleArchive()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(if (isArabic) "إعادة تسمية" else "Rename") },
                            leadingIcon = { Icon(Icons.Outlined.Edit, null) },
                            onClick = {
                                showOptions = false
                                onRename()
                            }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text(if (isArabic) "حذف" else "Delete", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = { Icon(Icons.Outlined.Delete, null, tint = MaterialTheme.colorScheme.error) },
                            onClick = {
                                showOptions = false
                                onDelete()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MainDrawerContent(
    settings: AppSettings,
    selectedModel: com.example.core.model.AiModel,
    onSelectModel: (com.example.core.model.AiModel) -> Unit,
    conversations: List<com.example.core.database.Conversation>,
    onSelectConversation: (String) -> Unit,
    onNewChat: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateTo: (String) -> Unit,
    onCloseDrawer: () -> Unit,
    onRenameConversation: (String, String) -> Unit,
    onDeleteConversation: (String) -> Unit,
    onTogglePinConversation: (com.example.core.database.Conversation) -> Unit,
    onToggleArchiveConversation: (com.example.core.database.Conversation) -> Unit
) {
    val isArabic = settings.language == AppLanguage.ARABIC
    val context = androidx.compose.ui.platform.LocalContext.current
    
    var conversationToRename by remember { mutableStateOf<Conversation?>(null) }
    var renameNewTitle by remember { mutableStateOf("") }
    var conversationToDelete by remember { mutableStateOf<Conversation?>(null) }

    conversationToRename?.let { conversation ->
        AlertDialog(
            onDismissRequest = { conversationToRename = null },
            title = { Text(if (isArabic) "إعادة تسمية المحادثة" else "Rename Conversation") },
            text = {
                OutlinedTextField(
                    value = renameNewTitle,
                    onValueChange = { renameNewTitle = it },
                    label = { Text(if (isArabic) "العنوان الجديد" else "New Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (renameNewTitle.isNotBlank()) {
                            onRenameConversation(conversation.id, renameNewTitle)
                        }
                        conversationToRename = null
                    }
                ) {
                    Text(if (isArabic) "تأكيد" else "Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { conversationToRename = null }) {
                    Text(if (isArabic) "إلغاء" else "Cancel")
                }
            }
        )
    }

    conversationToDelete?.let { conversation ->
        AlertDialog(
            onDismissRequest = { conversationToDelete = null },
            title = { Text(if (isArabic) "حذف المحادثة" else "Delete Conversation") },
            text = {
                Text(
                    if (isArabic) "هل أنت متأكد من رغبتك في حذف هذه المحادثة نهائياً؟ لا يمكن التراجع عن هذا الإجراء."
                    else "Are you sure you want to delete this conversation permanently? This action cannot be undone."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteConversation(conversation.id)
                        conversationToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(if (isArabic) "حذف" else "Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { conversationToDelete = null }) {
                    Text(if (isArabic) "إلغاء" else "Cancel")
                }
            }
        )
    }

    ModalDrawerSheet(
        drawerContainerColor = MaterialTheme.colorScheme.surface,
        drawerContentColor = MaterialTheme.colorScheme.onSurface,
        drawerShape = androidx.compose.foundation.shape.RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp),
        modifier = Modifier.width(300.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.logo),
                    contentDescription = "Nabih AI Logo",
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Nabih AI",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                item {
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                        label = { Text(if (isArabic) "محادثة جديدة" else "New Chat") },
                        selected = false,
                        onClick = {
                            onNewChat()
                            onCloseDrawer()
                        },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }



                item {
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Outlined.BookmarkBorder, contentDescription = null) },
                        label = { Text(if (isArabic) "المحادثات المحفوظة" else "Saved Chats") },
                        selected = false,
                        onClick = {
                            onNavigateTo("saved")
                            onCloseDrawer()
                        },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }

                item {
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Outlined.Folder, contentDescription = null) },
                        label = { Text(if (isArabic) "الملفات والمستندات" else "Files & Documents") },
                        selected = false,
                        onClick = {
                            onNavigateTo("files")
                            onCloseDrawer()
                        },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }

                item {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(vertical = 16.dp))
                    Text(
                        text = if (isArabic) "سجل المحادثات" else "Chat History",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 24.dp).padding(bottom = 8.dp)
                    )
                }

                if (conversations.isEmpty()) {
                    item {
                        Text(
                            text = if (isArabic) "لا توجد محادثات بعد" else "No conversations yet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                        )
                    }
                } else {
                    items(conversations, key = { it.id }) { conversation ->
                        SwipeableConversationItem(
                            conversation = conversation,
                            isArabic = isArabic,
                            onSelectConversation = {
                                onSelectConversation(it)
                                onCloseDrawer()
                            },
                            onRename = {
                                renameNewTitle = conversation.title
                                conversationToRename = conversation
                            },
                            onDelete = {
                                conversationToDelete = conversation
                            },
                            onTogglePin = {
                                onTogglePinConversation(conversation)
                            },
                            onToggleArchive = {
                                onToggleArchiveConversation(conversation)
                            }
                        )
                    }
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = selectedModel.displayName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = selectedModel.provider.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        modifier = Modifier.padding(start = 4.dp)
                    ) {
                        Text(
                            text = if (isArabic) "نشط" else "Active",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            
            NavigationDrawerItem(
                icon = { Icon(Icons.Outlined.Settings, contentDescription = null) },
                label = { Text(if (isArabic) "الإعدادات" else "Settings") },
                selected = false,
                onClick = {
                    onNavigateToSettings()
                    onCloseDrawer()
                },
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
            
            NavigationDrawerItem(
                icon = { Icon(Icons.Outlined.AccountCircle, contentDescription = null) },
                label = { val txt = if (settings.isLoggedIn) (if (settings.userName.isEmpty()) (if (settings.userEmail.isEmpty()) (if (isArabic) "الحساب" else "Account") else settings.userEmail.substringBefore("@")) else settings.userName) else (if (isArabic) "تسجيل الدخول" else "Sign In"); Text(txt, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) },
                selected = false,
                onClick = {
                    onNavigateTo("account")
                    onCloseDrawer()
                },
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }
    }
}
