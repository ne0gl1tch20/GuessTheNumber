package com.jarrlyyy.guessthenumber.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.jarrlyyy.guessthenumber.BuildConfig
import com.jarrlyyy.guessthenumber.scripting.LuaEngine
import com.jarrlyyy.guessthenumber.scripting.LuaScriptEntry
import com.jarrlyyy.guessthenumber.scripting.LuaStartResult
import com.jarrlyyy.guessthenumber.ui.localization.LocalAppLocaleManager
import com.jarrlyyy.guessthenumber.ui.localization.localizedText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LuaTestingHubScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val locale = LocalAppLocaleManager.current
    val engine = remember(context) { LuaEngine.get(context) }
    val scope = rememberCoroutineScope()
    var scripts by remember { mutableStateOf<List<LuaScriptEntry>>(emptyList()) }
    var status by remember { mutableStateOf(engine.status()) }
    var output by remember { mutableStateOf("") }
    var running by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        scripts = withContext(Dispatchers.IO) { engine.listScripts() }
        status = engine.status()
    }
    DisposableEffect(Unit) {
        onDispose { engine.stop() }
    }

    if (!BuildConfig.DEBUG) {
        LaunchedEffect(Unit) { onBack() }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(localizedText("Lua Testing Hub")) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = locale.getString("back", "Back"))
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(localizedText("Runtime status"), style = MaterialTheme.typography.titleMedium)
                        Text(
                            if (status.runtimeAvailable) localizedText("Ready") else localizedText("Unavailable"),
                            color = if (status.runtimeAvailable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                        Text(localizedText("Host API version: ${status.hostApiVersion}"))
                        Text(localizedText("Active instance: ${status.activeInstanceId ?: "none"}"))
                        status.lastError?.let { Text(localizedText("Last error: $it"), color = MaterialTheme.colorScheme.error) }
                        Text(
                            localizedText("This developer-only screen runs registered scripts through the same restricted Lua engine as Activity Hub. Raw Lua evaluation is disabled."),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            scripts = engine.listScripts()
                            status = engine.status()
                            output = localizedText("Script list and runtime status refreshed.")
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text(localizedText("Refresh status")) }
                    OutlinedButton(
                        onClick = {
                            val stopped = engine.stop()
                            status = engine.status()
                            output = if (stopped) localizedText("Active Lua activity stopped.") else localizedText("No Lua activity was running.")
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(localizedText("Stop"))
                    }
                }
            }
            if (output.isNotBlank()) {
                item {
                    Card(Modifier.fillMaxWidth()) {
                        Text(output, Modifier.padding(14.dp), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            item {
                Text(localizedText("Registered scripts (${scripts.size})"), style = MaterialTheme.typography.titleLarge)
            }
            items(scripts, key = { it.id }) { script ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(script.title, style = MaterialTheme.typography.titleMedium)
                        Text(script.description, style = MaterialTheme.typography.bodyMedium)
                        Text(localizedText("ID: ${script.id}"))
                        Text(localizedText("Source: ${script.source} · Host API v${script.hostApiVersion}"), style = MaterialTheme.typography.labelSmall)
                        Button(
                            onClick = {
                                if (running) return@Button
                                running = true
                                scope.launch {
                                    val result = withContext(Dispatchers.Default) { engine.start(script.id) }
                                    output = when (result) {
                                        is LuaStartResult.Started -> localizedText("Started ${result.activity.title} (instance ${result.activity.instanceId}).")
                                        is LuaStartResult.Rejected -> localizedText("Script rejected: ${result.reason}")
                                    }
                                    status = engine.status()
                                    running = false
                                }
                            },
                            enabled = !running && status.activeInstanceId == null,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(localizedText("Test script"))
                        }
                    }
                }
            }
        }
    }
}
