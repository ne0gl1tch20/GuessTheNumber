package com.jarrlyyy.guessthenumber.desktop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Switch
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.jarrlyyy.guessthenumber.shared.domain.SaveProfileRules
import com.jarrlyyy.guessthenumber.shared.domain.CreativeModeSave
import com.jarrlyyy.guessthenumber.domain.model.Difficulty
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jarrlyyy.guessthenumber.desktop.persistence.DesktopSaveSlotSummary
import com.jarrlyyy.guessthenumber.shared.ui.SharedSaveSlotCard
import com.jarrlyyy.guessthenumber.shared.ui.SharedSaveSlotUiModel
import com.jarrlyyy.guessthenumber.shared.ui.SharedSaveSlotMetric
import com.jarrlyyy.guessthenumber.shared.ui.SharedSaveSlotMetricIcon
import java.text.DateFormat
import java.util.Date
import kotlinx.coroutines.delay

internal data class DesktopSaveCreationOptions(
    val name: String, val iconId: String, val difficultyId: String,
    val creativeMode: Boolean, val rangeMin: Long, val rangeMax: Long,
    val extraHints: Int, val attemptLimit: Int
)

/**
 * Compose-first desktop save manager. The layout intentionally mirrors the
 * Android save-slot card hierarchy while using the desktop persistence callbacks.
 */
@Composable
internal fun DesktopSaveSlotsContent(
    activeSlot: Int,
    slots: List<DesktopSaveSlotSummary>,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onSwitchSlot: (Int) -> Unit,
    onCreateSlot: (Int, DesktopSaveCreationOptions) -> Unit,
    onDuplicateSlot: (Int, Int) -> Unit,
    onRestoreBackup: (Int) -> Unit,
    onEditProfile: (Int, String, String) -> Unit,
    onResetSlot: (Int) -> Unit,
    onExportSlot: (Int) -> Unit,
    onImportSlot: (Int) -> Unit,
    onExportAllSlots: () -> Unit,
    onImportAllSlots: () -> Unit,
    onExportAndroidBackup: () -> Unit,
    onImportAndroidBackup: () -> Unit,
    onResetEverything: () -> Unit = {},
    onContinueWithActiveSlot: (() -> Unit)? = null,
    showCloseButton: Boolean = true
) {
    val activeState = slots.firstOrNull { it.slot == activeSlot }?.state
    var editingSlot by remember { mutableStateOf<Int?>(null) }
    var profileNameDraft by remember { mutableStateOf("") }
    var profileIconDraft by remember { mutableStateOf(SaveProfileRules.DEFAULT_ICON) }
    var resettingSlot by remember { mutableStateOf<Int?>(null) }
    var creatingSlot by remember { mutableStateOf<Int?>(null) }
    var createName by remember { mutableStateOf("") }
    var createIcon by remember { mutableStateOf(SaveProfileRules.DEFAULT_ICON) }
    var createDifficulty by remember { mutableStateOf(Difficulty.CLASSIC) }
    var creativeEnabled by remember { mutableStateOf(false) }
    var creativeMax by remember { mutableStateOf("100") }
    var creativeHints by remember { mutableStateOf("0") }
    var creativeAttemptLimit by remember { mutableStateOf("0") }
    var showResetEverything by remember { mutableStateOf(false) }
    var resetEverythingAcknowledged by remember { mutableStateOf(false) }
    var resetChallengeAnswer by remember { mutableStateOf("") }
    var resetSecondsRemaining by remember { mutableIntStateOf(45) }
    val resetChallenge = remember { (12..48).random() to (2..19).random() }

    LaunchedEffect(showResetEverything) {
        while (showResetEverything && resetSecondsRemaining > 0) {
            delay(1_000)
            resetSecondsRemaining--
        }
    }

    if (editingSlot != null) {
        AlertDialog(
            onDismissRequest = { editingSlot = null },
            title = { Text("Edit save profile") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = profileNameDraft,
                        onValueChange = { profileNameDraft = it.take(24) },
                        label = { Text("Profile name") },
                        singleLine = true
                    )
                    Text("Profile icon")
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        SaveProfileRules.iconIds.forEach { iconId ->
                            OutlinedButton(
                                onClick = { profileIconDraft = iconId },
                                enabled = profileIconDraft != iconId
                            ) {
                                Text(SaveProfileRules.iconSymbol(iconId))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val slot = editingSlot ?: return@TextButton
                        onEditProfile(slot, profileNameDraft.trim().take(24), profileIconDraft)
                        editingSlot = null
                    },
                    enabled = SaveProfileRules.isValidIcon(profileIconDraft)
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { editingSlot = null }) { Text("Cancel") }
            }
        )
    }

    if (creatingSlot != null) {
        val slot = creatingSlot!!
        val max = creativeMax.toLongOrNull()
        val hints = creativeHints.toIntOrNull()
        val attempts = creativeAttemptLimit.toIntOrNull()
        val validCreative = !creativeEnabled || (max != null && max in 1L..CreativeModeSave.MAX_RANGE &&
            hints != null && hints in 0..5 && attempts != null && attempts in 0..100)
        AlertDialog(
            onDismissRequest = { creatingSlot = null },
            title = { Text("Create Save Slot $slot") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = createName, onValueChange = { createName = it.take(24) },
                        label = { Text("Save name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Text("Profile icon", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        SaveProfileRules.iconIds.forEach { id ->
                            FilterChip(selected = createIcon == id, onClick = { createIcon = id },
                                label = { Text(SaveProfileRules.iconSymbol(id)) })
                        }
                    }
                    Text("Difficulty", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Difficulty.ids.forEach { id ->
                            FilterChip(selected = createDifficulty == id, onClick = { createDifficulty = id },
                                label = { Text(id.replaceFirstChar(Char::uppercase)) })
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Creative Mode", style = MaterialTheme.typography.titleSmall)
                            Text("Permanent for this save. It cannot be disabled later.",
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = creativeEnabled, onCheckedChange = { creativeEnabled = it })
                    }
                    if (creativeEnabled) {
                        OutlinedTextField(value = creativeMax, onValueChange = { creativeMax = it.filter(Char::isDigit).take(7) },
                            label = { Text("Number range maximum (1–1,000,000)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = creativeHints, onValueChange = { creativeHints = it.filter(Char::isDigit).take(2) },
                            label = { Text("Extra hints (0–5)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = creativeAttemptLimit, onValueChange = { creativeAttemptLimit = it.filter(Char::isDigit).take(3) },
                            label = { Text("Attempt limit (0 = unlimited, 1–100)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        if (!validCreative) Text("Check the Creative Mode values and try again.", color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    onCreateSlot(slot, DesktopSaveCreationOptions(
                        name = createName.trim().ifBlank { "Slot $slot" }, iconId = createIcon,
                        difficultyId = createDifficulty, creativeMode = creativeEnabled, rangeMin = 1L,
                        rangeMax = max ?: 100L, extraHints = hints ?: 0, attemptLimit = attempts ?: 0
                    ))
                    creatingSlot = null
                }, enabled = SaveProfileRules.isValidIcon(createIcon) && Difficulty.isValid(createDifficulty) && validCreative) {
                    Text("Create save")
                }
            },
            dismissButton = { TextButton(onClick = { creatingSlot = null }) { Text("Cancel") } }
        )
    }

    if (showResetEverything) {
        AlertDialog(
            onDismissRequest = { showResetEverything = false },
            title = { Text("Reset Everything?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("This permanently clears every save slot and its recovery backup. This cannot be undone.")
                    Text("${resetChallenge.first} + ${resetChallenge.second} = ?", style = MaterialTheme.typography.headlineSmall)
                    OutlinedTextField(value = resetChallengeAnswer,
                        onValueChange = { resetChallengeAnswer = it.filter(Char::isDigit).take(3) },
                        label = { Text("Solve the confirmation") }, singleLine = true)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(checked = resetEverythingAcknowledged, onCheckedChange = { resetEverythingAcknowledged = it })
                        Text("I understand all save data will be erased.", style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onResetEverything()
                    showResetEverything = false
                    resetEverythingAcknowledged = false
                    resetChallengeAnswer = ""
                }, enabled = resetEverythingAcknowledged && resetSecondsRemaining == 0 &&
                    resetChallengeAnswer.toIntOrNull() == resetChallenge.first + resetChallenge.second) {
                    Text(if (resetSecondsRemaining > 0) "Wait ${resetSecondsRemaining}s" else "Reset everything",
                        color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { showResetEverything = false }) { Text("Cancel") } }
        )
    }

    if (resettingSlot != null) {
        val slot = resettingSlot!!
        AlertDialog(
            onDismissRequest = { resettingSlot = null },
            title = { Text("Reset Slot $slot?") },
            text = { Text("This permanently resets the selected save slot. Its current progress and recovery backup will be replaced.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onResetSlot(slot)
                        resettingSlot = null
                    }
                ) { Text("Reset", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { resettingSlot = null }) { Text("Cancel") }
            }
        )
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Save Slots", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Select a save slot to play or manage your games. Each slot keeps its own progress, profile and recovery backup.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        // Keep the action bar usable when the user narrows the desktop popup.
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            if (maxWidth < 760.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(onClick = onRefresh, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Filled.Refresh, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Refresh")
                        }
                        OutlinedButton(onClick = onExportAllSlots, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Filled.FileDownload, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Export")
                        }
                        OutlinedButton(onClick = onImportAllSlots, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Filled.FileUpload, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Import")
                        }
                    }
                    if (onContinueWithActiveSlot != null) {
                        Button(
                            onClick = onContinueWithActiveSlot,
                            enabled = activeState != null,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Continue with active save")
                        }
                    } else if (showCloseButton) {
                        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                            Text("Close")
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(onClick = onRefresh) {
                        Icon(Icons.Filled.Refresh, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Refresh")
                    }
                    OutlinedButton(onClick = onExportAllSlots) {
                        Icon(Icons.Filled.FileDownload, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Export all")
                    }
                    OutlinedButton(onClick = onImportAllSlots) {
                        Icon(Icons.Filled.FileUpload, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Import all")
                    }
                    Spacer(Modifier.weight(1f))
                    if (onContinueWithActiveSlot != null) {
                        Button(onClick = onContinueWithActiveSlot, enabled = activeState != null) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Continue")
                        }
                    } else if (showCloseButton) {
                        OutlinedButton(onClick = onBack) { Text("Close") }
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(onClick = onExportAndroidBackup, modifier = Modifier.weight(1f)) {
                Icon(Icons.Filled.FileDownload, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Export encrypted save")
            }
            OutlinedButton(onClick = onImportAndroidBackup, modifier = Modifier.weight(1f)) {
                Icon(Icons.Filled.FileUpload, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Import encrypted save")
            }
        }

        slots.sortedBy { it.slot }.forEach { slot ->
            val data = slot.state
            val total = data?.let { it.statistics.totalGuesses.coerceAtLeast(it.attempts) } ?: 0L
            val correct = data?.let { it.statistics.correctGuesses.coerceAtLeast(it.correctGuesses) } ?: 0L
            val accuracy = if (total > 0L) (correct * 100.0 / total).coerceIn(0.0, 100.0) else 0.0
            val model = SharedSaveSlotUiModel(
                slotIndex = slot.slot,
                isActive = slot.slot == activeSlot,
                isEmpty = slot.isEmpty,
                isCorrupt = slot.isCorrupt,
                hasBackup = slot.hasBackup,
                profileTitle = data?.takeUnless { slot.isCorrupt }?.let {
                    "${SaveProfileRules.iconSymbol(it.profileIconId)}  ${it.profileName.ifBlank { "Player" }}"
                },
                difficultyLabel = data?.takeUnless { slot.isCorrupt }?.let {
                    it.difficultyId.replaceFirstChar(Char::uppercase) +
                        if (it.creativeModeEnabled) " · Creative Mode" else ""
                },
                metricColumns = 3,
                metricItems = data?.takeUnless { slot.isCorrupt }?.let {
                    listOf(
                        SharedSaveSlotMetric(SharedSaveSlotMetricIcon.TARGET, "Hidden number: ${it.targetNumber}"),
                        SharedSaveSlotMetric(SharedSaveSlotMetricIcon.MONEY, "Money: ${it.money}"),
                        SharedSaveSlotMetric(SharedSaveSlotMetricIcon.PRESTIGE, "Prestige: ${it.prestige}"),
                        SharedSaveSlotMetric(SharedSaveSlotMetricIcon.ULTRA, "Ultra: ${it.ultra}"),
                        SharedSaveSlotMetric(SharedSaveSlotMetricIcon.NEBULA, "Nebula: ${it.nebula}"),
                        SharedSaveSlotMetric(SharedSaveSlotMetricIcon.ATTEMPTS, "Attempts: ${it.attempts}"),
                        SharedSaveSlotMetric(SharedSaveSlotMetricIcon.CORRECT, "Correct: ${it.correctGuesses}"),
                        SharedSaveSlotMetric(SharedSaveSlotMetricIcon.STREAK, "Streak: ${it.streak} · Best: ${it.bestStreak}"),
                        SharedSaveSlotMetric(SharedSaveSlotMetricIcon.STATISTICS, "Accuracy %.1f%% · Failed ${it.statistics.failedGuesses} · Playtime ${formatPlaytime(it.statistics.playtimeSeconds)}".format(accuracy))
                    )
                }.orEmpty(),
                lastSavedLabel = data?.takeIf { !slot.isCorrupt && it.lastSaveTimestamp > 0L }?.let {
                    "Saved ${DateFormat.getDateTimeInstance().format(Date(it.lastSaveTimestamp))}"
                },
                activeLabel = "Active",
                emptyLabel = "Empty slot",
                corruptLabel = "Needs recovery",
                createLabel = "Create save",
                resumeLabel = "Active save",
                switchLabel = "Switch to save"
            )

            SharedSaveSlotCard(
                model = model,
                onSelect = {
                    if (slot.isEmpty) {
                        creatingSlot = slot.slot; createName = ""; createIcon = SaveProfileRules.DEFAULT_ICON
                        createDifficulty = Difficulty.CLASSIC; creativeEnabled = false; creativeMax = "100"
                        creativeHints = "0"; creativeAttemptLimit = "0"
                    } else onSwitchSlot(slot.slot)
                },
                onCreate = {
                    creatingSlot = slot.slot; createName = ""; createIcon = SaveProfileRules.DEFAULT_ICON
                    createDifficulty = Difficulty.CLASSIC; creativeEnabled = false; creativeMax = "100"
                    creativeHints = "0"; creativeAttemptLimit = "0"
                },
                onEditProfile = if (!slot.isEmpty && !slot.isCorrupt) {
                    {
                        profileNameDraft = data?.profileName.orEmpty()
                        profileIconDraft = data?.profileIconId?.takeIf(SaveProfileRules::isValidIcon)
                            ?: SaveProfileRules.DEFAULT_ICON
                        editingSlot = slot.slot
                    }
                } else null,
                onReset = if (!slot.isEmpty && !slot.isCorrupt) {
                    { resettingSlot = slot.slot }
                } else null,
                onDuplicate = if (slot.isEmpty && activeState != null) {
                    { onDuplicateSlot(activeSlot, slot.slot) }
                } else null,
                onRestoreBackup = if (slot.hasBackup && !slot.isCorrupt) {
                    { onRestoreBackup(slot.slot) }
                } else null,
                onExport = if (!slot.isEmpty && !slot.isCorrupt) {
                    { onExportSlot(slot.slot) }
                } else null,
                onImport = { onImportSlot(slot.slot) }
            )
        }

        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Reset Everything", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
                Text("Erase all save slots and their recovery backups. An extra confirmation is required.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Button(onClick = {
                    resetChallengeAnswer = ""; resetEverythingAcknowledged = false
                    resetSecondsRemaining = 45; showResetEverything = true
                }, modifier = Modifier.fillMaxWidth()) { Text("Reset Everything") }
            }
        }
    }
}

private fun formatPlaytime(seconds: Long): String {
    val hours = seconds.coerceAtLeast(0L) / 3600L
    val minutes = (seconds.coerceAtLeast(0L) % 3600L) / 60L
    return "${hours}h ${minutes}m"
}
