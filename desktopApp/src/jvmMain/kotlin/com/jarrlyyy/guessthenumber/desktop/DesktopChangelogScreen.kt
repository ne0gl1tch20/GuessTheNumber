package com.jarrlyyy.guessthenumber.desktop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal fun loadDesktopChangelog(): Result<String> = runCatching {
    val stream = DesktopChangelogScreen::class.java.classLoader.getResourceAsStream("changelogs.md")
        ?: error("The bundled changelog could not be found.")
    stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
}

@Composable
internal fun DesktopChangelogScreen(onBack: () -> Unit) {
    var content by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val result = withContext(Dispatchers.IO) { loadDesktopChangelog() }
        result.onSuccess { content = it }.onFailure {
            errorMessage = it.message ?: "Could not load the changelog."
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onBack) { Text("Back to More") }
            Text("Changelog", style = MaterialTheme.typography.headlineMedium)
        }
        when {
            content == null && errorMessage == null -> Text("Loading changelog…", style = MaterialTheme.typography.bodyMedium)
            errorMessage != null -> Card(modifier = Modifier.fillMaxWidth()) {
                Text(errorMessage.orEmpty(), modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.error)
            }
            else -> content.orEmpty().lines().forEach { rawLine ->
                val line = rawLine.trimEnd()
                val trimmed = line.trim()
                when {
                    trimmed.isBlank() -> Spacer(modifier = Modifier.height(2.dp))
                    trimmed.startsWith("### ") -> Text(trimmed.removePrefix("### "), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.tertiary)
                    trimmed.startsWith("## ") -> Text(trimmed.removePrefix("## "), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                    trimmed.startsWith("# ") -> Text(trimmed.removePrefix("# "), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                    trimmed.startsWith("- ") || trimmed.startsWith("* ") -> Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("•", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        Text(inlineMarkdown(trimmed.drop(2)), style = MaterialTheme.typography.bodyMedium)
                    }
                    else -> Text(inlineMarkdown(trimmed), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

private fun inlineMarkdown(source: String) = buildAnnotatedString {
    var index = 0
    while (index < source.length) {
        when {
            source.startsWith("**", index) -> {
                val end = source.indexOf("**", index + 2)
                if (end > index + 2) {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(source.substring(index + 2, end)) }
                    index = end + 2
                } else { append(source[index]); index++ }
            }
            source[index] == '`' -> {
                val end = source.indexOf('`', index + 1)
                if (end > index + 1) {
                    withStyle(SpanStyle(fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)) { append(source.substring(index + 1, end)) }
                    index = end + 1
                } else { append(source[index]); index++ }
            }
            else -> { append(source[index]); index++ }
        }
    }
}
