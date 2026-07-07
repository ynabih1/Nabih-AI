import re

with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    content = f.read()

old_account_call = '''                            composable("account") {
                                AccountScreen(onNavigateBack = { navController.popBackStack() }, isArabic = settings.language == AppLanguage.ARABIC)
                            }'''
                            
new_account_call = '''                            composable("account") {
                                AccountScreen(
                                    settingsViewModel = settingsViewModel,
                                    onNavigateBack = { navController.popBackStack() },
                                    isArabic = settings.language == AppLanguage.ARABIC,
                                    onLogout = {
                                        settingsViewModel.logout()
                                        navController.navigate("login") {
                                            popUpTo("home") { inclusive = true }
                                        }
                                    },
                                    onDeleteAccount = {
                                        settingsViewModel.viewModelScope.launch {
                                            appContainer.chatRepository.deleteAllConversations()
                                            appContainer.memoryRepository.deleteAllMemories()
                                            settingsViewModel.logout()
                                            settingsViewModel.saveApiKeys("", "", "")
                                            navController.navigate("login") {
                                                popUpTo("home") { inclusive = true }
                                            }
                                        }
                                    }
                                )
                            }'''
                            
content = content.replace(old_account_call, new_account_call)

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(content)
