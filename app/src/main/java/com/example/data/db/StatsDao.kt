package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface StatsDao {

    @Query("SELECT * FROM song_stats ORDER BY playCount DESC, totalTimeListenedMs DESC")
    fun getAllSongStatsFlow(): Flow<List<SongStatsEntity>>

    @Query("SELECT * FROM song_stats")
    suspend fun getAllSongStatsSync(): List<SongStatsEntity>

    @Query("SELECT * FROM song_stats WHERE songId = :songId OR songPath = :songPath LIMIT 1")
    suspend fun getSongStats(songId: String, songPath: String): SongStatsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateSongStats(stats: SongStatsEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaybackRecord(record: PlaybackRecordEntity)

    @Query("SELECT * FROM playback_records WHERE timestamp >= :sinceTimestamp")
    suspend fun getPlaybackRecordsSince(sinceTimestamp: Long): List<PlaybackRecordEntity>

    @Query("SELECT SUM(timeListenedMs) FROM playback_records WHERE timestamp >= :sinceTimestamp")
    suspend fun getTotalListeningTimeSince(sinceTimestamp: Long): Long?

    @Query("SELECT COUNT(*) FROM playback_records WHERE timestamp >= :sinceTimestamp AND isCountedPlay = 1")
    suspend fun getTotalPlaysSince(sinceTimestamp: Long): Int?

    @Query("DELETE FROM song_stats")
    suspend fun clearSongStats()

    @Query("DELETE FROM playback_records")
    suspend fun clearPlaybackRecords()
}
