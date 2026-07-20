with open('app/src/main/java/com/example/chat/MainScreen.kt', 'r') as f:
    text = f.read()

import re

# We will apply a nice gradient background to Scaffold and TopAppBar
text = text.replace('containerColor = MaterialTheme.colorScheme.surface\n                        )', 'containerColor = Color.Transparent\n                        )')
text = text.replace('containerColor = MaterialTheme.colorScheme.surface\n        ) { paddingValues ->', 'containerColor = Color.Transparent\n        ) { paddingValues ->')

scaffold = """        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            contentWindowInsets = WindowInsets.safeDrawing,"""

gradient_box = """        Box(
            modifier = Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
        )
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            contentWindowInsets = WindowInsets.safeDrawing,"""

text = text.replace(scaffold, gradient_box)

with open('app/src/main/java/com/example/chat/MainScreen.kt', 'w') as f:
    f.write(text)
