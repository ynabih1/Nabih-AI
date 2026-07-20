with open('app/src/main/java/com/example/chat/MainScreen.kt', 'r') as f:
    text = f.read()

# Fix the duplicate arguments
text = text.replace('border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha=0.3f)),\n            color = MaterialTheme.colorScheme.surface,\n            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),\n            shadowElevation = if (isFocused) 2.dp else 0.dp', 'color = MaterialTheme.colorScheme.surface')

with open('app/src/main/java/com/example/chat/MainScreen.kt', 'w') as f:
    f.write(text)
