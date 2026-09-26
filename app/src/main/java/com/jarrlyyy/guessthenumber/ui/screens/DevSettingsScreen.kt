package com.jarrlyyy.guessthenumber.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarrlyyy.guessthenumber.data.logger.GameLogger
import com.jarrlyyy.guessthenumber.data.logger.LogLevel
import com.jarrlyyy.guessthenumber.data.logger.LoggerCategory
import com.jarrlyyy.guessthenumber.domain.model.GameSettings
import com.jarrlyyy.guessthenumber.domain.model.GameState
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevSettingsScreen(
    state: GameState,
    onExecuteCommand: (String) -> String,
    onImportSave: (String, (Boolean) -> Unit) -> Unit,
    onUpdateSettings: (GameSettings) -> Unit,
    onNavigate: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var commandInput by remember { mutableStateOf("") }
    var saveJsonEditorInput by remember { mutableStateOf("") }
    var logSearchQuery by remember { mutableStateOf("") }
    var isPaused by remember { mutableStateOf(false) }
    
    val logs by GameLogger.logFlow.collectAsState(initial = emptyList())
    val jsonSerializer = remember { Json { ignoreUnknownKeys = true; prettyPrint = true } }

    val allCommands = listOf(
        "/help",
        "/give money ",
        "/give prestige ",
        "/give ultra ",
        "/give nebula ",
        "/give all ",
        "/set money ",
        "/set prestige ",
        "/set ultra ",
        "/set nebula ",
        "/reset progression",
        "/reset prestige",
        "/reset ultra",
        "/reset all",
        "/timeskip ",
        "/max_upgrades",
        "/unlock_all",
        "/win",
        "/speed ",
        "/stats",
        "/matrix",
        "/konami",
        "/moneyprinter",
        "/easteregg",
        "/crash_test"
    )

    val suggestions = if (commandInput.isNotBlank()) {
        allCommands.filter { it.startsWith(commandInput, ignoreCase = true) && it != commandInput }
    } else {
        emptyList()
    }

    val filteredLogs = logs.filter {
        logSearchQuery.isBlank() ||
        it.message.contains(logSearchQuery, ignoreCase = true) ||
        it.category.name.contains(logSearchQuery, ignoreCase = true) ||
        it.level.name.contains(logSearchQuery, ignoreCase = true)
    }.reversed()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Developer Settings & Console") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section 0: Process Inspector Navigation Button
            item {
                Button(
                    onClick = { onNavigate("process_inspector") },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.Info, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Open Variable & Process Inspector", fontSize = 16.sp)
                }
            }

            // Section 0.5: Log Storage Setting
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Save non-crash logs to storage", style = MaterialTheme.typography.titleMedium, fontSize = 16.sp)
                        Text("Saves game logs to Android/data files directory", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = state.settings.saveLogsToStorage,
                        onCheckedChange = { enabled ->
                            onUpdateSettings(state.settings.copy(saveLogsToStorage = enabled))
                        }
                    )
                }
            }

            // Section 1: Developer Commands & Autocomplete
            item {
                HorizontalDivider()
                Spacer(modifier = Modifier.height(4.dp))
                Text("Developer Commands", style = MaterialTheme.typography.titleMedium, fontSize = 18.sp)
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = commandInput,
                            onValueChange = { commandInput = it },
                            label = { Text("Command (e.g. /help)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        Button(onClick = {
                            if (commandInput.isNotBlank()) {
                                val res = onExecuteCommand(commandInput)
                                GameLogger.log(LogLevel.INFO, LoggerCategory.COMMAND, "DEV_CMD", "Ran '$commandInput': $res")
                                commandInput = ""
                            }
                        }) {
                            Text("Run")
                        }
                    }

                    // Autocomplete Suggestions Row
                    if (suggestions.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            suggestions.forEach { suggestion ->
                                AssistChip(
                                    onClick = { commandInput = suggestion },
                                    label = { Text(suggestion) }
                                )
                            }
                        }
                    }
                }
            }

            // Section 2: Save Editor (JSON Text Editing)
            item {
                HorizontalDivider()
                Spacer(modifier = Modifier.height(4.dp))
                Text("Save Editor (JSON Editing)", style = MaterialTheme.typography.titleMedium, fontSize = 18.sp)
            }
            item {
                OutlinedTextField(
                    value = saveJsonEditorInput,
                    onValueChange = { saveJsonEditorInput = it },
                    label = { Text("Raw GameState JSON") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                )
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            saveJsonEditorInput = jsonSerializer.encodeToString(state)
                            Toast.makeText(context, "Loaded current state into editor", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Load Current Save", fontSize = 12.sp)
                    }
                    Button(
                        onClick = {
                            if (saveJsonEditorInput.isNotBlank()) {
                                onImportSave(saveJsonEditorInput) { success ->
                                    if (success) {
                                        Toast.makeText(context, "Save applied successfully!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Failed to apply invalid save JSON!", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } else {
                                Toast.makeText(context, "Failed to apply invalid save json", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Apply Save JSON", fontSize = 12.sp)
                    }
                }
            }

            // Section 3: Live Game Logs Console (Unified Log View)
            item {
                HorizontalDivider()
                Spacer(modifier = Modifier.height(4.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Live Console Logs", style = MaterialTheme.typography.titleMedium, fontSize = 18.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    if (isPaused) {
                                        GameLogger.resume()
                                        isPaused = false
                                    } else {
                                        GameLogger.pause()
                                        isPaused = true
                                    }
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                    contentDescription = if (isPaused) "Resume" else "Pause",
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            IconButton(
                                onClick = { GameLogger.clear() },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Clear",
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            IconButton(
                                onClick = {
                                    val exported = GameLogger.exportLogs()
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Game Logs", exported))
                                    Toast.makeText(context, "Logs copied to clipboard", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy",
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = logSearchQuery,
                        onValueChange = { logSearchQuery = it },
                        label = { Text("Filter logs (category, level, message)...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }

            // Unified Terminal Console View with Independent Smooth Scrolling & Color Coding
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(350.dp),
                    color = Color(0xFF1E1E1E),
                    shape = MaterialTheme.shapes.medium
                ) {
                    if (filteredLogs.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No logs recorded yet...",
                                color = Color(0xFF888888),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(filteredLogs.take(200)) { log ->
                                val timeStr = GameLogger.formatTimestamp(log.timestamp)
                                val color = when (log.level) {
                                    LogLevel.ERROR, LogLevel.FATAL -> Color(0xFFFF5555)
                                    LogLevel.WARN -> Color(0xFFFFB86C)
                                    LogLevel.INFO -> Color(0xFF50FA7B)
                                    else -> Color(0xFF8BE9FD)
                                }
                                Text(
                                    text = "[$timeStr][${log.category}][${log.level}] ${log.message}",
                                    color = color,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
