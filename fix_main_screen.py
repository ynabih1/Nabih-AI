import os

code = """package com.example.ui.screen

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AiModel
import com.example.data.model.AppLanguage
import com.example.data.model.Conversation
import com.example.ui.state.ChatUiState
import com.example.ui.viewmodel.ChatViewModel
import com.example.ui.viewmodel.HomeViewModel
import com.example.ui.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    homeViewModel: HomeViewModel,
    chatViewModel: ChatViewModel,
    settingsViewModel: SettingsViewModel,
    onNavigateToSettings: () -> Unit,
    onNavigateToVoice: () -> Unit
) {
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
    val isArabic = settings.language == AppLanguage.ARABIC
    val activeConversations by homeViewModel.activeConversations.collectAsStateWithLifecycle(emptyList())
    
    val chatState by chatViewModel.uiState.collectAsStateWithLifecycle()
    val isGenerating by chatViewModel.isGenerating.collectAsStateWithLifecycle()
    val streamResponse by chatViewModel.currentStreamingResponse.collectAsStateWithLifecycle()
    val selectedModel by chatViewModel.selectedModel.collectAsStateWithLifecycle()
    
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            MainDrawerContent(
                isArabic = isArabic,
                conversations = activeConversations,
                onNewChat = {
                    chatViewModel.cleanupIfTemporary() // Cleanup current if needed
                    val modelId = settings.defaultModel.id
                    homeViewModel.createConversation(
                        title = if (isArabic) "محادثة جديدة" else "New Chat",
                        modelId = modelId,
                        isTemporary = true
                    ) { newId ->
                        chatViewModel.selectConversation(newId)
                        scope.launch { drawerState.close() }
                    }
                },
                onSelectConversation = { conv ->
                    chatViewModel.selectConversation(conv.id)
                    scope.launch { drawerState.close() }
                },
                onNavigateToSettings = {
                    onNavigateToSettings()
                    scope.launch { drawerState.close() }
                }
            )
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Nabih AI",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = selectedModel.displayName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    },
                    actions = {
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(Icons.Outlined.Settings, contentDescription = "Settings")
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
                // Chat Area
                Box(modifier = Modifier.weight(1f)) {
                    when (val state = chatState) {
                        is ChatUiState.Empty -> {
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
                            LaunchedEffect(state.messages.size, streamResponse) {
                                if (state.messages.isNotEmpty()) {
                                    listState.animateScrollToItem(state.messages.lastIndex + (if (isGenerating) 1 else 0))
                                }
                            }
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(state.messages) { message ->
                                    MessageItem(message = message, isArabic = isArabic)
                                }
                                if (isGenerating) {
                                    item {
                                        MessageItem(
                                            message = com.example.data.database.Message(
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
                BottomInputArea(
                    isArabic = isArabic,
                    onSend = { text ->
                        val currentConvId = chatViewModel.activeConversationId.value
                        if (currentConvId == null) {
                            homeViewModel.createConversation(
                                title = text.take(20),
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
                    onAttach = { /* Open file picker */ },
                    onVoice = onNavigateToVoice,
                    isGenerating = isGenerating,
                    onStop = { chatViewModel.stopGeneration() }
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
            Icons.Default.AutoAwesome,
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
fun MessageItem(message: com.example.data.database.Message, isArabic: Boolean, isLoading: Boolean = false) {
    val isUser = message.role == "user"
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
                Icon(Icons.Default.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
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
            modifier = Modifier.weight(1f, fill = false)
        ) {
            if (isLoading) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(message.content, style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun BottomInputArea(
    isArabic: Boolean,
    onSend: (String) -> Unit,
    onAttach: () -> Unit,
    onVoice: () -> Unit,
    isGenerating: Boolean,
    onStop: () -> Unit
) {
    var text by remember { mutableStateOf("") }

    Surface(
        color = MaterialTheme.colorScheme.background,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            IconButton(onClick = onAttach, modifier = Modifier.padding(bottom = 4.dp)) {
                Icon(Icons.Outlined.AttachFile, contentDescription = "Attach")
            }
            
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text(if (isArabic) "اكتب رسالة..." else "Message Nabih AI...") },
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                shape = RoundedCornerShape(24.dp),
                maxLines = 5,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )
            
            if (isGenerating) {
                IconButton(
                    onClick = onStop,
                    modifier = Modifier
                        .padding(bottom = 4.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.errorContainer)
                ) {
                    Icon(Icons.Default.Stop, contentDescription = "Stop", tint = MaterialTheme.colorScheme.onErrorContainer)
                }
            } else {
                if (text.isNotBlank()) {
                    IconButton(
                        onClick = {
                            onSend(text)
                            text = ""
                        },
                        modifier = Modifier
                            .padding(bottom = 4.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                } else {
                    IconButton(
                        onClick = onVoice,
                        modifier = Modifier.padding(bottom = 4.dp)
                    ) {
                        Icon(Icons.Default.Mic, contentDescription = "Voice")
                    }
                }
            }
        }
    }
}

@Composable
fun MainDrawerContent(
    isArabic: Boolean,
    conversations: List<Conversation>,
    onNewChat: () -> Unit,
    onSelectConversation: (Conversation) -> Unit,
    onNavigateToSettings: () -> Unit
) {
    ModalDrawerSheet(
        drawerContainerColor = MaterialTheme.colorScheme.surface,
        drawerShape = RoundedCornerShape(0.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Nabih AI",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Divider(color = MaterialTheme.colorScheme.outlineVariant)
            
            Spacer(modifier = Modifier.height(12.dp))
            
            NavigationDrawerItem(
                icon = { Icon(Icons.Outlined.Add, null) },
                label = { Text(if (isArabic) "محادثة جديدة" else "New Chat", fontWeight = FontWeight.SemiBold) },
                selected = false,
                onClick = onNewChat,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
            
            NavigationDrawerItem(
                icon = { Icon(Icons.Outlined.Search, null) },
                label = { Text(if (isArabic) "البحث في المحادثات" else "Search conversations") },
                selected = false,
                onClick = { /* TODO */ },
                modifier = Modifier.padding(horizontal = 12.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = if (isArabic) "سجل المحادثات" else "Chat history",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 28.dp, vertical = 8.dp)
            )
            
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(conversations) { conv ->
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Outlined.ChatBubbleOutline, null, modifier = Modifier.size(20.dp)) },
                        label = { 
                            Text(
                                text = conv.title,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodyMedium
                            ) 
                        },
                        selected = false,
                        onClick = { onSelectConversation(conv) },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                    )
                }
            }
            
            Divider(color = MaterialTheme.colorScheme.outlineVariant)
            
            NavigationDrawerItem(
                icon = { Icon(Icons.Outlined.Folder, null) },
                label = { Text(if (isArabic) "الملفات والمستندات" else "Files and documents") },
                selected = false,
                onClick = { /* TODO */ },
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
            
            NavigationDrawerItem(
                icon = { Icon(Icons.Outlined.Build, null) },
                label = { Text(if (isArabic) "أدوات الذكاء الاصطناعي" else "AI tools") },
                selected = false,
                onClick = { /* TODO */ },
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
            
            NavigationDrawerItem(
                icon = { Icon(Icons.Outlined.Settings, null) },
                label = { Text(if (isArabic) "الإعدادات" else "Settings") },
                selected = false,
                onClick = onNavigateToSettings,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
"""

with open('app/src/main/java/com/example/ui/screen/MainScreen.kt', 'w') as f:
    f.write(code)

