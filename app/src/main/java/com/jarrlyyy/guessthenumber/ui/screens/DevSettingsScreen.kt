package com.jarrlyyy.guessthenumber.ui.screens

import com.jarrlyyy.guessthenumber.ui.localization.LocalAppLocaleManager
import com.jarrlyyy.guessthenumber.ui.localization.localizedText

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
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
    onGetAppPreferencesJson: ((String) -> Unit) -> Unit,
    onApplyAppPreferencesJson: (String, (Boolean) -> Unit) -> Unit,
    onGetSaveJson: (Int, (String?) -> Unit) -> Unit,
    onApplySaveJson: (Int, String, (Boolean) -> Unit) -> Unit,
    onNavigate: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val locale = LocalAppLocaleManager.current
    var commandInput by remember { mutableStateOf("") }
    var saveJsonEditorInput by remember { mutableStateOf("") }
    var appPreferencesJsonEditorInput by remember { mutableStateOf("") }
    var selectedSaveSlot by remember { mutableIntStateOf(1) }
    var saveSlotExpanded by remember { mutableStateOf(false) }
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
                title = { Text(localizedText("Developer Settings & Console")) },
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
                    Text(localizedText("Open Variable & Process Inspector"), fontSize = 16.sp)
                }
            }

            // Section 0.5: Log Storage Setting & Zip/Share
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(localizedText("Save non-crash logs to storage"), style = MaterialTheme.typography.titleMedium, fontSize = 16.sp)
                            Text(localizedText("Saves game logs to Android/data files directory"), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = state.settings.saveLogsToStorage,
                            onCheckedChange = { enabled ->
                                onUpdateSettings(state.settings.copy(saveLogsToStorage = enabled))
                            }
                        )
                    }

                    Button(
                        onClick = {
                            try {
                                val zipFile = File(context.cacheDir, "game_logs_${System.currentTimeMillis()}.zip")
                                ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
                                    val logFile = File(context.filesDir, "game_logs.txt")
                                    if (logFile.exists()) {
                                        zos.putNextEntry(ZipEntry(logFile.name))
                                        logFile.inputStream().use { it.copyTo(zos) }
                                        zos.closeEntry()
                                    }
                                    val memLogs = GameLogger.exportLogs()
                                    if (memLogs.isNotBlank()) {
                                        zos.putNextEntry(ZipEntry("in_memory_logs.txt"))
                                        zos.write(memLogs.toByteArray())
                                        zos.closeEntry()
                                    }
                                }
                                val uri = androidx.core.content.FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    zipFile
                                )
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    type = "application/zip"
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                val shareIntent = Intent.createChooser(sendIntent, "Share Zipped Logs")
                                context.startActivity(shareIntent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Failed to zip and share logs: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(localizedText("Zip & Share Logs from Storage"), fontSize = 14.sp)
                    }
                }
            }

            // Section 1: Developer Commands & Autocomplete
            item {
                HorizontalDivider()
                Spacer(modifier = Modifier.height(4.dp))
                Text(localizedText("Developer Commands"), style = MaterialTheme.typography.titleMedium, fontSize = 18.sp)
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
                            label = { Text(localizedText("Command (e.g. /help)")) },
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
                            Text(localizedText("Run"))
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

            // Section 1.5: Global App Preferences JSON
            item {
                HorizontalDivider()
                Spacer(modifier = Modifier.height(4.dp))
                Text(localizedText("App Preferences (Global JSON)"), style = MaterialTheme.typography.titleMedium, fontSize = 18.sp)
            }
            item {
                OutlinedTextField(
                    value = appPreferencesJsonEditorInput,
                    onValueChange = { appPreferencesJsonEditorInput = it },
                    label = { Text(localizedText("Raw App Preferences JSON")) },
                    modifier = Modifier.fillMaxWidth().height(150.dp)
                )
            }
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        onGetAppPreferencesJson {
                            appPreferencesJsonEditorInput = it
                            Toast.makeText(context, "Loaded global app preferences", Toast.LENGTH_SHORT).show()
                        }
                    }, modifier = Modifier.weight(1f)) { Text(localizedText("Load App Preferences")) }
                    Button(onClick = {
                        onApplyAppPreferencesJson(appPreferencesJsonEditorInput) { success ->
                            Toast.makeText(context, if (success) "App preferences applied!" else "Invalid app preferences JSON!", Toast.LENGTH_SHORT).show()
                        }
                    }, modifier = Modifier.weight(1f)) { Text(localizedText("Apply App Preferences")) }
                }
            }

            // Section 2: Save Editor (JSON Text Editing)
            item {
                HorizontalDivider()
                Spacer(modifier = Modifier.height(4.dp))
                Text(localizedText("Save Editor (JSON Editing)"), style = MaterialTheme.typography.titleMedium, fontSize = 18.sp)
            }
            item {
                ExposedDropdownMenuBox(
                    expanded = saveSlotExpanded,
                    onExpandedChange = { saveSlotExpanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = "Save Slot $selectedSaveSlot",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(localizedText("Save Slots")) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(saveSlotExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = saveSlotExpanded, onDismissRequest = { saveSlotExpanded = false }) {
                        (1..10).forEach { slot ->
                            DropdownMenuItem(
                                text = { Text(localizedText("Save Slot $slot")) },
                                onClick = { selectedSaveSlot = slot; saveSlotExpanded = false }
                            )
                        }
                    }
                }
            }
            item {
                OutlinedTextField(
                    value = saveJsonEditorInput,
                    onValueChange = { saveJsonEditorInput = it },
                    label = { Text(localizedText("Raw GameState JSON")) },
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
                            onGetSaveJson(selectedSaveSlot) { raw -> saveJsonEditorInput = raw ?: ""; }
                            Toast.makeText(context, "Loaded current state into editor", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(localizedText("Load Current Save"), fontSize = 12.sp)
                    }
                    Button(
                        onClick = {
                            if (saveJsonEditorInput.isNotBlank()) {
                                onApplySaveJson(selectedSaveSlot, saveJsonEditorInput) { success ->
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
                        Text(localizedText("Apply Save JSON"), fontSize = 12.sp)
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
                        Text(localizedText("Live Console Logs"), style = MaterialTheme.typography.titleMedium, fontSize = 18.sp)
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
                                    clipboard.setPrimaryClip(ClipData.newPlainText(locale.getString("Game Logs", "Game Logs"), exported))
                                    Toast.makeText(context, locale.getString("Logs copied to clipboard", "Logs copied to clipboard"), Toast.LENGTH_SHORT).show()
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
                        label = { Text(localizedText("Filter logs (category, level, message)...")) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }

            // Test Locales Section
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(locale.getString("test_locales", "Test Locales"), fontSize = 16.sp, style = MaterialTheme.typography.titleMedium)
                        Text(locale.getString("test"), fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                        Text(locale.getString("test_locales_desc", "Switch active locale temporarily. Resets to device default on app restart."), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = {
                                androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(
                                    androidx.core.os.LocaleListCompat.forLanguageTags("en-US")
                                )
                                Toast.makeText(context, locale.getString("switched_locale_us_toast", "Switched locale to English (US)"), Toast.LENGTH_SHORT).show()
                            }) {
                                Text(locale.getString("english_us", "English (US)"))
                            }
                            Button(onClick = {
                                androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(
                                    androidx.core.os.LocaleListCompat.forLanguageTags("fil-PH")
                                )
                                Toast.makeText(context, locale.getString("switched_locale_fil_toast", "Inilipat ang wika sa Filipino"), Toast.LENGTH_SHORT).show()
                            }) {
                                Text(locale.getString("filipino", "Filipino"))
                            }
                        }
                    }
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
                            Text(text = localizedText("No logs recorded yet..."),
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
                                Text(text = localizedText("[$timeStr][${log.category}][${log.level}] ${log.message}"),
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
