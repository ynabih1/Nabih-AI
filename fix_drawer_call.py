import re

with open('app/src/main/java/com/example/ui/screen/MainScreen.kt', 'r') as f:
    code = f.read()

# Replace the MainDrawerContent call
new_drawer_call = """            MainDrawerContent(
                isArabic = isArabic,
                conversations = activeConversations,
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

code = re.sub(r'MainDrawerContent\([\s\S]*?onNavigateToSettings = \{[\s\S]*?scope\.launch \{ drawerState\.close\(\) \}\n\s*\}\n\s*\)', new_drawer_call, code)

with open('app/src/main/java/com/example/ui/screen/MainScreen.kt', 'w') as f:
    f.write(code)

