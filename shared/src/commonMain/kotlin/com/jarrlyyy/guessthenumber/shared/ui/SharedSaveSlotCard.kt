package com.jarrlyyy.guessthenumber.shared.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Platform-neutral save-slot presentation model. Values are already formatted by the host. */
data class SharedSaveSlotUiModel(
    val slotIndex: Int,
    val isActive: Boolean,
    val isEmpty: Boolean,
    val isCorrupt: Boolean = false,
    val hasBackup: Boolean = false,
    val profileTitle: String? = null,
    val difficultyLabel: String? = null,
    val primaryMetric: String? = null,
    val secondaryMetrics: List<String> = emptyList(),
    val lastSavedLabel: String? = null,
    val activeLabel: String = "ACTIVE",
    val emptyLabel: String = "Empty slot",
    val corruptLabel: String = "Needs recovery",
    val createLabel: String = "Create save",
    val resumeLabel: String = "Resume game",
    val switchLabel: String = "Switch to save"
)

/**
 * Canonical compact Save Slots card shared by Android and Windows.
 * Persistence, validation and platform actions stay in the host callbacks.
 */
@Composable
fun SharedSaveSlotCard(
    model: SharedSaveSlotUiModel,
    onSelect: () -> Unit,
    onCreate: (() -> Unit)? = null,
    onEditProfile: (() -> Unit)? = null,
    onDuplicate: (() -> Unit)? = null,
    onRestoreBackup: (() -> Unit)? = null,
    onReset: (() -> Unit)? = null,
    onExport: (() -> Unit)? = null,
    onImport: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val containerColor = if (model.isActive) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    Card(
        modifier = modifier.fillMaxWidth().animateContentSize(),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        "Slot ${model.slotIndex}" + if (model.isActive) " (${model.activeLabel})" else "",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        when {
                            model.isCorrupt -> model.corruptLabel
                            model.isEmpty -> model.emptyLabel
                            model.isActive -> "Currently active"
                            else -> "Saved game"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = when {
                            model.isCorrupt -> MaterialTheme.colorScheme.error
                            model.isActive -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
                if (model.isCorrupt) {
                    Icon(Icons.Default.WarningAmber, contentDescription = model.corruptLabel,
                        tint = MaterialTheme.colorScheme.error)
                } else if (!model.isEmpty) {
                    Icon(Icons.Default.CheckCircle, contentDescription = "Save is available",
                        tint = MaterialTheme.colorScheme.primary)
                }
            }

            if (model.isEmpty || model.isCorrupt) {
                Text(
                    if (model.isCorrupt) "This save could not be read. Restore a valid backup if available."
                    else model.emptyLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (model.isCorrupt) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                model.profileTitle?.let {
                    Text(it, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }
                model.difficultyLabel?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                model.primaryMetric?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium)
                }
                model.secondaryMetrics.forEach {
                    Text(it, style = MaterialTheme.typography.bodySmall)
                }
                model.lastSavedLabel?.let {
                    Text(it, style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            if (model.hasBackup && !model.isCorrupt) {
                Text("A recovery backup is available.", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                onEditProfile?.let {
                    IconButton(onClick = it) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit profile")
                    }
                }
                onDuplicate?.let {
                    IconButton(onClick = it) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Duplicate save")
                    }
                }
                onRestoreBackup?.let {
                    IconButton(onClick = it) {
                        Icon(Icons.Default.Restore, contentDescription = "Restore backup")
                    }
                }
                onReset?.let {
                    IconButton(onClick = it) {
                        Icon(Icons.Default.Delete, contentDescription = "Reset save",
                            tint = MaterialTheme.colorScheme.error)
                    }
                }
                onExport?.let {
                    IconButton(onClick = it) {
                        Icon(Icons.Default.FileDownload, contentDescription = "Export save")
                    }
                }
                onImport?.let {
                    IconButton(onClick = it) {
                        Icon(Icons.Default.FileUpload, contentDescription = "Import save")
                    }
                }
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = if (model.isEmpty) (onCreate ?: onSelect) else onSelect,
                    enabled = !model.isCorrupt,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (model.isActive) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Icon(if (model.isEmpty) Icons.Default.Add else Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text(if (model.isEmpty) model.createLabel else if (model.isActive) model.resumeLabel else model.switchLabel)
                }
            }
        }
    }
}
