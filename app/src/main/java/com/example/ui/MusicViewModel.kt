package com.example.ui

import android.app.Application
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.MusicRepository
import com.example.model.ListeningStatsOverview
import com.example.model.MainTab
import com.example.model.PlaybackSnapshot
import com.example.model.Playlist
import com.example.model.Song
import com.example.model.SongRankingItem
import com.example.model.SortOption
import com.example.model.StatsFilterPeriod
import com.example.model.StatsRankingMode
import com.example.playback.MusicPlaybackManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MusicViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MusicRepository(application)
    val playbackManager = MusicPlaybackManager(application)

    // Main Navigation Tab (Songs, Playlists, Stats)
    private val _currentTab = MutableStateFlow(MainTab.SONGS)
    val currentTab: StateFlow<MainTab> = _currentTab.asStateFlow()

    // Songs & Search & Sort
    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _sortOption = MutableStateFlow(SortOption.TITLE)
    val sortOption: StateFlow<SortOption> = _sortOption.asStateFlow()

    val filteredSongs: StateFlow<List<Song>> = combine(
        _songs,
        _searchQuery,
        _sortOption
    ) { allSongs, query, sort ->
        val searched = if (query.isBlank()) {
            allSongs
        } else {
            val q = query.trim().lowercase()
            allSongs.filter {
                it.title.lowercase().contains(q) ||
                        it.artist.lowercase().contains(q) ||
                        it.album.lowercase().contains(q)
            }
        }

        when (sort) {
            SortOption.TITLE -> searched.sortedBy { it.title.lowercase() }
            SortOption.ARTIST -> searched.sortedBy { it.artist.lowercase() }
            SortOption.DATE_ADDED -> searched.sortedByDescending { it.dateAdded }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Playlists (Live from Room)
    val playlists: StateFlow<List<Playlist>> = repository.getPlaylistsFlow()
        .combine(_songs) { entities, allSongs ->
            val songMapById = allSongs.associateBy { it.id }
            val songMapByPath = allSongs.filter { it.filePath.isNotBlank() }.associateBy { it.filePath }

            entities.map { entity ->
                Playlist(
                    id = entity.id,
                    name = entity.name,
                    createdAt = entity.createdAt,
                    songs = emptyList() // Will be resolved when selected or needed
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedPlaylist = MutableStateFlow<Playlist?>(null)
    val selectedPlaylist: StateFlow<Playlist?> = _selectedPlaylist.asStateFlow()

    val playlistSongs: StateFlow<List<Song>> = combine(
        _selectedPlaylist,
        _songs
    ) { selectedPl, allSongs ->
        if (selectedPl == null) emptyList()
        else {
            val songMapById = allSongs.associateBy { it.id }
            val songMapByPath = allSongs.filter { it.filePath.isNotBlank() }.associateBy { it.filePath }

            val entities = repository.getPlaylistsFlow() // trigger
            selectedPl.songs
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Multi-Selection State
    private val _selectedSongIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedSongIds: StateFlow<Set<String>> = _selectedSongIds.asStateFlow()

    // Full Screen Player
    private val _isFullScreenPlayerOpen = MutableStateFlow(false)
    val isFullScreenPlayerOpen: StateFlow<Boolean> = _isFullScreenPlayerOpen.asStateFlow()

    // Playback state
    val playbackSnapshot: StateFlow<PlaybackSnapshot> = playbackManager.snapshot

    // Listening Stats State
    private val _statsPeriod = MutableStateFlow(StatsFilterPeriod.ALL_TIME)
    val statsPeriod: StateFlow<StatsFilterPeriod> = _statsPeriod.asStateFlow()

    private val _statsRankingMode = MutableStateFlow(StatsRankingMode.PLAYS)
    val statsRankingMode: StateFlow<StatsRankingMode> = _statsRankingMode.asStateFlow()

    private val _statsOverview = MutableStateFlow(ListeningStatsOverview())
    val statsOverview: StateFlow<ListeningStatsOverview> = _statsOverview.asStateFlow()

    private val _rankedSongs = MutableStateFlow<List<SongRankingItem>>(emptyList())
    val rankedSongs: StateFlow<List<SongRankingItem>> = _rankedSongs.asStateFlow()

    // User Feedback Message (Toast/Snackbar)
    private val _userFeedbackMessage = MutableStateFlow<String?>(null)
    val userFeedbackMessage: StateFlow<String?> = _userFeedbackMessage.asStateFlow()

    // MediaStore Content Observer for automatic rescan
    private var mediaObserver: ContentObserver? = null

    init {
        rescanSongs()
        registerMediaObserver()

        // Refresh stats when playback finishes or pauses
        playbackManager.onSongPlayedListener = {
            viewModelScope.launch {
                refreshStats()
            }
        }
    }

    private fun registerMediaObserver() {
        try {
            val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) {
                    super.onChange(selfChange)
                    rescanSongs()
                }
            }
            getApplication<Application>().contentResolver.registerContentObserver(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                true,
                observer
            )
            mediaObserver = observer
        } catch (e: Exception) {
            Log.w("MusicViewModel", "Failed to register media observer: ${e.message}")
        }
    }

    fun rescanSongs() {
        viewModelScope.launch(Dispatchers.IO) {
            val scanned = repository.scanMediaStoreAudio()
            _songs.value = scanned
            refreshStats()
            _selectedPlaylist.value?.let { currentSelected ->
                loadPlaylistDetails(currentSelected.id)
            }
        }
    }

    fun setTab(tab: MainTab) {
        _currentTab.value = tab
        if (tab == MainTab.STATS) {
            refreshStats()
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSortOption(sort: SortOption) {
        _sortOption.value = sort
    }

    fun setFullScreenPlayerOpen(open: Boolean) {
        _isFullScreenPlayerOpen.value = open
    }

    fun clearUserFeedback() {
        _userFeedbackMessage.value = null
    }

    // ==========================================
    // Multi-Selection Logic
    // ==========================================

    fun toggleSongSelection(songId: String) {
        val current = _selectedSongIds.value.toMutableSet()
        if (current.contains(songId)) {
            current.remove(songId)
        } else {
            current.add(songId)
        }
        _selectedSongIds.value = current
    }

    fun selectAllSongs(availableSongs: List<Song>) {
        val current = _selectedSongIds.value
        val allIds = availableSongs.map { it.id }.toSet()
        _selectedSongIds.value = if (current.size == allIds.size) emptySet() else allIds
    }

    fun clearSelection() {
        _selectedSongIds.value = emptySet()
    }

    // ==========================================
    // Playlists Operations (Room-backed)
    // ==========================================

    fun selectPlaylist(playlist: Playlist?) {
        if (playlist == null) {
            _selectedPlaylist.value = null
        } else {
            loadPlaylistDetails(playlist.id)
        }
    }

    fun loadPlaylistDetails(playlistId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentSongs = _songs.value
            val songMapById = currentSongs.associateBy { it.id }
            val songMapByPath = currentSongs.filter { it.filePath.isNotBlank() }.associateBy { it.filePath }

            val playlistEntity = repository.getPlaylistsFlow()
            val entities = com.example.data.db.AppDatabase.getInstance(getApplication())
                .playlistDao()
                .getSongsForPlaylistSync(playlistId)

            val resolvedSongs = entities.mapNotNull { entity ->
                val s = songMapById[entity.songId] ?: songMapByPath[entity.songPath]
                if (s != null && (s.filePath.isBlank() || java.io.File(s.filePath).exists())) {
                    s
                } else null
            }

            val plEntity = com.example.data.db.AppDatabase.getInstance(getApplication())
                .playlistDao()
                .getPlaylistById(playlistId)

            if (plEntity != null) {
                _selectedPlaylist.value = Playlist(
                    id = plEntity.id,
                    name = plEntity.name,
                    createdAt = plEntity.createdAt,
                    songs = resolvedSongs
                )
            }
        }
    }

    fun createPlaylist(name: String, songsToAdd: List<Song> = emptyList()) {
        viewModelScope.launch(Dispatchers.IO) {
            val newId = repository.createPlaylist(name)
            if (songsToAdd.isNotEmpty()) {
                val added = repository.addSongsToPlaylist(newId, songsToAdd)
                _userFeedbackMessage.value = "$added songs added to $name"
            } else {
                _userFeedbackMessage.value = "Created playlist \"$name\""
            }
            clearSelection()
        }
    }

    fun renamePlaylist(playlistId: Long, newName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.renamePlaylist(playlistId, newName)
            loadPlaylistDetails(playlistId)
        }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deletePlaylist(playlistId)
            if (_selectedPlaylist.value?.id == playlistId) {
                _selectedPlaylist.value = null
            }
            _userFeedbackMessage.value = "Playlist deleted"
        }
    }

    fun addSelectedSongsToPlaylist(playlistId: Long, playlistName: String) {
        val selected = _songs.value.filter { _selectedSongIds.value.contains(it.id) }
        if (selected.isEmpty()) return

        viewModelScope.launch(Dispatchers.IO) {
            val addedCount = repository.addSongsToPlaylist(playlistId, selected)
            _userFeedbackMessage.value = "$addedCount songs added to $playlistName"
            clearSelection()
            loadPlaylistDetails(playlistId)
        }
    }

    fun addSongsToPlaylist(playlistId: Long, songs: List<Song>) {
        if (songs.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            val addedCount = repository.addSongsToPlaylist(playlistId, songs)
            _userFeedbackMessage.value = "$addedCount songs added"
            loadPlaylistDetails(playlistId)
        }
    }

    fun removeSongsFromPlaylist(playlistId: Long, songsToRemove: List<Song>) {
        if (songsToRemove.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            repository.removeSongsFromPlaylist(playlistId, songsToRemove)
            loadPlaylistDetails(playlistId)
            _userFeedbackMessage.value = "${songsToRemove.size} songs removed from playlist"
        }
    }

    // ==========================================
    // Playback Helpers
    // ==========================================

    fun playSong(song: Song, queue: List<Song> = emptyList()) {
        playbackManager.playSong(song, queue)
    }

    fun playPlaylist(playlist: Playlist, startSong: Song? = null, isShuffle: Boolean = false) {
        val playlistSongs = playlist.songs
        if (playlistSongs.isEmpty()) return

        if (isShuffle) {
            playbackManager.playQueue(playlistSongs.shuffled(), 0)
        } else {
            val startIndex = if (startSong != null) playlistSongs.indexOfFirst { it.id == startSong.id }.coerceAtLeast(0) else 0
            playbackManager.playQueue(playlistSongs, startIndex)
        }
    }

    // ==========================================
    // Statistics Operations
    // ==========================================

    fun setStatsPeriod(period: StatsFilterPeriod) {
        _statsPeriod.value = period
        refreshStats()
    }

    fun setStatsRankingMode(mode: StatsRankingMode) {
        _statsRankingMode.value = mode
        refreshStats()
    }

    fun refreshStats() {
        viewModelScope.launch(Dispatchers.IO) {
            val period = _statsPeriod.value
            val mode = _statsRankingMode.value
            val allSongs = _songs.value

            val overview = repository.getStatsOverview(period)
            val ranked = repository.getRankedSongs(period, mode, allSongs)

            _statsOverview.value = overview
            _rankedSongs.value = ranked
        }
    }

    fun resetStatistics() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.resetAllStats()
            rescanSongs()
            _userFeedbackMessage.value = "Statistics reset"
        }
    }

    override fun onCleared() {
        mediaObserver?.let {
            getApplication<Application>().contentResolver.unregisterContentObserver(it)
        }
        playbackManager.release()
        super.onCleared()
    }
}
