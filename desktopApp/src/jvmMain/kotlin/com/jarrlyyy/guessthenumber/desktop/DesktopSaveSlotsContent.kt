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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
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
            val isActive = slot.slot == activeSlot
            val container = if (isActive) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceContainerLow

            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors(containerColor = container)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                "Slot ${slot.slot}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                when {
                                    slot.isCorrupt -> "Needs recovery"
                                    slot.isEmpty -> "Empty slot"
                                    isActive -> "Currently active"
                                    else -> "Saved game"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = when {
                                    slot.isCorrupt -> MaterialTheme.colorScheme.error
                                    isActive -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                        if (isActive && !slot.isCorrupt && !slot.isEmpty) {
                            FilterChip(selected = true, onClick = {}, label = { Text("ACTIVE") })
                        } else if (slot.isCorrupt) {
                            Icon(Icons.Filled.WarningAmber, contentDescription = "Save needs recovery",
                                tint = MaterialTheme.colorScheme.error)
                        } else if (!slot.isEmpty) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = "Save is available",
                                tint = MaterialTheme.colorScheme.primary)
                        }
                    }

                    if (data != null && !slot.isCorrupt) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(data.profileIconId.takeIf { it.isNotBlank() } ?: "🎮", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(data.profileName.ifBlank { "Player" }, fontWeight = FontWeight.SemiBold)
                                Text(data.difficultyId.replaceFirstChar { it.uppercase() } +
                                    if (data.creativeModeEnabled) " · Creative Mode" else "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SaveSlotMetric(Icons.Filled.Numbers, "Hidden number", data.targetNumber.toString(), Modifier.weight(1f))
                            SaveSlotMetric(Icons.Filled.AccountBalanceWallet, "Money", data.money.toString(), Modifier.weight(1f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SaveSlotMetric(Icons.Filled.MilitaryTech, "Prestige", data.prestige.toString(), Modifier.weight(1f))
                            SaveSlotMetric(Icons.Filled.AutoAwesome, "Ultra", data.ultra.toString(), Modifier.weight(1f))
                            SaveSlotMetric(Icons.Filled.AutoAwesome, "Nebula", data.nebula.toString(), Modifier.weight(1f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SaveSlotMetric(Icons.Filled.QueryStats, "Attempts", data.attempts.toString(), Modifier.weight(1f))
                            SaveSlotMetric(Icons.Filled.CheckCircle, "Correct", data.correctGuesses.toString(), Modifier.weight(1f))
                            SaveSlotMetric(Icons.Filled.LocalFireDepartment, "Streak", "${data.streak} · best ${data.bestStreak}", Modifier.weight(1f))
                        }
                        val total = data.statistics.totalGuesses.coerceAtLeast(data.attempts)
                        val correct = data.statistics.correctGuesses.coerceAtLeast(data.correctGuesses)
                        val accuracy = if (total > 0L) (correct * 100.0 / total).coerceIn(0.0, 100.0) else 0.0
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.QueryStats, contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                            Text("Accuracy %.1f%%".format(accuracy), style = MaterialTheme.typography.labelMedium)
                            Text("Failed ${data.statistics.failedGuesses}", style = MaterialTheme.typography.labelMedium)
                            Text("Playtime ${formatPlaytime(data.statistics.playtimeSeconds)}",
                                style = MaterialTheme.typography.labelMedium)
                        }
                        if (data.lastSaveTimestamp > 0L) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Schedule, contentDescription = null,
                                    modifier = Modifier.size(15.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.width(5.dp))
                                Text(
                                    "Saved ${DateFormat.getDateTimeInstance().format(Date(data.lastSaveTimestamp))}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else if (slot.isCorrupt) {
                        Text("This save could not be read. Restore its valid backup if one is available.",
                            color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                    } else {
                        Text("Ready for a new game.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    if (slot.hasBackup) {
                        Text("A recovery backup is available.", style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (slot.isEmpty) {
                            Button(onClick = { onCreateSlot(slot.slot) }) {
                                Icon(Icons.Filled.Add, contentDescription = null)
                                Spacer(Modifier.width(5.dp))
                                Text("Create save")
                            }
                            if (activeState != null) {
                                OutlinedButton(onClick = { onDuplicateSlot(activeSlot, slot.slot) }) {
                                    Icon(Icons.Filled.ContentCopy, contentDescription = null)
                                    Spacer(Modifier.width(5.dp))
                                    Text("Duplicate active")
                                }
                            }
                        } else if (!slot.isCorrupt) {
                            Button(onClick = { onSwitchSlot(slot.slot) }, enabled = !isActive) {
                                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                                Spacer(Modifier.width(5.dp))
                                Text(if (isActive) "Active save" else "Switch to save")
                            }
                            OutlinedButton(onClick = { onExportSlot(slot.slot) }) {
                                Icon(Icons.Filled.FileDownload, contentDescription = null)
                                Spacer(Modifier.width(5.dp))
                                Text("Export")
                            }
                        }
                        OutlinedButton(onClick = { onImportSlot(slot.slot) }) {
                            Icon(Icons.Filled.FileUpload, contentDescription = null)
                            Spacer(Modifier.width(5.dp))
                            Text("Import")
                        }
                        if (slot.hasBackup && !slot.isCorrupt) {
                            IconButton(onClick = { onRestoreBackup(slot.slot) }) {
                                Icon(Icons.Filled.Restore, contentDescription = "Restore backup")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SaveSlotMetric(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)
    )) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(17.dp),
                tint = MaterialTheme.colorScheme.primary)
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

private fun formatPlaytime(seconds: Long): String {
    val hours = seconds.coerceAtLeast(0L) / 3600L
    val minutes = (seconds.coerceAtLeast(0L) % 3600L) / 60L
    return "${hours}h ${minutes}m"
}
