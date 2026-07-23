package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = AccentPrimary,
    onPrimary = Color.White,
    primaryContainer = AccentPrimary.copy(alpha = 0.12f),
    onPrimaryContainer = TextPrimary,
    secondary = AccentPrimary,
    onSecondary = Color.White,
    secondaryContainer = SurfaceElevated,
    onSecondaryContainer = TextPrimary,
    background = AppBackground,
    onBackground = TextPrimary,
    surface = SurfaceCard,
    onSurface = TextPrimary,
    outline = BorderSubtle,
    outlineVariant = BorderSubtle,
    surfaceVariant = SurfaceElevated,
    onSurfaceVariant = TextSecondary,
    surfaceContainer = SurfaceElevated,
    surfaceContainerLow = AppBackground,
    surfaceContainerLowest = SurfaceElevated,
    surfaceContainerHigh = BorderSubtle
)

private val DarkColorScheme = darkColorScheme(
    primary = AccentPrimaryDark,
    onPrimary = Color.Black,
    primaryContainer = AccentPrimaryDark.copy(alpha = 0.18f),
    onPrimaryContainer = TextPrimaryDark,
    secondary = AccentPrimaryDark,
    onSecondary = Color.Black,
    secondaryContainer = SurfaceElevatedDark,
    onSecondaryContainer = TextPrimaryDark,
    background = AppBackgroundDark,
    onBackground = TextPrimaryDark,
    surface = SurfaceCardDark,
    onSurface = TextPrimaryDark,
    outline = BorderSubtleDark,
    outlineVariant = BorderSubtleDark,
    surfaceVariant = SurfaceElevatedDark,
    onSurfaceVariant = TextSecondaryDark,
    surfaceContainer = SurfaceElevatedDark,
    surfaceContainerLow = AppBackgroundDark,
    surfaceContainerLowest = SurfaceElevatedDark,
    surfaceContainerHigh = BorderSubtleDark
)

@Composable
fun NabihTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    isArabic: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val typography = if (isArabic) ArabicTypography else EnglishTypography

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        content = content
    )
}
