package com.jarrlyyy.guessthenumber.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarrlyyy.guessthenumber.domain.model.GameState
import com.jarrlyyy.guessthenumber.ui.theme.PrestigeBlue
import com.jarrlyyy.guessthenumber.ui.theme.UltraPurple
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreen(
    state: GameState,
    onNavigate: (String) -> Unit,
    onUpdateBackgroundMusic: (String?) -> Unit,
    isPlayingMusic: Boolean,
    onPlayMusic: () -> Unit,
    onPauseMusic: () -> Unit,
    onStopMusic: () -> Unit
) {
    val context = LocalContext.current
    var showChangelog by remember { mutableStateOf(false) }
    var showMusicDialog by remember { mutableStateOf(false) }
    var changelogText by remember { mutableStateOf("") }

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
        topBar = { TopAppBar(title = { Text("More Hub & Navigation") }) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text("Reset Tiers & Hubs", style = MaterialTheme.typography.titleMedium, fontSize = 18.sp)
            }

            item {
                Button(
                    onClick = { onNavigate("ultra") },
                    colors = ButtonDefaults.buttonColors(containerColor = UltraPurple),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.Star, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Ultra Hub & Upgrades", fontSize = 16.sp)
                }
            }

            item {
                Button(
                    onClick = { onNavigate("prestige") },
                    colors = ButtonDefaults.buttonColors(containerColor = PrestigeBlue),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.Star, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Prestige Hub & Upgrades", fontSize = 16.sp)
                }
            }

            item {
                HorizontalDivider()
                Spacer(modifier = Modifier.height(4.dp))
                Text("Audio & Features", style = MaterialTheme.typography.titleMedium, fontSize = 18.sp)
            }

            item {
                Button(
                    onClick = { showMusicDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Background Music Player", fontSize = 16.sp)
                }
            }

            item {
                HorizontalDivider()
                Spacer(modifier = Modifier.height(4.dp))
                Text("General Navigation", style = MaterialTheme.typography.titleMedium, fontSize = 18.sp)
            }

            item {
                Button(onClick = { showChangelog = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(imageVector = Icons.Default.Info, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Changelog", fontSize = 16.sp)
                }
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
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Select an audio file (MP3/WAV) to save and play in the background across all screens.")
                    Text(
                        text = if (currentMusicPath != null) "Current File: ${File(currentMusicPath).name}" else "No music file selected.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    OutlinedButton(
                        onClick = { audioPickerLauncher.launch(arrayOf("audio/*")) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.FolderOpen, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Select Audio File")
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilledIconButton(
                            onClick = {
                                if (currentMusicPath != null && File(currentMusicPath).exists()) {
                                    onPlayMusic()
                                    Toast.makeText(context, "Playing background music...", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Please select an audio file first!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = if (isPlayingMusic) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Play")
                        }

                        FilledIconButton(
                            onClick = {
                                onPauseMusic()
                                Toast.makeText(context, "Paused background music", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(imageVector = Icons.Default.Pause, contentDescription = "Pause")
                        }

                        FilledIconButton(
                            onClick = {
                                onStopMusic()
                                Toast.makeText(context, "Stopped background music", Toast.LENGTH_SHORT).show()
                            }
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

    if (showChangelog) {
        if (changelogText.isEmpty()) {
            changelogText = try {
                context.assets.open("changelogs.md").bufferedReader().use { it.readText() }
            } catch (_: Exception) {
                "Failed to load changelogs.md"
            }
        }
        AlertDialog(
            onDismissRequest = { showChangelog = false },
            title = { Text("Changelog") },
            text = {
                Box(modifier = Modifier.height(300.dp)) {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            Text(changelogText, fontSize = 14.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showChangelog = false }) {
                    Text("Close")
                }
            }
        )
    }
}
