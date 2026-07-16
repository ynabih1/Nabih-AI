package com.example.feature.auth

import com.example.feature.settings.SettingsViewModel
import com.example.feature.tools.GenericFeatureScreen

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(
    settingsViewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    onLogout: () -> Unit,
    onDeleteAccount: () -> Unit,
    isArabic: Boolean
) {
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    
    // Mock but interactive active sessions list
    var activeSessions by remember {
        mutableStateOf(
            listOf(
                SessionItem("This Device", "Android App (Active Now)", "10.0.2.2 - Linux", true),
                SessionItem("Web App", "Chrome on macOS", "192.168.1.15 - 2 hours ago", false),
                SessionItem("Smart Companion Screen", "Nabih TV Hub", "192.168.1.100 - Yesterday", false)
            )
        )
    }

    // Connected accounts based on actual login state
    var connectedAccounts by remember(settings.isLoggedIn, settings.authType, settings.userEmail) {
        mutableStateOf(
            listOf(
                ConnectedAccount("Microsoft", "microsoft_icon", settings.isLoggedIn && settings.authType == "MICROSOFT", if (settings.authType == "MICROSOFT") settings.userEmail else ""),
                ConnectedAccount("Passkey / Biometrics", "passkey_icon", settings.isLoggedIn, if (settings.isLoggedIn) "Secured" else "")
            )
        )
    }

    GenericFeatureScreen(
        title = if (isArabic) "الحساب والملف الشخصي" else "Account & Profile",
        onNavigateBack = onNavigateBack
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            if (settings.isLoggedIn) {
                // Profile Header Card with elegant gradient
                val gradientBrush = Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.colorScheme.surfaceVariant
                    )
                )

                // Image Picker Launcher
                val imagePickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                    contract = androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
                ) { uri: android.net.Uri? ->
                    uri?.let {
                        try {
                            context.contentResolver.takePersistableUriPermission(
                                it,
                                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        settingsViewModel.updateProfile(
                            name = settings.userName,
                            pictureUri = it.toString(),
                            info = settings.personalInfo,
                            newEmail = settings.userEmail
                        )
                        Toast.makeText(context, if (isArabic) "تم تحديث الصورة الشخصية" else "Profile picture updated", Toast.LENGTH_SHORT).show()
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(gradientBrush)
                            .padding(24.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Avatar container with click handler and edit pencil overlay
                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                                    .clickable { imagePickerLauncher.launch(arrayOf("image/*")) },
                                contentAlignment = Alignment.Center
                            ) {
                                if (settings.profilePictureUri.startsWith("content://") || settings.profilePictureUri.startsWith("http")) {
                                    coil.compose.AsyncImage(
                                        model = settings.profilePictureUri,
                                        contentDescription = "Profile Picture",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                    )
                                } else if (settings.profilePictureUri.startsWith("emoji:")) {
                                    Text(
                                        text = settings.profilePictureUri.removePrefix("emoji:"),
                                        fontSize = 48.sp
                                    )
                                } else {
                                    Text(
                                        text = if (settings.userName.isNotEmpty()) settings.userName.take(1).uppercase() 
                                               else if (settings.userEmail.isNotEmpty()) settings.userEmail.take(1).uppercase() 
                                               else "U",
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        style = MaterialTheme.typography.headlineLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                
                                // Pencil edit overlay
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer)
                                        .border(2.dp, MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Edit,
                                        contentDescription = "Edit Picture",
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = settings.userName.ifEmpty { settings.userEmail.substringBefore("@") },
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (settings.userEmail.isNotEmpty()) {
                                Text(
                                    text = settings.userEmail,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer,
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        Icons.Rounded.CheckCircle, 
                                        contentDescription = null, 
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = if (isArabic) "حساب نشط ومحمي" else "Active & Secure Account",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        }
                    }
                }

                // Section: Personal Info
                Text(
                    text = if (isArabic) "المعلومات الشخصية" else "Personal Details",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        ListItem(
                            headlineContent = { Text(if (isArabic) "الاسم بالكامل" else "Full Name", fontWeight = FontWeight.SemiBold) },
                            supportingContent = { Text(settings.userName.ifEmpty { settings.userEmail.substringBefore("@") }) },
                            leadingContent = { Icon(Icons.Rounded.Person, contentDescription = null, tint = MaterialTheme.colorScheme.secondary) }
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                        ListItem(
                            headlineContent = { Text(if (isArabic) "البريد الإلكتروني" else "Email Address", fontWeight = FontWeight.SemiBold) },
                            supportingContent = { Text(settings.userEmail) },
                            leadingContent = { Icon(Icons.Rounded.Email, contentDescription = null, tint = MaterialTheme.colorScheme.secondary) }
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                        ListItem(
                            headlineContent = { Text(if (isArabic) "نبذة شخصية" else "Bio / Personal Info", fontWeight = FontWeight.SemiBold) },
                            supportingContent = { Text(settings.personalInfo.ifBlank { if (isArabic) "لا توجد نبذة" else "No bio written yet" }) },
                            leadingContent = { Icon(Icons.Rounded.Info, contentDescription = null, tint = MaterialTheme.colorScheme.secondary) }
                        )
                    }
                }

                // Section: Security & Credentials
                Text(
                    text = if (isArabic) "الأمان وكلمة المرور" else "Security & Credentials",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ListItem(
                            headlineContent = { Text(if (isArabic) "تعديل الملف الشخصي" else "Edit Account Info", fontWeight = FontWeight.SemiBold) },
                            supportingContent = { Text(if (isArabic) "تغيير اسم العرض والبريد الإلكتروني والنبذة" else "Modify display name, email, and bio") },
                            leadingContent = { Icon(Icons.Rounded.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                            trailingContent = {
                                IconButton(onClick = { showEditProfileDialog = true }) {
                                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "Edit")
                                }
                            }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        ListItem(
                            headlineContent = { Text(if (isArabic) "تغيير كلمة المرور" else "Change Password", fontWeight = FontWeight.SemiBold) },
                            supportingContent = { Text(if (isArabic) "تحديث كلمة المرور الخاصة بحسابك" else "Set a new password for account login") },
                            leadingContent = { Icon(Icons.Rounded.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                            trailingContent = {
                                IconButton(onClick = { showChangePasswordDialog = true }) {
                                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "Change")
                                }
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Log Out and Delete Buttons in premium card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = onLogout,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.ExitToApp, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isArabic) "تسجيل الخروج" else "Sign Out", fontWeight = FontWeight.Bold)
                        }

                        TextButton(
                            onClick = { showDeleteDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Rounded.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isArabic) "حذف الحساب نهائياً" else "Delete Account Permanently", fontWeight = FontWeight.Bold)
                        }
                    }
                }

            } else {
                // Empty state if not logged in
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                        Icon(
                            Icons.Rounded.Person,
                            contentDescription = null,
                            modifier = Modifier.size(80.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (isArabic) "لم تقم بتسجيل الدخول" else "Not signed in",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isArabic) "سجل دخولك لتتمكن من تخصيص حسابك وإدارة جلساتك النشطة ومفاتيح الموديلات." else "Sign in to customize your profile, active sessions, and API configurations.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { 
                                Toast.makeText(context, if (isArabic) "الرجاء تسجيل الدخول من الشاشة الرئيسية" else "Please sign in from the login screen", Toast.LENGTH_SHORT).show() 
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(if (isArabic) "صفحة تسجيل الدخول" else "Go to Sign In")
                        }
                    }
                }
            }
        }

        // Dialog: Delete Account
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text(if (isArabic) "تأكيد حذف الحساب" else "Confirm Account Deletion", fontWeight = FontWeight.Bold) },
                text = { Text(if (isArabic) "هل أنت متأكد تماماً من رغبتك في حذف حسابك نهائياً؟ هذا الإجراء لا يمكن التراجع عنه وسيتم مسح كافة سجلات المحادثات والملفات والبيانات الخاصة بك فوراً." else "Are you absolutely sure you want to delete your account permanently? This action is completely irreversible and will immediately purge all your chat history, files, and stored settings.") },
                confirmButton = {
                    Button(
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        onClick = {
                            showDeleteDialog = false
                            onDeleteAccount()
                        }
                    ) {
                        Text(if (isArabic) "نعم، احذف حسابي" else "Yes, Delete Account")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text(if (isArabic) "إلغاء" else "Cancel")
                    }
                }
            )
        }

        // Dialog: Edit Profile
        if (showEditProfileDialog) {
            var nameField by remember { mutableStateOf(settings.userName.ifEmpty { settings.userEmail.substringBefore("@") }) }
            var bioField by remember { mutableStateOf(settings.personalInfo) }
            var emailField by remember { mutableStateOf(settings.userEmail) }

            AlertDialog(
                onDismissRequest = { showEditProfileDialog = false },
                title = {
                    Text(
                        text = if (isArabic) "تعديل الملف الشخصي" else "Edit Profile Details",
                        fontWeight = FontWeight.ExtraBold
                    )
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = nameField,
                            onValueChange = { nameField = it },
                            label = { Text(if (isArabic) "الاسم بالكامل" else "Full Name") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        OutlinedTextField(
                            value = emailField,
                            onValueChange = { emailField = it },
                            label = { Text(if (isArabic) "البريد الإلكتروني" else "Email Address") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = bioField,
                            onValueChange = { bioField = it },
                            label = { Text(if (isArabic) "نبذة شخصية" else "Personal Bio") },
                            maxLines = 3,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        shape = RoundedCornerShape(12.dp),
                        onClick = {
                            if (nameField.isNotBlank() && emailField.isNotBlank()) {
                                settingsViewModel.updateProfile(nameField, settings.profilePictureUri, bioField, emailField)
                                showEditProfileDialog = false
                                Toast.makeText(context, if (isArabic) "تم حفظ التغييرات" else "Profile updated successfully", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, if (isArabic) "يرجى تعبئة الحقول المطلوبة" else "Please fill required fields", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Text(if (isArabic) "حفظ التعديلات" else "Save Changes")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEditProfileDialog = false }) {
                        Text(if (isArabic) "إلغاء" else "Cancel")
                    }
                }
            )
        }

        // Dialog: Change Password
        if (showChangePasswordDialog) {
            var newPassword by remember { mutableStateOf("") }
            var confirmPassword by remember { mutableStateOf("") }
            var passwordVisible by remember { mutableStateOf(false) }

            AlertDialog(
                onDismissRequest = { showChangePasswordDialog = false },
                title = {
                    Text(
                        text = if (isArabic) "تغيير كلمة المرور" else "Change Account Password",
                        fontWeight = FontWeight.ExtraBold
                    )
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (isArabic) "أدخل كلمة المرور الجديدة وقم بتأكيدها." else "Enter your new desired secure password below.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        OutlinedTextField(
                            value = newPassword,
                            onValueChange = { newPassword = it },
                            label = { Text(if (isArabic) "كلمة المرور الجديدة" else "New Password") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                                        contentDescription = "Toggle Visibility"
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it },
                            label = { Text(if (isArabic) "تأكيد كلمة المرور" else "Confirm Password") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        shape = RoundedCornerShape(12.dp),
                        onClick = {
                            if (newPassword.isBlank()) {
                                Toast.makeText(context, if (isArabic) "يرجى كتابة كلمة المرور" else "Password cannot be blank", Toast.LENGTH_SHORT).show()
                            } else if (newPassword != confirmPassword) {
                                Toast.makeText(context, if (isArabic) "كلمات المرور غير متطابقة" else "Passwords do not match", Toast.LENGTH_SHORT).show()
                            } else {
                                coroutineScope.launch {
                                    val success = settingsViewModel.updatePassword(newPassword)
                                    if (success) {
                                        Toast.makeText(context, if (isArabic) "تم تغيير كلمة المرور بنجاح" else "Password updated successfully", Toast.LENGTH_SHORT).show()
                                        showChangePasswordDialog = false
                                    } else {
                                        Toast.makeText(context, if (isArabic) "حدث خطأ ما، يرجى المحاولة لاحقاً" else "Error updating password", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }
                    ) {
                        Text(if (isArabic) "تحديث كلمة المرور" else "Update Password")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showChangePasswordDialog = false }) {
                        Text(if (isArabic) "إلغاء" else "Cancel")
                    }
                }
            )
        }
    }
}

data class SessionItem(
    val device: String,
    val client: String,
    val ipTime: String,
    val isCurrent: Boolean
)

data class ConnectedAccount(
    val name: String,
    val iconKey: String,
    val isConnected: Boolean,
    val details: String
)
