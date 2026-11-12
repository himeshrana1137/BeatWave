package com.example.ui

import android.Manifest
import android.os.Build
import android.widget.Toast
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.QueueMusic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.MainTab
import com.example.model.Playlist
import com.example.ui.components.AddToPlaylistBottomSheet
import com.example.ui.components.FullScreenPlayer
import com.example.ui.components.MiniPlayer
import com.example.ui.components.PlaylistsTabScreen
import com.example.ui.components.SongPickerDialog
import com.example.ui.components.SongsTabScreen
import com.example.ui.components.StatsTabScreen
import com.example.ui.theme.PrimaryViolet
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceVariantDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary

@Composable
fun MusicPlayerScreen(
    viewModel: MusicViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val songs by viewModel.songs.collectAsStateWithLifecycle()
    val filteredSongs by viewModel.filteredSongs.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val sortOption by viewModel.sortOption.collectAsStateWithLifecycle()
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()

    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val selectedPlaylist by viewModel.selectedPlaylist.collectAsStateWithLifecycle()

    val selectedSongIds by viewModel.selectedSongIds.collectAsStateWithLifecycle()
    val isInSelectionMode = selectedSongIds.isNotEmpty()

    val playbackSnapshot by viewModel.playbackSnapshot.collectAsStateWithLifecycle()
    val isFullScreenOpen by viewModel.isFullScreenPlayerOpen.collectAsStateWithLifecycle()

    val statsPeriod by viewModel.statsPeriod.collectAsStateWithLifecycle()
    val statsRankingMode by viewModel.statsRankingMode.collectAsStateWithLifecycle()
    val statsOverview by viewModel.statsOverview.collectAsStateWithLifecycle()
    val rankedSongs by viewModel.rankedSongs.collectAsStateWithLifecycle()

    val userFeedbackMessage by viewModel.userFeedbackMessage.collectAsStateWithLifecycle()

    // Dialog state for adding songs to a playlist
    var showAddToPlaylistSheet by remember { mutableStateOf(false) }
    var playlistForSongPicker by remember { mutableStateOf<Playlist?>(null) }

    // User feedback toast/snackbar
    LaunchedEffect(userFeedbackMessage) {
        userFeedbackMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearUserFeedback()
        }
    }

    // Permission launcher for audio files
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.rescanSongs()
    }

    LaunchedEffect(Unit) {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.READ_MEDIA_AUDIO,
                Manifest.permission.POST_NOTIFICATIONS
            )
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        permissionLauncher.launch(permissions)
    }

    // Modal: Add selected songs from Songs Tab to a Playlist
    if (showAddToPlaylistSheet) {
        val selectedSongs = songs.filter { selectedSongIds.contains(it.id) }
        AddToPlaylistBottomSheet(
            playlists = playlists,
            selectedSongs = selectedSongs,
            onDismiss = { showAddToPlaylistSheet = false },
            onPlaylistSelected = { pl ->
                showAddToPlaylistSheet = false
                viewModel.addSelectedSongsToPlaylist(pl.id, pl.name)
            },
            onCreateNewPlaylistAndAdd = { newName ->
                showAddToPlaylistSheet = false
                viewModel.createPlaylist(newName, selectedSongs)
            }
        )
    }

    // Modal: Add songs inside a specific Playlist (Song Picker)
    playlistForSongPicker?.let { pl ->
        SongPickerDialog(
            playlistName = pl.name,
            allSongs = songs,
            alreadyInPlaylistIds = pl.songs.map { it.id }.toSet(),
            onDismiss = { playlistForSongPicker = null },
            onAddSongs = { songsToAdd ->
                viewModel.addSongsToPlaylist(pl.id, songsToAdd)
                playlistForSongPicker = null
            }
        )
    }

    Scaffold(
        modifier = modifier
            .testTag("main_scaffold")
            .fillMaxSize()
            .background(Color(0xFF0B0B10)),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // MiniPlayer (floats immediately above bottom nav if a song is loaded)
                if (playbackSnapshot.currentSong != null && !isFullScreenOpen) {
                    MiniPlayer(
                        snapshot = playbackSnapshot,
                        onExpand = { viewModel.setFullScreenPlayerOpen(true) },
                        onTogglePlayPause = { viewModel.playbackManager.togglePlayPause() },
                        onSkipNext = { viewModel.playbackManager.skipNext() },
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }

                // 3-Tab Bottom Navigation: Songs, Playlists, Stats
                NavigationBar(
                    modifier = Modifier
                        .testTag("bottom_nav_bar")
                        .fillMaxWidth()
                        .navigationBarsPadding(),
                    containerColor = Color(0xFF14141B),
                    tonalElevation = 0.dp
                ) {
                    // Tab 1: Songs
                    NavigationBarItem(
                        selected = currentTab == MainTab.SONGS,
                        onClick = { viewModel.setTab(MainTab.SONGS) },
                        icon = {
                            Icon(
                                imageVector = if (currentTab == MainTab.SONGS) Icons.Default.MusicNote else Icons.Outlined.MusicNote,
                                contentDescription = "Songs",
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        label = { Text("Songs", fontSize = 12.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = PrimaryViolet,
                            selectedTextColor = PrimaryViolet,
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted,
                            indicatorColor = Color.Transparent
                        )
                    )

                    // Tab 2: Playlists
                    NavigationBarItem(
                        selected = currentTab == MainTab.PLAYLISTS,
                        onClick = { viewModel.setTab(MainTab.PLAYLISTS) },
                        icon = {
                            Icon(
                                imageVector = if (currentTab == MainTab.PLAYLISTS) Icons.Default.QueueMusic else Icons.Outlined.QueueMusic,
                                contentDescription = "Playlists",
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        label = { Text("Playlists", fontSize = 12.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = PrimaryViolet,
                            selectedTextColor = PrimaryViolet,
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted,
                            indicatorColor = Color.Transparent
                        )
                    )

                    // Tab 3: Stats
                    NavigationBarItem(
                        selected = currentTab == MainTab.STATS,
                        onClick = { viewModel.setTab(MainTab.STATS) },
                        icon = {
                            Icon(
                                imageVector = if (currentTab == MainTab.STATS) Icons.Default.BarChart else Icons.Outlined.BarChart,
                                contentDescription = "Stats",
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        label = { Text("Stats", fontSize = 12.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = PrimaryViolet,
                            selectedTextColor = PrimaryViolet,
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted,
                            indicatorColor = Color.Transparent
                        )
                    )
                }
            }
        },
        containerColor = Color(0xFF0B0B10)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                MainTab.SONGS -> {
                    SongsTabScreen(
                        songs = filteredSongs,
                        searchQuery = searchQuery,
                        onSearchQueryChange = { viewModel.setSearchQuery(it) },
                        currentSort = sortOption,
                        onSortChange = { viewModel.setSortOption(it) },
                        currentSongId = playbackSnapshot.currentSong?.id,
                        isPlaying = playbackSnapshot.isPlaying,
                        selectedSongIds = selectedSongIds,
                        isInSelectionMode = isInSelectionMode,
                        onSongClick = { song ->
                            if (isInSelectionMode) {
                                viewModel.toggleSongSelection(song.id)
                            } else {
                                viewModel.playSong(song, filteredSongs)
                            }
                        },
                        onSongLongClick = { song ->
                            viewModel.toggleSongSelection(song.id)
                        },
                        onSelectAll = { viewModel.selectAllSongs(filteredSongs) },
                        onExitSelectionMode = { viewModel.clearSelection() },
                        onAddSelectedToPlaylistClick = { showAddToPlaylistSheet = true }
                    )
                }

                MainTab.PLAYLISTS -> {
                    PlaylistsTabScreen(
                        playlists = playlists,
                        selectedPlaylist = selectedPlaylist,
                        currentSongId = playbackSnapshot.currentSong?.id,
                        isPlaying = playbackSnapshot.isPlaying,
                        onSelectPlaylist = { pl -> viewModel.selectPlaylist(pl) },
                        onCreatePlaylist = { name -> viewModel.createPlaylist(name) },
                        onRenamePlaylist = { id, name -> viewModel.renamePlaylist(id, name) },
                        onDeletePlaylist = { id -> viewModel.deletePlaylist(id) },
                        onPlayPlaylist = { pl, startSong, isShuffle ->
                            viewModel.playPlaylist(pl, startSong, isShuffle)
                        },
                        onRemoveSongsFromPlaylist = { plId, toRemove ->
                            viewModel.removeSongsFromPlaylist(plId, toRemove)
                        },
                        onOpenSongPicker = { pl -> playlistForSongPicker = pl }
                    )
                }

                MainTab.STATS -> {
                    StatsTabScreen(
                        overview = statsOverview,
                        rankedSongs = rankedSongs,
                        currentPeriod = statsPeriod,
                        rankingMode = statsRankingMode,
                        onPeriodChange = { viewModel.setStatsPeriod(it) },
                        onRankingModeChange = { viewModel.setStatsRankingMode(it) },
                        onSongClick = { song -> viewModel.playSong(song) },
                        onResetStats = { viewModel.resetStatistics() }
                    )
                }
            }
        }
    }

    // Full Screen Player Animated Overlay
    AnimatedVisibility(
        visible = isFullScreenOpen,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
    ) {
        FullScreenPlayer(
            snapshot = playbackSnapshot,
            onCollapse = { viewModel.setFullScreenPlayerOpen(false) },
            onTogglePlayPause = { viewModel.playbackManager.togglePlayPause() },
            onSkipNext = { viewModel.playbackManager.skipNext() },
            onSkipPrevious = { viewModel.playbackManager.skipPrevious() },
            onSeekTo = { viewModel.playbackManager.seekTo(it) },
            onToggleShuffle = { viewModel.playbackManager.toggleShuffle() },
            onCycleRepeatMode = { viewModel.playbackManager.cycleRepeatMode() }
        )
    }
}
