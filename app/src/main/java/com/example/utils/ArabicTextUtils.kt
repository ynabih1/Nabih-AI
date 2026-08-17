package com.example.utils

object ArabicTextUtils {
    // Unicode ranges for Arabic diacritics / Tashkeel:
    // U+064B: FATHATAN
    // U+064C: DAMMATAN
    // U+064D: KASRATAN
    // U+064E: FATHA
    // U+064F: DAMMA
    // U+0650: KASRA
    // U+0651: SHADDA
    // U+0652: SUKUN
    // U+0670: SUPERSCRIPT ALEF
    // U+0653-U+065F: Other Arabic diacritical marks
    private val TASHKEEL_REGEX = Regex("[\u064B-\u0652\u0653-\u065F\u0670]")

    /**
     * Removes all Arabic diacritical marks (Tashkeel) including Fatha, Damma, Kasra, Sukun, Shadda, Tanween, etc.
     */
    fun removeTashkeel(text: String): String {
        if (text.isEmpty()) return text
        return text.replace(TASHKEEL_REGEX, "")
    }
}
