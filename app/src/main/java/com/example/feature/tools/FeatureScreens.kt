package com.example.feature.tools

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenericFeatureScreen(
    title: String,
    onNavigateBack: () -> Unit,
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        content(innerPadding)
    }
}

@Composable
fun SavedChatsScreen(onNavigateBack: () -> Unit, isArabic: Boolean) {
    GenericFeatureScreen(
        title = if (isArabic) "المحادثات المحفوظة" else "Saved Chats",
        onNavigateBack = onNavigateBack
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(it), contentAlignment = Alignment.Center) {
            Text(if (isArabic) "لا توجد محادثات محفوظة حتى الآن." else "No saved chats yet.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun FilesScreen(onNavigateBack: () -> Unit, isArabic: Boolean) {
    GenericFeatureScreen(
        title = if (isArabic) "الملفات والمستندات" else "Files & Documents",
        onNavigateBack = onNavigateBack
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(it), contentAlignment = Alignment.Center) {
            Text(if (isArabic) "لا توجد ملفات مرفوعة." else "No uploaded files.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun PrivacyScreen(onNavigateBack: () -> Unit, isArabic: Boolean) {
    GenericFeatureScreen(
        title = if (isArabic) "الخصوصية" else "Privacy",
        onNavigateBack = onNavigateBack
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(it), contentAlignment = Alignment.Center) {
            Text(if (isArabic) "جميع البيانات يتم معالجتها محلياً أو من خلال المفاتيح الخاصة بك." else "All data is processed locally or via your private keys.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(16.dp))
        }
    }
}

@Composable
fun HelpScreen(onNavigateBack: () -> Unit, isArabic: Boolean) {
    GenericFeatureScreen(
        title = if (isArabic) "المساعدة والتعليقات" else "Help & Feedback",
        onNavigateBack = onNavigateBack
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(it), contentAlignment = Alignment.Center) {
            Text(if (isArabic) "تواصل معنا للحصول على المساعدة." else "Contact us for support.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
