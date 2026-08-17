package com.example.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

object CodeSyntaxHighlighter {

    // Dark Terminal Theme Palette (Catppuccin / VS Code Dark)
    private val darkTag = Color(0xFF89B4FA)         // Blue (<style>, <head>, <body>)
    private val darkSelector = Color(0xFFF38BA8)    // Red / Pink (.logo span)
    private val darkProperty = Color(0xFFCDD6F4)    // Soft white/lavender for properties (width, height)
    private val darkNumber = Color(0xFFFAB387)      // Peach / Orange for numbers (55px, 50%)
    private val darkValue = Color(0xFFA6E3A1)       // Green for values & strings (white, block, "...")
    private val darkKeyword = Color(0xFFCBA6F7)     // Mauve / Purple for keywords (fun, val, class)
    private val darkComment = Color(0xFF6C7086)     // Muted grey for comments
    private val darkPunctuation = Color(0xFF9399B2)  // Braces, colons

    fun highlight(code: String, language: String): AnnotatedString {
        val lang = language.lowercase().trim()
        return when {
            lang in listOf("html", "xml", "svg") -> highlightHtml(code)
            lang in listOf("css", "scss", "sass", "less") -> highlightCss(code)
            lang in listOf("js", "javascript", "ts", "typescript", "json") -> highlightJs(code)
            lang in listOf("kt", "kotlin", "java", "py", "python", "cpp", "c", "csharp", "cs", "go", "rs", "rust", "swift") -> highlightGeneral(code)
            else -> highlightHtml(code)
        }
    }

    private fun highlightHtml(code: String): AnnotatedString {
        return buildAnnotatedString {
            val tagRegex = Regex("""(<!--.*?-->)|(</?[a-zA-Z0-9_-]+)|(/?>)|([a-zA-Z0-9_-]+)=("[^"]*"|'[^']*')|(\.[a-zA-Z0-9_-]+)|([a-zA-Z-]+)(:)|(\b\d+(?:px|rem|em|%|vh|vw|s|ms)?\b)|(\b[a-zA-Z0-9_-]+\b)""")
            
            var lastIndex = 0
            val matches = tagRegex.findAll(code)
            
            for (match in matches) {
                if (match.range.first > lastIndex) {
                    append(code.substring(lastIndex, match.range.first))
                }
                
                val matchText = match.value
                when {
                    matchText.startsWith("<!--") -> {
                        withStyle(SpanStyle(color = darkComment)) { append(matchText) }
                    }
                    matchText.startsWith("<") || matchText == ">" || matchText == "/>" -> {
                        withStyle(SpanStyle(color = darkTag, fontWeight = FontWeight.SemiBold)) { append(matchText) }
                    }
                    matchText.startsWith(".") -> {
                        withStyle(SpanStyle(color = darkSelector, fontWeight = FontWeight.SemiBold)) { append(matchText) }
                    }
                    matchText.contains("=") && (matchText.contains("\"") || matchText.contains("'")) -> {
                        val parts = matchText.split("=", limit = 2)
                        withStyle(SpanStyle(color = darkProperty)) { append(parts[0]) }
                        append("=")
                        if (parts.size > 1) {
                            withStyle(SpanStyle(color = darkValue)) { append(parts[1]) }
                        }
                    }
                    match.groups[7] != null && match.groups[8] != null -> {
                        withStyle(SpanStyle(color = darkProperty)) { append(match.groups[7]!!.value) }
                        append(":")
                    }
                    matchText.matches(Regex("""\b\d+(?:px|rem|em|%|vh|vw|s|ms)?\b""")) -> {
                        withStyle(SpanStyle(color = darkNumber, fontWeight = FontWeight.Medium)) { append(matchText) }
                    }
                    matchText in listOf("white", "black", "block", "inline", "flex", "grid", "none", "auto", "center", "hidden", "relative", "absolute", "fixed") -> {
                        withStyle(SpanStyle(color = darkValue)) { append(matchText) }
                    }
                    matchText in listOf("span", "div", "p", "a", "h1", "h2", "h3", "h4", "h5", "h6", "button", "input", "img", "svg", "table", "tr", "td", "th", "ul", "ol", "li", "header", "footer", "section", "main", "nav") -> {
                        withStyle(SpanStyle(color = darkTag)) { append(matchText) }
                    }
                    else -> {
                        append(matchText)
                    }
                }
                lastIndex = match.range.last + 1
            }
            
            if (lastIndex < code.length) {
                append(code.substring(lastIndex))
            }
        }
    }

    private fun highlightCss(code: String): AnnotatedString {
        return buildAnnotatedString {
            val regex = Regex("""(/\*.*?\*/)|(\.[a-zA-Z0-9_-]+|#[a-zA-Z0-9_-]+)|([a-zA-Z0-9_-]+)(\s*:)|(:\s*)([a-zA-Z0-9_-]+|\#[0-9a-fA-F]+)|(\b\d+(?:px|rem|em|%|vh|vw|s|ms)?\b)|([{}();])""")
            
            var lastIndex = 0
            for (match in regex.findAll(code)) {
                if (match.range.first > lastIndex) {
                    append(code.substring(lastIndex, match.range.first))
                }
                val matchText = match.value
                when {
                    matchText.startsWith("/*") -> {
                        withStyle(SpanStyle(color = darkComment)) { append(matchText) }
                    }
                    matchText.startsWith(".") || matchText.startsWith("#") -> {
                        withStyle(SpanStyle(color = darkSelector, fontWeight = FontWeight.SemiBold)) { append(matchText) }
                    }
                    match.groups[3] != null -> {
                        withStyle(SpanStyle(color = darkProperty)) { append(matchText) }
                    }
                    matchText.matches(Regex("""\b\d+(?:px|rem|em|%|vh|vw|s|ms)?\b""")) -> {
                        withStyle(SpanStyle(color = darkNumber)) { append(matchText) }
                    }
                    matchText in listOf("{", "}", "(", ")", ";", ":") -> {
                        withStyle(SpanStyle(color = darkPunctuation)) { append(matchText) }
                    }
                    else -> {
                        withStyle(SpanStyle(color = darkValue)) { append(matchText) }
                    }
                }
                lastIndex = match.range.last + 1
            }
            if (lastIndex < code.length) {
                append(code.substring(lastIndex))
            }
        }
    }

    private fun highlightJs(code: String): AnnotatedString {
        return buildAnnotatedString {
            val regex = Regex("""(//.*|/\*.*?\*/)|("[^"]*"|'[^']*'|`[^`]*`)|(\b(?:const|let|var|function|return|if|else|for|while|import|export|from|default|class|extends|new|this|typeof|async|await|try|catch)\b)|(\b\d+\.?\d*\b)|([{}()\[\];,.:])""")
            
            var lastIndex = 0
            for (match in regex.findAll(code)) {
                if (match.range.first > lastIndex) {
                    append(code.substring(lastIndex, match.range.first))
                }
                val text = match.value
                when {
                    text.startsWith("//") || text.startsWith("/*") -> {
                        withStyle(SpanStyle(color = darkComment)) { append(text) }
                    }
                    text.startsWith("\"") || text.startsWith("'") || text.startsWith("`") -> {
                        withStyle(SpanStyle(color = darkValue)) { append(text) }
                    }
                    match.groups[3] != null -> {
                        withStyle(SpanStyle(color = darkKeyword, fontWeight = FontWeight.SemiBold)) { append(text) }
                    }
                    match.groups[4] != null -> {
                        withStyle(SpanStyle(color = darkNumber)) { append(text) }
                    }
                    else -> {
                        withStyle(SpanStyle(color = darkPunctuation)) { append(text) }
                    }
                }
                lastIndex = match.range.last + 1
            }
            if (lastIndex < code.length) {
                append(code.substring(lastIndex))
            }
        }
    }

    private fun highlightGeneral(code: String): AnnotatedString {
        return buildAnnotatedString {
            val regex = Regex("""(//.*|/\*.*?\*/|#.*)|("[^"]*"|'[^']*')|(\b(?:val|var|fun|class|interface|object|return|if|else|when|for|while|import|package|public|private|protected|override|data|sealed|def|self|async|await|struct|enum|fn|mut)\b)|(\b\d+\.?\d*\b)|([{}()\[\];,.:])""")
            
            var lastIndex = 0
            for (match in regex.findAll(code)) {
                if (match.range.first > lastIndex) {
                    append(code.substring(lastIndex, match.range.first))
                }
                val text = match.value
                when {
                    text.startsWith("//") || text.startsWith("/*") || text.startsWith("#") -> {
                        withStyle(SpanStyle(color = darkComment)) { append(text) }
                    }
                    text.startsWith("\"") || text.startsWith("'") -> {
                        withStyle(SpanStyle(color = darkValue)) { append(text) }
                    }
                    match.groups[3] != null -> {
                        withStyle(SpanStyle(color = darkKeyword, fontWeight = FontWeight.SemiBold)) { append(text) }
                    }
                    match.groups[4] != null -> {
                        withStyle(SpanStyle(color = darkNumber)) { append(text) }
                    }
                    else -> {
                        withStyle(SpanStyle(color = darkPunctuation)) { append(text) }
                    }
                }
                lastIndex = match.range.last + 1
            }
            if (lastIndex < code.length) {
                append(code.substring(lastIndex))
            }
        }
    }
}
