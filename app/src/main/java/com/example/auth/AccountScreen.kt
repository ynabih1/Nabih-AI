package com.example.auth

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.settings.SettingsViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.draw.shadow

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
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    var isInitialized by remember { mutableStateOf(false) }
    var fullName by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var profilePictureUri by remember { mutableStateOf("") }

    LaunchedEffect(settings) {
        if (settings != null && !isInitialized) {
            fullName = settings?.userName ?: ""
            bio = settings?.personalInfo ?: ""
            profilePictureUri = settings?.profilePictureUri ?: ""
            isInitialized = true
        }
    }

    var isFullNameError by remember { mutableStateOf(false) }
    
    var isSaving by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }
    var isUploadingImage by remember { mutableStateOf(false) }

    var showDeleteDialog by remember { mutableStateOf(false) }
    var deleteConfirmationText by remember { mutableStateOf("") }
    var deleteErrorText by remember { mutableStateOf<String?>(null) }
    var showPhotoOptions by remember { mutableStateOf(false) }

    val profileChanged = fullName != (settings?.userName ?: "") ||
                         bio != (settings?.personalInfo ?: "") ||
                         profilePictureUri != (settings?.profilePictureUri ?: "")

    val isFileExist = remember(profilePictureUri) {
        if (profilePictureUri.isEmpty()) {
            false
        } else {
            try {
                val uri = Uri.parse(profilePictureUri)
                if (uri.scheme == "file") {
                    uri.path?.let { File(it).exists() } ?: false
                } else if (profilePictureUri.startsWith("/")) {
                    File(profilePictureUri).exists()
                } else {
                    true
                }
            } catch (e: Exception) {
                false
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                isUploadingImage = true
                delay(800) // Aesthetic delay for progress feedback
                val localUri = copyUriToInternalStorage(context, uri)
                if (localUri != null) {
                    profilePictureUri = localUri.toString()
                } else {
                    Toast.makeText(context, if (isArabic) "فشل حفظ الصورة" else "Failed to save image", Toast.LENGTH_SHORT).show()
                }
                isUploadingImage = false
            }
        }
    }

    if (showPhotoOptions) {
        ModalBottomSheet(onDismissRequest = { showPhotoOptions = false }) {
            Column(modifier = Modifier.padding(bottom = 32.dp)) {
                Text(
                    text = if (isArabic) "صورة الملف الشخصي" else "Profile Picture",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(16.dp)
                )
                ListItem(
                    headlineContent = { Text(if (isArabic) "اختيار صورة جديدة" else "Choose new photo") },
                    modifier = Modifier.clickable {
                        showPhotoOptions = false
                        try {
                            galleryLauncher.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        } catch (e: Exception) {
                            Toast.makeText(context, if (isArabic) "تعذر فتح معرض الصور" else "Failed to open gallery", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
                ListItem(
                    headlineContent = { Text(if (isArabic) "إزالة الصورة" else "Remove picture", color = MaterialTheme.colorScheme.error) },
                    modifier = Modifier.clickable {
                        showPhotoOptions = false
                        coroutineScope.launch {
                            isUploadingImage = true
                            delay(300)
                            profilePictureUri = ""
                            isUploadingImage = false
                        }
                    }
                )
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { 
                if (!isDeleting) {
                    showDeleteDialog = false
                    deleteConfirmationText = ""
                    deleteErrorText = null
                }
            },
            title = { 
                Text(
                    text = if (isArabic) "حذف الحساب نهائياً" else "Delete Account Permanently",
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold
                ) 
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = if (isArabic) 
                            "تحذير: هذا الإجراء نهائي وسيتم حذف جميع بياناتك ومحادثاتك بالكامل ولا يمكن التراجع عنه بأي حال من الأحوال."
                            else "Warning: This action is permanent. All your data, chats, and settings will be deleted completely and cannot be recovered.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    Text(
                        text = if (isArabic) 
                            "لتأكيد الحذف، يرجى كتابة كلمة \"حذف\" في الحقل أدناه:" 
                            else "To confirm deletion, please type \"DELETE\" in the field below:",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                    
                    OutlinedTextField(
                        value = deleteConfirmationText,
                        onValueChange = { 
                            deleteConfirmationText = it
                            deleteErrorText = null
                        },
                        placeholder = { Text(if (isArabic) "اكتب \"حذف\"" else "Type \"DELETE\"") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        isError = deleteErrorText != null,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.error,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                    
                    if (deleteErrorText != null) {
                        Text(
                            text = deleteErrorText!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val expectedWord = if (isArabic) "حذف" else "DELETE"
                        val trimmedInput = deleteConfirmationText.trim()
                        val isConfirmed = trimmedInput.equals(expectedWord, ignoreCase = true) || 
                                          trimmedInput == "حذف" || 
                                          trimmedInput.equals("DELETE", ignoreCase = true)
                        
                        if (!isConfirmed) {
                            deleteErrorText = if (isArabic) "الكلمة غير مطابقة" else "Confirmation word does not match"
                            return@Button
                        }
                        
                        coroutineScope.launch {
                            isDeleting = true
                            try {
                                val firebaseUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                                if (firebaseUser != null) {
                                    try {
                                        firebaseUser.delete().await()
                                    } catch (e: Exception) {
                                        android.util.Log.e("AccountScreen", "Error deleting Firebase user (Needs reauth)", e)
                                    }
                                }
                                onDeleteAccount()
                                showDeleteDialog = false
                                deleteConfirmationText = ""
                                Toast.makeText(context, if (isArabic) "تم حذف الحساب بنجاح" else "Account deleted successfully", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                android.util.Log.e("AccountScreen", "Error deleting account", e)
                                deleteErrorText = "Error: ${e.message}"
                            } finally {
                                isDeleting = false
                            }
                        }
                    },
                    enabled = deleteConfirmationText.isNotBlank() && !isDeleting,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    if (isDeleting) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onError, strokeWidth = 2.dp)
                    } else {
                        Text(if (isArabic) "حذف الحساب" else "Delete Account")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        showDeleteDialog = false
                        deleteConfirmationText = ""
                        deleteErrorText = null
                    },
                    enabled = !isDeleting
                ) {
                    Text(if (isArabic) "إلغاء" else "Cancel")
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(if (isArabic) "الملف الشخصي" else "Profile", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(if (isArabic) Icons.AutoMirrored.Rounded.ArrowForward else Icons.AutoMirrored.Rounded.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            
            // 5. Spacing & layout: top margin (24dp)
            Spacer(modifier = Modifier.height(24.dp))
            
            // 1. Avatar Section
            Box(contentAlignment = Alignment.BottomEnd) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    if (profilePictureUri.isNotEmpty() && isFileExist) {
                        AsyncImage(
                            model = profilePictureUri,
                            contentDescription = "Profile Picture",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                            onError = {
                                android.util.Log.e("AccountScreen", "Coil failed to load profile picture from $profilePictureUri")
                            }
                        )
                    } else {
                        // 1.1 Default colorful avatar with initials
                        val initials = fullName.split(" ").filter { it.isNotEmpty() }.take(2).joinToString("") { it.take(1) }.uppercase()
                        if (initials.isNotEmpty()) {
                            Text(
                                text = initials,
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.Person,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    // 1.3 Upload progress state overlay
                    if (isUploadingImage) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.4f)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(36.dp),
                                color = MaterialTheme.colorScheme.surface,
                                strokeWidth = 3.dp
                            )
                        }
                    }
                }
                
                // 1.2 Camera button: larger touch target & white border (2dp)
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .offset(x = 4.dp, y = 4.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .border(2.dp, MaterialTheme.colorScheme.background, CircleShape)
                        .clickable {
                            if (profilePictureUri.isEmpty()) {
                                try {
                                    galleryLauncher.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                } catch (e: Exception) {
                                    Toast.makeText(context, if (isArabic) "تعذر فتح معرض الصور" else "Failed to open gallery", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                showPhotoOptions = true
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CameraAlt,
                        contentDescription = "Change Picture",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // 2. User Info Section
            Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
                
                // 2.1 Full Name Input with permanent label
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = if (isArabic) "الاسم الكامل" else "Full Name",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it; isFullNameError = false },
                        placeholder = { Text(if (isArabic) "أدخل اسمك الكامل" else "Enter your full name") },
                        shape = RoundedCornerShape(12.dp),
                        isError = isFullNameError,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                }

                // 2.2 Read-only Email Field with distinct visual style and support note
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = if (isArabic) "البريد الإلكتروني" else "Email Address",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                    OutlinedTextField(
                        value = settings?.userEmail ?: "",
                        onValueChange = {},
                        placeholder = { Text(if (isArabic) "البريد الإلكتروني" else "Email Address") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        readOnly = true,
                        enabled = false,
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), // Light gray background
                            disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                    )
                    Text(
                        text = if (isArabic) "لتغيير البريد الإلكتروني، يرجى التواصل مع الدعم الفني." else "To change your email address, please contact support.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                    )
                }
                
                // 2.3 Bio Input with character count (max 150)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = if (isArabic) "نبذة عنك" else "Bio",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                    OutlinedTextField(
                        value = bio,
                        onValueChange = { 
                            if (it.length <= 150) {
                                bio = it 
                            }
                        },
                        placeholder = { Text(if (isArabic) "اكتب نبذة قصيرة عنك..." else "Write a short bio about yourself...") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 5,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Text(
                            text = "${bio.length}/150",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (bio.length >= 150) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.padding(end = 4.dp, top = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 3. Action Buttons
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = { 
                        if (fullName.isBlank()) {
                            isFullNameError = true
                        }
                        if (isFullNameError) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(if (isArabic) "يرجى ملء الحقول المطلوبة" else "Please fill in required fields")
                            }
                            return@Button
                        }
                        
                        coroutineScope.launch {
                            isSaving = true
                            settingsViewModel.updateProfile(fullName, profilePictureUri, bio, settings?.userEmail ?: "", settings?.userHandle ?: "")
                            delay(500)
                            isSaving = false
                            snackbarHostState.showSnackbar(if (isArabic) "تم حفظ التغييرات بنجاح" else "Profile saved successfully")
                        }
                    },
                    enabled = profileChanged && !isSaving,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                        disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    ),
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                    } else {
                        Text(if (isArabic) "حفظ الملف الشخصي" else "Save Profile", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
                
                if (profileChanged) {
                    OutlinedButton(
                        onClick = {
                            fullName = settings?.userName ?: ""
                            bio = settings?.personalInfo ?: ""
                            profilePictureUri = settings?.profilePictureUri ?: ""
                            isFullNameError = false
                        },
                        enabled = !isSaving,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(56.dp)
                    ) {
                        Text(if (isArabic) "إلغاء" else "Cancel", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }

            // 4. Delete Account Section with vertical spacer (32dp)
            Spacer(modifier = Modifier.height(32.dp))

            OutlinedButton(
                onClick = { showDeleteDialog = true },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Icon(Icons.Outlined.Delete, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isArabic) "حذف الحساب" else "Delete Account", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

private fun copyUriToInternalStorage(context: android.content.Context, uri: android.net.Uri): android.net.Uri? {
    return try {
        val contentResolver = context.contentResolver
        val type = contentResolver.getType(uri)
        val extension = when {
            type != null && type.contains("png") -> "png"
            else -> "jpg"
        }
        val profileDir = File(context.filesDir, "profile_images")
        if (!profileDir.exists()) {
            profileDir.mkdirs()
        }
        // Delete previous profile files in the directory
        profileDir.listFiles()?.forEach { file ->
            try { file.delete() } catch (e: Exception) {}
        }
        val destFile = File(profileDir, "profile_pic_${System.currentTimeMillis()}.$extension")
        contentResolver.openInputStream(uri)?.use { inputStream ->
            java.io.FileOutputStream(destFile).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
        android.net.Uri.fromFile(destFile)
    } catch (e: Exception) {
        android.util.Log.e("AccountScreen", "Failed to copy image to internal storage", e)
        null
    }
}

