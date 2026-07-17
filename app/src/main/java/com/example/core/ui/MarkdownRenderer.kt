package com.example.core.ui

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MarkdownRenderer(
    text: String,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val blocks = parseMarkdownBlocks(text, primaryColor)

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        blocks.forEach { block ->
            when (block) {
                is MarkdownBlock.CodeBlock -> {
                    CodeBlockLayout(
                        code = block.code,
                        language = block.language
                    )
                }
                is MarkdownBlock.TextBlock -> {
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

sealed interface MarkdownBlock {
    data class TextBlock(val annotatedText: AnnotatedString) : MarkdownBlock
    data class CodeBlock(val code: String, val language: String) : MarkdownBlock
}

private fun parseMarkdownBlocks(text: String, primaryColor: Color): List<MarkdownBlock> {
    val parts = text.split("```")
    val blocks = mutableListOf<MarkdownBlock>()

    for (i in parts.indices) {
        val part = parts[i]
        if (i % 2 == 1) {
            // This is a code block
            val lines = part.trim().lines()
            val language = if (lines.isNotEmpty() && lines.first().length < 15) lines.first() else "code"
            val code = if (lines.isNotEmpty() && lines.first() == language) {
                lines.drop(1).joinToString("\n")
            } else {
                part
            }
            blocks.add(MarkdownBlock.CodeBlock(code.trim(), language))
        } else {
            // Standard text blocks - parse simple markdown (bold, lists, headers)
            if (part.isNotBlank()) {
                blocks.add(MarkdownBlock.TextBlock(renderRichText(part, primaryColor)))
            }
        }
    }
    return blocks
}

private fun renderRichText(part: String, primaryColor: Color): AnnotatedString {
    return buildAnnotatedString {
        val lines = part.lines()
        lines.forEachIndexed { index, line ->
            var activeLine = line.trim()
            val isHeader = activeLine.startsWith("#")
            val isListItem = activeLine.startsWith("*") || activeLine.startsWith("-") || (activeLine.firstOrNull()?.isDigit() == true && activeLine.contains(". "))

            if (isHeader) {
                val depth = activeLine.takeWhile { it == '#' }.length
                val headerText = activeLine.drop(depth).trim()
                val fontSize = when (depth) {
                    1 -> 24.sp
                    2 -> 20.sp
                    else -> 18.sp
                }
                withStyle(
                    SpanStyle(
                        fontWeight = FontWeight.Bold,
                        fontSize = fontSize,
                        color = primaryColor
                    )
                ) {
                    append(headerText)
                }
            } else if (isListItem) {
                val bullet = if (activeLine.startsWith("*") || activeLine.startsWith("-")) "• " else ""
                val rawContent = if (bullet.isNotEmpty()) activeLine.drop(1).trim() else activeLine
                append(bullet)
                parseInlineStyles(rawContent, this)
            } else {
                parseInlineStyles(activeLine, this)
            }

            if (index < lines.size - 1) {
                append("\n")
            }
        }
    }
}

private fun parseInlineStyles(text: String, builder: AnnotatedString.Builder) {
    // Parse double stars **bold**
    val parts = text.split("**")
    for (i in parts.indices) {
        val part = parts[i]
        if (i % 2 == 1) {
            builder.withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                append(part)
            }
        } else {
            // Parse single stars *italic* or `inline code`
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
                        append(subPart)
                    }
                } else {
                    builder.append(subPart)
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
        // Header
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

        // Code Area
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
