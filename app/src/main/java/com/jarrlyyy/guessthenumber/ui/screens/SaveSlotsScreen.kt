package com.jarrlyyy.guessthenumber.ui.screens

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
import com.jarrlyyy.guessthenumber.domain.model.Difficulty
import com.jarrlyyy.guessthenumber.domain.model.SaveProfile
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
    var showProfileDialog by remember { mutableStateOf<Int?>(null) }
    var profileNameDraft by remember { mutableStateOf("") }
    var profileIconDraft by remember { mutableStateOf(SaveProfile.DEFAULT_ICON) }
    val difficultySelections = remember { mutableStateMapOf<Int, String>() }
    val locale = viewModel.localeManager

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

    if (showProfileDialog != null) {
        val slotNum = showProfileDialog!!
        AlertDialog(
            onDismissRequest = { showProfileDialog = null },
            title = { Text(locale.getString("save_profile_edit_title", "Edit Save Profile")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = profileNameDraft,
                        onValueChange = { profileNameDraft = it.take(24) },
                        label = { Text(locale.getString("save_profile_name", "Profile Name")) },
                        singleLine = true,
                        supportingText = { Text(locale.getString("save_profile_name_limit", "Up to 24 characters")) }
                    )
                    Text(locale.getString("save_profile_icon", "Profile Icon"), style = MaterialTheme.typography.labelLarge)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        SaveProfile.iconIds.forEach { iconId ->
                            FilterChip(
                                selected = profileIconDraft == iconId,
                                onClick = { profileIconDraft = iconId },
                                label = { Text(SaveProfile.iconSymbol(iconId)) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.updateSlotProfile(slotNum, profileNameDraft, profileIconDraft)
                    showProfileDialog = null
                    refreshMetadata()
                }) { Text(locale.getString("save_profile_save", "Save")) }
            },
            dismissButton = {
                TextButton(onClick = { showProfileDialog = null }) {
                    Text(locale.getString("cancel", "Cancel"))
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
                title = { Text(locale.getString("save_slots", "Save Slots"), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    locale.getString("save_slots_description", "Select a save slot to play or manage your games."),
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
                        selectedDifficultyId = difficultySelections[slot] ?: Difficulty.CLASSIC,
                        onDifficultyChange = { difficultySelections[slot] = it },
                        onSelect = {
                            val metadata = slotMetadata[slot - 1]
                            if (metadata?.isEmpty != false) {
                                viewModel.createSlot(slot, difficultySelections[slot] ?: Difficulty.CLASSIC)
                            } else {
                                viewModel.switchSlot(slot)
                            }
                            onNavigateBack()
                        },
                        onReset = { showResetDialog = slot },
                        onEditProfile = {
                            val metadata = slotMetadata[slot - 1]
                            if (metadata != null && !metadata.isEmpty) {
                                profileNameDraft = metadata.profileName
                                profileIconDraft = metadata.profileIconId
                                showProfileDialog = slot
                            }
                        },
                        locale = locale
                    )
                }
            }
        }
    }
}

@Composable
private fun DifficultySelector(
    selectedDifficultyId: String,
    onDifficultyChange: (String) -> Unit,
    locale: com.jarrlyyy.guessthenumber.data.repository.LocaleManager
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = locale.getString(Difficulty.nameKey(selectedDifficultyId), "Classic")

    Box {
        OutlinedButton(onClick = { expanded = true }) {
            Text("${locale.getString("save_slot_difficulty", "Difficulty")}: $selectedName")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            Difficulty.ids.forEach { difficultyId ->
                DropdownMenuItem(
                    text = { Text(locale.getString(Difficulty.nameKey(difficultyId), difficultyId)) },
                    onClick = {
                        onDifficultyChange(difficultyId)
                        expanded = false
                    }
                )
            }
        }
    }
}
@Composable
fun SlotCard(
    slotIndex: Int,
    isActive: Boolean,
    metadata: SaveSlotMetadata?,
    selectedDifficultyId: String,
    onDifficultyChange: (String) -> Unit,
    onSelect: () -> Unit,
    onReset: () -> Unit,
    onEditProfile: () -> Unit,
    locale: com.jarrlyyy.guessthenumber.data.repository.LocaleManager
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
                    text = locale.getString("save_slot_title", "Slot %d", slotIndex) + if (isActive) " ${locale.getString("save_slot_active", "(Active)")}" else "",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (metadata != null && !metadata.isEmpty) {
                    Row {
                        IconButton(onClick = onEditProfile) {
                            Icon(Icons.Default.Edit, contentDescription = locale.getString("save_profile_edit", "Edit Profile"))
                        }
                        IconButton(onClick = onReset) {
                        Icon(Icons.Default.Delete, contentDescription = locale.getString("save_slot_reset", "Reset Slot", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            if (metadata == null || metadata.isEmpty) {
                Text(
                    text = locale.getString("save_slot_empty", "Empty Slot"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = onSelect,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(locale.getString("save_slot_start_new", "Start New Game"))
                }
            } else {
                val profileDisplayName = metadata.profileName.ifBlank {
                    locale.getString("save_slot_title", "Slot %d", slotIndex)
                }
                Text(
                    text = "${SaveProfile.iconSymbol(metadata.profileIconId)}  $profileDisplayName",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(text = "${locale.getString("save_slot_difficulty", "Difficulty")}: ${locale.getString(Difficulty.nameKey(metadata.difficultyId), "Classic")}", style = MaterialTheme.typography.bodyMedium)
                Text(text = locale.getString("save_slot_money", "Money: %s", metadata.money), style = MaterialTheme.typography.bodyMedium)
                Text(text = locale.getString("save_slot_prestige_ultra", "Prestige: %s | Ultra: %s", metadata.prestige, metadata.ultra), style = MaterialTheme.typography.bodySmall)
                Text(text = locale.getString("save_slot_attempts_correct", "Attempts: %d | Correct: %d", metadata.attempts, metadata.correctGuesses), style = MaterialTheme.typography.bodySmall)
                if (metadata.lastSaveTimestamp > 0) {
                    Text(
                        text = locale.getString("save_slot_last_saved", "Last Saved: %s", dateFormat.format(Date(metadata.lastSaveTimestamp))),
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
                    Text(if (isActive) locale.getString("save_slot_resume", "Resume Game") else locale.getString("save_slot_switch", "Switch to Slot"))
                }
            }
        }
    }
}
