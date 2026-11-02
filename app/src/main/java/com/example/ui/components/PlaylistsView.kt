package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.example.model.ContainerSortMode
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Playlist
import com.example.model.Song
import com.example.ui.theme.CardBorder
import com.example.ui.theme.PrimaryNeon
import com.example.ui.theme.SecondaryCyan
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceHighlightDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun PlaylistsView(
    playlists: List<Playlist>,
    allSongs: List<Song>,
    selectedPlaylist: Playlist?,
    currentSongId: String?,
    isPlaying: Boolean,
    onSelectPlaylist: (Playlist?) -> Unit,
    onCreatePlaylistClick: () -> Unit,
    onDeletePlaylist: (String) -> Unit,
    onPlayPlaylist: (Playlist, Boolean) -> Unit,
    onSongClick: (Song) -> Unit,
    onToggleFavorite: (Song) -> Unit,
    onRemoveSongFromPlaylist: (String, String) -> Unit,
    onReorderPlaylistSongs: ((playlistId: String, newSongs: List<Song>) -> Unit)? = null,
    onPlayNext: ((Song) -> Unit)? = null,
    onAddToQueue: ((Song) -> Unit)? = null,
    onSongInfo: ((Song) -> Unit)? = null,
    onShareSong: ((Song) -> Unit)? = null,
    onCutRingtone: ((Song) -> Unit)? = null,
    showQualityBadges: Boolean = true,
    modifier: Modifier = Modifier
) {
    if (selectedPlaylist != null) {
        // Back handler when inside playlist detail
        BackHandler {
            onSelectPlaylist(null)
        }

        val context = LocalContext.current
        val repository = remember { com.example.data.MusicRepository(context) }
        val containerKey = "playlist_${selectedPlaylist.id}"

        var sortMode by remember(selectedPlaylist.id) {
            mutableStateOf(repository.getContainerSort(containerKey))
        }

        val songMap = allSongs.associateBy { it.id }
        val rawPlaylistSongs = selectedPlaylist.songIds.mapNotNull { songMap[it] }

        var displayedSongs by remember(selectedPlaylist.id, rawPlaylistSongs, sortMode) {
            mutableStateOf(repository.resolveContainerSongs(containerKey, rawPlaylistSongs, sortMode).first)
        }

        var sortMenuExpanded by remember { mutableStateOf(false) }

        Column(
            modifier = modifier
                .testTag("playlist_detail_view")
                .fillMaxSize()
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { onSelectPlaylist(null) },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to playlists",
                        tint = TextPrimary
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = selectedPlaylist.name,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${displayedSongs.size} tracks • ${if (selectedPlaylist.description.isNotBlank()) selectedPlaylist.description else "Custom Playlist"}",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Quick Play / Shuffle Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { onPlayPlaylist(selectedPlaylist.copy(songIds = displayedSongs.map { it.id }), false) },
                    enabled = displayedSongs.isNotEmpty(),
                    modifier = Modifier
                        .testTag("play_all_playlist_button")
                        .weight(1f)
                        .height(44.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryNeon,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Play All", fontWeight = FontWeight.SemiBold)
                }

                OutlinedButton(
                    onClick = { onPlayPlaylist(selectedPlaylist.copy(songIds = displayedSongs.map { it.id }), true) },
                    enabled = displayedSongs.isNotEmpty(),
                    modifier = Modifier
                        .testTag("shuffle_playlist_button")
                        .weight(1f)
                        .height(44.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = SecondaryCyan
                    ),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = Brush.horizontalGradient(listOf(SecondaryCyan, PrimaryNeon))
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Shuffle", fontWeight = FontWeight.SemiBold)
                }
            }

            // Sort & Reorder Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (sortMode == ContainerSortMode.CUSTOM) "Custom Order (Drag ≡)" else sortMode.displayName,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = if (sortMode == ContainerSortMode.CUSTOM) PrimaryNeon else SecondaryCyan,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

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
                            contentDescription = "Sort playlist",
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
                                displayedSongs = repository.resolveContainerSongs(containerKey, rawPlaylistSongs, ContainerSortMode.CUSTOM).first
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
                                displayedSongs = repository.resolveContainerSongs(containerKey, rawPlaylistSongs, ContainerSortMode.TITLE).first
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
                                displayedSongs = repository.resolveContainerSongs(containerKey, rawPlaylistSongs, ContainerSortMode.ARTIST).first
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
                                displayedSongs = repository.resolveContainerSongs(containerKey, rawPlaylistSongs, ContainerSortMode.DATE).first
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
                                displayedSongs = repository.resolveContainerSongs(containerKey, rawPlaylistSongs, ContainerSortMode.DURATION).first
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
                                displayedSongs = repository.resolveContainerSongs(containerKey, rawPlaylistSongs, ContainerSortMode.DEFAULT).first
                                sortMenuExpanded = false
                            }
                        )
                    }
                }
            }

            if (displayedSongs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.QueueMusic,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No songs in this playlist yet",
                            style = MaterialTheme.typography.titleMedium.copy(color = TextPrimary),
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Tap the three dots on any song in your library and select 'Add to Playlist'",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                ReorderableSongList(
                    songs = displayedSongs,
                    isCustomOrder = sortMode == ContainerSortMode.CUSTOM,
                    currentSongId = currentSongId,
                    isPlaying = isPlaying,
                    onSongClick = onSongClick,
                    onToggleFavorite = onToggleFavorite,
                    onAddToPlaylist = {},
                    onDeleteSong = { song ->
                        onRemoveSongFromPlaylist(selectedPlaylist.id, song.id)
                        val updated = displayedSongs.filter { it.id != song.id }
                        displayedSongs = updated
                        repository.saveCustomOrder(containerKey, updated.map { it.id })
                    },
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
                            onReorderPlaylistSongs?.invoke(selectedPlaylist.id, mutable)
                        }
                    },
                    onSongInfo = onSongInfo,
                    onShareSong = onShareSong,
                    onCutRingtone = onCutRingtone,
                    showQualityBadges = showQualityBadges,
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 120.dp),
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    } else {
        // Playlists Overview List
        Column(
            modifier = modifier
                .testTag("playlists_view")
                .fillMaxSize()
        ) {
            // Action button to create playlist
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Your Playlists (${playlists.size})",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )

                Button(
                    onClick = onCreatePlaylistClick,
                    modifier = Modifier
                        .testTag("create_playlist_button")
                        .height(38.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryNeon,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New Playlist", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(playlists, key = { it.id }) { playlist ->
                    PlaylistCard(
                        playlist = playlist,
                        onClick = { onSelectPlaylist(playlist) },
                        onPlayClick = { onPlayPlaylist(playlist, false) },
                        onDeleteClick = { onDeletePlaylist(playlist.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaylistCard(
    playlist: Playlist,
    onClick: () -> Unit,
    onPlayClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceDark)
            .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    Brush.linearGradient(
                        listOf(PrimaryNeon.copy(alpha = 0.5f), SecondaryCyan.copy(alpha = 0.5f))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.QueueMusic,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = playlist.name,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    fontSize = 16.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${playlist.songIds.size} songs • ${playlist.description.ifBlank { "Playlist" }}",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = TextSecondary,
                    fontSize = 13.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        IconButton(
            onClick = onPlayClick,
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .size(40.dp)
                .clip(CircleShape)
                .background(SurfaceHighlightDark)
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Play Playlist",
                tint = SecondaryCyan,
                modifier = Modifier.size(22.dp)
            )
        }

        if (playlist.id != "pl_favorites") {
            IconButton(
                onClick = onDeleteClick,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Delete Playlist",
                    tint = TextMuted,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
