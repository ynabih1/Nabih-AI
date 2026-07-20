package com.example.ui.theme

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

val serifDisplay = GoogleFont("Source Serif 4")
val interFont = GoogleFont("Inter")
val plexArabic = GoogleFont("IBM Plex Sans Arabic")

val DisplaySerifFamily = FontFamily(
    Font(googleFont = serifDisplay, fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = serifDisplay, fontProvider = provider, weight = FontWeight.SemiBold),
    Font(googleFont = serifDisplay, fontProvider = provider, weight = FontWeight.Bold),
    Font(googleFont = serifDisplay, fontProvider = provider, weight = FontWeight.Black),
)

val BodySansFamily = FontFamily(
    Font(googleFont = interFont, fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = interFont, fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = interFont, fontProvider = provider, weight = FontWeight.SemiBold),
    Font(googleFont = interFont, fontProvider = provider, weight = FontWeight.Bold),
    Font(googleFont = interFont, fontProvider = provider, weight = FontWeight.Black),
)

val ArabicFamily = FontFamily(
    Font(googleFont = plexArabic, fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = plexArabic, fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = plexArabic, fontProvider = provider, weight = FontWeight.SemiBold),
    Font(googleFont = plexArabic, fontProvider = provider, weight = FontWeight.Bold),
    Font(googleFont = plexArabic, fontProvider = provider, weight = FontWeight.Black),
)

fun getDisplayFontFamily(isArabic: Boolean): FontFamily {
    return if (isArabic) ArabicFamily else DisplaySerifFamily
}

fun getBodyFontFamily(isArabic: Boolean): FontFamily {
    return if (isArabic) ArabicFamily else BodySansFamily
}

fun getFontFamilyForText(text: String, isArabicApp: Boolean): FontFamily {
    val containsArabic = text.any { it.code in 0x0600..0x06FF }
    return if (containsArabic || isArabicApp) ArabicFamily else BodySansFamily
}

// Function to dynamically build Material 3 typography based on default font family
fun createTypography(bodyFontFamily: FontFamily, displayFontFamily: FontFamily): Typography {
    return Typography(
        displayLarge = TextStyle(
            fontFamily = displayFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 32.sp,
            lineHeight = 40.sp,
            letterSpacing = (-0.5).sp
        ),
        displayMedium = TextStyle(
            fontFamily = displayFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            lineHeight = 36.sp,
            letterSpacing = 0.sp
        ),
        displaySmall = TextStyle(
            fontFamily = displayFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            lineHeight = 32.sp,
            letterSpacing = 0.sp
        ),
        headlineLarge = TextStyle(
            fontFamily = displayFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 28.sp,
            lineHeight = 36.sp,
            letterSpacing = 0.sp
        ),
        headlineMedium = TextStyle(
            fontFamily = displayFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 24.sp,
            lineHeight = 32.sp,
            letterSpacing = 0.sp
        ),
        headlineSmall = TextStyle(
            fontFamily = displayFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 22.sp,
            lineHeight = 28.sp,
            letterSpacing = 0.sp
        ),
        titleLarge = TextStyle(
            fontFamily = displayFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 24.sp,
            lineHeight = 32.sp,
            letterSpacing = 0.sp
        ),
        titleMedium = TextStyle(
            fontFamily = bodyFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.15.sp
        ),
        titleSmall = TextStyle(
            fontFamily = bodyFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.1.sp
        ),
        bodyLarge = TextStyle(
            fontFamily = bodyFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.5.sp
        ),
        bodyMedium = TextStyle(
            fontFamily = bodyFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.25.sp
        ),
        bodySmall = TextStyle(
            fontFamily = bodyFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.4.sp
        ),
        labelLarge = TextStyle(
            fontFamily = bodyFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.1.sp
        ),
        labelMedium = TextStyle(
            fontFamily = bodyFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.5.sp
        ),
        labelSmall = TextStyle(
            fontFamily = bodyFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.5.sp
        )
    )
}

val EnglishTypography = createTypography(BodySansFamily, DisplaySerifFamily)
val ArabicTypography = createTypography(ArabicFamily, ArabicFamily)

fun getChatBodyFontFamily(text: String, isArabicApp: Boolean): FontFamily {
    return getFontFamilyForText(text, isArabicApp)
}
