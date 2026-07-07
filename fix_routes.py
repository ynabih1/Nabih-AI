import re

with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    code = f.read()

new_routes = """
                            composable("saved") {
                                SavedChatsScreen(onNavigateBack = { navController.popBackStack() }, isArabic = settings.language == AppLanguage.ARABIC)
                            }
                            composable("files") {
                                FilesScreen(onNavigateBack = { navController.popBackStack() }, isArabic = settings.language == AppLanguage.ARABIC)
                            }
                            composable("tools") {
                                AiToolsScreen(onNavigateBack = { navController.popBackStack() }, isArabic = settings.language == AppLanguage.ARABIC)
                            }
                            composable("account") {
                                AccountScreen(onNavigateBack = { navController.popBackStack() }, isArabic = settings.language == AppLanguage.ARABIC)
                            }
                            composable("privacy") {
                                PrivacyScreen(onNavigateBack = { navController.popBackStack() }, isArabic = settings.language == AppLanguage.ARABIC)
                            }
                            composable("help") {
                                HelpScreen(onNavigateBack = { navController.popBackStack() }, isArabic = settings.language == AppLanguage.ARABIC)
                            }
                        }
"""

code = code.replace("                        }\n                    }\n                }", new_routes + "                    }\n                }")

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(code)
