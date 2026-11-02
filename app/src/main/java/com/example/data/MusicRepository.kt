package com.example.data

import android.content.ContentUris
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.net.Uri
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import com.example.model.AppSettings
import com.example.model.DuplicateSongGroup
import com.example.model.ListeningStats
import com.example.model.Playlist
import com.example.model.Song
import com.example.model.SongDetailedInfo
import com.example.model.VideoItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.util.UUID
import kotlin.math.sin

class MusicRepository(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("harmoniq_music_storage", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_SONGS = "key_songs"
        private const val KEY_PLAYLISTS = "key_playlists"
        private const val KEY_VIDEOS = "key_videos"
        private const val KEY_THEME = "key_theme_mode"
        private const val KEY_VISUALIZER = "key_visualizer_enabled"
        private const val KEY_AUTO_PAUSE = "key_auto_pause_on_call"
        private const val KEY_HEADSET_DOUBLE_PRESS = "key_headset_double_press"
        private const val KEY_SHAKE_TO_SKIP = "key_shake_to_skip"
        private const val KEY_FILTER_SHORT_AUDIO = "key_filter_short_audio"
        private const val KEY_MIN_DURATION_SEC = "key_min_duration_sec"
        private const val KEY_BLACKLISTED_FOLDERS = "key_blacklisted_folders"
        private const val KEY_VOLUME_NORM = "key_volume_norm"
        private const val KEY_FADE_IN_OUT = "key_fade_in_out"
        private const val KEY_AUTO_LYRICS = "key_auto_lyrics"
        private const val KEY_BG_VIDEO_AUDIO = "key_bg_video_audio"
        private const val KEY_REDUCE_ANIM = "key_reduce_anim"
        private const val KEY_FETCH_ONLINE_COVER = "key_fetch_online_cover"
        private const val KEY_SHOW_QUALITY_BADGES = "key_show_quality_badges"
    }

    fun isVisualizerEnabled(): Boolean {
        return prefs.getBoolean(KEY_VISUALIZER, true)
    }

    fun setVisualizerEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_VISUALIZER, enabled).apply()
    }

    fun getAppSettings(): AppSettings {
        val blacklisted = prefs.getStringSet(KEY_BLACKLISTED_FOLDERS, emptySet()) ?: emptySet()
        return AppSettings(
            autoPauseOnCallAndUnplug = prefs.getBoolean(KEY_AUTO_PAUSE, true),
            headsetDoublePressEnabled = prefs.getBoolean(KEY_HEADSET_DOUBLE_PRESS, true),
            shakeToSkipEnabled = prefs.getBoolean(KEY_SHAKE_TO_SKIP, false),
            filterShortAudioEnabled = prefs.getBoolean(KEY_FILTER_SHORT_AUDIO, true),
            minAudioDurationSec = prefs.getInt(KEY_MIN_DURATION_SEC, 60),
            blacklistedFolders = blacklisted,
            volumeNormalization = prefs.getBoolean(KEY_VOLUME_NORM, false),
            fadeInOutEnabled = prefs.getBoolean(KEY_FADE_IN_OUT, true),
            autoFetchLyrics = prefs.getBoolean(KEY_AUTO_LYRICS, true),
            backgroundVideoAudioOnly = prefs.getBoolean(KEY_BG_VIDEO_AUDIO, false),
            reduceAnimations = prefs.getBoolean(KEY_REDUCE_ANIM, false),
            fetchOnlineCoverArt = prefs.getBoolean(KEY_FETCH_ONLINE_COVER, false),
            showQualityBadges = prefs.getBoolean(KEY_SHOW_QUALITY_BADGES, true)
        )
    }

    fun setShowQualityBadges(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_QUALITY_BADGES, enabled).apply()
    }

    fun setFetchOnlineCoverArt(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_FETCH_ONLINE_COVER, enabled).apply()
        com.example.util.AlbumArtLoader.setOnlineCoverFetchEnabled(enabled)
    }

    fun setVolumeNormalizationSetting(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_VOLUME_NORM, enabled).apply()
    }

    fun setFadeInOutEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_FADE_IN_OUT, enabled).apply()
    }

    fun setAutoFetchLyricsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_LYRICS, enabled).apply()
    }

    fun setBackgroundVideoAudioOnly(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BG_VIDEO_AUDIO, enabled).apply()
    }

    fun setReduceAnimations(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_REDUCE_ANIM, enabled).apply()
    }

    fun setAutoPauseOnCallAndUnplug(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_PAUSE, enabled).apply()
    }

    fun setHeadsetDoublePressEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_HEADSET_DOUBLE_PRESS, enabled).apply()
    }

    fun setShakeToSkipEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SHAKE_TO_SKIP, enabled).apply()
    }

    fun setFilterShortAudioEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_FILTER_SHORT_AUDIO, enabled).apply()
    }

    fun setMinAudioDurationSec(seconds: Int) {
        prefs.edit().putInt(KEY_MIN_DURATION_SEC, seconds).apply()
    }

    fun getBlacklistedFolders(): Set<String> {
        return prefs.getStringSet(KEY_BLACKLISTED_FOLDERS, emptySet()) ?: emptySet()
    }

    fun addBlacklistedFolder(folder: String) {
        val current = getBlacklistedFolders().toMutableSet()
        current.add(folder)
        prefs.edit().putStringSet(KEY_BLACKLISTED_FOLDERS, current).apply()
    }

    fun removeBlacklistedFolder(folder: String) {
        val current = getBlacklistedFolders().toMutableSet()
        current.remove(folder)
        prefs.edit().putStringSet(KEY_BLACKLISTED_FOLDERS, current).apply()
    }

    suspend fun findDuplicateSongs(): List<DuplicateSongGroup> = withContext(Dispatchers.IO) {
        val allSongs = loadAllRawSongs()
        val groups = allSongs.groupBy {
            val normTitle = it.title.lowercase().trim().replace(Regex("[^a-z0-9]"), "")
            val normArtist = it.artist.lowercase().trim().replace(Regex("[^a-z0-9]"), "")
            "$normTitle##$normArtist"
        }

        groups.filter { it.value.size > 1 }
            .map { (key, songs) ->
                DuplicateSongGroup(
                    key = key,
                    title = songs.first().title,
                    artist = songs.first().artist,
                    songs = songs.sortedByDescending { it.durationMs }
                )
            }
            .sortedBy { it.title.lowercase() }
    }

    suspend fun getSongDetailedInfo(song: Song): SongDetailedInfo = withContext(Dispatchers.IO) {
        var fileSize: Long = 0L
        var bitrate = "Unknown"
        var sampleRate = "Unknown"
        var format = "AUDIO"

        val uriStr = song.uriString
        val extension = uriStr.substringAfterLast('.', "").uppercase().takeIf { it.isNotBlank() && it.length <= 5 }
        if (extension != null) {
            format = extension
        }

        try {
            if (uriStr.startsWith("content://")) {
                val uri = Uri.parse(uriStr)
                context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                        if (sizeIdx >= 0) fileSize = cursor.getLong(sizeIdx)
                    }
                }
            } else {
                val file = File(if (uriStr.startsWith("file://")) Uri.parse(uriStr).path ?: "" else uriStr)
                if (file.exists()) {
                    fileSize = file.length()
                }
            }
        } catch (_: Exception) {}

        try {
            val retriever = MediaMetadataRetriever()
            if (uriStr.startsWith("content://")) {
                retriever.setDataSource(context, Uri.parse(uriStr))
            } else {
                retriever.setDataSource(uriStr)
            }
            val bStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
            if (!bStr.isNullOrBlank()) {
                val bInt = bStr.toLongOrNull()
                if (bInt != null && bInt > 0) {
                    bitrate = "${bInt / 1000} kbps"
                }
            }
            val mime = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
            if (!mime.isNullOrBlank()) {
                val sub = mime.substringAfterLast('/')
                if (format == "AUDIO") format = sub.uppercase()
            }
            val sRate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_SAMPLERATE)
            if (!sRate.isNullOrBlank()) {
                sampleRate = "${sRate.toIntOrNull()?.div(1000.0) ?: sRate} kHz"
            }
            retriever.release()
        } catch (_: Exception) {}

        if (bitrate == "Unknown" && fileSize > 0 && song.durationMs > 0) {
            val calcKbps = (fileSize * 8) / song.durationMs
            bitrate = "~$calcKbps kbps"
        }

        val sizeFormatted = when {
            fileSize >= 1024 * 1024 -> String.format(Locale.US, "%.2f MB", fileSize.toDouble() / (1024 * 1024))
            fileSize >= 1024 -> String.format(Locale.US, "%.1f KB", fileSize.toDouble() / 1024)
            fileSize > 0 -> "$fileSize bytes"
            else -> "Unknown"
        }

        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        val dateFormatted = dateFormat.format(Date(if (song.dateAdded > 0) song.dateAdded else System.currentTimeMillis()))

        val pathDisplay = if (song.folderPath.isNotBlank()) {
            "${song.folderPath}/${song.title}"
        } else {
            song.uriString
        }

        SongDetailedInfo(
            id = song.id,
            title = song.title,
            artist = song.artist,
            album = song.album,
            durationFormatted = song.formattedDuration,
            durationMs = song.durationMs,
            format = format,
            fileSizeFormatted = sizeFormatted,
            fileSizeBytes = fileSize,
            bitrateFormatted = bitrate,
            sampleRateFormatted = sampleRate,
            filePath = pathDisplay,
            dateAddedFormatted = dateFormatted
        )
    }

    suspend fun setSongLyrics(songId: String, lyricsUri: String?) = withContext(Dispatchers.IO) {
        val updated = loadSongs().map {
            if (it.id == songId) it.copy(lyricsUriString = lyricsUri) else it
        }
        saveSongs(updated)
    }

    suspend fun getListeningStats(): ListeningStats = withContext(Dispatchers.IO) {
        val songsList = loadSongs()
        val playlistsList = loadPlaylists()

        val totalPlays = songsList.sumOf { it.playCount }
        val totalTime = songsList.sumOf { it.playCount.toLong() * it.durationMs }
        val topSong = songsList.filter { it.playCount > 0 }.maxByOrNull { it.playCount }
        val topArtist = songsList.filter { it.playCount > 0 }
            .groupBy { it.artist }
            .maxByOrNull { entry -> entry.value.sumOf { it.playCount } }
            ?.key

        ListeningStats(
            totalTracksPlayed = totalPlays,
            totalListeningTimeMs = totalTime,
            mostPlayedSong = topSong,
            mostPlayedArtist = topArtist,
            totalSongs = songsList.size,
            totalPlaylists = playlistsList.size
        )
    }

    suspend fun exportBackupJson(): String = withContext(Dispatchers.IO) {
        val root = JSONObject()
        root.put("version", 1)
        root.put("exportTimestamp", System.currentTimeMillis())

        val songsArray = JSONArray()
        loadSongs().forEach { song ->
            val obj = JSONObject().apply {
                put("id", song.id)
                put("title", song.title)
                put("artist", song.artist)
                put("album", song.album)
                put("durationMs", song.durationMs)
                put("uriString", song.uriString)
                put("isFavorite", song.isFavorite)
                put("playCount", song.playCount)
                put("lastPlayedTime", song.lastPlayedTime)
                put("lyricsUriString", song.lyricsUriString ?: "")
            }
            songsArray.put(obj)
        }
        root.put("songs", songsArray)

        val playlistsArray = JSONArray()
        loadPlaylists().forEach { playlist ->
            val obj = JSONObject().apply {
                put("id", playlist.id)
                put("name", playlist.name)
                put("description", playlist.description)
                val sArray = JSONArray()
                playlist.songIds.forEach { sArray.put(it) }
                put("songIds", sArray)
            }
            playlistsArray.put(obj)
        }
        root.put("playlists", playlistsArray)

        root.toString(2)
    }

    suspend fun importBackupJson(jsonString: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(jsonString)
            if (root.has("songs")) {
                val songsArray = root.getJSONArray("songs")
                val importedSongs = mutableListOf<Song>()
                for (i in 0 until songsArray.length()) {
                    val obj = songsArray.getJSONObject(i)
                    importedSongs.add(
                        Song(
                            id = obj.getString("id"),
                            title = obj.getString("title"),
                            artist = obj.optString("artist", "Unknown Artist"),
                            album = obj.optString("album", "Unknown Album"),
                            durationMs = obj.optLong("durationMs", 0L),
                            uriString = obj.getString("uriString"),
                            isFavorite = obj.optBoolean("isFavorite", false),
                            playCount = obj.optInt("playCount", 0),
                            lastPlayedTime = obj.optLong("lastPlayedTime", 0L),
                            lyricsUriString = obj.optString("lyricsUriString", null).takeIf { !it.isNullOrBlank() }
                        )
                    )
                }
                saveSongs(importedSongs)
            }

            if (root.has("playlists")) {
                val playlistsArray = root.getJSONArray("playlists")
                val importedPlaylists = mutableListOf<Playlist>()
                for (i in 0 until playlistsArray.length()) {
                    val obj = playlistsArray.getJSONObject(i)
                    val sArray = obj.optJSONArray("songIds") ?: JSONArray()
                    val sIds = mutableListOf<String>()
                    for (j in 0 until sArray.length()) {
                        sIds.add(sArray.getString(j))
                    }
                    importedPlaylists.add(
                        Playlist(
                            id = obj.getString("id"),
                            name = obj.getString("name"),
                            description = obj.optString("description", ""),
                            songIds = sIds
                        )
                    )
                }
                savePlaylists(importedPlaylists)
            }
            true
        } catch (e: Exception) {
            Log.e("MusicRepository", "Failed to import backup JSON: ${e.message}")
            false
        }
    }

    suspend fun loadAllRawSongs(): List<Song> = withContext(Dispatchers.IO) {
        val jsonString = prefs.getString(KEY_SONGS, null)
        if (jsonString.isNullOrEmpty()) {
            val demoSongs = generateDemoTracksIfEmpty()
            saveSongs(demoSongs)
            return@withContext demoSongs
        }

        try {
            val jsonArray = JSONArray(jsonString)
            val songs = mutableListOf<Song>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                songs.add(
                    Song(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        artist = obj.optString("artist", "Unknown Artist"),
                        album = obj.optString("album", "Unknown Album"),
                        durationMs = obj.optLong("durationMs", 0L),
                        uriString = obj.getString("uriString"),
                        isFavorite = obj.optBoolean("isFavorite", false),
                        dateAdded = obj.optLong("dateAdded", System.currentTimeMillis()),
                        albumArtPath = obj.optString("albumArtPath", null).takeIf { !it.isNullOrBlank() },
                        isDemoTrack = obj.optBoolean("isDemoTrack", false),
                        playCount = obj.optInt("playCount", 0),
                        lastPlayedTime = obj.optLong("lastPlayedTime", 0L),
                        lyricsUriString = obj.optString("lyricsUriString", null).takeIf { !it.isNullOrBlank() },
                        folderName = obj.optString("folderName", "Music"),
                        folderPath = obj.optString("folderPath", ""),
                        moodTags = obj.optJSONArray("moodTags")?.let { arr ->
                            (0 until arr.length()).map { arr.getString(it) }
                        } ?: emptyList()
                    )
                )
            }
            songs
        } catch (e: Exception) {
            Log.e("MusicRepository", "Error parsing songs: ${e.message}")
            emptyList()
        }
    }

    suspend fun loadSongs(): List<Song> = withContext(Dispatchers.IO) {
        val all = loadAllRawSongs()
        val settings = getAppSettings()
        all.filter { song ->
            val durationOk = if (settings.filterShortAudioEnabled && !song.isDemoTrack) {
                song.durationMs >= (settings.minAudioDurationSec * 1000L)
            } else true

            val folderOk = settings.blacklistedFolders.none { blacklisted ->
                song.folderName.equals(blacklisted, ignoreCase = true) ||
                        (song.folderPath.isNotBlank() && song.folderPath.contains(blacklisted, ignoreCase = true))
            }

            durationOk && folderOk
        }
    }

    suspend fun saveSongs(songs: List<Song>) = withContext(Dispatchers.IO) {
        val jsonArray = JSONArray()
        songs.forEach { song ->
            val obj = JSONObject().apply {
                put("id", song.id)
                put("title", song.title)
                put("artist", song.artist)
                put("album", song.album)
                put("durationMs", song.durationMs)
                put("uriString", song.uriString)
                put("isFavorite", song.isFavorite)
                put("dateAdded", song.dateAdded)
                put("albumArtPath", song.albumArtPath ?: "")
                put("isDemoTrack", song.isDemoTrack)
                put("playCount", song.playCount)
                put("lastPlayedTime", song.lastPlayedTime)
                put("lyricsUriString", song.lyricsUriString ?: "")
                put("folderName", song.folderName)
                put("folderPath", song.folderPath)
                val mArr = JSONArray()
                song.moodTags.forEach { mArr.put(it) }
                put("moodTags", mArr)
            }
            jsonArray.put(obj)
        }
        prefs.edit().putString(KEY_SONGS, jsonArray.toString()).apply()
    }

    suspend fun setSongMoods(songId: String, moods: List<String>) = withContext(Dispatchers.IO) {
        val updated = loadSongs().map {
            if (it.id == songId) it.copy(moodTags = moods) else it
        }
        saveSongs(updated)
    }

    suspend fun recordSongPlayed(songId: String) = withContext(Dispatchers.IO) {
        val songs = loadSongs().map {
            if (it.id == songId) {
                it.copy(
                    playCount = it.playCount + 1,
                    lastPlayedTime = System.currentTimeMillis()
                )
            } else it
        }
        saveSongs(songs)
    }

    suspend fun loadPlaylists(): List<Playlist> = withContext(Dispatchers.IO) {
        val jsonString = prefs.getString(KEY_PLAYLISTS, null)
        if (jsonString.isNullOrEmpty()) {
            val defaults = listOf(
                Playlist(
                    id = "pl_favorites",
                    name = "Favorites",
                    description = "Songs you have starred",
                    songIds = emptyList()
                ),
                Playlist(
                    id = "pl_chill",
                    name = "Night Chill",
                    description = "Smooth ambient vibes",
                    songIds = emptyList()
                )
            )
            savePlaylists(defaults)
            return@withContext defaults
        }

        try {
            val jsonArray = JSONArray(jsonString)
            val playlists = mutableListOf<Playlist>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val songIdsArray = obj.optJSONArray("songIds") ?: JSONArray()
                val songIds = mutableListOf<String>()
                for (j in 0 until songIdsArray.length()) {
                    songIds.add(songIdsArray.getString(j))
                }
                playlists.add(
                    Playlist(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        description = obj.optString("description", ""),
                        songIds = songIds,
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }

            // Generate Mood-Based Smart Auto Playlists
            val allSongs = loadAllRawSongs()
            val moodChillSongs = allSongs.filter { it.moodTags.contains("Chill") || it.title.contains("chill", true) || it.album.contains("chill", true) }.map { it.id }
            val moodWorkoutSongs = allSongs.filter { it.moodTags.contains("Workout") || it.title.contains("workout", true) || it.title.contains("run", true) }.map { it.id }
            val moodPartySongs = allSongs.filter { it.moodTags.contains("Party") || it.title.contains("party", true) || it.title.contains("dance", true) }.map { it.id }
            val moodFocusSongs = allSongs.filter { it.moodTags.contains("Focus") || it.title.contains("focus", true) || it.title.contains("ambient", true) }.map { it.id }

            val moodPlaylists = listOf(
                Playlist("pl_mood_chill", "Chill Vibes 🍃", "Relaxing tracks for calm moments", moodChillSongs),
                Playlist("pl_mood_workout", "Workout Energy ⚡", "High-tempo beats to keep you moving", moodWorkoutSongs),
                Playlist("pl_mood_party", "Party Hits 🎉", "Upbeat bangers to set the mood", moodPartySongs),
                Playlist("pl_mood_focus", "Focus Flow 🧠", "Deep concentration and study sounds", moodFocusSongs)
            )

            val customPlaylists = playlists.filterNot { it.id.startsWith("pl_mood_") }
            moodPlaylists + customPlaylists
        } catch (e: Exception) {
            Log.e("MusicRepository", "Error parsing playlists: ${e.message}")
            emptyList()
        }
    }

    suspend fun savePlaylists(playlists: List<Playlist>) = withContext(Dispatchers.IO) {
        val jsonArray = JSONArray()
        playlists.forEach { playlist ->
            val obj = JSONObject().apply {
                put("id", playlist.id)
                put("name", playlist.name)
                put("description", playlist.description)
                put("createdAt", playlist.createdAt)
                val songIdsArray = JSONArray()
                playlist.songIds.forEach { songIdsArray.put(it) }
                put("songIds", songIdsArray)
            }
            jsonArray.put(obj)
        }
        prefs.edit().putString(KEY_PLAYLISTS, jsonArray.toString()).apply()
    }

    // Video Storage
    suspend fun loadVideos(): List<VideoItem> = withContext(Dispatchers.IO) {
        val jsonString = prefs.getString(KEY_VIDEOS, null)
        if (jsonString.isNullOrEmpty()) {
            val demoVideos = generateDemoVideoIfEmpty()
            saveVideos(demoVideos)
            return@withContext demoVideos
        }

        try {
            val jsonArray = JSONArray(jsonString)
            val videos = mutableListOf<VideoItem>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                videos.add(
                    VideoItem(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        durationMs = obj.optLong("durationMs", 0L),
                        uriString = obj.getString("uriString"),
                        lastPositionMs = obj.optLong("lastPositionMs", 0L),
                        thumbnailPath = obj.optString("thumbnailPath", null).takeIf { !it.isNullOrBlank() },
                        dateAdded = obj.optLong("dateAdded", System.currentTimeMillis()),
                        subtitleUriString = obj.optString("subtitleUriString", null).takeIf { !it.isNullOrBlank() },
                        isDemo = obj.optBoolean("isDemo", false)
                    )
                )
            }
            videos
        } catch (e: Exception) {
            Log.e("MusicRepository", "Error parsing videos: ${e.message}")
            emptyList()
        }
    }

    suspend fun saveVideos(videos: List<VideoItem>) = withContext(Dispatchers.IO) {
        val jsonArray = JSONArray()
        videos.forEach { video ->
            val obj = JSONObject().apply {
                put("id", video.id)
                put("title", video.title)
                put("durationMs", video.durationMs)
                put("uriString", video.uriString)
                put("lastPositionMs", video.lastPositionMs)
                put("thumbnailPath", video.thumbnailPath ?: "")
                put("dateAdded", video.dateAdded)
                put("subtitleUriString", video.subtitleUriString ?: "")
                put("isDemo", video.isDemo)
            }
            jsonArray.put(obj)
        }
        prefs.edit().putString(KEY_VIDEOS, jsonArray.toString()).apply()
    }

    suspend fun updateVideoProgress(videoId: String, positionMs: Long) = withContext(Dispatchers.IO) {
        val videos = loadVideos().map {
            if (it.id == videoId) it.copy(lastPositionMs = positionMs) else it
        }
        saveVideos(videos)
    }

    suspend fun setVideoSubtitle(videoId: String, subtitleUri: String?) = withContext(Dispatchers.IO) {
        val videos = loadVideos().map {
            if (it.id == videoId) it.copy(subtitleUriString = subtitleUri) else it
        }
        saveVideos(videos)
    }

    suspend fun parseVideosFromUris(uris: List<Uri>): List<VideoItem> = withContext(Dispatchers.IO) {
        val parsedVideos = mutableListOf<VideoItem>()
        val retriever = MediaMetadataRetriever()

        for (uri in uris) {
            try {
                var title: String? = null
                var durationMs = 0L
                var thumbPath: String? = null

                try {
                    retriever.setDataSource(context, uri)
                    title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                    val durStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    durationMs = durStr?.toLongOrNull() ?: 0L

                    val frame = retriever.getFrameAtTime(1000000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    if (frame != null) {
                        val thumbDir = File(context.filesDir, "video_thumbs")
                        if (!thumbDir.exists()) thumbDir.mkdirs()
                        val thumbFile = File(thumbDir, "thumb_${UUID.randomUUID()}.jpg")
                        FileOutputStream(thumbFile).use { fos ->
                            frame.compress(Bitmap.CompressFormat.JPEG, 85, fos)
                        }
                        thumbPath = thumbFile.absolutePath
                    }
                } catch (e: Exception) {
                    Log.w("MusicRepository", "Retriever error for video $uri: ${e.message}")
                }

                if (title.isNullOrBlank()) {
                    title = queryFileName(uri) ?: uri.lastPathSegment ?: "Video Clip"
                    val dotIdx = title.lastIndexOf('.')
                    if (dotIdx > 0) {
                        title = title.substring(0, dotIdx)
                    }
                }

                parsedVideos.add(
                    VideoItem(
                        id = UUID.randomUUID().toString(),
                        title = title,
                        durationMs = durationMs,
                        uriString = uri.toString(),
                        lastPositionMs = 0L,
                        thumbnailPath = thumbPath,
                        dateAdded = System.currentTimeMillis(),
                        isDemo = false
                    )
                )
            } catch (e: Exception) {
                Log.e("MusicRepository", "Error parsing video uri $uri: ${e.message}")
            }
        }

        try {
            retriever.release()
        } catch (_: Exception) {}

        parsedVideos
    }

    suspend fun parseSongsFromUris(uris: List<Uri>): List<Song> = withContext(Dispatchers.IO) {
        val parsedSongs = mutableListOf<Song>()
        val retriever = MediaMetadataRetriever()

        for (uri in uris) {
            try {
                var title: String? = null
                var artist: String? = null
                var album: String? = null
                var durationMs = 0L
                var artPath: String? = null

                try {
                    retriever.setDataSource(context, uri)
                    title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                    artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                    album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                    val durStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    durationMs = durStr?.toLongOrNull() ?: 0L

                    val artBytes = com.example.util.AlbumArtLoader.extractEmbeddedPictureBytes(context, uri)
                    if (artBytes != null && artBytes.isNotEmpty()) {
                        val artDir = File(context.filesDir, "album_art")
                        if (!artDir.exists()) artDir.mkdirs()
                        val artFile = File(artDir, "art_${UUID.randomUUID()}.jpg")
                        FileOutputStream(artFile).use { fos ->
                            fos.write(artBytes)
                        }
                        artPath = artFile.absolutePath
                    }
                } catch (e: Exception) {
                    Log.w("MusicRepository", "Retriever failed on $uri: ${e.message}")
                }

                if (title.isNullOrBlank()) {
                    title = queryFileName(uri) ?: uri.lastPathSegment ?: "Audio Track"
                    val dotIdx = title.lastIndexOf('.')
                    if (dotIdx > 0) {
                        title = title.substring(0, dotIdx)
                    }
                }

                val songId = UUID.randomUUID().toString()
                parsedSongs.add(
                    Song(
                        id = songId,
                        title = title,
                        artist = artist?.takeIf { it.isNotBlank() } ?: "Local Artist",
                        album = album?.takeIf { it.isNotBlank() } ?: "Local Audio",
                        durationMs = durationMs,
                        uriString = uri.toString(),
                        isFavorite = false,
                        dateAdded = System.currentTimeMillis(),
                        albumArtPath = artPath,
                        isDemoTrack = false,
                        playCount = 0,
                        lastPlayedTime = 0L
                    )
                )
            } catch (e: Exception) {
                Log.e("MusicRepository", "Error parsing URI $uri: ${e.message}")
            }
        }

        try {
            retriever.release()
        } catch (_: Exception) {}

        parsedSongs
    }

    private fun queryFileName(uri: Uri): String? {
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) {
                    return cursor.getString(nameIndex)
                }
            }
        } catch (_: Exception) {}
        return null
    }

    suspend fun scanMediaStoreAudio(): List<Song> = withContext(Dispatchers.IO) {
        val scanned = mutableListOf<Song>()
        val settings = getAppSettings()
        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.DATA
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} >= 1000"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        try {
            context.contentResolver.query(uri, projection, selection, null, sortOrder)?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
                val dataCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val title = cursor.getString(titleCol) ?: "Track $id"
                    val artist = cursor.getString(artistCol) ?: "Unknown Artist"
                    val album = cursor.getString(albumCol) ?: "Unknown Album"
                    val duration = cursor.getLong(durationCol)
                    val dateAdded = cursor.getLong(dateCol) * 1000L
                    val dataPath = if (dataCol >= 0) cursor.getString(dataCol) else null
                    val contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)

                    val file = dataPath?.let { File(it) }
                    val folderName = file?.parentFile?.name ?: "Music"
                    val folderPath = file?.parent ?: ""

                    val isShort = settings.filterShortAudioEnabled && duration < (settings.minAudioDurationSec * 1000L)
                    val isBlacklisted = settings.blacklistedFolders.any { b ->
                        folderName.equals(b, ignoreCase = true) || (folderPath.isNotEmpty() && folderPath.contains(b, ignoreCase = true))
                    }

                    if (!isShort && !isBlacklisted) {
                        val albumArtUri = "content://media/external/audio/media/$id/albumart"
                        scanned.add(
                            Song(
                                id = "media_$id",
                                title = title,
                                artist = if (artist == "<unknown>") "Unknown Artist" else artist,
                                album = if (album == "<unknown>") "Unknown Album" else album,
                                durationMs = duration,
                                uriString = contentUri.toString(),
                                dateAdded = if (dateAdded > 0) dateAdded else System.currentTimeMillis(),
                                albumArtPath = albumArtUri,
                                isDemoTrack = false,
                                folderName = folderName,
                                folderPath = folderPath
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("MusicRepository", "Error querying MediaStore audio: ${e.message}")
        }
        scanned
    }

    suspend fun scanMediaStoreVideos(): List<VideoItem> = withContext(Dispatchers.IO) {
        val scanned = mutableListOf<VideoItem>()
        val uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.TITLE,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.DATA
        )
        val selection = "${MediaStore.Video.Media.DURATION} >= 1000"
        val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"

        try {
            context.contentResolver.query(uri, projection, selection, null, sortOrder)?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
                val dataCol = cursor.getColumnIndex(MediaStore.Video.Media.DATA)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val title = cursor.getString(titleCol) ?: "Video $id"
                    val duration = cursor.getLong(durationCol)
                    val dateAdded = cursor.getLong(dateCol) * 1000L
                    val dataPath = if (dataCol >= 0) cursor.getString(dataCol) else null
                    val contentUri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)

                    scanned.add(
                        VideoItem(
                            id = "video_$id",
                            title = title,
                            durationMs = duration,
                            uriString = contentUri.toString(),
                            lastPositionMs = 0L,
                            thumbnailPath = dataPath,
                            dateAdded = if (dateAdded > 0) dateAdded else System.currentTimeMillis(),
                            isDemo = false
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("MusicRepository", "Error querying MediaStore videos: ${e.message}")
        }
        scanned
    }

    suspend fun syncDeviceMedia(): Pair<List<Song>, List<VideoItem>> = withContext(Dispatchers.IO) {
        val scannedSongs = scanMediaStoreAudio()
        val scannedVideos = scanMediaStoreVideos()

        val existingSongs = loadSongs()
        val existingVideos = loadVideos()

        val mergedSongs = if (scannedSongs.isNotEmpty()) {
            val existingMap = existingSongs.associateBy { it.uriString }
            val existingById = existingSongs.associateBy { it.id }

            val fromDevice = scannedSongs.map { scanned ->
                val match = existingMap[scanned.uriString] ?: existingById[scanned.id]
                if (match != null) {
                    scanned.copy(
                        isFavorite = match.isFavorite,
                        playCount = match.playCount,
                        lastPlayedTime = match.lastPlayedTime,
                        lyricsUriString = match.lyricsUriString
                    )
                } else {
                    scanned
                }
            }

            val deviceUriSet = fromDevice.map { it.uriString }.toSet()
            val extraSongs = existingSongs.filter { it.uriString !in deviceUriSet && !it.isDemoTrack }
            fromDevice + extraSongs
        } else {
            existingSongs
        }

        val mergedVideos = if (scannedVideos.isNotEmpty()) {
            val existingMap = existingVideos.associateBy { it.uriString }
            val fromDevice = scannedVideos.map { scanned ->
                val match = existingMap[scanned.uriString]
                if (match != null) {
                    scanned.copy(
                        lastPositionMs = match.lastPositionMs,
                        subtitleUriString = match.subtitleUriString
                    )
                } else {
                    scanned
                }
            }
            val deviceUriSet = fromDevice.map { it.uriString }.toSet()
            val extraVideos = existingVideos.filter { it.uriString !in deviceUriSet && !it.isDemo }
            fromDevice + extraVideos
        } else {
            existingVideos
        }

        saveSongs(mergedSongs)
        saveVideos(mergedVideos)

        Pair(mergedSongs, mergedVideos)
    }

    fun generateDemoTracksIfEmpty(): List<Song> {
        val demoDir = File(context.filesDir, "demo_audio")
        if (!demoDir.exists()) demoDir.mkdirs()

        val list = mutableListOf<Song>()
        val tracks = listOf(
            Triple("Neon Chillwave", "Harmoniq Studio", 14000L),
            Triple("Cyber Sunset", "Aura Beats", 18000L),
            Triple("Midnight Groove", "Synthwave Co.", 16000L),
            Triple("Deep Focus Lo-Fi", "Lofi Mind", 20000L)
        )

        tracks.forEachIndexed { index, (title, artist, durationMs) ->
            val file = File(demoDir, "demo_${index + 1}.wav")
            if (!file.exists() || file.length() < 1000) {
                createSynthWavFile(file, durationMs.toInt(), index)
            }
            list.add(
                Song(
                    id = "demo_$index",
                    title = title,
                    artist = artist,
                    album = "Harmoniq Essentials",
                    durationMs = durationMs,
                    uriString = Uri.fromFile(file).toString(),
                    isFavorite = (index == 0),
                    dateAdded = System.currentTimeMillis() - (index * 3600000L),
                    albumArtPath = null,
                    isDemoTrack = true,
                    playCount = 4 - index,
                    lastPlayedTime = System.currentTimeMillis() - (index * 1200000L)
                )
            )
        }
        return list
    }

    fun generateDemoVideoIfEmpty(): List<VideoItem> {
        val videoDir = File(context.filesDir, "demo_video")
        if (!videoDir.exists()) videoDir.mkdirs()

        val videoFile = File(videoDir, "demo_visualizer.mp4")
        val srtFile = File(videoDir, "demo_subtitles.srt")

        // Create demo subtitles
        if (!srtFile.exists()) {
            try {
                FileOutputStream(srtFile).use { fos ->
                    val srtContent = """
1
00:00:00,500 --> 00:00:02,500
BeatWave Video Player

2
00:00:02,800 --> 00:00:04,800
Built-in MP4/WebM video playback
                    """.trimIndent()
                    fos.write(srtContent.toByteArray())
                }
            } catch (_: Exception) {}
        }

        // Create demo MP4 video safely using MediaCodec & MediaMuxer
        if (!videoFile.exists() || videoFile.length() < 1000) {
            encodeDemoMp4(videoFile, 320, 240, 5)
        }

        val hasValidVideo = videoFile.exists() && videoFile.length() > 0
        return listOf(
            VideoItem(
                id = "demo_video_1",
                title = "BeatWave Visual Demo",
                durationMs = 5000L,
                uriString = if (hasValidVideo) Uri.fromFile(videoFile).toString() else "",
                lastPositionMs = 0L,
                thumbnailPath = null,
                dateAdded = System.currentTimeMillis(),
                subtitleUriString = if (srtFile.exists()) Uri.fromFile(srtFile).toString() else null,
                isDemo = true
            )
        )
    }

    private fun encodeDemoMp4(outputFile: File, width: Int, height: Int, durationSeconds: Int) {
        var encoder: MediaCodec? = null
        var muxer: MediaMuxer? = null
        var muxerStarted = false
        var sampleCount = 0

        try {
            val mimeType = MediaFormat.MIMETYPE_VIDEO_AVC
            val frameRate = 24
            val bitRate = 600000
            val numFrames = frameRate * durationSeconds

            val format = MediaFormat.createVideoFormat(mimeType, width, height).apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible)
                setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
                setInteger(MediaFormat.KEY_FRAME_RATE, frameRate)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
            }

            val codec = MediaCodec.createEncoderByType(mimeType)
            encoder = codec
            codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            codec.start()

            val mediaMuxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            muxer = mediaMuxer
            var videoTrackIndex = -1

            val bufferInfo = MediaCodec.BufferInfo()
            val yuvBuffer = ByteArray(width * height * 3 / 2)

            for (frameIndex in 0 until numFrames) {
                val inputBufferIndex = codec.dequeueInputBuffer(10000)
                if (inputBufferIndex >= 0) {
                    val inputBuffer = codec.getInputBuffer(inputBufferIndex)
                    if (inputBuffer != null) {
                        inputBuffer.clear()

                        // Procedural animated gradient color
                        val t = frameIndex.toFloat() / numFrames.toFloat()
                        val yVal = (128 + sin(t * Math.PI * 4.0) * 80).toInt().toByte()
                        val uVal = (128 + sin(t * Math.PI * 2.0) * 60).toInt().toByte()
                        val vVal = (128 + sin((t + 0.5) * Math.PI * 2.0) * 60).toInt().toByte()

                        val ySize = width * height
                        for (i in 0 until ySize) {
                            yuvBuffer[i] = yVal
                        }
                        val uvSize = ySize / 4
                        for (i in 0 until uvSize) {
                            yuvBuffer[ySize + i] = uVal
                            yuvBuffer[ySize + uvSize + i] = vVal
                        }

                        inputBuffer.put(yuvBuffer)
                        val pts = frameIndex * 1000000L / frameRate
                        codec.queueInputBuffer(inputBufferIndex, 0, yuvBuffer.size, pts, 0)
                    }
                }

                // Poll output buffers
                var outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, 10000)
                while (outputBufferIndex != MediaCodec.INFO_TRY_AGAIN_LATER) {
                    if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        if (!muxerStarted) {
                            videoTrackIndex = mediaMuxer.addTrack(codec.outputFormat)
                            mediaMuxer.start()
                            muxerStarted = true
                        }
                    } else if (outputBufferIndex >= 0) {
                        val outputBuffer = codec.getOutputBuffer(outputBufferIndex)
                        if (outputBuffer != null) {
                            // If this buffer has CODEC_CONFIG, ignore size so we don't write duplicate CSD
                            if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                                bufferInfo.size = 0
                            }
                            if (bufferInfo.size > 0 && muxerStarted && videoTrackIndex >= 0) {
                                outputBuffer.position(bufferInfo.offset)
                                outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                                mediaMuxer.writeSampleData(videoTrackIndex, outputBuffer, bufferInfo)
                                sampleCount++
                            }
                        }
                        codec.releaseOutputBuffer(outputBufferIndex, false)
                    }
                    outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, 0)
                }
            }

            // Signal End of Stream
            val eosIndex = codec.dequeueInputBuffer(10000)
            if (eosIndex >= 0) {
                codec.queueInputBuffer(
                    eosIndex,
                    0,
                    0,
                    numFrames * 1000000L / frameRate,
                    MediaCodec.BUFFER_FLAG_END_OF_STREAM
                )
            }

            // Drain remaining output buffers
            var eosReached = false
            var drainAttempts = 0
            while (!eosReached && drainAttempts < 30) {
                drainAttempts++
                val outIndex = codec.dequeueOutputBuffer(bufferInfo, 10000)
                if (outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    if (!muxerStarted) {
                        videoTrackIndex = mediaMuxer.addTrack(codec.outputFormat)
                        mediaMuxer.start()
                        muxerStarted = true
                    }
                } else if (outIndex >= 0) {
                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                        bufferInfo.size = 0
                    }
                    val outBuf = codec.getOutputBuffer(outIndex)
                    if (outBuf != null && bufferInfo.size > 0 && muxerStarted && videoTrackIndex >= 0) {
                        outBuf.position(bufferInfo.offset)
                        outBuf.limit(bufferInfo.offset + bufferInfo.size)
                        mediaMuxer.writeSampleData(videoTrackIndex, outBuf, bufferInfo)
                        sampleCount++
                    }
                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        eosReached = true
                    }
                    codec.releaseOutputBuffer(outIndex, false)
                } else if (outIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    break
                }
            }
        } catch (e: Exception) {
            Log.w("MusicRepository", "Could not encode demo MP4: ${e.message}")
        } finally {
            try {
                encoder?.stop()
            } catch (_: Exception) {}
            try {
                encoder?.release()
            } catch (_: Exception) {}

            if (muxerStarted && sampleCount > 0) {
                try {
                    muxer?.stop()
                } catch (e: Exception) {
                    Log.w("MusicRepository", "Muxer stop error: ${e.message}")
                }
            }
            try {
                muxer?.release()
            } catch (_: Exception) {}

            if (sampleCount == 0 || outputFile.length() == 0L) {
                try {
                    outputFile.delete()
                } catch (_: Exception) {}
            }
        }
    }

    private fun createSynthWavFile(file: File, durationMs: Int, styleIndex: Int) {
        val sampleRate = 44100
        val numSamples = (durationMs / 1000.0 * sampleRate).toInt()
        val numChannels = 2
        val bytesPerSample = 2

        try {
            val byteStream = ByteArrayOutputStream()
            val chordProgressions = when (styleIndex % 4) {
                0 -> listOf(
                    doubleArrayOf(261.63, 329.63, 392.00),
                    doubleArrayOf(220.00, 261.63, 329.63),
                    doubleArrayOf(174.61, 220.00, 261.63),
                    doubleArrayOf(196.00, 246.94, 293.66)
                )
                1 -> listOf(
                    doubleArrayOf(293.66, 369.99, 440.00),
                    doubleArrayOf(246.94, 311.13, 369.99),
                    doubleArrayOf(196.00, 246.94, 293.66),
                    doubleArrayOf(220.00, 277.18, 329.63)
                )
                2 -> listOf(
                    doubleArrayOf(220.00, 261.63, 329.63, 392.00),
                    doubleArrayOf(293.66, 349.23, 440.00, 523.25),
                    doubleArrayOf(196.00, 246.94, 293.66, 349.23),
                    doubleArrayOf(261.63, 329.63, 392.00, 493.88)
                )
                else -> listOf(
                    doubleArrayOf(164.81, 196.00, 246.94),
                    doubleArrayOf(174.61, 220.00, 261.63),
                    doubleArrayOf(196.00, 246.94, 293.66),
                    doubleArrayOf(220.00, 261.63, 329.63)
                )
            }

            val samplesPerChord = (sampleRate * 2.0).toInt()

            for (i in 0 until numSamples) {
                val chordIdx = (i / samplesPerChord) % chordProgressions.size
                val chord = chordProgressions[chordIdx]
                val t = i.toDouble() / sampleRate

                var sampleVal = 0.0
                for (freq in chord) {
                    sampleVal += sin(2.0 * Math.PI * freq * t) * 0.2
                }

                val bassFreq = chord[0] / 2.0
                sampleVal += sin(2.0 * Math.PI * bassFreq * t) * 0.25

                val beatInterval = (sampleRate * 0.5).toInt()
                val beatSample = i % beatInterval
                if (beatSample < sampleRate * 0.08) {
                    val kickProgress = beatSample / (sampleRate * 0.08)
                    val kickFreq = 120.0 * (1.0 - kickProgress) + 40.0
                    sampleVal += sin(2.0 * Math.PI * kickFreq * (beatSample.toDouble() / sampleRate)) * (1.0 - kickProgress) * 0.4
                }

                val envelope = when {
                    i < sampleRate * 0.2 -> i / (sampleRate * 0.2)
                    i > numSamples - sampleRate * 0.5 -> (numSamples - i) / (sampleRate * 0.5)
                    else -> 1.0
                }

                val finalSample = (sampleVal * envelope).coerceIn(-1.0, 1.0)
                val intSample = (finalSample * 32767.0).toInt().toShort()

                byteStream.write(intSample.toInt() and 0xFF)
                byteStream.write((intSample.toInt() shr 8) and 0xFF)
                byteStream.write(intSample.toInt() and 0xFF)
                byteStream.write((intSample.toInt() shr 8) and 0xFF)
            }

            val pcmData = byteStream.toByteArray()
            writeWavHeader(file, pcmData.size, sampleRate, numChannels, bytesPerSample * 8)
            FileOutputStream(file, true).use { fos ->
                fos.write(pcmData)
            }
        } catch (e: Exception) {
            Log.e("MusicRepository", "Failed to synthesize demo WAV: ${e.message}")
        }
    }

    private fun writeWavHeader(
        file: File,
        dataLength: Int,
        sampleRate: Int,
        channels: Int,
        bitsPerSample: Int
    ) {
        val totalDataLen = dataLength + 36
        val byteRate = sampleRate * channels * (bitsPerSample / 8)

        FileOutputStream(file).use { out ->
            val header = ByteArray(44)
            header[0] = 'R'.code.toByte(); header[1] = 'I'.code.toByte(); header[2] = 'F'.code.toByte(); header[3] = 'F'.code.toByte()
            header[4] = (totalDataLen and 0xff).toByte()
            header[5] = ((totalDataLen shr 8) and 0xff).toByte()
            header[6] = ((totalDataLen shr 16) and 0xff).toByte()
            header[7] = ((totalDataLen shr 24) and 0xff).toByte()
            header[8] = 'W'.code.toByte(); header[9] = 'A'.code.toByte(); header[10] = 'V'.code.toByte(); header[11] = 'E'.code.toByte()

            header[12] = 'f'.code.toByte(); header[13] = 'm'.code.toByte(); header[14] = 't'.code.toByte(); header[15] = ' '.code.toByte()
            header[16] = 16; header[17] = 0; header[18] = 0; header[19] = 0
            header[20] = 1; header[21] = 0
            header[22] = channels.toByte(); header[23] = 0
            header[24] = (sampleRate and 0xff).toByte()
            header[25] = ((sampleRate shr 8) and 0xff).toByte()
            header[26] = ((sampleRate shr 16) and 0xff).toByte()
            header[27] = ((sampleRate shr 24) and 0xff).toByte()
            header[28] = (byteRate and 0xff).toByte()
            header[29] = ((byteRate shr 8) and 0xff).toByte()
            header[30] = ((byteRate shr 16) and 0xff).toByte()
            header[31] = ((byteRate shr 24) and 0xff).toByte()
            header[32] = (channels * (bitsPerSample / 8)).toByte()
            header[33] = 0
            header[34] = bitsPerSample.toByte()
            header[35] = 0

            header[36] = 'd'.code.toByte(); header[37] = 'a'.code.toByte(); header[38] = 't'.code.toByte(); header[39] = 'a'.code.toByte()
            header[40] = (dataLength and 0xff).toByte()
            header[41] = ((dataLength shr 8) and 0xff).toByte()
            header[42] = ((dataLength shr 16) and 0xff).toByte()
            header[43] = ((dataLength shr 24) and 0xff).toByte()

            out.write(header, 0, 44)
        }
    }
}
