package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Song
import com.example.ui.theme.AccentPink
import com.example.ui.theme.CardBorder
import com.example.ui.theme.PrimaryViolet
import com.example.ui.theme.SecondaryAccent
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceHighlightDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun SongItemCard(
    song: Song,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onSongClick: (Song) -> Unit,
    onToggleFavorite: (Song) -> Unit,
    onAddToPlaylist: (Song) -> Unit,
    onDeleteSong: (Song) -> Unit,
    onSongInfo: ((Song) -> Unit)? = null,
    onShareSong: ((Song) -> Unit)? = null,
    onCutRingtone: ((Song) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }

    val containerColor = if (isCurrent) SurfaceHighlightDark else SurfaceDark.copy(alpha = 0.6f)
    val borderColor = if (isCurrent) PrimaryViolet.copy(alpha = 0.7f) else CardBorder.copy(alpha = 0.3f)

    Row(
        modifier = modifier
            .testTag("song_item_${song.id}")
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(containerColor)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable { onSongClick(song) }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail with playing indicator
        Box(contentAlignment = Alignment.BottomEnd) {
            AlbumArtThumbnail(
                song = song,
                size = 50.dp,
                shapeRadius = 10.dp
            )
            if (isCurrent) {
                Box(
                    modifier = Modifier
                        .padding(2.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.75f))
                        .padding(horizontal = 3.dp, vertical = 2.dp)
                ) {
                    MiniEqualizerBars(
                        isPlaying = isPlaying,
                        color = PrimaryViolet
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Title and Artist
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 15.sp,
                    color = if (isCurrent) PrimaryViolet else TextPrimary
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextSecondary,
                        fontSize = 13.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Text(
                    text = "•",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                )
                Text(
                    text = song.formattedDuration,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                )
            }
        }

        // Favorite Button
        IconButton(
            onClick = { onToggleFavorite(song) },
            modifier = Modifier
                .testTag("favorite_button_${song.id}")
                .minimumInteractiveComponentSize()
                .size(40.dp)
        ) {
            Icon(
                imageVector = if (song.isFavorite) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                contentDescription = if (song.isFavorite) "Remove from favorites" else "Add to favorites",
                tint = if (song.isFavorite) AccentPink else TextMuted
            )
        }

        // More Menu
        Box {
            IconButton(
                onClick = { menuExpanded = true },
                modifier = Modifier
                    .testTag("song_menu_${song.id}")
                    .minimumInteractiveComponentSize()
                    .size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "More song options",
                    tint = TextMuted
                )
            }

            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
                modifier = Modifier.background(SurfaceHighlightDark)
            ) {
                DropdownMenuItem(
                    text = { Text("Add to Playlist", color = TextPrimary) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.PlaylistAdd,
                            contentDescription = null,
                            tint = PrimaryViolet
                        )
                    },
                    onClick = {
                        menuExpanded = false
                        onAddToPlaylist(song)
                    }
                )
                onSongInfo?.let { onInfo ->
                    DropdownMenuItem(
                        text = { Text("Song Info", color = TextPrimary) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = SecondaryAccent
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onInfo(song)
                        }
                    )
                }
                onShareSong?.let { onShare ->
                    DropdownMenuItem(
                        text = { Text("Share Track", color = TextPrimary) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                tint = PrimaryViolet
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onShare(song)
                        }
                    )
                }
                onCutRingtone?.let { onCut ->
                    DropdownMenuItem(
                        text = { Text("Cut Ringtone", color = TextPrimary) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.ContentCut,
                                contentDescription = null,
                                tint = SecondaryAccent
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onCut(song)
                        }
                    )
                }
                DropdownMenuItem(
                    text = { Text("Delete Track", color = Color(0xFFEF4444)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            tint = Color(0xFFEF4444)
                        )
                    },
                    onClick = {
                        menuExpanded = false
                        onDeleteSong(song)
                    }
                )
            }
        }
    }
}
