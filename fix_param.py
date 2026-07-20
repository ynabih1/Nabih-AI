with open('app/src/main/java/com/example/chat/MainScreen.kt', 'r') as f:
    text = f.read()

import re
text = re.sub(r'onSuggestionClick = \{ prompt ->.*?\}', '', text, flags=re.DOTALL)
text = text.replace('fun EmptyChatState(isArabic: Boolean, onSuggestionClick: (String) -> Unit) {', 'fun EmptyChatState(isArabic: Boolean) {')

with open('app/src/main/java/com/example/chat/MainScreen.kt', 'w') as f:
    f.write(text)
