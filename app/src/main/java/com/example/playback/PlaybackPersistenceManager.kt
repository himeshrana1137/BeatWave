package com.example.playback

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.model.PlaybackSnapshot
import com.example.model.RepeatMode
import com.example.model.Song
import org.json.JSONArray
import java.io.File

data class SavedPlaybackState(
    val currentSongId: String?,
    val currentSongUri: String?,
    val queueSongIds: List<String>,
    val positionMs: Long,
    val durationMs: Long,
    val isShuffle: Boolean,
    val repeatMode: RepeatMode,
    val speed: Float,
    val pitch: Float
)

class PlaybackPersistenceManager(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("beatwave_last_played", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_CURRENT_SONG_ID = "key_current_song_id"
        private const val KEY_CURRENT_SONG_URI = "key_current_song_uri"
        private const val KEY_QUEUE_IDS = "key_queue_ids"
        private const val KEY_POSITION_MS = "key_position_ms"
        private const val KEY_DURATION_MS = "key_duration_ms"
        private const val KEY_IS_SHUFFLE = "key_is_shuffle"
        private const val KEY_REPEAT_MODE = "key_repeat_mode"
        private const val KEY_SPEED = "key_speed"
        private const val KEY_PITCH = "key_pitch"
    }

    fun savePlaybackState(snapshot: PlaybackSnapshot, activeQueue: List<Song>, currentPositionMs: Long) {
        val song = snapshot.currentSong ?: return
        try {
            val queueArray = JSONArray()
            activeQueue.forEach { queueArray.put(it.id) }

            val pos = if (currentPositionMs > 0) currentPositionMs else snapshot.currentPositionMs

            prefs.edit()
                .putString(KEY_CURRENT_SONG_ID, song.id)
                .putString(KEY_CURRENT_SONG_URI, song.uriString)
                .putString(KEY_QUEUE_IDS, queueArray.toString())
                .putLong(KEY_POSITION_MS, pos)
                .putLong(KEY_DURATION_MS, snapshot.durationMs.coerceAtLeast(song.durationMs))
                .putBoolean(KEY_IS_SHUFFLE, snapshot.isShuffle)
                .putString(KEY_REPEAT_MODE, snapshot.repeatMode.name)
                .putFloat(KEY_SPEED, snapshot.playbackSpeed)
                .putFloat(KEY_PITCH, snapshot.playbackPitch)
                .apply()
        } catch (e: Exception) {
            Log.e("PlaybackPersistence", "Error saving playback state: ${e.message}")
        }
    }

    fun updatePosition(positionMs: Long) {
        if (positionMs >= 0) {
            try {
                prefs.edit().putLong(KEY_POSITION_MS, positionMs).apply()
            } catch (_: Exception) {}
        }
    }

    fun getSavedPlaybackState(): SavedPlaybackState? {
        val songId = prefs.getString(KEY_CURRENT_SONG_ID, null) ?: return null
        val songUri = prefs.getString(KEY_CURRENT_SONG_URI, null)
        val queueJson = prefs.getString(KEY_QUEUE_IDS, "[]") ?: "[]"
        val queueIds = mutableListOf<String>()
        try {
            val arr = JSONArray(queueJson)
            for (i in 0 until arr.length()) {
                queueIds.add(arr.getString(i))
            }
        } catch (_: Exception) {}

        val positionMs = prefs.getLong(KEY_POSITION_MS, 0L)
        val durationMs = prefs.getLong(KEY_DURATION_MS, 0L)
        val isShuffle = prefs.getBoolean(KEY_IS_SHUFFLE, false)
        val repeatModeStr = prefs.getString(KEY_REPEAT_MODE, RepeatMode.OFF.name) ?: RepeatMode.OFF.name
        val repeatMode = try {
            RepeatMode.valueOf(repeatModeStr)
        } catch (_: Exception) {
            RepeatMode.OFF
        }
        val speed = prefs.getFloat(KEY_SPEED, 1.0f)
        val pitch = prefs.getFloat(KEY_PITCH, 1.0f)

        return SavedPlaybackState(
            currentSongId = songId,
            currentSongUri = songUri,
            queueSongIds = queueIds,
            positionMs = positionMs,
            durationMs = durationMs,
            isShuffle = isShuffle,
            repeatMode = repeatMode,
            speed = speed,
            pitch = pitch
        )
    }

    fun clear() {
        prefs.edit().clear().apply()
    }
}
