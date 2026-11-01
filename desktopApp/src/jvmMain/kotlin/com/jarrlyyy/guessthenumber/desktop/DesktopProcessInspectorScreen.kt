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
import com.jarrlyyy.guessthenumber.desktop.persistence.DesktopSaveSlotSummary
import com.jarrlyyy.guessthenumber.desktop.persistence.defaultDesktopSaveDirectory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.management.ManagementFactory
import java.text.DecimalFormat

internal data class DesktopProcessSnapshot(
    val osName: String,
    val osVersion: String,
    val architecture: String,
    val javaVersion: String,
    val availableProcessors: Int,
    val uptimeMillis: Long,
    val heapUsedBytes: Long,
    val heapCommittedBytes: Long,
    val heapMaxBytes: Long,
    val nonHeapUsedBytes: Long
)

internal object DesktopProcessDiagnostics {
    fun capture(): DesktopProcessSnapshot {
        val runtime = ManagementFactory.getRuntimeMXBean()
        val memory = ManagementFactory.getMemoryMXBean()
        val heap = memory.heapMemoryUsage
        val nonHeap = memory.nonHeapMemoryUsage
        return DesktopProcessSnapshot(
            osName = System.getProperty("os.name").take(120),
            osVersion = System.getProperty("os.version").take(80),
            architecture = System.getProperty("os.arch").take(40),
            javaVersion = System.getProperty("java.version").take(40),
            availableProcessors = Runtime.getRuntime().availableProcessors().coerceAtLeast(1),
            uptimeMillis = runtime.uptime.coerceAtLeast(0L),
            heapUsedBytes = heap.used.coerceAtLeast(0L),
            heapCommittedBytes = heap.committed.coerceAtLeast(0L),
            heapMaxBytes = heap.max.coerceAtLeast(0L),
            nonHeapUsedBytes = nonHeap.used.coerceAtLeast(0L)
        )
    }
}

@Composable
internal fun DesktopProcessInspectorScreen(onBack: () -> Unit) {
    val store = remember { DesktopSaveStore(defaultDesktopSaveDirectory()) }
    val scope = rememberCoroutineScope()
    var snapshot by remember { mutableStateOf(DesktopProcessDiagnostics.capture()) }
    var activeSlot by remember { mutableStateOf(1) }
    var slotSummaries by remember { mutableStateOf<List<DesktopSaveSlotSummary>>(emptyList()) }
    var saveCheck by remember { mutableStateOf("Run the save check to inspect the active slot and recovery backup.") }
    var isChecking by remember { mutableStateOf(false) }
    var uiCounter by remember { mutableStateOf(0) }
    val formatter = remember { DecimalFormat("#,##0.0") }
    fun megabytes(bytes: Long): String = "${formatter.format(bytes / (1024.0 * 1024.0))} MB"

    LaunchedEffect(Unit) {
        activeSlot = withContext(Dispatchers.IO) { store.activeSlot() }
        slotSummaries = withContext(Dispatchers.IO) { store.slotSummaries() }
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onBack) { Text("Back to Developer Settings") }
            Text("Developer Playground", style = MaterialTheme.typography.headlineSmall)
        }
        Text(
            "Windows counterpart to Android's Developer & Testing Playground. Each panel uses real desktop runtime, save and WebPlay information.",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Core Process", style = MaterialTheme.typography.titleLarge)
                Text("Active save slot: $activeSlot")
                val active = slotSummaries.firstOrNull { it.slot == activeSlot }
                Text(
                    active?.state?.let { "Profile: ${it.profileName.ifBlank { "Slot $activeSlot" }} · Difficulty: ${it.difficultyId} · Range: ${it.currentRangeMin}–${it.currentRangeMax}" }
                        ?: if (active?.isCorrupt == true) "Active save is corrupt and needs recovery." else "No active save is available."
                )
                active?.state?.let {
                    Text("Attempts: ${it.attempts} · Correct: ${it.correctGuesses} · Streak: ${it.streak}")
                    Text("Money: ${it.money} · Prestige: ${it.prestige} · Ultra: ${it.ultra} · Nebula: ${it.nebula}")
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Web Process", style = MaterialTheme.typography.titleLarge)
                Text("WebPlay is the HTML/CSS/JavaScript player interface. Kotlin remains authoritative for game state, save data and validation.")
                Text("Active validated asset cache: ${DesktopWebPlayAssetSyncer.activeVersion() ?: "Bundled assets"}")
                Text("Updates are checked in the background and staged for the next launch after manifest and SHA-256 validation.")
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Save Lab & Recovery Checks", style = MaterialTheme.typography.titleLarge)
                Text("Check the active save, all ten slots and available recovery backups.")
                Button(
                    onClick = {
                        scope.launch {
                            isChecking = true
                            val latest = withContext(Dispatchers.IO) { store.slotSummaries() }
                            slotSummaries = latest
                            val active = latest.firstOrNull { it.slot == activeSlot }
                            val populated = latest.count { !it.isEmpty && !it.isCorrupt }
                            val corrupt = latest.filter { it.isCorrupt }.map { it.slot }
                            val backups = latest.filter { it.hasBackup }.map { it.slot }
                            saveCheck = buildString {
                                append("Checked ${latest.size} slots · $populated valid populated saves.")
                                append(if (active == null || active.isEmpty) " Active slot is empty." else if (active.isCorrupt) " Active slot is corrupt." else " Active slot is valid.")
                                append(if (backups.isEmpty()) " No recovery backups found." else " Recovery backups: ${backups.joinToString()}.")
                                append(if (corrupt.isEmpty()) " No corrupt slots found." else " Corrupt slots: ${corrupt.joinToString()}.")
                            }
                            isChecking = false
                        }
                    },
                    enabled = !isChecking
                ) { Text(if (isChecking) "Checking…" else "Check saves & backups") }
                Text(saveCheck, style = MaterialTheme.typography.bodySmall)
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("UI Component Playground", style = MaterialTheme.typography.titleLarge)
                Text("A small desktop interaction check for Compose controls, click handling and state updates.")
                Text("Button interaction count: $uiCounter")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { uiCounter++ }) { Text("Test button") }
                    OutlinedButton(onClick = { uiCounter = 0 }) { Text("Reset counter") }
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Runtime Diagnostics", style = MaterialTheme.typography.titleLarge)
                Text("Local JVM and memory diagnostics only. This does not enumerate other processes or transmit telemetry.")
                OutlinedButton(onClick = { snapshot = DesktopProcessDiagnostics.capture() }) { Text("Refresh diagnostics") }
                Text("Operating system: ${snapshot.osName} ${snapshot.osVersion}")
                Text("Architecture: ${snapshot.architecture}")
                Text("Java runtime: ${snapshot.javaVersion}")
                Text("Available processors: ${snapshot.availableProcessors}")
                Text("Application uptime: ${snapshot.uptimeMillis / 1000}s")
                Text("Heap used: ${megabytes(snapshot.heapUsedBytes)}")
                Text("Heap committed: ${megabytes(snapshot.heapCommittedBytes)}")
                Text("Heap maximum: ${if (snapshot.heapMaxBytes > 0) megabytes(snapshot.heapMaxBytes) else "Not reported"}")
                Text("Non-heap used: ${megabytes(snapshot.nonHeapUsedBytes)}")
            }
        }
    }
}
