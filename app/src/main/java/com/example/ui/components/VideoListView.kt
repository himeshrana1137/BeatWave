package com.example.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.VideoItem
import com.example.model.VideoResolutionFilter
import com.example.model.VideoResolutionTier
import com.example.model.VideoSortOption
import com.example.util.MediaQualityManager
import com.example.ui.theme.PrimaryViolet
import com.example.ui.theme.SecondaryAccent
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceHighlightDark
import com.example.ui.theme.SurfaceVariantDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.io.File

@Composable
fun VideoListView(
    videos: List<VideoItem>,
    onVideoClick: (VideoItem) -> Unit,
    onAddVideosClick: () -> Unit,
    onLoadDemoVideo: () -> Unit,
    onDeleteVideo: (String) -> Unit,
    onShareVideo: ((VideoItem) -> Unit)? = null,
    onOpenStreamUrl: () -> Unit = {},
    onOpenVault: () -> Unit = {},
    showQualityBadges: Boolean = true,
    modifier: Modifier = Modifier
) {
    var resolutionFilter by remember { mutableStateOf(VideoResolutionFilter.ALL) }
    var sortOption by remember { mutableStateOf(VideoSortOption.DATE_DESC) }
    var showSortMenu by remember { mutableStateOf(false) }

    val processedVideos = remember(videos, resolutionFilter, sortOption) {
        var list = videos
        if (resolutionFilter != VideoResolutionFilter.ALL) {
            list = list.filter { video ->
                val q = MediaQualityManager.getVideoQualitySync(video)
                when (resolutionFilter) {
                    VideoResolutionFilter.UHD_4K_8K -> q.resolutionTier == VideoResolutionTier.UHD_8K || q.resolutionTier == VideoResolutionTier.UHD_4K
                    VideoResolutionFilter.FHD_1080P -> q.resolutionTier == VideoResolutionTier.FHD || q.resolutionTier == VideoResolutionTier.QHD_2K
                    VideoResolutionFilter.HD_720P -> q.resolutionTier == VideoResolutionTier.HD
                    VideoResolutionFilter.SD -> q.resolutionTier == VideoResolutionTier.SD
                    VideoResolutionFilter.ALL -> true
                }
            }
        }
        when (sortOption) {
            VideoSortOption.DATE_DESC -> list.sortedByDescending { it.dateAdded }
            VideoSortOption.RESOLUTION_DESC -> list.sortedWith(
                compareByDescending<VideoItem> { MediaQualityManager.getVideoQualitySync(it).resolutionTier.rank }
                    .thenByDescending { MediaQualityManager.getVideoQualitySync(it).height }
                    .thenByDescending { it.dateAdded }
            )
            VideoSortOption.RESOLUTION_ASC -> list.sortedWith(
                compareBy<VideoItem> { MediaQualityManager.getVideoQualitySync(it).resolutionTier.rank }
                    .thenBy { MediaQualityManager.getVideoQualitySync(it).height }
                    .thenByDescending { it.dateAdded }
            )
            VideoSortOption.DURATION_DESC -> list.sortedByDescending { it.durationMs }
            VideoSortOption.TITLE_ASC -> list.sortedBy { it.title.lowercase() }
        }
    }

    Column(
        modifier = modifier
            .testTag("video_list_view")
            .fillMaxSize()
    ) {
        // Top action bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Videos",
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )
                Text(
                    text = "${videos.size} videos available",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Stream URL Player
                IconButton(
                    onClick = onOpenStreamUrl,
                    modifier = Modifier
                        .testTag("stream_url_button")
                        .minimumInteractiveComponentSize()
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(SurfaceDark)
                ) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = "Stream URL",
                        tint = PrimaryViolet,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Private Vault
                IconButton(
                    onClick = onOpenVault,
                    modifier = Modifier
                        .testTag("private_vault_button")
                        .minimumInteractiveComponentSize()
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(SurfaceDark)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Private Vault",
                        tint = SecondaryAccent,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onLoadDemoVideo,
                    modifier = Modifier
                        .testTag("demo_video_button")
                        .minimumInteractiveComponentSize()
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(SurfaceDark)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Demo Video",
                        tint = SecondaryAccent,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Button(
                    onClick = onAddVideosClick,
                    modifier = Modifier
                        .testTag("add_videos_button")
                        .height(36.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryViolet,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // Resolution Filter & Sort Options Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
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
                VideoResolutionFilter.values().forEach { filter ->
                    val isSelected = resolutionFilter == filter
                    val label = when (filter) {
                        VideoResolutionFilter.ALL -> "All Resolutions"
                        VideoResolutionFilter.UHD_4K_8K -> "4K / 8K UHD"
                        VideoResolutionFilter.FHD_1080P -> "1080p FHD"
                        VideoResolutionFilter.HD_720P -> "720p HD"
                        VideoResolutionFilter.SD -> "SD"
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) PrimaryViolet else SurfaceVariantDark)
                            .clickable { resolutionFilter = filter }
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

            // Sort button
            Box {
                IconButton(
                    onClick = { showSortMenu = true },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(SurfaceDark)
                ) {
                    Icon(
                        imageVector = Icons.Default.Sort,
                        contentDescription = "Sort Videos",
                        tint = PrimaryViolet,
                        modifier = Modifier.size(18.dp)
                    )
                }

                DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = { showSortMenu = false },
                    modifier = Modifier.background(SurfaceHighlightDark)
                ) {
                    DropdownMenuItem(
                        text = { Text("Date Added (Newest)", color = if (sortOption == VideoSortOption.DATE_DESC) PrimaryViolet else TextPrimary) },
                        onClick = {
                            sortOption = VideoSortOption.DATE_DESC
                            showSortMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Resolution (Highest first)", color = if (sortOption == VideoSortOption.RESOLUTION_DESC) PrimaryViolet else TextPrimary) },
                        onClick = {
                            sortOption = VideoSortOption.RESOLUTION_DESC
                            showSortMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Resolution (Lowest first)", color = if (sortOption == VideoSortOption.RESOLUTION_ASC) PrimaryViolet else TextPrimary) },
                        onClick = {
                            sortOption = VideoSortOption.RESOLUTION_ASC
                            showSortMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Duration (Longest)", color = if (sortOption == VideoSortOption.DURATION_DESC) PrimaryViolet else TextPrimary) },
                        onClick = {
                            sortOption = VideoSortOption.DURATION_DESC
                            showSortMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Title (A-Z)", color = if (sortOption == VideoSortOption.TITLE_ASC) PrimaryViolet else TextPrimary) },
                        onClick = {
                            sortOption = VideoSortOption.TITLE_ASC
                            showSortMenu = false
                        }
                    )
                }
            }
        }

        if (processedVideos.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(SurfaceDark),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = null,
                            tint = PrimaryViolet,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = if (resolutionFilter != VideoResolutionFilter.ALL) "No ${resolutionFilter.name} videos" else "No videos yet",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Pick MP4 or WebM videos from your device storage, or generate demo clip to test playback!",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = onAddVideosClick,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PrimaryViolet,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Pick Videos")
                        }

                        OutlinedButton(
                            onClick = onLoadDemoVideo,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryViolet),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Load Demo")
                        }
                    }
                }
            }
        } else {
            // 2-Column Grid of Video Thumbnails with Duration & Quality Badges
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(processedVideos, key = { it.id }) { video ->
                    VideoGridCard(
                        video = video,
                        onClick = { onVideoClick(video) },
                        onDeleteClick = { onDeleteVideo(video.id) },
                        onShareClick = onShareVideo?.let { { it(video) } },
                        showQualityBadges = showQualityBadges
                    )
                }
            }
        }
    }
}

/**
 * Modern 2-column video grid card with thumbnail, duration badge, title, and options
 */
@Composable
fun VideoGridCard(
    video: VideoItem,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onShareClick: (() -> Unit)? = null,
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
            .testTag("video_grid_card_${video.id}")
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceDark)
            .border(1.dp, Color(0x228B5CF6), RoundedCornerShape(12.dp))
            .clickable { onClick() }
    ) {
        // Thumbnail & Duration Badge
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 10f)
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF201733), Color(0xFF141420))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = video.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Videocam,
                    contentDescription = null,
                    tint = PrimaryViolet.copy(alpha = 0.6f),
                    modifier = Modifier.size(36.dp)
                )
            }

            // Resolution and HDR/60fps badges in top-start corner
            if (showQualityBadges) {
                val context = androidx.compose.ui.platform.LocalContext.current
                var quality by remember(video.id) {
                    mutableStateOf(MediaQualityManager.getVideoQualitySync(video))
                }
                LaunchedEffect(video.id) {
                    quality = MediaQualityManager.getVideoQuality(context, video)
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                ) {
                    VideoQualityBadges(quality = quality)
                }
            }

            // Play overlay circle
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Duration badge in bottom corner
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (!video.subtitleUriString.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.Black.copy(alpha = 0.8f))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "CC",
                            color = PrimaryViolet,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.8f))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = video.formattedDuration,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Watch progress line
        if (progress > 0f) {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.5.dp),
                color = PrimaryViolet,
                trackColor = SurfaceVariantDark
            )
        }

        // Video Info & Delete Action
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        fontSize = 13.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (video.lastPositionMs > 5000L) {
                    Text(
                        text = "Watched ${formatTime(video.lastPositionMs)}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = PrimaryViolet,
                            fontSize = 11.sp
                        )
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                onShareClick?.let { onShare ->
                    IconButton(
                        onClick = onShare,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share Video",
                            tint = TextMuted,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Video",
                        tint = TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

private fun formatTime(millis: Long): String {
    if (millis <= 0) return "00:00"
    val totalSec = millis / 1000
    val min = totalSec / 60
    val sec = totalSec % 60
    return String.format("%02d:%02d", min, sec)
}
