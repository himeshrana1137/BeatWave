package com.example.model

data class Song(
    val id: String,
    val title: String,
    val artist: String = "Unknown Artist",
    val album: String = "Unknown Album",
    val durationMs: Long = 0L,
    val uriString: String,
    val isFavorite: Boolean = false,
    val dateAdded: Long = System.currentTimeMillis(),
    val albumArtPath: String? = null,
    val isDemoTrack: Boolean = false,
    val playCount: Int = 0,
    val lastPlayedTime: Long = 0L,
    val lyricsUriString: String? = null,
    val folderName: String = "Music",
    val folderPath: String = "",
    val moodTags: List<String> = emptyList()
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

            // If title is only digits or blank, derive nice name from uri/file path
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

            // Replace underscores with spaces
            clean = clean.replace('_', ' ')

            // Strip tags such as (Lyrics), [HD], (Official Video), (8D AUDIO), etc.
            val tagRegex = """\s*[\(\[\{]\s*(?:official\s*(?:music\s*)?video|official\s*audio|official\s*lyric\s*video|official|lyrics?|8d\s*audio|hd|4k|hq|remastered|audio|video|visualizer|slowed(?:\s*\+\s*reverb)?|sped\s*up|clean|explicit)\s*[\)\]\}]""".toRegex(RegexOption.IGNORE_CASE)
            clean = tagRegex.replace(clean, "")

            // Collapse duplicate whitespace
            clean = clean.replace("""\s+""".toRegex(), " ").trim()

            return if (clean.isNotBlank()) clean else title
        }

    val displayArtist: String
        get() {
            val raw = artist.trim()
            val isMissing = raw.isBlank() ||
                    raw.equals("Unknown Artist", ignoreCase = true) ||
                    raw.equals("<unknown>", ignoreCase = true) ||
                    raw.equals("Unknown", ignoreCase = true)

            if (isMissing) {
                val folder = folderName.trim()
                if (folder.isNotBlank() &&
                    !folder.equals("Unknown", ignoreCase = true)
                ) {
                    return folder
                }
                if (folderPath.isNotBlank()) {
                    val dirName = folderPath.trimEnd('/').substringAfterLast('/')
                    if (dirName.isNotBlank()) {
                        return dirName
                    }
                }
                return if (folder.isNotBlank()) folder else "Music"
            }
            return raw
        }
}

data class ListeningStats(
    val totalTracksPlayed: Int = 0,
    val totalListeningTimeMs: Long = 0L,
    val mostPlayedSong: Song? = null,
    val mostPlayedArtist: String? = null,
    val totalSongs: Int = 0,
    val totalPlaylists: Int = 0
) {
    val formattedTotalTime: String
        get() {
            val totalSeconds = totalListeningTimeMs / 1000
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
        }
}

data class LyricLine(
    val timeMs: Long,
    val text: String
)

data class VideoItem(
    val id: String,
    val title: String,
    val durationMs: Long = 0L,
    val uriString: String,
    val lastPositionMs: Long = 0L,
    val thumbnailPath: String? = null,
    val dateAdded: Long = System.currentTimeMillis(),
    val subtitleUriString: String? = null,
    val isDemo: Boolean = false
) {
    val formattedDuration: String
        get() {
            if (durationMs <= 0) return "--:--"
            val totalSeconds = durationMs / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return String.format("%02d:%02d", minutes, seconds)
        }
}

data class SubtitleLine(
    val startMs: Long,
    val endMs: Long,
    val text: String
)

enum class VideoAspectRatio {
    FIT,
    FILL,
    ZOOM
}

data class Playlist(
    val id: String,
    val name: String,
    val description: String = "",
    val songIds: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)

enum class RepeatMode {
    OFF,
    ALL,
    ONE
}

enum class SortOption {
    TITLE_ASC,
    ARTIST_ASC,
    DURATION_DESC,
    DATE_ADDED_DESC,
    MOST_PLAYED,
    RECENTLY_PLAYED,
    QUALITY_DESC,
    QUALITY_ASC
}

data class AlbumItem(
    val title: String,
    val artist: String,
    val songCount: Int,
    val songs: List<Song>,
    val albumArtPath: String? = null
)

data class ArtistItem(
    val name: String,
    val songCount: Int,
    val albumCount: Int,
    val songs: List<Song>
)

data class FolderItem(
    val name: String,
    val path: String,
    val songCount: Int,
    val songs: List<Song>
)

enum class NavigationTab {
    SONGS,
    ALBUMS,
    ARTISTS,
    FOLDERS,
    VIDEOS,
    FAVORITES,
    RECENTLY_PLAYED,
    MOST_PLAYED,
    PLAYLISTS
}

enum class MainBottomTab {
    HOME,
    SEARCH,
    LIBRARY,
    VIDEOS
}

data class PlaybackSnapshot(
    val currentSong: Song? = null,
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val isShuffle: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val queue: List<Song> = emptyList(),
    val currentQueueIndex: Int = -1,
    val sleepTimerRemainingSeconds: Int? = null,
    val abRepeatA: Long? = null,
    val abRepeatB: Long? = null,
    val playbackSpeed: Float = 1.0f,
    val playbackPitch: Float = 1.0f
)
