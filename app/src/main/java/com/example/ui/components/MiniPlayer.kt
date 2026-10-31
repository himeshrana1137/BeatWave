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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.PlaybackSnapshot
import com.example.model.Song
import com.example.ui.theme.AccentPink
import com.example.ui.theme.PrimaryViolet
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceVariantDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun MiniPlayer(
    snapshot: PlaybackSnapshot,
    onExpand: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onToggleFavorite: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    val song = snapshot.currentSong ?: return

    val progress = if (snapshot.durationMs > 0) {
        (snapshot.currentPositionMs.toFloat() / snapshot.durationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    Box(
        modifier = modifier
            .testTag("mini_player")
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .shadow(12.dp, RoundedCornerShape(14.dp), spotColor = PrimaryViolet.copy(alpha = 0.35f))
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceDark)
            .border(1.dp, Color(0x338B5CF6), RoundedCornerShape(14.dp))
            .clickable { onExpand() }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Main Content Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Album Art (with placeholder gradient if no art)
                AlbumArtThumbnail(
                    song = song,
                    size = 44.dp,
                    shapeRadius = 8.dp
                )

                Spacer(modifier = Modifier.width(12.dp))

                // Song Title & Artist
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary,
                            fontSize = 14.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = song.artist,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextSecondary,
                            fontSize = 12.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Favorite button
                IconButton(
                    onClick = { onToggleFavorite(song) },
                    modifier = Modifier
                        .testTag("mini_player_favorite")
                        .minimumInteractiveComponentSize()
                        .size(36.dp)
                ) {
                    Icon(
                        imageVector = if (song.isFavorite) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = if (song.isFavorite) "Favorited" else "Favorite",
                        tint = if (song.isFavorite) AccentPink else TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(2.dp))

                // Play / Pause Button in Electric Violet (#8B5CF6)
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(PrimaryViolet)
                        .clickable { onTogglePlayPause() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (snapshot.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (snapshot.isPlaying) "Pause" else "Play",
                        tint = Color.White,
                        modifier = Modifier
                            .testTag("mini_player_play_pause")
                            .size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(2.dp))

                // Next Button
                IconButton(
                    onClick = { onSkipNext() },
                    modifier = Modifier
                        .testTag("mini_player_next")
                        .minimumInteractiveComponentSize()
                        .size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next Track",
                        tint = TextPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Thin progress line at the bottom of the mini player
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.5.dp),
                color = PrimaryViolet,
                trackColor = SurfaceVariantDark
            )
        }
    }
}
