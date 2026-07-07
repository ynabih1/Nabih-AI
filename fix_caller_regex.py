import re

with open('app/src/main/java/com/example/ui/screen/MainScreen.kt', 'r') as f:
    content = f.read()

# Match the old MainDrawerContent call
pattern = r'MainDrawerContent\(\s*isArabic = isArabic,[\s\S]*?onNavigateToRoute\(route\)\n\s*\}\n\s*\n\s*\)'

new_caller = """MainDrawerContent(
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

content, count = re.subn(pattern, new_caller, content)
print(f"Replaced {count} instances.")

with open('app/src/main/java/com/example/ui/screen/MainScreen.kt', 'w') as f:
    f.write(content)
