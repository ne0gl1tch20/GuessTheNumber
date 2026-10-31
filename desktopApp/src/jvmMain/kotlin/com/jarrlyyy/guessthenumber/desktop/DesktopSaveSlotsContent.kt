package com.jarrlyyy.guessthenumber.desktop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import java.text.DateFormat
import java.util.Date

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
    onCreateSlot: (Int) -> Unit,
    onDuplicateSlot: (Int, Int) -> Unit,
    onRestoreBackup: (Int) -> Unit,
    onExportSlot: (Int) -> Unit,
    onImportSlot: (Int) -> Unit,
    onExportAllSlots: () -> Unit,
    onImportAllSlots: () -> Unit,
    onContinueWithActiveSlot: (() -> Unit)? = null,
    showCloseButton: Boolean = true
) {
    val activeState = slots.firstOrNull { it.slot == activeSlot }?.state

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // The window already says Save Slots. Keep this row action-focused.
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

        slots.sortedBy { it.slot }.forEach { slot ->
            val data = slot.state
            val total = data?.statistics?.totalGuesses?.coerceAtLeast(data.attempts) ?: 0L
            val correct = data?.statistics?.correctGuesses?.coerceAtLeast(data.correctGuesses) ?: 0L
            val accuracy = if (total > 0L) (correct * 100.0 / total).coerceIn(0.0, 100.0) else 0.0
            val model = SharedSaveSlotUiModel(
                slotIndex = slot.slot,
                isActive = slot.slot == activeSlot,
                isEmpty = slot.isEmpty,
                isCorrupt = slot.isCorrupt,
                hasBackup = slot.hasBackup,
                profileTitle = data?.takeUnless { slot.isCorrupt }?.let {
                    "${it.profileIconId.takeIf(String::isNotBlank) ?: "🎮"}  ${it.profileName.ifBlank { "Player" }}"
                },
                difficultyLabel = data?.takeUnless { slot.isCorrupt }?.let {
                    it.difficultyId.replaceFirstChar(Char::uppercase) +
                        if (it.creativeModeEnabled) " · Creative Mode" else ""
                },
                primaryMetric = data?.takeUnless { slot.isCorrupt }?.let {
                    "Hidden number: ${it.targetNumber} · Money: ${it.money}"
                },
                secondaryMetrics = data?.takeUnless { slot.isCorrupt }?.let {
                    listOf(
                        "Prestige: ${it.prestige} · Ultra: ${it.ultra} · Nebula: ${it.nebula}",
                        "Attempts: ${it.attempts} · Correct: ${it.correctGuesses} · Streak: ${it.streak} (best ${it.bestStreak})",
                        "Accuracy %.1f%% · Failed ${it.statistics.failedGuesses} · Playtime ${formatPlaytime(it.statistics.playtimeSeconds)}".format(accuracy)
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
                    if (slot.isEmpty) onCreateSlot(slot.slot) else onSwitchSlot(slot.slot)
                },
                onCreate = { onCreateSlot(slot.slot) },
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
    }
}

private fun formatPlaytime(seconds: Long): String {
    val hours = seconds.coerceAtLeast(0L) / 3600L
    val minutes = (seconds.coerceAtLeast(0L) % 3600L) / 60L
    return "${hours}h ${minutes}m"
}
