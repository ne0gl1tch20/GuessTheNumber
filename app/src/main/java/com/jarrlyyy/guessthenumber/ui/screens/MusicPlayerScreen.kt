package com.jarrlyyy.guessthenumber.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.Image
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.jarrlyyy.guessthenumber.data.audio.BackgroundMusicManager
import com.jarrlyyy.guessthenumber.ui.localization.LocalAppLocaleManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicPlayerScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val locale = LocalAppLocaleManager.current
    // BackgroundMusicManager is application-scoped and shared with GameViewModel.
    // Do not release it from this screen: navigation must not stop app-wide playback.
    val manager = remember { BackgroundMusicManager(context.applicationContext) }
    val library by manager.library.collectAsState()
    val queue by manager.queue.collectAsState()
    val playlists by manager.playlists.collectAsState()
    val playing by manager.isPlaying.collectAsState()
    val currentTrackId by manager.currentTrackId.collectAsState()
    val position by manager.currentPosition.collectAsState()
    val duration by manager.duration.collectAsState()
    val albumArt by manager.albumArt.collectAsState()
    val shuffle by manager.shuffle.collectAsState()
    val repeat by manager.repeatMode.collectAsState()
    var playlistName by remember { mutableStateOf("") }
    var expandedPlaylist by remember { mutableStateOf<String?>(null) }
    val current = library.firstOrNull { it.id == currentTrackId }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) manager.importTrack(uri)?.let { manager.playTrack(it.id) }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(locale.getString("music_player_title", "Offline Music Player")) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = locale.getString("back", "Back")) } }) }
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Card {
                    Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        if (albumArt != null) {
                            Image(
                                bitmap = albumArt!!.asImageBitmap(),
                                contentDescription = current?.title ?: locale.getString("music_no_track", "No track selected"),
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(220.dp).clip(RoundedCornerShape(28.dp))
                            )
                        } else {
                            Surface(
                                modifier = Modifier.size(220.dp),
                                shape = RoundedCornerShape(28.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHighest
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.MusicNote,
                                        contentDescription = current?.title ?: locale.getString("music_no_track", "No track selected"),
                                        modifier = Modifier.size(76.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                        Text(current?.title ?: locale.getString("music_no_track", "No track selected"), style = MaterialTheme.typography.titleLarge)
                        val subtitle = current?.artist?.takeIf { it.isNotBlank() } ?: current?.album?.takeIf { it.isNotBlank() }
                        if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodyMedium)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(locale.formatDuration((position.coerceAtLeast(0) / 1000).toLong()), style = MaterialTheme.typography.labelSmall)
                            Text(locale.formatDuration((duration.coerceAtLeast(0) / 1000).toLong()), style = MaterialTheme.typography.labelSmall)
                        }
                        Slider(value = position.toFloat().coerceIn(0f, duration.coerceAtLeast(1).toFloat()), onValueChange = { manager.seekTo(it.toInt()) }, valueRange = 0f..duration.coerceAtLeast(1).toFloat())
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            IconButton(onClick = { manager.previous() }) { Icon(Icons.Default.SkipPrevious, contentDescription = locale.getString("music_control_previous", "Previous")) }
                            FilledIconButton(onClick = { if (playing) manager.pause() else manager.play() }) { Icon(if (playing) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = locale.getString(if (playing) "music_control_pause" else "music_control_play", if (playing) "Pause" else "Play")) }
                            IconButton(onClick = { manager.next() }) { Icon(Icons.Default.SkipNext, contentDescription = locale.getString("music_control_next", "Next")) }
                            IconButton(onClick = { manager.setShuffle(!shuffle) }) { Icon(Icons.Default.Shuffle, null, tint = if (shuffle) MaterialTheme.colorScheme.primary else LocalContentColor.current) }
                            IconButton(onClick = { manager.setRepeatMode(BackgroundMusicManager.RepeatMode.entries[(repeat.ordinal + 1) % BackgroundMusicManager.RepeatMode.entries.size]) }) { Icon(Icons.Default.Repeat, null, tint = if (repeat != BackgroundMusicManager.RepeatMode.OFF) MaterialTheme.colorScheme.primary else LocalContentColor.current) }
                        }
                    }
                }
            }
            item { Button(onClick = { picker.launch(arrayOf("audio/*")) }, Modifier.fillMaxWidth()) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp)); Text(locale.getString("music_import", "Add local audio")) } }
            item { Text(locale.getString("music_library", "Library"), style = MaterialTheme.typography.titleLarge) }
            items(library, key = { it.id }) { track ->
                ListItem(
                    headlineContent = { Text(track.title) },
                    supportingContent = { Text(track.artist.ifBlank { track.album }) },
                    leadingContent = { Icon(Icons.Default.MusicNote, null) },
                    trailingContent = { Row {
                        IconButton(onClick = { manager.playTrack(track.id) }) { Icon(Icons.Default.PlayArrow, null) }
                        IconButton(onClick = { manager.enqueue(track.id) }) { Icon(Icons.Default.QueueMusic, null) }
                        IconButton(onClick = { manager.removeTrack(track.id) }) { Icon(Icons.Default.Delete, null) }
                    } }
                )
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(locale.getString("music_queue_format", "Queue (%d)", queue.size), style = MaterialTheme.typography.titleLarge)
                    TextButton(onClick = manager::clearQueue) { Text(locale.getString("clear", "Clear")) }
                }
            }
            items(queue.indices.toList()) { index ->
                val track = library.firstOrNull { it.id == queue[index] } ?: return@items
                ListItem(
                    headlineContent = { Text(track.title) },
                    supportingContent = { Text(locale.getString("music_queue_position", "Position %d", index + 1)) },
                    trailingContent = { Row {
                        IconButton(onClick = { if (index > 0) manager.moveQueue(index, index - 1) }) { Icon(Icons.Default.KeyboardArrowUp, null) }
                        IconButton(onClick = { if (index < queue.lastIndex) manager.moveQueue(index, index + 1) }) { Icon(Icons.Default.KeyboardArrowDown, null) }
                        IconButton(onClick = { manager.removeFromQueue(index) }) { Icon(Icons.Default.Close, null) }
                    } }
                )
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(playlistName, { playlistName = it }, Modifier.weight(1f), label = { Text(locale.getString("music_playlist_name", "Playlist name")) })
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = { if (manager.createPlaylist(playlistName)) playlistName = "" }, enabled = playlistName.isNotBlank()) { Text(locale.getString("music_create_playlist", "Create")) }
                }
            }
            item { Text(locale.getString("music_playlists", "Playlists"), style = MaterialTheme.typography.titleLarge) }
            items(playlists.keys.toList()) { name ->
                val ids = playlists[name].orEmpty()
                ListItem(
                    headlineContent = { Text(name) },
                    supportingContent = { Text(locale.getString("music_playlist_tracks", "%d tracks", ids.size)) },
                    trailingContent = { Row {
                        IconButton(onClick = { manager.playQueue(ids) }) { Icon(Icons.Default.PlayArrow, null) }
                        IconButton(onClick = { expandedPlaylist = if (expandedPlaylist == name) null else name }) { Icon(if (expandedPlaylist == name) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null) }
                        IconButton(onClick = { manager.deletePlaylist(name); if (expandedPlaylist == name) expandedPlaylist = null }) { Icon(Icons.Default.Delete, null) }
                    } }
                )
                if (expandedPlaylist == name) {
                    library.forEach { track ->
                        val included = track.id in ids
                        ListItem(
                            modifier = Modifier.padding(start = 20.dp),
                            headlineContent = { Text(track.title) },
                            trailingContent = { IconButton(onClick = { if (included) manager.removeFromPlaylist(name, track.id) else manager.addToPlaylist(name, track.id) }) { Icon(if (included) Icons.Default.CheckCircle else Icons.Default.AddCircleOutline, null) } }
                        )
                    }
                }
            }
        }
    }
}
