package com.jarrlyyy.guessthenumber.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarkdownViewerScreen(
    assetFileName: String,
    title: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var markdownText by remember { mutableStateOf("") }

    LaunchedEffect(assetFileName) {
        markdownText = try {
            context.assets.open(assetFileName).bufferedReader().use { it.readText() }
        } catch (_: Exception) {
            "Failed to load $assetFileName"
        }
    }

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
            val lines = markdownText.lines()
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(lines.size) { index ->
                    val line = lines[index]
                    val trimmed = line.trim()
                    when {
                        trimmed.startsWith("# ") -> {
                            Text(
                                text = trimmed.removePrefix("# "),
                                style = MaterialTheme.typography.titleLarge,
                                fontSize = 20.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        trimmed.startsWith("## ") -> {
                            Text(
                                text = trimmed.removePrefix("## "),
                                style = MaterialTheme.typography.titleMedium,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        trimmed.startsWith("### ") -> {
                            Text(
                                text = trimmed.removePrefix("### "),
                                style = MaterialTheme.typography.titleSmall,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                        trimmed.startsWith("- ") || trimmed.startsWith("* ") -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text("•", fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                                Text(
                                    text = trimmed.removePrefix("- ").removePrefix("* "),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontSize = 14.sp
                                )
                            }
                        }
                        trimmed.isBlank() -> {
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                        else -> {
                            Text(
                                text = trimmed,
                                style = MaterialTheme.typography.bodyMedium,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
