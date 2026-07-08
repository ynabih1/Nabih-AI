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
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
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
    val context = LocalContext.current
    
    
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

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
                                onDismissRequest = { modelMenuExpanded = false }
                            ) {
                                val availableModels = listOf(
                                    com.example.core.model.AiModel.NABIH_ULTRA,
                                    com.example.core.model.AiModel.GPT_4O,
                                    com.example.core.model.AiModel.GEMINI_PRO,
                                    com.example.core.model.AiModel.CLAUDE_SONNET
                                )
                                availableModels.forEach { model ->
                                    val isLocked = when (model) {
                                        com.example.core.model.AiModel.NABIH_ULTRA -> false
                                        com.example.core.model.AiModel.GPT_4O -> settings.openaiApiKey.isEmpty()
                                        com.example.core.model.AiModel.GEMINI_PRO -> settings.googleApiKey.isEmpty()
                                        com.example.core.model.AiModel.CLAUDE_SONNET -> settings.anthropicApiKey.isEmpty()
                                        else -> false
                                    }
                                    DropdownMenuItem(
                                        text = { 
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                if (isLocked) {
                                                    Icon(Icons.Outlined.Lock, contentDescription = "Locked", modifier = Modifier.size(16.dp).padding(end = 4.dp))
                                                } else {
                                                    Icon(Icons.Outlined.Check, contentDescription = "Ready", modifier = Modifier.size(16.dp).padding(end = 4.dp), tint = if (selectedModel == model) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Transparent)
                                                }
                                                Text(model.displayName, color = if (isLocked) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface) 
                                            }
                                        },
                                        onClick = {
                                            if (isLocked) {
                                                android.widget.Toast.makeText(context, "API Key Required", android.widget.Toast.LENGTH_SHORT).show()
                                            } else {
                                                chatViewModel.selectModel(model)
                                                modelMenuExpanded = false
                                            }
                                        }
                                    )
                                }
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
                                    MessageItem(message = message, isArabic = isArabic, onRetry = { chatViewModel.retryLastResponse() })
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
                            homeViewModel.createConversation(
                                title = if (text.isNotBlank()) text.take(20) else "New Chat",
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
fun MessageItem(message: com.example.core.database.Message, isArabic: Boolean, isLoading: Boolean = false, onRetry: () -> Unit = {}) {
    val isUser = message.role == "user"
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }

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

            Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            Box {
                IconButton(onClick = { showAttachmentMenu = true }, modifier = Modifier.padding(bottom = 4.dp)) {
                    Icon(Icons.Outlined.AttachFile, contentDescription = "Attach")
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
            
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
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
                if (text.isNotBlank() || attachedImageUri != null || attachedDocUri != null) {
                    IconButton(
                        onClick = {
                            onSend(text)
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
    onCloseDrawer: () -> Unit
) {
    val isArabic = settings.language == AppLanguage.ARABIC
    val context = androidx.compose.ui.platform.LocalContext.current

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
            // Header
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

            // Scrollable Content
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                // Main Navigation
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
                        icon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                        label = { Text(if (isArabic) "البحث" else "Search") },
                        selected = false,
                        onClick = {
                            onNavigateTo("search")
                            onCloseDrawer()
                        },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }

                item {
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Outlined.Folder, contentDescription = null) },
                        label = { Text(if (isArabic) "الملفات" else "Files") },
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
                        text = if (isArabic) "المحادثات" else "Chat History",
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
                    items(conversations) { conversation ->
                        NavigationDrawerItem(
                            icon = { Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = null, modifier = Modifier.size(20.dp)) },
                            label = { 
                                Text(
                                    text = conversation.title,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                ) 
                            },
                            selected = false,
                            onClick = {
                                onSelectConversation(conversation.id)
                                onCloseDrawer()
                            },
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                        )
                    }
                }

                item {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(vertical = 16.dp))
                    Text(
                        text = if (isArabic) "نموذج الذكاء الاصطناعي" else "AI Models",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 24.dp).padding(bottom = 8.dp)
                    )
                }

                item {
                    val availableModels = listOf(
                        com.example.core.model.AiModel.NABIH_ULTRA,
                        com.example.core.model.AiModel.GPT_4O,
                        com.example.core.model.AiModel.GEMINI_PRO,
                        com.example.core.model.AiModel.CLAUDE_SONNET
                    )
                    
                    availableModels.forEach { model ->
                        val isLocked = when (model) {
                            com.example.core.model.AiModel.NABIH_ULTRA -> false
                            com.example.core.model.AiModel.GPT_4O -> settings.openaiApiKey.isEmpty()
                            com.example.core.model.AiModel.GEMINI_PRO -> settings.googleApiKey.isEmpty()
                            com.example.core.model.AiModel.CLAUDE_SONNET -> settings.anthropicApiKey.isEmpty()
                            else -> false
                        }
                        NavigationDrawerItem(
                            icon = {
                                if (isLocked) {
                                    Icon(Icons.Outlined.Lock, contentDescription = "Locked", modifier = Modifier.size(20.dp))
                                } else {
                                    Icon(Icons.Outlined.CheckCircle, contentDescription = "Ready", modifier = Modifier.size(20.dp), tint = if (selectedModel.id == model.id) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            },
                            label = {
                                Text(
                                    text = model.displayName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isLocked) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface
                                )
                            },
                            selected = selectedModel.id == model.id,
                            onClick = {
                                if (isLocked) {
                                    android.widget.Toast.makeText(context, if (isArabic) "مفتاح API مطلوب" else "API Key Required", android.widget.Toast.LENGTH_SHORT).show()
                                    // Navigate to settings to add keys
                                    onNavigateToSettings()
                                    onCloseDrawer()
                                } else {
                                    onSelectModel(model)
                                    onCloseDrawer()
                                }
                            },
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Bottom Section
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
