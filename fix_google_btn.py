with open('app/src/main/java/com/example/auth/LoginScreen.kt', 'r') as f:
    text = f.read()

# I will revert the change for the google login button from previous turn
text = text.replace('containerColor = MaterialTheme.colorScheme.onBackground,\n                                        contentColor = MaterialTheme.colorScheme.background', 'containerColor = MaterialTheme.colorScheme.surface,\n                                        contentColor = MaterialTheme.colorScheme.onSurface')

with open('app/src/main/java/com/example/auth/LoginScreen.kt', 'w') as f:
    f.write(text)
