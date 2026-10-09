package com.jarrlyyy.guessthenumber.data.audio

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import kotlin.random.Random

class BackgroundMusicManager(context: Context) {
    data class Track(
        val id: String,
        val path: String,
        val title: String,
        val artist: String = "",
        val album: String = "",
        val duration: Int = 0
    )

    enum class RepeatMode { OFF, ONE, ALL }

    private val appContext = context.applicationContext
    private val preferences = appContext.getSharedPreferences("offline_music_library", Context.MODE_PRIVATE)
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var mediaPlayer: MediaPlayer? = null
    private var progressJob: Job? = null
    private var queueIndex = 0

    private val _library = MutableStateFlow(loadLibrary())
    val library: StateFlow<List<Track>> = _library.asStateFlow()
    private val _queue = MutableStateFlow<List<String>>(loadQueue())
    val queue: StateFlow<List<String>> = _queue.asStateFlow()
    private val _repeatMode = MutableStateFlow(loadRepeatMode())
    val repeatMode: StateFlow<RepeatMode> = _repeatMode.asStateFlow()
    private val _shuffle = MutableStateFlow(preferences.getBoolean("shuffle", false))
    val shuffle: StateFlow<Boolean> = _shuffle.asStateFlow()
    private val _playlists = MutableStateFlow(loadPlaylists())
    val playlists: StateFlow<Map<String, List<String>>> = _playlists.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()
    private val _currentPath = MutableStateFlow<String?>(null)
    val currentPath: StateFlow<String?> = _currentPath.asStateFlow()
    private val _currentTrackId = MutableStateFlow<String?>(preferences.getString("current_track", null))
    val currentTrackId: StateFlow<String?> = _currentTrackId.asStateFlow()
    private val _currentPosition = MutableStateFlow(0)
    val currentPosition: StateFlow<Int> = _currentPosition.asStateFlow()
    private val _duration = MutableStateFlow(0)
    val duration: StateFlow<Int> = _duration.asStateFlow()
    private val _albumArt = MutableStateFlow<Bitmap?>(null)
    val albumArt: StateFlow<Bitmap?> = _albumArt.asStateFlow()

    init { startProgressTracker() }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                delay(200)
                mediaPlayer?.let { player ->
                    try {
                        _currentPosition.value = player.currentPosition
                        _duration.value = player.duration
                        if (!player.isPlaying && player.currentPosition >= player.duration - 250) advance()
                    } catch (_: Exception) {}
                }
            }
        }
    }

    fun importTrack(uri: Uri, displayName: String? = null): Track? {
        return try {
            val musicDir = File(appContext.filesDir, "music").apply { mkdirs() }
            val id = "track_${System.currentTimeMillis()}_${Random.nextInt(1000, 9999)}"
            val destination = File(musicDir, "$id.audio")
            appContext.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destination).use { output -> input.copyTo(output) }
            } ?: return null
            val metadata = readMetadata(destination)
            val track = Track(id, destination.absolutePath, displayName ?: metadata.title ?: destination.nameWithoutExtension, metadata.artist.orEmpty(), metadata.album.orEmpty(), metadata.duration)
            val updated = _library.value + track
            _library.value = updated
            persistLibrary(updated)
            track
        } catch (_: Exception) { null }
    }

    fun removeTrack(trackId: String) {
        val track = _library.value.firstOrNull { it.id == trackId } ?: return
        if (_currentTrackId.value == trackId) stop()
        runCatching { File(track.path).delete() }
        val updated = _library.value.filterNot { it.id == trackId }
        _library.value = updated
        persistLibrary(updated)
        _queue.value = _queue.value.filterNot { it == trackId }
        persistQueue()
        _playlists.value = _playlists.value.mapValues { (_, ids) -> ids.filterNot { it == trackId } }
        persistPlaylists()
    }

    fun createPlaylist(name: String): Boolean {
        val clean = name.trim()
        if (clean.isEmpty() || _playlists.value.containsKey(clean)) return false
        _playlists.value = _playlists.value + (clean to emptyList())
        persistPlaylists()
        return true
    }

    fun deletePlaylist(name: String) {
        _playlists.value = _playlists.value - name
        persistPlaylists()
    }

    fun addToPlaylist(name: String, trackId: String): Boolean {
        if (trackId !in _library.value.map { it.id } || name !in _playlists.value) return false
        val ids = _playlists.value.getValue(name)
        if (trackId in ids) return true
        _playlists.value = _playlists.value + (name to ids + trackId)
        persistPlaylists()
        return true
    }

    fun removeFromPlaylist(name: String, trackId: String) {
        val ids = _playlists.value[name] ?: return
        _playlists.value = _playlists.value + (name to ids.filterNot { it == trackId })
        persistPlaylists()
    }

    fun playQueue(trackIds: List<String>, startIndex: Int = 0) {
        _queue.value = trackIds.filter { id -> _library.value.any { it.id == id } }
        persistQueue()
        queueIndex = startIndex.coerceIn(0, (_queue.value.size - 1).coerceAtLeast(0))
        playQueueIndex()
    }

    fun enqueue(trackId: String) {
        if (trackId !in _library.value.map { it.id }) return
        _queue.value = _queue.value + trackId
        persistQueue()
    }

    fun removeFromQueue(index: Int) {
        if (index !in _queue.value.indices) return
        _queue.value = _queue.value.toMutableList().also { it.removeAt(index) }
        if (queueIndex >= _queue.value.size) queueIndex = (_queue.value.size - 1).coerceAtLeast(0)
        persistQueue()
    }

    fun clearQueue() {
        _queue.value = emptyList()
        queueIndex = 0
        persistQueue()
    }

    fun setShuffle(enabled: Boolean) {
        _shuffle.value = enabled
        preferences.edit().putBoolean("shuffle", enabled).apply()
    }

    fun setRepeatMode(mode: RepeatMode) {
        _repeatMode.value = mode
        preferences.edit().putString("repeat", mode.name).apply()
    }

    fun next() = advance()
    fun previous() {
        if (_currentPosition.value > 3000) { seekTo(0); return }
        queueIndex = (queueIndex - 1).coerceAtLeast(0)
        playQueueIndex()
    }

    private fun advance() {
        if (_repeatMode.value == RepeatMode.ONE && _currentTrackId.value != null) {
            playTrack(_currentTrackId.value!!)
            return
        }
        if (_queue.value.isEmpty()) return
        queueIndex = if (_shuffle.value) Random.nextInt(_queue.value.size) else queueIndex + 1
        if (queueIndex >= _queue.value.size) {
            if (_repeatMode.value == RepeatMode.ALL) queueIndex = 0 else { stop(); return }
        }
        playQueueIndex()
    }

    private fun playQueueIndex() {
        _queue.value.getOrNull(queueIndex)?.let(::playTrack)
    }

    fun setSourceAndPlay(path: String?) {
        val track = _library.value.firstOrNull { it.path == path }
        if (track != null) playTrack(track.id) else if (!path.isNullOrEmpty()) startPath(path, null)
    }

    fun playTrack(trackId: String) {
        val track = _library.value.firstOrNull { it.id == trackId } ?: return
        startPath(track.path, track)
    }

    private fun startPath(path: String, track: Track?) {
        if (!File(path).exists()) {
            track?.let { removeTrack(it.id) }
            stop()
            return
        }
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(path)
                setOnCompletionListener { advance() }
                prepare()
                start()
            }
            _currentPath.value = path
            _currentTrackId.value = track?.id
            preferences.edit().putString("current_track", track?.id).apply()
            _duration.value = mediaPlayer?.duration ?: track?.duration ?: 0
            _currentPosition.value = 0
            extractAlbumArt(path)
            _isPlaying.value = true
        } catch (_: Exception) { _isPlaying.value = false }
    }

    fun play() {
        try {
            if (mediaPlayer == null) {
                _currentTrackId.value?.let(::playTrack) ?: _currentPath.value?.let { startPath(it, null) }
            } else if (mediaPlayer?.isPlaying == false) {
                mediaPlayer?.start()
                _isPlaying.value = true
            }
        } catch (_: Exception) {}
    }

    fun pause() {
        runCatching { mediaPlayer?.pause(); _isPlaying.value = false }
    }

    fun stop() {
        runCatching { mediaPlayer?.stop(); mediaPlayer?.release() }
        mediaPlayer = null
        _isPlaying.value = false
        _currentPosition.value = 0
    }

    fun seekTo(position: Int) {
        runCatching { mediaPlayer?.seekTo(position.coerceAtLeast(0)); _currentPosition.value = position.coerceAtLeast(0) }
    }

    fun release() {
        progressJob?.cancel()
        scope.cancel()
        runCatching { mediaPlayer?.release() }
        mediaPlayer = null
        _isPlaying.value = false
    }

    private fun extractAlbumArt(path: String) {
        try {
            MediaMetadataRetriever().use { retriever ->
                retriever.setDataSource(path)
                retriever.embeddedPicture?.let { bytes -> _albumArt.value = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) } ?: run { _albumArt.value = null }
            }
        } catch (_: Exception) { _albumArt.value = null }
    }

    private data class Metadata(val title: String?, val artist: String?, val album: String?, val duration: Int)
    private fun readMetadata(file: File): Metadata {
        return try {
            MediaMetadataRetriever().use { retriever ->
                retriever.setDataSource(file.absolutePath)
                Metadata(
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE),
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST),
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM),
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toIntOrNull() ?: 0
                )
            }
        } catch (_: Exception) { Metadata(null, null, null, 0) }
    }

    private fun persistLibrary(items: List<Track>) {
        val array = JSONArray()
        items.forEach { array.put(JSONObject().apply { put("id", it.id); put("path", it.path); put("title", it.title); put("artist", it.artist); put("album", it.album); put("duration", it.duration) }) }
        preferences.edit().putString("library", array.toString()).apply()
    }

    private fun loadLibrary(): List<Track> {
        return runCatching {
            val array = JSONArray(preferences.getString("library", "[]"))
            buildList { for (i in 0 until array.length()) { val o = array.getJSONObject(i); if (File(o.getString("path")).exists()) add(Track(o.getString("id"), o.getString("path"), o.getString("title"), o.optString("artist"), o.optString("album"), o.optInt("duration"))) } }
        }.getOrDefault(emptyList())
    }

    private fun persistQueue() = preferences.edit().putString("queue", JSONArray(_queue.value).toString()).apply()
    private fun loadQueue(): List<String> = runCatching { val a = JSONArray(preferences.getString("queue", "[]")); buildList { for (i in 0 until a.length()) add(a.getString(i)) } }.getOrDefault(emptyList())
    private fun loadRepeatMode() = runCatching { RepeatMode.valueOf(preferences.getString("repeat", RepeatMode.OFF.name)!!) }.getOrDefault(RepeatMode.OFF)

    private fun persistPlaylists() {
        val root = JSONObject()
        _playlists.value.forEach { (name, ids) -> root.put(name, JSONArray(ids)) }
        preferences.edit().putString("playlists", root.toString()).apply()
    }

    private fun loadPlaylists(): Map<String, List<String>> = runCatching {
        val root = JSONObject(preferences.getString("playlists", "{}"))
        buildMap { root.keys().forEach { name -> val a = root.getJSONArray(name); put(name, buildList { for (i in 0 until a.length()) add(a.getString(i)) }) } }
    }.getOrDefault(emptyMap())
}
