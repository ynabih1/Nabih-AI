package com.example.utils

object ArabicPostProcessor {
    fun process(text: String): String {
        var processed = text
        
        // 1. Remove all diacritical marks (Tashkeel) including Fatha, Damma, Kasra, Sukun, Shadda, Tanween
        processed = ArabicTextUtils.removeTashkeel(processed)
        
        // 2. Fix spaced punctuation
        processed = processed.replace(Regex("\\s+([،.؟!:])"), "$1")
        processed = processed.replace(Regex("([،.؟!:])(?=[^\\s\"'»”’])"), "$1 ")
        
        // 3. Common typo fixes without diacritics
        processed = processed.replace(Regex("\\bعلى\\b"), "على")
        processed = processed.replace(Regex("\\bالى\\b"), "إلى")
        processed = processed.replace(Regex("\\bفى\\b"), "في")
        
        // 4. Enforce proper spacing around conjunctions like Waw
        processed = processed.replace(Regex("\\bو\\s+"), "و")
        
        // 5. Normalizing Alef with Hamza where commonly omitted at start of words
        processed = processed.replace(Regex("\\bاذا\\b"), "إذا")
        processed = processed.replace(Regex("\\bان\\b"), "إن")
        
        // 6. Remove multiple consecutive spaces
        processed = processed.replace(Regex(" {2,}"), " ")
        
        return processed
    }
}
