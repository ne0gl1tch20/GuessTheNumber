package com.jarrlyyy.guessthenumber.desktop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jarrlyyy.guessthenumber.domain.model.GameSettings
import com.jarrlyyy.guessthenumber.domain.model.GameState

private val desktopLanguageOptions = listOf(
    "en-US" to "English (US)", "en-GB" to "English (UK)", "fil-PH" to "Filipino",
    "es" to "Español", "fr" to "Français", "de" to "Deutsch", "it" to "Italiano",
    "pt-BR" to "Português (Brasil)", "pt-PT" to "Português (Portugal)", "nl" to "Nederlands",
    "pl" to "Polski", "ru" to "Русский", "uk" to "Українська", "tr" to "Türkçe",
    "ar" to "العربية", "hi-IN" to "हिन्दी", "id" to "Bahasa Indonesia", "th" to "ไทย",
    "vi" to "Tiếng Việt", "ja" to "日本語", "ko" to "한국어", "zh-CN" to "简体中文",
    "zh-TW" to "繁體中文"
)

private val desktopThemePresets = mapOf(
    "Neon" to listOf("#7C4DFF", "#00BCD4", "#00C853"),
    "Sunset" to listOf("#FF4081", "#FF9800", "#FFC107"),
    "Ocean" to listOf("#2196F3", "#00BCD4", "#3F51B5"),
    "Forest" to listOf("#4CAF50", "#009688", "#8BC34A")
)

@Composable
fun DesktopSettingsScreen(
    state: GameState,
    isSaving: Boolean,
    onUpdateSettings: (GameSettings) -> Unit,
    onResetData: () -> Unit,
    navigationPosition: DesktopNavigationPosition,
    onNavigationPositionChange: (DesktopNavigationPosition) -> Unit,
    onBack: () -> Unit
) {
    val settings = state.settings
    var showResetDialog by remember { mutableStateOf(false) }
    var resetConfirmed by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Settings & Save Management", style = MaterialTheme.typography.headlineSmall)
                Text("Your settings use the same saved GameSettings model as Android.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick = onBack) { Text("Back") }
        }

        SettingsCard("Windows Navigation") {
            Text(
                "Choose which edge of the app holds the navigation bar. Left is the default.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
            DropdownSettingRow(
                "Navigation Position",
                navigationPosition.label,
                DesktopNavigationPosition.entries.map { it.label to it.name },
                onSelect = { selected ->
                    DesktopNavigationPosition.entries.firstOrNull { it.name == selected }?.let(onNavigationPositionChange)
                }
            )
        }

        SettingsCard("Game Preferences") {
            DropdownSettingRow("Language", settings.locale, desktopLanguageOptions, onSelect = {
                onUpdateSettings(settings.copy(locale = it))
            })
            Text(
                "The language preference is saved. Full Windows translation coverage is still being ported.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            DropdownSettingRow(
                "Theme Appearance",
                settings.themeMode,
                listOf("System", "Dark", "Light", "AMOLED").map { it to it },
                onSelect = { onUpdateSettings(settings.copy(themeMode = it)) }
            )
            DropdownSettingRow(
                "Number Notation",
                settings.numberNotation,
                listOf("Standard", "Scientific", "Engineering").map { it to it },
                onSelect = { onUpdateSettings(settings.copy(numberNotation = it)) }
            )
        }

        SettingsCard("Theme Colors") {
            DropdownSettingRow(
                "Theme Preset",
                settings.themePreset,
                (desktopThemePresets.keys.toList() + "Custom").map { it to it },
                onSelect = { selected ->
                    val colors = desktopThemePresets[selected]
                    onUpdateSettings(
                        if (colors == null) settings.copy(themePreset = "Custom")
                        else settings.copy(
                            themePreset = selected,
                            customPrimaryColor = colors[0],
                            customSecondaryColor = colors[1],
                            customTertiaryColor = colors[2]
                        )
                    )
                }
            )
            Text("Changes apply to the Windows theme as you edit them.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(
                value = settings.customPrimaryColor,
                onValueChange = { onUpdateSettings(settings.copy(themePreset = "Custom", customPrimaryColor = it)) },
                label = { Text("Primary color (hex)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = settings.customSecondaryColor,
                onValueChange = { onUpdateSettings(settings.copy(themePreset = "Custom", customSecondaryColor = it)) },
                label = { Text("Secondary color (hex)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = settings.customTertiaryColor,
                onValueChange = { onUpdateSettings(settings.copy(themePreset = "Custom", customTertiaryColor = it)) },
                label = { Text("Tertiary color (hex)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        SettingsCard("Sound & Interaction") {
            SettingsSwitch("Sound FX", "Enable or disable sound effects.", settings.soundEnabled) {
                onUpdateSettings(settings.copy(soundEnabled = it))
            }
            SettingsSwitch("Vibration", "Stored preference. Windows vibration support depends on compatible hardware.", settings.vibrationEnabled) {
                onUpdateSettings(settings.copy(vibrationEnabled = it))
            }
            Text("Master Volume: ${(settings.volume * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium)
            Slider(value = settings.volume.coerceIn(0f, 1f), onValueChange = {
                onUpdateSettings(settings.copy(volume = it))
            }, valueRange = 0f..1f)
            Text("Sound Effects Volume: ${(settings.sfxVolume * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium)
            Slider(
                value = settings.sfxVolume.coerceIn(0f, 1f),
                onValueChange = { onUpdateSettings(settings.copy(sfxVolume = it)) },
                enabled = settings.soundEnabled,
                valueRange = 0f..1f
            )
        }

        SettingsCard("Visual Comfort & Accessibility") {
            SettingsSwitch("Reduce Flashes", "Reduce sudden visual changes where supported.", settings.reduceFlashes) {
                onUpdateSettings(settings.copy(reduceFlashes = it))
            }
            SettingsSwitch("Reduced Motion", "Soften or disable animated screen transitions.", settings.reducedMotion) {
                onUpdateSettings(settings.copy(reducedMotion = it))
            }
        }

        SettingsCard("Notifications") {
            SettingsSwitch("Notifications & Reminders", "Preference saved; Windows notification delivery is not connected yet.", settings.notificationsEnabled) {
                onUpdateSettings(settings.copy(notificationsEnabled = it))
            }
            if (settings.notificationsEnabled) {
                DropdownSettingRow(
                    "Reminder Frequency",
                    "${settings.notificationIntervalHours} hours",
                    listOf(6L, 12L, 24L, 48L, 72L).map { "${it} hours" to it.toString() },
                    onSelect = { onUpdateSettings(settings.copy(notificationIntervalHours = it.toLong())) }
                )
                SettingsSwitch("Reminders", "Reminder notification category preference.", settings.notificationRemindersEnabled) {
                    onUpdateSettings(settings.copy(notificationRemindersEnabled = it))
                }
                SettingsSwitch("Progression Milestones", "Progression notification category preference.", settings.notificationProgressionEnabled) {
                    onUpdateSettings(settings.copy(notificationProgressionEnabled = it))
                }
                SettingsSwitch("Game Events", "Game-event notification category preference.", settings.notificationEventsEnabled) {
                    onUpdateSettings(settings.copy(notificationEventsEnabled = it))
                }
            }
        }

        SettingsCard("Developer & Save Preferences") {
            SettingsSwitch("Save Logs to Storage", "Keep this preference aligned with the Android settings model.", settings.saveLogsToStorage) {
                onUpdateSettings(settings.copy(saveLogsToStorage = it))
            }
            SettingsSwitch("Verbose Logging", "Keep this preference aligned with the Android settings model.", settings.verboseLogging) {
                onUpdateSettings(settings.copy(verboseLogging = it))
            }
            Text(
                "Windows saves are stored locally and include a recovery backup. Resetting progress removes the old backup too.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
            Button(
                onClick = {
                    resetConfirmed = false
                    showResetDialog = true
                },
                enabled = !isSaving
            ) { Text("Reset All Game Data") }
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { if (!isSaving) showResetDialog = false },
            title = { Text("Confirm Data Reset") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("This clears the saved game progress in the active Windows slot. The old recovery backup is removed too.")
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = resetConfirmed, onCheckedChange = { resetConfirmed = it })
                        Text("I understand this will erase my progress.")
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showResetDialog = false
                        onResetData()
                    },
                    enabled = resetConfirmed && !isSaving
                ) { Text("Reset Everything") }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }, enabled = !isSaving) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SettingsCard(title: String, content: @Composable () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            content()
        }
    }
}

@Composable
private fun SettingsSwitch(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun DropdownSettingRow(
    label: String,
    selected: String,
    options: List<Pair<String, String>>,
    onSelect: (String) -> Unit
) {
    var expanded by remember(label) { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        androidx.compose.foundation.layout.Box {
            TextButton(onClick = { expanded = true }) {
                Text("$selected  ▾")
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { (title, value) ->
                    DropdownMenuItem(
                        text = { Text(title) },
                        onClick = {
                            onSelect(value)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
