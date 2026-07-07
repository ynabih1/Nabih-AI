import re

with open('app/src/main/java/com/example/ui/screen/MainScreen.kt', 'r') as f:
    code = f.read()

# I need to add `settings` to MainScreen arguments if not there, or just read it from ViewModel.
# MainScreen receives `chatViewModel`. We can collect `settings`.

old_viewmodel_collect = """    val selectedModel by chatViewModel.selectedModel.collectAsStateWithLifecycle()"""
new_viewmodel_collect = """    val selectedModel by chatViewModel.selectedModel.collectAsStateWithLifecycle()
    val settings by chatViewModel.settings.collectAsStateWithLifecycle()"""

code = code.replace(old_viewmodel_collect, new_viewmodel_collect)

# Update top bar dropdown
old_top_bar = """                            DropdownMenu(
                                expanded = modelMenuExpanded,
                                onDismissRequest = { modelMenuExpanded = false }
                            ) {
                                com.example.data.model.AiModel.values().forEach { model ->
                                    DropdownMenuItem(
                                        text = { Text(model.displayName) },
                                        onClick = {
                                            chatViewModel.selectModel(model)
                                            modelMenuExpanded = false
                                        }
                                    )
                                }
                            }"""

new_top_bar = """                            DropdownMenu(
                                expanded = modelMenuExpanded,
                                onDismissRequest = { modelMenuExpanded = false }
                            ) {
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
                                    DropdownMenuItem(
                                        text = { 
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                if (isLocked) {
                                                    Icon(Icons.Outlined.Lock, contentDescription = "Locked", modifier = Modifier.size(16.dp).padding(end = 4.dp))
                                                } else {
                                                    Icon(Icons.Outlined.Check, contentDescription = "Ready", modifier = Modifier.size(16.dp).padding(end = 4.dp), tint = if (selectedModel == model) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Transparent)
                                                }
                                                Text(model.displayName, color = if (isLocked) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface) 
                                            }
                                        },
                                        onClick = {
                                            if (isLocked) {
                                                android.widget.Toast.makeText(context, "API Key Required", android.widget.Toast.LENGTH_SHORT).show()
                                            } else {
                                                chatViewModel.selectModel(model)
                                                modelMenuExpanded = false
                                            }
                                        }
                                    )
                                }
                            }"""

code = code.replace(old_top_bar, new_top_bar)

# Update Drawer
old_drawer_call = """                        MainDrawerContent(
                isArabic = isArabic,
                conversations = activeConversations,
                selectedModel = selectedModel,
                onSelectModel = { chatViewModel.selectModel(it) },"""

new_drawer_call = """                        MainDrawerContent(
                isArabic = isArabic,
                conversations = activeConversations,
                selectedModel = selectedModel,
                settings = settings,
                onSelectModel = { chatViewModel.selectModel(it) },"""
code = code.replace(old_drawer_call, new_drawer_call)

old_drawer_sig = """@Composable
fun MainDrawerContent(
    isArabic: Boolean,
    conversations: List<com.example.data.database.Conversation>,
    selectedModel: com.example.data.model.AiModel,
    onSelectModel: (com.example.data.model.AiModel) -> Unit,"""
new_drawer_sig = """@Composable
fun MainDrawerContent(
    isArabic: Boolean,
    conversations: List<com.example.data.database.Conversation>,
    selectedModel: com.example.data.model.AiModel,
    settings: com.example.data.model.AppSettings,
    onSelectModel: (com.example.data.model.AiModel) -> Unit,"""
code = code.replace(old_drawer_sig, new_drawer_sig)

old_drawer_menu = """                            availableModels.forEach { model ->
                                NavigationDrawerItem(
                                    label = { Text(model.displayName, style = MaterialTheme.typography.bodyMedium) },
                                    selected = selectedModel.id == model.id,
                                    onClick = { onSelectModel(model); modelExpanded = false },
                                    modifier = Modifier.padding(start = 32.dp, end = 12.dp, top = 2.dp, bottom = 2.dp)
                                )
                            }"""

new_drawer_menu = """                            val context = androidx.compose.ui.platform.LocalContext.current
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
                                            Icon(Icons.Outlined.Lock, contentDescription = "Locked", modifier = Modifier.size(16.dp))
                                        } else if (selectedModel.id == model.id) {
                                            Icon(Icons.Outlined.Check, contentDescription = "Ready", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                        }
                                    },
                                    label = { 
                                        Text(
                                            model.displayName, 
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (isLocked) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface
                                        ) 
                                    },
                                    selected = selectedModel.id == model.id,
                                    onClick = { 
                                        if (isLocked) {
                                            android.widget.Toast.makeText(context, "API Key Required", android.widget.Toast.LENGTH_SHORT).show()
                                        } else {
                                            onSelectModel(model); modelExpanded = false 
                                        }
                                    },
                                    modifier = Modifier.padding(start = 32.dp, end = 12.dp, top = 2.dp, bottom = 2.dp)
                                )
                            }"""

code = code.replace(old_drawer_menu, new_drawer_menu)

with open('app/src/main/java/com/example/ui/screen/MainScreen.kt', 'w') as f:
    f.write(code)

