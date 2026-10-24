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
fun DeveloperPlaygroundScreen(state: GameState, onExecuteCommand: (String) -> String, onBack: () -> Unit) {
    var destination by remember { mutableStateOf("hub") }
    Scaffold(topBar = { TopAppBar(title = { Text(if (destination == "hub") "Developer Playground" else destinationLabel(destination)) }, navigationIcon = { IconButton(onClick = { if (destination == "hub") onBack() else destination = "hub" }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } }) }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (destination) {
                "core" -> CoreProcessScreen(state, onExecuteCommand)
                "web" -> WebProcessScreen()
                "tests" -> DeveloperTestSuitesScreen(state, onExecuteCommand)
                "ui" -> DeveloperUiPlaygroundScreen()
                "save" -> DeveloperSaveLabScreen(state, onExecuteCommand)
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
    var input by remember { mutableStateOf("") }; var output by remember { mutableStateOf("") }; var logs by remember { mutableStateOf(listOf<String>()) }
    val suggestions = remember(input) { CommandAutocomplete.suggest(input) }
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Text("Native Kotlin / Compose", style = MaterialTheme.typography.titleLarge) }
        item { OutlinedTextField(input, { input = it }, Modifier.fillMaxWidth(), label = { Text("Core command") }, singleLine = true) }
        if (suggestions.isNotEmpty()) item { Text(suggestions.joinToString("  • ") { it.insertText }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary) }
        item { Button(onClick = { if (input.isNotBlank()) { val result = onExecuteCommand(input); output = result; logs = (logs + "${System.currentTimeMillis()}  $input → $result").takeLast(100); GameLogger.log(LogLevel.INFO, LoggerCategory.COMMAND, "DEV_PLAYGROUND", "Core command '$input': $result"); input = "" } }) { Icon(Icons.Default.PlayArrow, null); Spacer(Modifier.width(6.dp)); Text("Run") } }
        item { DeveloperPanel("Core state", "money=${state.money}\nprestige=${state.prestige}\nultra=${state.ultra}\nnebula=${state.nebula}\nautoClicker=${state.autoClickerActive}\nlastSave=${state.lastSaveTimestamp}") }
        item { DeveloperPanel("Last result", output.ifBlank { "No command executed yet." }) }
        item { Text("Core command log", style = MaterialTheme.typography.titleMedium) }
        items(logs.asReversed()) { Text(it, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall) }
    }
}

@Composable
private fun WebProcessScreen() {
    var input by remember { mutableStateOf("document.title") }; var logs by remember { mutableStateOf(emptyList<String>()) }
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Text("HTML / JavaScript / Android WebView", style = MaterialTheme.typography.titleLarge) }
        item { Text("Web commands and logs are isolated from the Core Process. JavaScript execution uses the existing debug WebView console bridge.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        item { OutlinedTextField(input, { input = it }, Modifier.fillMaxWidth(), label = { Text("JavaScript / Web command") }, minLines = 2) }
        item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Button(onClick = { WebViewDebugConsole.runJavascript(input); logs = (logs + "${System.currentTimeMillis()}  JS → $input").takeLast(100) }) { Icon(Icons.Default.PlayArrow, null); Spacer(Modifier.width(6.dp)); Text("Run") }; OutlinedButton(onClick = { logs = emptyList(); WebViewDebugConsole.clear() }) { Text("Clear") } } }
        item { DeveloperPanel("Suggested Web commands", "/testpopup <name>\n/testtutorial <name>\n/testdailyrewards\n/testofflineearnings\n/testbridge\n/testui") }
        item { Text("Web console", style = MaterialTheme.typography.titleMedium) }
        items(WebViewDebugConsole.entries.takeLast(100).asReversed()) { Text(it, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall) }
        item { Text("Web command log", style = MaterialTheme.typography.titleMedium) }
        items(logs.asReversed()) { Text(it, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall) }
    }
}

@Composable
private fun DeveloperTestSuitesScreen(state: GameState, onExecuteCommand: (String) -> String) {
    var results by remember { mutableStateOf(listOf<String>()) }
    val suites = listOf("Core sanity" to listOf("/version", "/state", "/stats"), "Recovery" to listOf("/test antitimetravel"), "Gameplay" to listOf("/newnumber", "/autoclicker on"))
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Test Suites", style = MaterialTheme.typography.titleLarge) }
        items(suites) { (name, commands) -> Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { Text(name, style = MaterialTheme.typography.titleMedium); Text(commands.joinToString("  "), fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall); Button(onClick = { val suite = commands.map { command -> val result = onExecuteCommand(command); val pass = result.isNotBlank() && !result.startsWith("Unknown command") && !result.startsWith("Invalid command"); "${if (pass) "PASS" else "FAIL"}  $command → $result" }; results = results + suite }) { Text("Run suite") } } } }
        item { DeveloperPanel("Latest results", results.takeLast(30).joinToString("\n").ifBlank { "No tests run yet." }) }
        item { Text("Web-only commands such as /testdailyrewards remain on the Web Process screen so Core and Web tests stay separated.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
private fun DeveloperUiPlaygroundScreen() {
    var selected by remember { mutableStateOf("popup") }
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Text("UI Playground", style = MaterialTheme.typography.titleLarge) }
        item { Text("Organize popup, tutorial, loading and edge-case scenarios here before wiring feature-specific Web commands.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        item { Button(onClick = { selected = "popup" }) { Text("Popup scenarios") }; Spacer(Modifier.height(4.dp)); Button(onClick = { selected = "tutorial" }) { Text("Tutorial scenarios") }; Spacer(Modifier.height(4.dp)); Button(onClick = { selected = "edge" }) { Text("Edge-case scenarios") } }
        item { DeveloperPanel("Selected scenario", selected) }
        item { DeveloperPanel("Acceptance checks", "Box remains fully visible\nCentered correctly\nButtons remain tappable\nLong text does not clip\nDismiss/collect closes correctly\nLocalization remains readable") }
    }
}

@Composable
private fun DeveloperSaveLabScreen(state: GameState, onExecuteCommand: (String) -> String) {
    var result by remember { mutableStateOf("") }
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Text("Save Laboratory", style = MaterialTheme.typography.titleLarge) }
        item { DeveloperPanel("Current save state", "lastSaveTimestamp=${state.lastSaveTimestamp}\nautoClickerActive=${state.autoClickerActive}\ntimeTravelPenaltyUntil=${state.timeTravelPenaltyUntil}") }
        item { Button(onClick = { result = onExecuteCommand("/timeskip 3600") }) { Text("Simulate 1 hour passive progress") } }
        item { Button(onClick = { result = onExecuteCommand("/test antitimetravel") }) { Text("Simulate time-travel protection") } }
        item { DeveloperPanel("Result", result.ifBlank { "No save scenario executed." }) }
        item { Text("This laboratory operates on the developer playground's isolated state snapshot; real player saves are not overwritten.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
private fun DeveloperDiagnosticsScreen(state: GameState) {
    val logs by GameLogger.logFlow.collectAsState(initial = emptyList())
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Text("Diagnostics", style = MaterialTheme.typography.titleLarge) }
        item { DeveloperPanel("Runtime", "App debug build\nCore state loaded\nHTML console entries=${WebViewDebugConsole.entries.size}\nCore logs=${logs.size}\nCommands registered=${CommandRegistry.commands.size}") }
        item { DeveloperPanel("Game snapshot", "money=${state.money}\nrange=${state.currentRangeMin}..${state.currentRangeMax}\nattempts=${state.attempts}\ncorrect=${state.correctGuesses}\nstreak=${state.streak}") }
        item { Text("Recent core logs", style = MaterialTheme.typography.titleMedium) }
        items(logs.takeLast(50).asReversed()) { log -> Text("${log.level} ${log.category} ${log.event}: ${log.message}", fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall) }
    }
}

@Composable
private fun DeveloperPanel(title: String, body: String) { Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { Text(title, style = MaterialTheme.typography.titleSmall); Text(body, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall) } } }
