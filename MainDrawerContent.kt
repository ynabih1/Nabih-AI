import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.database.Conversation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainDrawerContent(
    settings: com.example.core.settings.AppSettings,
    selectedModel: com.example.core.model.AiModel,
    activeConversationId: String?,
    onSelectModel: (com.example.core.model.AiModel) -> Unit,
    conversations: List<Conversation>,
    onSelectConversation: (String) -> Unit,
    onNewChat: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateTo: (String) -> Unit,
    onCloseDrawer: () -> Unit,
    onRenameConversation: (String, String) -> Unit,
    onDeleteConversation: (String) -> Unit,
    onTogglePinConversation: (Conversation) -> Unit,
    onToggleArchiveConversation: (Conversation) -> Unit
) {
    val isArabic = settings.language == com.example.core.settings.AppLanguage.ARABIC
    val context = androidx.compose.ui.platform.LocalContext.current
    
    var conversationToRename by remember { mutableStateOf<Conversation?>(null) }
    var renameNewTitle by remember { mutableStateOf("") }
    var conversationToDelete by remember { mutableStateOf<Conversation?>(null) }

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

    if (conversationToDelete != null) {
        AlertDialog(
            onDismissRequest = { conversationToDelete = null },
            title = { Text(if (isArabic) "حذف المحادثة" else "Delete Conversation") },
            text = { Text(if (isArabic) "هل أنت متأكد أنك تريد حذف هذه المحادثة؟" else "Are you sure you want to delete this conversation?") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteConversation(conversationToDelete!!.id)
                        conversationToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(if (isArabic) "حذف" else "Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { conversationToDelete = null }) {
                    Text(if (isArabic) "إلغاء" else "Cancel")
                }
            }
        )
    }

    ModalDrawerSheet(
        drawerContainerColor = MaterialTheme.colorScheme.background,
        drawerContentColor = MaterialTheme.colorScheme.onSurface,
        drawerShape = RoundedCornerShape(0.dp),
        modifier = Modifier.width(320.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(vertical = 12.dp)
        ) {
            // Header: Nabih AI Logo
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Nabih AI",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1967D2) // Match the logo blue color
                )
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    painter = painterResource(id = R.drawable.logo),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(36.dp)
                )
            }

            LazyColumn(modifier = Modifier.weight(1f)) {
                // Top Actions
                item {
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(24.dp)) },
                        label = { Text(if (isArabic) "محادثة جديدة" else "New Chat", style = MaterialTheme.typography.bodyLarge) },
                        selected = false,
                        onClick = { onNewChat(); onCloseDrawer() },
                        colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent, unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant, unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
                    )
                }
                item {
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(24.dp)) },
                        label = { Text(if (isArabic) "البحث" else "Search", style = MaterialTheme.typography.bodyLarge) },
                        selected = false,
                        onClick = { onNavigateTo("search"); onCloseDrawer() },
                        colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent, unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant, unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
                    )
                }
                item {
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Rounded.FolderOpen, contentDescription = null, modifier = Modifier.size(24.dp)) },
                        label = { Text(if (isArabic) "الملفات" else "Files", style = MaterialTheme.typography.bodyLarge) },
                        selected = false,
                        onClick = { onNavigateTo("files"); onCloseDrawer() },
                        colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent, unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant, unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
                    )
                }

                item {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 12.dp))
                    Text(
                        text = if (isArabic) "المحادثات" else "Conversations",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 24.dp).padding(bottom = 8.dp)
                    )
                }

                // Conversations List
                if (conversations.isEmpty()) {
                    item {
                        Text(
                            text = if (isArabic) "لا توجد محادثات مؤخراً" else "No recent conversations",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                        )
                    }
                } else {
                    conversations.take(5).forEach { conversation ->
                        item(key = conversation.id) {
                            NavigationDrawerItem(
                                icon = { Icon(Icons.Rounded.ChatBubbleOutline, contentDescription = null, modifier = Modifier.size(20.dp)) },
                                label = { Text(conversation.title, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) },
                                selected = conversation.id == activeConversationId,
                                onClick = { onSelectConversation(conversation.id); onCloseDrawer() },
                                colors = NavigationDrawerItemDefaults.colors(
                                    unselectedContainerColor = Color.Transparent,
                                    selectedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ),
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 1.dp)
                            )
                        }
                    }
                }

                item {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 12.dp))
                    Text(
                        text = if (isArabic) "نموذج الذكاء الاصطناعي" else "AI Model",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 24.dp).padding(bottom = 8.dp)
                    )
                }

                // AI Models List
                val models = listOf(
                    Triple(com.example.core.model.AiModel.NABIH_ULTRA, true, "Nabih Ultra"),
                    Triple(com.example.core.model.AiModel.CHATGPT, settings.openaiApiKey.isNotEmpty(), "ChatGPT"),
                    Triple(com.example.core.model.AiModel.CLAUDE, settings.anthropicApiKey.isNotEmpty(), "Claude"),
                    Triple(com.example.core.model.AiModel.GEMINI, settings.googleApiKey.isNotEmpty(), "Gemini")
                )

                models.forEach { (model, isUnlocked, displayName) ->
                    val isSelected = selectedModel.id == model.id
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) Color(0xFFE9D5FF).copy(alpha = 0.5f) else Color.Transparent)
                                .clickable(enabled = isUnlocked) {
                                    onSelectModel(model)
                                    onCloseDrawer()
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = displayName,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isSelected) Color.Black else if (isUnlocked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                modifier = Modifier.weight(1f)
                            )
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircleOutline,
                                    contentDescription = null,
                                    tint = Color(0xFF1967D2), // Blue tick like image
                                    modifier = Modifier.size(20.dp)
                                )
                            } else if (!isUnlocked) {
                                Icon(
                                    imageVector = Icons.Rounded.Lock,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 8.dp))

            // Footer
            NavigationDrawerItem(
                icon = { Icon(Icons.Rounded.Settings, contentDescription = null, modifier = Modifier.size(24.dp)) },
                label = { Text(if (isArabic) "الإعدادات" else "Settings", style = MaterialTheme.typography.bodyLarge) },
                selected = false,
                onClick = { onNavigateToSettings(); onCloseDrawer() },
                colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent, unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant, unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
            )
            
            NavigationDrawerItem(
                icon = { Icon(Icons.Rounded.AccountCircle, contentDescription = null, modifier = Modifier.size(24.dp)) },
                label = { Text(if (isArabic) "الحساب" else "Account", style = MaterialTheme.typography.bodyLarge) },
                selected = false,
                onClick = { /* Handle Account Click */ },
                colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent, unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant, unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
            )
        }
    }
}
