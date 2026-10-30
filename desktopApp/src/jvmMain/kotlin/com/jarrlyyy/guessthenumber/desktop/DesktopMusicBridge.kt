package com.jarrlyyy.guessthenumber.desktop

import com.jarrlyyy.guessthenumber.domain.model.GameSettings
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest
import java.util.prefs.Preferences
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip
import javax.sound.sampled.FloatControl
import javax.swing.JFileChooser
import javax.swing.SwingUtilities
import javax.swing.filechooser.FileNameExtensionFilter
import kotlin.math.log10

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
    private var lastSettings: GameSettings? = null
    private val durationCache = mutableMapOf<String, Long>()

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
        var responseSettings = settings
        lastSettings = settings
        fun persist(updated: GameSettings) {
            responseSettings = updated
            lastSettings = updated
            updateSettings(updated)
        }
        try {
            validateRequest(action, request)
            when (action) {
                "import" -> {
                    val files = chooseAudioFiles()
                    if (files.isNotEmpty()) {
                        val added = files.map { it.absolutePath }
                        val library = (settings.musicLibraryPaths + added).distinct()
                        val queue = if (settings.musicQueuePaths.isEmpty()) added else (settings.musicQueuePaths + added).distinct()
                        persist(settings.copy(musicLibraryPaths = library, musicQueuePaths = queue))
                        if (currentPath == null) currentPath = added.first()
                    }
                }
                "play" -> {
                    if (!settings.musicEnabled) error("Enable background music in Settings first.")
                    if (paused && clip != null) { clip?.start(); paused = false }
                    else {
                        val path = currentPath ?: settings.musicQueuePaths.firstOrNull() ?: settings.musicLibraryPaths.firstOrNull()
                            ?: error("Add an audio file from this device first.")
                        playPath(path, settings.volume)
                    }
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
                    persist(settings.copy(musicQueuePaths = (settings.musicQueuePaths + path).distinct()))
                }
                "remove_track" -> {
                    val path = resolveTrack(settings, request.optString("trackId"))
                    if (currentPath == path) stopPlayback()
                    persist(settings.copy(
                        musicLibraryPaths = settings.musicLibraryPaths - path,
                        musicQueuePaths = settings.musicQueuePaths - path,
                        backgroundMusicPath = if (settings.backgroundMusicPath == path) null else settings.backgroundMusicPath
                    ))
                    writePlaylists(readPlaylists().mapValues { (_, paths) -> paths - path })
                    durationCache.remove(path)
                }
                "clear_queue" -> persist(settings.copy(musicQueuePaths = emptyList()))
                "remove_queue" -> {
                    val index = request.optInt("index", -1)
                    val queue = settings.musicQueuePaths.toMutableList()
                    require(index in queue.indices) { "Queue item no longer exists." }
                    queue.removeAt(index)
                    persist(settings.copy(musicQueuePaths = queue))
                }
                "move_queue" -> {
                    val from = request.optInt("index", -1)
                    val to = request.optInt("toIndex", -1)
                    val queue = settings.musicQueuePaths.toMutableList()
                    require(from in queue.indices && to in queue.indices) { "Queue position is invalid." }
                    val item = queue.removeAt(from); queue.add(to, item)
                    persist(settings.copy(musicQueuePaths = queue))
                }
                "seek" -> {
                    val active = clip ?: error("No track is playing.")
                    active.microsecondPosition = request.optLong("position", 0L).coerceIn(0L, active.microsecondLength)
                }
                "create_playlist" -> {
                    val name = request.optString("name").trim()
                    require(name.length in 1..60 && name.none { it.isISOControl() }) { "Enter a valid playlist name." }
                    val playlists = readPlaylists().toMutableMap()
                    require(name !in playlists) { "A playlist with that name already exists." }
                    playlists[name] = emptyList()
                    writePlaylists(playlists)
                }
                "delete_playlist" -> {
                    val name = request.optString("name")
                    val playlists = readPlaylists().toMutableMap()
                    require(playlists.remove(name) != null) { "Playlist not found." }
                    writePlaylists(playlists)
                }
                "play_playlist" -> {
                    val paths = readPlaylists()[request.optString("name")] ?: error("Playlist not found.")
                    require(paths.isNotEmpty()) { "This playlist is empty." }
                    if (!settings.musicEnabled) error("Enable background music in Settings first.")
                    persist(settings.copy(musicQueuePaths = paths))
                    playPath(paths.first(), settings.volume)
                }
                "add_to_playlist", "remove_from_playlist" -> {
                    val name = request.optString("name")
                    val path = resolveTrack(settings, request.optString("trackId"))
                    val playlists = readPlaylists().toMutableMap()
                    val existing = playlists[name]?.toMutableList() ?: error("Playlist not found.")
                    if (action == "add_to_playlist") {
                        if (path !in existing) existing.add(path)
                    } else existing.remove(path)
                    playlists[name] = existing
                    writePlaylists(playlists)
                }
                else -> error("Unsupported music action.")
            }
        } catch (error: Exception) {
            lastError = error.message ?: "The audio action could not be completed."
        }
        return state(responseSettings)
    }

    @Synchronized
    fun applySettings(settings: GameSettings) {
        setVolume(settings.volume)
        if (!settings.musicEnabled) stopPlayback()
    }

    @Synchronized
    fun setVolume(volume: Float) {
        val safe = volume.coerceIn(0f, 1f)
        val activeClip = clip ?: return
        val control = when {
            activeClip.isControlSupported(FloatControl.Type.MASTER_GAIN) ->
                activeClip.getControl(FloatControl.Type.MASTER_GAIN) as? FloatControl
            activeClip.isControlSupported(FloatControl.Type.VOLUME) ->
                activeClip.getControl(FloatControl.Type.VOLUME) as? FloatControl
            else -> null
        } ?: return
        val gain = if (control.type == FloatControl.Type.MASTER_GAIN) {
            20f * log10(safe.coerceAtLeast(0.001f))
        } else {
            safe
        }
        control.value = gain.coerceIn(control.minimum, control.maximum)
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
            next.addLineListener { event ->
                if (event.type == javax.sound.sampled.LineEvent.Type.STOP &&
                    clip === next && !paused && next.microsecondPosition >= next.microsecondLength
                ) {
                    when (repeatMode) {
                        "ONE" -> { next.framePosition = 0; next.start() }
                        "ALL" -> lastSettings?.let { runCatching { moveTrack(it, 1) } }
                    }
                }
            }
            next.start()
        } catch (error: Exception) {
            next.close()
            throw error
        }
    }

    @Synchronized
    private fun stopPlayback() {
        val previous = clip
        clip = null
        paused = true
        previous?.let { runCatching { it.stop(); it.close() } }
        paused = false
    }

    @Synchronized
    private fun moveTrack(settings: GameSettings, delta: Int) {
        if (!settings.musicEnabled) error("Enable background music in Settings first.")
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

    private fun trackId(path: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(path.toByteArray(Charsets.UTF_8))
        return "track_" + digest.take(24).joinToString("") { "%02x".format(it) }
    }

    private fun validateRequest(action: String, request: JSONObject) {
        val allowedFields = setOf("version", "id", "type", "action", "trackId", "index", "toIndex", "position", "name")
        require(request.keys().asSequence().all { it in allowedFields }) { "Invalid music request fields." }
        val extra = when (action) {
            "play", "pause", "stop", "next", "previous", "shuffle", "repeat", "import", "clear_queue" -> emptySet()
            "play_track", "enqueue", "remove_track" -> setOf("trackId")
            "remove_queue" -> setOf("index")
            "move_queue" -> setOf("index", "toIndex")
            "seek" -> setOf("position")
            "create_playlist", "delete_playlist", "play_playlist" -> setOf("name")
            "add_to_playlist", "remove_from_playlist" -> setOf("name", "trackId")
            else -> error("Unsupported music action.")
        }
        require(request.length() == 4 + extra.size && extra.all(request::has)) { "Invalid fields for music action." }
        if (extra.contains("trackId")) require(request.optString("trackId").matches(Regex("track_[a-f0-9]{48}"))) { "Invalid music track." }
        if (extra.contains("index")) require(request.optInt("index", -1) in 0..1000) { "Invalid queue position." }
        if (extra.contains("toIndex")) require(request.optInt("toIndex", -1) in 0..1000) { "Invalid queue position." }
        if (extra.contains("position")) require(request.optLong("position", -1L) in 0L..86_400_000L) { "Invalid playback position." }
        if (extra.contains("name")) require(request.optString("name").trim().length in 1..60) { "Invalid playlist name." }
    }

    private fun writePlaylists(playlists: Map<String, List<String>>) {
        val json = JSONObject()
        playlists.forEach { (name, paths) -> json.put(name, JSONArray(paths)) }
        preferences.put("playlists", json.toString())
        preferences.flush()
    }

    private fun durationMillis(path: String): Long = durationCache.getOrPut(path) {
        runCatching {
            AudioSystem.getAudioInputStream(File(path)).use { stream ->
                if (stream.format.frameRate <= 0f) 0L
                else (stream.frameLength / stream.format.frameRate * 1000.0).toLong().coerceAtLeast(0L)
            }
        }.getOrDefault(0L)
    }

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
