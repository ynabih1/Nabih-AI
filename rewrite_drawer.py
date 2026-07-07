import re

with open('app/src/main/java/com/example/ui/screen/MainScreen.kt', 'r') as f:
    content = f.read()

# Let's extract the definition of MainDrawerContent exactly.
# It starts with @Composable fun MainDrawerContent(...)
# and ends when we find another top-level function or component, or we can balance braces.

def find_composable(content, name):
    start_idx = content.find(f"fun {name}(")
    if start_idx == -1: return None, None
    
    # find the previous @Composable if it exists
    composable_idx = content.rfind("@Composable", 0, start_idx)
    if composable_idx != -1 and content[composable_idx:start_idx].strip() == "@Composable":
        start_idx = composable_idx
        
    brace_count = 0
    in_string = False
    in_char = False
    escape = False
    idx = content.find("{", start_idx)
    if idx == -1: return None, None
    
    for i in range(idx, len(content)):
        c = content[i]
        if escape:
            escape = False
            continue
        if c == '\\':
            escape = True
            continue
        if c == '"' and not in_char:
            in_string = not in_string
            continue
        if c == "'" and not in_string:
            in_char = not in_char
            continue
            
        if not in_string and not in_char:
            if c == '{':
                brace_count += 1
            elif c == '}':
                brace_count -= 1
                if brace_count == 0:
                    return start_idx, i + 1
                    
    return start_idx, -1

start_idx, end_idx = find_composable(content, "MainDrawerContent")
if start_idx is not None and end_idx != -1:
    old_drawer = content[start_idx:end_idx]
    
    # New drawer design
    new_drawer = """@Composable
fun MainDrawerContent(
    settings: AppSettings,
    selectedModel: com.example.data.model.AiModel,
    onSelectModel: (com.example.data.model.AiModel) -> Unit,
    conversations: List<com.example.data.database.Conversation>,
    onSelectConversation: (String) -> Unit,
    onNewChat: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateTo: (String) -> Unit,
    onCloseDrawer: () -> Unit
) {
    val isArabic = settings.language == AppLanguage.ARABIC
    val context = androidx.compose.ui.platform.LocalContext.current

    ModalDrawerSheet(
        drawerContainerColor = MaterialTheme.colorScheme.surface,
        drawerContentColor = MaterialTheme.colorScheme.onSurface,
        drawerShape = androidx.compose.foundation.shape.RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp),
        modifier = Modifier.width(300.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 16.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_launcher_foreground),
                    contentDescription = "Nabih AI Logo",
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Nabih AI",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Scrollable Content
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                // Main Navigation
                item {
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                        label = { Text(if (isArabic) "محادثة جديدة" else "New Chat") },
                        selected = false,
                        onClick = {
                            onNewChat()
                            onCloseDrawer()
                        },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }

                item {
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                        label = { Text(if (isArabic) "البحث" else "Search") },
                        selected = false,
                        onClick = {
                            // Can be mapped to a search route if available or just do nothing for now
                        },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }

                item {
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Outlined.Folder, contentDescription = null) },
                        label = { Text(if (isArabic) "الملفات" else "Files") },
                        selected = false,
                        onClick = {
                            onNavigateTo("files")
                            onCloseDrawer()
                        },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }

                item {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(vertical = 16.dp))
                    Text(
                        text = if (isArabic) "المحادثات" else "Chat History",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 24.dp, bottom = 8.dp)
                    )
                }

                if (conversations.isEmpty()) {
                    item {
                        Text(
                            text = if (isArabic) "لا توجد محادثات بعد" else "No conversations yet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                        )
                    }
                } else {
                    items(conversations) { conversation ->
                        NavigationDrawerItem(
                            icon = { Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = null, modifier = Modifier.size(20.dp)) },
                            label = { 
                                Text(
                                    text = conversation.title,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                ) 
                            },
                            selected = false,
                            onClick = {
                                onSelectConversation(conversation.id)
                                onCloseDrawer()
                            },
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                        )
                    }
                }

                item {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(vertical = 16.dp))
                    Text(
                        text = if (isArabic) "نموذج الذكاء الاصطناعي" else "AI Models",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 24.dp, bottom = 8.dp)
                    )
                }

                item {
                    val availableModels = listOf(
                        com.example.data.model.AiModel.NABIH_ULTRA,
                        com.example.data.model.AiModel.GPT_4O,
                        com.example.data.model.AiModel.GEMINI_PRO,
                        com.example.data.model.AiModel.CLAUDE_SONNET
                    )
                    
                    availableModels.forEach { model ->
                        val isLocked = when (model) {
                            com.example.data.model.AiModel.NABIH_ULTRA -> false
                            com.example.data.model.AiModel.GPT_4O -> settings.openaiApiKey.isEmpty()
                            com.example.data.model.AiModel.GEMINI_PRO -> settings.googleApiKey.isEmpty()
                            com.example.data.model.AiModel.CLAUDE_SONNET -> settings.anthropicApiKey.isEmpty()
                            else -> false
                        }
                        NavigationDrawerItem(
                            icon = {
                                if (isLocked) {
                                    Icon(Icons.Outlined.Lock, contentDescription = "Locked", modifier = Modifier.size(20.dp))
                                } else {
                                    Icon(Icons.Outlined.CheckCircle, contentDescription = "Ready", modifier = Modifier.size(20.dp), tint = if (selectedModel.id == model.id) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            },
                            label = {
                                Text(
                                    text = model.displayName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isLocked) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface
                                )
                            },
                            selected = selectedModel.id == model.id,
                            onClick = {
                                if (isLocked) {
                                    android.widget.Toast.makeText(context, if (isArabic) "مفتاح API مطلوب" else "API Key Required", android.widget.Toast.LENGTH_SHORT).show()
                                    // Navigate to settings to add keys
                                    onNavigateToSettings()
                                    onCloseDrawer()
                                } else {
                                    onSelectModel(model)
                                    onCloseDrawer()
                                }
                            },
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Bottom Section
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            
            NavigationDrawerItem(
                icon = { Icon(Icons.Outlined.Settings, contentDescription = null) },
                label = { Text(if (isArabic) "الإعدادات" else "Settings") },
                selected = false,
                onClick = {
                    onNavigateToSettings()
                    onCloseDrawer()
                },
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
            
            NavigationDrawerItem(
                icon = { Icon(Icons.Outlined.AccountCircle, contentDescription = null) },
                label = { Text(if (settings.isLoggedIn) (settings.userName.ifEmpty { if (isArabic) "الحساب" else "Account" }) else (if (isArabic) "تسجيل الدخول" else "Sign In")) },
                selected = false,
                onClick = {
                    onNavigateTo("account")
                    onCloseDrawer()
                },
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }
    }
}"""
    
    with open('app/src/main/java/com/example/ui/screen/MainScreen.kt', 'w') as f:
        f.write(content.replace(old_drawer, new_drawer))
    print("Drawer replaced successfully.")
else:
    print("Could not find MainDrawerContent.")
