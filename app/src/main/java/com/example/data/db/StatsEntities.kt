package com.example.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "playback_records",
    indices = [Index(value = ["timestamp"]), Index(value = ["songId"])]
)
data class PlaybackRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val songId: String,
    val songPath: String,
    val timestamp: Long,
    val timeListenedMs: Long,
    val isCountedPlay: Boolean
)

@Entity(tableName = "song_stats")
data class SongStatsEntity(
    @PrimaryKey
    val songId: String,
    val songPath: String,
    val playCount: Int,
    val totalTimeListenedMs: Long,
    val lastPlayedTimestamp: Long
)
