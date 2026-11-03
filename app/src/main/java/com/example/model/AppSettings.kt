package com.example.model

data class AppSettings(
    val autoPauseOnCallAndUnplug: Boolean = true,
    val headsetDoublePressEnabled: Boolean = true,
    val shakeToSkipEnabled: Boolean = false,
    val filterShortAudioEnabled: Boolean = true,
    val minAudioDurationSec: Int = 60,
    val blacklistedFolders: Set<String> = emptySet(),
    val volumeNormalization: Boolean = false,
    val fadeInOutEnabled: Boolean = true,
    val autoFetchLyrics: Boolean = true,
    val backgroundVideoAudioOnly: Boolean = false,
    val reduceAnimations: Boolean = false,
    val fetchOnlineCoverArt: Boolean = false,
    val showQualityBadges: Boolean = true,
    val autoScanEnabled: Boolean = true
)

data class DuplicateSongGroup(
    val key: String,
    val title: String,
    val artist: String,
    val songs: List<Song>
)

data class SongDetailedInfo(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationFormatted: String,
    val durationMs: Long,
    val format: String,
    val fileSizeFormatted: String,
    val fileSizeBytes: Long,
    val bitrateFormatted: String,
    val sampleRateFormatted: String,
    val filePath: String,
    val dateAddedFormatted: String
)
