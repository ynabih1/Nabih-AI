import re

with open('app/src/main/java/com/example/auth/LoginScreen.kt', 'r') as f:
    text = f.read()

# Replace any remaining old email colors
old_email = """                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                        disabledContainerColor = MaterialTheme.colorScheme.surface,
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                        errorBorderColor = MaterialTheme.colorScheme.error
                                    ),"""

new_email = """                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = if (androidx.compose.foundation.isSystemInDarkTheme()) MaterialTheme.colorScheme.surfaceVariant else Color.White,
                                        unfocusedContainerColor = if (androidx.compose.foundation.isSystemInDarkTheme()) MaterialTheme.colorScheme.surfaceVariant else Color.White,
                                        disabledContainerColor = if (androidx.compose.foundation.isSystemInDarkTheme()) MaterialTheme.colorScheme.surfaceVariant else Color.White,
                                        errorContainerColor = if (androidx.compose.foundation.isSystemInDarkTheme()) MaterialTheme.colorScheme.surfaceVariant else Color.White,
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                                        errorBorderColor = MaterialTheme.colorScheme.error
                                    ),"""

if old_email in text:
    text = text.replace(old_email, new_email)
    print("Replaced other input colors")
else:
    print("Other input colors not found")

with open('app/src/main/java/com/example/auth/LoginScreen.kt', 'w') as f:
    f.write(text)
