package com.example.data

import android.content.ContentUris
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import com.example.data.db.AppDatabase
import com.example.data.db.PlaybackRecordEntity
import com.example.data.db.PlaylistEntity
import com.example.data.db.PlaylistSongEntity
import com.example.data.db.SongStatsEntity
import com.example.model.ListeningStatsOverview
import com.example.model.Playlist
import com.example.model.Song
import com.example.model.SongRankingItem
import com.example.model.StatsFilterPeriod
import com.example.model.StatsRankingMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Calendar

class MusicRepository(private val context: Context) {

    private val db = AppDatabase.getInstance(context)
    private val playlistDao = db.playlistDao()
    private val statsDao = db.statsDao()

    private val prefs: SharedPreferences =
        context.getSharedPreferences("beatwave_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_MIGRATED_OLD_STORAGE = "key_migrated_old_storage_v1"
    }

    init {
        cleanupLegacyStorageOnce()
    }

    /**
     * One-time cleanup of legacy SharedPreferences / JSON storage from previous versions.
     * Guarantees zero pre-made or auto-created playlists exist on fresh install or upgrade.
     */
    private fun cleanupLegacyStorageOnce() {
        val alreadyCleaned = prefs.getBoolean(KEY_MIGRATED_OLD_STORAGE, false)
        if (!alreadyCleaned) {
            try {
                // Clear any legacy storage files that contained pre-made playlists
                val oldStoragePrefs = context.getSharedPreferences("harmoniq_music_storage", Context.MODE_PRIVATE)
                oldStoragePrefs.edit().clear().apply()

                val oldVideoPrefs = context.getSharedPreferences("beatwave_video_prefs", Context.MODE_PRIVATE)
                oldVideoPrefs.edit().clear().apply()

                val oldLastPlayed = context.getSharedPreferences("beatwave_last_played", Context.MODE_PRIVATE)
                oldLastPlayed.edit().clear().apply()
            } catch (_: Exception) {}

            prefs.edit().putBoolean(KEY_MIGRATED_OLD_STORAGE, true).apply()
        }
    }

    /**
     * Scans MediaStore audio files from the device.
     */
    suspend fun scanMediaStoreAudio(): List<Song> = withContext(Dispatchers.IO) {
        val scanned = mutableListOf<Song>()
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
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        val statsMap = try {
            statsDao.getAllSongStatsSync().associateBy { it.songId }
        } catch (_: Exception) {
            emptyMap()
        }
        val statsByPath = try {
            statsDao.getAllSongStatsSync().associateBy { it.songPath }
        } catch (_: Exception) {
            emptyMap()
        }

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
                    val dataPath = if (dataCol >= 0) cursor.getString(dataCol) ?: "" else ""
                    val contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)

                    val songId = id.toString()
                    val stat = statsMap[songId] ?: statsByPath[dataPath]
                    val playCount = stat?.playCount ?: 0
                    val totalTime = stat?.totalTimeListenedMs ?: 0L
                    val lastPlayed = stat?.lastPlayedTimestamp ?: 0L

                    val albumArtUri = "content://media/external/audio/media/$id/albumart"

                    scanned.add(
                        Song(
                            id = songId,
                            title = title,
                            artist = if (artist == "<unknown>") "Unknown Artist" else artist,
                            album = if (album == "<unknown>") "Unknown Album" else album,
                            durationMs = duration,
                            uriString = contentUri.toString(),
                            dateAdded = if (dateAdded > 0) dateAdded else System.currentTimeMillis(),
                            albumArtPath = albumArtUri,
                            playCount = playCount,
                            totalTimeListenedMs = totalTime,
                            lastPlayedTime = lastPlayed,
                            filePath = dataPath
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("MusicRepository", "Error querying MediaStore audio: ${e.message}")
        }

        scanned
    }

    // ==========================================
    // PLAYLISTS (Room-Backed, Zero Defaults)
    // ==========================================

    /**
     * Observes all playlists live as Flow.
     */
    fun getPlaylistsFlow(): Flow<List<PlaylistEntity>> {
        return playlistDao.getAllPlaylistsFlow()
    }

    /**
     * Observes the songs of a specific playlist, resolved against current available songs.
     * Songs whose file no longer exists are hidden, but their entry remains in the database.
     */
    fun getPlaylistSongsFlow(playlistId: Long, allSongsFlow: Flow<List<Song>>): Flow<List<Song>> {
        return playlistDao.getSongsForPlaylistFlow(playlistId).combine(allSongsFlow) { entities, allSongs ->
            val songMapById = allSongs.associateBy { it.id }
            val songMapByPath = allSongs.filter { it.filePath.isNotBlank() }.associateBy { it.filePath }

            entities.mapNotNull { entity ->
                // Match by MediaStore ID first, fall back to file path
                val song = songMapById[entity.songId] ?: songMapByPath[entity.songPath]
                // Only include if song file still exists
                if (song != null && (song.filePath.isBlank() || File(song.filePath).exists())) {
                    song
                } else null
            }
        }
    }

    suspend fun createPlaylist(name: String): Long = withContext(Dispatchers.IO) {
        val trimmed = name.trim().ifBlank { "New Playlist" }
        playlistDao.insertPlaylist(
            PlaylistEntity(
                name = trimmed,
                createdAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun renamePlaylist(playlistId: Long, newName: String) = withContext(Dispatchers.IO) {
        val existing = playlistDao.getPlaylistById(playlistId) ?: return@withContext
        playlistDao.updatePlaylist(existing.copy(name = newName.trim().ifBlank { existing.name }))
    }

    suspend fun deletePlaylist(playlistId: Long) = withContext(Dispatchers.IO) {
        playlistDao.deletePlaylistById(playlistId)
    }

    /**
     * Adds songs to a playlist, skipping songs that are already in the playlist.
     * Returns the count of newly added songs.
     */
    suspend fun addSongsToPlaylist(playlistId: Long, songs: List<Song>): Int = withContext(Dispatchers.IO) {
        if (songs.isEmpty()) return@withContext 0

        val existingEntries = playlistDao.getSongsForPlaylistSync(playlistId)
        val existingIds = existingEntries.map { it.songId }.toSet()
        val existingPaths = existingEntries.map { it.songPath }.filter { it.isNotBlank() }.toSet()

        val newSongs = songs.filterNot { song ->
            existingIds.contains(song.id) || (song.filePath.isNotBlank() && existingPaths.contains(song.filePath))
        }

        if (newSongs.isEmpty()) return@withContext 0

        var currentMaxPos = playlistDao.getMaxPosition(playlistId) ?: -1
        val now = System.currentTimeMillis()

        val entitiesToInsert = newSongs.mapIndexed { index, song ->
            currentMaxPos++
            PlaylistSongEntity(
                playlistId = playlistId,
                songId = song.id,
                songPath = song.filePath,
                position = currentMaxPos,
                addedAt = now + index
            )
        }

        playlistDao.insertPlaylistSongs(entitiesToInsert)
        newSongs.size
    }

    suspend fun removeSongsFromPlaylist(playlistId: Long, songsToRemove: List<Song>) = withContext(Dispatchers.IO) {
        val ids = songsToRemove.map { it.id }
        val paths = songsToRemove.map { it.filePath }.filter { it.isNotBlank() }

        if (ids.isNotEmpty()) {
            playlistDao.removeSongsFromPlaylistByIds(playlistId, ids)
        }
        if (paths.isNotEmpty()) {
            playlistDao.removeSongsFromPlaylistByPaths(playlistId, paths)
        }
    }

    // ==========================================
    // LISTENING STATISTICS (Room-Backed)
    // ==========================================

    suspend fun recordPlaybackSession(
        song: Song,
        timeListenedMs: Long,
        isCountedPlay: Boolean
    ) = withContext(Dispatchers.IO) {
        if (timeListenedMs <= 0 && !isCountedPlay) return@withContext

        val now = System.currentTimeMillis()

        // 1. Log playback record for time-window filtering
        statsDao.insertPlaybackRecord(
            PlaybackRecordEntity(
                songId = song.id,
                songPath = song.filePath,
                timestamp = now,
                timeListenedMs = timeListenedMs,
                isCountedPlay = isCountedPlay
            )
        )

        // 2. Update aggregate song stats
        val currentStat = statsDao.getSongStats(song.id, song.filePath)
        val newPlayCount = (currentStat?.playCount ?: 0) + (if (isCountedPlay) 1 else 0)
        val newTotalTime = (currentStat?.totalTimeListenedMs ?: 0L) + timeListenedMs

        statsDao.insertOrUpdateSongStats(
            SongStatsEntity(
                songId = song.id,
                songPath = song.filePath,
                playCount = newPlayCount,
                totalTimeListenedMs = newTotalTime,
                lastPlayedTimestamp = now
            )
        )
    }

    private fun getPeriodStartTimestamp(period: StatsFilterPeriod): Long {
        val cal = Calendar.getInstance()
        return when (period) {
            StatsFilterPeriod.TODAY -> {
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                cal.timeInMillis
            }
            StatsFilterPeriod.THIS_WEEK -> {
                cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                cal.timeInMillis
            }
            StatsFilterPeriod.THIS_MONTH -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                cal.timeInMillis
            }
            StatsFilterPeriod.ALL_TIME -> 0L
        }
    }

    suspend fun getStatsOverview(period: StatsFilterPeriod): ListeningStatsOverview = withContext(Dispatchers.IO) {
        val since = getPeriodStartTimestamp(period)
        val totalTime = statsDao.getTotalListeningTimeSince(since) ?: 0L
        val totalPlays = statsDao.getTotalPlaysSince(since) ?: 0
        ListeningStatsOverview(totalTime, totalPlays)
    }

    suspend fun getRankedSongs(
        period: StatsFilterPeriod,
        mode: StatsRankingMode,
        allSongs: List<Song>
    ): List<SongRankingItem> = withContext(Dispatchers.IO) {
        val songMapById = allSongs.associateBy { it.id }
        val songMapByPath = allSongs.filter { it.filePath.isNotBlank() }.associateBy { it.filePath }

        val since = getPeriodStartTimestamp(period)
        val records = statsDao.getPlaybackRecordsSince(since)

        // Group records by songId or songPath
        val grouped = records.groupBy { record ->
            val matched = songMapById[record.songId] ?: songMapByPath[record.songPath]
            matched?.id ?: record.songId
        }

        val items = grouped.mapNotNull { (songId, recList) ->
            val song = songMapById[songId] ?: allSongs.find { it.id == songId } ?: return@mapNotNull null
            val plays = recList.count { it.isCountedPlay }
            val timeMs = recList.sumOf { it.timeListenedMs }

            if (plays > 0 || timeMs > 0) {
                SongRankingItem(song = song, playCount = plays, timeListenedMs = timeMs)
            } else null
        }

        when (mode) {
            StatsRankingMode.PLAYS -> items.sortedWith(
                compareByDescending<SongRankingItem> { it.playCount }
                    .thenByDescending { it.timeListenedMs }
            )
            StatsRankingMode.TIME_LISTENED -> items.sortedWith(
                compareByDescending<SongRankingItem> { it.timeListenedMs }
                    .thenByDescending { it.playCount }
            )
        }
    }

    suspend fun resetAllStats() = withContext(Dispatchers.IO) {
        statsDao.clearSongStats()
        statsDao.clearPlaybackRecords()
    }
}
