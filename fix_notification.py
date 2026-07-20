with open('app/src/main/java/com/example/chat/ChatViewModel.kt', 'r') as f:
    text = f.read()

# We need to insert a call to notificationHelper if settings.completionNotifications is true.
# Let's see how onCompletion handles success.
# It does:
# val responseText = _currentStreamingResponse.value
# if (responseText.isNotEmpty()) {
#     saveMessage(convId, "model", responseText)
# }
# We can just add our code right after saveMessage.

import re

# Find all occurrences of `saveMessage(convId, "model", responseText)`
# and insert notification logic.

replacement = """saveMessage(convId, "model", responseText)
                        if (settingsRepository.settings.value.completionNotifications) {
                            val title = if (settingsRepository.settings.value.language == com.example.model.AppLanguage.ARABIC) "اكتمل الرد" else "Response Completed"
                            val msg = if (settingsRepository.settings.value.language == com.example.model.AppLanguage.ARABIC) "أنهى Nabih Ultra إجابته." else "Nabih Ultra has finished answering."
                            notificationHelper.showCompletionNotification(title, msg, convId)
                        }"""

text = text.replace('saveMessage(convId, "model", responseText)', replacement)

with open('app/src/main/java/com/example/chat/ChatViewModel.kt', 'w') as f:
    f.write(text)
