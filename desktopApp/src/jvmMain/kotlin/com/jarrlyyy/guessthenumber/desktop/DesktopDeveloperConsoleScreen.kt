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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.text.font.FontFamily
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

@Composable
internal fun DesktopDeveloperConsoleScreen(onBack: () -> Unit, onOpenProcessInspector: () -> Unit = {}) {
    val store = remember { DesktopSaveStore(defaultDesktopSaveDirectory()) }
    val executor = remember { DesktopDeveloperCommandExecutor() }
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf<GameState?>(null) }
    var activeSlot by remember { mutableStateOf(1) }
    var command by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var output by remember { mutableStateOf(listOf("Windows developer console · typed commands only · no shell or arbitrary code execution.")) }
    var saveJson by remember { mutableStateOf("") }
    var jsonStatus by remember { mutableStateOf("Load the active slot to inspect its validated JSON.") }
    var processSnapshot by remember { mutableStateOf(DesktopProcessDiagnostics.capture()) }

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
            Text("Developer Console", style = MaterialTheme.typography.headlineSmall)
        }
        Text("Debug-only command allowlist · active save slot $activeSlot", color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedButton(onClick = onOpenProcessInspector) { Text("Open Developer Playground & Process Inspector") }
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Active Save Overview", style = MaterialTheme.typography.titleLarge)
                state?.let { current ->
                    Text("${current.profileName.ifBlank { "Slot $activeSlot" }} · ${current.difficultyId} · ${if (current.creativeModeEnabled) "Creative Mode" else "Standard"}")
                    Text("Range: ${current.currentRangeMin}–${current.currentRangeMax} · Attempts: ${current.attempts} · Correct: ${current.correctGuesses}")
                    Text("Streak: ${current.streak} · Best: ${current.bestStreak} · Upgrades: ${current.upgradeLevels.size} · Traits: ${current.discoveredTraitIds.size}")
                    Text("Money: ${current.money} · Prestige: ${current.prestige} · Ultra: ${current.ultra} · Nebula: ${current.nebula}")
                } ?: Text(if (isLoading) "Loading…" else "No valid active save.", color = MaterialTheme.colorScheme.error)
            }
        }
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Quick Commands", style = MaterialTheme.typography.titleLarge)
                listOf("/help", "/version", "/state", "/stats", "/newnumber", "/autoclicker on", "/autoclicker off").forEach { quick ->
                    OutlinedButton(onClick = { command = quick }, enabled = !isSaving) { Text(quick) }
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
                Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Output history", style = MaterialTheme.typography.titleMedium)
                    output.forEach { line ->
                        Text(line, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            OutlinedButton(onClick = { output = emptyList() }, enabled = !isSaving) { Text("Clear output") }
        }
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Save JSON Inspector", style = MaterialTheme.typography.titleLarge)
                Text("Inspect or apply the active slot JSON through the desktop save validator. Rejected imports leave the current save untouched.")
                OutlinedButton(onClick = {
                    scope.launch {
                        jsonStatus = "Loading active save…"
                        val raw = withContext(Dispatchers.IO) { store.exportSlotJson(activeSlot) }
                        if (raw == null) jsonStatus = "No valid JSON for slot $activeSlot."
                        else { saveJson = raw; jsonStatus = "Loaded slot $activeSlot." }
                    }
                }, enabled = !isLoading && !isSaving) { Text("Load active save JSON") }
                OutlinedTextField(
                    value = saveJson,
                    onValueChange = { saveJson = it.take(1_000_000) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Save JSON") },
                    minLines = 7,
                    maxLines = 14,
                    textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        scope.launch {
                            isSaving = true
                            val imported = withContext(Dispatchers.IO) { store.importSlotJson(activeSlot, saveJson) }
                            if (imported) {
                                state = withContext(Dispatchers.IO) { store.load(activeSlot) }
                                jsonStatus = "Validated and saved slot $activeSlot."
                                append("Applied validated JSON to slot $activeSlot.")
                            } else {
                                jsonStatus = "Import rejected. Existing save was left intact."
                            }
                            isSaving = false
                        }
                    }, enabled = saveJson.isNotBlank() && !isSaving) { Text("Validate & Apply") }
                    OutlinedButton(onClick = { saveJson = ""; jsonStatus = "Editor cleared." }, enabled = !isSaving) { Text("Clear") }
                }
                Text(jsonStatus, style = MaterialTheme.typography.bodySmall)
            }
        }
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("WebPlay Asset Diagnostics", style = MaterialTheme.typography.titleLarge)
                Text("Active validated cache: ${DesktopWebPlayAssetSyncer.activeVersion() ?: "Bundled assets"}")
                Text("The desktop sync checks the manifest at startup, validates SHA-256 hashes and activates a complete version on the next launch.")
            }
        }
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Runtime & Memory Diagnostics", style = MaterialTheme.typography.titleLarge)
                OutlinedButton(onClick = { processSnapshot = DesktopProcessDiagnostics.capture() }) { Text("Refresh diagnostics") }
                Text("OS: ${processSnapshot.osName} ${processSnapshot.osVersion}")
                Text("Architecture: ${processSnapshot.architecture} · JVM: ${processSnapshot.javaVersion}")
                Text("Processors: ${processSnapshot.availableProcessors} · Uptime: ${processSnapshot.uptimeMillis / 1000}s")
                Text("Heap: ${processSnapshot.heapUsedBytes / (1024 * 1024)} MB used / ${processSnapshot.heapMaxBytes / (1024 * 1024)} MB max")
                Text("Non-heap: ${processSnapshot.nonHeapUsedBytes / (1024 * 1024)} MB")
            }
        }
    }
}
