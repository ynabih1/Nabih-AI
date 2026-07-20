package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MarkdownRenderer(
    text: String,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val blocks = parseMarkdownBlocks(text, primaryColor)
    
    // Explicitly enforce proper text direction and layout
    // Even if it's RTL, the LRM marks will ensure English LTR words are ordered correctly
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        val layoutDirection = LocalLayoutDirection.current
        
        Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            blocks.forEach { block ->
                when (block) {
                    is MarkdownBlock.CodeBlock -> {
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            CodeBlockLayout(
                                code = block.code,
                                language = block.language
                            )
                        }
                    }
                    is MarkdownBlock.Paragraph -> {
                        Text(
                            text = block.annotatedText,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    is MarkdownBlock.Header -> {
                        val fontSize = when (block.level) {
                            1 -> 24.sp
                            2 -> 20.sp
                            else -> 18.sp
                        }
                        Text(
                            text = block.annotatedText,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontSize = fontSize,
                                fontWeight = FontWeight.Bold
                            ),
                            color = primaryColor
                        )
                    }
                    is MarkdownBlock.ListItem -> {
                        Row(
                            verticalAlignment = Alignment.Top,
                            modifier = Modifier.padding(start = 16.dp)
                        ) {
                            Text(
                                text = block.bullet + " ",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onBackground,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = block.annotatedText,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }
            }
        }
    }
}

sealed interface MarkdownBlock {
    data class CodeBlock(val code: String, val language: String) : MarkdownBlock
    data class Paragraph(val annotatedText: AnnotatedString) : MarkdownBlock
    data class Header(val annotatedText: AnnotatedString, val level: Int) : MarkdownBlock
    data class ListItem(val annotatedText: AnnotatedString, val bullet: String) : MarkdownBlock
}

private fun parseMarkdownBlocks(text: String, primaryColor: Color): List<MarkdownBlock> {
    val parts = text.split("```")
    val blocks = mutableListOf<MarkdownBlock>()
    
    for (i in parts.indices) {
        val part = parts[i]
        if (i % 2 == 1) {
            val lines = part.trim().lines()
            val language = if (lines.isNotEmpty() && lines.first().length < 15) lines.first() else "code"
            val code = if (lines.isNotEmpty() && lines.first() == language) {
                lines.drop(1).joinToString("\n")
            } else {
                part
            }
            blocks.add(MarkdownBlock.CodeBlock(code.trim(), language))
        } else {
            val lines = part.lines()
            var currentParagraph = java.lang.StringBuilder()
            
            for (line in lines) {
                val activeLine = line.trim()
                if (activeLine.isEmpty()) {
                    if (currentParagraph.isNotEmpty()) {
                        blocks.add(MarkdownBlock.Paragraph(renderRichText(currentParagraph.toString(), primaryColor)))
                        currentParagraph = java.lang.StringBuilder()
                    }
                    continue
                }
                
                val isHeader = activeLine.startsWith("#")
                val isUnorderedList = activeLine.startsWith("* ") || activeLine.startsWith("- ")
                val isOrderedList = activeLine.firstOrNull()?.isDigit() == true && activeLine.contains(". ")
                
                if (isHeader) {
                    if (currentParagraph.isNotEmpty()) {
                        blocks.add(MarkdownBlock.Paragraph(renderRichText(currentParagraph.toString(), primaryColor)))
                        currentParagraph = java.lang.StringBuilder()
                    }
                    val depth = activeLine.takeWhile { it == '#' }.length
                    val headerText = activeLine.drop(depth).trim()
                    blocks.add(MarkdownBlock.Header(renderRichText(headerText, primaryColor), depth))
                } else if (isUnorderedList) {
                    if (currentParagraph.isNotEmpty()) {
                        blocks.add(MarkdownBlock.Paragraph(renderRichText(currentParagraph.toString(), primaryColor)))
                        currentParagraph = java.lang.StringBuilder()
                    }
                    val content = activeLine.drop(2).trim()
                    blocks.add(MarkdownBlock.ListItem(renderRichText(content, primaryColor), "•"))
                } else if (isOrderedList) {
                    if (currentParagraph.isNotEmpty()) {
                        blocks.add(MarkdownBlock.Paragraph(renderRichText(currentParagraph.toString(), primaryColor)))
                        currentParagraph = java.lang.StringBuilder()
                    }
                    val dotIndex = activeLine.indexOf(". ")
                    val number = activeLine.substring(0, dotIndex)
                    val content = activeLine.substring(dotIndex + 2).trim()
                    blocks.add(MarkdownBlock.ListItem(renderRichText(content, primaryColor), "\$number."))
                } else {
                    if (currentParagraph.isNotEmpty()) {
                        currentParagraph.append(" ")
                    }
                    currentParagraph.append(activeLine)
                }
            }
            if (currentParagraph.isNotEmpty()) {
                blocks.add(MarkdownBlock.Paragraph(renderRichText(currentParagraph.toString(), primaryColor)))
            }
        }
    }
    return blocks
}

private fun renderRichText(text: String, primaryColor: Color): AnnotatedString {
    return buildAnnotatedString {
        parseInlineStyles(text, this)
    }
}

private fun addBidiMarks(text: String): String {
    // Add Left-to-Right Mark (LRM) around English terms to maintain order in RTL layout
    val englishRegex = Regex("""([a-zA-Z0-9]+(?:[\s_\-.,:]+[a-zA-Z0-9]+)*)""")
    return text.replace(englishRegex) { matchResult ->
        "‎${matchResult.value}‎"
    }
}

private fun parseInlineStyles(text: String, builder: AnnotatedString.Builder) {
    val parts = text.split("**")
    for (i in parts.indices) {
        val part = parts[i]
        if (i % 2 == 1) {
            builder.withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                // Ensure English terms in bold are wrapped with LRM
                append(addBidiMarks(part))
            }
        } else {
            val subParts = part.split("`")
            for (j in subParts.indices) {
                val subPart = subParts[j]
                if (j % 2 == 1) {
                    builder.withStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            background = Color(0x1F808080),
                            fontSize = 13.sp
                        )
                    ) {
                        append(addBidiMarks(subPart))
                    }
                } else {
                    builder.append(addBidiMarks(subPart))
                }
            }
        }
    }
}

@Composable
fun CodeBlockLayout(
    code: String,
    language: String,
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    
    val codeBgColor = Color(0xFF1E1E2E)
    val headerBgColor = Color(0xFF151521)
    val textColor = Color(0xFFCDD6F4)
    val accentColor = Color(0xFF89B4FA)
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(codeBgColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(headerBgColor)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = language.lowercase().replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = accentColor,
                fontFamily = FontFamily.Monospace
            )
            IconButton(
                onClick = {
                    clipboardManager.setText(AnnotatedString(code))
                    Toast.makeText(context, if (context.resources.configuration.locales[0].language == "ar") "تم نسخ الكود البرمجي" else "Code copied to clipboard", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.ContentCopy,
                    contentDescription = "Copy code",
                    tint = Color(0xFFA6ADC8),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text(
                text = code,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.5.sp,
                color = textColor,
                lineHeight = 20.sp
            )
        }
    }
}
