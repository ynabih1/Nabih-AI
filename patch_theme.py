with open("app/src/main/java/com/example/core/theme/Theme.kt", "r") as f:
    content = f.read()

import re

light = """private val LightColorScheme = lightColorScheme(
    primary = AccentPrimary,
    onPrimary = Color.White,
    background = BackgroundScreen,
    onBackground = TextPrimary,
    surface = BackgroundScreen,
    onSurface = TextPrimary,
    outline = BorderColor,
    outlineVariant = BorderColor,
    surfaceVariant = BackgroundChatBox,
    onSurfaceVariant = TextSecondary,
    surfaceContainer = BackgroundChatBox,
    surfaceContainerLow = BackgroundScreen,
    surfaceContainerLowest = BackgroundChatBox,
    surfaceContainerHigh = BorderColor
)"""

dark = """private val DarkColorScheme = darkColorScheme(
    primary = AccentPrimaryDark,
    onPrimary = Color.Black,
    background = BackgroundScreenDark,
    onBackground = TextPrimaryDark,
    surface = BackgroundScreenDark,
    onSurface = TextPrimaryDark,
    outline = BorderColorDark,
    outlineVariant = BorderColorDark,
    surfaceVariant = BackgroundChatBoxDark,
    onSurfaceVariant = TextSecondaryDark,
    surfaceContainer = BackgroundChatBoxDark,
    surfaceContainerLow = BackgroundScreenDark,
    surfaceContainerLowest = BackgroundChatBoxDark,
    surfaceContainerHigh = BorderColorDark
)"""

content = re.sub(r'private val LightColorScheme = lightColorScheme\([^)]+\)', light, content, flags=re.DOTALL)
content = re.sub(r'private val DarkColorScheme = darkColorScheme\([^)]+\)', dark, content, flags=re.DOTALL)

with open("app/src/main/java/com/example/core/theme/Theme.kt", "w") as f:
    f.write(content)
