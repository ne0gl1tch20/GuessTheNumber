package com.jarrlyyy.guessthenumber.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.input.pointer.consume
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
                title = { Text("More Hub", style = MaterialTheme.typography.titleMedium) },
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
                val defaultOrder = listOf("achievements", "live_ops", "mutators", "talent", "arcade", "stats", "changelog", "settings", "about", "dev_settings")
                var order by remember(state.settings.moreScreenOrder) {
                    mutableStateOf((state.settings.moreScreenOrder + defaultOrder).distinct().take(defaultOrder.size))
                }
                var dragging by remember { mutableStateOf<Int?>(null) }
                var dragDistance by remember { mutableFloatStateOf(0f) }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Reorder More", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    Text("Long-press and drag items to change their order.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    order.forEachIndexed { index, route ->
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
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .pointerInput(order) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = { dragging = index; dragDistance = 0f },
                                        onDragCancel = { dragging = null; dragDistance = 0f },
                                        onDragEnd = {
                                            dragging = null
                                            dragDistance = 0f
                                            onUpdateSettings(state.settings.copy(moreScreenOrder = order))
                                        },
                                        onDrag = { change, amount ->
                                            change.consume()
                                            if (dragging == index) {
                                                dragDistance += amount.y
                                                if (dragDistance > 48f && index < order.lastIndex) {
                                                    val next = order.toMutableList()
                                                    next[index] = next[index + 1].also { next[index + 1] = next[index] }
                                                    order = next
                                                    dragDistance = 0f
                                                } else if (dragDistance < -48f && index > 0) {
                                                    val next = order.toMutableList()
                                                    next[index] = next[index - 1].also { next[index - 1] = next[index] }
                                                    order = next
                                                    dragDistance = 0f
                                                }
                                            }
                                        }
                                    )
                                },
                            shape = MaterialTheme.shapes.small,
                            color = if (dragging == index) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.DragHandle, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text(labels[route] ?: route, modifier = Modifier.weight(1f))
                                Text("↕", style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }
                }
            }

            item {
                Text("Reset Tiers & Hubs", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
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
                    Text("Ultra Hub & Upgrades", style = MaterialTheme.typography.titleMedium)
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
                    Text("Prestige Hub & Upgrades", style = MaterialTheme.typography.titleMedium)
                }
            }

            item {
                HorizontalDivider()
                Spacer(modifier = Modifier.height(4.dp))
                Text("Audio & Save Management", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            }

            item {
                Button(
                    onClick = { onNavigate(Screen.SaveSlots.route) },
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer),
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Icon(imageVector = Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save Slots", style = MaterialTheme.typography.titleMedium)
                }
            }

            item {
                Button(
                    onClick = { showMusicDialog = true },
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Background Music Player", style = MaterialTheme.typography.titleMedium)
                }
            }

            item {
                HorizontalDivider()
                Spacer(modifier = Modifier.height(4.dp))
                Text("Latest Systems", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Text(
                    "v1.10–v1.12 features",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            item {
                Button(onClick = { onNavigate("achievements") }, modifier = Modifier.fillMaxWidth()) {
                    Icon(imageVector = Icons.Default.EmojiEvents, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Achievements & Tiers", fontSize = 16.sp)
                }
            }
            item {
                Button(onClick = { onNavigate("live_ops") }, modifier = Modifier.fillMaxWidth()) {
                    Icon(imageVector = Icons.Default.Event, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Random & Seasonal Events", fontSize = 16.sp)
                }
            }
            item {
                Button(onClick = { onNavigate("mutators") }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)) {
                    Icon(imageVector = Icons.Default.Tune, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Mutators & Challenge Builder", fontSize = 16.sp)
                }
            }
            item {
                Button(onClick = { onNavigate("talent") }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)) {
                    Icon(imageVector = Icons.Default.AccountTree, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Prestige Talent Web", fontSize = 16.sp)
                }
            }

            item {
                HorizontalDivider()
                Spacer(modifier = Modifier.height(4.dp))
                Text("Explore & Progress", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            }
            item {
                Button(onClick = { onNavigate("arcade") }, modifier = Modifier.fillMaxWidth()) {
                    Icon(imageVector = Icons.Default.Favorite, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Arcade Minigames", fontSize = 16.sp)
                }
            }
            item {
                Button(onClick = { onNavigate("stats") }, modifier = Modifier.fillMaxWidth()) {
                    Icon(imageVector = Icons.Default.Face, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Statistics", fontSize = 16.sp)
                }
            }
            item {
                Button(onClick = { onNavigate(Screen.ChangelogViewer.route) }, modifier = Modifier.fillMaxWidth()) {
                    Icon(imageVector = Icons.Default.Info, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Changelog", fontSize = 16.sp)
                }
            }

            item {
                HorizontalDivider()
                Spacer(modifier = Modifier.height(4.dp))
                Text("App & Developer", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            }
            item {
                Button(onClick = { onNavigate("settings") }, modifier = Modifier.fillMaxWidth()) {
                    Icon(imageVector = Icons.Default.Settings, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Settings", fontSize = 16.sp)
                }
            }
            item {
                Button(onClick = { onNavigate("about") }, modifier = Modifier.fillMaxWidth()) {
                    Icon(imageVector = Icons.Default.Info, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("About & Licenses", fontSize = 16.sp)
                }
            }
            item {
                Button(onClick = { onNavigate("dev_settings") }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)) {
                    Icon(imageVector = Icons.Default.Build, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Dev Settings", fontSize = 16.sp)
                }
            }
        }
    }

    if (showMusicDialog) {
        AlertDialog(
            onDismissRequest = { showMusicDialog = false },
            title = { Text("🎵 Background Music Player") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Select an audio file (MP3/WAV) to save and play across all screens.")
                    Text(
                        text = if (currentMusicPath != null) "File: ${File(currentMusicPath).name}" else "No music file selected.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Album Art Display if available
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
                        Text("Select Audio File")
                    }

                    // Sliding Progress Bar
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

                    // Play/Pause and Stop Controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Play/Toggle Button
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

                        // Stop Button
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
                    Text("Close")
                }
            }
        )
    }


}
