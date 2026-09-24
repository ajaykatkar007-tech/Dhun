package com.dhun.music.playback

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.app.ServiceCompat
import androidx.media.app.NotificationCompat.MediaStyle
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import com.dhun.music.MainActivity
import com.dhun.music.R
import com.dhun.music.model.Song

class DhunPlaybackService : Service() {
    private lateinit var mediaSession: MediaSessionCompat

    private val noisyReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == android.media.AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                AudioPlaybackManager.getInstance(applicationContext).pause()
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        mediaSession = MediaSessionCompat(this, "Dhun").apply {
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() = AudioPlaybackManager.getInstance(applicationContext).resume()
                override fun onPause() = AudioPlaybackManager.getInstance(applicationContext).pause()
                override fun onSkipToNext() = AudioPlaybackManager.getInstance(applicationContext).next()
                override fun onSkipToPrevious() = AudioPlaybackManager.getInstance(applicationContext).previous()
                override fun onSeekTo(pos: Long) = AudioPlaybackManager.getInstance(applicationContext).seekTo(pos)
                override fun onStop() = AudioPlaybackManager.getInstance(applicationContext).pause()
            })
            isActive = true
        }
        ContextCompat.registerReceiver(
            this,
            noisyReceiver,
            IntentFilter(android.media.AudioManager.ACTION_AUDIO_BECOMING_NOISY),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(noisyReceiver) }
        if (::mediaSession.isInitialized) {
            mediaSession.isActive = false
            mediaSession.release()
        }
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        val playbackManager = AudioPlaybackManager.getInstance(applicationContext)

        when (action) {
            ACTION_PLAY -> playbackManager.resume()
            ACTION_PAUSE -> playbackManager.pause()
            ACTION_NEXT -> playbackManager.next()
            ACTION_PREV -> playbackManager.previous()
            ACTION_STOP -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_UPDATE -> {
                val title = intent.getStringExtra(EXTRA_TITLE) ?: "Dhun"
                val artist = intent.getStringExtra(EXTRA_ARTIST) ?: "Playing Music"
                val isPlaying = intent.getBooleanExtra(EXTRA_IS_PLAYING, false)
                val notification = buildNotification(title, artist, isPlaying)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ServiceCompat.startForeground(
                        this, NOTIFICATION_ID, notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                    )
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun buildNotification(title: String, artist: String, isPlaying: Boolean): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val prevIntent = Intent(this, DhunPlaybackService::class.java).apply { action = ACTION_PREV }
        val prevPendingIntent = PendingIntent.getService(
            this, 1, prevIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val playPauseIntent = Intent(this, DhunPlaybackService::class.java).apply {
            action = if (isPlaying) ACTION_PAUSE else ACTION_PLAY
        }
        val playPausePendingIntent = PendingIntent.getService(
            this, 2, playPauseIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val nextIntent = Intent(this, DhunPlaybackService::class.java).apply { action = ACTION_NEXT }
        val nextPendingIntent = PendingIntent.getService(
            this, 3, nextIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val playPauseIcon = if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
        updateMediaSession(title, artist, isPlaying)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(artist)
            .setSubText("Dhun")
            .setSmallIcon(R.drawable.ic_stat_dhun)
            .setContentIntent(contentPendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(isPlaying)
            .addAction(android.R.drawable.ic_media_previous, "Previous", prevPendingIntent)
            .addAction(playPauseIcon, if (isPlaying) "Pause" else "Play", playPausePendingIntent)
            .addAction(android.R.drawable.ic_media_next, "Next", nextPendingIntent)
            .setStyle(MediaStyle().setMediaSession(mediaSession.sessionToken).setShowActionsInCompactView(0, 1, 2))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Dhun Music Playback", NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Controls and information for currently playing music"
                setShowBadge(false)
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun updateMediaSession(title: String, artist: String, isPlaying: Boolean) {
        if (!::mediaSession.isInitialized) return
        val manager = AudioPlaybackManager.getInstance(applicationContext)
        val info = manager.playbackInfo.value
        mediaSession.setMetadata(
            MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, artist)
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, info.currentSong?.album ?: "Dhun")
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, info.durationMs)
                .build()
        )
        val actions = PlaybackStateCompat.ACTION_PLAY or PlaybackStateCompat.ACTION_PAUSE or
            PlaybackStateCompat.ACTION_SKIP_TO_NEXT or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
            PlaybackStateCompat.ACTION_SEEK_TO or PlaybackStateCompat.ACTION_STOP
        mediaSession.setPlaybackState(
            PlaybackStateCompat.Builder().setActions(actions).setState(
                if (isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED,
                info.currentPositionMs, 1f
            ).build()
        )
    }

    companion object {
        const val CHANNEL_ID = "dhun_playback_channel"
        const val NOTIFICATION_ID = 2001
        const val ACTION_PLAY = "com.dhun.music.ACTION_PLAY"
        const val ACTION_PAUSE = "com.dhun.music.ACTION_PAUSE"
        const val ACTION_NEXT = "com.dhun.music.ACTION_NEXT"
        const val ACTION_PREV = "com.dhun.music.ACTION_PREV"
        const val ACTION_STOP = "com.dhun.music.ACTION_STOP"
        const val ACTION_UPDATE = "com.dhun.music.ACTION_UPDATE"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_ARTIST = "extra_artist"
        const val EXTRA_IS_PLAYING = "extra_is_playing"

        fun startOrUpdateService(context: Context, song: Song, isPlaying: Boolean) {
            val intent = Intent(context, DhunPlaybackService::class.java).apply {
                action = ACTION_UPDATE
                putExtra(EXTRA_TITLE, song.title)
                putExtra(EXTRA_ARTIST, song.artist)
                putExtra(EXTRA_IS_PLAYING, isPlaying)
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent)
                else context.startService(intent)
            } catch (_: Exception) { }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, DhunPlaybackService::class.java).apply { action = ACTION_STOP }
            try { context.startService(intent) } catch (_: Exception) { }
        }
    }
}
