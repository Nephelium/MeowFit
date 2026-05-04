package com.example.calorietracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Renders text with basic Markdown formatting support:
 * - **bold** or __bold__
 * - *italic* or _italic_
 * - # / ## / ### headers
 * - - / * bullet items
 * - 【section】 Chinese-style bracketed headers
 */
@Composable
fun MarkdownText(
    text: String,
    modifier: Modifier = Modifier,
    baseColor: Color = Color.Unspecified,
    baseFontSize: Float = 15f,
    lineHeight: Float = 22f
) {
    val lines = text.split("\n")
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                continue
            }

            when {
                // Header 3
                trimmed.startsWith("### ") -> {
                    val content = trimmed.removePrefix("### ").trim()
                    Text(
                        buildAnnotatedString { parseInlineMarkdown(this, content) },
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = (baseFontSize + 1).sp,
                            lineHeight = (lineHeight + 2).sp,
                            color = baseColor
                        ),
                        modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
                    )
                }
                // Header 2
                trimmed.startsWith("## ") -> {
                    val content = trimmed.removePrefix("## ").trim()
                    Text(
                        buildAnnotatedString { parseInlineMarkdown(this, content) },
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = (baseFontSize + 2).sp,
                            lineHeight = (lineHeight + 3).sp,
                            color = baseColor
                        ),
                        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                    )
                }
                // Header 1 / 【section】
                trimmed.startsWith("# ") -> {
                    val content = trimmed.removePrefix("# ").trim()
                    Text(
                        buildAnnotatedString { parseInlineMarkdown(this, content) },
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = (baseFontSize + 4).sp,
                            lineHeight = (lineHeight + 4).sp,
                            color = baseColor
                        ),
                        modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
                    )
                }
                // Chinese bracketed header 【...】
                trimmed.matches(Regex("^【.*】$")) -> {
                    Text(
                        trimmed,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = (baseFontSize + 1).sp,
                            lineHeight = (lineHeight + 2).sp,
                            color = baseColor.copy(alpha = 0.9f)
                        ),
                        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                    )
                }
                // Bullet: - item or * item
                trimmed.startsWith("- ") || trimmed.startsWith("* ") -> {
                    val content = trimmed.drop(2).trim()
                    Row(modifier = Modifier.padding(start = 8.dp)) {
                        Text("•  ", style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = baseFontSize.sp,
                            lineHeight = lineHeight.sp,
                            color = baseColor.copy(alpha = 0.7f)
                        ))
                        Text(
                            buildAnnotatedString { parseInlineMarkdown(this, content) },
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = baseFontSize.sp,
                                lineHeight = lineHeight.sp,
                                color = baseColor
                            )
                        )
                    }
                }
                // Numbered list: 1. item
                trimmed.matches(Regex("^\\d+\\.\\s.*")) -> {
                    val number = trimmed.substringBefore(".")
                    val content = trimmed.substringAfter(". ").trim()
                    Row(modifier = Modifier.padding(start = 8.dp)) {
                        Text("$number.  ", style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = baseFontSize.sp,
                            lineHeight = lineHeight.sp,
                            color = baseColor.copy(alpha = 0.7f)
                        ))
                        Text(
                            buildAnnotatedString { parseInlineMarkdown(this, content) },
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = baseFontSize.sp,
                                lineHeight = lineHeight.sp,
                                color = baseColor
                            )
                        )
                    }
                }
                // Regular paragraph
                else -> {
                    Text(
                        buildAnnotatedString { parseInlineMarkdown(this, trimmed) },
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = baseFontSize.sp,
                            lineHeight = lineHeight.sp,
                            color = baseColor
                        )
                    )
                }
            }
        }
    }
}

/**
 * Parse inline markdown tokens (bold, italic) into an AnnotatedString builder.
 */
private fun parseInlineMarkdown(builder: AnnotatedString.Builder, text: String) {
    var i = 0
    while (i < text.length) {
        // Bold: **text**
        if (i + 2 < text.length && text[i] == '*' && text[i + 1] == '*') {
            val end = text.indexOf("**", i + 2)
            if (end > i) {
                builder.withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(text.substring(i + 2, end))
                }
                i = end + 2
                continue
            }
        }
        // Italic: *text* (but not **)
        if (i + 1 < text.length && text[i] == '*' && (i == 0 || text[i - 1] != '*')) {
            val end = text.indexOf("*", i + 1)
            if (end > i && (end + 1 >= text.length || text[end + 1] != '*')) {
                builder.withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                    append(text.substring(i + 1, end))
                }
                i = end + 1
                continue
            }
        }
        // Bold: __text__
        if (i + 2 < text.length && text[i] == '_' && text[i + 1] == '_') {
            val end = text.indexOf("__", i + 2)
            if (end > i) {
                builder.withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(text.substring(i + 2, end))
                }
                i = end + 2
                continue
            }
        }
        // Inline code: `text`
        if (text[i] == '`') {
            val end = text.indexOf("`", i + 1)
            if (end > i) {
                builder.withStyle(SpanStyle(
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    background = Color(0x22000000)
                )) {
                    append(text.substring(i + 1, end))
                }
                i = end + 1
                continue
            }
        }
        // Regular character
        builder.append(text[i])
        i++
    }
}
