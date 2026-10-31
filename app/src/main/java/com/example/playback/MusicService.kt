package com.example.playback

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.BitmapFactory
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.KeyEvent
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.example.MainActivity
import com.example.R
import com.example.model.PlaybackSnapshot
import java.io.File

class MusicService : Service() {

    private var mediaSession: MediaSession? = null
    private var lastHeadsetClickTime = 0L
    private val mediaButtonHandler = Handler(Looper.getMainLooper())
    private val singleClickRunnable = Runnable {
        playbackManager?.togglePlayPause()
    }

    companion object {
        const val CHANNEL_ID = "beatwave_music_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_PLAY = "com.example.harmoniq.ACTION_PLAY"
        const val ACTION_PAUSE = "com.example.harmoniq.ACTION_PAUSE"
        const val ACTION_TOGGLE = "com.example.harmoniq.ACTION_TOGGLE"
        const val ACTION_NEXT = "com.example.harmoniq.ACTION_NEXT"
        const val ACTION_PREV = "com.example.harmoniq.ACTION_PREV"
        const val ACTION_STOP = "com.example.harmoniq.ACTION_STOP"

        var instance: MusicService? = null
        var playbackManager: MusicPlaybackManager? = null
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannel()
        initMediaSession()

        // Call startForeground immediately to fulfill startForegroundService contract
        val initialSnapshot = playbackManager?.snapshot?.value ?: PlaybackSnapshot()
        startInForeground(buildNotification(initialSnapshot))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val currentSnapshot = playbackManager?.snapshot?.value ?: PlaybackSnapshot()
        startInForeground(buildNotification(currentSnapshot))

        when (intent?.action) {
            ACTION_PLAY -> playbackManager?.play()
            ACTION_PAUSE -> playbackManager?.pause()
            ACTION_TOGGLE -> playbackManager?.togglePlayPause()
            ACTION_NEXT -> playbackManager?.skipNext()
            ACTION_PREV -> playbackManager?.skipPrevious()
            ACTION_STOP -> {
                playbackManager?.pause()
                try {
                    ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
                } catch (_: Exception) {}
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun startInForeground(notification: Notification) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    ServiceCompat.startForeground(
                        this,
                        NOTIFICATION_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                    )
                } catch (typeEx: Exception) {
                    Log.w("MusicService", "MediaPlayback type startForeground failed, falling back: ${typeEx.message}")
                    startForeground(NOTIFICATION_ID, notification)
                }
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            Log.e("MusicService", "Critical error calling startForeground: ${e.message}", e)
        }
    }

    private fun initMediaSession() {
        mediaSession = MediaSession(this, "BeatWaveMediaSession").apply {
            setCallback(object : MediaSession.Callback() {
                override fun onMediaButtonEvent(mediaButtonIntent: Intent): Boolean {
                    val keyEvent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        mediaButtonIntent.getParcelableExtra(Intent.EXTRA_KEY_EVENT, KeyEvent::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        mediaButtonIntent.getParcelableExtra(Intent.EXTRA_KEY_EVENT)
                    }

                    if (keyEvent != null && keyEvent.action == KeyEvent.ACTION_DOWN) {
                        val keyCode = keyEvent.keyCode
                        val isHook = keyCode == KeyEvent.KEYCODE_HEADSETHOOK ||
                                keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE

                        val settings = playbackManager?.repository?.getAppSettings()
                        val doublePressEnabled = settings?.headsetDoublePressEnabled ?: true

                        if (isHook && doublePressEnabled) {
                            val now = System.currentTimeMillis()
                            if (now - lastHeadsetClickTime < 450L) {
                                // Double press detected: skip to next track!
                                mediaButtonHandler.removeCallbacks(singleClickRunnable)
                                lastHeadsetClickTime = 0L
                                playbackManager?.skipNext()
                            } else {
                                lastHeadsetClickTime = now
                                mediaButtonHandler.removeCallbacks(singleClickRunnable)
                                mediaButtonHandler.postDelayed(singleClickRunnable, 450L)
                            }
                            return true
                        } else if (keyCode == KeyEvent.KEYCODE_MEDIA_NEXT) {
                            playbackManager?.skipNext()
                            return true
                        } else if (keyCode == KeyEvent.KEYCODE_MEDIA_PREVIOUS) {
                            playbackManager?.skipPrevious()
                            return true
                        } else if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY) {
                            playbackManager?.play()
                            return true
                        } else if (keyCode == KeyEvent.KEYCODE_MEDIA_PAUSE) {
                            playbackManager?.pause()
                            return true
                        }
                    }
                    return super.onMediaButtonEvent(mediaButtonIntent)
                }

                override fun onPlay() {
                    playbackManager?.play()
                }

                override fun onPause() {
                    playbackManager?.pause()
                }

                override fun onSkipToNext() {
                    playbackManager?.skipNext()
                }

                override fun onSkipToPrevious() {
                    playbackManager?.skipPrevious()
                }

                override fun onSeekTo(pos: Long) {
                    playbackManager?.seekTo(pos)
                }

                override fun onStop() {
                    playbackManager?.pause()
                }
            })
            isActive = true
        }
    }

    fun updateState(snapshot: PlaybackSnapshot) {
        val song = snapshot.currentSong

        // Update MediaSession PlaybackState
        val stateBuilder = PlaybackState.Builder()
            .setActions(
                PlaybackState.ACTION_PLAY or
                        PlaybackState.ACTION_PAUSE or
                        PlaybackState.ACTION_PLAY_PAUSE or
                        PlaybackState.ACTION_SKIP_TO_NEXT or
                        PlaybackState.ACTION_SKIP_TO_PREVIOUS or
                        PlaybackState.ACTION_SEEK_TO
            )
            .setState(
                if (snapshot.isPlaying) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED,
                snapshot.currentPositionMs,
                1.0f
            )
        mediaSession?.setPlaybackState(stateBuilder.build())

        // Update MediaMetadata
        if (song != null) {
            val metaBuilder = MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_TITLE, song.title)
                .putString(MediaMetadata.METADATA_KEY_ARTIST, song.artist)
                .putString(MediaMetadata.METADATA_KEY_ALBUM, song.album)
                .putLong(MediaMetadata.METADATA_KEY_DURATION, snapshot.durationMs)

            song.albumArtPath?.let { path ->
                try {
                    val file = File(path)
                    if (file.exists()) {
                        val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                        metaBuilder.putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, bitmap)
                    }
                } catch (_: Exception) {}
            }
            mediaSession?.setMetadata(metaBuilder.build())
        }

        // Build notification
        val notification = buildNotification(snapshot)
        if (snapshot.isPlaying) {
            startInForeground(notification)
        } else {
            try {
                ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_DETACH)
                val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                manager?.notify(NOTIFICATION_ID, notification)
            } catch (e: Exception) {
                Log.w("MusicService", "Failed to update notification when paused: ${e.message}")
            }
        }
    }

    private fun buildNotification(snapshot: PlaybackSnapshot): Notification {
        val song = snapshot.currentSong
        val isPlaying = snapshot.isPlaying

        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val prevPending = PendingIntent.getService(
            this,
            1,
            Intent(this, MusicService::class.java).apply { action = ACTION_PREV },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val togglePending = PendingIntent.getService(
            this,
            2,
            Intent(this, MusicService::class.java).apply { action = ACTION_TOGGLE },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val nextPending = PendingIntent.getService(
            this,
            3,
            Intent(this, MusicService::class.java).apply { action = ACTION_NEXT },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(song?.title ?: "BeatWave Player")
            .setContentText(song?.artist ?: "Local music playback")
            .setSubText(song?.album)
            .setContentIntent(contentIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(isPlaying)
            .addAction(android.R.drawable.ic_media_previous, "Previous", prevPending)
            .addAction(
                if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
                if (isPlaying) "Pause" else "Play",
                togglePending
            )
            .addAction(android.R.drawable.ic_media_next, "Next", nextPending)

        // Large icon if album art exists
        song?.albumArtPath?.let { path ->
            try {
                val file = File(path)
                if (file.exists()) {
                    val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                    builder.setLargeIcon(bitmap)
                }
            } catch (_: Exception) {}
        }

        return builder.build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Harmoniq Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Foreground playback controls and lock screen media status"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onTaskRemoved(rootIntent: Intent?) {
        val isPlaying = playbackManager?.snapshot?.value?.isPlaying ?: false
        if (!isPlaying) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        mediaSession?.release()
        mediaSession = null
        instance = null
        super.onDestroy()
    }
}
