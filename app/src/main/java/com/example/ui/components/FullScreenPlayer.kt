package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.LyricLine
import com.example.model.PlaybackSnapshot
import com.example.model.RepeatMode as SongRepeatMode
import com.example.model.Song
import com.example.playback.LrcParser
import com.example.ui.theme.AccentPink
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.CardBorder
import com.example.ui.theme.PrimaryNeon
import com.example.ui.theme.PrimaryViolet
import com.example.ui.theme.PrimaryVioletLight
import com.example.ui.theme.SecondaryAccent
import com.example.ui.theme.SecondaryCyan
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceHighlightDark
import com.example.ui.theme.SurfaceVariantDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.sin

import android.widget.Toast
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Speed
import androidx.compose.runtime.rememberCoroutineScope
import com.example.playback.LyricsRepository
import com.example.util.NowPlayingCardGenerator
/**
 * Refined seek bar with 4dp thin track, 12dp round thumb, and accent played color.
 */
@Composable
fun CustomSeekBar(
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val thumbSizePx = with(density) { 12.dp.toPx() }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(28.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { offset ->
                        val availableWidth = (size.width - thumbSizePx).coerceAtLeast(1f)
                        val fraction = ((offset.x - thumbSizePx / 2f) / availableWidth).coerceIn(0f, 1f)
                        onValueChange(fraction)
                        tryAwaitRelease()
                        onValueChangeFinished()
                    }
                )
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        val availableWidth = (size.width - thumbSizePx).coerceAtLeast(1f)
                        val fraction = ((offset.x - thumbSizePx / 2f) / availableWidth).coerceIn(0f, 1f)
                        onValueChange(fraction)
                    },
                    onDragEnd = { onValueChangeFinished() },
                    onDragCancel = { onValueChangeFinished() },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        val availableWidth = (size.width - thumbSizePx).coerceAtLeast(1f)
                        val deltaFraction = dragAmount / availableWidth
                        val newFraction = (value + deltaFraction).coerceIn(0f, 1f)
                        onValueChange(newFraction)
                    }
                )
            },
        contentAlignment = Alignment.CenterStart
    ) {
        val totalWidthPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        val availableTrackWidthPx = (totalWidthPx - thumbSizePx).coerceAtLeast(1f)
        val thumbOffsetPx = (value.coerceIn(0f, 1f) * availableTrackWidthPx)

        // Track (4dp thin, rounded)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color(0x29FFFFFF))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(value.coerceIn(0f, 1f))
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(PrimaryViolet)
            )
        }

        // Small 12dp round thumb
        Box(
            modifier = Modifier
                .graphicsLayer { translationX = thumbOffsetPx }
                .size(12.dp)
                .shadow(3.dp, CircleShape, spotColor = Color.Black.copy(alpha = 0.5f))
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullScreenPlayer(
    snapshot: PlaybackSnapshot,
    onCollapse: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onToggleFavorite: (Song) -> Unit,
    onAddToPlaylist: (Song) -> Unit,
    onSelectQueueSong: (Song) -> Unit,
    onMoveQueueItem: (from: Int, to: Int) -> Unit,
    onSetPointA: () -> Unit,
    onSetPointB: () -> Unit,
    onClearABRepeat: () -> Unit,
    onAttachLyrics: (songId: String, uri: Uri) -> Unit,
    isVisualizerEnabled: Boolean = true,
    onSongInfo: ((Song) -> Unit)? = null,
    onShareSong: ((Song) -> Unit)? = null,
    onCutRingtone: ((Song) -> Unit)? = null,
    onSetPlaybackSpeedAndPitch: ((speed: Float, pitch: Float) -> Unit)? = null,
    onTagMood: ((Song) -> Unit)? = null,
    showQualityBadges: Boolean = true,
    modifier: Modifier = Modifier
) {
    BackHandler {
        onCollapse()
    }

    val context = LocalContext.current
    val song = snapshot.currentSong ?: return

    var isDraggingSlider by remember { mutableStateOf(false) }
    var dragPositionMs by remember { mutableFloatStateOf(0f) }
    var showQueueSheet by remember { mutableStateOf(false) }
    var showLyricsView by remember { mutableStateOf(false) }
    var optionsMenuExpanded by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    var showSpeedPitchSheet by remember { mutableStateOf(false) }
    var currentSpeed by remember(snapshot.playbackSpeed) { mutableFloatStateOf(snapshot.playbackSpeed) }
    var currentPitch by remember(snapshot.playbackPitch) { mutableFloatStateOf(snapshot.playbackPitch) }
    var isFetchingLyrics by remember { mutableStateOf(false) }
    var showAbRepeatRow by remember { mutableStateOf(snapshot.abRepeatA != null || snapshot.abRepeatB != null) }

    // Lyrics State
    var lyrics by remember { mutableStateOf<List<LyricLine>>(emptyList()) }
    val lyricsListState = rememberLazyListState()

    // LRC file picker
    val lrcPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            onAttachLyrics(song.id, uri)
            lyrics = LrcParser.parseLrcFromUri(context, uri)
        }
    }

    // Load lyrics (auto LRCLIB fetch and local cache)
    LaunchedEffect(song.id, song.lyricsUriString) {
        if (!song.lyricsUriString.isNullOrBlank()) {
            lyrics = LrcParser.parseLrcFromUri(context, Uri.parse(song.lyricsUriString))
        } else {
            val fetched = LyricsRepository(context).getOrFetchLyrics(song)
            if (fetched.isNotEmpty()) {
                lyrics = fetched
            } else if (song.isDemoTrack) {
                lyrics = LrcParser.generateDemoLyrics(song.title)
            } else {
                lyrics = emptyList()
            }
        }
    }

    val currentPos = if (isDraggingSlider) {
        dragPositionMs.toLong()
    } else {
        snapshot.currentPositionMs
    }

    // Active lyric line index
    val activeLyricIndex = remember(lyrics, currentPos) {
        if (lyrics.isEmpty()) -1
        else lyrics.indexOfLast { currentPos >= it.timeMs }.coerceAtLeast(0)
    }

    // Auto-scroll lyrics
    LaunchedEffect(activeLyricIndex, showLyricsView) {
        if (showLyricsView && activeLyricIndex >= 0 && activeLyricIndex < lyrics.size) {
            lyricsListState.animateScrollToItem((activeLyricIndex - 2).coerceAtLeast(0))
        }
    }

    val totalDuration = snapshot.durationMs.coerceAtLeast(1L)
    val sliderValue = (currentPos.toFloat() / totalDuration.toFloat()).coerceIn(0f, 1f)

    // Visualizer animation
    val infiniteTransition = rememberInfiniteTransition(label = "player_visuals")
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (snapshot.isPlaying) 360f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_phase"
    )

    val dominantColor = rememberDominantColor(song)
    val playerBaseColor = Color(0xFF0B0B10)

    Box(
        modifier = modifier
            .testTag("full_screen_player")
            .fillMaxSize()
            .background(playerBaseColor)
            .pointerInput(Unit) {
                detectTapGestures { } // consume all touch events
            }
    ) {
        // Blurred artwork / gradient layer on top with subtle opacity
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            dominantColor.copy(alpha = 0.22f),
                            playerBaseColor.copy(alpha = 0.65f),
                            playerBaseColor,
                            playerBaseColor
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // (a) Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onCollapse,
                    modifier = Modifier
                        .testTag("player_collapse_button")
                        .minimumInteractiveComponentSize()
                        .size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Collapse Player",
                        tint = TextPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (showLyricsView) "LYRICS" else "NOW PLAYING",
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 1.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryVioletLight,
                            fontSize = 11.sp
                        )
                    )
                    Text(
                        text = song.album.ifBlank { "BeatWave" },
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextSecondary,
                            fontSize = 12.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Queue Icon Button
                    IconButton(
                        onClick = { showQueueSheet = true },
                        modifier = Modifier
                            .testTag("player_queue_button")
                            .minimumInteractiveComponentSize()
                            .size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                            contentDescription = "Queue",
                            tint = TextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Lyrics Toggle Button
                    IconButton(
                        onClick = { showLyricsView = !showLyricsView },
                        modifier = Modifier
                            .testTag("lyrics_toggle_button")
                            .minimumInteractiveComponentSize()
                            .size(40.dp)
                    ) {
                        Icon(
                            imageVector = if (showLyricsView) Icons.Default.MusicNote else Icons.Default.FormatQuote,
                            contentDescription = "Toggle Lyrics",
                            tint = if (showLyricsView) PrimaryViolet else TextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Overflow Menu (More Options)
                    Box {
                        IconButton(
                            onClick = { optionsMenuExpanded = true },
                            modifier = Modifier
                                .testTag("player_more_options_button")
                                .minimumInteractiveComponentSize()
                                .size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More track options",
                                tint = TextSecondary,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = optionsMenuExpanded,
                            onDismissRequest = { optionsMenuExpanded = false },
                            modifier = Modifier.background(SurfaceHighlightDark)
                        ) {
                            // A-B Repeat toggle
                            DropdownMenuItem(
                                text = { Text(if (showAbRepeatRow) "Hide A-B Repeat" else "A-B Repeat", color = TextPrimary) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Loop,
                                        contentDescription = null,
                                        tint = PrimaryViolet
                                    )
                                },
                                onClick = {
                                    optionsMenuExpanded = false
                                    showAbRepeatRow = !showAbRepeatRow
                                }
                            )

                            // Add to Playlist
                            DropdownMenuItem(
                                text = { Text("Add to Playlist", color = TextPrimary) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.PlaylistAdd,
                                        contentDescription = null,
                                        tint = TextSecondary
                                    )
                                },
                                onClick = {
                                    optionsMenuExpanded = false
                                    onAddToPlaylist(song)
                                }
                            )

                            // Playback Speed & Pitch
                            DropdownMenuItem(
                                text = { Text("Speed & Pitch (${String.format("%.2f", currentSpeed)}x)", color = TextPrimary) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Speed,
                                        contentDescription = null,
                                        tint = PrimaryViolet
                                    )
                                },
                                onClick = {
                                    optionsMenuExpanded = false
                                    showSpeedPitchSheet = true
                                }
                            )

                            // Share Story Card
                            DropdownMenuItem(
                                text = { Text("Share Story Card", color = TextPrimary) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Image,
                                        contentDescription = null,
                                        tint = PrimaryViolet
                                    )
                                },
                                onClick = {
                                    optionsMenuExpanded = false
                                    coroutineScope.launch {
                                        NowPlayingCardGenerator.generateAndShareCard(context, song)
                                    }
                                }
                            )

                            // Tag Mood Playlists
                            onTagMood?.let { onTag ->
                                DropdownMenuItem(
                                    text = { Text("Tag Mood Playlists", color = TextPrimary) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Mood,
                                            contentDescription = null,
                                            tint = SecondaryAccent
                                        )
                                    },
                                    onClick = {
                                        optionsMenuExpanded = false
                                        onTag(song)
                                    }
                                )
                            }

                            // Song Info
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
                                        optionsMenuExpanded = false
                                        onInfo(song)
                                    }
                                )
                            }

                            // Share Audio File
                            onShareSong?.let { onShare ->
                                DropdownMenuItem(
                                    text = { Text("Share Audio File", color = TextPrimary) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Share,
                                            contentDescription = null,
                                            tint = PrimaryViolet
                                        )
                                    },
                                    onClick = {
                                        optionsMenuExpanded = false
                                        onShare(song)
                                    }
                                )
                            }

                            // Cut Ringtone
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
                                        optionsMenuExpanded = false
                                        onCut(song)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // (b) Main Area: Artwork OR Lyrics filling available space with weight(1f)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Crossfade(
                    targetState = showLyricsView,
                    label = "main_artwork_lyrics_crossfade",
                    modifier = Modifier.fillMaxSize()
                ) { isLyrics ->
                    if (isLyrics) {
                        // Lyrics View: Clipped to main area with soft 36dp fade at top and bottom
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clipToBounds(),
                            contentAlignment = Alignment.Center
                        ) {
                            if (lyrics.isEmpty()) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(16.dp),
                                    modifier = Modifier.padding(24.dp)
                                ) {
                                    Text(
                                        text = "No lyrics loaded yet.\nBeatWave automatically queries LRCLIB for line-by-line synced lyrics.",
                                        color = TextSecondary,
                                        textAlign = TextAlign.Center,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp)
                                    )
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Button(
                                            onClick = {
                                                coroutineScope.launch {
                                                    isFetchingLyrics = true
                                                    val fetched = LyricsRepository(context).getOrFetchLyrics(song)
                                                    if (fetched.isNotEmpty()) {
                                                        lyrics = fetched
                                                        Toast.makeText(context, "Lyrics synced!", Toast.LENGTH_SHORT).show()
                                                    } else {
                                                        Toast.makeText(context, "No lyrics found on LRCLIB", Toast.LENGTH_SHORT).show()
                                                    }
                                                    isFetchingLyrics = false
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = PrimaryViolet,
                                                contentColor = Color.White
                                            ),
                                            shape = RoundedCornerShape(20.dp),
                                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CloudDownload,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(if (isFetchingLyrics) "Searching..." else "Fetch from LRCLIB", fontSize = 13.sp)
                                        }

                                        Button(
                                            onClick = { lrcPickerLauncher.launch(arrayOf("*/*")) },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(0xFF1C1C26),
                                                contentColor = TextPrimary
                                            ),
                                            shape = RoundedCornerShape(20.dp),
                                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                                        ) {
                                            Text("Load .lrc file", fontSize = 13.sp)
                                        }
                                    }
                                }
                            } else {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "Line-by-line sync",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = TextSecondary.copy(alpha = 0.7f),
                                            fontSize = 11.sp,
                                            letterSpacing = 1.sp
                                        ),
                                        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxWidth()
                                    ) {
                                        LazyColumn(
                                            state = lyricsListState,
                                            modifier = Modifier
                                                .testTag("lyrics_list")
                                                .fillMaxSize(),
                                            contentPadding = PaddingValues(vertical = 48.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(16.dp)
                                        ) {
                                            itemsIndexed(lyrics) { index, lyricLine ->
                                                val isActive = index == activeLyricIndex
                                                Text(
                                                    text = lyricLine.text,
                                                    style = if (isActive) {
                                                        MaterialTheme.typography.titleLarge.copy(
                                                            color = Color.White,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 20.sp
                                                        )
                                                    } else {
                                                        MaterialTheme.typography.bodyLarge.copy(
                                                            color = TextSecondary.copy(alpha = 0.45f),
                                                            fontWeight = FontWeight.Normal,
                                                            fontSize = 15.sp
                                                        )
                                                    },
                                                    textAlign = TextAlign.Center,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable { onSeekTo(lyricLine.timeMs) }
                                                        .padding(horizontal = 16.dp, vertical = 4.dp)
                                                )
                                            }
                                        }

                                        // Soft 36dp fade at the top edge
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(36.dp)
                                                .align(Alignment.TopCenter)
                                                .background(
                                                    Brush.verticalGradient(
                                                        listOf(playerBaseColor, Color.Transparent)
                                                    )
                                                )
                                        )

                                        // Soft 36dp fade at the bottom edge
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(36.dp)
                                                .align(Alignment.BottomCenter)
                                                .background(
                                                    Brush.verticalGradient(
                                                        listOf(Color.Transparent, playerBaseColor)
                                                    )
                                                )
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // Artwork View: Tapping switches to lyrics
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable { showLyricsView = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.82f)
                                        .aspectRatio(1f)
                                        .shadow(16.dp, RoundedCornerShape(20.dp), spotColor = Color.Black.copy(alpha = 0.45f))
                                        .clip(RoundedCornerShape(20.dp))
                                        .border(1.dp, CardBorder, RoundedCornerShape(20.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    AlbumArtThumbnail(
                                        song = song,
                                        size = 320.dp,
                                        shapeRadius = 20.dp,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }

                                if (isVisualizerEnabled) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    AudioVisualizerBars(
                                        isPlaying = snapshot.isPlaying,
                                        phase = wavePhase,
                                        modifier = Modifier
                                            .fillMaxWidth(0.82f)
                                            .height(28.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // A-B Repeat Collapsible Row
            AnimatedVisibility(
                visible = showAbRepeatRow || snapshot.abRepeatA != null || snapshot.abRepeatB != null,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Loop,
                            contentDescription = null,
                            tint = if (snapshot.abRepeatA != null) PrimaryViolet else TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "A-B:",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextSecondary,
                                fontWeight = FontWeight.Medium
                            )
                        )
                        if (snapshot.abRepeatA != null) {
                            Text(
                                text = "[A: ${formatTime(snapshot.abRepeatA!!)}${if (snapshot.abRepeatB != null) " → B: " + formatTime(snapshot.abRepeatB!!) else " → set B"}]",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = PrimaryVioletLight,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(
                            modifier = Modifier
                                .testTag("ab_repeat_set_a")
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (snapshot.abRepeatA != null) PrimaryViolet else SurfaceHighlightDark)
                                .clickable { onSetPointA() }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Set A",
                                color = Color.White,
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .testTag("ab_repeat_set_b")
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (snapshot.abRepeatB != null) PrimaryVioletLight else SurfaceHighlightDark)
                                .clickable { onSetPointB() }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Set B",
                                color = Color.White,
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            )
                        }

                        if (snapshot.abRepeatA != null || snapshot.abRepeatB != null) {
                            Box(
                                modifier = Modifier
                                    .testTag("ab_repeat_clear")
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFEF4444).copy(alpha = 0.25f))
                                    .clickable { onClearABRepeat() }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "Clear",
                                    color = Color(0xFFEF4444),
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(SurfaceHighlightDark)
                                    .clickable { showAbRepeatRow = false }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "Hide",
                                    color = TextSecondary,
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // (c) Title & Artist with Favorite Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary,
                            fontSize = 22.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = song.artist,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = TextSecondary,
                            fontSize = 14.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = { onToggleFavorite(song) },
                    modifier = Modifier
                        .testTag("full_player_favorite")
                        .minimumInteractiveComponentSize()
                        .size(44.dp)
                ) {
                    Icon(
                        imageVector = if (song.isFavorite) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (song.isFavorite) AccentPink else TextMuted,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            if (showQualityBadges) {
                var audioQuality by remember(song.id) {
                    mutableStateOf(com.example.util.MediaQualityManager.getAudioQualitySync(song))
                }
                LaunchedEffect(song.id) {
                    audioQuality = com.example.util.MediaQualityManager.getAudioQuality(context, song)
                }
                DetailedAudioQualityStrip(
                    quality = audioQuality,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            } else {
                Spacer(modifier = Modifier.height(6.dp))
            }

            // (d) Seek Bar with Times: 4dp track, 12dp round thumb, PrimaryViolet played part
            Column(modifier = Modifier.fillMaxWidth()) {
                CustomSeekBar(
                    value = sliderValue,
                    onValueChange = { newVal ->
                        isDraggingSlider = true
                        dragPositionMs = newVal * totalDuration
                    },
                    onValueChangeFinished = {
                        onSeekTo(dragPositionMs.toLong())
                        isDraggingSlider = false
                    },
                    modifier = Modifier
                        .testTag("seek_slider")
                        .fillMaxWidth()
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 2.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatTime(currentPos),
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 12.sp)
                    )
                    Text(
                        text = formatTime(totalDuration),
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 12.sp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // (e) Controls Deck: Shuffle, Prev, Play/Pause, Next, Repeat
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Shuffle Button
                Box(contentAlignment = Alignment.Center) {
                    IconButton(
                        onClick = onToggleShuffle,
                        modifier = Modifier
                            .testTag("shuffle_button")
                            .minimumInteractiveComponentSize()
                            .size(42.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shuffle,
                            contentDescription = "Toggle Shuffle",
                            tint = if (snapshot.isShuffle) PrimaryViolet else TextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    if (snapshot.isShuffle) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(PrimaryViolet)
                        )
                    }
                }

                // Previous Button
                IconButton(
                    onClick = onSkipPrevious,
                    modifier = Modifier
                        .testTag("prev_button")
                        .minimumInteractiveComponentSize()
                        .size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous Track",
                        tint = TextSecondary,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Play / Pause Large Circular Button (White with dark icon #0B0B10)
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .shadow(12.dp, CircleShape, spotColor = Color.Black.copy(alpha = 0.35f))
                        .clip(CircleShape)
                        .background(Color.White)
                        .clickable { onTogglePlayPause() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (snapshot.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (snapshot.isPlaying) "Pause" else "Play",
                        tint = Color(0xFF0B0B10),
                        modifier = Modifier
                            .testTag("play_pause_button")
                            .size(32.dp)
                    )
                }

                // Next Button
                IconButton(
                    onClick = onSkipNext,
                    modifier = Modifier
                        .testTag("next_button")
                        .minimumInteractiveComponentSize()
                        .size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next Track",
                        tint = TextSecondary,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Repeat Mode Button
                Box(contentAlignment = Alignment.Center) {
                    IconButton(
                        onClick = onCycleRepeat,
                        modifier = Modifier
                            .testTag("repeat_button")
                            .minimumInteractiveComponentSize()
                            .size(42.dp)
                    ) {
                        Icon(
                            imageVector = if (snapshot.repeatMode == SongRepeatMode.ONE) Icons.Default.RepeatOne else Icons.Default.Repeat,
                            contentDescription = "Cycle Repeat Mode",
                            tint = if (snapshot.repeatMode != SongRepeatMode.OFF) PrimaryViolet else TextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    if (snapshot.repeatMode != SongRepeatMode.OFF) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(PrimaryViolet)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            // (f) "Up Next" chip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF1C1C26))
                        .clickable { showQueueSheet = true }
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                        contentDescription = "Queue",
                        tint = PrimaryViolet,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Up Next (${snapshot.queue.size})",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextPrimary,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp
                        )
                    )
                }
            }
        }
    }

    // Up Next Queue Bottom Sheet with Reorder
    if (showQueueSheet) {
        ModalBottomSheet(
            onDismissRequest = { showQueueSheet = false },
            containerColor = SurfaceDark,
            contentColor = TextPrimary,
            sheetState = rememberModalBottomSheetState()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Current Queue",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                    Text(
                        text = "${snapshot.queue.size} songs • Tap ▲ / ▼ to reorder",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(350.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(snapshot.queue) { index, queueSong ->
                        val isTrackCurrent = queueSong.id == snapshot.currentSong?.id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isTrackCurrent) SurfaceHighlightDark else Color.Transparent)
                                .clickable {
                                    onSelectQueueSong(queueSong)
                                    showQueueSheet = false
                                }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${index + 1}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = if (isTrackCurrent) SecondaryCyan else TextMuted,
                                    fontWeight = if (isTrackCurrent) FontWeight.Bold else FontWeight.Normal
                                ),
                                modifier = Modifier.width(26.dp)
                            )

                            AlbumArtThumbnail(
                                song = queueSong,
                                size = 38.dp,
                                shapeRadius = 8.dp
                            )

                            Spacer(modifier = Modifier.width(10.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = queueSong.title,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = if (isTrackCurrent) SecondaryCyan else TextPrimary,
                                        fontWeight = if (isTrackCurrent) FontWeight.Bold else FontWeight.Medium
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = queueSong.artist,
                                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            if (isTrackCurrent) {
                                MiniEqualizerBars(
                                    isPlaying = snapshot.isPlaying,
                                    color = SecondaryCyan
                                )
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (index > 0) {
                                        IconButton(
                                            onClick = { onMoveQueueItem(index, index - 1) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Text("▲", color = SecondaryCyan, fontSize = 12.sp)
                                        }
                                    }
                                    if (index < snapshot.queue.lastIndex) {
                                        IconButton(
                                            onClick = { onMoveQueueItem(index, index + 1) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Text("▼", color = SecondaryCyan, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Feature 2: Playback Speed & Pitch Bottom Sheet
    if (showSpeedPitchSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSpeedPitchSheet = false },
            containerColor = SurfaceDark,
            contentColor = TextPrimary,
            sheetState = rememberModalBottomSheetState()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 36.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = null,
                            tint = SecondaryCyan,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Playback Speed & Pitch",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                    }

                    IconButton(
                        onClick = {
                            currentSpeed = 1.0f
                            currentPitch = 1.0f
                            onSetPlaybackSpeedAndPitch?.invoke(1.0f, 1.0f)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.RestartAlt,
                            contentDescription = "Reset",
                            tint = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Speed Slider (0.5x to 2.5x)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Speed", style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary))
                    Text(
                        "${String.format("%.2f", currentSpeed)}x",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = SecondaryCyan
                        )
                    )
                }

                Slider(
                    value = currentSpeed,
                    onValueChange = {
                        currentSpeed = it
                        onSetPlaybackSpeedAndPitch?.invoke(currentSpeed, currentPitch)
                    },
                    valueRange = 0.5f..2.5f,
                    colors = SliderDefaults.colors(
                        thumbColor = SecondaryCyan,
                        activeTrackColor = SecondaryCyan,
                        inactiveTrackColor = SurfaceHighlightDark
                    )
                )

                // Quick speed presets
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { preset ->
                        val isSelected = kotlin.math.abs(currentSpeed - preset) < 0.04f
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) SecondaryCyan else SurfaceHighlightDark)
                                .clickable {
                                    currentSpeed = preset
                                    onSetPlaybackSpeedAndPitch?.invoke(preset, currentPitch)
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "${preset}x",
                                color = if (isSelected) Color.Black else TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Pitch Slider (0.5x to 2.0x)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Pitch", style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary))
                    Text(
                        "${String.format("%.2f", currentPitch)}x",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = PrimaryViolet
                        )
                    )
                }

                Slider(
                    value = currentPitch,
                    onValueChange = {
                        currentPitch = it
                        onSetPlaybackSpeedAndPitch?.invoke(currentSpeed, currentPitch)
                    },
                    valueRange = 0.5f..2.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = PrimaryViolet,
                        activeTrackColor = PrimaryViolet,
                        inactiveTrackColor = SurfaceHighlightDark
                    )
                )
            }
        }
    }
}

@Composable
fun AudioVisualizerBars(
    isPlaying: Boolean,
    phase: Float,
    modifier: Modifier = Modifier,
    barCount: Int = 24
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until barCount) {
            val wave = if (isPlaying) {
                (sin(Math.toRadians((phase + i * 15.0)).toFloat()) * 0.4f + 0.6f) *
                        (sin(Math.toRadians((phase * 1.5 + i * 25.0)).toFloat()) * 0.3f + 0.7f)
            } else {
                0.15f
            }

            val barHeightRatio = wave.coerceIn(0.1f, 1f)
            val barBrush = Brush.verticalGradient(
                listOf(
                    SecondaryCyan,
                    PrimaryNeon
                )
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height((32 * barHeightRatio).dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(barBrush)
            )
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
