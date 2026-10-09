package com.jarrlyyy.guessthenumber.data.audio

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.media.session.MediaButtonReceiver
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import com.jarrlyyy.guessthenumber.MainActivity
import com.jarrlyyy.guessthenumber.R

class MusicPlaybackService : Service() {
    private lateinit var manager: BackgroundMusicManager
    private lateinit var mediaSession: MediaSessionCompat
    private val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private val stateUpdater = object : Runnable {
        override fun run() {
            updateSessionAndNotification()
            mainHandler.postDelayed(this, 500L)
        }
    }

    override fun onCreate() {
        super.onCreate()
        manager = BackgroundMusicManager(applicationContext)
        createChannel()

        mediaSession = MediaSessionCompat(this, "GuessTheNumberMusic").apply {
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() = manager.play()
                override fun onPause() = manager.pause()
                override fun onStop() = stopPlayback()
                override fun onSkipToNext() = manager.next()
                override fun onSkipToPrevious() = manager.previous()
                override fun onSeekTo(pos: Long) = manager.seekTo(pos.toInt())
                override fun onMediaButtonEvent(mediaButtonEvent: Intent): Boolean {
                    MediaButtonReceiver.handleIntent(this@apply, mediaButtonEvent)
                    return true
                }
            })
            isActive = true
        }

        startForeground(NOTIFICATION_ID, buildNotification())
        mainHandler.post(stateUpdater)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        MediaButtonReceiver.handleIntent(mediaSession, intent)
        when (intent?.action) {
            ACTION_PLAY -> manager.play()
            ACTION_PAUSE -> manager.pause()
            ACTION_NEXT -> manager.next()
            ACTION_PREVIOUS -> manager.previous()
            ACTION_STOP -> stopPlayback()
        }
        updateSessionAndNotification()
        return START_STICKY
    }

    override fun onDestroy() {
        mainHandler.removeCallbacks(stateUpdater)
        mediaSession.isActive = false
        mediaSession.release()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun stopPlayback() {
        manager.stop()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun updateSessionAndNotification() {
        val track = manager.library.value.firstOrNull { it.id == manager.currentTrackId.value }
        val playing = manager.isPlaying.value
        val state = if (playing) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED
        val actions = PlaybackStateCompat.ACTION_PLAY or
            PlaybackStateCompat.ACTION_PAUSE or
            PlaybackStateCompat.ACTION_PLAY_PAUSE or
            PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
            PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
            PlaybackStateCompat.ACTION_SEEK_TO or
            PlaybackStateCompat.ACTION_STOP
        mediaSession.setPlaybackState(
            PlaybackStateCompat.Builder()
                .setActions(actions)
                .setState(state, manager.currentPosition.value.toLong(), if (playing) 1f else 0f)
                .build()
        )
        mediaSession.setMetadata(
            MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, track?.title ?: getString(R.string.app_name))
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, track?.artist.orEmpty())
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, track?.album.orEmpty())
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, manager.duration.value.toLong())
                .build()
        )
        val notification = buildNotification()
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification)
    }

    private fun buildNotification(): Notification {
        val track = manager.library.value.firstOrNull { it.id == manager.currentTrackId.value }
        val openIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val previous = PendingIntent.getService(this, 1, commandIntent(ACTION_PREVIOUS), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val playPause = PendingIntent.getService(this, 2, commandIntent(if (manager.isPlaying.value) ACTION_PAUSE else ACTION_PLAY), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val next = PendingIntent.getService(this, 3, commandIntent(ACTION_NEXT), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val stop = PendingIntent.getService(this, 4, commandIntent(ACTION_STOP), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(track?.title ?: getString(R.string.app_name))
            .setContentText(track?.artist?.takeIf { it.isNotBlank() } ?: getString(R.string.app_name))
            .setContentIntent(openIntent)
            .setOngoing(manager.isPlaying.value)
            .setOnlyAlertOnce(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(NotificationCompat.Action(android.R.drawable.ic_media_previous, "Previous", previous))
            .addAction(NotificationCompat.Action(if (manager.isPlaying.value) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play, if (manager.isPlaying.value) "Pause" else "Play", playPause))
            .addAction(NotificationCompat.Action(android.R.drawable.ic_media_next, "Next", next))
            .addAction(NotificationCompat.Action(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stop))
            .setStyle(MediaNotificationStyle(mediaSession.sessionToken))
            .build()
    }

    private fun commandIntent(action: String): Intent = Intent(this, MusicPlaybackService::class.java).setAction(action)

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, getString(R.string.app_name), NotificationManager.IMPORTANCE_LOW).apply {
                description = getString(R.string.app_name)
                setShowBadge(false)
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    companion object {
        const val ACTION_PLAY = "com.jarrlyyy.guessthenumber.music.PLAY"
        const val ACTION_PAUSE = "com.jarrlyyy.guessthenumber.music.PAUSE"
        const val ACTION_NEXT = "com.jarrlyyy.guessthenumber.music.NEXT"
        const val ACTION_PREVIOUS = "com.jarrlyyy.guessthenumber.music.PREVIOUS"
        const val ACTION_STOP = "com.jarrlyyy.guessthenumber.music.STOP"
        private const val CHANNEL_ID = "music_playback"
        private const val NOTIFICATION_ID = 4101
    }
}

private class MediaNotificationStyle(token: android.support.v4.media.session.MediaSessionCompat.Token) : NotificationCompat.MediaStyle() {
    init {
        setMediaSession(token)
        setShowActionsInCompactView(0, 1, 2)
    }
}
