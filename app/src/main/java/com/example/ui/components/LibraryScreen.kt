package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.example.model.AudioQualityFilter
import com.example.model.ContainerSortMode
import com.example.model.SortOption
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AlbumItem
import com.example.model.ArtistItem
import com.example.model.FolderItem
import com.example.model.NavigationTab
import com.example.model.Playlist
import com.example.model.Song
import com.example.ui.theme.CardBorder
import com.example.ui.theme.PrimaryNeon
import com.example.ui.theme.PrimaryViolet
import com.example.ui.theme.SecondaryAccent
import com.example.ui.theme.SecondaryCyan
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceHighlightDark
import com.example.ui.theme.SurfaceVariantDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun LibraryScreen(
    activeTab: NavigationTab,
    onTabSelected: (NavigationTab) -> Unit,
    songs: List<Song>,
    albums: List<AlbumItem>,
    artists: List<ArtistItem>,
    folders: List<FolderItem>,
    playlists: List<Playlist>,
    selectedPlaylist: Playlist?,
    selectedAlbum: AlbumItem?,
    selectedArtist: ArtistItem?,
    selectedFolder: FolderItem?,
    currentSongId: String?,
    isPlaying: Boolean,
    onSelectPlaylist: (Playlist?) -> Unit,
    onSelectAlbum: (AlbumItem?) -> Unit,
    onSelectArtist: (ArtistItem?) -> Unit,
    onSelectFolder: (FolderItem?) -> Unit,
    onCreatePlaylistClick: () -> Unit,
    onDeletePlaylist: (String) -> Unit,
    onPlayPlaylist: (Playlist, Boolean) -> Unit,
    onRemoveSongFromPlaylist: (String, String) -> Unit,
    onSongClick: (Song, List<Song>) -> Unit,
    onToggleFavorite: (Song) -> Unit,
    onAddToPlaylist: (Song) -> Unit,
    onDeleteSong: (Song) -> Unit,
    onPlayGroup: (List<Song>, Int) -> Unit,
    onShuffleGroup: (List<Song>) -> Unit,
    onSongInfo: ((Song) -> Unit)? = null,
    onShareSong: ((Song) -> Unit)? = null,
    onCutRingtone: ((Song) -> Unit)? = null,
    showQualityBadges: Boolean = true,
    audioQualityFilter: AudioQualityFilter = AudioQualityFilter.ALL,
    onAudioQualityFilterSelected: (AudioQualityFilter) -> Unit = {},
    sortOption: SortOption = SortOption.DATE_ADDED_DESC,
    onSortOptionSelected: (SortOption) -> Unit = {},
    onPlayNext: ((Song) -> Unit)? = null,
    onAddToQueue: ((Song) -> Unit)? = null,
    onReorderPlaylistSongs: ((playlistId: String, newSongs: List<Song>) -> Unit)? = null,
    onReorderFolderSongs: ((folderKey: String, newSongs: List<Song>) -> Unit)? = null,
    reduceAnimations: Boolean = false,
    modifier: Modifier = Modifier
) {
    // Intercept back presses when inside a detail view
    if (selectedPlaylist != null || selectedAlbum != null || selectedArtist != null || selectedFolder != null) {
        BackHandler {
            when {
                selectedPlaylist != null -> onSelectPlaylist(null)
                selectedAlbum != null -> onSelectAlbum(null)
                selectedArtist != null -> onSelectArtist(null)
                selectedFolder != null -> onSelectFolder(null)
            }
        }
    }

    Column(
        modifier = modifier
            .testTag("library_screen")
            .fillMaxSize()
    ) {
        // Only show Header and Chips if NOT in a detail view
        if (selectedPlaylist == null && selectedAlbum == null && selectedArtist == null && selectedFolder == null) {
            // Header: Title & Create Playlist (+) Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Your Library",
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontSize = 28.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                )

                IconButton(
                    onClick = onCreatePlaylistClick,
                    modifier = Modifier
                        .testTag("create_playlist_header_button")
                        .minimumInteractiveComponentSize()
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(SurfaceVariantDark)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Create Playlist",
                        tint = TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Filter Chips Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LibraryFilterChip(
                    title = "Playlists",
                    isSelected = activeTab == NavigationTab.PLAYLISTS,
                    onClick = { onTabSelected(NavigationTab.PLAYLISTS) }
                )
                LibraryFilterChip(
                    title = "Albums",
                    isSelected = activeTab == NavigationTab.ALBUMS,
                    onClick = { onTabSelected(NavigationTab.ALBUMS) }
                )
                LibraryFilterChip(
                    title = "Artists",
                    isSelected = activeTab == NavigationTab.ARTISTS,
                    onClick = { onTabSelected(NavigationTab.ARTISTS) }
                )
                LibraryFilterChip(
                    title = "Folders",
                    isSelected = activeTab == NavigationTab.FOLDERS,
                    onClick = { onTabSelected(NavigationTab.FOLDERS) }
                )
                LibraryFilterChip(
                    title = "Songs",
                    isSelected = activeTab == NavigationTab.SONGS,
                    onClick = { onTabSelected(NavigationTab.SONGS) }
                )
                LibraryFilterChip(
                    title = "Favorites",
                    isSelected = activeTab == NavigationTab.FAVORITES,
                    onClick = { onTabSelected(NavigationTab.FAVORITES) }
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
        }

        // Body Content based on active category / detail view
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                // Selected Album Detail View
                selectedAlbum != null -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        GroupDetailHeader(
                            title = selectedAlbum.title,
                            subtitle = "${selectedAlbum.artist} • ${selectedAlbum.songCount} tracks",
                            artPath = selectedAlbum.albumArtPath,
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Album,
                                    contentDescription = null,
                                    tint = PrimaryViolet
                                )
                            },
                            songs = selectedAlbum.songs,
                            onBack = { onSelectAlbum(null) },
                            onPlayAll = { onPlayGroup(selectedAlbum.songs, 0) },
                            onShuffle = { onShuffleGroup(selectedAlbum.songs) }
                        )
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 120.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(selectedAlbum.songs, key = { it.id }) { song ->
                                val isCurrent = song.id == currentSongId
                                SongItemCard(
                                    song = song,
                                    isCurrent = isCurrent,
                                    isPlaying = isCurrent && isPlaying,
                                    onSongClick = { onSongClick(song, selectedAlbum.songs) },
                                    onToggleFavorite = { onToggleFavorite(song) },
                                    onAddToPlaylist = { onAddToPlaylist(song) },
                                    onDeleteSong = { onDeleteSong(song) },
                                    onPlayNext = onPlayNext,
                                    onAddToQueue = onAddToQueue,
                                    onSongInfo = onSongInfo,
                                    onShareSong = onShareSong,
                                    onCutRingtone = onCutRingtone,
                                    showQualityBadges = showQualityBadges,
                                    reduceAnimations = reduceAnimations
                                )
                            }
                        }
                    }
                }

                // Selected Artist Detail View
                selectedArtist != null -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        GroupDetailHeader(
                            title = selectedArtist.name,
                            subtitle = "${selectedArtist.songCount} tracks • ${selectedArtist.albumCount} albums",
                            artPath = null,
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = PrimaryViolet
                                )
                            },
                            songs = selectedArtist.songs,
                            onBack = { onSelectArtist(null) },
                            onPlayAll = { onPlayGroup(selectedArtist.songs, 0) },
                            onShuffle = { onShuffleGroup(selectedArtist.songs) }
                        )
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 120.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(selectedArtist.songs, key = { it.id }) { song ->
                                val isCurrent = song.id == currentSongId
                                SongItemCard(
                                    song = song,
                                    isCurrent = isCurrent,
                                    isPlaying = isCurrent && isPlaying,
                                    onSongClick = { onSongClick(song, selectedArtist.songs) },
                                    onToggleFavorite = { onToggleFavorite(song) },
                                    onAddToPlaylist = { onAddToPlaylist(song) },
                                    onDeleteSong = { onDeleteSong(song) },
                                    onPlayNext = onPlayNext,
                                    onAddToQueue = onAddToQueue,
                                    onSongInfo = onSongInfo,
                                    onShareSong = onShareSong,
                                    onCutRingtone = onCutRingtone,
                                    showQualityBadges = showQualityBadges,
                                    reduceAnimations = reduceAnimations
                                )
                            }
                        }
                    }
                }

                // Selected Folder Detail View
                selectedFolder != null -> {
                    val context = LocalContext.current
                    val repository = remember { com.example.data.MusicRepository(context) }
                    val folderKey = selectedFolder.path.ifBlank { selectedFolder.name }
                    val containerKey = "folder_$folderKey"

                    var sortMode by remember(folderKey) {
                        mutableStateOf(repository.getContainerSort(containerKey))
                    }

                    var displayedSongs by remember(folderKey, selectedFolder.songs, sortMode) {
                        mutableStateOf(repository.resolveContainerSongs(containerKey, selectedFolder.songs, sortMode).first)
                    }

                    var sortMenuExpanded by remember { mutableStateOf(false) }
                    val folderListState = rememberLazyListState()
                    val isFolderHeaderCollapsed by remember {
                        derivedStateOf {
                            folderListState.firstVisibleItemIndex > 0 || folderListState.firstVisibleItemScrollOffset > 30
                        }
                    }

                    Column(modifier = Modifier.fillMaxSize()) {
                        GroupDetailHeader(
                            title = selectedFolder.name,
                            subtitle = if (selectedFolder.path.isNotBlank()) selectedFolder.path else "${displayedSongs.size} tracks",
                            artPath = null,
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Folder,
                                    contentDescription = null,
                                    tint = PrimaryViolet
                                )
                            },
                            songs = displayedSongs,
                            isCollapsed = isFolderHeaderCollapsed,
                            onBack = { onSelectFolder(null) },
                            onPlayAll = { onPlayGroup(displayedSongs, 0) },
                            onShuffle = { onShuffleGroup(displayedSongs) }
                        )

                        // Sort & Reorder Bar for Folder
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (sortMode == ContainerSortMode.CUSTOM) "Custom Order (Drag ≡)" else sortMode.displayName,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = if (sortMode == ContainerSortMode.CUSTOM) PrimaryNeon else SecondaryCyan,
                                    fontWeight = FontWeight.Bold
                                )
                            )

                            Box {
                                Button(
                                    onClick = { sortMenuExpanded = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceHighlightDark),
                                    shape = RoundedCornerShape(20.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Sort,
                                        contentDescription = "Sort folder songs",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Sort",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.White
                                    )
                                }

                                DropdownMenu(
                                    expanded = sortMenuExpanded,
                                    onDismissRequest = { sortMenuExpanded = false },
                                    modifier = Modifier.background(SurfaceHighlightDark)
                                ) {
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                "Custom Order ≡",
                                                color = if (sortMode == ContainerSortMode.CUSTOM) PrimaryNeon else TextPrimary,
                                                fontWeight = if (sortMode == ContainerSortMode.CUSTOM) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        onClick = {
                                            sortMode = ContainerSortMode.CUSTOM
                                            repository.saveContainerSort(containerKey, ContainerSortMode.CUSTOM)
                                            displayedSongs = repository.resolveContainerSongs(containerKey, selectedFolder.songs, ContainerSortMode.CUSTOM).first
                                            sortMenuExpanded = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                "Title (A-Z)",
                                                color = if (sortMode == ContainerSortMode.TITLE) PrimaryNeon else TextPrimary
                                            )
                                        },
                                        onClick = {
                                            sortMode = ContainerSortMode.TITLE
                                            repository.saveContainerSort(containerKey, ContainerSortMode.TITLE)
                                            displayedSongs = repository.resolveContainerSongs(containerKey, selectedFolder.songs, ContainerSortMode.TITLE).first
                                            sortMenuExpanded = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                "Artist (A-Z)",
                                                color = if (sortMode == ContainerSortMode.ARTIST) PrimaryNeon else TextPrimary
                                            )
                                        },
                                        onClick = {
                                            sortMode = ContainerSortMode.ARTIST
                                            repository.saveContainerSort(containerKey, ContainerSortMode.ARTIST)
                                            displayedSongs = repository.resolveContainerSongs(containerKey, selectedFolder.songs, ContainerSortMode.ARTIST).first
                                            sortMenuExpanded = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                "Date Added",
                                                color = if (sortMode == ContainerSortMode.DATE) PrimaryNeon else TextPrimary
                                            )
                                        },
                                        onClick = {
                                            sortMode = ContainerSortMode.DATE
                                            repository.saveContainerSort(containerKey, ContainerSortMode.DATE)
                                            displayedSongs = repository.resolveContainerSongs(containerKey, selectedFolder.songs, ContainerSortMode.DATE).first
                                            sortMenuExpanded = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                "Duration",
                                                color = if (sortMode == ContainerSortMode.DURATION) PrimaryNeon else TextPrimary
                                            )
                                        },
                                        onClick = {
                                            sortMode = ContainerSortMode.DURATION
                                            repository.saveContainerSort(containerKey, ContainerSortMode.DURATION)
                                            displayedSongs = repository.resolveContainerSongs(containerKey, selectedFolder.songs, ContainerSortMode.DURATION).first
                                            sortMenuExpanded = false
                                        }
                                    )
                                    HorizontalDivider(color = CardBorder)
                                    DropdownMenuItem(
                                        text = { Text("Reset Order", color = TextSecondary) },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.RestartAlt,
                                                contentDescription = null,
                                                tint = TextSecondary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        },
                                        onClick = {
                                            repository.clearCustomOrder(containerKey)
                                            repository.saveContainerSort(containerKey, ContainerSortMode.DEFAULT)
                                            sortMode = ContainerSortMode.DEFAULT
                                            displayedSongs = repository.resolveContainerSongs(containerKey, selectedFolder.songs, ContainerSortMode.DEFAULT).first
                                            sortMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        ReorderableSongList(
                            songs = displayedSongs,
                            isCustomOrder = sortMode == ContainerSortMode.CUSTOM,
                            currentSongId = currentSongId,
                            isPlaying = isPlaying,
                            onSongClick = { onSongClick(it, displayedSongs) },
                            onToggleFavorite = onToggleFavorite,
                            onAddToPlaylist = onAddToPlaylist,
                            onDeleteSong = onDeleteSong,
                            onPlayNext = { onPlayNext?.invoke(it) },
                            onAddToQueue = { onAddToQueue?.invoke(it) },
                            onReorder = { fromIdx, toIdx ->
                                if (fromIdx in displayedSongs.indices && toIdx in displayedSongs.indices) {
                                    val mutable = displayedSongs.toMutableList()
                                    val item = mutable.removeAt(fromIdx)
                                    mutable.add(toIdx, item)
                                    displayedSongs = mutable
                                    sortMode = ContainerSortMode.CUSTOM
                                    repository.saveCustomOrder(containerKey, mutable.map { it.id })
                                    repository.saveContainerSort(containerKey, ContainerSortMode.CUSTOM)
                                    onReorderFolderSongs?.invoke(folderKey, mutable)
                                }
                            },
                            onSongInfo = onSongInfo,
                            onShareSong = onShareSong,
                            onCutRingtone = onCutRingtone,
                            showQualityBadges = showQualityBadges,
                            reduceAnimations = reduceAnimations,
                            listState = folderListState,
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        )
                    }
                }

                // Main Views according to Active Filter Chip
                activeTab == NavigationTab.PLAYLISTS -> {
                    PlaylistsView(
                        playlists = playlists,
                        allSongs = songs,
                        selectedPlaylist = selectedPlaylist,
                        currentSongId = currentSongId,
                        isPlaying = isPlaying,
                        onSelectPlaylist = onSelectPlaylist,
                        onCreatePlaylistClick = onCreatePlaylistClick,
                        onDeletePlaylist = onDeletePlaylist,
                        onPlayPlaylist = onPlayPlaylist,
                        onSongClick = { onSongClick(it, songs) },
                        onToggleFavorite = onToggleFavorite,
                        onRemoveSongFromPlaylist = onRemoveSongFromPlaylist,
                        onReorderPlaylistSongs = onReorderPlaylistSongs,
                        onPlayNext = onPlayNext,
                        onAddToQueue = onAddToQueue,
                        onSongInfo = onSongInfo,
                        onShareSong = onShareSong,
                        onCutRingtone = onCutRingtone,
                        showQualityBadges = showQualityBadges,
                        reduceAnimations = reduceAnimations
                    )
                }

                activeTab == NavigationTab.ALBUMS -> {
                    AlbumsGridView(
                        albums = albums,
                        onAlbumClick = onSelectAlbum,
                        onPlayAlbum = { onPlayGroup(it.songs, 0) }
                    )
                }

                activeTab == NavigationTab.ARTISTS -> {
                    ArtistsListView(
                        artists = artists,
                        onArtistClick = onSelectArtist,
                        onPlayArtist = { onPlayGroup(it.songs, 0) }
                    )
                }

                activeTab == NavigationTab.FOLDERS -> {
                    FoldersListView(
                        folders = folders,
                        onFolderClick = onSelectFolder,
                        onPlayFolder = { onPlayGroup(it.songs, 0) }
                    )
                }

                activeTab == NavigationTab.FAVORITES -> {
                    val favSongs = songs.filter { it.isFavorite }
                    if (favSongs.isEmpty()) {
                        EmptyLibraryState(
                            title = "No favorites yet",
                            message = "Tap the heart icon on any song to add it to your Favorites collection."
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 120.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(favSongs, key = { it.id }) { song ->
                                val isCurrent = song.id == currentSongId
                                SongItemCard(
                                    song = song,
                                    isCurrent = isCurrent,
                                    isPlaying = isCurrent && isPlaying,
                                    onSongClick = { onSongClick(song, favSongs) },
                                    onToggleFavorite = { onToggleFavorite(song) },
                                    onAddToPlaylist = { onAddToPlaylist(song) },
                                    onDeleteSong = { onDeleteSong(song) },
                                    onPlayNext = onPlayNext,
                                    onAddToQueue = onAddToQueue,
                                    onSongInfo = onSongInfo,
                                    onShareSong = onShareSong,
                                    onCutRingtone = onCutRingtone,
                                    showQualityBadges = showQualityBadges,
                                    reduceAnimations = reduceAnimations
                                )
                            }
                        }
                    }
                }

                else -> { // Default: SONGS
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Quality filter chips & Sort button row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AudioQualityFilter.values().forEach { filter ->
                                    val isSelected = audioQualityFilter == filter
                                    val label = when (filter) {
                                        AudioQualityFilter.ALL -> "All Quality"
                                        AudioQualityFilter.LOSSLESS -> "Lossless 💎"
                                        AudioQualityFilter.HIGH -> "High (256k+)"
                                        AudioQualityFilter.STANDARD -> "Standard"
                                        AudioQualityFilter.LOW -> "Low (<128k)"
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(if (isSelected) PrimaryViolet else SurfaceVariantDark)
                                            .clickable { onAudioQualityFilterSelected(filter) }
                                            .padding(horizontal = 10.dp, vertical = 5.dp)
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) Color.White else TextSecondary
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            // Sort Button
                            var sortMenuExpanded by remember { mutableStateOf(false) }
                            Box {
                                IconButton(
                                    onClick = { sortMenuExpanded = true },
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(SurfaceDark)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Sort,
                                        contentDescription = "Sort Songs",
                                        tint = PrimaryViolet,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                DropdownMenu(
                                    expanded = sortMenuExpanded,
                                    onDismissRequest = { sortMenuExpanded = false },
                                    modifier = Modifier.background(SurfaceHighlightDark)
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Date Added (Newest)", color = if (sortOption == SortOption.DATE_ADDED_DESC) PrimaryViolet else TextPrimary) },
                                        onClick = {
                                            onSortOptionSelected(SortOption.DATE_ADDED_DESC)
                                            sortMenuExpanded = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Quality (Highest first 💎)", color = if (sortOption == SortOption.QUALITY_DESC) PrimaryViolet else TextPrimary) },
                                        onClick = {
                                            onSortOptionSelected(SortOption.QUALITY_DESC)
                                            sortMenuExpanded = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Quality (Lowest first)", color = if (sortOption == SortOption.QUALITY_ASC) PrimaryViolet else TextPrimary) },
                                        onClick = {
                                            onSortOptionSelected(SortOption.QUALITY_ASC)
                                            sortMenuExpanded = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Title (A-Z)", color = if (sortOption == SortOption.TITLE_ASC) PrimaryViolet else TextPrimary) },
                                        onClick = {
                                            onSortOptionSelected(SortOption.TITLE_ASC)
                                            sortMenuExpanded = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Artist (A-Z)", color = if (sortOption == SortOption.ARTIST_ASC) PrimaryViolet else TextPrimary) },
                                        onClick = {
                                            onSortOptionSelected(SortOption.ARTIST_ASC)
                                            sortMenuExpanded = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Duration (Longest)", color = if (sortOption == SortOption.DURATION_DESC) PrimaryViolet else TextPrimary) },
                                        onClick = {
                                            onSortOptionSelected(SortOption.DURATION_DESC)
                                            sortMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        if (songs.isEmpty()) {
                            EmptyLibraryState(
                                title = if (audioQualityFilter != AudioQualityFilter.ALL) "No ${audioQualityFilter.name.lowercase()} songs" else "Your library is empty",
                                message = "Songs matching this filter will show up right here."
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 120.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(songs, key = { it.id }) { song ->
                                    val isCurrent = song.id == currentSongId
                                    SongItemCard(
                                        song = song,
                                        isCurrent = isCurrent,
                                        isPlaying = isCurrent && isPlaying,
                                        onSongClick = { onSongClick(song, songs) },
                                        onToggleFavorite = { onToggleFavorite(song) },
                                        onAddToPlaylist = { onAddToPlaylist(song) },
                                        onDeleteSong = { onDeleteSong(song) },
                                        onPlayNext = onPlayNext,
                                        onAddToQueue = onAddToQueue,
                                        onSongInfo = onSongInfo,
                                        onShareSong = onShareSong,
                                        onCutRingtone = onCutRingtone,
                                        showQualityBadges = showQualityBadges,
                                        reduceAnimations = reduceAnimations
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Filter Chip pill for Library tabs
 */
@Composable
fun LibraryFilterChip(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (isSelected) PrimaryViolet else SurfaceVariantDark)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Medium,
                color = if (isSelected) Color.White else TextSecondary,
                fontSize = 13.sp
            )
        )
    }
}

@Composable
fun EmptyLibraryState(
    title: String,
    message: String
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(SurfaceDark),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = PrimaryViolet,
                    modifier = Modifier.size(32.dp)
                )
            }

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            )

            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = TextSecondary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            )
        }
    }
}
