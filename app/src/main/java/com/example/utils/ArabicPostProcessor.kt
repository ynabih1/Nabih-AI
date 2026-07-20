package com.example.utils

object ArabicPostProcessor {
    fun process(text: String): String {
        var processed = text
        
        // Fix spaced punctuation
        processed = processed.replace(Regex("\\s+([،.؟!:])"), "$1")
        processed = processed.replace(Regex("([،.؟!:])(?=[^\\s\"'»”’])"), "$1 ")
        
        // Common typo fixes for Ya/Alef Maksura at the end of common words
        processed = processed.replace(Regex("\\bعلى\\b"), "علَى")
        processed = processed.replace(Regex("\\bالى\\b"), "إلى")
        processed = processed.replace(Regex("\\bفى\\b"), "في")
        
        // Enforce proper spacing around conjunctions like Waw
        // (careful not to separate it from the word, Arabic Waw is attached)
        processed = processed.replace(Regex("\\bو\\s+"), "و")
        
        // Normalizing Alef with Hamza where commonly omitted at start of words
        processed = processed.replace(Regex("\\bاذا\\b"), "إذا")
        processed = processed.replace(Regex("\\bان\\b"), "إن")
        
        // Remove multiple consecutive spaces
        processed = processed.replace(Regex(" {2,}"), " ")
        
        return processed
    }
}
