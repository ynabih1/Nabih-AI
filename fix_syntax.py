with open('app/src/main/java/com/example/chat/MainScreen.kt', 'r') as f:
    text = f.read()

import re
bad_block = """                            EmptyChatState(
                                isArabic = isArabic,
                                
                                    } else {
                                        chatViewModel.sendMessage(prompt)
                                    }
                                }
                            )"""
                            
good_block = """                            EmptyChatState(
                                isArabic = isArabic
                            )"""

text = text.replace(bad_block, good_block)

with open('app/src/main/java/com/example/chat/MainScreen.kt', 'w') as f:
    f.write(text)
