package com.jarrlyyy.guessthenumber.desktop

import com.jarrlyyy.guessthenumber.domain.model.GameSettings
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.nio.file.Files
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicReference
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip
import javax.sound.sampled.FloatControl
import javax.sound.sampled.LineEvent
import javax.swing.JFileChooser
import javax.swing.SwingUtilities
import javax.swing.filechooser.FileNameExtensionFilter
import kotlin.math.log10
import kotlin.random.Random

/**
 * Windows music host for the canonical shared HTML music screen.
 *
 * Library, queue, playlist, repeat and shuffle data live in GameSettings so the
 * same save can move between hosts. Playback itself remains an OS-owned resource.
 */
internal class DesktopMusicPlayerHost : AutoCloseable {
    private val lock = Any()
    private var clip: Clip? = null
    private var currentPath: String? = null
    private var isPlaying = false
    private var queueIndex = 0
    private var currentSettings = GameSettings()

    fun stateJson(settings: GameSettings): JSONObject = synchronized(lock) {
        currentSettings = settings
        applyVolume(settings.volume)
        val libraryPaths = settings.musicLibraryPaths.distinct().filter { File(it).isFile }
        val queuePaths = settings.musicQueuePaths.filter { it in libraryPaths }
        val library = JSONArray().apply {
            libraryPaths.forEach { path ->
                val file = File(path)
                val metadata = metadata(file)
                put(
                    JSONObject()
                        .put("id", trackId(path))
                        .put("title", metadata.title.ifBlank { file.nameWithoutExtension })
                        .put("artist", metadata.artist)
                        .put("album", metadata.album)
                        .put("duration", metadata.durationMs)
                )
            }
        }
        val queue = JSONArray().apply { queuePaths.forEach { put(trackId(it)) } }
        val playlists = JSONObject().apply {
            settings.musicPlaylistPaths.forEach { (name, paths) ->
                put(name, JSONArray().apply {
                    paths.filter { it in libraryPaths }.forEach { put(trackId(it)) }
                })
            }
        }
        val duration = clip?.microsecondLength?.div(1_000L)?.toInt()
            ?: currentPath?.let { metadata(File(it)).durationMs }
            ?: 0
        val position = clip?.microsecondPosition?.div(1_000L)?.toInt() ?: 0
        JSONObject()
            .put("library", library)
            .put("queue", queue)
            .put("playlists", playlists)
            .put("currentTrackId", currentPath?.takeIf { it in libraryPaths }?.let(::trackId) ?: "")
            .put("currentPosition", position.coerceAtLeast(0))
            .put("duration", duration.coerceAtLeast(0))
            .put("isPlaying", isPlaying)
            .put("shuffle", settings.musicShuffle)
            .put("repeatMode", settings.musicRepeatMode.uppercase())
    }

    fun handleAction(
        request: JSONObject,
        settings: GameSettings,
        updateSettings: (GameSettings) -> Unit
    ) = synchronized(lock) {
        currentSettings = settings
        val allowedFields = setOf("version", "id", "type", "action", "trackId", "index", "toIndex", "position", "name")
        require(request.keys().asSequence().all { it in allowedFields }) { "The music request contains unsupported fields." }
        val action = request.optString("action")
        val trackId = request.optString("trackId").takeIf { request.has("trackId") }
        val index = if (request.has("index")) request.optInt("index", -1) else null
        val toIndex = if (request.has("toIndex")) request.optInt("toIndex", -1) else null
        val position = if (request.has("position")) request.optInt("position", -1) else null
        val name = request.optString("name").takeIf { request.has("name") }?.trim()
        val library = settings.musicLibraryPaths.distinct().filter { File(it).isFile }

        when (action) {
            "import" -> {
                val imported = chooseAndImport()
                if (imported.isNotEmpty()) {
                    val queue = if (settings.musicQueuePaths.isEmpty()) imported else settings.musicQueuePaths + imported
                    updateSettings(settings.copy(
                        musicLibraryPaths = (settings.musicLibraryPaths + imported).distinct(),
                        musicQueuePaths = queue.distinct()
                    ))
                }
            }
            "play" -> {
                require(settings.musicEnabled) { "Enable music in Settings before playing audio." }
                val active = clip
                if (active != null) {
                    isPlaying = true
                    active.start()
                } else {
                    val path = currentPath ?: settings.musicQueuePaths.firstOrNull { it in library }
                        ?: library.firstOrNull()
                        ?: throw IllegalArgumentException("Add audio files to your library first.")
                    playPath(path, settings.volume)
                }
            }
            "pause" -> pausePlayback()
            "stop" -> stopPlayback()
            "next" -> advance(settings)
            "previous" -> {
                if (currentPositionMs() > 3_000) seek(0)
                else {
                    val queue = settings.musicQueuePaths.filter { it in library }
                    queueIndex = (queueIndex - 1).coerceAtLeast(0)
                    queue.getOrNull(queueIndex)?.let { playPath(it, settings.volume) }
                }
            }
            "shuffle" -> updateSettings(settings.copy(musicShuffle = !settings.musicShuffle))
            "repeat" -> {
                val modes = listOf("off", "one", "all")
                val current = settings.musicRepeatMode.lowercase().takeIf { it in modes } ?: "off"
                updateSettings(settings.copy(musicRepeatMode = modes[(modes.indexOf(current) + 1) % modes.size]))
            }
            "play_track" -> {
                val path = resolveTrack(trackId, library)
                require(settings.musicEnabled) { "Enable music in Settings before playing audio." }
                queueIndex = settings.musicQueuePaths.indexOf(path)
                playPath(path, settings.volume)
            }
            "enqueue" -> {
                val path = resolveTrack(trackId, library)
                updateSettings(settings.copy(musicQueuePaths = settings.musicQueuePaths + path))
            }
            "remove_track" -> {
                val path = resolveTrack(trackId, library)
                if (currentPath == path) clearCurrentTrack()
                updateSettings(settings.copy(
                    musicLibraryPaths = settings.musicLibraryPaths.filterNot { it == path },
                    musicQueuePaths = settings.musicQueuePaths.filterNot { it == path },
                    musicPlaylistPaths = settings.musicPlaylistPaths.mapValues { (_, paths) -> paths.filterNot { it == path } },
                    backgroundMusicPath = settings.backgroundMusicPath?.takeIf { it != path }
                ))
            }
            "add_to_playlist" -> {
                val playlist = validPlaylistName(name)
                val path = resolveTrack(trackId, library)
                require(playlist in settings.musicPlaylistPaths) { "Playlist not found." }
                val paths = settings.musicPlaylistPaths.getValue(playlist)
                if (path !in paths) updateSettings(settings.copy(
                    musicPlaylistPaths = settings.musicPlaylistPaths + (playlist to (paths + path))
                ))
            }
            "remove_from_playlist" -> {
                val playlist = validPlaylistName(name)
                val path = resolveTrack(trackId, library)
                val paths = settings.musicPlaylistPaths[playlist] ?: throw IllegalArgumentException("Playlist not found.")
                updateSettings(settings.copy(
                    musicPlaylistPaths = settings.musicPlaylistPaths + (playlist to paths.filterNot { it == path })
                ))
            }
            "clear_queue" -> {
                queueIndex = 0
                updateSettings(settings.copy(musicQueuePaths = emptyList()))
            }
            "remove_queue" -> {
                val queue = settings.musicQueuePaths.filter { it in library }
                require(index != null && index in queue.indices) { "Queue position is invalid." }
                val removed = queue.toMutableList().also { it.removeAt(index) }
                queueIndex = when {
                    removed.isEmpty() -> 0
                    index <= queueIndex -> queueIndex - 1
                    else -> queueIndex
                }.coerceIn(0, (removed.lastIndex).coerceAtLeast(0))
                updateSettings(settings.copy(musicQueuePaths = removed))
            }
            "move_queue" -> {
                val queue = settings.musicQueuePaths.filter { it in library }
                require(index != null && toIndex != null && index in queue.indices && toIndex in queue.indices) {
                    "Queue position is invalid."
                }
                val moved = queue.toMutableList().also { item -> item.add(toIndex, item.removeAt(index)) }
                queueIndex = when {
                    queueIndex == index -> toIndex
                    index < queueIndex && toIndex >= queueIndex -> queueIndex - 1
                    index > queueIndex && toIndex <= queueIndex -> queueIndex + 1
                    else -> queueIndex
                }
                updateSettings(settings.copy(musicQueuePaths = moved))
            }
            "create_playlist" -> {
                val playlist = validPlaylistName(name)
                require(playlist !in settings.musicPlaylistPaths) { "A playlist with that name already exists." }
                updateSettings(settings.copy(musicPlaylistPaths = settings.musicPlaylistPaths + (playlist to emptyList())))
            }
            "delete_playlist" -> {
                val playlist = validPlaylistName(name)
                require(playlist in settings.musicPlaylistPaths) { "Playlist not found." }
                updateSettings(settings.copy(musicPlaylistPaths = settings.musicPlaylistPaths - playlist))
            }
            "play_playlist" -> {
                val playlist = validPlaylistName(name)
                val paths = settings.musicPlaylistPaths[playlist]?.filter { it in library }
                    ?: throw IllegalArgumentException("Playlist not found.")
                require(settings.musicEnabled) { "Enable music in Settings before playing audio." }
                val updatedSettings = settings.copy(musicQueuePaths = paths)
                currentSettings = updatedSettings
                updateSettings(updatedSettings)
                queueIndex = 0
                paths.firstOrNull()?.let { playPath(it, settings.volume) } ?: stopPlayback()
            }
            "seek" -> {
                require(position != null && position in 0..86_400_000) { "Playback position is invalid." }
                seek(position)
            }
            else -> throw IllegalArgumentException("Unsupported music action.")
        }
    }

    fun setEnabled(enabled: Boolean) = synchronized(lock) {
        if (!enabled) stopPlayback()
    }

    fun setVolume(volume: Float) = synchronized(lock) {
        applyVolume(volume)
    }

    private fun chooseAndImport(): List<String> {
        val selected = AtomicReference<List<File>>(emptyList())
        val showChooser = Runnable {
            val chooser = JFileChooser().apply {
                isMultiSelectionEnabled = true
                fileFilter = FileNameExtensionFilter("Audio files (WAV, AIFF, AU)", "wav", "aiff", "aif", "au")
            }
            if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                selected.set(chooser.selectedFiles.toList().ifEmpty { listOfNotNull(chooser.selectedFile) })
            }
        }
        if (SwingUtilities.isEventDispatchThread()) showChooser.run() else SwingUtilities.invokeAndWait(showChooser)

        if (selected.get().isEmpty()) return emptyList()
        val musicDirectory = defaultDesktopSaveDirectory().resolve("music")
        Files.createDirectories(musicDirectory)
        val imported = mutableListOf<String>()
        selected.get().forEach { source ->
            require(source.isFile && source.length() in 1..MAX_IMPORT_BYTES) {
                "Each imported music file must be between 1 byte and 100 MiB."
            }
            val extension = source.extension.lowercase().takeIf { it in SUPPORTED_EXTENSIONS }
                ?: throw IllegalArgumentException("Only WAV, AIFF and AU files are supported on Windows.")
            val destination = musicDirectory.resolve(System.currentTimeMillis().toString() + "-" + source.nameWithoutExtension.take(48) + "." + extension)
            Files.newInputStream(source.toPath()).use { input ->
                Files.newOutputStream(destination).use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var total = 0L
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        total += count
                        require(total <= MAX_IMPORT_BYTES) { "Music file exceeds the 100 MiB import limit." }
                        output.write(buffer, 0, count)
                    }
                }
            }
            runCatching { AudioSystem.getAudioFileFormat(destination.toFile()) }
                .getOrElse {
                    Files.deleteIfExists(destination)
                    throw IllegalArgumentException("Windows Java Sound cannot read " + source.name + ".")
                }
            imported += destination.toAbsolutePath().normalize().toString()
        }
        return imported
    }

    private fun playPath(path: String, volume: Float) {
        require(File(path).isFile) { "The selected audio file no longer exists." }
        stopClipOnly()
        val next = AudioSystem.getClip()
        try {
            AudioSystem.getAudioInputStream(File(path)).use { stream -> next.open(stream) }
            clip = next
            currentPath = path
            isPlaying = true
            next.addLineListener { event ->
                if (event.type == LineEvent.Type.STOP) {
                    synchronized(lock) {
                        if (clip === next && isPlaying && next.microsecondPosition >= next.microsecondLength - 10_000L) {
                            clip = null
                            isPlaying = false
                            next.close()
                            advance(currentSettings)
                        }
                    }
                }
            }
            applyVolume(volume)
            next.start()
        } catch (error: Exception) {
            if (clip === next) clip = null
            isPlaying = false
            next.close()
            throw IllegalStateException("Could not play this audio file: " + (error.message ?: error.javaClass.simpleName), error)
        }
    }

    private fun advance(settings: GameSettings) {
        val queue = settings.musicQueuePaths.filter { File(it).isFile }
        if (queue.isEmpty()) {
            stopPlayback()
            return
        }
        if (settings.musicRepeatMode.equals("one", ignoreCase = true) && currentPath in queue) {
            currentPath?.let { playPath(it, settings.volume) }
            return
        }
        val nextIndex = if (settings.musicShuffle) Random.nextInt(queue.size) else queueIndex + 1
        queueIndex = if (nextIndex >= queue.size) {
            if (settings.musicRepeatMode.equals("all", ignoreCase = true)) 0 else {
                stopPlayback()
                return
            }
        } else nextIndex
        playPath(queue[queueIndex], settings.volume)
    }

    private fun resolveTrack(id: String?, library: List<String>): String {
        require(id != null && id.matches(Regex("track_[A-Za-z0-9_-]{1,64}"))) { "Music track ID is invalid." }
        return library.firstOrNull { trackId(it) == id }
            ?: throw IllegalArgumentException("The selected track is no longer in your library.")
    }

    private fun validPlaylistName(name: String?): String {
        val value = name?.trim().orEmpty()
        require(value.length in 1..60 && value.none(Char::isISOControl)) { "Playlist name must be 1–60 characters." }
        return value
    }

    private fun seek(positionMs: Int) {
        val active = clip ?: return
        val bounded = positionMs.coerceIn(0, (active.microsecondLength / 1_000L).toInt())
        active.microsecondPosition = bounded * 1_000L
    }

    private fun currentPositionMs(): Int = (clip?.microsecondPosition?.div(1_000L) ?: 0L).toInt()

    private fun pausePlayback() {
        isPlaying = false
        clip?.stop()
    }

    private fun stopPlayback() {
        stopClipOnly()
    }

    private fun stopClipOnly() {
        val previous = clip
        clip = null
        isPlaying = false
        if (previous != null) {
            runCatching { previous.stop() }
            runCatching { previous.close() }
        }
    }

    private fun clearCurrentTrack() {
        stopClipOnly()
        currentPath = null
    }

    private fun applyVolume(volume: Float) {
        val control = clip?.takeIf { it.isControlSupported(FloatControl.Type.MASTER_GAIN) }
            ?.getControl(FloatControl.Type.MASTER_GAIN) as? FloatControl ?: return
        val gain = if (volume <= 0f) control.minimum else (20f * log10(volume.coerceIn(0.001f, 1f)))
        control.value = gain.coerceIn(control.minimum, control.maximum)
    }

    private data class TrackMetadata(
        val title: String,
        val artist: String,
        val album: String,
        val durationMs: Int
    )

    private fun metadata(file: File): TrackMetadata {
        val titleFallback = file.nameWithoutExtension
        return runCatching {
            val audioFormat = AudioSystem.getAudioFileFormat(file)
            val properties = audioFormat.properties()
            val title = properties["title"]?.toString()?.take(160).orEmpty().ifBlank { titleFallback }
            val artist = (properties["author"] ?: properties["artist"])?.toString()?.take(160).orEmpty()
            val album = properties["album"]?.toString()?.take(160).orEmpty()
            val duration = AudioSystem.getAudioInputStream(file).use { stream ->
                val frames = stream.frameLength
                val rate = stream.format.frameRate
                if (frames > 0 && rate > 0f) ((frames / rate) * 1_000f).toInt() else 0
            }
            TrackMetadata(title, artist, album, duration.coerceAtLeast(0))
        }.getOrElse { TrackMetadata(titleFallback, "", "", 0) }
    }

    private fun trackId(path: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(path.toByteArray(Charsets.UTF_8))
        val token = digest.take(16).joinToString("") { byte -> "%02x".format(byte) }
        return "track_$token"
    }

    override fun close() = synchronized(lock) {
        stopClipOnly()
        currentPath = null
    }

    private companion object {
        const val MAX_IMPORT_BYTES = 100L * 1024L * 1024L
        val SUPPORTED_EXTENSIONS = setOf("wav", "aiff", "aif", "au")
    }
}
