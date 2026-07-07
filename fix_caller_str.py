with open('app/src/main/java/com/example/ui/screen/MainScreen.kt', 'r') as f:
    content = f.read()

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
                    onNavigateToSettings()
                    scope.launch { drawerState.close() }
                },
                onNavigateTo = { route ->
                    scope.launch { drawerState.close() }
                    if (route == "search") {
                        chatViewModel.toggleSearch()
                    } else {
                        onNavigateToRoute(route)
                    }
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
                onNavigateTo = { route -> 
                    if (route == "search") {
                        chatViewModel.toggleSearch()
                    } else {
                        onNavigateToRoute(route)
                    }
                },
                onCloseDrawer = {
                    scope.launch { drawerState.close() }
                }
            )"""

content = content.replace(old_caller, new_caller)
with open('app/src/main/java/com/example/ui/screen/MainScreen.kt', 'w') as f:
    f.write(content)
