package com.example.myfit.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp

@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = MaterialTheme.typography.bodyMedium,
    color: Color = LocalContentColor.current
) {
    val lines = markdown.trimEnd().split("\n")

    Column(modifier = modifier) {
        val codeLines = mutableListOf<String>()
        var inCode = false

        lines.forEach { rawLine ->
            if (rawLine.trimStart().startsWith("```")) {
                if (inCode) {
                    CodeBlock(code = codeLines.joinToString("\n"))
                    codeLines.clear()
                    inCode = false
                } else {
                    inCode = true
                }
                return@forEach
            }
            if (inCode) {
                codeLines.add(rawLine)
                return@forEach
            }

            val line = rawLine.trimEnd()
            when {
                line.startsWith("### ") -> HeadingText(
                    text = line.removePrefix("### "),
                    style = MaterialTheme.typography.titleSmall,
                    color = color,
                    topPad = 6.dp
                )
                line.startsWith("## ") -> HeadingText(
                    text = line.removePrefix("## "),
                    style = MaterialTheme.typography.titleMedium,
                    color = color,
                    topPad = 8.dp
                )
                line.startsWith("# ") -> HeadingText(
                    text = line.removePrefix("# "),
                    style = MaterialTheme.typography.titleLarge,
                    color = color,
                    topPad = 10.dp
                )
                line.startsWith("- ") || line.startsWith("* ") -> Row(
                    Modifier.padding(vertical = 1.dp)
                ) {
                    Text("• ", style = textStyle, color = color)
                    Text(parseInline(line.substring(2).trim()), style = textStyle, color = color)
                }
                line.matches(Regex("^\\d+\\. .+")) -> {
                    val dot = line.indexOf(". ")
                    Row(Modifier.padding(vertical = 1.dp)) {
                        Text("${line.substring(0, dot + 1)} ", style = textStyle, color = color)
                        Text(parseInline(line.substring(dot + 2)), style = textStyle, color = color)
                    }
                }
                line.isBlank() -> Spacer(Modifier.height(4.dp))
                else -> Text(parseInline(line), style = textStyle, color = color)
            }
        }
        if (inCode && codeLines.isNotEmpty()) {
            CodeBlock(code = codeLines.joinToString("\n"))
        }
    }
}

@Composable
private fun HeadingText(text: String, style: TextStyle, color: Color, topPad: androidx.compose.ui.unit.Dp) {
    Text(
        text = parseInline(text),
        style = style,
        color = color,
        modifier = Modifier.padding(top = topPad, bottom = 2.dp)
    )
}

@Composable
private fun CodeBlock(code: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.small
            )
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = code,
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

fun parseInline(text: String): AnnotatedString = buildAnnotatedString {
    var i = 0
    while (i < text.length) {
        when {
            // Bold+Italic: ***text***
            text.startsWith("***", i) -> {
                val close = text.indexOf("***", i + 3)
                if (close != -1) {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic)) {
                        append(text.substring(i + 3, close))
                    }
                    i = close + 3
                } else { append(text[i]); i++ }
            }
            // Bold: **text**
            text.startsWith("**", i) -> {
                val close = text.indexOf("**", i + 2)
                if (close != -1) {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(text.substring(i + 2, close))
                    }
                    i = close + 2
                } else { append(text[i]); i++ }
            }
            // Italic: *text* — only if content has no leading/trailing space
            text[i] == '*' -> {
                val close = text.indexOf('*', i + 1)
                val content = if (close != -1) text.substring(i + 1, close) else null
                if (content != null && content.isNotEmpty()
                    && !content.startsWith(' ') && !content.endsWith(' ')) {
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(content) }
                    i = close + 1
                } else { append(text[i]); i++ }
            }
            // Inline code: `code`
            text[i] == '`' -> {
                val close = text.indexOf('`', i + 1)
                if (close != -1) {
                    withStyle(SpanStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Medium)) {
                        append(text.substring(i + 1, close))
                    }
                    i = close + 1
                } else { append(text[i]); i++ }
            }
            else -> { append(text[i]); i++ }
        }
    }
}
