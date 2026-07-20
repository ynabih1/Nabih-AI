with open('app/src/main/java/com/example/chat/MainScreen.kt', 'r') as f:
    text = f.read()

text = text.replace('border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha=0.3f)),\n                                    color = MaterialTheme.colorScheme.surface,\n                                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))', 'color = MaterialTheme.colorScheme.surface,\n                                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))')

with open('app/src/main/java/com/example/chat/MainScreen.kt', 'w') as f:
    f.write(text)
