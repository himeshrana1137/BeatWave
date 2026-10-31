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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
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
    modifier: Modifier = Modifier
) {
    if (selectedPlaylist != null) {
        // Back handler when inside playlist detail
        BackHandler {
            onSelectPlaylist(null)
        }

        val songMap = allSongs.associateBy { it.id }
        val playlistSongs = selectedPlaylist.songIds.mapNotNull { songMap[it] }

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
                        text = "${playlistSongs.size} tracks • ${if (selectedPlaylist.description.isNotBlank()) selectedPlaylist.description else "Custom Playlist"}",
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
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { onPlayPlaylist(selectedPlaylist, false) },
                    enabled = playlistSongs.isNotEmpty(),
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
                    onClick = { onPlayPlaylist(selectedPlaylist, true) },
                    enabled = playlistSongs.isNotEmpty(),
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

            if (playlistSongs.isEmpty()) {
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
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(playlistSongs, key = { it.id }) { song ->
                        val isTrackCurrent = song.id == currentSongId
                        SongItemCard(
                            song = song,
                            isCurrent = isTrackCurrent,
                            isPlaying = isTrackCurrent && isPlaying,
                            onSongClick = { onSongClick(song) },
                            onToggleFavorite = { onToggleFavorite(song) },
                            onAddToPlaylist = {},
                            onDeleteSong = {
                                onRemoveSongFromPlaylist(selectedPlaylist.id, song.id)
                            }
                        )
                    }
                }
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
