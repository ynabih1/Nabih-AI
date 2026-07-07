import re

with open('app/src/main/java/com/example/ui/screen/MainScreen.kt', 'r') as f:
    code = f.read()

# I will replace everything from `ModalDrawerSheet(` inside `MainDrawerContent` until the end of the file.
# I can do this using regex.

drawer_start_idx = code.find("fun MainDrawerContent(")
if drawer_start_idx != -1:
    modal_start_idx = code.find("    ModalDrawerSheet(", drawer_start_idx)
    if modal_start_idx != -1:
        # Keep everything before modal_start_idx
        prefix = code[:modal_start_idx]
        
        replacement = """    ModalDrawerSheet(
        drawerContainerColor = MaterialTheme.colorScheme.surface,
        drawerShape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp),
        modifier = Modifier.width(320.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(vertical = 12.dp)) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.logo), contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Nabih AI",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "yousif@nabih.ai",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 8.dp))
            
            LazyColumn(modifier = Modifier.weight(1f)) {
                item {
                    Text(
                        text = if (isArabic) "القائمة الرئيسية" else "Main Menu",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                    )
                }
                
                item {
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Outlined.Add, null) },
                        label = { Text(if (isArabic) "محادثة جديدة" else "New Chat", fontWeight = FontWeight.SemiBold) },
                        selected = false,
                        onClick = onNewChat,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }
                
                item {
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Outlined.Search, null) },
                        label = { Text(if (isArabic) "البحث" else "Search") },
                        selected = false,
                        onClick = { onNavigateTo("search") },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }
                
                item {
                    var historyExpanded by remember { mutableStateOf(false) }
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Outlined.History, null) },
                        label = { Text(if (isArabic) "سجل المحادثات" else "Chat History") },
                        selected = false,
                        onClick = { historyExpanded = !historyExpanded },
                        modifier = Modifier.padding(horizontal = 12.dp),
                        badge = { Icon(if (historyExpanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown, null) }
                    )
                    
                    AnimatedVisibility(visible = historyExpanded) {
                        Column {
                            conversations.take(5).forEach { conv ->
                                NavigationDrawerItem(
                                    icon = { Icon(Icons.Outlined.ChatBubbleOutline, null, modifier = Modifier.size(20.dp)) },
                                    label = { 
                                        Text(
                                            text = conv.title,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            style = MaterialTheme.typography.bodyMedium
                                        ) 
                                    },
                                    selected = false,
                                    onClick = { onSelectConversation(conv) },
                                    modifier = Modifier.padding(start = 32.dp, end = 12.dp, top = 2.dp, bottom = 2.dp)
                                )
                            }
                            if (conversations.size > 5) {
                                NavigationDrawerItem(
                                    label = { Text(if (isArabic) "عرض الكل..." else "View All...", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary) },
                                    selected = false,
                                    onClick = { onNavigateTo("history") },
                                    modifier = Modifier.padding(start = 52.dp, end = 12.dp)
                                )
                            }
                        }
                    }
                }
                
                item {
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Outlined.BookmarkBorder, null) },
                        label = { Text(if (isArabic) "المحادثات المحفوظة" else "Saved Chats") },
                        selected = false,
                        onClick = { onNavigateTo("saved") },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }
                
                item {
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Outlined.Folder, null) },
                        label = { Text(if (isArabic) "الملفات والمستندات" else "Files & Documents") },
                        selected = false,
                        onClick = { onNavigateTo("files") },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }
                
                item {
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Outlined.Build, null) },
                        label = { Text(if (isArabic) "أدوات الذكاء الاصطناعي" else "AI Tools") },
                        selected = false,
                        onClick = { onNavigateTo("tools") },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }
                
                item {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 8.dp))
                    Text(
                        text = if (isArabic) "نموذج الذكاء الاصطناعي" else "AI Model",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                    )
                }
                
                item {
                    var modelExpanded by remember { mutableStateOf(false) }
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Outlined.AutoAwesome, null) },
                        label = { Text(selectedModel.displayName, fontWeight = FontWeight.Medium) },
                        selected = false,
                        onClick = { modelExpanded = !modelExpanded },
                        modifier = Modifier.padding(horizontal = 12.dp),
                        badge = { Icon(if (modelExpanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown, null) }
                    )
                    
                    AnimatedVisibility(visible = modelExpanded) {
                        Column {
                            val availableModels = listOf(
                                com.example.data.model.AiModel.NABIH_ULTRA,
                                com.example.data.model.AiModel.GPT_4O,
                                com.example.data.model.AiModel.GEMINI_PRO,
                                com.example.data.model.AiModel.CLAUDE_SONNET
                            )
                            availableModels.forEach { model ->
                                NavigationDrawerItem(
                                    label = { Text(model.displayName, style = MaterialTheme.typography.bodyMedium) },
                                    selected = selectedModel.id == model.id,
                                    onClick = { onSelectModel(model); modelExpanded = false },
                                    modifier = Modifier.padding(start = 32.dp, end = 12.dp, top = 2.dp, bottom = 2.dp)
                                )
                            }
                        }
                    }
                }
                
                item {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 8.dp))
                    Text(
                        text = if (isArabic) "الإعدادات" else "Settings",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                    )
                }
                
                item {
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Outlined.Settings, null) },
                        label = { Text(if (isArabic) "الإعدادات العامة" else "Settings") },
                        selected = false,
                        onClick = onNavigateToSettings,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }
                
                item {
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Outlined.VpnKey, null) },
                        label = { Text(if (isArabic) "مفاتيح API" else "API Keys") },
                        selected = false,
                        onClick = { onNavigateTo("api_keys") },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }
                
                item {
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Outlined.Palette, null) },
                        label = { Text(if (isArabic) "المظهر" else "Appearance") },
                        selected = false,
                        onClick = { onNavigateTo("appearance") },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }
                
                item {
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Outlined.Language, null) },
                        label = { Text(if (isArabic) "اللغة" else "Language") },
                        selected = false,
                        onClick = { onNavigateTo("language") },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }
                
                item {
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Outlined.PrivacyTip, null) },
                        label = { Text(if (isArabic) "الخصوصية والأمان" else "Privacy & Security") },
                        selected = false,
                        onClick = { onNavigateTo("privacy") },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }
                
                item {
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Outlined.HelpOutline, null) },
                        label = { Text(if (isArabic) "المساعدة والتعليقات" else "Help & Feedback") },
                        selected = false,
                        onClick = { onNavigateTo("help") },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }
                
                item {
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Outlined.Info, null) },
                        label = { Text(if (isArabic) "حول Nabih AI" else "About Nabih AI") },
                        selected = false,
                        onClick = { onNavigateTo("about") },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }
            }
        }
    }
}
"""

        new_code = prefix + replacement
        with open('app/src/main/java/com/example/ui/screen/MainScreen.kt', 'w') as f:
            f.write(new_code)
        print("Replaced!")
    else:
        print("modal_start_idx not found")
else:
    print("drawer_start_idx not found")
