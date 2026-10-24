package com.jarrlyyy.guessthenumber.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.jarrlyyy.guessthenumber.data.logger.GameLogger
import com.jarrlyyy.guessthenumber.data.logger.LogLevel
import com.jarrlyyy.guessthenumber.data.logger.LoggerCategory
import com.jarrlyyy.guessthenumber.domain.command.CommandAutocomplete
import com.jarrlyyy.guessthenumber.domain.command.CommandRegistry
import com.jarrlyyy.guessthenumber.domain.model.GameState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeveloperPlaygroundScreen(state: GameState, onExecuteCommand: (String) -> String, onTestDailyRewards: () -> Unit, onBack: () -> Unit) {
    var destination by remember { mutableStateOf("hub") }
    Scaffold(topBar = { TopAppBar(title = { Text(if (destination == "hub") "Developer Playground" else destinationLabel(destination)) }, navigationIcon = { IconButton(onClick = { if (destination == "hub") onBack() else destination = "hub" }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } }) }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (destination) {
                "core" -> CoreProcessScreen(state, onExecuteCommand)
                "web" -> WebProcessScreen(onTestDailyRewards)
                "tests" -> DeveloperTestSuitesScreen(state, onExecuteCommand)
                "ui" -> DeveloperUiPlaygroundScreen(onTestDailyRewards)
                "save" -> DeveloperSaveLabScreen(state)
                "diagnostics" -> DeveloperDiagnosticsScreen(state)
                "runtime" -> RuntimeInspectorScreen(state) { destination = "hub" }
                else -> DeveloperHub { destination = it }
            }
        }
    }
}

private fun destinationLabel(destination: String) = when (destination) {
    "core" -> "Core Process"
    "web" -> "Web Process"
    "tests" -> "Test Suites"
    "ui" -> "UI Playground"
    "save" -> "Save Laboratory"
    "runtime" -> "Runtime Inspector"
    else -> "Diagnostics"
}

@Composable
private fun DeveloperHub(onOpen: (String) -> Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Developer & Testing Playground", style = MaterialTheme.typography.headlineSmall) }
        item { Text("Native Core and HTML WebView tooling live on separate screens. Test state, UI, saves, bridge traffic, and diagnostics without player-facing clutter.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        item { DeveloperEntry("Core Process", "Kotlin commands, native logs, state and diagnostics", Icons.Default.Terminal) { onOpen("core") } }
        item { DeveloperEntry("Web Process", "HTML/JS commands, WebView console and bridge events", Icons.Default.Web) { onOpen("web") } }
        item { DeveloperEntry("Test Suites", "Run grouped checks with PASS, FAIL and SKIPPED results", Icons.Default.CheckCircle) { onOpen("tests") } }
        item { DeveloperEntry("UI Playground", "Exercise popups, tutorials, loading and edge-case UI states", Icons.Default.Palette) { onOpen("ui") } }
        item { DeveloperEntry("Save Laboratory", "Exercise save/load, offline earnings and recovery scenarios", Icons.Default.Save) { onOpen("save") } }
        item { DeveloperEntry("Runtime Inspector", "Inspect active loops and authoritative runtime state", Icons.Default.Analytics) { onOpen("runtime") } }
        item { DeveloperEntry("Diagnostics", "Build, state, logging and runtime information", Icons.Default.Info) { onOpen("diagnostics") } }
    }
}

@Composable
private fun DeveloperEntry(title: String, description: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) { Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) { Icon(icon, null, tint = MaterialTheme.colorScheme.primary); Column { Text(title, style = MaterialTheme.typography.titleMedium); Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } } }
}

@Composable
private fun CoreProcessScreen(state: GameState, onExecuteCommand: (String) -> String) {
    var input by remember { mutableStateOf("") }
    var output by remember { mutableStateOf("") }
    var logs by remember { mutableStateOf(listOf<String>()) }
    var pendingCommand by remember { mutableStateOf<String?>(null) }
    val suggestions = remember(input) { CommandAutocomplete.suggest(input) }
    fun runCommand(command: String) {
        val result = onExecuteCommand(command)
        output = result
        logs = (logs + "${System.currentTimeMillis()}  $command → $result").takeLast(100)
        GameLogger.log(LogLevel.INFO, LoggerCategory.COMMAND, "DEV_PLAYGROUND", "Core command '$command': $result")
        input = ""
    }
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Text("Native Kotlin / Compose", style = MaterialTheme.typography.titleLarge) }
        item { Text("Commands execute against the active debug game state. Commands that can alter progress require confirmation.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        item { OutlinedTextField(input, { input = it }, Modifier.fillMaxWidth(), label = { Text("Core command") }, singleLine = true) }
        if (suggestions.isNotEmpty()) item { Text(suggestions.joinToString("  • ") { it.insertText }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary) }
        item {
            Button(onClick = {
                if (input.isNotBlank()) {
                    val command = input.trim()
                    if (command.substringBefore(' ').lowercase() in setOf("/reset", "/give", "/set", "/timeskip", "/skip", "/moneyprinter", "/unlock", "/unlock_all", "/crash_test", "/konami", "/win", "/completeguess", "/max_upgrades", "/maxupgrades", "/autoclicker", "/setlevel", "/speed", "/test", "/newnumber")) {
                        pendingCommand = command
                    } else runCommand(command)
                }
            }) { Icon(Icons.Default.PlayArrow, null); Spacer(Modifier.width(6.dp)); Text("Run") }
        }
        item { DeveloperPanel("Core state", "money=${state.money}\nprestige=${state.prestige}\nultra=${state.ultra}\nnebula=${state.nebula}\nautoClicker=${state.autoClickerActive}\nlastSave=${state.lastSaveTimestamp}") }
        item { DeveloperPanel("Last result", output.ifBlank { "No command executed yet." }) }
        item { Text("Core command log", style = MaterialTheme.typography.titleMedium) }
        items(logs.asReversed()) { Text(it, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall) }
    }
    pendingCommand?.let { command ->
        AlertDialog(
            onDismissRequest = { pendingCommand = null },
            title = { Text("Confirm developer command") },
            text = { Text("$command can change gameplay progress or trigger disruptive behavior. Continue?") },
            confirmButton = { TextButton(onClick = { pendingCommand = null; runCommand(command) }) { Text("Run command") } },
            dismissButton = { TextButton(onClick = { pendingCommand = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun WebProcessScreen(onTestDailyRewards: () -> Unit) {
    var input by remember { mutableStateOf("document.title") }; var logs by remember { mutableStateOf(emptyList<String>()) }
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Text("HTML / JavaScript / Android WebView", style = MaterialTheme.typography.titleLarge) }
        item { Text("Web commands and logs are isolated from the Core Process. JavaScript execution uses the existing debug WebView console bridge.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        item { DeveloperPanel("Bridge connection", if (WebViewDebugConsole.evaluator == null) "WebView evaluator is not attached" else "WebView evaluator attached; JavaScript evaluation is available") }
        item { OutlinedButton(onClick = { WebViewDebugConsole.runJavascript("typeof window.gtnHost+'|'+typeof window.gtnShowDailyRewardTest+'|'+document.readyState") }, modifier = Modifier.fillMaxWidth()) { Text("Check Web bridge and page readiness") } }
        item { OutlinedTextField(input, { input = it }, Modifier.fillMaxWidth(), label = { Text("JavaScript / Web command") }, minLines = 2) }
        item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                val command = input.trim()
                if (command.startsWith("/")) {
                    val name = command.substringBefore(' ').lowercase()
                    val argument = command.substringAfter(' ', "").trim().lowercase()
                    when (name) {
                        "/testdailyrewards" -> {
                            logs = (logs + "${System.currentTimeMillis()} HOST → $command").takeLast(100)
                            onTestDailyRewards()
                        }
                        "/testpopup" -> {
                            val script = when (argument) {
                                "daily" -> "window.gtnShowDailyRewardTest && window.gtnShowDailyRewardTest()"
                                "offline" -> "document.getElementById('offlineRewardOverlay').hidden=false; document.getElementById('offlineRewardAmount').textContent='TEST ONLY'; 'Visual-only popup; no reward granted'"
                                else -> null
                            }
                            if (script == null) logs = (logs + "${System.currentTimeMillis()} SKIPPED → $command (use daily or offline)").takeLast(100)
                            else { WebViewDebugConsole.runJavascript(script); logs = (logs + "${System.currentTimeMillis()} WEB TEST → $command").takeLast(100) }
                        }
                        "/testofflineearnings" -> {
                            WebViewDebugConsole.runJavascript("document.getElementById('offlineRewardOverlay').hidden=false; document.getElementById('offlineRewardAmount').textContent='TEST ONLY'; 'Visual-only popup; no reward granted'")
                            logs = (logs + "${System.currentTimeMillis()} WEB PREVIEW → $command (does not simulate save-loader earnings)").takeLast(100)
                        }
                        "/testtutorial" -> {
                            if (argument == "nimmy") {
                                WebViewDebugConsole.runJavascript("document.getElementById('rerunNimmyButton')?.click()")
                                logs = (logs + "${System.currentTimeMillis()} WEB TEST → Nimmy tutorial replay requested").takeLast(100)
                            } else logs = (logs + "${System.currentTimeMillis()} SKIPPED → $command (use nimmy)").takeLast(100)
                        }
                        "/testbridge" -> {
                            WebViewDebugConsole.runJavascript("window.gtnReadState && window.gtnReadState()")
                            logs = (logs + "${System.currentTimeMillis()} BRIDGE PING → read_state request sent; check JS console and bridge logs").takeLast(100)
                        }
                        "/testui" -> {
                            WebViewDebugConsole.runJavascript("Array.from(document.querySelectorAll('#popupLayer .reward-overlay')).map(e=>e.id+':'+e.hidden+':'+e.getBoundingClientRect().width+'x'+e.getBoundingClientRect().height).join(',')")
                            logs = (logs + "${System.currentTimeMillis()} UI AUDIT → popup overlay bounds requested").takeLast(100)
                        }
                        else -> logs = (logs + "${System.currentTimeMillis()} NOT WIRED → $command (no action taken)").takeLast(100)
                    }
                } else {
                    WebViewDebugConsole.runJavascript(input)
                    logs = (logs + "${System.currentTimeMillis()} JS → $input").takeLast(100)
                }
            }) { Icon(Icons.Default.PlayArrow, null); Spacer(Modifier.width(6.dp)); Text("Run") }
            OutlinedButton(onClick = { logs = emptyList() }) { Text("Clear command log") }
            OutlinedButton(onClick = { WebViewDebugConsole.clear() }) { Text("Clear JS console") }
        } }
        item { DeveloperPanel("Web commands", "/testdailyrewards\n/testpopup daily|offline\n/testofflineearnings (visual-only preview)\n/testtutorial nimmy\n/testbridge\n/testui\n\nOther slash commands are marked NOT WIRED. JavaScript expressions can be run directly.") }
        item { Text("Web console", style = MaterialTheme.typography.titleMedium) }
        items(WebViewDebugConsole.entries.takeLast(100).asReversed()) { Text(it, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall) }
        item { Text("Web command log", style = MaterialTheme.typography.titleMedium) }
        items(logs.asReversed()) { Text(it, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall) }
    }
}

@Composable
private fun DeveloperTestSuitesScreen(state: GameState, onExecuteCommand: (String) -> String) {
    var results by remember { mutableStateOf(listOf<String>()) }
    val suites = listOf("Core read-only smoke checks" to listOf("/version", "/state", "/stats", "/help"))
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Test Suites", style = MaterialTheme.typography.titleLarge) }
        items(suites) { (name, commands) -> Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { Text(name, style = MaterialTheme.typography.titleMedium); Text(commands.joinToString("  "), fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall); Button(onClick = { val suite = commands.map { command -> val result = onExecuteCommand(command); val pass = result.isNotBlank() && !result.startsWith("Unknown command") && !result.startsWith("Invalid command") && !result.startsWith("Developer mode is disabled"); "${if (pass) "PASS" else "FAIL"}  $command → $result" }; results = results + suite }) { Text("Run suite") } } } }
        item { DeveloperPanel("Latest results", results.takeLast(30).joinToString("\n").ifBlank { "No tests run yet." }) }
        item { Text("Web-only commands such as /testdailyrewards remain on the Web Process screen so Core and Web tests stay separated.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
private fun DeveloperUiPlaygroundScreen(onTestDailyRewards: () -> Unit) {
    var selected by remember { mutableStateOf("popup") }
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Text("UI Playground", style = MaterialTheme.typography.titleLarge) }
        item { Text("Organize popup, tutorial, loading and edge-case scenarios here before wiring feature-specific Web commands.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        item { Button(onClick = { selected = "popup" }) { Text("Popup scenarios") }; Spacer(Modifier.height(4.dp)); Button(onClick = { selected = "tutorial" }) { Text("Tutorial scenarios") }; Spacer(Modifier.height(4.dp)); Button(onClick = { selected = "edge" }) { Text("Edge-case scenarios") } }
        item { DeveloperPanel("Selected scenario", selected) }
        item { Button(onClick = { WebViewDebugConsole.runJavascript("window.gtnShowDailyRewardTest && window.gtnShowDailyRewardTest()") }, modifier = Modifier.fillMaxWidth()) { Text("Open daily reward popup in WebView") } }
        item { OutlinedButton(onClick = { WebViewDebugConsole.runJavascript("document.getElementById('offlineRewardOverlay').hidden=false; document.getElementById('offlineRewardAmount').textContent='TEST ONLY'; 'Visual-only popup; no reward granted'") }, modifier = Modifier.fillMaxWidth()) { Text("Preview offline-reward popup (visual only)") } }
        item { OutlinedButton(onClick = { WebViewDebugConsole.runJavascript("document.getElementById('rerunNimmyButton')?.click()") }, modifier = Modifier.fillMaxWidth()) { Text("Replay Nimmy tutorial") } }
        item { OutlinedButton(onClick = { WebViewDebugConsole.runJavascript("Array.from(document.querySelectorAll('#popupLayer .reward-overlay')).map(e=>e.id+':'+e.hidden+':'+e.getBoundingClientRect().width+'x'+e.getBoundingClientRect().height).join(',')") }, modifier = Modifier.fillMaxWidth()) { Text("Inspect popup bounds") } }
        item { OutlinedButton(onClick = onTestDailyRewards, modifier = Modifier.fillMaxWidth()) { Text("Run native daily-reward test flow") } }
        item { DeveloperPanel("Acceptance checks", "Popup box remains fully visible\nCentered correctly\nButtons remain tappable\nLong text does not clip\nDismiss/collect closes correctly\nLocalization remains readable") }
    }
}

@Composable
private fun DeveloperSaveLabScreen(state: GameState) {
    var simulatedElapsedSeconds by remember { mutableStateOf(3600L) }
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Text("Save Laboratory", style = MaterialTheme.typography.titleLarge) }
        item { Text("Read-only preview only. These scenarios do not alter the active save.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        item { DeveloperPanel("Current save state", "lastSaveTimestamp=${state.lastSaveTimestamp}\nautoClickerActive=${state.autoClickerActive}\ntimeTravelPenaltyUntil=${state.timeTravelPenaltyUntil}") }
        item { Text("Offline duration preview", style = MaterialTheme.typography.titleMedium) }
        item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { simulatedElapsedSeconds = 60L }) { Text("1 min") }
            OutlinedButton(onClick = { simulatedElapsedSeconds = 3600L }) { Text("1 hour") }
            OutlinedButton(onClick = { simulatedElapsedSeconds = 28800L }) { Text("8 hours") }
        } }
        item { DeveloperPanel("Offline earnings preview", "Elapsed seconds: ${simulatedElapsedSeconds}\nAuto-clicker active: ${state.autoClickerActive}\nEstimated money: ${if (state.autoClickerActive) 500L * simulatedElapsedSeconds else 0L}\nMaximum duration: 28800 seconds") }
        item { Text("Corrupted-save, migration, and restore tests need isolated fixture storage before they can safely execute.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
private fun DeveloperDiagnosticsScreen(state: GameState) {
    val logs by GameLogger.logFlow.collectAsState(initial = emptyList())
    val clipboard = LocalClipboardManager.current
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Text("Diagnostics", style = MaterialTheme.typography.titleLarge) }
        item { DeveloperPanel("Runtime", "App debug build\nCore state loaded\nHTML console entries=${WebViewDebugConsole.entries.size}\nCore logs=${logs.size}\nCommands registered=${CommandRegistry.commands.size}") }
        item { DeveloperPanel("Game snapshot", "money=${state.money}\nrange=${state.currentRangeMin}..${state.currentRangeMax}\nattempts=${state.attempts}\ncorrect=${state.correctGuesses}\nstreak=${state.streak}") }
        item { Button(onClick = {
            val report = buildString {
                appendLine("GuessTheNumber Developer Diagnostic Report")
                appendLine("Build: debug")
                appendLine("HTML console entries: ${WebViewDebugConsole.entries.size}")
                appendLine("Core logs: ${logs.size}")
                appendLine("Commands registered: ${CommandRegistry.commands.size}")
                appendLine("Game snapshot: money=${state.money}, range=${state.currentRangeMin}..${state.currentRangeMax}, attempts=${state.attempts}, streak=${state.streak}")
                appendLine("\nRecent Core Logs:")
                appendLine(GameLogger.exportLogs().takeLast(12000))
                appendLine("\nRecent Web Console:")
                appendLine(WebViewDebugConsole.entries.takeLast(100).joinToString("\n"))
            }
            clipboard.setText(AnnotatedString(report))
        }, modifier = Modifier.fillMaxWidth()) { Text("Copy diagnostic report") } }
        item { Text("Recent core logs", style = MaterialTheme.typography.titleMedium) }
        items(logs.takeLast(50).asReversed()) { log -> Text("${log.level} ${log.category} ${log.event}: ${log.message}", fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall) }
    }
}

@Composable
private fun DeveloperPanel(title: String, body: String) { Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { Text(title, style = MaterialTheme.typography.titleSmall); Text(body, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall) } } }
