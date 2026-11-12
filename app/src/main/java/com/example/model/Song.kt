package com.example.model

data class Song(
    val id: String,
    val title: String,
    val artist: String = "Unknown Artist",
    val album: String = "Unknown Album",
    val durationMs: Long = 0L,
    val uriString: String,
    val dateAdded: Long = System.currentTimeMillis(),
    val albumArtPath: String? = null,
    val playCount: Int = 0,
    val totalTimeListenedMs: Long = 0L,
    val lastPlayedTime: Long = 0L,
    val filePath: String = ""
) {
    val formattedDuration: String
        get() {
            if (durationMs <= 0) return "--:--"
            val totalSeconds = durationMs / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return String.format("%02d:%02d", minutes, seconds)
        }

    val displayTitle: String
        get() {
            var clean = title.trim()
            if (clean.isBlank() || clean.all { it.isDigit() }) {
                val fileName = try {
                    val path = if (uriString.startsWith("content://")) {
                        android.net.Uri.parse(uriString).lastPathSegment ?: ""
                    } else {
                        java.io.File(uriString).name
                    }
                    path.substringBeforeLast('.')
                } catch (_: Exception) {
                    ""
                }
                if (fileName.isNotBlank() && !fileName.all { it.isDigit() }) {
                    clean = fileName
                }
            }
            clean = clean.replace('_', ' ')
            val tagRegex = """\s*[\(\[\{]\s*(?:official\s*(?:music\s*)?video|official\s*audio|official\s*lyric\s*video|official|lyrics?|8d\s*audio|hd|4k|hq|remastered|audio|video|visualizer|slowed(?:\s*\+\s*reverb)?|sped\s*up|clean|explicit)\s*[\)\]\}]""".toRegex(RegexOption.IGNORE_CASE)
            clean = tagRegex.replace(clean, "")
            clean = clean.replace("""\s+""".toRegex(), " ").trim()
            return if (clean.isNotBlank()) clean else title
        }

    val displayArtist: String
        get() {
            val raw = artist.trim()
            if (raw.isBlank() ||
                raw.equals("Unknown Artist", ignoreCase = true) ||
                raw.equals("<unknown>", ignoreCase = true) ||
                raw.equals("Unknown", ignoreCase = true)
            ) {
                return "Unknown Artist"
            }
            return raw
        }
}

data class Playlist(
    val id: Long,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val songs: List<Song> = emptyList()
) {
    val songCount: Int get() = songs.size
    val totalDurationMs: Long get() = songs.sumOf { it.durationMs }

    val formattedDuration: String
        get() {
            if (totalDurationMs <= 0) return "0 min"
            val totalSeconds = totalDurationMs / 1000
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            return if (hours > 0) "${hours} hr ${minutes} min" else "${minutes} min"
        }
}

enum class RepeatMode {
    OFF,
    ALL,
    ONE
}

enum class SortOption {
    TITLE,
    ARTIST,
    DATE_ADDED
}

enum class MainTab {
    SONGS,
    PLAYLISTS,
    STATS
}

enum class StatsFilterPeriod(val label: String) {
    TODAY("Today"),
    THIS_WEEK("This week"),
    THIS_MONTH("This month"),
    ALL_TIME("All time")
}

enum class StatsRankingMode(val label: String) {
    PLAYS("By plays"),
    TIME_LISTENED("By time")
}

data class SongRankingItem(
    val song: Song,
    val playCount: Int,
    val timeListenedMs: Long
) {
    val formattedTimeListened: String
        get() {
            val seconds = timeListenedMs / 1000
            val hours = seconds / 3600
            val minutes = (seconds % 3600) / 60
            val secs = seconds % 60
            return when {
                hours > 0 -> "${hours}h ${minutes}m"
                minutes > 0 -> "${minutes}m ${secs}s"
                else -> "${secs}s"
            }
        }
}

data class ListeningStatsOverview(
    val totalListeningTimeMs: Long = 0L,
    val totalPlays: Int = 0
) {
    val formattedTotalTime: String
        get() {
            val totalSeconds = totalListeningTimeMs / 1000
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            return if (hours > 0) "${hours} h ${minutes} min" else "${minutes} min"
        }
}

data class PlaybackSnapshot(
    val currentSong: Song? = null,
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val isShuffle: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val queue: List<Song> = emptyList(),
    val currentQueueIndex: Int = -1
)
