package com.example.feature.chat

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState




import com.example.R

import com.example.core.database.Conversation
import com.example.core.database.Folder
import com.example.core.database.Message
import com.example.core.model.AiModel
import com.example.core.model.AppLanguage
import com.example.core.model.AppSettings
import com.example.feature.settings.SettingsViewModel
import com.example.core.ui.MarkdownRenderer
import com.example.core.ui.TypingAnimation
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Check

import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import androidx.compose.ui.draw.scale

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material3.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.window.Popup
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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

// Custom Markdown segment representation for premium content rendering
sealed class MarkdownSegment {
    data class TextBlock(val content: String) : MarkdownSegment()
    data class HeaderBlock(val level: Int, val content: String) : MarkdownSegment()
    data class CodeBlock(val language: String, val code: String) : MarkdownSegment()
    data class BulletListBlock(val items: List<String>) : MarkdownSegment()
    data class OrderedListBlock(val items: List<String>) : MarkdownSegment()
    data class BlockQuoteBlock(val content: String) : MarkdownSegment()
}

// In-depth robust client-side line parsing of markdown content
fun parseMarkdown(text: String): List<MarkdownSegment> {
    val segments = mutableListOf<MarkdownSegment>()
    val lines = text.split("\n")
    var i = 0
    val n = lines.size
    
    while (i < n) {
        val line = lines[i]
        
        // Code blocks
        if (line.trim().startsWith("```")) {
            val language = line.trim().substring(3).trim()
            val codeBuilder = StringBuilder()
            i++
            while (i < n && !lines[i].trim().startsWith("```")) {
                codeBuilder.append(lines[i]).append("\n")
                i++
            }
            if (i < n && lines[i].trim().startsWith("```")) {
                // finished block
            }
            segments.add(MarkdownSegment.CodeBlock(language, codeBuilder.toString().trimEnd()))
            i++
            continue
        }
        
        // Block quotes
        if (line.trim().startsWith(">")) {
            val contentBuilder = StringBuilder()
            contentBuilder.append(line.trim().substring(1).trim()).append("\n")
            i++
            while (i < n && lines[i].trim().startsWith(">")) {
                contentBuilder.append(lines[i].trim().substring(1).trim()).append("\n")
                i++
            }
            segments.add(MarkdownSegment.BlockQuoteBlock(contentBuilder.toString().trimEnd()))
            continue
        }
        
        // Bullet list
        if (line.trim().startsWith("- ") || line.trim().startsWith("* ") || line.trim().startsWith("• ")) {
            val items = mutableListOf<String>()
            val prefixLen = if (line.trim().startsWith("• ")) 1 else 2
            items.add(line.trim().substring(prefixLen).trim())
            i++
            while (i < n && (lines[i].trim().startsWith("- ") || lines[i].trim().startsWith("* ") || lines[i].trim().startsWith("• "))) {
                val itemLine = lines[i].trim()
                val innerPrefixLen = if (itemLine.startsWith("• ")) 1 else 2
                items.add(itemLine.substring(innerPrefixLen).trim())
                i++
            }
            segments.add(MarkdownSegment.BulletListBlock(items))
            continue
        }

        // Ordered list
        val trimmedLine = line.trim()
        val numMatch = "^[0-9]+\\.\\s+".toRegex().find(trimmedLine)
        if (numMatch != null) {
            val items = mutableListOf<String>()
            items.add(trimmedLine.substring(numMatch.value.length).trim())
            i++
            while (i < n) {
                val nextTrimmed = lines[i].trim()
                val nextMatch = "^[0-9]+\\.\\s+".toRegex().find(nextTrimmed)
                if (nextMatch != null) {
                    items.add(nextTrimmed.substring(nextMatch.value.length).trim())
                    i++
                } else {
                    break
                }
            }
            segments.add(MarkdownSegment.OrderedListBlock(items))
            continue
        }
        
        // Header
        if (line.trim().startsWith("#")) {
            val headerTrim = line.trim()
            val level = headerTrim.takeWhile { it == '#' }.length
            if (level in 1..6 && headerTrim.substring(level).startsWith(" ")) {
                val content = headerTrim.substring(level).trim()
                segments.add(MarkdownSegment.HeaderBlock(level, content))
                i++
                continue
            }
        }
        
        // Text block
        if (line.trim().isNotEmpty()) {
            val textBuilder = StringBuilder()
            textBuilder.append(line).append("\n")
            i++
            while (i < n) {
                val nextLine = lines[i]
                if (nextLine.trim().isEmpty() || 
                    nextLine.trim().startsWith("```") || 
                    nextLine.trim().startsWith(">") || 
                    nextLine.trim().startsWith("- ") || 
                    nextLine.trim().startsWith("* ") || 
                    nextLine.trim().startsWith("• ") || 
                    "^[0-9]+\\.\\s+".toRegex().containsMatchIn(nextLine.trim()) || 
                    nextLine.trim().startsWith("#")
                ) {
                    break
                }
                textBuilder.append(nextLine).append("\n")
                i++
            }
            segments.add(MarkdownSegment.TextBlock(textBuilder.toString().trimEnd()))
        } else {
            i++
        }
    }
    
    return segments
}

// Inline markdown styling formatter with bold, italic, and inline code spans
@Composable
fun formatInlineMarkdown(text: String, isArabic: Boolean): androidx.compose.ui.text.AnnotatedString {
    val primaryColor = MaterialTheme.colorScheme.primary
    val codeBg = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
    return androidx.compose.ui.text.buildAnnotatedString {
        var index = 0
        val n = text.length
        
        while (index < n) {
            // Bold check **
            if (index + 1 < n && text[index] == '*' && text[index + 1] == '*') {
                val endBold = text.indexOf("**", index + 2)
                if (endBold != -1) {
                    pushStyle(androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Bold))
                    append(text.substring(index + 2, endBold))
                    pop()
                    index = endBold + 2
                    continue
                }
            }
            
            // Italic check *
            if (text[index] == '*') {
                val endItalic = text.indexOf("*", index + 1)
                if (endItalic != -1 && endItalic != index + 1) {
                    pushStyle(androidx.compose.ui.text.SpanStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic))
                    append(text.substring(index + 1, endItalic))
                    pop()
                    index = endItalic + 1
                    continue
                }
            }
            
            // Inline code check `
            if (text[index] == '`') {
                val endCode = text.indexOf('`', index + 1)
                if (endCode != -1) {
                    pushStyle(androidx.compose.ui.text.SpanStyle(
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        background = codeBg,
                        color = primaryColor,
                        fontWeight = FontWeight.Medium
                    ))
                    append(text.substring(index + 1, endCode))
                    pop()
                    index = endCode + 1
                    continue
                }
            }
            
            append(text[index])
            index++
        }
    }
}

// Light tokenizer for syntax highlighting within code blocks
@Composable
fun highlightSyntax(code: String, language: String): androidx.compose.ui.text.AnnotatedString {
    val keywords = setOf(
        "fun", "class", "val", "var", "if", "else", "return", "import", "package", "def", "func", "let", "const", "function", "public", "private", "protected", "override", "null", "true", "false", "for", "while", "import", "from", "as", "struct", "enum", "struct", "impl"
    )
    return androidx.compose.ui.text.buildAnnotatedString {
        var i = 0
        val n = code.length
        
        while (i < n) {
            // Line comment //
            if (i + 1 < n && code[i] == '/' && code[i + 1] == '/') {
                pushStyle(androidx.compose.ui.text.SpanStyle(color = Color(0xFF6A9955))) // Green comments
                val endLine = code.indexOf("\n", i)
                if (endLine != -1) {
                    append(code.substring(i, endLine))
                    i = endLine
                } else {
                    append(code.substring(i))
                    i = n
                }
                pop()
                continue
            }
            
            // Strings "..."
            if (code[i] == '"') {
                pushStyle(androidx.compose.ui.text.SpanStyle(color = Color(0xFFCE9178))) // Terracotta strings
                append('"')
                i++
                while (i < n && code[i] != '"') {
                    if (code[i] == '\\' && i + 1 < n) {
                        append(code[i])
                        append(code[i + 1])
                        i += 2
                    } else {
                        append(code[i])
                        i++
                    }
                }
                if (i < n) {
                    append('"')
                    i++
                }
                pop()
                continue
            }

            // Keyword highlight
            if (code[i].isLetter() || code[i] == '_') {
                val start = i
                while (i < n && (code[i].isLetterOrDigit() || code[i] == '_')) {
                    i++
                }
                val word = code.substring(start, i)
                if (keywords.contains(word)) {
                    pushStyle(androidx.compose.ui.text.SpanStyle(color = Color(0xFF569CD6), fontWeight = FontWeight.Bold)) // Blue keywords
                    append(word)
                    pop()
                } else {
                    append(word)
                }
                continue
            }

            append(code[i])
            i++
        }
    }
}

// Implements fluid, bouncing triple dots loading animations
@Composable
fun BouncingDotsIndicator() {
    val transition = rememberInfiniteTransition(label = "dots")
    val dotCount = 3
    val dots = List(dotCount) { index ->
        transition.animateFloat(
            initialValue = 0f,
            targetValue = -8f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 350, delayMillis = index * 110, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "dot_$index"
        )
    }
    Row(
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        dots.forEach { offset ->
            Box(
                modifier = Modifier
                    .size(9.dp)
                    .graphicsLayer(translationY = offset.value)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MainScreen(
    homeViewModel: com.example.feature.chat.HomeViewModel,
    chatViewModel: com.example.feature.chat.ChatViewModel,
    settingsViewModel: com.example.feature.settings.SettingsViewModel,
    onNavigateToSettings: () -> Unit,
    onNavigateToRoute: (String) -> Unit = {}
) {
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
    val isArabic = settings.language == AppLanguage.ARABIC
    val activeConversations by homeViewModel.activeConversations.collectAsStateWithLifecycle(emptyList())
    
    val chatState by chatViewModel.uiState.collectAsStateWithLifecycle()
    val activeConversationId by chatViewModel.activeConversationId.collectAsStateWithLifecycle()
    val isGenerating by chatViewModel.isGenerating.collectAsStateWithLifecycle()
    val streamResponse by chatViewModel.currentStreamingResponse.collectAsStateWithLifecycle()
    val selectedModel by chatViewModel.selectedModel.collectAsStateWithLifecycle()
    val inputText by chatViewModel.currentInputText.collectAsStateWithLifecycle()
    val searchEnabled by chatViewModel.searchEnabled.collectAsStateWithLifecycle()
    val searchQuery by chatViewModel.searchQuery.collectAsStateWithLifecycle()
    val context = LocalContext.current
    
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(selectedModel, settings) {
        if (selectedModel.id != com.example.core.model.AiModel.NABIH_ULTRA.id) {
            val isProviderValid = when (selectedModel.provider) {
                com.example.core.model.ApiProvider.NABIH -> true
                com.example.core.model.ApiProvider.GOOGLE -> settings.googleApiKey.isNotEmpty() || settings.nabihApiKey.isNotEmpty()
                com.example.core.model.ApiProvider.OPENAI -> settings.openaiApiKey.isNotEmpty()
                com.example.core.model.ApiProvider.ANTHROPIC -> settings.anthropicApiKey.isNotEmpty()
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
                activeConversationId = activeConversationId,
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
                onDeleteConversation = { id -> 
                    homeViewModel.deleteConversation(id) 
                    if (activeConversationId == id) {
                        chatViewModel.createNewChat(settings.defaultModel.id)
                    }
                },
                onTogglePinConversation = { conv -> homeViewModel.togglePinConversation(conv) },
                onToggleArchiveConversation = { conv -> homeViewModel.toggleArchiveConversation(conv) },
                onUpdateTheme = { theme -> settingsViewModel.updateTheme(theme) },
                onUpdateLanguage = { lang -> settingsViewModel.updateLanguage(lang) }
            )
        }
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(painter = painterResource(id = R.drawable.logo), contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(28.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Nabih AI",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }, modifier = Modifier.minimumInteractiveComponentSize()) {
                            Icon(Icons.Rounded.Menu, contentDescription = "Menu")
                        }
                    },
                    actions = {},
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            },
            containerColor = MaterialTheme.colorScheme.surface
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
                                imageVector = Icons.Rounded.Search,
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
                                IconButton(onClick = { chatViewModel.setSearchQuery("") }, modifier = Modifier.minimumInteractiveComponentSize()) {
                                    Icon(
                                        imageVector = Icons.Rounded.Clear,
                                        contentDescription = "Clear",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            IconButton(onClick = { 
                                chatViewModel.setSearchQuery("")
                                chatViewModel.toggleSearch() 
                            }, modifier = Modifier.minimumInteractiveComponentSize()) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
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
                            
                            val coroutineScope = rememberCoroutineScope()
                            
                            LaunchedEffect(filteredMessages.size, streamResponse, imeVisible, inputText) {
                                if (filteredMessages.isNotEmpty()) {
                                    listState.animateScrollToItem(filteredMessages.lastIndex + (if (isGenerating) 1 else 0))
                                }
                            }
                            
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(filteredMessages) { message ->
                                    MessageItem(
                                        message = message,
                                        isArabic = isArabic,
                                        isStreaming = false,
                                        onRetry = { chatViewModel.retryLastResponse() },
                                        onDelete = { chatViewModel.deleteMessage(message.id) },
                                        onEdit = { newContent -> chatViewModel.editUserMessageAndRegenerate(message.id, newContent) },
                                        onShowFeedbackSuccess = {
                                            scope.launch {
                                                snackbarHostState.showSnackbar(if (isArabic) "شكرًا لك على ملاحظاتك." else "Thank you for your feedback.")
                                            }
                                        }
                                    )
                                }
                                if (isGenerating) {
                                    item {
                                        MessageItem(
                                            message = com.example.core.database.Message(
                                                id = "streaming",
                                                conversationId = "",
                                                role = "model",
                                                content = streamResponse
                                            ),
                                            isArabic = isArabic,
                                            isStreaming = true,
                                            isLoading = streamResponse.isEmpty(),
                                            onShowFeedbackSuccess = {
                                                scope.launch {
                                                    snackbarHostState.showSnackbar(if (isArabic) "شكرًا لك على ملاحظاتك." else "Thank you for your feedback.")
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                            
                            // Auto-scroll floating navigation button
                            val showScrollToBottom by remember {
                                derivedStateOf {
                                    listState.firstVisibleItemIndex > 1
                                }
                            }
                            
                            androidx.compose.animation.AnimatedVisibility(
                                visible = showScrollToBottom,
                                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                                modifier = Modifier
                                    .align(if (isArabic) Alignment.BottomStart else Alignment.BottomEnd)
                                    .padding(16.dp)
                            ) {
                                FloatingActionButton(
                                    onClick = {
                                        coroutineScope.launch {
                                            if (filteredMessages.isNotEmpty()) {
                                                listState.animateScrollToItem(filteredMessages.lastIndex + (if (isGenerating) 1 else 0))
                                            }
                                        }
                                    },
                                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    shape = CircleShape,
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Icon(Icons.Rounded.ArrowDownward, contentDescription = "Scroll Down", modifier = Modifier.size(20.dp))
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
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(painter = painterResource(id = R.drawable.logo), contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(80.dp))
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = if (isArabic) "كيف يمكنني مساعدتك اليوم؟" else "How can I assist you today?",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

// Renders visual markdown nodes block by block with high typography fidelity
@Composable
fun MarkdownText(content: String, isArabic: Boolean) {
    val segments = remember(content) { parseMarkdown(content) }
    val density = LocalDensity.current
    val context = LocalContext.current
    
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        segments.forEach { segment ->
            when (segment) {
                is MarkdownSegment.TextBlock -> {
                    Text(
                        text = formatInlineMarkdown(segment.content, isArabic),
                        style = MaterialTheme.typography.bodyLarge,
                        lineHeight = 24.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                is MarkdownSegment.HeaderBlock -> {
                    val scaleFactor = when (segment.level) {
                        1 -> 1.35f
                        2 -> 1.2f
                        else -> 1.1f
                    }
                    val style = MaterialTheme.typography.titleMedium
                    Text(
                        text = formatInlineMarkdown(segment.content, isArabic),
                        fontSize = (style.fontSize.value * scaleFactor).sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                    )
                }
                is MarkdownSegment.CodeBlock -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
                    ) {
                        Column {
                            // Code block Header
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF2D2D2D))
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = segment.language.uppercase(Locale.ROOT).ifEmpty { "CODE" },
                                    color = Color(0xFFCCCCCC),
                                    fontSize = 11.sp,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                                Row(
                                    modifier = Modifier.clickable {
                                        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                        val clip = android.content.ClipData.newPlainText("Copied Code", segment.code)
                                        clipboard.setPrimaryClip(clip)
                                        Toast.makeText(context, if (isArabic) "تم نسخ الكود" else "Code copied to clipboard", Toast.LENGTH_SHORT).show()
                                    },
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Rounded.ContentCopy, contentDescription = "Copy", tint = Color(0xFFCCCCCC), modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isArabic) "نسخ" else "Copy",
                                        color = Color(0xFFCCCCCC),
                                        fontSize = 11.sp
                                    )
                                }
                            }
                            
                            // Horizontal scrolling code area
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(androidx.compose.foundation.rememberScrollState())
                                    .padding(14.dp)
                            ) {
                                Text(
                                    text = highlightSyntax(segment.code, segment.language),
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    fontSize = 13.sp,
                                    color = Color(0xFFD4D4D4),
                                    lineHeight = 20.sp
                                )
                            }
                        }
                    }
                }
                is MarkdownSegment.BulletListBlock -> {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(start = 8.dp)) {
                        segment.items.forEach { bulletText ->
                            Row(verticalAlignment = Alignment.Top) {
                                Text(
                                    text = "• ",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = formatInlineMarkdown(bulletText, isArabic),
                                    style = MaterialTheme.typography.bodyLarge,
                                    lineHeight = 22.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
                is MarkdownSegment.OrderedListBlock -> {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(start = 8.dp)) {
                        segment.items.forEachIndexed { idx, orderedText ->
                            Row(verticalAlignment = Alignment.Top) {
                                Text(
                                    text = "${idx + 1}. ",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = formatInlineMarkdown(orderedText, isArabic),
                                    style = MaterialTheme.typography.bodyLarge,
                                    lineHeight = 22.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
                is MarkdownSegment.BlockQuoteBlock -> {
                    Row(modifier = Modifier.padding(vertical = 4.dp)) {
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .height(IntrinsicSize.Min)
                                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp))
                                .padding(vertical = 2.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = formatInlineMarkdown(segment.content, isArabic),
                            style = MaterialTheme.typography.bodyLarge.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
                            lineHeight = 22.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AiResponseToolbar(
    isArabic: Boolean,
    content: String,
    onDelete: () -> Unit,
    onEditClick: () -> Unit,
    onShowFeedbackSuccess: () -> Unit
) {
    val context = LocalContext.current
    var reaction by remember { mutableStateOf<Boolean?>(null) } // true = like, false = dislike
    var showFeedbackSheet by remember { mutableStateOf(false) }

    if (showFeedbackSheet) {
        FeedbackBottomSheet(
            isArabic = isArabic,
            onDismiss = { showFeedbackSheet = false },
            onSubmit = {
                showFeedbackSheet = false
                onShowFeedbackSuccess()
            }
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, start = 4.dp, end = 4.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Copy Button
        IconButton(
            onClick = {
                val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                val clip = android.content.ClipData.newPlainText("Copied Text", content)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(context, if (isArabic) "تم نسخ النص" else "Copied to clipboard", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.ContentCopy,
                contentDescription = "Copy",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(16.dp)
            )
        }
        
        // Like Button
        IconButton(
            onClick = {
                reaction = if (reaction == true) null else true
            },
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = if (reaction == true) Icons.Rounded.ThumbUp else Icons.Outlined.ThumbUp,
                contentDescription = "Like",
                tint = if (reaction == true) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(16.dp)
            )
        }
        
        // Dislike Button
        IconButton(
            onClick = {
                val newReaction = if (reaction == false) null else false
                reaction = newReaction
                if (newReaction == false) {
                    showFeedbackSheet = true
                }
            },
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = if (reaction == false) Icons.Rounded.ThumbDown else Icons.Outlined.ThumbDown,
                contentDescription = "Dislike",
                tint = if (reaction == false) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(16.dp)
            )
        }
        
        // Share Button
        IconButton(
            onClick = {
                val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(android.content.Intent.EXTRA_TEXT, content)
                }
                context.startActivity(android.content.Intent.createChooser(intent, null))
            },
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Share,
                contentDescription = "Share",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

// Gorgeous modernized message card item bubble supporting adaptive widths & rich imagery
@Composable
fun MessageItem(
    message: com.example.core.database.Message,
    isArabic: Boolean,
    isStreaming: Boolean = false,
    isLoading: Boolean = false,
    onRetry: () -> Unit = {},
    onDelete: () -> Unit = {},
    onEdit: (String) -> Unit = {},
    onShowFeedbackSuccess: () -> Unit = {}
) {
    val isUser = message.role == "user"
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var editMessageText by remember { mutableStateOf(message.content) }

    if (showEditDialog) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showEditDialog = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Top Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { showEditDialog = false }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Close")
                        }
                        Text(
                            text = if (isArabic) "تعديل الرسالة" else "Edit message",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 16.dp),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.size(48.dp)) // To balance the close button
                    }

                    // Info Banner
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Info,
                            contentDescription = "Info",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = if (isArabic) "سيؤدي تعديل هذه الرسالة إلى إعادة بدء المحادثة من هنا." else "Editing this message will restart the conversation from here.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Text Input Area
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 16.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        IconButton(
                            onClick = { /* Do nothing for now */ },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = "Add",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp)
                                .heightIn(min = 40.dp, max = 120.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            androidx.compose.foundation.text.BasicTextField(
                                value = editMessageText,
                                onValueChange = { editMessageText = it },
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                                cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
                                maxLines = 5
                            )
                        }
                        
                        IconButton(
                            onClick = {
                                onEdit(editMessageText)
                                showEditDialog = false
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(androidx.compose.foundation.shape.CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ArrowUpward,
                                contentDescription = "Send",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(painter = painterResource(id = R.drawable.logo), contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
        }
        
        val bubbleShape = RoundedCornerShape(
            topStart = 24.dp,
            topEnd = 24.dp,
            bottomStart = if (isUser) 24.dp else 4.dp,
            bottomEnd = if (isUser) 4.dp else 24.dp
        )
        
        val formattedTime = remember(message.timestamp) {
            try {
                val sdf = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
                val formatted = sdf.format(java.util.Date(message.timestamp))
                if (isArabic) {
                    formatted.replace("AM", "ص").replace("PM", "م")
                } else {
                    formatted
                }
            } catch (e: Exception) {
                ""
            }
        }
        
        Column(
            modifier = Modifier.weight(1f, fill = false)
        ) {
            Surface(
                shape = bubbleShape,
                color = if (isUser) MaterialTheme.colorScheme.surfaceContainer else MaterialTheme.colorScheme.surface,
                border = if (!isUser) androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                ) else null,
                tonalElevation = if (!isUser) 1.dp else 0.dp,
                modifier = Modifier
                    .clickable(enabled = !isLoading) { showMenu = true }
            ) {
                Box {
                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                        if (message.imageUri != null) {
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 240.dp)
                                    .padding(bottom = 8.dp)
                            ) {
                                androidx.compose.foundation.Image(
                                    painter = coil.compose.rememberAsyncImagePainter(message.imageUri),
                                    contentDescription = "Attached Image",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )
                            }
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
                                Icon(Icons.Rounded.Description, contentDescription = "Document", tint = MaterialTheme.colorScheme.primary)
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
                                TypingAnimation()
                                if (message.content.isNotEmpty()) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = message.content, 
                                        style = MaterialTheme.typography.bodyMedium, 
                                        color = if (isUser) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            if (isUser) {
                                Text(
                                    text = message.content,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            } else {
                                // Blinking cursor setup for streaming text
                                val finalContent = if (isStreaming && message.content.isNotEmpty()) {
                                    val infiniteTransition = rememberInfiniteTransition(label = "cursor")
                                    val alpha by infiniteTransition.animateFloat(
                                        initialValue = 0.2f,
                                        targetValue = 1f,
                                        animationSpec = infiniteRepeatable(
                                            animation = tween(durationMillis = 400, easing = LinearEasing),
                                            repeatMode = RepeatMode.Reverse
                                        ),
                                        label = "alpha"
                                    )
                                    message.content + " ▌"
                                } else {
                                    message.content
                                }
                                
                                MarkdownRenderer(text = finalContent)
                            }
                            
                            if (message.content.startsWith("An error occurred:") || message.content.startsWith("حدث خطأ:")) {
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedButton(onClick = onRetry, shape = RoundedCornerShape(10.dp), modifier = Modifier.minimumInteractiveComponentSize()) {
                                    Icon(Icons.Rounded.Refresh, contentDescription = "Retry")
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(if (isArabic) "إعادة المحاولة" else "Retry")
                                }
                            }
                        }
                        
                        // Elegantly placed, ultra-subtle timestamp inside bubble
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = formattedTime,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                            color = if (isUser) {
                                MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            },
                            modifier = Modifier.align(Alignment.End)
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(vertical = 4.dp)
                            .width(220.dp)
                    ) {
                        Text(
                            text = if (formattedTime.isNotEmpty()) "Today, $formattedTime" else "Today",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                        )
                        DropdownMenuItem(
                            text = { Text(if (isArabic) "نسخ" else "Copy", fontWeight = FontWeight.SemiBold) },
                            leadingIcon = { Icon(Icons.Outlined.ContentCopy, null, tint = MaterialTheme.colorScheme.onSurface) },
                            onClick = {
                                val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                val clip = android.content.ClipData.newPlainText("Copied Text", message.content)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, if (isArabic) "تم النسخ" else "Copied to clipboard", Toast.LENGTH_SHORT).show()
                                showMenu = false
                            },
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                        )

                        if (isUser) {
                            DropdownMenuItem(
                                text = { Text(if (isArabic) "تعديل الرسالة" else "Edit message", fontWeight = FontWeight.SemiBold) },
                                leadingIcon = { Icon(Icons.Outlined.Edit, null, tint = MaterialTheme.colorScheme.onSurface) },
                                onClick = {
                                    showMenu = false
                                    showEditDialog = true
                                },
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }
            
            if (!isUser && !isLoading) {
                AiResponseToolbar(
                    isArabic = isArabic,
                    content = message.content,
                    onDelete = onDelete,
                    onEditClick = { showEditDialog = true },
                    onShowFeedbackSuccess = onShowFeedbackSuccess
                )
            }
        }
    }
}

// Beautifully format URI sizes for display in composer
fun getUriSizeFormatted(context: android.content.Context, uri: android.net.Uri): String {
    try {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
            if (cursor.moveToFirst() && sizeIndex >= 0) {
                val sizeBytes = cursor.getLong(sizeIndex)
                return when {
                    sizeBytes >= 1024 * 1024 -> String.format(java.util.Locale.US, "%.1f MB", sizeBytes.toFloat() / (1024 * 1024))
                    sizeBytes >= 1024 -> "${sizeBytes / 1024} KB"
                    else -> "$sizeBytes B"
                }
            }
        }
    } catch (e: Exception) {
        // Fallback
    }
    return "0 KB"
}

@Composable
fun AttachmentMenuItem(
    isArabic: Boolean,
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
                if (subtitle.isNotEmpty()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// Capsule styling parameters with high fluid visual accents
@Composable
@OptIn(ExperimentalFoundationApi::class)
fun BottomInputArea(
    isArabic: Boolean,
    text: String,
    onTextChange: (String) -> Unit,
    onSend: (String) -> Unit,
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
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

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
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Elegant Premium ChatGPT-style capsule layout
Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(32.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                border = if (isFocused) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                shadowElevation = if (isFocused) 4.dp else 0.dp
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // 1. Error Display Area inside Card
                    if (attachError != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.errorContainer)
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Rounded.ErrorOutline, contentDescription = "Error", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = attachError,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(onClick = onRemoveAttachment, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Rounded.Close, contentDescription = "Clear", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    // 2. Beautiful Attachment Row inside Card
                    if (attachedImageUri != null || attachedDocUri != null) {
                        androidx.compose.foundation.lazy.LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (attachedImageUri != null) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .size(80.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                    ) {
                                        AsyncImage(
                                            model = attachedImageUri,
                                            contentDescription = "Image preview",
                                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )

                                        // Image size overlay
                                        val sizeStr = getUriSizeFormatted(context, attachedImageUri)
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .align(Alignment.BottomCenter)
                                                .background(
                                                    androidx.compose.ui.graphics.Brush.verticalGradient(
                                                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f))
                                                    )
                                                )
                                                .padding(vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = sizeStr,
                                                color = Color(0xFFFAFAFA),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.align(Alignment.Center)
                                            )
                                        }

                                        // Upload progress overlay
                                        if (isAttaching) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(Color.Black.copy(alpha = 0.4f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                CircularProgressIndicator(
                                                    progress = attachProgress,
                                                    modifier = Modifier.size(24.dp),
                                                    strokeWidth = 2.dp,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }

                                        // Close Button
                                        IconButton(
                                            onClick = onRemoveAttachment,
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(4.dp)
                                                .size(20.dp)
                                                .clip(CircleShape)
                                                .background(Color.Black.copy(alpha = 0.5f))
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Close,
                                                contentDescription = "Remove",
                                                tint = Color(0xFFFAFAFA),
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            if (attachedDocUri != null) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .height(80.dp)
                                            .widthIn(min = 140.dp, max = 200.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                            .padding(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxSize(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Description,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = attachedDocName ?: "Document",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    fontWeight = FontWeight.Medium
                                                )
                                                Text(
                                                    text = getUriSizeFormatted(context, attachedDocUri),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                        
                                        // Upload progress overlay
                                        if (isAttaching) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                CircularProgressIndicator(
                                                    progress = attachProgress,
                                                    modifier = Modifier.size(24.dp),
                                                    strokeWidth = 2.dp,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }

                                        // Close Button
                                        IconButton(
                                            onClick = onRemoveAttachment,
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(4.dp)
                                                .size(20.dp)
                                                .clip(CircleShape)
                                                .background(Color.Black.copy(alpha = 0.5f))
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Close,
                                                contentDescription = "Remove",
                                                tint = Color(0xFFFAFAFA),
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 3. Message Input Area and Controls in a single row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 4.dp)
                            .heightIn(min = 56.dp, max = 200.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        // Attachment Button with Custom popup anchoring
                        Box {
                            IconButton(
                                onClick = { showAttachmentMenu = true },
                                modifier = Modifier
                                    .padding(bottom = 8.dp)
                                    .size(40.dp)
                                    .clip(CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Add,
                                    contentDescription = "Attach",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            // Modern Material 3 floating popup
                            if (showAttachmentMenu) {
                                Popup(
                                    alignment = Alignment.TopStart,
                                    offset = androidx.compose.ui.unit.IntOffset(x = 0, y = -195),
                                    onDismissRequest = { showAttachmentMenu = false },
                                    properties = androidx.compose.ui.window.PopupProperties(focusable = true)
                                ) {
                                    Surface(
                                        modifier = Modifier
                                            .width(230.dp)
                                            .shadow(12.dp, RoundedCornerShape(24.dp)),
                                        shape = RoundedCornerShape(24.dp),
                                        color = MaterialTheme.colorScheme.surface,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(6.dp),
                                            verticalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            AttachmentMenuItem(
                                                isArabic = isArabic,
                                                title = if (isArabic) "كاميرا" else "Camera",
                                                subtitle = if (isArabic) "التقاط صورة فورية" else "Take a picture",
                                                icon = Icons.Rounded.CameraAlt,
                                                onClick = {
                                                    showAttachmentMenu = false
                                                    cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                                                }
                                            )
                                            AttachmentMenuItem(
                                                isArabic = isArabic,
                                                title = if (isArabic) "صور" else "Photos",
                                                subtitle = if (isArabic) "اختر من معرض الصور" else "Select from Gallery",
                                                icon = Icons.Rounded.Image,
                                                onClick = {
                                                    showAttachmentMenu = false
                                                    imagePicker.launch(arrayOf("image/*"))
                                                }
                                            )
                                            AttachmentMenuItem(
                                                isArabic = isArabic,
                                                title = if (isArabic) "ملفات" else "Files",
                                                subtitle = if (isArabic) "رفع مستند أو ملف" else "Upload a document",
                                                icon = Icons.Rounded.Description,
                                                onClick = {
                                                    showAttachmentMenu = false
                                                    docPicker.launch(arrayOf("*/*"))
                                                }
                                            )
                                            AttachmentMenuItem(
                                                isArabic = isArabic,
                                                title = if (isArabic) "المكونات الإضافية" else "Extensions",
                                                subtitle = if (isArabic) "الأدوات والمكونات" else "Tools & extensions",
                                                icon = Icons.Rounded.Extension,
                                                onClick = {
                                                    showAttachmentMenu = false
                                                    android.widget.Toast.makeText(context, if (isArabic) "المكونات الإضافية قريباً!" else "Extensions coming soon!", android.widget.Toast.LENGTH_SHORT).show()
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Text Input
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 4.dp, vertical = 14.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (text.isEmpty()) {
                                Text(
                                    text = if (isArabic) "الرد على Nabih AI" else "Message Nabih AI",
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            androidx.compose.foundation.text.BasicTextField(
                                value = text,
                                onValueChange = onTextChange,
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                                cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
                                maxLines = 8,
                                interactionSource = interactionSource
                            )
                        }

                        // Send / Stop button inside AnimatedContent
                        AnimatedContent(
                            targetState = when {
                                isGenerating -> 0
                                text.isNotBlank() || attachedImageUri != null || attachedDocUri != null -> 1
                                else -> 2
                            },
                            transitionSpec = {
                                scaleIn(animationSpec = tween(180)) togetherWith scaleOut(animationSpec = tween(180))
                            },
                            label = "composer_action",
                            modifier = Modifier.padding(bottom = 8.dp, end = 8.dp)
                        ) { state ->
                            when (state) {
                                0 -> {
                                    IconButton(
                                        onClick = onStop,
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.error)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Stop,
                                            contentDescription = "Stop",
                                            tint = MaterialTheme.colorScheme.onError,
                                            modifier = Modifier.size(20.dp)
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
                                            imageVector = Icons.Rounded.ArrowUpward,
                                            contentDescription = "Send",
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                2 -> {
                                    // Disabled state
                                    IconButton(
                                        onClick = {},
                                        enabled = false,
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.ArrowUpward,
                                            contentDescription = "Send",
                                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                            modifier = Modifier.size(20.dp)
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


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ConversationItem(
    conversation: com.example.core.database.Conversation,
    isSelected: Boolean,
    isArabic: Boolean,
    onSelect: () -> Unit,
    onTogglePin: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    val formattedTime = remember(conversation.updatedAt) {
        val format = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault())
        format.format(java.util.Date(conversation.updatedAt))
    }

    Surface(
        onClick = onSelect,
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (conversation.isPinned) {
                    Icon(
                        imageVector = Icons.Rounded.PushPin,
                        contentDescription = "Pinned",
                        tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(18.dp)
                            .padding(end = 4.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = conversation.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Text(
                        text = formattedTime,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
            Box {
                IconButton(onClick = { showMenu = true }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Rounded.MoreVert, contentDescription = "More", tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                if (isArabic) {
                                    if (conversation.isPinned) "إلغاء التثبيت" else "تثبيت"
                                } else {
                                    if (conversation.isPinned) "Unpin" else "Pin"
                                }
                            )
                        },
                        onClick = { showMenu = false; onTogglePin() }
                    )
                    DropdownMenuItem(
                        text = { Text(if (isArabic) "إعادة تسمية" else "Rename") },
                        onClick = { showMenu = false; onRename() }
                    )
                    DropdownMenuItem(
                        text = { Text(if (isArabic) "حذف" else "Delete", color = MaterialTheme.colorScheme.error) },
                        onClick = { showMenu = false; onDelete() }
                    )
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun MainDrawerContent(
    settings: com.example.core.model.AppSettings,
    selectedModel: com.example.core.model.AiModel,
    activeConversationId: String?,
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
    onToggleArchiveConversation: (com.example.core.database.Conversation) -> Unit,
    onUpdateTheme: (com.example.core.model.AppTheme) -> Unit,
    onUpdateLanguage: (com.example.core.model.AppLanguage) -> Unit
) {
    val isArabic = settings.language == com.example.core.model.AppLanguage.ARABIC
    val context = androidx.compose.ui.platform.LocalContext.current
    
    val (pinnedConversations, unpinnedConversations) = remember(conversations) {
        conversations.partition { it.isPinned }
    }
    
    var conversationToRename by remember { mutableStateOf<com.example.core.database.Conversation?>(null) }
    var renameNewTitle by remember { mutableStateOf("") }
    
    if (conversationToRename != null) {
        AlertDialog(
            onDismissRequest = { conversationToRename = null },
            title = { Text(if (isArabic) "إعادة تسمية المحادثة" else "Rename Conversation") },
            text = {
                OutlinedTextField(
                    value = renameNewTitle,
                    onValueChange = { renameNewTitle = it },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    onRenameConversation(conversationToRename!!.id, renameNewTitle)
                    conversationToRename = null
                }) {
                    Text(if (isArabic) "حفظ" else "Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { conversationToRename = null }) {
                    Text(if (isArabic) "إلغاء" else "Cancel")
                }
            }
        )
    }

    ModalDrawerSheet(drawerContainerColor = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Nabih AI", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                IconButton(onClick = { onNewChat(); onCloseDrawer() }) {
                    Icon(Icons.Rounded.Add, contentDescription = "New Chat", tint = MaterialTheme.colorScheme.onBackground)
                }
            }
            
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            
            androidx.compose.foundation.lazy.LazyColumn(modifier = Modifier.weight(1f).padding(vertical = 8.dp)) {
                
                item {
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(24.dp)) },
                        label = { Text(if (isArabic) "البحث" else "Search", style = MaterialTheme.typography.bodyLarge) },
                        selected = false,
                        onClick = { onNavigateTo("search"); onCloseDrawer() },
                        colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
                    )
                }
                
                item {
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Rounded.FolderOpen, contentDescription = null, modifier = Modifier.size(24.dp)) },
                        label = { Text(if (isArabic) "الملفات" else "Files", style = MaterialTheme.typography.bodyLarge) },
                        selected = false,
                        onClick = { onNavigateTo("files"); onCloseDrawer() },
                        colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
                    )
                }

                item {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 8.dp))
                }

                if (pinnedConversations.isNotEmpty()) {
                    item {
                        Text(
                            text = if (isArabic) "المحادثات المثبتة" else "Pinned",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                        )
                    }
                    
                    items(pinnedConversations, key = { it.id }) { conv ->
                        ConversationItem(
                            conversation = conv,
                            isSelected = conv.id == activeConversationId,
                            isArabic = isArabic,
                            onSelect = { onSelectConversation(conv.id); onCloseDrawer() },
                            onTogglePin = { onTogglePinConversation(conv) },
                            onRename = { 
                                renameNewTitle = conv.title
                                conversationToRename = conv
                            },
                            onDelete = { onDeleteConversation(conv.id) },
                            modifier = Modifier.animateItemPlacement()
                        )
                    }
                    
                    item {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }

                if (unpinnedConversations.isNotEmpty()) {
                    if (pinnedConversations.isNotEmpty()) {
                        item {
                            Text(
                                text = if (isArabic) "المحادثات الأخيرة" else "Recent",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                            )
                        }
                    }
                    
                    items(unpinnedConversations, key = { it.id }) { conv ->
                        ConversationItem(
                            conversation = conv,
                            isSelected = conv.id == activeConversationId,
                            isArabic = isArabic,
                            onSelect = { onSelectConversation(conv.id); onCloseDrawer() },
                            onTogglePin = { onTogglePinConversation(conv) },
                            onRename = { 
                                renameNewTitle = conv.title
                                conversationToRename = conv
                            },
                            onDelete = { onDeleteConversation(conv.id) },
                            modifier = Modifier.animateItemPlacement()
                        )
                    }
                }
            }
            
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            
            Column(modifier = Modifier.padding(8.dp)) {
                // API Keys Shortcut
                NavigationDrawerItem(
                    icon = { Icon(Icons.Rounded.VpnKey, contentDescription = null, modifier = Modifier.size(24.dp)) },
                    label = { Text(if (isArabic) "مفاتيح API" else "API Keys", style = MaterialTheme.typography.bodyLarge) },
                    selected = false,
                    onClick = { onNavigateTo("api_keys"); onCloseDrawer() },
                    colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                NavigationDrawerItem(
                    icon = {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            if (settings.profilePictureUri.isNotEmpty()) {
                                AsyncImage(
                                    model = settings.profilePictureUri,
                                    contentDescription = "Profile Picture",
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                val initials = settings.userName.split(" ").filter { it.isNotEmpty() }.take(2).joinToString("") { it.take(1) }.uppercase()
                                if (initials.isNotEmpty()) {
                                    Text(
                                        text = initials,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Rounded.Person,
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp),
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    },
                    label = {
                        Column {
                            Text(
                                text = if (isArabic) "الحساب" else "Account",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                            if (settings.userName.isNotEmpty()) {
                                Text(
                                    text = settings.userName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    selected = false,
                    onClick = { onNavigateTo("account"); onCloseDrawer() },
                    colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Rounded.Settings, contentDescription = null, modifier = Modifier.size(24.dp)) },
                    label = { Text(if (isArabic) "الإعدادات" else "Settings", style = MaterialTheme.typography.bodyLarge) },
                    selected = false,
                    onClick = { onNavigateToSettings(); onCloseDrawer() },
                    colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }
    }
}
