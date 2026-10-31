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
    val folderPath: String = ""
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
    RECENTLY_PLAYED
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
    val abRepeatB: Long? = null
)
