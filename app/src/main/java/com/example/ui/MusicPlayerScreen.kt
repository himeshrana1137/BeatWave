package com.example.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.MainBottomTab
import com.example.model.NavigationTab
import com.example.ui.components.AddToPlaylistDialog
import com.example.ui.components.CreatePlaylistDialog
import com.example.ui.components.EqualizerScreen
import com.example.ui.components.FullScreenPlayer
import com.example.ui.components.HomeScreen
import com.example.ui.components.LibraryScreen
import com.example.ui.components.MiniPlayer
import com.example.ui.components.SearchScreen
import com.example.ui.components.SettingsScreen
import com.example.ui.components.DuplicateFinderDialog
import com.example.ui.components.FolderBlacklistDialog
import com.example.ui.components.RingtoneCutterDialog
import com.example.ui.components.SongInfoDialog
import com.example.ui.components.VideoListView
import com.example.ui.components.MoodTagDialog
import com.example.ui.components.VaultDialog
import com.example.ui.components.StreamUrlDialog
import com.example.ui.components.BeatWaveWrappedDialog
import com.example.ui.components.AchievementsDialog
import com.example.util.MediaShareHelper
import com.example.ui.theme.PrimaryViolet
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceHighlightDark
import com.example.ui.theme.SurfaceVariantDark
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.video.VideoPlayerView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicPlayerScreen(
    viewModel: MusicViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val songs by viewModel.songs.collectAsStateWithLifecycle()
    val videos by viewModel.videos.collectAsStateWithLifecycle()
    val selectedVideo by viewModel.selectedVideo.collectAsStateWithLifecycle()
    val mainBottomTab by viewModel.mainBottomTab.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val filteredSongs by viewModel.filteredSongs.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val activeTab by viewModel.activeTab.collectAsStateWithLifecycle()
    val selectedPlaylist by viewModel.selectedPlaylist.collectAsStateWithLifecycle()
    val isFullScreenOpen by viewModel.isFullScreenPlayerOpen.collectAsStateWithLifecycle()
    val songToAddToPlaylist by viewModel.songToAddToPlaylist.collectAsStateWithLifecycle()
    val showCreatePlaylistDialog by viewModel.showCreatePlaylistDialog.collectAsStateWithLifecycle()
    val showEqualizer by viewModel.showEqualizer.collectAsStateWithLifecycle()
    val showSettings by viewModel.showSettings.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val playbackSnapshot by viewModel.playbackSnapshot.collectAsStateWithLifecycle()
    val equalizerState by viewModel.equalizerState.collectAsStateWithLifecycle()
    val isVisualizerEnabled by viewModel.isVisualizerEnabled.collectAsStateWithLifecycle()
    val listeningStats by viewModel.listeningStats.collectAsStateWithLifecycle()

    val albums by viewModel.albums.collectAsStateWithLifecycle()
    val artists by viewModel.artists.collectAsStateWithLifecycle()
    val folders by viewModel.folders.collectAsStateWithLifecycle()
    val selectedAlbum by viewModel.selectedAlbum.collectAsStateWithLifecycle()
    val selectedArtist by viewModel.selectedArtist.collectAsStateWithLifecycle()
    val selectedFolder by viewModel.selectedFolder.collectAsStateWithLifecycle()

    val appSettings by viewModel.appSettings.collectAsStateWithLifecycle()
    val selectedSongForInfo by viewModel.selectedSongForInfo.collectAsStateWithLifecycle()
    val songDetailedInfo by viewModel.songDetailedInfo.collectAsStateWithLifecycle()
    val selectedSongForRingtone by viewModel.selectedSongForRingtone.collectAsStateWithLifecycle()
    val showDuplicateFinderDialog by viewModel.showDuplicateFinderDialog.collectAsStateWithLifecycle()
    val duplicateGroups by viewModel.duplicateGroups.collectAsStateWithLifecycle()
    val showFolderBlacklistDialog by viewModel.showFolderBlacklistDialog.collectAsStateWithLifecycle()

    val songForMoodTag by viewModel.songForMoodTag.collectAsStateWithLifecycle()
    val showVaultDialog by viewModel.showVaultDialog.collectAsStateWithLifecycle()
    val showStreamUrlDialog by viewModel.showStreamUrlDialog.collectAsStateWithLifecycle()
    val showWrappedDialog by viewModel.showWrappedDialog.collectAsStateWithLifecycle()
    val showAchievementsDialog by viewModel.showAchievementsDialog.collectAsStateWithLifecycle()
    val audioQualityFilter by viewModel.audioQualityFilter.collectAsStateWithLifecycle()
    val sortOption by viewModel.sortOption.collectAsStateWithLifecycle()

    // Automatic storage permission request on first launch
    val permissionsToRequest = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.READ_MEDIA_AUDIO,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.POST_NOTIFICATIONS
            )
        } else {
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
        }
    }

    val storagePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val anyGranted = permissions.values.any { it }
        if (anyGranted) {
            viewModel.scanDeviceMedia()
        }
    }

    LaunchedEffect(Unit) {
        storagePermissionLauncher.launch(permissionsToRequest)
    }

    // Audio file picker launcher
    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            uris.forEach { uri ->
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: Exception) {}
            }
            viewModel.addSongsFromUris(uris)
        }
    }

    // Video file picker launcher
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            uris.forEach { uri ->
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: Exception) {}
            }
            viewModel.addVideosFromUris(uris)
        }
    }

    // If video is selected, show built-in Video Player
    selectedVideo?.let { video ->
        VideoPlayerView(
            video = video,
            onClose = { viewModel.selectVideo(null) },
            onSaveProgress = { id, pos -> viewModel.saveVideoProgress(id, pos) },
            onAttachSubtitle = { id, sub -> viewModel.attachVideoSubtitle(id, sub) }
        )
        return
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                ) {
                    // Mini-Player Docked above bottom navigation if music is playing
                    if (playbackSnapshot.currentSong != null) {
                        MiniPlayer(
                            snapshot = playbackSnapshot,
                            showQualityBadges = appSettings.showQualityBadges,
                            onExpand = { viewModel.setFullScreenPlayerOpen(true) },
                            onTogglePlayPause = { viewModel.playbackManager.togglePlayPause() },
                            onSkipNext = { viewModel.playbackManager.skipNext() },
                            onToggleFavorite = { viewModel.toggleFavorite(it) }
                        )
                    }

                    // Modern Spotify-style Bottom Navigation Bar
                    NavigationBar(
                        containerColor = SurfaceDark,
                        contentColor = TextPrimary,
                        tonalElevation = 6.dp,
                        modifier = Modifier.testTag("main_bottom_nav")
                    ) {
                        NavigationBarItem(
                            selected = mainBottomTab == MainBottomTab.HOME,
                            onClick = { viewModel.setMainBottomTab(MainBottomTab.HOME) },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Home,
                                    contentDescription = "Home"
                                )
                            },
                            label = { Text("Home", fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = PrimaryViolet,
                                selectedTextColor = PrimaryViolet,
                                unselectedIconColor = TextSecondary,
                                unselectedTextColor = TextSecondary,
                                indicatorColor = SurfaceVariantDark
                            ),
                            modifier = Modifier.testTag("bottom_tab_home")
                        )

                        NavigationBarItem(
                            selected = mainBottomTab == MainBottomTab.SEARCH,
                            onClick = { viewModel.setMainBottomTab(MainBottomTab.SEARCH) },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search"
                                )
                            },
                            label = { Text("Search", fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = PrimaryViolet,
                                selectedTextColor = PrimaryViolet,
                                unselectedIconColor = TextSecondary,
                                unselectedTextColor = TextSecondary,
                                indicatorColor = SurfaceVariantDark
                            ),
                            modifier = Modifier.testTag("bottom_tab_search")
                        )

                        NavigationBarItem(
                            selected = mainBottomTab == MainBottomTab.LIBRARY,
                            onClick = { viewModel.setMainBottomTab(MainBottomTab.LIBRARY) },
                            icon = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                                    contentDescription = "Library"
                                )
                            },
                            label = { Text("Library", fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = PrimaryViolet,
                                selectedTextColor = PrimaryViolet,
                                unselectedIconColor = TextSecondary,
                                unselectedTextColor = TextSecondary,
                                indicatorColor = SurfaceVariantDark
                            ),
                            modifier = Modifier.testTag("bottom_tab_library")
                        )

                        NavigationBarItem(
                            selected = mainBottomTab == MainBottomTab.VIDEOS,
                            onClick = { viewModel.setMainBottomTab(MainBottomTab.VIDEOS) },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Videocam,
                                    contentDescription = "Videos"
                                )
                            },
                            label = { Text("Videos", fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = PrimaryViolet,
                                selectedTextColor = PrimaryViolet,
                                unselectedIconColor = TextSecondary,
                                unselectedTextColor = TextSecondary,
                                indicatorColor = SurfaceVariantDark
                            ),
                            modifier = Modifier.testTag("bottom_tab_videos")
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .statusBarsPadding()
            ) {
                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = PrimaryViolet)
                    }
                } else {
                    when (mainBottomTab) {
                        MainBottomTab.HOME -> {
                            HomeScreen(
                                songs = songs,
                                albums = albums,
                                videos = videos,
                                currentSongId = playbackSnapshot.currentSong?.id,
                                isPlaying = playbackSnapshot.isPlaying,
                                showQualityBadges = appSettings.showQualityBadges,
                                onSongClick = { song, list ->
                                    viewModel.playSongFromList(song, list)
                                },
                                onAlbumClick = {
                                    viewModel.selectAlbum(it)
                                    viewModel.setActiveTab(NavigationTab.ALBUMS)
                                    viewModel.setMainBottomTab(MainBottomTab.LIBRARY)
                                },
                                onVideoClick = { viewModel.selectVideo(it) },
                                onScanMedia = { viewModel.scanDeviceMedia() },
                                onAddSongs = {
                                    audioPickerLauncher.launch(
                                        arrayOf("audio/*", "application/ogg", "application/x-flac")
                                    )
                                },
                                onDemoTracks = { viewModel.generateSampleTracks() },
                                onOpenEqualizer = { viewModel.setShowEqualizer(true) },
                                onOpenSettings = { viewModel.setShowSettings(true) },
                                onOpenWrapped = { viewModel.openWrappedDialog() },
                                onOpenAchievements = { viewModel.openAchievementsDialog() },
                                onSearchClick = { viewModel.setMainBottomTab(MainBottomTab.SEARCH) },
                                autoScanEnabled = appSettings.autoScanEnabled
                            )
                        }

                        MainBottomTab.SEARCH -> {
                            SearchScreen(
                                searchQuery = searchQuery,
                                onSearchQueryChange = { viewModel.onSearchQueryChange(it) },
                                filteredSongs = filteredSongs,
                                allSongs = songs,
                                currentSongId = playbackSnapshot.currentSong?.id,
                                isPlaying = playbackSnapshot.isPlaying,
                                onSongClick = { song, list ->
                                    viewModel.playSongFromList(song, list)
                                },
                                onToggleFavorite = { viewModel.toggleFavorite(it) },
                                onAddToPlaylist = { viewModel.setSongToAddToPlaylist(it) },
                                onDeleteSong = { viewModel.deleteSong(it) },
                                onCategoryClick = { tab ->
                                    viewModel.setActiveTab(tab)
                                    viewModel.setMainBottomTab(MainBottomTab.LIBRARY)
                                },
                                onSongInfo = { viewModel.openSongInfo(it) },
                                onShareSong = { MediaShareHelper.shareSong(context, it) },
                                onCutRingtone = { viewModel.openRingtoneCutter(it) },
                                onPlayNext = { viewModel.playNext(it) },
                                onAddToQueue = { viewModel.addToQueue(it) },
                                showQualityBadges = appSettings.showQualityBadges,
                                reduceAnimations = appSettings.reduceAnimations
                            )
                        }

                        MainBottomTab.LIBRARY -> {
                            LibraryScreen(
                                activeTab = activeTab,
                                onTabSelected = { viewModel.setActiveTab(it) },
                                songs = songs,
                                albums = albums,
                                artists = artists,
                                folders = folders,
                                playlists = playlists,
                                selectedPlaylist = selectedPlaylist,
                                selectedAlbum = selectedAlbum,
                                selectedArtist = selectedArtist,
                                selectedFolder = selectedFolder,
                                currentSongId = playbackSnapshot.currentSong?.id,
                                isPlaying = playbackSnapshot.isPlaying,
                                showQualityBadges = appSettings.showQualityBadges,
                                audioQualityFilter = audioQualityFilter,
                                onAudioQualityFilterSelected = { viewModel.setAudioQualityFilter(it) },
                                sortOption = sortOption,
                                onSortOptionSelected = { viewModel.setSortOption(it) },
                                onSelectPlaylist = { viewModel.selectPlaylist(it) },
                                onSelectAlbum = { viewModel.selectAlbum(it) },
                                onSelectArtist = { viewModel.selectArtist(it) },
                                onSelectFolder = { viewModel.selectFolder(it) },
                                onCreatePlaylistClick = { viewModel.setShowCreatePlaylistDialog(true) },
                                onDeletePlaylist = { viewModel.deletePlaylist(it) },
                                onPlayPlaylist = { pl, shuffle -> viewModel.playSelectedPlaylist(pl, shuffle) },
                                onRemoveSongFromPlaylist = { plId, sId -> viewModel.removeSongFromPlaylist(plId, sId) },
                                onSongClick = { song, list -> viewModel.playSongFromList(song, list) },
                                onToggleFavorite = { viewModel.toggleFavorite(it) },
                                onAddToPlaylist = { viewModel.setSongToAddToPlaylist(it) },
                                onDeleteSong = { viewModel.deleteSong(it) },
                                onPlayGroup = { list, idx -> viewModel.playGroup(list, idx) },
                                onShuffleGroup = { list -> viewModel.shuffleGroup(list) },
                                onSongInfo = { viewModel.openSongInfo(it) },
                                onShareSong = { MediaShareHelper.shareSong(context, it) },
                                onCutRingtone = { viewModel.openRingtoneCutter(it) },
                                onPlayNext = { viewModel.playNext(it) },
                                onAddToQueue = { viewModel.addToQueue(it) },
                                onReorderPlaylistSongs = { id, list -> viewModel.reorderContainerSongs("playlist_$id", list) },
                                onReorderFolderSongs = { key, list -> viewModel.reorderContainerSongs("folder_$key", list) },
                                reduceAnimations = appSettings.reduceAnimations
                            )
                        }

                        MainBottomTab.VIDEOS -> {
                            VideoListView(
                                videos = videos,
                                showQualityBadges = appSettings.showQualityBadges,
                                onVideoClick = { viewModel.selectVideo(it) },
                                onAddVideosClick = {
                                    videoPickerLauncher.launch(
                                        arrayOf("video/mp4", "video/webm", "video/*")
                                    )
                                },
                                onLoadDemoVideo = { viewModel.generateDemoVideo() },
                                onDeleteVideo = { viewModel.deleteVideo(it) },
                                onShareVideo = { MediaShareHelper.shareVideo(context, it) },
                                onOpenStreamUrl = { viewModel.openStreamUrlDialog() },
                                onOpenVault = { viewModel.openVaultDialog() }
                            )
                        }
                    }
                }
            }
        }

        // Animated Full-Screen Player Overlay
        AnimatedVisibility(
            visible = isFullScreenOpen && playbackSnapshot.currentSong != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            FullScreenPlayer(
                snapshot = playbackSnapshot,
                showQualityBadges = appSettings.showQualityBadges,
                onCollapse = { viewModel.setFullScreenPlayerOpen(false) },
                onTogglePlayPause = { viewModel.playbackManager.togglePlayPause() },
                onSkipNext = { viewModel.playbackManager.skipNext() },
                onSkipPrevious = { viewModel.playbackManager.skipPrevious() },
                onSeekTo = { viewModel.playbackManager.seekTo(it) },
                onToggleShuffle = { viewModel.playbackManager.toggleShuffle() },
                onCycleRepeat = { viewModel.playbackManager.cycleRepeatMode() },
                onToggleFavorite = { viewModel.toggleFavorite(it) },
                onAddToPlaylist = { viewModel.setSongToAddToPlaylist(it) },
                onSelectQueueSong = { viewModel.playbackManager.playSong(it) },
                onMoveQueueItem = { from, to -> viewModel.moveQueueItem(from, to) },
                onSetPointA = { viewModel.setPointA() },
                onSetPointB = { viewModel.setPointB() },
                onClearABRepeat = { viewModel.clearABRepeat() },
                onAttachLyrics = { songId, uri -> viewModel.setSongLyrics(songId, uri) },
                isVisualizerEnabled = isVisualizerEnabled,
                onSongInfo = { viewModel.openSongInfo(it) },
                onShareSong = { MediaShareHelper.shareSong(context, it) },
                onCutRingtone = { viewModel.openRingtoneCutter(it) },
                onSetPlaybackSpeedAndPitch = { speed, pitch -> viewModel.setPlaybackSpeedAndPitch(speed, pitch) },
                onTagMood = { viewModel.openMoodTagDialog(it) }
            )
        }

        // Equalizer Full Overlay with Back Handler
        if (showEqualizer) {
            BackHandler { viewModel.setShowEqualizer(false) }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { viewModel.setShowEqualizer(false) },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = TextPrimary
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Audio Equalizer & FX",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                    }
                    EqualizerScreen(
                        equalizerState = equalizerState,
                        onSetEnabled = { viewModel.setEqualizerEnabled(it) },
                        onSetBassBoost = { viewModel.setBassBoost(it) },
                        onSetVirtualizer = { viewModel.setVirtualizer(it) },
                        onToggleVolumeNormalization = { viewModel.setVolumeNormalization(it) },
                        onSetBandLevel = { idx, lvl -> viewModel.setEqualizerBandLevel(idx, lvl) },
                        onSelectPreset = { viewModel.selectEqualizerPreset(it) },
                        sleepTimerSecondsRemaining = playbackSnapshot.sleepTimerRemainingSeconds,
                        onSetSleepTimer = { viewModel.setSleepTimer(it) },
                        onCancelSleepTimer = { viewModel.cancelSleepTimer() }
                    )
                }
            }
        }

        // Settings Full Overlay with Back Handler
        if (showSettings) {
            BackHandler { viewModel.setShowSettings(false) }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { viewModel.setShowSettings(false) },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = TextPrimary
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Settings & Themes",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                    }
                    SettingsScreen(
                        currentTheme = themeMode,
                        onSelectTheme = { viewModel.setThemeMode(it) },
                        isVisualizerEnabled = isVisualizerEnabled,
                        onToggleVisualizer = { viewModel.setVisualizerEnabled(it) },
                        appSettings = appSettings,
                        onToggleAutoPause = { viewModel.setAutoPauseOnCallAndUnplug(it) },
                        onToggleHeadsetDoublePress = { viewModel.setHeadsetDoublePressEnabled(it) },
                        onToggleShakeToSkip = { viewModel.setShakeToSkipEnabled(it) },
                        onToggleFilterShortAudio = { viewModel.setFilterShortAudioEnabled(it) },
                        onSetMinAudioDuration = { viewModel.setMinAudioDurationSec(it) },
                        onOpenFolderBlacklist = { viewModel.openFolderBlacklist() },
                        onOpenDuplicateFinder = { viewModel.openDuplicateFinder() },
                        onOpenRingtoneCutter = {
                            val target = playbackSnapshot.currentSong ?: songs.firstOrNull()
                            if (target != null) {
                                viewModel.openRingtoneCutter(target)
                            } else {
                                android.widget.Toast.makeText(context, "No songs available to trim", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        },
                        listeningStats = listeningStats,
                        songCount = songs.size,
                        videoCount = videos.size,
                        playlistCount = playlists.size,
                        onExportBackup = { viewModel.exportBackupJson() },
                        onImportBackup = { viewModel.importBackupJson(it) },
                        onToggleFadeInOut = { viewModel.setFadeInOutEnabled(it) },
                        onToggleAutoFetchLyrics = { viewModel.setAutoFetchLyricsEnabled(it) },
                        onToggleBackgroundVideoAudioOnly = { viewModel.setBackgroundVideoAudioOnly(it) },
                        onToggleReduceAnimations = { viewModel.setReduceAnimations(it) },
                        onToggleFetchOnlineCoverArt = { viewModel.setFetchOnlineCoverArt(it) },
                        onToggleShowQualityBadges = { viewModel.setShowQualityBadges(it) },
                        onOpenWrapped = { viewModel.openWrappedDialog() },
                        onOpenAchievements = { viewModel.openAchievementsDialog() }
                    )
                }
            }
        }

        // Dialogs
        if (showCreatePlaylistDialog) {
            CreatePlaylistDialog(
                onDismiss = { viewModel.setShowCreatePlaylistDialog(false) },
                onCreate = { name, desc -> viewModel.createPlaylist(name, desc) }
            )
        }

        songToAddToPlaylist?.let { song ->
            AddToPlaylistDialog(
                song = song,
                playlists = playlists,
                onDismiss = { viewModel.setSongToAddToPlaylist(null) },
                onPlaylistSelected = { playlistId ->
                    viewModel.addSongToPlaylist(playlistId, song.id)
                    viewModel.setSongToAddToPlaylist(null)
                }
            )
        }

        // Song Info Dialog
        selectedSongForInfo?.let { song ->
            SongInfoDialog(
                song = song,
                info = songDetailedInfo,
                onDismiss = { viewModel.closeSongInfo() }
            )
        }

        // Ringtone Cutter Dialog
        selectedSongForRingtone?.let { song ->
            RingtoneCutterDialog(
                song = song,
                onDismiss = { viewModel.closeRingtoneCutter() }
            )
        }

        // Duplicate Song Finder Dialog
        if (showDuplicateFinderDialog) {
            DuplicateFinderDialog(
                duplicateGroups = duplicateGroups,
                onPlaySong = { viewModel.playbackManager.playSong(it) },
                onDeleteSong = { viewModel.deleteSong(it) },
                onAutoCleanGroup = { viewModel.autoCleanDuplicateGroup(it) },
                onDismiss = { viewModel.closeDuplicateFinder() }
            )
        }

        // Folder Blacklist Dialog
        if (showFolderBlacklistDialog) {
            FolderBlacklistDialog(
                blacklistedFolders = appSettings.blacklistedFolders,
                detectedFolders = folders.map { it.path }.filter { it.isNotBlank() },
                onAddBlacklistFolder = { viewModel.addBlacklistedFolder(it) },
                onRemoveBlacklistFolder = { viewModel.removeBlacklistedFolder(it) },
                onDismiss = { viewModel.closeFolderBlacklist() }
            )
        }

        // Feature 12: Mood Tag Dialog
        songForMoodTag?.let { song ->
            MoodTagDialog(
                song = song,
                onSaveMoods = { moods -> viewModel.setSongMoods(song.id, moods) },
                onDismiss = { viewModel.closeMoodTagDialog() }
            )
        }

        // Feature 10: Private Video Vault Dialog
        if (showVaultDialog) {
            VaultDialog(
                vaultManager = viewModel.vaultManager,
                onPlayVideo = { video ->
                    viewModel.selectVideo(video)
                    viewModel.closeVaultDialog()
                },
                onDismiss = { viewModel.closeVaultDialog() }
            )
        }

        // Feature 9: Network Stream Player Dialog
        if (showStreamUrlDialog) {
            StreamUrlDialog(
                onPlayUrl = { url, title ->
                    viewModel.playNetworkStream(url, title)
                    viewModel.closeStreamUrlDialog()
                },
                onDismiss = { viewModel.closeStreamUrlDialog() }
            )
        }

        // Feature 11: BeatWave Wrapped Dialog
        if (showWrappedDialog) {
            BeatWaveWrappedDialog(
                stats = listeningStats,
                allSongs = songs,
                onDismiss = { viewModel.closeWrappedDialog() }
            )
        }

        // Feature 14: Achievements & Badges Dialog
        if (showAchievementsDialog) {
            AchievementsDialog(
                achievements = viewModel.achievementManager.getAchievements(listeningStats),
                onDismiss = { viewModel.closeAchievementsDialog() }
            )
        }
    }
}
