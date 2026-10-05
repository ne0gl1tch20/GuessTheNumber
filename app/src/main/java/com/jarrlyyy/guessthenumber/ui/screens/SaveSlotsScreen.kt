package com.jarrlyyy.guessthenumber.ui.screens

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jarrlyyy.guessthenumber.data.store.SaveSlotMetadata
import com.jarrlyyy.guessthenumber.data.store.MAX_SAVE_SLOTS
import com.jarrlyyy.guessthenumber.ui.viewmodel.GameViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaveSlotsScreen(
    viewModel: GameViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current as? Activity
    val scope = rememberCoroutineScope()
    val activeSlot by viewModel.activeSlot.collectAsState()
    val hasLegacySave by viewModel.hasLegacySave.collectAsState()

    val slotMetadata = remember {
        mutableStateListOf<SaveSlotMetadata?>().apply {
            repeat(MAX_SAVE_SLOTS) { add(null) }
        }
    }

    var showResetDialog by remember { mutableStateOf<Int?>(null) }

    val refreshMetadata = {
        scope.launch {
            for (slot in 1..MAX_SAVE_SLOTS) {
                slotMetadata[slot - 1] = viewModel.getSlotMetadata(slot)
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshMetadata()
    }

    // Legacy migration dialog prompt
    if (hasLegacySave) {
        AlertDialog(
            onDismissRequest = {
                // If dismissed without confirming, close app with no okay
                context?.finish()
            },
            title = { Text("Legacy Save Detected") },
            text = { Text("We detected an existing single-save from a previous version of Guess The Number. Would you like to transfer your save into Slot 1?\n\nIf you decline, the app will close.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.migrateLegacySave()
                        refreshMetadata()
                    }
                ) {
                    Text("Transfer to Slot 1")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        context?.finish()
                    }
                ) {
                    Text("Decline & Close App")
                }
            }
        )
    }

    if (showResetDialog != null) {
        val slotNum = showResetDialog!!
        AlertDialog(
            onDismissRequest = { showResetDialog = null },
            title = { Text("Reset Slot $slotNum?") },
            text = { Text("Are you sure you want to reset Slot $slotNum? All progress in this slot will be permanently lost.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.resetSlot(slotNum)
                        showResetDialog = null
                        refreshMetadata()
                    }
                ) {
                    Text("Reset", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Save Slots", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
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
                Text(
                    "Select a save slot to play or manage your games.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            for (slot in 1..MAX_SAVE_SLOTS) {
                item {
                    SlotCard(
                        slotIndex = slot,
                        isActive = activeSlot == slot,
                        metadata = slotMetadata[slot - 1],
                        onSelect = {
                            viewModel.switchSlot(slot)
                            onNavigateBack()
                        },
                        onReset = { showResetDialog = slot }
                    )
                }
            }
        }
    }
}

@Composable
fun SlotCard(
    slotIndex: Int,
    isActive: Boolean,
    metadata: SaveSlotMetadata?,
    onSelect: () -> Unit,
    onReset: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Slot $slotIndex${if (isActive) " (Active)" else ""}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (metadata != null && !metadata.isEmpty) {
                    IconButton(onClick = onReset) {
                        Icon(Icons.Default.Delete, contentDescription = "Reset Slot", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }

            if (metadata == null || metadata.isEmpty) {
                Text(
                    text = "Empty Slot",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = onSelect,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Start New Game")
                }
            } else {
                Text(text = "Money: ${metadata.money}", style = MaterialTheme.typography.bodyMedium)
                Text(text = "Prestige: ${metadata.prestige} | Ultra: ${metadata.ultra}", style = MaterialTheme.typography.bodySmall)
                Text(text = "Attempts: ${metadata.attempts} | Correct: ${metadata.correctGuesses}", style = MaterialTheme.typography.bodySmall)
                if (metadata.lastSaveTimestamp > 0) {
                    Text(
                        text = "Last Saved: ${dateFormat.format(Date(metadata.lastSaveTimestamp))}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = onSelect,
                    modifier = Modifier.align(Alignment.End),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isActive) "Resume Game" else "Switch to Slot")
                }
            }
        }
    }
}
