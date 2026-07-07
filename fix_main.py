import re

with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    content = f.read()

old_str = """                                    onDeleteAccount = {
                                        settingsViewModel.viewModelScope.launch {
                                            appContainer.chatRepository.deleteAllConversations()
                                            appContainer.memoryRepository.deleteAllMemories()
                                            settingsViewModel.logout()
                                            settingsViewModel.saveApiKeys("", "", "")
                                            navController.navigate("login") {
                                                popUpTo("home") { inclusive = true }
                                            }
                                        }
                                    },"""

content = content.replace(old_str, "")

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(content)
