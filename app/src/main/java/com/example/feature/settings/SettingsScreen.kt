package com.example.feature.settings

import com.example.core.model.AppLanguage
import com.example.core.model.AppSettings
import com.example.core.model.AppTheme
import com.example.core.model.ApiProvider
import com.example.core.ui.icon.ProviderIcon

import kotlinx.coroutines.launch
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsViewModel: SettingsViewModel,
    onClearChatHistory: () -> Unit = {},
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
    val isArabic = settings.language == AppLanguage.ARABIC
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                AiConfigurationSection(settings, settingsViewModel, isArabic, snackbarHostState)
            }
            item {
                StorageSettingsSection(isArabic, onClearChatHistory)
            }
        }
    }
}

@Composable
fun SettingsSectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
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
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
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
fun AiConfigurationSection(settings: AppSettings, viewModel: SettingsViewModel, isArabic: Boolean, snackbarHostState: SnackbarHostState) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    // Light theme enforced colors for ultra-minimal design
    val primaryBlue = MaterialTheme.colorScheme.primary
    val cardWhite = MaterialTheme.colorScheme.surface
    val backgroundGray = MaterialTheme.colorScheme.background
    val connectedGreen = Color(0xFF34A853)
    val notConnectedGray = Color(0xFF9AA0A6)
    val textDark = MaterialTheme.colorScheme.onSurface
    val borderLight = MaterialTheme.colorScheme.surfaceVariant
    
    var nabihKey by remember { mutableStateOf(settings.nabihApiKey) }
    var openaiKey by remember { mutableStateOf(settings.openaiApiKey) }
    var anthropicKey by remember { mutableStateOf(settings.anthropicApiKey) }
    var googleKey by remember { mutableStateOf(settings.googleApiKey) }

    val providersList = listOf(
        Triple("nabih", "Nabih Ultra", nabihKey),
        Triple("openai", "ChatGPT", openaiKey),
        Triple("claude", "Claude", anthropicKey),
        Triple("google", "Gemini", googleKey)
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundGray, RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        providersList.forEach { (id, name, savedKey) ->
            var tempKey by remember(savedKey) { mutableStateOf(savedKey) }
            var isVerifying by remember { mutableStateOf(false) }
            var isVerified by remember { mutableStateOf(false) }
            var isKeyVisible by remember { mutableStateOf(false) }
            
            val status = when {
                isVerifying -> if (isArabic) "جاري التحقق..." else "Verifying..."
                isVerified || savedKey.isNotEmpty() -> if (isArabic) "متصل" else "Connected"
                else -> if (isArabic) "غير متصل" else "Not Connected"
            }
            val statusColor = when {
                isVerifying -> primaryBlue
                isVerified || savedKey.isNotEmpty() -> connectedGreen
                else -> notConnectedGray
            }

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = cardWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = name,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = textDark
                            )
                            if (id == "nabih") {
                                Surface(
                                    color = primaryBlue.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = if (isArabic) "الافتراضي" else "Default",
                                        color = primaryBlue,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                        
                        Surface(
                            color = statusColor.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = status,
                                color = statusColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    OutlinedTextField(
                        value = tempKey,
                        onValueChange = { 
                            tempKey = it
                            isVerified = false
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        visualTransformation = if (isKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { isKeyVisible = !isKeyVisible }) {
                                Icon(
                                    imageVector = if (isKeyVisible) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                                    contentDescription = null,
                                    tint = notConnectedGray
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = cardWhite,
                            unfocusedContainerColor = cardWhite,
                            focusedBorderColor = primaryBlue,
                            unfocusedBorderColor = borderLight,
                            focusedTextColor = textDark,
                            unfocusedTextColor = textDark
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                if (tempKey.isBlank()) return@Button
                                coroutineScope.launch {
                                    isVerifying = true
                                    val success = testApiKeyConnection(id, tempKey)
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
                            enabled = !isVerifying && tempKey.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                contentColor = primaryBlue,
                                disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f).copy(alpha = 0.5f),
                                disabledContentColor = primaryBlue.copy(alpha = 0.5f)
                            ),
                            elevation = ButtonDefaults.buttonElevation(0.dp),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            if (isVerifying) {
                                CircularProgressIndicator(
                                    color = primaryBlue,
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(if (isArabic) "تحقق" else "Verify", fontWeight = FontWeight.SemiBold)
                            }
                        }
                        
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    when (id) {
                                        "nabih" -> { nabihKey = tempKey; viewModel.saveApiKeys(tempKey, googleKey, openaiKey, anthropicKey) }
                                        "google" -> { googleKey = tempKey; viewModel.saveApiKeys(nabihKey, tempKey, openaiKey, anthropicKey) }
                                        "openai" -> { openaiKey = tempKey; viewModel.saveApiKeys(nabihKey, googleKey, tempKey, anthropicKey) }
                                        "claude" -> { anthropicKey = tempKey; viewModel.saveApiKeys(nabihKey, googleKey, openaiKey, tempKey) }
                                    }
                                    snackbarHostState.showSnackbar(
                                        message = if (isArabic) "تم حفظ المفتاح" else "Key saved securely",
                                        duration = SnackbarDuration.Short
                                    )
                                }
                            },
                            enabled = isVerified,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = primaryBlue,
                                contentColor = cardWhite,
                                disabledContainerColor = borderLight,
                                disabledContentColor = notConnectedGray
                            ),
                            elevation = ButtonDefaults.buttonElevation(0.dp),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (isArabic) "حفظ" else "Save", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
        
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = { 
                    nabihKey = settings.nabihApiKey
                    googleKey = settings.googleApiKey
                    openaiKey = settings.openaiApiKey
                    anthropicKey = settings.anthropicApiKey
                },
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, borderLight),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = textDark),
                modifier = Modifier.weight(1f)
            ) {
                Text(if (isArabic) "إلغاء" else "Cancel", fontWeight = FontWeight.SemiBold)
            }
            
            Button(
                onClick = {
                    coroutineScope.launch {
                        viewModel.saveApiKeys(nabihKey, googleKey, openaiKey, anthropicKey)
                        com.example.core.model.ModelRegistry.syncAndRefresh(context)
                        snackbarHostState.showSnackbar(
                            message = if (isArabic) "تم حفظ الكل" else "All keys saved",
                            duration = SnackbarDuration.Short
                        )
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = primaryBlue,
                    contentColor = cardWhite
                ),
                elevation = ButtonDefaults.buttonElevation(0.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(if (isArabic) "حفظ الكل" else "Save All", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

suspend fun testApiKeyConnection(provider: String, key: String): Boolean = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
    if (key.isBlank()) return@withContext false
    try {
        val client = okhttp3.OkHttpClient.Builder()
            .connectTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
            .build()
            
        val request = when (provider) {
            "google" -> okhttp3.Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/models?key=$key")
                .get()
                .build()
            "openai" -> okhttp3.Request.Builder()
                .url("https://api.openai.com/v1/models")
                .header("Authorization", "Bearer $key")
                .get()
                .build()
            "claude" -> okhttp3.Request.Builder()
                .url("https://api.anthropic.com/v1/messages")
                .header("x-api-key", key)
                .header("anthropic-version", "2023-06-01")
                .post(okhttp3.RequestBody.create(null, ByteArray(0)))
                .build()
            else -> return@withContext false
        }
        
        val response = client.newCall(request).execute()
        
        if (provider == "claude") {
            response.code != 401 && response.code != 403
        } else {
            response.isSuccessful
        }
    } catch (e: Exception) {
        false
    }
}



@Composable
fun StorageSettingsSection(isArabic: Boolean, onClearChatHistory: () -> Unit) {
    val context = LocalContext.current
    var showClearDialog by remember { mutableStateOf(false) }

    SettingsSectionCard(if (isArabic) "سجل المحادثات والبيانات" else "Conversation Data & Storage", Icons.Rounded.Storage) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(if (isArabic) "سجل المحادثات" else "Conversation Data", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Text(if (isArabic) "حذف كل المحادثات المحفوظة محلياً بشكل نهائي." else "Permanently wipe all offline conversation records.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Button(
                    onClick = { showClearDialog = true },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer),
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Text(if (isArabic) "حذف المحادثات" else "Wipe Chats")
                }
            }
        }

        if (showClearDialog) {
            AlertDialog(
                onDismissRequest = { showClearDialog = false },
                title = { Text(if (isArabic) "تأكيد مسح كافة المحادثات" else "Wipe All Chats?", fontWeight = FontWeight.Bold) },
                text = { Text(if (isArabic) "هل أنت متأكد تماماً أنك تريد حذف كافة سجلات المحادثات المخزنة محلياً؟ لا يمكن التراجع عن هذا الإجراء." else "Are you absolutely certain you want to purge all local conversation database logs? This action is permanent and cannot be undone.") },
                confirmButton = {
                    Button(
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        onClick = {
                            showClearDialog = false
                            onClearChatHistory()
                            Toast.makeText(context, if (isArabic) "تم حذف كافة المحادثات" else "All conversations cleared", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Text(if (isArabic) "حذف الكل" else "Confirm Wipe")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearDialog = false }) {
                        Text(if (isArabic) "إلغاء" else "Cancel")
                    }
                }
            )
        }
    }
}
