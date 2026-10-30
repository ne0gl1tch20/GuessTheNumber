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
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jarrlyyy.guessthenumber.domain.model.GameSettings
import java.io.File
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter
import kotlin.math.log10

private class DesktopAudioPlayer : AutoCloseable {
    private var clip: Clip? = null

    fun play(file: File, volume: Float) {
        stop()
        require(file.isFile) { "The selected audio file no longer exists." }
        val next = AudioSystem.getClip()
        try {
            AudioSystem.getAudioInputStream(file).use { stream -> next.open(stream) }
            clip = next
            setVolume(volume)
            next.start()
        } catch (error: Exception) {
            next.close()
            throw error
        }
    }

    fun pause() {
        clip?.stop()
    }

    fun resume() {
        clip?.start()
    }

    fun stop() {
        clip?.let {
            it.stop()
            it.framePosition = 0
            it.close()
        }
        clip = null
    }

    fun setVolume(volume: Float) {
        val control = clip?.takeIf { it.isControlSupported(javax.sound.sampled.FloatControl.Type.MASTER_GAIN) }
            ?.getControl(javax.sound.sampled.FloatControl.Type.MASTER_GAIN) as? javax.sound.sampled.FloatControl
            ?: return
        val safe = volume.coerceIn(0.001f, 1f)
        control.value = (20f * log10(safe)).coerceIn(control.minimum, control.maximum)
    }

    override fun close() = stop()
}

@Composable
internal fun DesktopMusicPlayerContent(
    settings: GameSettings,
    onUpdateSettings: (GameSettings) -> Unit,
    onBack: () -> Unit
) {
    val player = remember { DesktopAudioPlayer() }
    var selectedPath by remember(settings.musicQueuePaths) {
        mutableStateOf(settings.musicQueuePaths.firstOrNull() ?: settings.backgroundMusicPath)
    }
    var isPaused by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }
    DisposableEffect(player) { onDispose { player.close() } }

    Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onBack) { Text("Back to More") }
            Text("Music Player", style = MaterialTheme.typography.headlineSmall)
        }
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Music playback", style = MaterialTheme.typography.titleMedium)
                        Text("Local audio stays on this PC. Supported formats depend on Windows Java Sound (WAV, AIFF and AU are the usual options).", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = settings.musicEnabled,
                        onCheckedChange = {
                            if (!it) {
                                player.stop()
                                isPaused = false
                            }
                            onUpdateSettings(settings.copy(musicEnabled = it))
                        }
                    )
                }
                Text("Volume: ${(settings.volume * 100).toInt()}%")
                Slider(
                    value = settings.volume.coerceIn(0f, 1f),
                    onValueChange = {
                        onUpdateSettings(settings.copy(volume = it))
                        player.setVolume(it)
                    },
                    enabled = settings.musicEnabled,
                    valueRange = 0f..1f
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        val chooser = JFileChooser().apply {
                            isMultiSelectionEnabled = true
                            fileFilter = FileNameExtensionFilter("Audio files (WAV, AIFF, AU)", "wav", "aiff", "aif", "au")
                        }
                        if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                            val added = (chooser.selectedFiles.toList().ifEmpty { listOfNotNull(chooser.selectedFile) })
                                .map { it.absolutePath }
                            val library = (settings.musicLibraryPaths + added).distinct()
                            val queue = if (settings.musicQueuePaths.isEmpty()) added else settings.musicQueuePaths + added
                            selectedPath = added.firstOrNull() ?: selectedPath
                            onUpdateSettings(settings.copy(musicLibraryPaths = library, musicQueuePaths = queue))
                            status = "${added.size} audio file(s) added."
                        }
                    }) { Text("Add audio files") }
                    OutlinedButton(
                        onClick = {
                            val path = selectedPath
                            if (path != null && settings.musicEnabled) {
                                runCatching { player.play(File(path), settings.volume) }
                                    .onSuccess { isPaused = false; status = "Playing ${File(path).name}" }
                                    .onFailure { status = it.message ?: "Could not play this audio file." }
                            }
                        },
                        enabled = settings.musicEnabled && selectedPath != null
                    ) { Text("Play") }
                    OutlinedButton(
                        onClick = { player.pause(); isPaused = true; status = "Playback paused." },
                        enabled = settings.musicEnabled && selectedPath != null
                    ) { Text("Pause") }
                    OutlinedButton(onClick = { player.resume(); isPaused = false; status = "Playback resumed." }, enabled = settings.musicEnabled && isPaused) { Text("Resume") }
                    OutlinedButton(onClick = { player.stop(); isPaused = false; status = "Playback stopped." }) { Text("Stop") }
                }
            }
        }
        Text("Library · ${settings.musicLibraryPaths.size} file(s)", style = MaterialTheme.typography.titleMedium)
        if (settings.musicLibraryPaths.isEmpty()) {
            Text("Add local audio files to build your library.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        settings.musicLibraryPaths.forEach { path ->
            val file = File(path)
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(file.name, style = MaterialTheme.typography.titleSmall)
                        Text(path, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                    }
                    OutlinedButton(onClick = { selectedPath = path }) { Text(if (selectedPath == path) "Selected" else "Select") }
                    OutlinedButton(onClick = {
                        val library = settings.musicLibraryPaths - path
                        val queue = settings.musicQueuePaths - path
                        if (selectedPath == path) {
                            player.stop()
                            selectedPath = queue.firstOrNull() ?: library.firstOrNull()
                        }
                        onUpdateSettings(settings.copy(
                            musicLibraryPaths = library,
                            musicQueuePaths = queue,
                            backgroundMusicPath = if (settings.backgroundMusicPath == path) null else settings.backgroundMusicPath
                        ))
                    }) { Text("Remove") }
                }
            }
        }
        status?.let { Text(it, color = MaterialTheme.colorScheme.tertiary) }
    }
}
