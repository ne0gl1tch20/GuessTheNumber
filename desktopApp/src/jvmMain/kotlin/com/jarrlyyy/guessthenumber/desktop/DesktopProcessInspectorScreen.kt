package com.jarrlyyy.guessthenumber.desktop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
    var snapshot by remember { mutableStateOf(DesktopProcessDiagnostics.capture()) }
    val formatter = remember { DecimalFormat("#,##0.0") }
    fun megabytes(bytes: Long): String = "${formatter.format(bytes / (1024.0 * 1024.0))} MB"

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onBack) { Text("Back to More") }
            Text("Process Inspector", style = MaterialTheme.typography.headlineSmall)
        }
        Text(
            "Local JVM and memory diagnostics only. This screen does not enumerate other processes, collect usernames, inspect command-line arguments or transmit telemetry.",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedButton(onClick = { snapshot = DesktopProcessDiagnostics.capture() }) { Text("Refresh diagnostics") }
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Runtime", style = MaterialTheme.typography.titleMedium)
                Text("Operating system: ${snapshot.osName} ${snapshot.osVersion}")
                Text("Architecture: ${snapshot.architecture}")
                Text("Java runtime: ${snapshot.javaVersion}")
                Text("Available processors: ${snapshot.availableProcessors}")
                Text("Application uptime: ${snapshot.uptimeMillis / 1000}s")
            }
        }
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Memory", style = MaterialTheme.typography.titleMedium)
                Text("Heap used: ${megabytes(snapshot.heapUsedBytes)}")
                Text("Heap committed: ${megabytes(snapshot.heapCommittedBytes)}")
                Text("Heap maximum: ${if (snapshot.heapMaxBytes > 0) megabytes(snapshot.heapMaxBytes) else "Not reported"}")
                Text("Non-heap used: ${megabytes(snapshot.nonHeapUsedBytes)}")
            }
        }
    }
}
