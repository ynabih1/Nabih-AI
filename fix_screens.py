import re

with open('app/src/main/java/com/example/ui/screen/FeatureScreens.kt', 'r') as f:
    code = f.read()

code = code.replace("content: @Composable PaddingValues.() -> Unit", "content: @Composable (PaddingValues) -> Unit")

with open('app/src/main/java/com/example/ui/screen/FeatureScreens.kt', 'w') as f:
    f.write(code)

with open('app/src/main/java/com/example/ui/screen/MainScreen.kt', 'r') as f:
    code2 = f.read()

# Fix the MainScreen parameter `onNavigateTo` not being passed in empty state
empty_state_drawer = r"""MainDrawerContent\(
                                    isArabic = isArabic,
                                    conversations = activeConversations,
                                    onNewChat = \{
                                        chatViewModel.cleanupIfTemporary\(\)
                                        chatViewModel.createNewChat\(\)
                                        scope.launch \{ drawerState.close\(\) \}
                                    \},
                                    onSelectConversation = \{ conv ->
                                        chatViewModel.loadConversation\(conv.id\)
                                        scope.launch \{ drawerState.close\(\) \}
                                    \},
                                    onNavigateToSettings = \{
                                        scope.launch \{ drawerState.close\(\) \}
                                        onNavigateToSettings\(\)
                                    \}
                                \)"""

new_empty_drawer = """MainDrawerContent(
                                    isArabic = isArabic,
                                    conversations = activeConversations,
                                    onNewChat = {
                                        chatViewModel.cleanupIfTemporary()
                                        chatViewModel.createNewChat()
                                        scope.launch { drawerState.close() }
                                    },
                                    onSelectConversation = { conv ->
                                        chatViewModel.loadConversation(conv.id)
                                        scope.launch { drawerState.close() }
                                    },
                                    onNavigateToSettings = {
                                        scope.launch { drawerState.close() }
                                        onNavigateToSettings()
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

code2 = re.sub(empty_state_drawer, new_empty_drawer, code2)

with open('app/src/main/java/com/example/ui/screen/MainScreen.kt', 'w') as f:
    f.write(code2)

