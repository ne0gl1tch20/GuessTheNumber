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
import java.io.File

class BackgroundMusicManager(context: Context) {
    private val appContext = context.applicationContext
    private var mediaPlayer: MediaPlayer? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var progressJob: Job? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPath = MutableStateFlow<String?>(null)
    val currentPath: StateFlow<String?> = _currentPath.asStateFlow()

    private val _currentPosition = MutableStateFlow(0)
    val currentPosition: StateFlow<Int> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0)
    val duration: StateFlow<Int> = _duration.asStateFlow()

    private val _albumArt = MutableStateFlow<Bitmap?>(null)
    val albumArt: StateFlow<Bitmap?> = _albumArt.asStateFlow()

    init {
        startProgressTracker()
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                delay(200)
                mediaPlayer?.let { player ->
                    try {
                        if (player.isPlaying) {
                            _currentPosition.value = player.currentPosition
                            _duration.value = player.duration
                        }
                    } catch (_: Exception) {}
                }
            }
        }
    }

    private fun extractMetadata(path: String?) {
        if (path.isNullOrEmpty() || !File(path).exists()) {
            _albumArt.value = null
            _duration.value = 0
            _currentPosition.value = 0
            return
        }
        try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(path)
            val artBytes = retriever.embeddedPicture
            if (artBytes != null) {
                _albumArt.value = BitmapFactory.decodeByteArray(artBytes, 0, artBytes.size)
            } else {
                _albumArt.value = null
            }
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            _duration.value = durationStr?.toIntOrNull() ?: 0
            retriever.release()
        } catch (e: Exception) {
            e.printStackTrace()
            _albumArt.value = null
        }
    }

    fun setSourceAndPlay(path: String?) {
        _currentPath.value = path
        extractMetadata(path)
        if (!path.isNullOrEmpty() && File(path).exists()) {
            try {
                mediaPlayer?.release()
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(appContext, Uri.fromFile(File(path)))
                    isLooping = true
                    prepare()
                    start()
                }
                _duration.value = mediaPlayer?.duration ?: 0
                _currentPosition.value = 0
                _isPlaying.value = true
            } catch (e: Exception) {
                e.printStackTrace()
                _isPlaying.value = false
            }
        } else {
            stop()
        }
    }

    fun play() {
        val path = _currentPath.value
        if (!path.isNullOrEmpty() && File(path).exists()) {
            try {
                if (mediaPlayer == null) {
                    setSourceAndPlay(path)
                } else if (mediaPlayer?.isPlaying == false) {
                    mediaPlayer?.start()
                    _isPlaying.value = true
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun pause() {
        try {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.pause()
                _isPlaying.value = false
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stop() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
            _isPlaying.value = false
            _currentPosition.value = 0
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun seekTo(position: Int) {
        try {
            mediaPlayer?.seekTo(position)
            _currentPosition.value = position
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun release() {
        try {
            progressJob?.cancel()
            scope.cancel()
            mediaPlayer?.release()
            mediaPlayer = null
            _isPlaying.value = false
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
