package com.example.ui.components

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import com.example.model.AlbumItem
import com.example.model.Song
import com.example.ui.theme.PrimaryViolet
import com.example.ui.theme.SecondaryAccent
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceHighlightDark
import com.example.ui.theme.SurfaceVariantDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Calendar

@Composable
fun HomeScreen(
    songs: List<Song>,
    albums: List<AlbumItem>,
    currentSongId: String?,
    isPlaying: Boolean,
    onSongClick: (Song, List<Song>) -> Unit,
    onAlbumClick: (AlbumItem) -> Unit,
    onScanMedia: () -> Unit,
    onAddSongs: () -> Unit,
    onDemoTracks: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Dynamic greeting based on time of day
    val greeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when {
            hour < 12 -> "Good morning"
            hour < 17 -> "Good afternoon"
            else -> "Good evening"
        }
    }

    // Curated sections
    val recentlyPlayed = remember(songs) {
        val played = songs.filter { it.lastPlayedTime > 0 }.sortedByDescending { it.lastPlayedTime }
        if (played.isNotEmpty()) played.take(6) else songs.take(6)
    }

    val recentlyAdded = remember(songs) {
        songs.sortedByDescending { it.dateAdded }.take(10)
    }

    val mostPlayed = remember(songs) {
        val sorted = songs.filter { it.playCount > 0 }.sortedByDescending { it.playCount }
        if (sorted.isNotEmpty()) sorted.take(10) else songs.shuffled().take(10)
    }

    val favorites = remember(songs) {
        songs.filter { it.isFavorite }.take(10)
    }

    LazyColumn(
        modifier = modifier
            .testTag("home_screen")
            .fillMaxSize(),
        contentPadding = PaddingValues(top = 12.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        // Top App Bar & Brand Actions
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                BeatWaveLogoHeader(iconSize = 34.dp)

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = onScanMedia,
                        modifier = Modifier
                            .testTag("home_scan_button")
                            .minimumInteractiveComponentSize()
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(SurfaceDark)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Scan Media",
                            tint = PrimaryViolet,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onDemoTracks,
                        modifier = Modifier
                            .testTag("home_demo_button")
                            .minimumInteractiveComponentSize()
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(SurfaceDark)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Demo Tracks",
                            tint = SecondaryAccent,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onOpenEqualizer,
                        modifier = Modifier
                            .testTag("home_equalizer_button")
                            .minimumInteractiveComponentSize()
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(SurfaceDark)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Equalizer,
                            contentDescription = "Equalizer",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier
                            .testTag("home_settings_button")
                            .minimumInteractiveComponentSize()
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(SurfaceDark)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // Greeting Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = greeting,
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )

                Button(
                    onClick = onAddSongs,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryViolet,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // Spotify-style 2-Column Grid of Quick-Access / Recently Played Cards
        if (recentlyPlayed.isNotEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val rows = recentlyPlayed.chunked(2)
                    for (rowItems in rows) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (song in rowItems) {
                                val isCurrent = song.id == currentSongId
                                SpotifyQuickCard(
                                    song = song,
                                    isCurrent = isCurrent,
                                    isPlaying = isCurrent && isPlaying,
                                    onClick = { onSongClick(song, songs) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (rowItems.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }

        // Section: Recently Added (Horizontal Scrolling Carousel)
        if (recentlyAdded.isNotEmpty()) {
            item {
                HomeCarouselSection(
                    title = "Recently Added",
                    songs = recentlyAdded,
                    currentSongId = currentSongId,
                    isPlaying = isPlaying,
                    onSongClick = { onSongClick(it, recentlyAdded) }
                )
            }
        }

        // Section: Most Played (Horizontal Scrolling Carousel)
        if (mostPlayed.isNotEmpty()) {
            item {
                HomeCarouselSection(
                    title = "Most Played",
                    songs = mostPlayed,
                    currentSongId = currentSongId,
                    isPlaying = isPlaying,
                    onSongClick = { onSongClick(it, mostPlayed) }
                )
            }
        }

        // Section: Favorites (Horizontal Scrolling Carousel)
        if (favorites.isNotEmpty()) {
            item {
                HomeCarouselSection(
                    title = "Favorites",
                    songs = favorites,
                    currentSongId = currentSongId,
                    isPlaying = isPlaying,
                    onSongClick = { onSongClick(it, favorites) }
                )
            }
        }

        // Section: Albums (Horizontal Scrolling Carousel)
        if (albums.isNotEmpty()) {
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Albums",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        ),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(albums, key = { it.title + it.artist }) { album ->
                            HomeAlbumCard(
                                album = album,
                                onClick = { onAlbumClick(album) }
                            )
                        }
                    }
                }
            }
        }

        // Empty state placeholder if device has no tracks
        if (songs.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        BeatWaveIcon(size = 64.dp)

                        Text(
                            text = "No songs found on device",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )

                        Text(
                            text = "Scan your device or tap below to pick local audio files or demo tracks.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextSecondary,
                                fontSize = 13.sp
                            )
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = onScanMedia,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = PrimaryViolet,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Scan Media")
                            }

                            Button(
                                onClick = onDemoTracks,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SurfaceVariantDark,
                                    contentColor = TextPrimary
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Load Demo")
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Spotify-style 2-column quick access card (56dp height, album art left, title right, play state)
 */
@Composable
fun SpotifyQuickCard(
    song: Song,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .testTag("quick_card_${song.id}")
            .height(56.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isCurrent) SurfaceHighlightDark else SurfaceVariantDark)
            .border(
                1.dp,
                if (isCurrent) PrimaryViolet.copy(alpha = 0.6f) else Color(0x1A8B5CF6),
                RoundedCornerShape(8.dp)
            )
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        AlbumArtThumbnail(
            song = song,
            size = 56.dp,
            shapeRadius = 0.dp
        )

        Spacer(modifier = Modifier.width(10.dp))

        Text(
            text = song.title,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold,
                fontSize = 13.sp,
                color = if (isCurrent) PrimaryViolet else TextPrimary
            ),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        if (isCurrent) {
            Box(
                modifier = Modifier
                    .padding(end = 8.dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(PrimaryViolet),
                contentAlignment = Alignment.Center
            ) {
                MiniEqualizerBars(isPlaying = isPlaying, barCount = 3, color = Color.White)
            }
        } else {
            Spacer(modifier = Modifier.width(8.dp))
        }
    }
}

/**
 * Horizontal Carousel Section with large rounded-corner cards
 */
@Composable
fun HomeCarouselSection(
    title: String,
    songs: List<Song>,
    currentSongId: String?,
    isPlaying: Boolean,
    onSongClick: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            ),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(songs, key = { it.id }) { song ->
                val isCurrent = song.id == currentSongId
                HomeSongCard(
                    song = song,
                    isCurrent = isCurrent,
                    isPlaying = isCurrent && isPlaying,
                    onClick = { onSongClick(song) }
                )
            }
        }
    }
}

/**
 * Large rounded-corner card for horizontal carousel (140dp x 140dp artwork)
 */
@Composable
fun HomeSongCard(
    song: Song,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .testTag("home_card_${song.id}")
            .width(140.dp)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(140.dp)
                .shadow(8.dp, RoundedCornerShape(12.dp), spotColor = Color(0x338B5CF6))
                .clip(RoundedCornerShape(12.dp))
                .border(
                    1.dp,
                    if (isCurrent) PrimaryViolet else Color(0x228B5CF6),
                    RoundedCornerShape(12.dp)
                )
        ) {
            AlbumArtThumbnail(
                song = song,
                size = 140.dp,
                shapeRadius = 12.dp
            )

            // Play overlay button
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(PrimaryViolet)
                    .shadow(4.dp, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (isCurrent) {
                    MiniEqualizerBars(isPlaying = isPlaying, barCount = 3, color = Color.White)
                } else {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = song.title,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = if (isCurrent) PrimaryViolet else TextPrimary
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = song.artist,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 12.sp,
                color = TextSecondary
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun HomeAlbumCard(
    album: AlbumItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .testTag("home_album_${album.title}")
            .width(140.dp)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(140.dp)
                .shadow(8.dp, RoundedCornerShape(12.dp), spotColor = Color(0x338B5CF6))
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, Color(0x228B5CF6), RoundedCornerShape(12.dp))
        ) {
            AlbumArtThumbnail(
                song = album.songs.firstOrNull(),
                size = 140.dp,
                shapeRadius = 12.dp
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = album.title,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = TextPrimary
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = "${album.artist} • ${album.songCount} tracks",
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 11.sp,
                color = TextSecondary
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
