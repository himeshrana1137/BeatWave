package com.example.ui

import android.app.Application
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.MusicRepository
import com.example.model.AlbumItem
import com.example.model.AppSettings
import com.example.model.ArtistItem
import com.example.model.ContainerSortMode
import com.example.model.DuplicateSongGroup
import com.example.model.FolderItem
import com.example.model.MainBottomTab
import com.example.model.NavigationTab
import com.example.model.PlaybackSnapshot
import com.example.model.Playlist
import com.example.model.Song
import com.example.model.displayTitle
import com.example.model.displayArtist
import com.example.model.SongDetailedInfo
import com.example.model.AudioQualityFilter
import com.example.model.AudioQualityTier
import com.example.model.SortOption
import com.example.model.VideoItem
import com.example.playback.EqualizerState
import com.example.playback.MusicPlaybackManager
import com.example.playback.MusicService
import com.example.ui.theme.AppThemeMode
import com.example.util.AlbumArtLoader
import com.example.util.MediaQualityManager
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class MusicViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MusicRepository(application)
    val playbackManager = MusicPlaybackManager(application)

    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs.asStateFlow()

    private val _videos = MutableStateFlow<List<VideoItem>>(emptyList())
    val videos: StateFlow<List<VideoItem>> = _videos.asStateFlow()

    private val _selectedVideo = MutableStateFlow<VideoItem?>(null)
    val selectedVideo: StateFlow<VideoItem?> = _selectedVideo.asStateFlow()

    private val _playlists = MutableStateFlow<List<Playlist>>(emptyList())
    val playlists: StateFlow<List<Playlist>> = _playlists.asStateFlow()

    private val _mainBottomTab = MutableStateFlow(MainBottomTab.HOME)
    val mainBottomTab: StateFlow<MainBottomTab> = _mainBottomTab.asStateFlow()

    private val _themeMode = MutableStateFlow(AppThemeMode.DARK)
    val themeMode: StateFlow<AppThemeMode> = _themeMode.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _activeTab = MutableStateFlow(NavigationTab.SONGS)
    val activeTab: StateFlow<NavigationTab> = _activeTab.asStateFlow()

    private val _sortOption = MutableStateFlow(SortOption.DATE_ADDED_DESC)
    val sortOption: StateFlow<SortOption> = _sortOption.asStateFlow()

    private val _audioQualityFilter = MutableStateFlow(AudioQualityFilter.ALL)
    val audioQualityFilter: StateFlow<AudioQualityFilter> = _audioQualityFilter.asStateFlow()

    fun setAudioQualityFilter(filter: AudioQualityFilter) {
        _audioQualityFilter.value = filter
    }

    fun setShowQualityBadges(enabled: Boolean) {
        repository.setShowQualityBadges(enabled)
        _appSettings.value = repository.getAppSettings()
    }

    private val _selectedPlaylist = MutableStateFlow<Playlist?>(null)
    val selectedPlaylist: StateFlow<Playlist?> = _selectedPlaylist.asStateFlow()

    private val _selectedAlbum = MutableStateFlow<AlbumItem?>(null)
    val selectedAlbum: StateFlow<AlbumItem?> = _selectedAlbum.asStateFlow()

    private val _selectedArtist = MutableStateFlow<ArtistItem?>(null)
    val selectedArtist: StateFlow<ArtistItem?> = _selectedArtist.asStateFlow()

    private val _selectedFolder = MutableStateFlow<FolderItem?>(null)
    val selectedFolder: StateFlow<FolderItem?> = _selectedFolder.asStateFlow()

    private val _isFullScreenPlayerOpen = MutableStateFlow(false)
    val isFullScreenPlayerOpen: StateFlow<Boolean> = _isFullScreenPlayerOpen.asStateFlow()

    private val _isVisualizerEnabled = MutableStateFlow(repository.isVisualizerEnabled())
    val isVisualizerEnabled: StateFlow<Boolean> = _isVisualizerEnabled.asStateFlow()

    private val _listeningStats = MutableStateFlow(com.example.model.ListeningStats())
    val listeningStats: StateFlow<com.example.model.ListeningStats> = _listeningStats.asStateFlow()

    private val _songToAddToPlaylist = MutableStateFlow<Song?>(null)
    val songToAddToPlaylist: StateFlow<Song?> = _songToAddToPlaylist.asStateFlow()

    private val _showCreatePlaylistDialog = MutableStateFlow(false)
    val showCreatePlaylistDialog: StateFlow<Boolean> = _showCreatePlaylistDialog.asStateFlow()

    private val _showEqualizer = MutableStateFlow(false)
    val showEqualizer: StateFlow<Boolean> = _showEqualizer.asStateFlow()

    private val _showSettings = MutableStateFlow(false)
    val showSettings: StateFlow<Boolean> = _showSettings.asStateFlow()

    private val _appSettings = MutableStateFlow(repository.getAppSettings())
    val appSettings: StateFlow<AppSettings> = _appSettings.asStateFlow()

    private val _selectedSongForInfo = MutableStateFlow<Song?>(null)
    val selectedSongForInfo: StateFlow<Song?> = _selectedSongForInfo.asStateFlow()

    private val _songDetailedInfo = MutableStateFlow<SongDetailedInfo?>(null)
    val songDetailedInfo: StateFlow<SongDetailedInfo?> = _songDetailedInfo.asStateFlow()

    private val _selectedSongForRingtone = MutableStateFlow<Song?>(null)
    val selectedSongForRingtone: StateFlow<Song?> = _selectedSongForRingtone.asStateFlow()

    private val _showDuplicateFinderDialog = MutableStateFlow(false)
    val showDuplicateFinderDialog: StateFlow<Boolean> = _showDuplicateFinderDialog.asStateFlow()

    private val _duplicateGroups = MutableStateFlow<List<DuplicateSongGroup>>(emptyList())
    val duplicateGroups: StateFlow<List<DuplicateSongGroup>> = _duplicateGroups.asStateFlow()

    private val _showFolderBlacklistDialog = MutableStateFlow(false)
    val showFolderBlacklistDialog: StateFlow<Boolean> = _showFolderBlacklistDialog.asStateFlow()

    val vaultManager = com.example.util.VaultManager(application)
    val achievementManager = com.example.util.AchievementManager(application)

    private val _songForMoodTag = MutableStateFlow<Song?>(null)
    val songForMoodTag: StateFlow<Song?> = _songForMoodTag.asStateFlow()

    private val _showVaultDialog = MutableStateFlow(false)
    val showVaultDialog: StateFlow<Boolean> = _showVaultDialog.asStateFlow()

    private val _showStreamUrlDialog = MutableStateFlow(false)
    val showStreamUrlDialog: StateFlow<Boolean> = _showStreamUrlDialog.asStateFlow()

    private val _showWrappedDialog = MutableStateFlow(false)
    val showWrappedDialog: StateFlow<Boolean> = _showWrappedDialog.asStateFlow()

    private val _showAchievementsDialog = MutableStateFlow(false)
    val showAchievementsDialog: StateFlow<Boolean> = _showAchievementsDialog.asStateFlow()

    fun openMoodTagDialog(song: Song) {
        _songForMoodTag.value = song
    }

    fun closeMoodTagDialog() {
        _songForMoodTag.value = null
    }

    fun setSongMoods(songId: String, moods: List<String>) {
        viewModelScope.launch {
            repository.setSongMoods(songId, moods)
            _songs.value = repository.loadSongs()
            _playlists.value = repository.loadPlaylists()
        }
    }

    fun openVaultDialog() {
        _showVaultDialog.value = true
    }

    fun closeVaultDialog() {
        _showVaultDialog.value = false
    }

    fun openStreamUrlDialog() {
        _showStreamUrlDialog.value = true
    }

    fun closeStreamUrlDialog() {
        _showStreamUrlDialog.value = false
    }

    fun openWrappedDialog() {
        _showWrappedDialog.value = true
    }

    fun closeWrappedDialog() {
        _showWrappedDialog.value = false
    }

    fun openAchievementsDialog() {
        _showAchievementsDialog.value = true
    }

    fun closeAchievementsDialog() {
        _showAchievementsDialog.value = false
    }

    fun playNetworkStream(url: String, title: String) {
        val streamVideo = VideoItem(
            id = UUID.randomUUID().toString(),
            title = title.ifBlank { "Network Stream" },
            uriString = url,
            durationMs = 0L,
            dateAdded = System.currentTimeMillis()
        )
        selectVideo(streamVideo)
    }

    fun setShowEqualizer(show: Boolean) {
        _showEqualizer.value = show
    }

    fun setShowSettings(show: Boolean) {
        _showSettings.value = show
    }

    fun setAutoPauseOnCallAndUnplug(enabled: Boolean) {
        repository.setAutoPauseOnCallAndUnplug(enabled)
        _appSettings.value = repository.getAppSettings()
    }

    fun setHeadsetDoublePressEnabled(enabled: Boolean) {
        repository.setHeadsetDoublePressEnabled(enabled)
        _appSettings.value = repository.getAppSettings()
    }

    fun setShakeToSkipEnabled(enabled: Boolean) {
        repository.setShakeToSkipEnabled(enabled)
        _appSettings.value = repository.getAppSettings()
    }

    fun setFilterShortAudioEnabled(enabled: Boolean) {
        repository.setFilterShortAudioEnabled(enabled)
        _appSettings.value = repository.getAppSettings()
        viewModelScope.launch {
            _songs.value = repository.loadSongs()
        }
    }

    fun setMinAudioDurationSec(seconds: Int) {
        repository.setMinAudioDurationSec(seconds)
        _appSettings.value = repository.getAppSettings()
        viewModelScope.launch {
            _songs.value = repository.loadSongs()
        }
    }

    fun addBlacklistedFolder(folder: String) {
        repository.addBlacklistedFolder(folder)
        _appSettings.value = repository.getAppSettings()
        viewModelScope.launch {
            _songs.value = repository.loadSongs()
        }
    }

    fun removeBlacklistedFolder(folder: String) {
        repository.removeBlacklistedFolder(folder)
        _appSettings.value = repository.getAppSettings()
        viewModelScope.launch {
            _songs.value = repository.loadSongs()
        }
    }

    fun openSongInfo(song: Song) {
        _selectedSongForInfo.value = song
        viewModelScope.launch {
            _songDetailedInfo.value = repository.getSongDetailedInfo(song)
        }
    }

    fun closeSongInfo() {
        _selectedSongForInfo.value = null
        _songDetailedInfo.value = null
    }

    fun openRingtoneCutter(song: Song) {
        _selectedSongForRingtone.value = song
    }

    fun closeRingtoneCutter() {
        _selectedSongForRingtone.value = null
    }

    fun openDuplicateFinder() {
        viewModelScope.launch {
            _duplicateGroups.value = repository.findDuplicateSongs()
            _showDuplicateFinderDialog.value = true
        }
    }

    fun closeDuplicateFinder() {
        _showDuplicateFinderDialog.value = false
    }

    fun autoCleanDuplicateGroup(group: DuplicateSongGroup) {
        viewModelScope.launch {
            val toDelete = group.songs.drop(1)
            toDelete.forEach { deleteSong(it) }
            _duplicateGroups.value = repository.findDuplicateSongs()
        }
    }

    fun openFolderBlacklist() {
        _showFolderBlacklistDialog.value = true
    }

    fun closeFolderBlacklist() {
        _showFolderBlacklistDialog.value = false
    }

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val playbackSnapshot: StateFlow<PlaybackSnapshot> = playbackManager.snapshot
    val equalizerState: StateFlow<EqualizerState> = playbackManager.equalizerManager.state

    // Grouping flows for Albums, Artists, and Folders
    val albums: StateFlow<List<AlbumItem>> = combine(_songs, _searchQuery) { songList, query ->
        val q = query.trim().lowercase()
        songList.groupBy { it.album }
            .map { (albumTitle, tracks) ->
                AlbumItem(
                    title = albumTitle,
                    artist = tracks.firstOrNull()?.artist ?: "Unknown Artist",
                    songCount = tracks.size,
                    songs = tracks,
                    albumArtPath = tracks.firstOrNull { !it.albumArtPath.isNullOrBlank() }?.albumArtPath
                )
            }
            .filter { album ->
                if (q.isBlank()) true
                else album.title.lowercase().contains(q) || album.artist.lowercase().contains(q)
            }
            .sortedBy { it.title.lowercase() }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val artists: StateFlow<List<ArtistItem>> = combine(_songs, _searchQuery) { songList, query ->
        val q = query.trim().lowercase()
        songList.groupBy { it.artist }
            .map { (artistName, tracks) ->
                ArtistItem(
                    name = artistName,
                    songCount = tracks.size,
                    albumCount = tracks.map { it.album }.distinct().size,
                    songs = tracks
                )
            }
            .filter { artist ->
                if (q.isBlank()) true
                else artist.name.lowercase().contains(q)
            }
            .sortedBy { it.name.lowercase() }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val folders: StateFlow<List<FolderItem>> = combine(_songs, _searchQuery) { songList, query ->
        val q = query.trim().lowercase()
        songList.groupBy { it.folderName.ifBlank { "Music" } }
            .map { (folderName, tracks) ->
                FolderItem(
                    name = folderName,
                    path = tracks.firstOrNull()?.folderPath ?: "",
                    songCount = tracks.size,
                    songs = tracks
                )
            }
            .filter { folder ->
                if (q.isBlank()) true
                else folder.name.lowercase().contains(q) || folder.path.lowercase().contains(q)
            }
            .sortedBy { it.name.lowercase() }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered and sorted songs
    val filteredSongs: StateFlow<List<Song>> = combine(
        _songs,
        _searchQuery,
        _sortOption,
        _activeTab,
        _selectedPlaylist,
        _selectedAlbum,
        _selectedArtist,
        _selectedFolder,
        _audioQualityFilter
    ) { args ->
        val songsList = args[0] as List<Song>
        val query = args[1] as String
        val sort = args[2] as SortOption
        val tab = args[3] as NavigationTab
        val playlist = args[4] as Playlist?
        val album = args[5] as AlbumItem?
        val artist = args[6] as ArtistItem?
        val folder = args[7] as FolderItem?
        val qualityFilter = args[8] as AudioQualityFilter

        var list = when {
            playlist != null -> {
                val containerKey = "playlist_${playlist.id}"
                val songMap = songsList.associateBy { it.id }
                val rawSongs = playlist.songIds.mapNotNull { songMap[it] }
                repository.resolveContainerSongs(containerKey, rawSongs).first
            }
            album != null -> {
                songsList.filter { it.album == album.title }
            }
            artist != null -> {
                songsList.filter { it.artist == artist.name }
            }
            folder != null -> {
                val containerKey = "folder_${folder.path.ifBlank { folder.name }}"
                val rawSongs = songsList.filter { (it.folderName.ifBlank { "Music" }) == folder.name }
                repository.resolveContainerSongs(containerKey, rawSongs).first
            }
            tab == NavigationTab.FAVORITES -> {
                songsList.filter { it.isFavorite }
            }
            tab == NavigationTab.RECENTLY_PLAYED -> {
                songsList.filter { it.lastPlayedTime > 0L }.sortedByDescending { it.lastPlayedTime }
            }
            tab == NavigationTab.MOST_PLAYED -> {
                songsList.filter { it.playCount > 0 }.sortedByDescending { it.playCount }
            }
            else -> songsList
        }

        if (query.isNotBlank()) {
            val q = query.trim().lowercase()
            list = list.filter {
                it.title.lowercase().contains(q) ||
                        it.displayTitle.lowercase().contains(q) ||
                        it.artist.lowercase().contains(q) ||
                        it.displayArtist.lowercase().contains(q) ||
                        it.album.lowercase().contains(q) ||
                        it.folderName.lowercase().contains(q)
            }
        }

        if (qualityFilter != AudioQualityFilter.ALL) {
            list = list.filter { song ->
                val q = MediaQualityManager.getAudioQualitySync(song)
                when (qualityFilter) {
                    AudioQualityFilter.LOSSLESS -> q.tier == AudioQualityTier.LOSSLESS
                    AudioQualityFilter.HIGH -> q.tier == AudioQualityTier.HIGH
                    AudioQualityFilter.STANDARD -> q.tier == AudioQualityTier.STANDARD
                    AudioQualityFilter.LOW -> q.tier == AudioQualityTier.LOW
                    AudioQualityFilter.ALL -> true
                }
            }
        }

        if (tab != NavigationTab.RECENTLY_PLAYED && tab != NavigationTab.MOST_PLAYED && album == null && artist == null) {
            when (sort) {
                SortOption.TITLE_ASC -> list.sortedBy { it.title.lowercase() }
                SortOption.ARTIST_ASC -> list.sortedBy { it.artist.lowercase() }
                SortOption.DURATION_DESC -> list.sortedByDescending { it.durationMs }
                SortOption.DATE_ADDED_DESC -> list.sortedByDescending { it.dateAdded }
                SortOption.MOST_PLAYED -> list.sortedByDescending { it.playCount }
                SortOption.RECENTLY_PLAYED -> list.sortedByDescending { it.lastPlayedTime }
                SortOption.QUALITY_DESC -> list.sortedWith(
                    compareByDescending<Song> { MediaQualityManager.getAudioQualitySync(it).tier.rank }
                        .thenByDescending { MediaQualityManager.getAudioQualitySync(it).bitrateKbps }
                        .thenBy { it.title.lowercase() }
                )
                SortOption.QUALITY_ASC -> list.sortedWith(
                    compareBy<Song> { MediaQualityManager.getAudioQualitySync(it).tier.rank }
                        .thenBy { MediaQualityManager.getAudioQualitySync(it).bitrateKbps }
                        .thenBy { it.title.lowercase() }
                )
            }
        } else {
            list
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var mediaObserver: ContentObserver? = null

    init {
        MusicService.playbackManager = playbackManager
        playbackManager.onSongPlayedListener = { song ->
            viewModelScope.launch {
                repository.recordSongPlayed(song.id)
                _songs.value = repository.loadSongs()
                refreshListeningStats()
            }
        }
        loadInitialData()
        registerMediaObserver()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            _isLoading.value = true
            val loadedSongs = repository.loadSongs()
            val loadedPlaylists = repository.loadPlaylists()
            val loadedVideos = repository.loadVideos()
            _songs.value = loadedSongs
            _playlists.value = loadedPlaylists
            _videos.value = loadedVideos
            refreshListeningStats()
            _isLoading.value = false
            scanDeviceMedia()
        }
    }

    fun scanDeviceMedia() {
        viewModelScope.launch {
            try {
                AlbumArtLoader.clearNoArtCache()
                val (updatedSongs, updatedVideos) = repository.syncDeviceMedia()
                _songs.value = updatedSongs
                _videos.value = updatedVideos
                refreshListeningStats()
            } catch (e: Exception) {
                Log.e("MusicViewModel", "Error scanning device media: ${e.message}")
            }
        }
    }

    private fun registerMediaObserver() {
        try {
            mediaObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean, uri: Uri?) {
                    super.onChange(selfChange, uri)
                    scanDeviceMedia()
                }
            }
            val resolver = getApplication<Application>().contentResolver
            resolver.registerContentObserver(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                true,
                mediaObserver!!
            )
            resolver.registerContentObserver(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                true,
                mediaObserver!!
            )
        } catch (e: Exception) {
            Log.w("MusicViewModel", "Unable to register MediaStore observer: ${e.message}")
        }
    }

    fun setMainBottomTab(tab: MainBottomTab) {
        _mainBottomTab.value = tab
    }

    fun setThemeMode(mode: AppThemeMode) {
        _themeMode.value = mode
    }

    fun onSearchQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun setActiveTab(tab: NavigationTab) {
        _selectedPlaylist.value = null
        _selectedAlbum.value = null
        _selectedArtist.value = null
        _selectedFolder.value = null
        _activeTab.value = tab
    }

    fun setSortOption(sort: SortOption) {
        _sortOption.value = sort
    }

    fun selectPlaylist(playlist: Playlist?) {
        _selectedPlaylist.value = playlist
    }

    fun selectAlbum(album: AlbumItem?) {
        _selectedAlbum.value = album
    }

    fun selectArtist(artist: ArtistItem?) {
        _selectedArtist.value = artist
    }

    fun selectFolder(folder: FolderItem?) {
        _selectedFolder.value = folder
    }

    fun playGroup(songs: List<Song>, startIndex: Int = 0) {
        if (songs.isNotEmpty()) {
            val index = startIndex.coerceIn(0, songs.lastIndex)
            playbackManager.playSong(songs[index], songs)
        }
    }

    fun shuffleGroup(songs: List<Song>) {
        if (songs.isNotEmpty()) {
            val shuffled = songs.shuffled()
            playbackManager.playSong(shuffled.first(), shuffled)
        }
    }

    fun setFullScreenPlayerOpen(isOpen: Boolean) {
        _isFullScreenPlayerOpen.value = isOpen
    }

    fun setShowCreatePlaylistDialog(show: Boolean) {
        _showCreatePlaylistDialog.value = show
    }

    fun setSongToAddToPlaylist(song: Song?) {
        _songToAddToPlaylist.value = song
    }

    fun toggleFavorite(song: Song) {
        val updated = _songs.value.map {
            if (it.id == song.id) it.copy(isFavorite = !it.isFavorite) else it
        }
        _songs.value = updated
        viewModelScope.launch {
            repository.saveSongs(updated)
        }
    }

    fun addSongsFromUris(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            _isLoading.value = true
            val newSongs = repository.parseSongsFromUris(uris)
            val updated = _songs.value + newSongs
            _songs.value = updated
            repository.saveSongs(updated)
            _isLoading.value = false

            if (playbackSnapshot.value.currentSong == null && newSongs.isNotEmpty()) {
                playbackManager.playSong(newSongs.first(), updated)
            }
        }
    }

    fun generateSampleTracks() {
        viewModelScope.launch {
            _isLoading.value = true
            val demoSongs = repository.generateDemoTracksIfEmpty()
            val currentMap = _songs.value.associateBy { it.id }.toMutableMap()
            demoSongs.forEach { currentMap[it.id] = it }
            val updated = currentMap.values.toList()
            _songs.value = updated
            repository.saveSongs(updated)
            _isLoading.value = false

            if (playbackSnapshot.value.currentSong == null && demoSongs.isNotEmpty()) {
                playbackManager.playSong(demoSongs.first(), updated)
            }
        }
    }

    fun createPlaylist(name: String, description: String) {
        if (name.isBlank()) return
        val newPlaylist = Playlist(
            id = UUID.randomUUID().toString(),
            name = name.trim(),
            description = description.trim(),
            songIds = emptyList(),
            createdAt = System.currentTimeMillis()
        )
        val updated = _playlists.value + newPlaylist
        _playlists.value = updated
        viewModelScope.launch {
            repository.savePlaylists(updated)
        }
        _showCreatePlaylistDialog.value = false
    }

    fun deletePlaylist(playlistId: String) {
        val updated = _playlists.value.filter { it.id != playlistId }
        _playlists.value = updated
        if (_selectedPlaylist.value?.id == playlistId) {
            _selectedPlaylist.value = null
        }
        viewModelScope.launch {
            repository.savePlaylists(updated)
        }
    }

    fun addSongToPlaylist(playlistId: String, songId: String) {
        val updated = _playlists.value.map { playlist ->
            if (playlist.id == playlistId) {
                if (!playlist.songIds.contains(songId)) {
                    playlist.copy(songIds = playlist.songIds + songId)
                } else {
                    playlist
                }
            } else {
                playlist
            }
        }
        _playlists.value = updated
        _songToAddToPlaylist.value = null
        viewModelScope.launch {
            repository.savePlaylists(updated)
        }
    }

    fun removeSongFromPlaylist(playlistId: String, songId: String) {
        val updated = _playlists.value.map { playlist ->
            if (playlist.id == playlistId) {
                playlist.copy(songIds = playlist.songIds.filter { it != songId })
            } else {
                playlist
            }
        }
        _playlists.value = updated
        if (_selectedPlaylist.value?.id == playlistId) {
            _selectedPlaylist.value = updated.find { it.id == playlistId }
        }
        viewModelScope.launch {
            repository.savePlaylists(updated)
        }
    }

    fun deleteSong(song: Song) {
        viewModelScope.launch {
            try {
                if (song.uriString.startsWith("content://")) {
                    getApplication<Application>().contentResolver.delete(Uri.parse(song.uriString), null, null)
                } else {
                    val file = File(if (song.uriString.startsWith("file://")) Uri.parse(song.uriString).path ?: "" else song.uriString)
                    if (file.exists()) file.delete()
                }
            } catch (_: Exception) {}

            val updatedSongs = _songs.value.filter { it.id != song.id }
            _songs.value = updatedSongs

            val updatedPlaylists = _playlists.value.map { playlist ->
                playlist.copy(songIds = playlist.songIds.filter { it != song.id })
            }
            _playlists.value = updatedPlaylists

            repository.saveSongs(updatedSongs)
            repository.savePlaylists(updatedPlaylists)
        }
    }

    fun playSongFromList(song: Song, contextList: List<Song>) {
        playbackManager.playSong(song, contextList)
    }

    fun playNext(song: Song) {
        playbackManager.playNext(song)
    }

    fun addToQueue(song: Song) {
        playbackManager.addToQueue(song)
    }

    fun resolveContainerSongs(containerKey: String, rawSongs: List<Song>, explicitSort: ContainerSortMode? = null): Pair<List<Song>, ContainerSortMode> {
        return repository.resolveContainerSongs(containerKey, rawSongs, explicitSort)
    }

    fun reorderContainerSongs(containerKey: String, newSongs: List<Song>) {
        val songIds = newSongs.map { it.id }
        repository.saveCustomOrder(containerKey, songIds)
        repository.saveContainerSort(containerKey, ContainerSortMode.CUSTOM)

        if (containerKey.startsWith("playlist_")) {
            val playlistId = containerKey.removePrefix("playlist_")
            val currentPlaylists = _playlists.value.map { pl ->
                if (pl.id == playlistId) pl.copy(songIds = songIds) else pl
            }
            _playlists.value = currentPlaylists
            viewModelScope.launch {
                repository.savePlaylists(currentPlaylists)
            }
            _selectedPlaylist.value?.let { sel ->
                if (sel.id == playlistId) {
                    _selectedPlaylist.value = sel.copy(songIds = songIds)
                }
            }
        } else if (containerKey.startsWith("folder_")) {
            _selectedFolder.value?.let { sel ->
                _selectedFolder.value = sel.copy(songs = newSongs)
            }
        }

        // Sync queue in full-screen player if playing this container
        if (playbackSnapshot.value.currentSong != null && newSongs.any { it.id == playbackSnapshot.value.currentSong?.id }) {
            playbackManager.updateQueueOrder(newSongs)
        }
    }

    fun setContainerSort(containerKey: String, sortMode: ContainerSortMode) {
        repository.saveContainerSort(containerKey, sortMode)
    }

    fun resetContainerOrder(containerKey: String) {
        repository.clearCustomOrder(containerKey)
        repository.saveContainerSort(containerKey, ContainerSortMode.DEFAULT)
    }

    fun playSelectedPlaylist(playlist: Playlist, shuffle: Boolean = false) {
        val containerKey = "playlist_${playlist.id}"
        val songMap = _songs.value.associateBy { it.id }
        val rawSongs = playlist.songIds.mapNotNull { songMap[it] }
        val playlistSongs = repository.resolveContainerSongs(containerKey, rawSongs).first
        if (playlistSongs.isEmpty()) return

        val queue = if (shuffle) playlistSongs.shuffled() else playlistSongs
        if (shuffle && !playbackSnapshot.value.isShuffle) {
            playbackManager.toggleShuffle()
        }
        playbackManager.playQueue(queue, 0)
    }

    // Video Operations
    fun selectVideo(video: VideoItem?) {
        // If music was playing, pause it so video sound is clear
        if (video != null && playbackSnapshot.value.isPlaying) {
            playbackManager.pause()
        }
        _selectedVideo.value = video
    }

    fun addVideosFromUris(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            _isLoading.value = true
            val newVideos = repository.parseVideosFromUris(uris)
            val updated = _videos.value + newVideos
            _videos.value = updated
            repository.saveVideos(updated)
            _isLoading.value = false
        }
    }

    fun generateDemoVideo() {
        viewModelScope.launch {
            _isLoading.value = true
            val demo = repository.generateDemoVideoIfEmpty()
            val map = _videos.value.associateBy { it.id }.toMutableMap()
            demo.forEach { map[it.id] = it }
            val updated = map.values.toList()
            _videos.value = updated
            repository.saveVideos(updated)
            _isLoading.value = false
        }
    }

    fun saveVideoProgress(videoId: String, positionMs: Long) {
        viewModelScope.launch {
            repository.updateVideoProgress(videoId, positionMs)
            _videos.value = repository.loadVideos()
        }
    }

    fun attachVideoSubtitle(videoId: String, subtitleUri: String?) {
        viewModelScope.launch {
            repository.setVideoSubtitle(videoId, subtitleUri)
            _videos.value = repository.loadVideos()
        }
    }

    fun deleteVideo(videoId: String) {
        val target = _videos.value.find { it.id == videoId }
        val updated = _videos.value.filter { it.id != videoId }
        _videos.value = updated
        viewModelScope.launch {
            target?.let { video ->
                try {
                    if (video.uriString.startsWith("content://")) {
                        getApplication<Application>().contentResolver.delete(Uri.parse(video.uriString), null, null)
                    } else {
                        val file = File(if (video.uriString.startsWith("file://")) Uri.parse(video.uriString).path ?: "" else video.uriString)
                        if (file.exists()) file.delete()
                    }
                } catch (_: Exception) {}
            }
            repository.saveVideos(updated)
        }
    }

    // Queue reorder
    fun moveQueueItem(from: Int, to: Int) {
        playbackManager.moveQueueItem(from, to)
    }

    // Equalizer Passthrough
    fun setEqualizerEnabled(enabled: Boolean) = playbackManager.equalizerManager.setEnabled(enabled)
    fun setBassBoost(level: Int) = playbackManager.equalizerManager.setBassBoost(level)
    fun setVirtualizer(level: Int) = playbackManager.equalizerManager.setVirtualizer(level)
    fun setVolumeNormalization(enabled: Boolean) = playbackManager.equalizerManager.setVolumeNormalization(enabled)
    fun setEqualizerBandLevel(bandIndex: Int, level: Int) = playbackManager.equalizerManager.setBandLevel(bandIndex, level)
    fun selectEqualizerPreset(preset: String) = playbackManager.equalizerManager.selectPreset(preset)

    // Sleep Timer Passthrough
    fun setSleepTimer(minutes: Int) = playbackManager.setSleepTimer(minutes)
    fun cancelSleepTimer() = playbackManager.cancelSleepTimer()

    // Playback Speed & Pitch
    fun setPlaybackSpeedAndPitch(speed: Float, pitch: Float) = playbackManager.setPlaybackSpeedAndPitch(speed, pitch)

    // New Settings Toggles
    fun setFadeInOutEnabled(enabled: Boolean) {
        repository.setFadeInOutEnabled(enabled)
        _appSettings.value = repository.getAppSettings()
    }

    fun setAutoFetchLyricsEnabled(enabled: Boolean) {
        repository.setAutoFetchLyricsEnabled(enabled)
        _appSettings.value = repository.getAppSettings()
    }

    fun setBackgroundVideoAudioOnly(enabled: Boolean) {
        repository.setBackgroundVideoAudioOnly(enabled)
        _appSettings.value = repository.getAppSettings()
    }

    fun setReduceAnimations(enabled: Boolean) {
        repository.setReduceAnimations(enabled)
        _appSettings.value = repository.getAppSettings()
    }

    fun setFetchOnlineCoverArt(enabled: Boolean) {
        repository.setFetchOnlineCoverArt(enabled)
        _appSettings.value = repository.getAppSettings()
    }

    // Visualizer Toggle
    fun setVisualizerEnabled(enabled: Boolean) {
        _isVisualizerEnabled.value = enabled
        repository.setVisualizerEnabled(enabled)
    }

    // Lyrics & A-B Repeat
    fun setSongLyrics(songId: String, uri: Uri) {
        viewModelScope.launch {
            repository.setSongLyrics(songId, uri.toString())
            _songs.value = repository.loadSongs()
        }
    }

    fun setPointA() = playbackManager.setPointA()
    fun setPointB() = playbackManager.setPointB()
    fun clearABRepeat() = playbackManager.clearABRepeat()

    // Backup & Restore
    suspend fun exportBackupJson(): String = repository.exportBackupJson()

    fun importBackupJson(jsonString: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val success = repository.importBackupJson(jsonString)
            if (success) {
                _songs.value = repository.loadSongs()
                _playlists.value = repository.loadPlaylists()
                refreshListeningStats()
            }
            _isLoading.value = false
        }
    }

    fun refreshListeningStats() {
        viewModelScope.launch {
            _listeningStats.value = repository.getListeningStats()
        }
    }

    override fun onCleared() {
        super.onCleared()
        try {
            mediaObserver?.let {
                getApplication<Application>().contentResolver.unregisterContentObserver(it)
            }
        } catch (_: Exception) {}
        playbackManager.release()
    }
}
