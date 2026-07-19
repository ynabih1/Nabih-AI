package com.example.feature.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.feature.chat.ChatViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back")
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
fun FilesScreen(
    chatViewModel: ChatViewModel,
    onNavigateBack: () -> Unit,
    isArabic: Boolean
) {
    val attachments by chatViewModel.attachments.collectAsStateWithLifecycle()

    GenericFeatureScreen(
        title = if (isArabic) "الملفات والمستندات" else "Files & Documents",
        onNavigateBack = onNavigateBack
    ) { paddingValues ->
        if (attachments.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.FolderOpen,
                        contentDescription = null,
                        modifier = Modifier.size(72.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (isArabic) "لا توجد ملفات مرفوعة" else "No uploaded files",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isArabic) "الملفات والصور التي تشاركها في الدردشة ستظهر هنا." else "Files and images you share in chats will appear here.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(attachments, key = { it.messageId }) { item ->
                    val isImage = item.imageUri != null && item.imageUri!!.isNotEmpty()
                    val uriString = item.imageUri ?: item.documentUri ?: ""
                    val fileName = item.documentName ?: (if (isImage) {
                        if (isArabic) "صورة مرفقة" else "Image attachment"
                    } else {
                        item.documentUri?.substringAfterLast('/') ?: (if (isArabic) "ملف" else "File")
                    })
                    
                    val fileType = getFileType(fileName, isImage)
                    val fileSize = chatViewModel.getFileSizeString(uriString)
                    val uploadDate = formatTimestamp(item.timestamp, isArabic)

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Thumbnail / Icon Area
                            if (isImage && item.imageUri != null) {
                                AsyncImage(
                                    model = item.imageUri,
                                    contentDescription = "Thumbnail",
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surfaceContainer),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = getDocumentIcon(fileType),
                                        contentDescription = "File Icon",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // Details Area
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = fileName,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                
                                // Type & Size
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = fileType,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    if (fileSize.isNotEmpty()) {
                                        Text(
                                            text = " • $fileSize",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))

                                // Conversation Name
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.ChatBubbleOutline,
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                    Text(
                                        text = item.conversationTitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(2.dp))
                                
                                // Date
                                Text(
                                    text = uploadDate,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Actions
                            IconButton(
                                onClick = { chatViewModel.deleteMessage(item.messageId) }
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.DeleteOutline,
                                    contentDescription = "Delete attachment",
                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun getFileType(fileName: String?, isImage: Boolean): String {
    if (isImage) return "Image"
    if (fileName == null) return "Document"
    val ext = fileName.substringAfterLast('.', "").uppercase()
    return if (ext.isNotEmpty()) ext else "Document"
}

private fun getDocumentIcon(fileType: String): androidx.compose.ui.graphics.vector.ImageVector {
    return when (fileType) {
        "PDF" -> Icons.Rounded.PictureAsPdf
        "ZIP", "RAR" -> Icons.Rounded.FolderZip
        "AUDIO", "MP3", "WAV", "M4A" -> Icons.Rounded.AudioFile
        "TXT", "DOC", "DOCX" -> Icons.Rounded.Description
        else -> Icons.Rounded.InsertDriveFile
    }
}

private fun formatTimestamp(timestamp: Long, isArabic: Boolean): String {
    val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", if (isArabic) Locale("ar") else Locale.US)
    return sdf.format(Date(timestamp))
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
