package com.jarrlyyy.guessthenumber.ui.screens

import android.Manifest
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
import androidx.compose.ui.graphics.Color
import com.jarrlyyy.guessthenumber.domain.model.GameSettings
import com.jarrlyyy.guessthenumber.domain.model.GameState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: GameState,
    onResetData: () -> Unit,
    onUpdateSettings: (GameSettings) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var showResetDialog by remember { mutableStateOf(false) }
    var confirmResetCheck by remember { mutableStateOf(false) }
    var colorTarget by remember { mutableStateOf<String?>(null) }

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

            // Language Selector Dropdown
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Language / Wika", style = MaterialTheme.typography.bodyLarge)
                    var expanded by remember { mutableStateOf(false) }
                    val languageMap = mapOf("en-US" to "English (US)", "fil-PH" to "Filipino")
                    val currentLangName = languageMap[settings.locale] ?: "English (US)"
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = it },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = currentLangName,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            languageMap.forEach { (code, name) ->
                                DropdownMenuItem(
                                    text = { Text(name) },
                                    onClick = {
                                        onUpdateSettings(settings.copy(locale = code))
                                        Toast.makeText(context, "Language changed. Restart app to take effect.", Toast.LENGTH_SHORT).show()
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Theme Appearance", style = MaterialTheme.typography.bodyLarge)
                    var expanded by remember { mutableStateOf(false) }
                    val themeOptions = listOf("System", "Dark", "Light", "AMOLED")
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = it },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = settings.themeMode,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            themeOptions.forEach { mode ->
                                DropdownMenuItem(
                                    text = { Text(mode) },
                                    onClick = {
                                        onUpdateSettings(settings.copy(themeMode = mode))
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            item {
                Text("Theme Colors", fontSize = 18.sp, style = MaterialTheme.typography.titleMedium)
                val presets = listOf(
                    "Neon" to listOf("#7C4DFF", "#00BCD4", "#00C853"),
                    "Sunset" to listOf("#FF4081", "#FF9800", "#FFC107"),
                    "Ocean" to listOf("#2196F3", "#00BCD4", "#3F51B5"),
                    "Forest" to listOf("#4CAF50", "#009688", "#8BC34A")
                )
                var presetExpanded by remember { mutableStateOf(false) }
                val selectedPreset = presets.firstOrNull { it.first == settings.themePreset }?.first ?: "Custom"
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ExposedDropdownMenuBox(
                        expanded = presetExpanded,
                        onExpandedChange = { presetExpanded = it },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = selectedPreset,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Theme Preset") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = presetExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = presetExpanded,
                            onDismissRequest = { presetExpanded = false }
                        ) {
                            presets.forEach { (name, colors) ->
                                DropdownMenuItem(
                                    text = { Text(name) },
                                    trailingIcon = { if (name == selectedPreset) Icon(Icons.Default.Check, contentDescription = null) },
                                    onClick = {
                                        onUpdateSettings(settings.copy(themePreset = name, customPrimaryColor = colors[0], customSecondaryColor = colors[1], customTertiaryColor = colors[2]))
                                        presetExpanded = false
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("Custom") },
                                trailingIcon = { if (selectedPreset == "Custom") Icon(Icons.Default.Check, contentDescription = null) },
                                onClick = {
                                    onUpdateSettings(settings.copy(themePreset = "Custom"))
                                    presetExpanded = false
                                }
                            )
                        }
                    }
                    listOf(
                        "Primary" to settings.customPrimaryColor,
                        "Secondary" to settings.customSecondaryColor,
                        "Tertiary" to settings.customTertiaryColor
                    ).forEach { (label, value) ->
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(modifier = Modifier.size(36.dp), shape = MaterialTheme.shapes.small, color = runCatching { Color(android.graphics.Color.parseColor(value)) }.getOrDefault(MaterialTheme.colorScheme.primary)) {}
                            Text(label, modifier = Modifier.weight(1f))
                            OutlinedButton(onClick = { colorTarget = label }) { Text(value) }
                        }
                    }
                }
            }

            // Number Notation Selector
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Number Notation", style = MaterialTheme.typography.bodyLarge)
                    var expanded by remember { mutableStateOf(false) }
                    val notationOptions = listOf("Standard", "Scientific", "Engineering")
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = it },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = settings.numberNotation,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            notationOptions.forEach { notation ->
                                DropdownMenuItem(
                                    text = { Text(notation) },
                                    onClick = {
                                        onUpdateSettings(settings.copy(numberNotation = notation))
                                        expanded = false
                                    }
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
                        Text("Reminder Frequency", style = MaterialTheme.typography.bodyMedium)
                        var expanded by remember { mutableStateOf(false) }
                        val intervalOptions = listOf(12L, 24L, 48L)
                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = it },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = "${settings.notificationIntervalHours} Hours",
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                intervalOptions.forEach { hours ->
                                    DropdownMenuItem(
                                        text = { Text("$hours Hours") },
                                        onClick = {
                                            onUpdateSettings(settings.copy(notificationIntervalHours = hours))
                                            expanded = false
                                        }
                                    )
                                }
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

    if (colorTarget != null) {
        val target = colorTarget!!
        val current = when (target) {
            "Primary" -> settings.customPrimaryColor
            "Secondary" -> settings.customSecondaryColor
            else -> settings.customTertiaryColor
        }
        ColorPickerDialog(
            initialHex = current,
            onDismiss = { colorTarget = null },
            onApply = { hex ->
                val updated = when (target) {
                    "Primary" -> settings.copy(themePreset = "Custom", customPrimaryColor = hex)
                    "Secondary" -> settings.copy(themePreset = "Custom", customSecondaryColor = hex)
                    else -> settings.copy(themePreset = "Custom", customTertiaryColor = hex)
                }
                onUpdateSettings(updated)
                colorTarget = null
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


@Composable
private fun ColorPickerDialog(
    initialHex: String,
    onDismiss: () -> Unit,
    onApply: (String) -> Unit
) {
    val initial = runCatching { android.graphics.Color.parseColor(initialHex) }.getOrDefault(android.graphics.Color.MAGENTA)
    val hsv = FloatArray(3).also { android.graphics.Color.colorToHSV(initial, it) }
    var hue by remember { mutableFloatStateOf(hsv[0]) }
    var saturation by remember { mutableFloatStateOf(hsv[1]) }
    var value by remember { mutableFloatStateOf(hsv[2]) }
    var hexInput by remember { mutableStateOf(initialHex.uppercase()) }

    val colorInt = android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, value))
    val color = Color(colorInt)
    val hex = "#%02X%02X%02X".format(android.graphics.Color.red(colorInt), android.graphics.Color.green(colorInt), android.graphics.Color.blue(colorInt))

    AlertDialog(onDismissRequest = onDismiss, title = { Text("🎨 Custom Theme Color") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Surface(Modifier.fillMaxWidth().height(72.dp), color = color, shape = MaterialTheme.shapes.medium) {}
            Text("Hue: " + hue.toInt() + "°")
            Slider(value = hue, onValueChange = { hue = it }, valueRange = 0f..360f)
            Text("Saturation: " + (saturation * 100).toInt() + "%")
            Slider(value = saturation, onValueChange = { saturation = it }, valueRange = 0f..1f)
            Text("Brightness: " + (value * 100).toInt() + "%")
            Slider(value = value, onValueChange = { value = it }, valueRange = 0f..1f)
            OutlinedTextField(value = hexInput, onValueChange = {
                hexInput = it.uppercase()
                runCatching { val parsed = android.graphics.Color.parseColor(hexInput); val next = FloatArray(3); android.graphics.Color.colorToHSV(parsed, next); hue = next[0]; saturation = next[1]; value = next[2] }
            }, label = { Text("Hex") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Text("Live preview: " + hex, style = MaterialTheme.typography.labelLarge)
        }
    }, confirmButton = { Button(onClick = { onApply(hex) }) { Text("Apply") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}