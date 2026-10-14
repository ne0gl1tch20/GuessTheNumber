package com.jarrlyyy.guessthenumber.ui.screens

import com.jarrlyyy.guessthenumber.ui.localization.localizedText

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
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
import com.jarrlyyy.guessthenumber.ui.components.ReusableColorPickerDialog
import com.jarrlyyy.guessthenumber.data.repository.LocaleManager
import com.jarrlyyy.guessthenumber.data.repository.JsonConfigRepository

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
    val locale = remember(settings.locale) { JsonConfigRepository(context, settings.locale).localeManager }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            onUpdateSettings(settings.copy(notificationsEnabled = true))
            Toast.makeText(context, locale.getString("notifications_enabled_toast", "Notifications enabled!"), Toast.LENGTH_SHORT).show()
        } else {
            onUpdateSettings(settings.copy(notificationsEnabled = false))
            Toast.makeText(context, locale.getString("notification_permission_denied_toast", "Notification permission denied."), Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(localizedText("Settings & Save Management")) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = locale.getString("back", "Back"))
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(localizedText("Game Preferences"), fontSize = 18.sp, style = MaterialTheme.typography.titleMedium)
            }

            // Language Selector Dropdown
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(locale.getString("language", "Language"), style = MaterialTheme.typography.bodyLarge)
                    Text(locale.getString("settings_desc_language", "Choose the language used for game text and menus."), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    var expanded by remember { mutableStateOf(false) }
                    val languageMap = LocaleManager.supportedLocales.associate { it.tag to it.displayName }
                    val currentLangName = languageMap[settings.locale] ?: LocaleManager.supportedLocales.first().displayName
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
                                        androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(
                                            androidx.core.os.LocaleListCompat.forLanguageTags(code)
                                        )
                                        Toast.makeText(context, locale.getString("language_changed_restart", "Language changed."), Toast.LENGTH_SHORT).show()
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
                    Text(localizedText("Theme Appearance"), style = MaterialTheme.typography.bodyLarge)
                    Text(locale.getString("settings_desc_theme_mode", "Choose whether the app follows your device theme or uses Dark, Light or AMOLED mode."), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                Text(localizedText("Theme Colors"), fontSize = 18.sp, style = MaterialTheme.typography.titleMedium)
                Text(locale.getString("settings_desc_theme_colors", "Customize the app’s primary, secondary and tertiary colors. Changes apply immediately."), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                val previewPrimary = runCatching { Color(android.graphics.Color.parseColor(settings.customPrimaryColor)) }.getOrDefault(MaterialTheme.colorScheme.primary)
                val previewSecondary = runCatching { Color(android.graphics.Color.parseColor(settings.customSecondaryColor)) }.getOrDefault(MaterialTheme.colorScheme.secondary)
                val previewTertiary = runCatching { Color(android.graphics.Color.parseColor(settings.customTertiaryColor)) }.getOrDefault(MaterialTheme.colorScheme.tertiary)

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            locale.getString("color_picker_live_preview", "Live preview"),
                            style = MaterialTheme.typography.titleSmall
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(locale.getString("play_correct_celebration_title", "Nice guess!"), style = MaterialTheme.typography.titleMedium)
                                Text(
                                    locale.getString("play_correct_celebration_desc", "Secret number found. Your streak keeps growing."),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Surface(
                                shape = MaterialTheme.shapes.extraLarge,
                                color = previewPrimary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ) {
                                Text(
                                    locale.getString("guess_button", "Guess"),
                                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(previewPrimary, previewSecondary, previewTertiary).forEach { color ->
                                Surface(
                                    modifier = Modifier.weight(1f).height(8.dp),
                                    shape = MaterialTheme.shapes.extraLarge,
                                    color = color
                                ) {}
                            }
                        }
                    }
                }
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
                            label = { Text(localizedText("Theme Preset")) },
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
                                text = { Text(localizedText("Custom")) },
                                trailingIcon = { if (selectedPreset == "Custom") Icon(Icons.Default.Check, contentDescription = null) },
                                onClick = {
                                    onUpdateSettings(settings.copy(themePreset = "Custom"))
                                    presetExpanded = false
                                }
                            )
                        }
                    }
                    Text(locale.getString("settings_desc_theme_preset", "Choose a ready-made color set or customize the colors yourself."), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                    Text(localizedText("Number Notation"), style = MaterialTheme.typography.bodyLarge)
                    Text(locale.getString("settings_desc_number_notation", "Choose how large numbers are displayed: Standard, Scientific or Engineering."), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        locale.getString("notifications_reminders_title", "Push Notifications & Reminders"),
                        style = MaterialTheme.typography.titleMedium
                    )
                    HorizontalDivider()
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text(locale.getString("settings_desc_notifications", "Enable optional game reminders and notifications. You can turn them off at any time."), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                        Text(localizedText("Reminder Frequency"), style = MaterialTheme.typography.bodyMedium)
                        Text(locale.getString("settings_desc_reminder_frequency", "Choose how many hours pass between scheduled reminders."), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                                        text = { Text(locale.getString("hours_format", "%d Hours", hours)) },
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

            if (settings.notificationsEnabled) {
                item {
                    Text(locale.getString("notification_category_title", "Notification Categories"), style = MaterialTheme.typography.titleMedium)
                }
                item {
                    NotificationCategorySwitch(
                        title = locale.getString("notification_category_reminders", "Reminders"),
                        description = locale.getString("notification_category_reminders_desc", "Periodic reminders to return to the game"),
                        checked = settings.notificationRemindersEnabled,
                        onCheckedChange = { onUpdateSettings(settings.copy(notificationRemindersEnabled = it)) }
                    )
                }
                item {
                    NotificationCategorySwitch(
                        title = locale.getString("notification_category_progression", "Progression milestones"),
                        description = locale.getString("notification_category_progression_desc", "World unlocks and boss victories"),
                        checked = settings.notificationProgressionEnabled,
                        onCheckedChange = { onUpdateSettings(settings.copy(notificationProgressionEnabled = it)) }
                    )
                }
                item {
                    NotificationCategorySwitch(
                        title = locale.getString("notification_category_events", "Game events"),
                        description = locale.getString("notification_category_events_desc", "New random events and limited-time event rewards"),
                        checked = settings.notificationEventsEnabled,
                        onCheckedChange = { onUpdateSettings(settings.copy(notificationEventsEnabled = it)) }
                    )
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(locale.getString("sound_fx_label", "Sound FX"), style = MaterialTheme.typography.titleMedium)
                    HorizontalDivider()
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(locale.getString("settings_desc_sound_fx", "Turn sound effects for in-game interactions on or off."), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
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
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(localizedText("Vibration"), style = MaterialTheme.typography.bodyLarge)
                        Text(locale.getString("settings_desc_vibration", "Turn app-triggered vibration during interactions on or off."), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = settings.vibrationEnabled,
                        onCheckedChange = { onUpdateSettings(settings.copy(vibrationEnabled = it)) }
                    )
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        locale.getString("visual_comfort_description", "Visual comfort & accessibility"),
                        style = MaterialTheme.typography.titleMedium
                    )
                    HorizontalDivider()
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text(localizedText("Reduce Flashes"), style = MaterialTheme.typography.bodyLarge)
                        Text(locale.getString("settings_desc_reduce_flashes", "Reduce flashes and sudden visual changes for greater visual comfort."), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = settings.reduceFlashes,
                        onCheckedChange = { onUpdateSettings(settings.copy(reduceFlashes = it)) }
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
                        Text(localizedText("Reduced Motion"), style = MaterialTheme.typography.bodyLarge)
                        Text(locale.getString("settings_desc_reduced_motion", "Soften or disable animated screen transitions."), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = settings.reducedMotion,
                        onCheckedChange = { onUpdateSettings(settings.copy(reducedMotion = it)) }
                    )
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(locale.getString("master_volume_format", "Master Volume: %d%%", (settings.volume * 100).toInt()), style = MaterialTheme.typography.bodyLarge)
                    Text(locale.getString("settings_desc_master_volume", "Set the app’s audio level from silent to maximum."), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Slider(
                        value = settings.volume,
                        onValueChange = { onUpdateSettings(settings.copy(volume = it)) },
                        valueRange = 0f..1f
                    )
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(locale.getString("sfx_volume_format", "Sound Effects Volume: %d%%", (settings.sfxVolume * 100).toInt()), style = MaterialTheme.typography.bodyLarge)
                    Text(locale.getString("settings_desc_sfx_volume", "Adjust sound-effect loudness independently of background music."), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Slider(
                        value = settings.sfxVolume,
                        onValueChange = { onUpdateSettings(settings.copy(sfxVolume = it)) },
                        valueRange = 0f..1f,
                        enabled = settings.soundEnabled
                    )
                }
            }

            item {
                HorizontalDivider()
            }

            item {
                Text(localizedText("Save Data Management"), fontSize = 18.sp, style = MaterialTheme.typography.titleMedium)
            }

            item {
                Text(locale.getString("settings_desc_reset_all_data", "This clears saved game progress. An extra confirmation is required before continuing."), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Button(
                    onClick = {
                        confirmResetCheck = false
                        showResetDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(localizedText("Reset All Game Data"))
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
        ReusableColorPickerDialog(
            initialHex = current,
            title = locale.getString("settings_custom_theme_color", "🎨 Custom Theme Color"),
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
            title = { Text(localizedText("Confirm Data Reset")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(localizedText("Are you sure you want to reset all progress? This action is irreversible."))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Checkbox(
                            checked = confirmResetCheck,
                            onCheckedChange = { confirmResetCheck = it }
                        )
                        Text(localizedText("I understand this will erase all my progress"), fontSize = 14.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onResetData()
                        showResetDialog = false
                        Toast.makeText(context, locale.getString("all_game_data_reset_toast", "All game data has been reset."), Toast.LENGTH_SHORT).show()
                    },
                    enabled = confirmResetCheck,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(localizedText("Reset Everything"))
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text(localizedText("Cancel"))
                }
            }
        )
    }
}



@Composable
private fun NotificationCategorySwitch(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
