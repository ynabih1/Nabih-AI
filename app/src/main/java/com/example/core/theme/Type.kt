package com.example.core.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.example.R

// Downloadable fonts provider config
val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

val interFontName = GoogleFont("Inter")
val plexArabicFontName = GoogleFont("IBM Plex Sans Arabic")
val sourceSerifFontName = GoogleFont("Source Serif 4")

val InterFontFamily = FontFamily(
    Font(googleFont = interFontName, fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = interFontName, fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = interFontName, fontProvider = provider, weight = FontWeight.SemiBold),
)

val PlexArabicFontFamily = FontFamily(
    Font(googleFont = plexArabicFontName, fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = plexArabicFontName, fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = plexArabicFontName, fontProvider = provider, weight = FontWeight.SemiBold),
)

val SourceSerif4FontFamily = FontFamily(
    Font(googleFont = sourceSerifFontName, fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = sourceSerifFontName, fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = sourceSerifFontName, fontProvider = provider, weight = FontWeight.SemiBold),
)

// Helper to determine font family dynamically based on content and app locale
fun getFontFamilyForText(text: String, isArabicApp: Boolean): FontFamily {
    val containsArabic = text.any { it.code in 0x0600..0x06FF }
    return if (containsArabic || isArabicApp) PlexArabicFontFamily else InterFontFamily
}

fun getChatBodyFontFamily(text: String, isArabicApp: Boolean): FontFamily {
    val containsArabic = text.any { it.code in 0x0600..0x06FF }
    return when {
        containsArabic -> PlexArabicFontFamily
        isArabicApp -> PlexArabicFontFamily
        else -> SourceSerif4FontFamily
    }
}

// Function to dynamically build Material 3 typography based on default font family
fun createTypography(fontFamily: FontFamily): Typography {
    return Typography(
        displayLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 32.sp,
            lineHeight = 40.sp,
            letterSpacing = (-0.5).sp
        ),
        headlineMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 24.sp,
            lineHeight = 32.sp,
            letterSpacing = 0.sp
        ),
        titleLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 20.sp,
            lineHeight = 26.sp,
            letterSpacing = 0.15.sp
        ),
        titleMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.15.sp
        ),
        bodyLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.5.sp
        ),
        bodyMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.25.sp
        ),
        labelLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.1.sp
        )
    )
}

val InterTypography = createTypography(InterFontFamily)
val PlexArabicTypography = createTypography(PlexArabicFontFamily)

// Default export for standard themes
val Typography = InterTypography
