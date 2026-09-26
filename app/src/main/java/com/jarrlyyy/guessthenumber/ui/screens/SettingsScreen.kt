package com.jarrlyyy.guessthenumber.ui.screens

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarrlyyy.guessthenumber.domain.model.GameSettings
import com.jarrlyyy.guessthenumber.domain.model.GameState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: GameState,
    onExportSave: ((String) -> Unit) -> Unit,
    onImportSave: (String, (Boolean) -> Unit) -> Unit,
    onResetData: () -> Unit,
    onUpdateSettings: (GameSettings) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var importString by remember { mutableStateOf("") }
    var showImportDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    var confirmResetCheck by remember { mutableStateOf(false) }

    val settings = state.settings

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            onUpdateSettings(settings.copy(notificationsEnabled = true))
            Toast.makeText(context, "Notifications enabled!", Toast.LENGTH_SHORT).show()
        } else {
            onUpdateSettings(settings.copy(notificationsEnabled = false))
            Toast.makeText(context, "Notification permission denied.", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings & Save Management") },
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
            item {
                Text("Game Preferences", fontSize = 18.sp, style = MaterialTheme.typography.titleMedium)
            }

            // Theme Selector
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Theme Appearance", style = MaterialTheme.typography.bodyLarge)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("System", "Dark", "Light").forEach { mode ->
                            FilterChip(
                                selected = settings.themeMode == mode,
                                onClick = { onUpdateSettings(settings.copy(themeMode = mode)) },
                                label = { Text(mode) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Number Notation Selector
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Number Notation", style = MaterialTheme.typography.bodyLarge)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Standard", "Scientific", "Engineering").forEach { notation ->
                            FilterChip(
                                selected = settings.numberNotation == notation,
                                onClick = { onUpdateSettings(settings.copy(numberNotation = notation)) },
                                label = { Text(notation) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text("Push Notifications & Reminders", style = MaterialTheme.typography.bodyLarge)
                        Text("Get periodic reminder notifications", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = settings.notificationsEnabled,
                        onCheckedChange = { checked ->
                            if (checked) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    onUpdateSettings(settings.copy(notificationsEnabled = true))
                                }
                            } else {
                                onUpdateSettings(settings.copy(notificationsEnabled = false))
                            }
                        }
                    )
                }
            }

            // Notification Interval Selector
            if (settings.notificationsEnabled) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Reminder Frequency: ${settings.notificationIntervalHours} Hours", style = MaterialTheme.typography.bodyMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            listOf(12L, 24L, 48L).forEach { hours ->
                                FilterChip(
                                    selected = settings.notificationIntervalHours == hours,
                                    onClick = { onUpdateSettings(settings.copy(notificationIntervalHours = hours)) },
                                    label = { Text("${hours}h") },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Sound FX", style = MaterialTheme.typography.bodyLarge)
                    Switch(
                        checked = settings.soundEnabled,
                        onCheckedChange = { onUpdateSettings(settings.copy(soundEnabled = it)) }
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Vibration", style = MaterialTheme.typography.bodyLarge)
                    Switch(
                        checked = settings.vibrationEnabled,
                        onCheckedChange = { onUpdateSettings(settings.copy(vibrationEnabled = it)) }
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text("Reduce Flashes", style = MaterialTheme.typography.bodyLarge)
                        Text("Visual comfort & accessibility", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = settings.reduceFlashes,
                        onCheckedChange = { onUpdateSettings(settings.copy(reduceFlashes = it)) }
                    )
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Master Volume: ${(settings.volume * 100).toInt()}%", style = MaterialTheme.typography.bodyLarge)
                    Slider(
                        value = settings.volume,
                        onValueChange = { onUpdateSettings(settings.copy(volume = it)) },
                        valueRange = 0f..1f
                    )
                }
            }

            item {
                HorizontalDivider()
            }

            item {
                Text("Save Data Management", fontSize = 18.sp, style = MaterialTheme.typography.titleMedium)
            }

            item {
                Button(
                    onClick = {
                        onExportSave { json ->
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("GuessTheNumberSave", json)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Encrypted save copied to clipboard!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Export Encrypted Save to Clipboard")
                }
            }

            item {
                Button(
                    onClick = { showImportDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Import Encrypted Save from String")
                }
            }

            item {
                Button(
                    onClick = {
                        confirmResetCheck = false
                        showResetDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Reset All Game Data")
                }
            }
        }
    }

    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Import Save Data") },
            text = {
                OutlinedTextField(
                    value = importString,
                    onValueChange = { importString = it },
                    label = { Text("Paste encrypted save string here") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onImportSave(importString) { success ->
                        if (success) {
                            Toast.makeText(context, "Save imported successfully!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Invalid save string!", Toast.LENGTH_SHORT).show()
                        }
                        showImportDialog = false
                    }
                }) {
                    Text("Import")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Confirm Data Reset") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Are you sure you want to reset all progress? This action is irreversible.")
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Checkbox(
                            checked = confirmResetCheck,
                            onCheckedChange = { confirmResetCheck = it }
                        )
                        Text("I understand this will erase all my progress", fontSize = 14.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onResetData()
                        showResetDialog = false
                        Toast.makeText(context, "All game data has been reset.", Toast.LENGTH_SHORT).show()
                    },
                    enabled = confirmResetCheck,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Reset Everything")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
