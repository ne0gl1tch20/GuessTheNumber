package com.jarrlyyy.guessthenumber.ui.screens

import com.jarrlyyy.guessthenumber.ui.localization.localizedText

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.input.pointer.consumePositionChange
import com.jarrlyyy.guessthenumber.domain.model.GameState
import com.jarrlyyy.guessthenumber.ui.navigation.Screen
import java.io.File


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreen(
    state: GameState,
    onNavigate: (String) -> Unit,
    onUpdateBackgroundMusic: (String?) -> Unit,
    isPlayingMusic: Boolean,
    musicCurrentPosition: Int,
    musicDuration: Int,
    musicAlbumArt: android.graphics.Bitmap?,
    onPlayMusic: () -> Unit,
    onPauseMusic: () -> Unit,
    onStopMusic: () -> Unit,
    onSeekMusic: (Int) -> Unit,
    onUpdateSettings: (com.jarrlyyy.guessthenumber.domain.model.GameSettings) -> Unit
) {
    val context = LocalContext.current
    var showMusicDialog by remember { mutableStateOf(false) }

    val currentMusicPath = state.settings.backgroundMusicPath

    val defaultOrder = com.jarrlyyy.guessthenumber.domain.model.DEFAULT_MORE_SCREEN_ORDER
    val normalizedOrder = remember(state.settings.moreScreenOrder) {
        (state.settings.moreScreenOrder + defaultOrder).distinct()
            .filter { it in defaultOrder }
            .let { it + defaultOrder.filterNot(it::contains) }
    }
    var order by remember(normalizedOrder) { mutableStateOf(normalizedOrder) }
    var dragging by remember { mutableStateOf<String?>(null) }
    var dragDistance by remember { mutableFloatStateOf(0f) }
    var isReordering by remember { mutableStateOf(false) }

    LaunchedEffect(state.settings.moreScreenOrder) {
        if (state.settings.moreScreenOrder != normalizedOrder) {
            onUpdateSettings(state.settings.copy(moreScreenOrder = normalizedOrder))
        }
    }

    val labels = mapOf(
        "achievements" to "Achievements & Tiers",
        "live_ops" to "Random & Seasonal Events",
        "mutators" to "Mutators & Challenge Builder",
        "talent" to "Prestige Talent Web",
        "arcade" to "Arcade Minigames",
        "stats" to "Statistics",
        "changelog" to "Changelog",
        "settings" to "Settings",
        "about" to "About & Licenses",
        "dev_settings" to "Dev Settings"
    )
    val icons = mapOf(
        "achievements" to Icons.Default.EmojiEvents,
        "live_ops" to Icons.Default.Event,
        "mutators" to Icons.Default.Tune,
        "talent" to Icons.Default.AccountTree,
        "arcade" to Icons.Default.Favorite,
        "stats" to Icons.Default.Face,
        "changelog" to Icons.Default.Info,
        "settings" to Icons.Default.Settings,
        "about" to Icons.Default.Info,
        "dev_settings" to Icons.Default.Build
    )

    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val musicFile = File(context.filesDir, "background_music.mp3")
                    musicFile.outputStream().use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                    onUpdateBackgroundMusic(musicFile.absolutePath)
                    onPlayMusic()
                    Toast.makeText(context, "Background music saved & playing!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to load audio: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(localizedText("More Hub"), style = MaterialTheme.typography.titleMedium) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
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
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(localizedText("More Menu"), style = MaterialTheme.typography.headlineSmall)
                            Text(
                                localizedText("Your shortcuts, your order."),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        FilledTonalButton(
                            onClick = {
                                if (isReordering) {
                                    dragging = null
                                    dragDistance = 0f
                                    onUpdateSettings(state.settings.copy(moreScreenOrder = order))
                                }
                                isReordering = !isReordering
                            },
                            shape = MaterialTheme.shapes.extraLarge
                        ) {
                            Icon(Icons.Default.DragHandle, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text(if (isReordering) localizedText("Done") else localizedText("Reorder"))
                        }
                    }

                    if (isReordering) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.large,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            tonalElevation = 2.dp
                        ) {
                            Text(
                                localizedText("Long-press and drag. The new order is saved and used by this menu."),
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

                                items(order, key = { it }) { route ->
                        val selected = dragging == route
                        val animatedScale by animateFloatAsState(
                            targetValue = if (selected) 1.025f else 1f,
                            animationSpec = androidx.compose.animation.core.spring(
                                stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow,
                                dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy
                            ),
                            label = "moreMenuItemScale"
                        )
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateContentSize()
                                .animateItem(
                                    placementSpec = androidx.compose.animation.core.spring(
                                        stiffness = androidx.compose.animation.core.Spring.StiffnessLow,
                                        dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy
                                    )
                                )
                                .graphicsLayer {
                                    scaleX = animatedScale
                                    scaleY = animatedScale
                                    shadowElevation = if (selected) 16f else 0f
                                }
                                .then(
                                    if (isReordering) {
                                        Modifier.pointerInput(isReordering, route) {
                                            detectDragGesturesAfterLongPress(
                                                onDragStart = { dragging = route; dragDistance = 0f },
                                                onDragCancel = { dragging = null; dragDistance = 0f },
                                                onDragEnd = {
                                                    dragging = null
                                                    dragDistance = 0f
                                                    onUpdateSettings(state.settings.copy(moreScreenOrder = order))
                                                },
                                                onDrag = { change, amount ->
                                                    change.consumePositionChange()
                                                    if (dragging == route) {
                                                        dragDistance += amount.y
                                                        val index = order.indexOf(route)
                                                        if (dragDistance > 48f && index < order.lastIndex) {
                                                            order = order.toMutableList().also {
                                                                val tmp = it[index]
                                                                it[index] = it[index + 1]
                                                                it[index + 1] = tmp
                                                            }
                                                            dragDistance = 0f
                                                        } else if (dragDistance < -48f && index > 0) {
                                                            order = order.toMutableList().also {
                                                                val tmp = it[index]
                                                                it[index] = it[index - 1]
                                                                it[index - 1] = tmp
                                                            }
                                                            dragDistance = 0f
                                                        }
                                                    }
                                                }
                                            )
                                        }
                                    } else {
                                        Modifier.clickable { onNavigate(route) }
                                    }
                                ),
                            shape = MaterialTheme.shapes.large,
                            color = if (selected) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceContainerHigh
                        ) {
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = icons[route] ?: Icons.Default.Circle,
                                    contentDescription = null,
                                    tint = if (selected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.width(14.dp))
                                Text(
                                    labels[route] ?: route,
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.titleMedium
                                )
                                if (isReordering) {
                                    Icon(Icons.Default.DragHandle, contentDescription = localizedText("Reorder"))
                                } else {
                                    Icon(Icons.Default.ChevronRight, contentDescription = localizedText("Open"))
                                }
                            }
                        }
                    }

            item {
                Text(localizedText("Reset Tiers & Hubs"), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            }

            item {
                Button(
                    onClick = { onNavigate("ultra") },
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer, contentColor = MaterialTheme.colorScheme.onTertiaryContainer),
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Icon(imageVector = Icons.Default.Star, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(localizedText("Ultra Hub & Upgrades"), style = MaterialTheme.typography.titleMedium)
                }
            }

            item {
                Button(
                    onClick = { onNavigate("prestige") },
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer),
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Icon(imageVector = Icons.Default.Star, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(localizedText("Prestige Hub & Upgrades"), style = MaterialTheme.typography.titleMedium)
                }
            }

            item {
                HorizontalDivider()
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    localizedText("Audio & Save Management"),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                FilledTonalButton(
                    onClick = { onNavigate(Screen.SaveSlots.route) },
                    shape = MaterialTheme.shapes.extraLarge,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(localizedText("Save Slots"), style = MaterialTheme.typography.titleMedium)
                }
            }

            item {
                FilledTonalButton(
                    onClick = { showMusicDialog = true },
                    shape = MaterialTheme.shapes.extraLarge,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(localizedText("Background Music Player"), style = MaterialTheme.typography.titleMedium)
                }
            }

            item {
                HorizontalDivider()
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    localizedText("Latest Systems"),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    localizedText("v1.10–v1.13.0 features"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (showMusicDialog) {
        AlertDialog(
            onDismissRequest = { showMusicDialog = false },
            title = { Text(localizedText("🎵 Background Music Player")) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(localizedText("Select an audio file (MP3/WAV) to save and play across all screens."))
                    Text(
                        text = if (currentMusicPath != null) "File: ${File(currentMusicPath).name}" else "No music file selected.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary
                    )

                    if (musicAlbumArt != null) {
                        androidx.compose.foundation.Image(
                            bitmap = musicAlbumArt.asImageBitmap(),
                            contentDescription = "Album Art",
                            modifier = Modifier
                                .size(120.dp)
                                .padding(4.dp),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    }

                    OutlinedButton(
                        onClick = { audioPickerLauncher.launch(arrayOf("audio/*")) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.FolderOpen, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(localizedText("Select Audio File"))
                    }

                    val maxDur = if (musicDuration > 0) musicDuration.toFloat() else 1f
                    var sliderPos by remember(musicCurrentPosition) { mutableStateOf(musicCurrentPosition.toFloat()) }
                    var isSeeking by remember { mutableStateOf(false) }

                    Column(modifier = Modifier.fillMaxWidth()) {
                        Slider(
                            value = if (isSeeking) sliderPos else musicCurrentPosition.toFloat(),
                            onValueChange = {
                                isSeeking = true
                                sliderPos = it
                            },
                            onValueChangeFinished = {
                                isSeeking = false
                                onSeekMusic(sliderPos.toInt())
                            },
                            valueRange = 0f..maxDur
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val currentPosSec = (if (isSeeking) sliderPos.toInt() else musicCurrentPosition) / 1000
                            val totalDurSec = musicDuration / 1000
                            Text(
                                text = String.format(java.util.Locale.US, "%d:%02d", currentPosSec / 60, currentPosSec % 60),
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = String.format(java.util.Locale.US, "%d:%02d", totalDurSec / 60, totalDurSec % 60),
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilledIconButton(
                            onClick = {
                                if (currentMusicPath != null && File(currentMusicPath).exists()) {
                                    if (isPlayingMusic) {
                                        onPauseMusic()
                                        Toast.makeText(context, "Paused background music", Toast.LENGTH_SHORT).show()
                                    } else {
                                        onPlayMusic()
                                        Toast.makeText(context, "Playing background music...", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    Toast.makeText(context, "Please select an audio file first!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = if (isPlayingMusic) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Icon(
                                imageVector = if (isPlayingMusic) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlayingMusic) "Pause" else "Play"
                            )
                        }

                        FilledIconButton(
                            onClick = {
                                onStopMusic()
                                Toast.makeText(context, "Stopped background music", Toast.LENGTH_SHORT).show()
                            },
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            )
                        ) {
                            Icon(imageVector = Icons.Default.Stop, contentDescription = "Stop")
                        }
                    }

                    Text(
                        text = if (isPlayingMusic) "Status: Playing 🎵" else "Status: Paused / Stopped",
                        fontSize = 12.sp,
                        color = if (isPlayingMusic) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showMusicDialog = false }) {
                    Text(localizedText("Close"))
                }
            }
        )
    }
}
