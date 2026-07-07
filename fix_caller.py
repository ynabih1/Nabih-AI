import re
with open('app/src/main/java/com/example/ui/screen/MainScreen.kt', 'r') as f:
    content = f.read()

# Let's fix the call site
old_caller = """                        MainDrawerContent(
                isArabic = isArabic,
                conversations = activeConversations,
                selectedModel = selectedModel,
                settings = settings,
                onSelectModel = { chatViewModel.selectModel(it) },
                onNewChat = {
                    chatViewModel.cleanupIfTemporary() // Cleanup current if needed
                    val modelId = settings.defaultModel.id
                    chatViewModel.createNewChat(modelId)
                    scope.launch { drawerState.close() }
                },
                onSelectConversation = { conv ->
                    chatViewModel.selectConversation(conv.id)
                    scope.launch { drawerState.close() }
                },
                onNavigateToSettings = {
                    scope.launch { drawerState.close() }
                    onNavigateToSettings()
                },
                onNavigateTo = {
                    scope.launch { drawerState.close() }
                    onNavigateToRoute(it)
                }
            )"""

new_caller = """                        MainDrawerContent(
                settings = settings,
                selectedModel = selectedModel,
                onSelectModel = { chatViewModel.selectModel(it) },
                conversations = activeConversations,
                onSelectConversation = { id ->
                    chatViewModel.selectConversation(id)
                },
                onNewChat = {
                    chatViewModel.cleanupIfTemporary() // Cleanup current if needed
                    chatViewModel.createNewChat(settings.defaultModel.id)
                },
                onNavigateToSettings = onNavigateToSettings,
                onNavigateTo = onNavigateToRoute,
                onCloseDrawer = {
                    scope.launch { drawerState.close() }
                }
            )"""

content = content.replace(old_caller, new_caller)
with open('app/src/main/java/com/example/ui/screen/MainScreen.kt', 'w') as f:
    f.write(content)
