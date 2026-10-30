package com.jarrlyyy.guessthenumber.desktop

import com.jarrlyyy.guessthenumber.domain.model.GameSettings
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.Base64
import java.util.prefs.Preferences
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip
import javax.sound.sampled.FloatControl
import javax.swing.JFileChooser
import javax.swing.SwingUtilities
import javax.swing.filechooser.FileNameExtensionFilter
import kotlin.math.log10
import kotlin.random.Random

/**
 * Windows audio host for the canonical shared HTML music screen.
 * The web UI owns presentation; this adapter owns local files and Java Sound.
 */
internal object DesktopMusicBridge {
    private val preferences = Preferences.userRoot().node("GuessTheNumber/music")
    private var clip: Clip? = null
    private var currentPath: String? = null
    private var paused = false
    private var shuffle = preferences.getBoolean("shuffle", false)
    private var repeatMode = preferences.get("repeatMode", "OFF")
    private var lastError: String? = null

    @Synchronized
    fun state(settings: GameSettings): JSONObject {
        val library = settings.musicLibraryPaths.distinct()
        val queue = settings.musicQueuePaths.filter { it in library }
        val current = currentPath?.takeIf { it in library } ?: settings.backgroundMusicPath?.takeIf { it in library }
        val playlists = readPlaylists()
        val currentClip = clip
        val durationMs = currentClip?.let { it.microsecondLength / 1000L } ?: current?.let(::durationMillis) ?: 0L
        val positionMs = currentClip?.let { it.microsecondPosition / 1000L } ?: 0L
        return JSONObject()
            .put("library", JSONArray().apply {
                library.forEach { path ->
                    val file = File(path)
                    put(JSONObject()
                        .put("id", trackId(path))
                        .put("title", file.nameWithoutExtension.ifBlank { file.name })
                        .put("artist", "")
                        .put("album", "")
                        .put("duration", durationMillis(path) / 1000L))
                }
            })
            .put("queue", JSONArray().apply { queue.forEach { put(trackId(it)) } })
            .put("playlists", JSONObject().apply {
                playlists.forEach { (name, paths) -> put(name, JSONArray().apply { paths.forEach { put(trackId(it)) } }) }
            })
            .put("currentTrackId", current?.let(::trackId) ?: "")
            .put("currentPosition", positionMs)
            .put("duration", durationMs)
            .put("isPlaying", currentClip?.isRunning == true && !paused)
            .put("shuffle", shuffle)
            .put("repeatMode", repeatMode)
            .put("status", lastError ?: "")
    }

    @Synchronized
    fun perform(action: String, request: JSONObject, settings: GameSettings, updateSettings: (GameSettings) -> Unit): JSONObject {
        lastError = null
        try {
            when (action) {
                "import" -> {
                    val files = chooseAudioFiles()
                    if (files.isNotEmpty()) {
                        val added = files.map { it.absolutePath }
                        val library = (settings.musicLibraryPaths + added).distinct()
                        val queue = if (settings.musicQueuePaths.isEmpty()) added else (settings.musicQueuePaths + added).distinct()
                        updateSettings(settings.copy(musicLibraryPaths = library, musicQueuePaths = queue))
                        if (currentPath == null) currentPath = added.first()
                    }
                }
                "play" -> {
                    if (!settings.musicEnabled) error("Enable background music in Settings first.")
                    val path = currentPath ?: settings.musicQueuePaths.firstOrNull() ?: settings.musicLibraryPaths.firstOrNull()
                        ?: error("Add an audio file from this device first.")
                    playPath(path, settings.volume)
                }
                "pause" -> { clip?.stop(); paused = true }
                "stop" -> stopPlayback()
                "next" -> moveTrack(settings, 1)
                "previous" -> moveTrack(settings, -1)
                "shuffle" -> { shuffle = !shuffle; preferences.putBoolean("shuffle", shuffle); preferences.flush() }
                "repeat" -> {
                    repeatMode = when (repeatMode) { "OFF" -> "ALL"; "ALL" -> "ONE"; else -> "OFF" }
                    preferences.put("repeatMode", repeatMode); preferences.flush()
                }
                "play_track" -> {
                    val path = resolveTrack(settings, request.optString("trackId"))
                    if (!settings.musicEnabled) error("Enable background music in Settings first.")
                    playPath(path, settings.volume)
                }
                "enqueue" -> {
                    val path = resolveTrack(settings, request.optString("trackId"))
                    updateSettings(settings.copy(musicQueuePaths = (settings.musicQueuePaths + path).distinct()))
                }
                "remove_track" -> {
                    val path = resolveTrack(settings, request.optString("trackId"))
                    if (currentPath == path) stopPlayback()
                    updateSettings(settings.copy(
                        musicLibraryPaths = settings.musicLibraryPaths - path,
                        musicQueuePaths = settings.musicQueuePaths - path,
                        backgroundMusicPath = if (settings.backgroundMusicPath == path) null else settings.backgroundMusicPath
                    ))
                }
                "clear_queue" -> updateSettings(settings.copy(musicQueuePaths = emptyList()))
                "remove_queue" -> {
                    val index = request.optInt("index", -1)
                    val queue = settings.musicQueuePaths.toMutableList()
                    require(index in queue.indices) { "Queue item no longer exists." }
                    queue.removeAt(index)
                    updateSettings(settings.copy(musicQueuePaths = queue))
                }
                "move_queue" -> {
                    val from = request.optInt("index", -1)
                    val to = request.optInt("toIndex", -1)
                    val queue = settings.musicQueuePaths.toMutableList()
                    require(from in queue.indices && to in queue.indices) { "Queue position is invalid." }
                    val item = queue.removeAt(from); queue.add(to, item)
                    updateSettings(settings.copy(musicQueuePaths = queue))
                }
                "seek" -> {
                    val active = clip ?: error("No track is playing.")
                    active.microsecondPosition = request.optLong("position", 0L).coerceIn(0L, active.microsecondLength)
                }
                "create_playlist", "delete_playlist", "play_playlist", "add_to_playlist", "remove_from_playlist" ->
                    error("Playlists are not supported by the current Windows audio adapter yet.")
                else -> error("Unsupported music action.")
            }
        } catch (error: Exception) {
            lastError = error.message ?: "The audio action could not be completed."
        }
        return state(settings)
    }

    @Synchronized
    fun setVolume(volume: Float) {
        val safe = volume.coerceIn(0f, 1f)
        val control = clip?.takeIf { it.isControlSupported(FloatControl.Type.MASTER_GAIN) }
            ?.getControl(FloatControl.Type.MASTER_GAIN) as? FloatControl ?: return
        control.value = (20f * log10(safe.coerceAtLeast(0.001f))).coerceIn(control.minimum, control.maximum)
    }

    @Synchronized
    private fun playPath(path: String, volume: Float) {
        val file = File(path)
        require(file.isFile) { "The selected audio file is no longer available." }
        val next = AudioSystem.getClip()
        try {
            AudioSystem.getAudioInputStream(file).use { stream -> next.open(stream) }
            stopPlayback()
            clip = next
            currentPath = path
            paused = false
            setVolume(volume)
            next.start()
        } catch (error: Exception) {
            next.close()
            throw error
        }
    }

    @Synchronized
    private fun stopPlayback() {
        clip?.let { runCatching { it.stop(); it.close() } }
        clip = null
        paused = false
    }

    private fun moveTrack(settings: GameSettings, delta: Int) {
        val queue = settings.musicQueuePaths.ifEmpty { settings.musicLibraryPaths }
        if (queue.isEmpty()) error("Your music library is empty.")
        val currentIndex = queue.indexOf(currentPath).let { if (it < 0) 0 else it }
        val nextIndex = if (shuffle && queue.size > 1) {
            (queue.indices.filter { it != currentIndex }.randomOrNull() ?: currentIndex)
        } else (currentIndex + delta + queue.size) % queue.size
        playPath(queue[nextIndex], settings.volume)
    }

    private fun chooseAudioFiles(): List<File> {
        var selected: List<File> = emptyList()
        val show = Runnable {
            val chooser = JFileChooser().apply {
                isMultiSelectionEnabled = true
                fileFilter = FileNameExtensionFilter("Audio files supported by Windows Java Sound (WAV, AIFF, AU)", "wav", "aiff", "aif", "au")
            }
            if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                selected = chooser.selectedFiles.toList().ifEmpty { listOfNotNull(chooser.selectedFile) }
            }
        }
        if (SwingUtilities.isEventDispatchThread()) show.run() else SwingUtilities.invokeAndWait(show)
        return selected.filter { it.isFile }
    }

    private fun resolveTrack(settings: GameSettings, id: String): String =
        (settings.musicLibraryPaths + settings.musicQueuePaths).distinct().firstOrNull { trackId(it) == id }
            ?: error("That audio track is no longer in your library.")

    private fun trackId(path: String): String =
        "track_" + Base64.getUrlEncoder().withoutPadding().encodeToString(path.toByteArray(Charsets.UTF_8)).take(64)

    private fun durationMillis(path: String): Long = runCatching {
        AudioSystem.getAudioInputStream(File(path)).use { stream ->
            if (stream.format.frameRate <= 0f) 0L
            else (stream.frameLength / stream.format.frameRate * 1000.0).toLong().coerceAtLeast(0L)
        }
    }.getOrDefault(0L)

    private fun readPlaylists(): Map<String, List<String>> = runCatching {
        val json = JSONObject(preferences.get("playlists", "{}"))
        buildMap {
            json.keys().forEach { name ->
                val values = json.optJSONArray(name) ?: JSONArray()
                put(name, (0 until values.length()).map { values.getString(it) })
            }
        }
    }.getOrDefault(emptyMap())
}
