theme = """package com.example.core.theme
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
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
)

private val DarkColorScheme = darkColorScheme(
    primary = AccentPrimaryDark,
    onPrimary = Color.White,
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
)

@Composable
fun NabihTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    isArabic: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val typography = if (isArabic) PlexArabicTypography else InterTypography
    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        content = content
    )
}"""

with open("app/src/main/java/com/example/core/theme/Theme.kt", "w") as f:
    f.write(theme)
