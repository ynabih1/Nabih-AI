import re

with open('app/src/main/java/com/example/ui/screen/SettingsScreen.kt', 'r') as f:
    original_code = f.read()

# Replace the whole file with a cleaner version.

new_code = """package com.example.ui.screen

import kotlinx.coroutines.launch
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import com.example.R
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.*
import com.example.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsViewModel: SettingsViewModel,
    onClearChatHistory: () -> Unit = {},
    onDeleteAccount: () -> Unit = {},
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
    val isArabic = settings.language == AppLanguage.ARABIC

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isArabic) "الإعدادات" else "Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
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
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 48.dp)
        ) {
            item {
                ApiKeysSection(settings, settingsViewModel, isArabic)
            }
            item {
                AppearanceSection(settings, settingsViewModel, isArabic)
            }
            item {
                LanguageSection(settings, settingsViewModel, isArabic)
            }
            item {
                AccountSection(settings, settingsViewModel, isArabic, onDeleteAccount)
            }
            item {
                PrivacySecuritySection(settings, isArabic, onClearChatHistory)
            }
            item {
                AboutSection(isArabic)
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(bottom = 8.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            content()
        }
    }
}

@Composable
fun ApiKeysSection(settings: AppSettings, viewModel: SettingsViewModel, isArabic: Boolean) {
    SettingsSectionCard(if (isArabic) "مفاتيح API الخاصة بك" else "AI API Keys", Icons.Outlined.Key) {
        var showKeys by remember { mutableStateOf(false) }
        var googleKey by remember { mutableStateOf(settings.googleApiKey) }
        var openaiKey by remember { mutableStateOf(settings.openaiApiKey) }
        var anthropicKey by remember { mutableStateOf(settings.anthropicApiKey) }
        
        var isValidating by remember { mutableStateOf(false) }
        var validationMessage by remember { mutableStateOf("") }
        val coroutineScope = rememberCoroutineScope()

        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showKeys = !showKeys }
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(if (isArabic) "إدارة المفاتيح" else "Manage API Keys", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        text = if (isArabic) "أضف مفاتيح لاستخدام نماذج أخرى." else "Add keys for other models.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(if (showKeys) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, null)
            }

            AnimatedVisibility(visible = showKeys) {
                Column(modifier = Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = googleKey, onValueChange = { googleKey = it }, label = { Text("Google Gemini Key") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = openaiKey, onValueChange = { openaiKey = it }, label = { Text("OpenAI Key") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = anthropicKey, onValueChange = { anthropicKey = it }, label = { Text("Anthropic Claude Key") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
                    
                    if (validationMessage.isNotEmpty()) {
                        Text(text = validationMessage, style = MaterialTheme.typography.bodySmall, color = if (validationMessage.contains("Error") || validationMessage.contains("خطأ")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                isValidating = true
                                validationMessage = if (isArabic) "جاري الحفظ..." else "Saving keys..."
                                coroutineScope.launch {
                                    kotlinx.coroutines.delay(500)
                                    viewModel.saveApiKeys(googleKey, openaiKey, anthropicKey)
                                    isValidating = false
                                    validationMessage = if (isArabic) "تم الحفظ بنجاح!" else "Keys saved successfully!"
                                    kotlinx.coroutines.delay(2000)
                                    validationMessage = ""
                                    showKeys = false
                                }
                            },
                            enabled = !isValidating,
                            modifier = Modifier.weight(1f)
                        ) {
                            if (isValidating) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text(if (isArabic) "حفظ" else "Save")
                        }
                        
                        OutlinedButton(
                            onClick = {
                                googleKey = ""
                                openaiKey = ""
                                anthropicKey = ""
                                viewModel.saveApiKeys("", "", "")
                                validationMessage = if (isArabic) "تم مسح المفاتيح" else "Keys removed"
                                coroutineScope.launch {
                                    kotlinx.coroutines.delay(2000)
                                    validationMessage = ""
                                }
                            },
                            enabled = !isValidating,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (isArabic) "مسح المفاتيح" else "Remove Keys")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AppearanceSection(settings: AppSettings, viewModel: SettingsViewModel, isArabic: Boolean) {
    SettingsSectionCard(if (isArabic) "المظهر" else "Appearance", Icons.Outlined.Palette) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            AppTheme.values().forEach { theme ->
                val label = when(theme) {
                    AppTheme.LIGHT -> if (isArabic) "فاتح" else "Light"
                    AppTheme.DARK -> if (isArabic) "داكن" else "Dark"
                    AppTheme.SYSTEM -> if (isArabic) "النظام" else "System"
                }
                FilterChip(
                    selected = settings.theme == theme,
                    onClick = { viewModel.updateTheme(theme) },
                    label = { Text(label) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun LanguageSection(settings: AppSettings, viewModel: SettingsViewModel, isArabic: Boolean) {
    SettingsSectionCard(if (isArabic) "اللغة" else "Language", Icons.Outlined.Language) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            AppLanguage.values().forEach { language ->
                FilterChip(
                    selected = settings.language == language,
                    onClick = { viewModel.updateLanguage(language) },
                    label = { Text(language.displayName) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun AccountSection(settings: AppSettings, viewModel: SettingsViewModel, isArabic: Boolean, onDeleteAccount: () -> Unit) {
    SettingsSectionCard(if (isArabic) "الحساب" else "Account", Icons.Outlined.Person) {
        var showDeleteDialog by remember { mutableStateOf(false) }
        val context = LocalContext.current

        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            if (settings.isLoggedIn) {
                ListItem(
                    headlineContent = { Text(settings.userName.ifEmpty { "User" }) },
                    supportingContent = { Text(settings.userEmail.ifEmpty { "user@example.com" }) },
                    leadingContent = {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = settings.userName.take(1).uppercase().ifEmpty { "U" },
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                )
                
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                
                ListItem(
                    headlineContent = { Text(if (isArabic) "تسجيل الخروج" else "Sign Out") },
                    modifier = Modifier.clickable { 
                        viewModel.logout()
                        Toast.makeText(context, if (isArabic) "تم تسجيل الخروج" else "Signed out", Toast.LENGTH_SHORT).show()
                    }
                )
            } else {
                ListItem(
                    headlineContent = { Text(if (isArabic) "تسجيل الدخول" else "Sign In") },
                    modifier = Modifier.clickable { 
                        // Typically handled via navigation, but for simple fix:
                        Toast.makeText(context, if (isArabic) "الرجاء إعادة تشغيل التطبيق لتسجيل الدخول" else "Please restart app to login", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            ListItem(
                headlineContent = { Text(if (isArabic) "حذف الحساب نهائياً" else "Delete Account Permanently", color = MaterialTheme.colorScheme.error) },
                modifier = Modifier.clickable { showDeleteDialog = true }
            )
        }

        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text(if (isArabic) "تأكيد الحذف" else "Confirm Deletion") },
                text = { Text(if (isArabic) "هل أنت متأكد أنك تريد حذف حسابك؟ سيؤدي هذا إلى مسح كل محادثاتك وبياناتك نهائياً." else "Are you sure you want to delete your account? This will permanently wipe all your conversations and data.") },
                confirmButton = {
                    TextButton(onClick = {
                        showDeleteDialog = false
                        onDeleteAccount()
                        Toast.makeText(context, if (isArabic) "تم حذف الحساب" else "Account deleted", Toast.LENGTH_SHORT).show()
                    }) {
                        Text(if (isArabic) "حذف" else "Delete", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text(if (isArabic) "إلغاء" else "Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun PrivacySecuritySection(settings: AppSettings, isArabic: Boolean, onClearChatHistory: () -> Unit) {
    SettingsSectionCard(if (isArabic) "الخصوصية وإدارة البيانات" else "Privacy & Data Management", Icons.Outlined.Lock) {
        val context = LocalContext.current
        var showClearDialog by remember { mutableStateOf(false) }

        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            ListItem(
                headlineContent = { Text(if (isArabic) "مسح جميع المحادثات" else "Clear All Chat History") },
                modifier = Modifier.clickable { showClearDialog = true }
            )
        }

        if (showClearDialog) {
            AlertDialog(
                onDismissRequest = { showClearDialog = false },
                title = { Text(if (isArabic) "تأكيد مسح المحادثات" else "Confirm Clear Chats") },
                text = { Text(if (isArabic) "هل أنت متأكد أنك تريد مسح جميع المحادثات المخزنة؟ لا يمكن التراجع عن هذا." else "Are you sure you want to delete all saved conversations? This cannot be undone.") },
                confirmButton = {
                    TextButton(onClick = {
                        showClearDialog = false
                        onClearChatHistory()
                        Toast.makeText(context, if (isArabic) "تم مسح المحادثات" else "Conversations cleared", Toast.LENGTH_SHORT).show()
                    }) {
                        Text(if (isArabic) "مسح" else "Clear", color = MaterialTheme.colorScheme.error)
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

@Composable
fun AboutSection(isArabic: Boolean) {
    SettingsSectionCard(if (isArabic) "حول Nabih AI" else "About Nabih AI", Icons.Outlined.Info) {
        val context = LocalContext.current
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            ListItem(
                headlineContent = { Text(if (isArabic) "إصدار التطبيق" else "App Version") },
                trailingContent = { Text("1.0.0", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            )
            
            ListItem(
                headlineContent = { Text(if (isArabic) "شروط الخدمة" else "Terms of Service") },
                modifier = Modifier.clickable { Toast.makeText(context, "Terms of Service", Toast.LENGTH_SHORT).show() },
                trailingContent = { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null) }
            )
            
            ListItem(
                headlineContent = { Text(if (isArabic) "سياسة الخصوصية" else "Privacy Policy") },
                modifier = Modifier.clickable { Toast.makeText(context, "Privacy Policy", Toast.LENGTH_SHORT).show() },
                trailingContent = { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null) }
            )
        }
    }
}
"""

with open('app/src/main/java/com/example/ui/screen/SettingsScreen.kt', 'w') as f:
    f.write(new_code)
