package com.example.feature.auth

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
import com.example.feature.settings.SettingsViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

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

    var fullName by remember(settings?.userName) { mutableStateOf(settings?.userName ?: "") }
    var bio by remember(settings?.personalInfo) { mutableStateOf(settings?.personalInfo ?: "") }
    var profilePictureUri by remember(settings?.profilePictureUri) { mutableStateOf(settings?.profilePictureUri ?: "") }

    var isFullNameError by remember { mutableStateOf(false) }
    
    var isSaving by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }

    var showDeleteDialog by remember { mutableStateOf(false) }
    var showPhotoOptions by remember { mutableStateOf(false) }

    val profileChanged = fullName != (settings?.userName ?: "") ||
                         bio != (settings?.personalInfo ?: "") ||
                         profilePictureUri != (settings?.profilePictureUri ?: "")

    val photoUri = remember {
        try {
            val file = File.createTempFile("profile_", ".jpg", context.cacheDir).apply { createNewFile() }
            FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        } catch (e: Exception) {
            android.util.Log.e("AccountScreen", "Failed to create temp photo file", e)
            Uri.EMPTY
        }
    }

    val takePhotoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && photoUri != Uri.EMPTY) {
            profilePictureUri = photoUri.toString()
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            profilePictureUri = uri.toString()
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
                    headlineContent = { Text(if (isArabic) "التقاط صورة" else "Take a photo") },
                    modifier = Modifier.clickable {
                        showPhotoOptions = false
                        if (photoUri != Uri.EMPTY) {
                            try {
                                takePhotoLauncher.launch(photoUri)
                            } catch (e: Exception) {
                                Toast.makeText(context, if (isArabic) "تعذر تشغيل الكاميرا" else "Failed to launch camera", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(context, if (isArabic) "تعذر حفظ الصورة المؤقتة" else "Failed to create temp photo file", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
                ListItem(
                    headlineContent = { Text(if (isArabic) "اختيار من المعرض" else "Choose from gallery") },
                    modifier = Modifier.clickable {
                        showPhotoOptions = false
                        try {
                            galleryLauncher.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        } catch (e: Exception) {
                            Toast.makeText(context, if (isArabic) "تعذر فتح معرض الصور" else "Failed to open gallery", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
                if (profilePictureUri.isNotEmpty()) {
                    ListItem(
                        headlineContent = { Text(if (isArabic) "إزالة الصورة" else "Remove picture", color = MaterialTheme.colorScheme.error) },
                        modifier = Modifier.clickable {
                            showPhotoOptions = false
                            profilePictureUri = ""
                        }
                    )
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { if (!isDeleting) showDeleteDialog = false },
            title = { Text(if (isArabic) "حذف الحساب" else "Delete Account") },
            text = { Text(if (isArabic) "هذا الإجراء دائم ولا يمكن التراجع عنه. هل أنت متأكد أنك تريد حذف حسابك؟" else "This action is permanent and cannot be undone. Are you sure you want to delete your account?") },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            isDeleting = true
                            delay(1000) // Simulating network request
                            onDeleteAccount()
                            isDeleting = false
                            showDeleteDialog = false
                            Toast.makeText(context, if (isArabic) "تم حذف الحساب بنجاح" else "Account deleted successfully", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    if (isDeleting) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onError, strokeWidth = 2.dp)
                    } else {
                        Text(if (isArabic) "حذف" else "Delete")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDialog = false },
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
            
            // Avatar Section
            Box(contentAlignment = Alignment.BottomEnd) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainer),
                    contentAlignment = Alignment.Center
                ) {
                    if (profilePictureUri.isNotEmpty()) {
                        AsyncImage(
                            model = profilePictureUri,
                            contentDescription = "Profile Picture",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        val initials = fullName.split(" ").filter { it.isNotEmpty() }.take(2).joinToString("") { it.take(1) }.uppercase()
                        if (initials.isNotEmpty()) {
                            Text(
                                text = initials,
                                style = MaterialTheme.typography.displayMedium,
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
                }
                
                IconButton(
                    onClick = { showPhotoOptions = true },
                    modifier = Modifier
                        .size(36.dp)
                        .offset(x = (-4).dp, y = (-4).dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                        .border(2.dp, MaterialTheme.colorScheme.background, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CameraAlt,
                        contentDescription = "Change Picture",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // User Info Section
            Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
                
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it; isFullNameError = false },
                    label = { Text(if (isArabic) "الاسم الكامل" else "Full Name") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = isFullNameError
                )

                OutlinedTextField(
                    value = settings?.userEmail ?: "",
                    onValueChange = {},
                    label = { Text(if (isArabic) "البريد الإلكتروني" else "Email Address") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    readOnly = true,
                    enabled = false,
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                
                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    label = { Text(if (isArabic) "نبذة (اختياري)" else "Bio (Optional)") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action Buttons
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
                    modifier = Modifier.fillMaxWidth().height(50.dp)
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
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                    ) {
                        Text(if (isArabic) "إلغاء" else "Cancel", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Delete Account Button
            OutlinedButton(
                onClick = { showDeleteDialog = true },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Icon(Icons.Outlined.Delete, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isArabic) "حذف الحساب" else "Delete Account", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
