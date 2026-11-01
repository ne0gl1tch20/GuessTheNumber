package com.jarrlyyy.guessthenumber.desktop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jarrlyyy.guessthenumber.desktop.persistence.DesktopSaveStore
import com.jarrlyyy.guessthenumber.desktop.persistence.defaultDesktopSaveDirectory
import com.jarrlyyy.guessthenumber.domain.model.GameState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.lang.management.ManagementFactory
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Composable
internal fun DesktopDeveloperConsoleScreen(onBack: () -> Unit) {
    val store = remember { DesktopSaveStore(defaultDesktopSaveDirectory()) }
    val executor = remember { DesktopDeveloperCommandExecutor() }
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf<GameState?>(null) }
    var activeSlot by remember { mutableStateOf(1) }
    var command by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var output by remember { mutableStateOf(listOf("Windows developer tools · allowlisted commands only · no shell or arbitrary code execution.")) }
    var showStateJson by remember { mutableStateOf(false) }
    val json = remember { Json { prettyPrint = true; encodeDefaults = true } }
    val stateJson = remember(state) { state?.let { json.encodeToString(it) }.orEmpty() }
    val runtime = remember { ManagementFactory.getRuntimeMXBean() }
    val memory = remember { ManagementFactory.getMemoryMXBean() }

    LaunchedEffect(Unit) {
        val loaded = withContext(Dispatchers.IO) {
            val slot = store.activeSlot()
            slot to store.load(slot)
        }
        activeSlot = loaded.first
        state = loaded.second
        isLoading = false
    }

    fun append(line: String) {
        val timestamp = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
        output = (output + "[$timestamp] $line").takeLast(50)
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onBack) { Text("Close") }
            Text("Advanced Developer Tools", style = MaterialTheme.typography.headlineSmall)
        }
        Text("Windows diagnostics, save inspection and the safe developer command console · active save slot $activeSlot",
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("Runtime diagnostics", style = MaterialTheme.typography.titleMedium)
                Text("OS: ${System.getProperty("os.name")} ${System.getProperty("os.version")} · ${System.getProperty("os.arch")}")
                Text("Java: ${System.getProperty("java.version")} · CPU threads: ${Runtime.getRuntime().availableProcessors()}")
                Text("Uptime: ${runtime.uptime / 1000}s · Heap: ${memory.heapMemoryUsage.used / (1024 * 1024)} MB / ${memory.heapMemoryUsage.max / (1024 * 1024)} MB")
                Text("Save directory: ${defaultDesktopSaveDirectory()}", style = MaterialTheme.typography.bodySmall)
            }
        }
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Quick commands", style = MaterialTheme.typography.titleMedium)
                Text("Choose a safe command shortcut.", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("/help", "/version", "/state", "/stats").forEach { quick ->
                        OutlinedButton(onClick = { command = quick }) { Text(quick) }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(onClick = { command = "/newnumber" }) { Text("New target") }
                    OutlinedButton(onClick = { command = "/autoclicker on" }) { Text("Enable bot") }
                    OutlinedButton(onClick = { command = "/autoclicker off" }) { Text("Disable bot") }
                }
            }
        }
        if (isLoading) {
            Text("Loading active save…")
        } else if (state == null) {
            Text("No active save exists. Create a save before running state commands.", color = MaterialTheme.colorScheme.error)
        } else {
            OutlinedTextField(
                value = command,
                onValueChange = { command = it.take(256) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Command") },
                placeholder = { Text("/help") },
                singleLine = true,
                enabled = !isSaving
            )
            Button(
                onClick = {
                    val current = state ?: return@Button
                    val submitted = command.trim()
                    if (submitted.isNotEmpty()) {
                        append("> $submitted")
                        val result = executor.execute(submitted, current)
                        val updated = result.updatedState
                        if (updated == null) {
                            append(result.message)
                        } else {
                            isSaving = true
                            scope.launch {
                                val saved = withContext(Dispatchers.IO) {
                                    store.save(activeSlot, updated.copy(lastSaveTimestamp = System.currentTimeMillis()))
                                }
                                if (saved) {
                                    state = updated
                                    append(result.message)
                                } else {
                                    append("Save failed. The previous state remains active.")
                                }
                                isSaving = false
                            }
                        }
                        command = ""
                    }
                },
                enabled = command.isNotBlank() && !isSaving && state != null
            ) { Text(if (isSaving) "Saving…" else "Run command") }
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Save data inspector", style = MaterialTheme.typography.titleMedium)
                        androidx.compose.material3.TextButton(onClick = { showStateJson = !showStateJson }) {
                            Text(if (showStateJson) "Hide JSON" else "Inspect JSON")
                        }
                    }
                    Text("Read-only serialized GameState. This view does not bypass save validation.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (showStateJson) {
                        OutlinedTextField(value = stateJson, onValueChange = {}, readOnly = true,
                            modifier = Modifier.fillMaxWidth(), minLines = 8, maxLines = 18)
                        OutlinedButton(onClick = {
                            runCatching {
                                java.awt.Toolkit.getDefaultToolkit().systemClipboard.setContents(
                                    java.awt.datatransfer.StringSelection(stateJson), null
                                )
                            }.onSuccess { append("Copied active save JSON to clipboard.") }
                                .onFailure { append("Clipboard access is unavailable: ${it.message}") }
                        }) { Text("Copy save JSON") }
                    }
                }
            }
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Output history", style = MaterialTheme.typography.titleMedium)
                    output.forEach { line ->
                        Text(line, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { output = emptyList() }, enabled = !isSaving) { Text("Clear output") }
                OutlinedButton(onClick = {
                    scope.launch {
                        val loaded = withContext(Dispatchers.IO) { store.load(activeSlot) }
                        if (loaded != null) {
                            state = loaded
                            append("Reloaded slot $activeSlot from disk.")
                        } else append("The active save could not be loaded.")
                    }
                }, enabled = !isSaving) { Text("Reload active save") }
            }
        }
    }
}
