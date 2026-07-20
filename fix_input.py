with open('app/src/main/java/com/example/chat/MainScreen.kt', 'r') as f:
    text = f.read()

# Make the input area float nicely
text = text.replace('color = if (isFocused) MaterialTheme.colorScheme.surfaceContainerLowest else MaterialTheme.colorScheme.surfaceContainer,', 'color = MaterialTheme.colorScheme.surface,')
text = text.replace('shape = RoundedCornerShape(24.dp),', 'shape = RoundedCornerShape(28.dp),\n            shadowElevation = 8.dp,\n            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha=0.3f)),')

with open('app/src/main/java/com/example/chat/MainScreen.kt', 'w') as f:
    f.write(text)
