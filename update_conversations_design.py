import re

path = 'app/src/main/java/com/example/feature/chat/MainScreen.kt'
with open(path, 'r') as f:
    content = f.read()

# Replace NavigationDrawerItem colors for conversations
old_colors = """                                colors = NavigationDrawerItemDefaults.colors(
                                    unselectedContainerColor = Color.Transparent,
                                    selectedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ),"""

new_colors = """                                colors = NavigationDrawerItemDefaults.colors(
                                    unselectedContainerColor = Color.Transparent,
                                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                    selectedTextColor = MaterialTheme.colorScheme.onSecondaryContainer
                                ),"""

if old_colors in content:
    content = content.replace(old_colors, new_colors)
    with open(path, 'w') as f:
        f.write(content)
    print("Updated conversations styling")
else:
    print("Old styling not found")

