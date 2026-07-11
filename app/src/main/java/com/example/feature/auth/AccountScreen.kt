package com.example.feature.auth

import com.example.feature.settings.SettingsViewModel
import com.example.feature.tools.GenericFeatureScreen

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.ExitToApp
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

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
    var showDeleteDialog by remember { mutableStateOf(false) }

    GenericFeatureScreen(
        title = if (isArabic) "الحساب" else "Account",
        onNavigateBack = onNavigateBack
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (settings.isLoggedIn) {
                // Profile Header Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (settings.userName.isNotEmpty()) settings.userName.take(1).uppercase() 
                                       else if (settings.userEmail.isNotEmpty()) settings.userEmail.take(1).uppercase() 
                                       else "U",
                                color = MaterialTheme.colorScheme.onPrimary,
                                style = MaterialTheme.typography.displaySmall
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = settings.userName.ifEmpty { settings.userEmail.substringBefore("@") },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        if (settings.userEmail.isNotEmpty()) {
                            Text(
                                text = settings.userEmail,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Text(
                                text = if (isArabic) "حساب نشط" else "Active Account",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                // Account Information
                Text(
                    text = if (isArabic) "المعلومات الشخصية" else "Personal Information",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                )
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column {
                        ListItem(
                            headlineContent = { Text(if (isArabic) "الاسم" else "Name") },
                            supportingContent = { Text(settings.userName.ifEmpty { settings.userEmail.substringBefore("@") }) },
                            leadingContent = { Icon(Icons.Outlined.Person, contentDescription = null) }
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        ListItem(
                            headlineContent = { Text(if (isArabic) "البريد الإلكتروني" else "Email") },
                            supportingContent = { Text(settings.userEmail) },
                            leadingContent = { Icon(Icons.Outlined.Email, contentDescription = null) }
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        ListItem(
                            headlineContent = { Text(if (isArabic) "طريقة التسجيل" else "Authentication Method") },
                            supportingContent = { Text(settings.authType.ifEmpty { "EMAIL" }) },
                            leadingContent = { Icon(Icons.Outlined.Info, contentDescription = null) }
                        )
                    }
                }

                if (settings.microsoftEmail.isNotEmpty()) {
                    Text(
                        text = if (isArabic) "الجلسات المحفوظة" else "Saved Provider Sessions",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column {
                            ListItem(
                                headlineContent = { Text("Microsoft Account") },
                                supportingContent = { Text(settings.microsoftEmail) },
                                leadingContent = { Icon(Icons.Outlined.Person, contentDescription = null, tint = MaterialTheme.colorScheme.secondary) },
                                trailingContent = {
                                    if (settings.authType == "MICROSOFT") {
                                        Icon(Icons.Filled.CheckCircle, "Active", tint = MaterialTheme.colorScheme.primary)
                                    } else {
                                        TextButton(onClick = { settingsViewModel.switchActiveAccount("MICROSOFT") }) {
                                            Text(if (isArabic) "تبديل" else "Switch")
                                        }
                                    }
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Actions
                OutlinedButton(
                    onClick = { 
                        Toast.makeText(context, if (isArabic) "تعديل الملف الشخصي قريباً" else "Edit Profile coming soon", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Outlined.Person, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isArabic) "تعديل الملف الشخصي" else "Edit Profile")
                }
                
                OutlinedButton(
                    onClick = onLogout,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Outlined.ExitToApp, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isArabic) "تسجيل الخروج" else "Sign Out")
                }

                TextButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Outlined.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isArabic) "حذف الحساب نهائياً" else "Delete Account Permanently")
                }

            } else {
                // Empty state if not logged in
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Outlined.Person,
                            contentDescription = null,
                            modifier = Modifier.size(72.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (isArabic) "لم تقم بتسجيل الدخول" else "Not signed in",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { 
                            Toast.makeText(context, if (isArabic) "الرجاء تسجيل الدخول من الشاشة الرئيسية" else "Please sign in from the main screen", Toast.LENGTH_SHORT).show() 
                        }) {
                            Text(if (isArabic) "تسجيل الدخول" else "Sign In")
                        }
                    }
                }
            }
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
