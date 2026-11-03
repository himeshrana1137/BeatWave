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
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.example.model.AlbumItem
import com.example.model.Song
import com.example.model.VideoItem
import com.example.ui.theme.PrimaryViolet
import com.example.ui.theme.SecondaryAccent
import com.example.ui.theme.SecondaryCyan
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceHighlightDark
import com.example.ui.theme.SurfaceVariantDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.io.File
import java.util.Calendar

@Composable
fun HomeScreen(
    songs: List<Song>,
    albums: List<AlbumItem>,
    videos: List<VideoItem> = emptyList(),
    currentSongId: String?,
    isPlaying: Boolean,
    onSongClick: (Song, List<Song>) -> Unit,
    onAlbumClick: (AlbumItem) -> Unit,
    onVideoClick: (VideoItem) -> Unit = {},
    onScanMedia: () -> Unit,
    onAddSongs: () -> Unit,
    onDemoTracks: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenWrapped: () -> Unit = {},
    onOpenAchievements: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    autoScanEnabled: Boolean = true,
    showQualityBadges: Boolean = true,
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

    // Continue Watching Videos (Feature 6)
    val continueWatching = remember(videos) {
        videos.filter { it.lastPositionMs > 1000L }.sortedByDescending { it.lastPositionMs }
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

    var overflowMenuExpanded by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .testTag("home_screen")
            .fillMaxSize(),
        contentPadding = PaddingValues(top = 12.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        // Top App Bar: Brand Logo, Search icon, Settings icon, Overflow (⋮) Menu
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
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    IconButton(
                        onClick = onSearchClick,
                        modifier = Modifier
                            .testTag("home_search_button")
                            .minimumInteractiveComponentSize()
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(SurfaceDark)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = TextPrimary,
                            modifier = Modifier.size(19.dp)
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
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    Box {
                        IconButton(
                            onClick = { overflowMenuExpanded = true },
                            modifier = Modifier
                                .testTag("home_overflow_button")
                                .minimumInteractiveComponentSize()
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(SurfaceDark)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More options",
                                tint = TextSecondary,
                                modifier = Modifier.size(19.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = overflowMenuExpanded,
                            onDismissRequest = { overflowMenuExpanded = false },
                            modifier = Modifier.background(SurfaceHighlightDark)
                        ) {
                            if (autoScanEnabled) {
                                DropdownMenuItem(
                                    text = { Text("Add Songs / Files", color = TextPrimary) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = null,
                                            tint = PrimaryViolet,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    onClick = {
                                        overflowMenuExpanded = false
                                        onAddSongs()
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("Scan / Refresh Media", color = TextPrimary) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = null,
                                        tint = SecondaryCyan,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                onClick = {
                                    overflowMenuExpanded = false
                                    onScanMedia()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("BeatWave Wrapped", color = TextPrimary) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Celebration,
                                        contentDescription = null,
                                        tint = PrimaryViolet,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                onClick = {
                                    overflowMenuExpanded = false
                                    onOpenWrapped()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Stats & Achievements", color = TextPrimary) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.EmojiEvents,
                                        contentDescription = null,
                                        tint = SecondaryAccent,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                onClick = {
                                    overflowMenuExpanded = false
                                    onOpenAchievements()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Equalizer", color = TextPrimary) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Equalizer,
                                        contentDescription = null,
                                        tint = TextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                onClick = {
                                    overflowMenuExpanded = false
                                    onOpenEqualizer()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Demo Tracks", color = TextPrimary) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = SecondaryAccent,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                onClick = {
                                    overflowMenuExpanded = false
                                    onDemoTracks()
                                }
                            )
                        }
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

                if (!autoScanEnabled) {
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
        }

        // Quick Action Chips (BeatWave Wrapped & Achievements)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // BeatWave Wrapped Pill
                Row(
                    modifier = Modifier
                        .testTag("home_wrapped_chip")
                        .clip(RoundedCornerShape(20.dp))
                        .background(PrimaryViolet.copy(alpha = 0.2f))
                        .border(1.dp, PrimaryViolet.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                        .clickable { onOpenWrapped() }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Celebration,
                        contentDescription = null,
                        tint = PrimaryViolet,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "BeatWave Wrapped",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                }

                // Achievements Pill
                Row(
                    modifier = Modifier
                        .testTag("home_achievements_chip")
                        .clip(RoundedCornerShape(20.dp))
                        .background(SecondaryAccent.copy(alpha = 0.2f))
                        .border(1.dp, SecondaryAccent.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                        .clickable { onOpenAchievements() }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = SecondaryAccent,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Badges & Stats",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                }
            }
        }

        // Section: Continue Watching (Videos with progress)
        if (continueWatching.isNotEmpty()) {
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Continue Watching",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        Text(
                            text = "${continueWatching.size} in progress",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(continueWatching, key = { it.id }) { video ->
                            ContinueWatchingCard(
                                video = video,
                                onClick = { onVideoClick(video) },
                                showQualityBadges = showQualityBadges
                            )
                        }
                    }
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
                    onSongClick = { onSongClick(it, recentlyAdded) },
                    showQualityBadges = showQualityBadges
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
                    onSongClick = { onSongClick(it, mostPlayed) },
                    showQualityBadges = showQualityBadges
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
                    onSongClick = { onSongClick(it, favorites) },
                    showQualityBadges = showQualityBadges
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
            text = song.displayTitle,
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
    showQualityBadges: Boolean = true,
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
                    onClick = { onSongClick(song) },
                    showQualityBadges = showQualityBadges
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
    showQualityBadges: Boolean = true,
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
            text = song.displayTitle,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = if (isCurrent) PrimaryViolet else TextPrimary
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (showQualityBadges) {
                val quality = remember(song.id) {
                    com.example.util.MediaQualityManager.getAudioQualitySync(song)
                }
                AudioQualityBadge(quality = quality, compact = true)
            }
            Text(
                text = song.displayArtist,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp,
                    color = TextSecondary
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
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

@Composable
fun ContinueWatchingCard(
    video: VideoItem,
    onClick: () -> Unit,
    showQualityBadges: Boolean = true,
    modifier: Modifier = Modifier
) {
    val bitmap = remember(video.thumbnailPath) {
        video.thumbnailPath?.let { path ->
            try {
                val f = File(path)
                if (f.exists()) BitmapFactory.decodeFile(f.absolutePath)?.asImageBitmap() else null
            } catch (_: Exception) {
                null
            }
        }
    }

    val progress = if (video.durationMs > 0 && video.lastPositionMs > 0) {
        (video.lastPositionMs.toFloat() / video.durationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    Column(
        modifier = modifier
            .testTag("continue_watching_${video.id}")
            .width(170.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceDark)
            .border(1.dp, Color(0x228B5CF6), RoundedCornerShape(12.dp))
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(96.dp)
                .background(SurfaceHighlightDark)
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = video.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Videocam,
                        contentDescription = null,
                        tint = PrimaryViolet,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            // Quality badges (Resolution, HDR, 60fps)
            if (showQualityBadges) {
                val quality = remember(video.id) {
                    com.example.util.MediaQualityManager.getVideoQualitySync(video)
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                ) {
                    VideoQualityBadges(quality = quality)
                }
            }

            // Duration and progress overlay
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.75f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "${(video.lastPositionMs / 1000) / 60}:${String.format("%02d", (video.lastPositionMs / 1000) % 60)}",
                    color = SecondaryCyan,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp),
            color = PrimaryViolet,
            trackColor = SurfaceHighlightDark
        )

        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = video.title,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Resume playback",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 10.sp,
                    color = TextSecondary
                )
            )
        }
    }
}
