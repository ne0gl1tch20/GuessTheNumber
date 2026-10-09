package com.jarrlyyy.guessthenumber.ui.screens

import com.jarrlyyy.guessthenumber.ui.localization.localizedText

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.interaction.MutableInteractionSource
import com.jarrlyyy.guessthenumber.ui.theme.expressivePressScale
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
    val gameState by viewModel.gameState.collectAsState()

    val slotMetadata = remember {
        mutableStateListOf<SaveSlotMetadata?>().apply {
            repeat(MAX_SAVE_SLOTS) { add(null) }
        }
    }

    var showResetDialog by remember { mutableStateOf<Int?>(null) }
    var showProfileDialog by remember { mutableStateOf<Int?>(null) }
    var showCreateDialog by remember { mutableStateOf<Int?>(null) }
    var showDuplicateDialog by remember { mutableStateOf<Int?>(null) }
    var showRestoreDialog by remember { mutableStateOf<Int?>(null) }
    var showImportDialog by remember { mutableStateOf(false) }
    var importString by remember { mutableStateOf("") }
    var duplicateTarget by remember { mutableStateOf(0) }
    var profileNameDraft by remember { mutableStateOf("") }
    var profileIconDraft by remember { mutableStateOf(SaveProfile.DEFAULT_ICON) }
    var createNameDraft by remember { mutableStateOf("") }
    var createIconDraft by remember { mutableStateOf(SaveProfile.DEFAULT_ICON) }
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
            title = { Text(locale.getString("legacy_save_title", "Legacy Save Detected")) },
            text = { Text(locale.getString("legacy_save_description", "We detected an existing single-save from a previous version of Guess The Number. Would you like to transfer your save into Slot 1?\n\nIf you decline, the app will close.")) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.migrateLegacySave()
                        refreshMetadata()
                    }
                ) {
                    Text(locale.getString("legacy_save_transfer", "Transfer to Slot 1"))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        context?.finish()
                    }
                ) {
                    Text(locale.getString("legacy_save_decline", "Decline & Close App"))
                }
            }
        )
    }

    if (showDuplicateDialog != null) {
        val sourceSlot = showDuplicateDialog!!
        val emptyTargets = (1..MAX_SAVE_SLOTS).filter { slotMetadata[it - 1]?.isEmpty != false && it != sourceSlot }
        val target = if (duplicateTarget in emptyTargets) duplicateTarget else emptyTargets.firstOrNull() ?: 0
        AlertDialog(
            onDismissRequest = { showDuplicateDialog = null },
            title = { Text(locale.getString("save_slot_duplicate_title", "Duplicate Slot %d", sourceSlot)) },
            text = {
                if (emptyTargets.isEmpty()) {
                    Text(locale.getString("save_slot_duplicate_no_targets", "No empty slots are available."))
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(locale.getString("save_slot_duplicate_description", "Choose an empty slot to copy this save into."))
                        var expanded by remember { mutableStateOf(false) }
                        Box {
                            OutlinedButton(onClick = { expanded = true }) {
                                Text(locale.getString("save_slot_title", "Slot %d", target))
                            }
                            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                emptyTargets.forEach { slot ->
                                    DropdownMenuItem(
                                        text = { Text(locale.getString("save_slot_title", "Slot %d", slot)) },
                                        onClick = { duplicateTarget = slot; expanded = false }
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = emptyTargets.isNotEmpty() && target in emptyTargets,
                    onClick = {
                        viewModel.duplicateSlot(sourceSlot, target) { refreshMetadata() }
                        showDuplicateDialog = null
                    }
                ) { Text(locale.getString("save_slot_duplicate_confirm", "Duplicate")) }
            },
            dismissButton = {
                TextButton(onClick = { showDuplicateDialog = null }) {
                    Text(locale.getString("cancel", "Cancel"))
                }
            }
        )
    }

    if (showRestoreDialog != null) {
        val slotNum = showRestoreDialog!!
        AlertDialog(
            onDismissRequest = { showRestoreDialog = null },
            title = { Text(locale.getString("save_slot_restore_title", "Restore Slot %d Backup?", slotNum)) },
            text = { Text(locale.getString("save_slot_restore_description", "Restore the previous backup for Slot %d? Your current save will become the backup.", slotNum)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.restoreBackup(slotNum) { refreshMetadata() }
                    showRestoreDialog = null
                }) { Text(locale.getString("save_slot_restore_confirm", "Restore")) }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreDialog = null }) {
                    Text(locale.getString("cancel", "Cancel"))
                }
            }
        )
    }

    if (showCreateDialog != null) {
        val slotNum = showCreateDialog!!
        var difficultyDraft by remember(slotNum) { mutableStateOf(difficultySelections[slotNum] ?: Difficulty.CLASSIC) }
        AlertDialog(
            onDismissRequest = { showCreateDialog = null },
            title = { Text(localizedText("Create Save Slot $slotNum")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = createNameDraft,
                        onValueChange = { createNameDraft = it.take(24) },
                        label = { Text(localizedText("Save Name")) },
                        singleLine = true
                    )
                    Text(localizedText("Icon"), style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        SaveProfile.iconIds.forEach { iconId ->
                            FilterChip(
                                selected = createIconDraft == iconId,
                                onClick = { createIconDraft = iconId },
                                label = { Text(SaveProfile.iconSymbol(iconId)) }
                            )
                        }
                    }
                    Text(localizedText("Difficulty"), style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Difficulty.ids.forEach { id ->
                            FilterChip(
                                selected = difficultyDraft == id,
                                onClick = { difficultyDraft = id },
                                label = { Text(locale.getString(Difficulty.nameKey(id), id)) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    difficultySelections[slotNum] = difficultyDraft
                    viewModel.createSlot(slotNum, difficultyDraft, createNameDraft, createIconDraft) { success ->
                        if (success) showCreateDialog = null
                        refreshMetadata()
                    }
                }) { Text(localizedText("Create")) }
            },
            dismissButton = { TextButton(onClick = { showCreateDialog = null }) { Text(locale.getString("cancel", "Cancel")) } }
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
            title = { Text(locale.getString("save_slot_reset_title", "Reset Slot %d?", slotNum)) },
            text = { Text(locale.getString("save_slot_reset_description", "Are you sure you want to reset Slot %d? All progress in this slot will be permanently lost.", slotNum)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.resetSlot(slotNum)
                        showResetDialog = null
                        refreshMetadata()
                    }
                ) {
                    Text(locale.getString("save_slot_reset_confirm", "Reset"), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = null }) {
                    Text(locale.getString("cancel", "Cancel"))
                }
            }
        )
    }

    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text(locale.getString("save_import_title", "Import Save Data")) },
            text = {
                OutlinedTextField(
                    value = importString,
                    onValueChange = { importString = it },
                    label = { Text(locale.getString("save_import_hint", "Paste encrypted save string here")) },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.importSave(importString) { success ->
                        android.widget.Toast.makeText(
                            context,
                            locale.getString(
                                if (success) "save_import_success" else "save_import_invalid",
                                if (success) "Save imported successfully!" else "Invalid save string!"
                            ),
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                        if (success) {
                            importString = ""
                            refreshMetadata()
                        }
                        showImportDialog = false
                    }
                }) {
                    Text(locale.getString("import", "Import"))
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text(locale.getString("cancel", "Cancel"))
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

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            viewModel.exportSave { json ->
                                val clipboard = context?.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                clipboard?.setPrimaryClip(
                                    ClipData.newPlainText(locale.getString("GuessTheNumberSave", "GuessTheNumberSave"), json)
                                )
                                android.widget.Toast.makeText(
                                    context,
                                    locale.getString("save_export_success", "Encrypted save copied to clipboard!"),
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(locale.getString("save_export_button", "Export Encrypted Save"))
                    }
                    OutlinedButton(
                        onClick = { showImportDialog = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(locale.getString("save_import_button", "Import Encrypted Save"))
                    }
                }
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
                                createNameDraft = ""
                                createIconDraft = SaveProfile.DEFAULT_ICON
                                difficultySelections[slot] = Difficulty.CLASSIC
                                showCreateDialog = slot
                            } else {
                                viewModel.switchSlot(slot)
                                onNavigateBack()
                            }
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
                        onDuplicate = { duplicateTarget = 0; showDuplicateDialog = slot },
                        onRestoreBackup = { showRestoreDialog = slot },
                        locale = locale,
                        reducedMotion = gameState.settings.reducedMotion
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
            Text(locale.getString("save_slot_difficulty", "Difficulty") + ": " + selectedName)
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
    onDuplicate: () -> Unit,
    onRestoreBackup: () -> Unit,
    locale: com.jarrlyyy.guessthenumber.data.repository.LocaleManager,
    reducedMotion: Boolean = false
) {
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }
    val slotContainerColor by animateColorAsState(
        targetValue = if (isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        animationSpec = if (reducedMotion) snap() else spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "save-slot-container"
    )
    val slotScale by animateFloatAsState(
        targetValue = if (isActive && !reducedMotion) 1.015f else 1f,
        animationSpec = if (reducedMotion) snap() else spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "save-slot-scale"
    )
    val slotActionInteractionSource = remember { MutableInteractionSource() }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(slotScale)
            .animateContentSize(),
        colors = CardDefaults.cardColors(containerColor = slotContainerColor)
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
                        IconButton(onClick = onDuplicate) {
                            Icon(Icons.Default.ContentCopy, contentDescription = locale.getString("save_slot_duplicate", "Duplicate Slot"))
                        }
                        if (metadata.hasBackup) {
                            IconButton(onClick = onRestoreBackup) {
                                Icon(Icons.Default.Restore, contentDescription = locale.getString("save_slot_restore", "Restore Backup"))
                            }
                        }
                        IconButton(onClick = onReset) {
                            Icon(Icons.Default.Delete, contentDescription = locale.getString("save_slot_reset", "Reset Slot"), tint = MaterialTheme.colorScheme.error)
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
                    interactionSource = slotActionInteractionSource,
                    modifier = Modifier
                        .align(Alignment.End)
                        .expressivePressScale(
                            interactionSource = slotActionInteractionSource,
                            reducedMotion = reducedMotion
                        )
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(locale.getString("save_slot_start_new", "Start New Game"))
                }
            } else {
                val profileDisplayName = metadata.profileName.ifBlank {
                    locale.getString("save_slot_title", "Slot %d", slotIndex)
                }
                Text(text = "${SaveProfile.iconSymbol(metadata.profileIconId)}  $profileDisplayName",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(text = locale.getString("save_slot_difficulty", "Difficulty") + ": " + locale.getString(Difficulty.nameKey(metadata.difficultyId), "Classic"), style = MaterialTheme.typography.bodyMedium)
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
                    interactionSource = slotActionInteractionSource,
                    modifier = Modifier
                        .align(Alignment.End)
                        .expressivePressScale(
                            interactionSource = slotActionInteractionSource,
                            reducedMotion = reducedMotion
                        ),
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
