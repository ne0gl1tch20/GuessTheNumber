package com.jarrlyyy.guessthenumber.data.audio

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

class BackgroundMusicManager(context: Context) {
    private val appContext = context.applicationContext
    private var mediaPlayer: MediaPlayer? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPath = MutableStateFlow<String?>(null)
    val currentPath: StateFlow<String?> = _currentPath.asStateFlow()

    fun setSourceAndPlay(path: String?) {
        _currentPath.value = path
        if (!path.isNullOrEmpty() && File(path).exists()) {
            try {
                mediaPlayer?.release()
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(appContext, Uri.fromFile(File(path)))
                    isLooping = true
                    prepare()
                    start()
                }
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
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun release() {
        try {
            mediaPlayer?.release()
            mediaPlayer = null
            _isPlaying.value = false
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
