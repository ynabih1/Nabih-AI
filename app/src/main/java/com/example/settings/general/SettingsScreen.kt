package com.example.settings.general

import com.example.settings.profile.SettingsViewModel

import com.example.models.AppLanguage
import com.example.models.AppSettings
import com.example.models.AppTheme
import com.example.models.ApiProvider
import com.example.chat.ui.ProviderIcon

import kotlinx.coroutines.launch
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsViewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
    val isArabic = settings.language == AppLanguage.ARABIC
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(if (isArabic) "الإعدادات" else "Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.minimumInteractiveComponentSize()) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 48.dp)
        ) {
            item {
                PreferencesSection(settings, settingsViewModel, isArabic)
            }
            item {
                NotificationsSection(settings, settingsViewModel, isArabic, snackbarHostState)
            }
        }
    }
}

@Composable
fun SettingsSectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color = MaterialTheme.colorScheme.primary,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(bottom = 12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            content()
        }
    }
}

@Composable
fun AiConfigurationSection(
    settings: AppSettings,
    viewModel: SettingsViewModel,
    isArabic: Boolean,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    var nabihKey by remember { mutableStateOf(settings.nabihApiKey) }
    var openaiKey by remember { mutableStateOf(settings.openaiApiKey) }
    var anthropicKey by remember { mutableStateOf(settings.anthropicApiKey) }
    var googleKey by remember { mutableStateOf(settings.googleApiKey) }

    LaunchedEffect(settings.nabihApiKey) { nabihKey = settings.nabihApiKey }
    LaunchedEffect(settings.openaiApiKey) { openaiKey = settings.openaiApiKey }
    LaunchedEffect(settings.anthropicApiKey) { anthropicKey = settings.anthropicApiKey }
    LaunchedEffect(settings.googleApiKey) { googleKey = settings.googleApiKey }

    Box(modifier = modifier.fillMaxSize()) {
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(bottom = 24.dp)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ApiKeyCard(
                id = "nabih",
                name = "Nabih Ultra",
                savedKey = settings.nabihApiKey,
                initialKey = nabihKey,
                onSaveKey = { key ->
                    nabihKey = key
                    viewModel.saveApiKeys(key, googleKey, openaiKey, anthropicKey)
                    coroutineScope.launch { com.example.models.ModelRegistry.syncAndRefresh(context) }
                },
                isArabic = isArabic,
                isDefault = true,
                snackbarHostState = snackbarHostState,
                onKeyChange = { nabihKey = it },
                onDeleteKey = {
                    nabihKey = ""
                    viewModel.saveApiKeys("", googleKey, openaiKey, anthropicKey)
                    coroutineScope.launch { com.example.models.ModelRegistry.syncAndRefresh(context) }
                }
            )
            
            ApiKeyCard(
                id = "openai",
                name = "ChatGPT",
                savedKey = settings.openaiApiKey,
                initialKey = openaiKey,
                onSaveKey = { key ->
                    openaiKey = key
                    viewModel.saveApiKeys(nabihKey, googleKey, key, anthropicKey)
                    coroutineScope.launch { com.example.models.ModelRegistry.syncAndRefresh(context) }
                },
                isArabic = isArabic,
                isDefault = false,
                snackbarHostState = snackbarHostState,
                onKeyChange = { openaiKey = it },
                onDeleteKey = {
                    openaiKey = ""
                    viewModel.saveApiKeys(nabihKey, googleKey, "", anthropicKey)
                    coroutineScope.launch { com.example.models.ModelRegistry.syncAndRefresh(context) }
                }
            )
            
            ApiKeyCard(
                id = "claude",
                name = "Claude",
                savedKey = settings.anthropicApiKey,
                initialKey = anthropicKey,
                onSaveKey = { key ->
                    anthropicKey = key
                    viewModel.saveApiKeys(nabihKey, googleKey, openaiKey, key)
                    coroutineScope.launch { com.example.models.ModelRegistry.syncAndRefresh(context) }
                },
                isArabic = isArabic,
                isDefault = false,
                snackbarHostState = snackbarHostState,
                onKeyChange = { anthropicKey = it },
                onDeleteKey = {
                    anthropicKey = ""
                    viewModel.saveApiKeys(nabihKey, googleKey, openaiKey, "")
                    coroutineScope.launch { com.example.models.ModelRegistry.syncAndRefresh(context) }
                }
            )
            
            ApiKeyCard(
                id = "google",
                name = "Gemini",
                savedKey = settings.googleApiKey,
                initialKey = googleKey,
                onSaveKey = { key ->
                    googleKey = key
                    viewModel.saveApiKeys(nabihKey, key, openaiKey, anthropicKey)
                    coroutineScope.launch { com.example.models.ModelRegistry.syncAndRefresh(context) }
                },
                isArabic = isArabic,
                isDefault = false,
                isOptional = true,
                snackbarHostState = snackbarHostState,
                onKeyChange = { googleKey = it },
                onDeleteKey = {
                    googleKey = ""
                    viewModel.saveApiKeys(nabihKey, "", openaiKey, anthropicKey)
                    coroutineScope.launch { com.example.models.ModelRegistry.syncAndRefresh(context) }
                }
            )
        }
    }
}

class ApiKeyVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val originalText = text.text
        if (originalText.isEmpty()) return TransformedText(text, OffsetMapping.Identity)
        
        val visibleCount = minOf(originalText.length, 4)
        val maskedCount = originalText.length - visibleCount
        
        val maskedBuilder = StringBuilder()
        for (i in 0 until maskedCount) {
            maskedBuilder.append('•')
            if ((i + 1) % 4 == 0 && i != maskedCount - 1) {
                maskedBuilder.append(' ')
            }
        }
        if (maskedCount > 0 && visibleCount > 0) {
            maskedBuilder.append(' ')
        }
        if (visibleCount > 0) {
            maskedBuilder.append(originalText.takeLast(visibleCount))
        }
        
        val transformedText = AnnotatedString(maskedBuilder.toString())
        
        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= 0) return 0
                var transformedOffset = 0
                var origIndex = 0
                while (origIndex < offset && origIndex < originalText.length) {
                    if (origIndex < maskedCount) {
                        transformedOffset++
                        if ((origIndex + 1) % 4 == 0 && origIndex != maskedCount - 1) {
                            transformedOffset++
                        }
                    } else {
                        if (origIndex == maskedCount && maskedCount > 0) {
                            transformedOffset++
                        }
                        transformedOffset++
                    }
                    origIndex++
                }
                return transformedOffset
            }

            override fun transformedToOriginal(offset: Int): Int {
                if (offset <= 0) return 0
                var origIndex = 0
                var transIndex = 0
                while (transIndex < offset && origIndex < originalText.length) {
                    if (origIndex < maskedCount) {
                        transIndex++
                        if ((origIndex + 1) % 4 == 0 && origIndex != maskedCount - 1) {
                            if (transIndex == offset) return origIndex + 1
                            transIndex++
                        }
                    } else {
                        if (origIndex == maskedCount && maskedCount > 0) {
                            if (transIndex == offset) return origIndex
                            transIndex++
                        }
                        transIndex++
                    }
                    origIndex++
                }
                return origIndex
            }
        }
        return TransformedText(transformedText, offsetMapping)
    }
}

@Composable
fun ApiKeyCard(
    id: String,
    name: String,
    savedKey: String,
    initialKey: String,
    onSaveKey: (String) -> Unit,
    isArabic: Boolean,
    isDefault: Boolean = false,
    isOptional: Boolean = false,
    snackbarHostState: SnackbarHostState,
    onKeyChange: (String) -> Unit,
    onDeleteKey: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var tempKey by remember(initialKey) { mutableStateOf(initialKey) }
    var isVerifying by remember { mutableStateOf(false) }
    var isVerified by remember { mutableStateOf(false) }
    var isKeyVisible by remember { mutableStateOf(false) }

    LaunchedEffect(tempKey) {
        if (tempKey != savedKey) {
            isVerified = false
        }
    }

    val status = when {
        isVerifying -> if (isArabic) "جاري التحقق..." else "Verifying..."
        isVerified || (tempKey.isNotEmpty() && tempKey == savedKey) -> if (isArabic) "متصل" else "Connected"
        else -> if (isArabic) "غير متصل" else "Not Connected"
    }

    val statusBgColor = when {
        isVerifying -> Color(0xFFE8F0FE)
        status == "Connected" || status == "متصل" -> Color(0xFFE3F5E9)
        else -> Color(0xFFF1F3F4)
    }

    val statusTextColor = when {
        isVerifying -> Color(0xFF1A73E8)
        status == "Connected" || status == "متصل" -> Color(0xFF137333)
        else -> Color(0xFF5F6368)
    }

    val dotColor = when {
        isVerifying -> Color(0xFF1A73E8)
        status == "Connected" || status == "متصل" -> Color(0xFF1E8E3E)
        else -> Color(0xFF9AA0A6)
    }

    val isConnected = status == "Connected" || status == "متصل"
    
    val border = when {
        isDefault -> BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
        isConnected -> BorderStroke(1.5.dp, Color(0xFFC4EAD3))
        else -> BorderStroke(1.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    }

    val containerColor = if (isDefault) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text(if (isArabic) "حذف المفتاح" else "Delete Key") },
            text = { Text(if (isArabic) "هل تريد حذف مفتاح $name؟" else "Are you sure you want to delete the $name key?") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDeleteKey()
                        tempKey = ""
                        isVerified = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(if (isArabic) "حذف" else "Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text(if (isArabic) "إلغاء" else "Cancel")
                }
            }
        )
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = border,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = if (isOptional) "$name ${if (isArabic) "(اختياري)" else "(Optional)"}" else name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (isDefault) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (isArabic) "الافتراضي" else "Default",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .background(statusBgColor, RoundedCornerShape(16.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(dotColor, CircleShape)
                    )
                    Text(
                        text = status,
                        color = statusTextColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            val placeholderText = when (id) {
                "nabih" -> if (isArabic) "أدخل مفتاح API الخاص بـ Nabih Ultra" else "Enter Nabih Ultra API Key"
                "openai" -> if (isArabic) "أدخل مفتاح API الخاص بـ ChatGPT" else "Enter ChatGPT API Key"
                "claude" -> if (isArabic) "أدخل مفتاح API الخاص بـ Claude" else "Enter Claude API Key"
                "google" -> if (isArabic) "أدخل مفتاح API الخاص بـ Gemini" else "Enter Gemini API Key"
                else -> if (isArabic) "أدخل مفتاح API" else "Enter API Key"
            }

            OutlinedTextField(
                value = tempKey,
                onValueChange = { 
                    tempKey = it
                    onKeyChange(it)
                },
                placeholder = {
                    Text(
                        text = placeholderText,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        fontSize = 14.sp
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                visualTransformation = if (isKeyVisible) VisualTransformation.None else ApiKeyVisualTransformation(),
                trailingIcon = {
                    IconButton(
                        onClick = { isKeyVisible = !isKeyVisible },
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            imageVector = if (isKeyVisible) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val verifyButtonBgColor by animateColorAsState(
                    targetValue = if (tempKey.isNotBlank() && !isVerifying) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
                    },
                    animationSpec = tween(durationMillis = 300)
                )
                val verifyButtonTextColor by animateColorAsState(
                    targetValue = if (tempKey.isNotBlank() && !isVerifying) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    },
                    animationSpec = tween(durationMillis = 300)
                )

                Button(
                    onClick = {
                        val cleanKey = tempKey.trim()
                        if (cleanKey.isBlank()) return@Button
                        coroutineScope.launch {
                            isVerifying = true
                            val success = testApiKeyConnection(id, cleanKey)
                            isVerifying = false
                            isVerified = success
                            if (success) {
                                snackbarHostState.showSnackbar(
                                    message = if (isArabic) "تم التحقق بنجاح" else "Verification successful",
                                    duration = SnackbarDuration.Short
                                )
                            } else {
                                snackbarHostState.showSnackbar(
                                    message = if (isArabic) "فشل التحقق من المفتاح" else "API Key verification failed",
                                    duration = SnackbarDuration.Short
                                )
                            }
                        }
                    },
                    enabled = tempKey.trim().isNotBlank() && !isVerifying,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = verifyButtonBgColor,
                        contentColor = verifyButtonTextColor,
                        disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
                        disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    ),
                    elevation = ButtonDefaults.buttonElevation(0.dp),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    if (isVerifying) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(if (isArabic) "تحقق" else "Verify", fontWeight = FontWeight.SemiBold)
                    }
                }
                
                val isSaveEnabled = tempKey.trim().isNotBlank() && !isVerifying
                val saveButtonBgColor by animateColorAsState(
                    targetValue = if (isSaveEnabled) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                    },
                    animationSpec = tween(durationMillis = 300)
                )
                val saveButtonTextColor by animateColorAsState(
                    targetValue = if (isSaveEnabled) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    },
                    animationSpec = tween(durationMillis = 300)
                )
                Button(
                    onClick = {
                        val cleanKey = tempKey.trim()
                        if (cleanKey.isBlank()) return@Button
                        coroutineScope.launch {
                            onSaveKey(cleanKey)
                            isVerified = true
                            snackbarHostState.showSnackbar(
                                message = if (isArabic) "تم حفظ المفتاح بنجاح" else "Key saved securely",
                                duration = SnackbarDuration.Short
                            )
                        }
                    },
                    enabled = isSaveEnabled,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = saveButtonBgColor,
                        contentColor = saveButtonTextColor,
                        disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                        disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    ),
                    elevation = ButtonDefaults.buttonElevation(0.dp),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (isArabic) "حفظ" else "Save", fontWeight = FontWeight.SemiBold)
                }
                
                if (savedKey.isNotBlank()) {
                    IconButton(
                        onClick = { showDeleteConfirmDialog = true },
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(16.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Delete,
                            contentDescription = if (isArabic) "حذف المفتاح" else "Delete Key",
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }
    }
}

suspend fun testApiKeyConnection(provider: String, key: String): Boolean = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
    val cleanKey = key.trim()
    if (cleanKey.isBlank()) return@withContext false
    try {
        val client = okhttp3.OkHttpClient.Builder()
            .connectTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
            .build()
            
        val request = when (provider) {
            "google" -> okhttp3.Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/models?key=$cleanKey")
                .get()
                .build()
            "openai" -> okhttp3.Request.Builder()
                .url("https://api.openai.com/v1/models")
                .header("Authorization", "Bearer $cleanKey")
                .get()
                .build()
            "claude" -> okhttp3.Request.Builder()
                .url("https://api.anthropic.com/v1/messages")
                .header("x-api-key", cleanKey)
                .header("anthropic-version", "2023-06-01")
                .post(okhttp3.RequestBody.create(null, ByteArray(0)))
                .build()
            "nabih" -> {
                if (cleanKey.startsWith("sk-ant-")) {
                    okhttp3.Request.Builder()
                        .url("https://api.anthropic.com/v1/messages")
                        .header("x-api-key", cleanKey)
                        .header("anthropic-version", "2023-06-01")
                        .post(okhttp3.RequestBody.create(null, ByteArray(0)))
                        .build()
                } else if (cleanKey.startsWith("sk-")) {
                    okhttp3.Request.Builder()
                        .url("https://api.openai.com/v1/models")
                        .header("Authorization", "Bearer $cleanKey")
                        .get()
                        .build()
                } else {
                    okhttp3.Request.Builder()
                        .url("https://generativelanguage.googleapis.com/v1beta/models?key=$cleanKey")
                        .get()
                        .build()
                }
            }
            else -> return@withContext false
        }
        
        val response = client.newCall(request).execute()
        
        if (provider == "claude" || (provider == "nabih" && cleanKey.startsWith("sk-ant-"))) {
            response.code != 401 && response.code != 403
        } else {
            response.isSuccessful
        }
    } catch (e: Exception) {
        false
    }
}


@Composable
fun CustomSelectionOption(
    selected: Boolean,
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    testTag: String = ""
) {
    val backgroundColor = if (selected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
    }
    val borderColor = if (selected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
    }
    val textColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val fontWeight = if (selected) {
        FontWeight.SemiBold
    } else {
        FontWeight.Medium
    }

    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .then(if (testTag.isNotEmpty()) Modifier.testTag(testTag) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (selected) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = fontWeight,
                color = textColor,
                maxLines = 1
            )
        }
    }
}

@Composable
fun PreferencesSection(settings: AppSettings, viewModel: SettingsViewModel, isArabic: Boolean) {
    SettingsSectionCard(
        title = if (isArabic) "التفضيلات" else "Preferences",
        icon = Icons.Rounded.Settings,
        iconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
    ) {
        Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 20.dp)) {
            // Language Label
            Text(
                text = if (isArabic) "اللغة" else "Language",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    AppLanguage.ENGLISH to "English",
                    AppLanguage.ARABIC to "العربية"
                ).forEach { (lang, label) ->
                    val isSelected = settings.language == lang
                    CustomSelectionOption(
                        selected = isSelected,
                        onClick = { viewModel.updateLanguage(lang) },
                        text = label,
                        modifier = Modifier.weight(1f),
                        testTag = if (lang == AppLanguage.ENGLISH) "lang_en_button" else "lang_ar_button"
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Theme Label
            Text(
                text = if (isArabic) "المظهر" else "Theme",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    AppTheme.LIGHT to if (isArabic) "فاتح" else "Light",
                    AppTheme.DARK to if (isArabic) "داكن" else "Dark",
                    AppTheme.SYSTEM to if (isArabic) "النظام" else "System"
                ).forEach { (theme, label) ->
                    val isSelected = settings.theme == theme
                    CustomSelectionOption(
                        selected = isSelected,
                        onClick = { viewModel.updateTheme(theme) },
                        text = label,
                        modifier = Modifier.weight(1f),
                        testTag = when(theme) {
                            AppTheme.LIGHT -> "theme_light_button"
                            AppTheme.DARK -> "theme_dark_button"
                            AppTheme.SYSTEM -> "theme_system_button"
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun NotificationsSection(
    settings: AppSettings,
    viewModel: SettingsViewModel,
    isArabic: Boolean,
    snackbarHostState: SnackbarHostState
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Permission request launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.updateNotificationsEnabled(true)
        } else {
            scope.launch {
                val actionLabel = if (isArabic) "الإعدادات" else "Settings"
                val message = if (isArabic) {
                    "الإشعارات معطلة في إعدادات النظام. يرجى تفعيلها."
                } else {
                    "Notifications are disabled in system settings. Please enable them."
                }
                val result = snackbarHostState.showSnackbar(
                    message = message,
                    actionLabel = actionLabel,
                    duration = SnackbarDuration.Long
                )
                if (result == SnackbarResult.ActionPerformed) {
                    try {
                        val intent = android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = android.net.Uri.fromParts("package", context.packageName, null)
                        }
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(context, if (isArabic) "عذرًا، لم نتمكن من فتح الإعدادات" else "Unable to open settings", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    SettingsSectionCard(
        title = if (isArabic) "الإشعارات" else "Notifications",
        icon = Icons.Rounded.Notifications
    ) {
        Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 20.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isArabic) "تفعيل الإشعارات" else "Enable Notifications",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (isArabic) "تلقي التنبيهات من Nabih AI" else "Receive notifications from Nabih AI",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = settings.notificationsEnabled,
                    onCheckedChange = { checked ->
                        if (checked) {
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                // Request permission on API 33+
                                val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                                    context,
                                    android.Manifest.permission.POST_NOTIFICATIONS
                                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                                
                                if (hasPermission) {
                                    viewModel.updateNotificationsEnabled(true)
                                } else {
                                    permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                }
                            } else {
                                viewModel.updateNotificationsEnabled(true)
                            }
                        } else {
                            viewModel.updateNotificationsEnabled(false)
                        }
                    },
                    modifier = Modifier.testTag("enable_notifications_switch")
                )
            }

            AnimatedVisibility(
                visible = settings.notificationsEnabled,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, top = 8.dp, bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    
                    // Sub option 1: New reply notifications
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isArabic) "إشعارات الرد الجديد" else "New Response Notifications",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isArabic) "عند اكتمال توليد الرد في الخلفية" else "When responses complete in background",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settings.completionNotifications,
                            onCheckedChange = { viewModel.updateCompletionNotifications(it) },
                            modifier = Modifier.testTag("new_response_notifications_switch")
                        )
                    }

                    // Sub option 2: Reminders & Updates
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isArabic) "التذكيرات والتحديثات" else "Reminders & Updates",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isArabic) "نصائح يومية وإشعارات دورية" else "Daily tips and periodic updates",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settings.remindersEnabled,
                            onCheckedChange = { viewModel.updateRemindersEnabled(it) },
                            modifier = Modifier.testTag("reminders_notifications_switch")
                        )
                    }
                }
            }
        }
    }
}


