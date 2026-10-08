package com.jarrlyyy.guessthenumber.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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
fun parseMarkdownInline(text: String): AnnotatedString {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface

    return remember(text, primaryColor, secondaryColor, onSurfaceColor) {
        try {
            buildAnnotatedString {
            var index = 0

            fun appendStyled(delimiter: String, style: SpanStyle, transform: (String) -> String = { it }): Boolean {
                if (!text.startsWith(delimiter, index)) return false
                val contentStart = index + delimiter.length
                val closingIndex = text.indexOf(delimiter, contentStart)
                if (closingIndex < 0) return false

                withStyle(style) {
                    append(transform(text.substring(contentStart, closingIndex)))
                }
                index = closingIndex + delimiter.length
                return true
            }

            while (index < text.length) {
                val parsed = when {
                    appendStyled(
                        "**",
                        SpanStyle(fontWeight = FontWeight.Bold, color = onSurfaceColor)
                    ) -> true

                    appendStyled(
                        "__",
                        SpanStyle(fontWeight = FontWeight.Bold, color = onSurfaceColor)
                    ) -> true

                    appendStyled(
                        "*",
                        SpanStyle(fontStyle = FontStyle.Italic, color = onSurfaceColor)
                    ) -> true

                    appendStyled(
                        "_",
                        SpanStyle(fontStyle = FontStyle.Italic, color = onSurfaceColor)
                    ) -> true

                    appendStyled(
                        "`",
                        SpanStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp, color = primaryColor)
                    ) -> true

                    (appendStyled(
                        "$",
                        SpanStyle(fontStyle = FontStyle.Italic, fontWeight = FontWeight.Medium, color = secondaryColor)
                    ) { math ->
                        math
                            .replace("\\times", "×")
                            .replace("\\cdot", "·")
                            .replace("\\to", "→")
                            .replace("\\div", "÷")
                            .replace("\\pm", "±")
                            .replace("\\leq", "≤")
                            .replace("\\geq", "≥")
                    }) -> true

                    else -> {
                        append(text[index])
                        index++
                        true
                    }
                }
                if (!parsed) index++
            }
            }
        } catch (_: RuntimeException) {
            buildAnnotatedString { append(text) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarkdownViewerScreen(
    assetFileName: String,
    title: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var markdownText by remember(assetFileName) { mutableStateOf("") }
    var loadError by remember(assetFileName) { mutableStateOf(false) }

    LaunchedEffect(assetFileName) {
        loadError = false
        markdownText = try {
            context.assets.open(assetFileName).bufferedReader().use { it.readText() }
        } catch (_: Exception) {
            loadError = true
            ""
        }
    }

    val lines = remember(markdownText) { markdownText.lines() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(lines.size) { index ->
                    val line = lines[index]
                    val trimmed = line.trim()
                    when {
                        trimmed.startsWith("# ") -> {
                            Text(
                                text = parseMarkdownInline(trimmed.removePrefix("# ")),
                                style = MaterialTheme.typography.titleLarge,
                                fontSize = 22.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        trimmed.startsWith("## ") -> {
                            Text(
                                text = parseMarkdownInline(trimmed.removePrefix("## ")),
                                style = MaterialTheme.typography.titleMedium,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        trimmed.startsWith("### ") -> {
                            Text(
                                text = parseMarkdownInline(trimmed.removePrefix("### ")),
                                style = MaterialTheme.typography.titleSmall,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                        trimmed.startsWith("- ") || trimmed.startsWith("* ") -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("•", fontSize = 14.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                Text(
                                    text = parseMarkdownInline(trimmed.removePrefix("- ").removePrefix("* ")),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        trimmed.isBlank() -> {
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                        else -> {
                            Text(
                                text = parseMarkdownInline(trimmed),
                                style = MaterialTheme.typography.bodyMedium,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        
                    }
                }
                }
            }
    }
}