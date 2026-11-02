package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.model.Song
import com.example.playback.MusicService
import com.example.util.AlbumArtLoader
import java.io.File

class BeatWaveWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_WIDGET_PREV = "com.example.beatwave.WIDGET_PREV"
        const val ACTION_WIDGET_TOGGLE = "com.example.beatwave.WIDGET_TOGGLE"
        const val ACTION_WIDGET_NEXT = "com.example.beatwave.WIDGET_NEXT"

        private var cachedSong: Song? = null
        private var cachedIsPlaying: Boolean = false

        fun updateWidget(context: Context, song: Song?, isPlaying: Boolean) {
            cachedSong = song
            cachedIsPlaying = isPlaying

            val appWidgetManager = AppWidgetManager.getInstance(context) ?: return
            val componentName = ComponentName(context, BeatWaveWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            if (appWidgetIds.isEmpty()) return

            for (widgetId in appWidgetIds) {
                val views = buildRemoteViews(context, song, isPlaying)
                appWidgetManager.updateAppWidget(widgetId, views)
            }
        }

        private fun buildRemoteViews(context: Context, song: Song?, isPlaying: Boolean): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_beatwave)

            // Content Intent - Open App
            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val openAppPending = PendingIntent.getActivity(
                context,
                100,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, openAppPending)
            views.setOnClickPendingIntent(R.id.widget_info_container, openAppPending)

            // Previous Button Intent
            val prevIntent = Intent(context, BeatWaveWidgetProvider::class.java).apply {
                action = ACTION_WIDGET_PREV
            }
            val prevPending = PendingIntent.getBroadcast(
                context,
                101,
                prevIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_prev, prevPending)

            // Play/Pause Button Intent
            val toggleIntent = Intent(context, BeatWaveWidgetProvider::class.java).apply {
                action = ACTION_WIDGET_TOGGLE
            }
            val togglePending = PendingIntent.getBroadcast(
                context,
                102,
                toggleIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_play_pause, togglePending)

            // Next Button Intent
            val nextIntent = Intent(context, BeatWaveWidgetProvider::class.java).apply {
                action = ACTION_WIDGET_NEXT
            }
            val nextPending = PendingIntent.getBroadcast(
                context,
                103,
                nextIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_next, nextPending)

            // Update Metadata Texts
            if (song != null) {
                views.setTextViewText(R.id.widget_title, song.title)
                views.setTextViewText(R.id.widget_artist, "${song.artist} • ${song.formattedDuration}")
            } else {
                views.setTextViewText(R.id.widget_title, "BeatWave Player")
                views.setTextViewText(R.id.widget_artist, "Tap to play music")
            }

            // Update Play/Pause Icon
            val playPauseRes = if (isPlaying) {
                android.R.drawable.ic_media_pause
            } else {
                android.R.drawable.ic_media_play
            }
            views.setImageViewResource(R.id.widget_play_pause, playPauseRes)

            // Load Album Art thumbnail with AlbumArtLoader (memory-efficient sample size, 32-bit low-RAM safe)
            var artLoaded = false
            if (song != null) {
                try {
                    val bitmap = AlbumArtLoader.loadArtSync(context, song, AlbumArtLoader.SIZE_THUMBNAIL)
                    if (bitmap != null && !bitmap.isRecycled) {
                        views.setImageViewBitmap(R.id.widget_album_art, bitmap)
                        artLoaded = true
                    }
                } catch (_: Exception) {}
            }

            if (!artLoaded) {
                views.setImageViewResource(R.id.widget_album_art, android.R.drawable.ic_media_play)
            }

            return views
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val currentSnapshot = MusicService.playbackManager?.snapshot?.value
        val song = currentSnapshot?.currentSong ?: cachedSong
        val isPlaying = currentSnapshot?.isPlaying ?: cachedIsPlaying

        for (appWidgetId in appWidgetIds) {
            val views = buildRemoteViews(context, song, isPlaying)
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val manager = MusicService.playbackManager

        when (intent.action) {
            ACTION_WIDGET_PREV -> {
                manager?.skipPrevious()
            }
            ACTION_WIDGET_TOGGLE -> {
                manager?.togglePlayPause()
            }
            ACTION_WIDGET_NEXT -> {
                manager?.skipNext()
            }
        }
    }
}
