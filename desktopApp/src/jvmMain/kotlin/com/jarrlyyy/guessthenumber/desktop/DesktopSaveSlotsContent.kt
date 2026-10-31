package com.jarrlyyy.guessthenumber.desktop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jarrlyyy.guessthenumber.desktop.persistence.DesktopSaveSlotSummary
import java.text.DateFormat
import java.util.Date

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
    onContinueWithActiveSlot: (() -> Unit)? = null
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onBack) { Text("Close") }
            Text("Save Slots", style = MaterialTheme.typography.headlineSmall)
            OutlinedButton(onClick = onRefresh) { Text("Refresh") }
            if (onContinueWithActiveSlot != null) {
                Button(onClick = onContinueWithActiveSlot) { Text("Continue with active slot") }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onExportAllSlots) { Text("Export all slots") }
            OutlinedButton(onClick = onImportAllSlots) { Text("Import all slots") }
        }
        Text("Importing an all-slot bundle replaces all ten local slots. A recovery copy of each previous save is kept where possible.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        Text("Ten independent local saves. Switching saves the current slot first. Valid previous saves can be restored.")
        Text("Portable backups are plain JSON files compatible with Android single-save import. They are not encrypted.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        slots.forEach { slot ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Slot ${slot.slot}${if (slot.slot == activeSlot) " · Active" else ""}", style = MaterialTheme.typography.titleMedium)
                        when {
                            slot.isCorrupt -> Text("Needs recovery", color = MaterialTheme.colorScheme.error)
                            slot.isEmpty -> Text("Empty", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            else -> Text("Saved", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    val data = slot.state
                    if (data != null) {
                        Text(data.profileName.ifBlank { "Player" } + " · " + data.difficultyId.replaceFirstChar { it.uppercase() })
                        Text("Money ${data.money} · Prestige ${data.prestige} · Ultra ${data.ultra}")
                        Text("Attempts ${data.attempts} · Correct ${data.correctGuesses}")
                        if (data.lastSaveTimestamp > 0L) Text("Last saved: ${DateFormat.getDateTimeInstance().format(Date(data.lastSaveTimestamp))}")
                    }
                    if (slot.hasBackup) Text("A valid recovery backup is available.", color = MaterialTheme.colorScheme.primary)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (!slot.isEmpty && !slot.isCorrupt) OutlinedButton(onClick = { onExportSlot(slot.slot) }) { Text("Export JSON") }
                        OutlinedButton(onClick = { onImportSlot(slot.slot) }) { Text("Import JSON") }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (slot.isEmpty) {
                            Button(onClick = { onCreateSlot(slot.slot) }) { Text("Create slot") }
                            if (slots.firstOrNull { it.slot == activeSlot }?.state != null) OutlinedButton(onClick = { onDuplicateSlot(activeSlot, slot.slot) }) { Text("Duplicate active here") }
                        }
                        else if (!slot.isCorrupt && slot.slot != activeSlot) Button(onClick = { onSwitchSlot(slot.slot) }) { Text("Switch to slot") }
                        if (!slot.isEmpty && !slot.isCorrupt && slot.slot != activeSlot) {
                            OutlinedButton(onClick = { onDuplicateSlot(activeSlot, slot.slot) }) { Text("Duplicate active here") }
                        }
                        if (slot.hasBackup && !slot.isCorrupt) OutlinedButton(onClick = { onRestoreBackup(slot.slot) }) { Text("Restore backup") }
                    }
                }
            }
        }
    }
}
