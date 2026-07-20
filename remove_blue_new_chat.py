import os

filepath = 'app/src/main/java/com/example/chat/MainScreen.kt'
if os.path.exists(filepath):
    with open(filepath, 'r') as f:
        text = f.read()

    old_colors = """                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),"""
    new_colors = """                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),"""

    if old_colors in text:
        text = text.replace(old_colors, new_colors)
        print("Replaced colors")
    else:
        print("Colors pattern not found")

    with open(filepath, 'w') as f:
        f.write(text)
