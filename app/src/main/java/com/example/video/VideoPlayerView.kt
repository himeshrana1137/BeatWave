package com.example.video

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.ClosedCaption
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.MainActivity
import com.example.model.SubtitleLine
import com.example.model.VideoAspectRatio
import com.example.model.VideoItem
import com.example.ui.components.VideoInfoPanelDialog
import com.example.ui.theme.PrimaryNeon
import com.example.ui.theme.PrimaryViolet
import com.example.ui.theme.SecondaryCyan
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceHighlightDark
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.MediaQualityManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

data class AudioTrackInfo(val index: Int, val label: String)

data class SubtitleStyle(
    val textSizeSp: Int = 18,
    val textColor: Color = Color.White,
    val backgroundColor: Color = Color.Black.copy(alpha = 0.7f),
    val delayMs: Long = 0L
)

@Composable
fun VideoPlayerView(
    video: VideoItem,
    onClose: () -> Unit,
    onSaveProgress: (videoId: String, positionMs: Long) -> Unit,
    onAttachSubtitle: (videoId: String, subtitleUri: String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val mainActivity = context as? MainActivity
    val hapticFeedback = LocalHapticFeedback.current
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val prefs: SharedPreferences = remember { context.getSharedPreferences("beatwave_video_prefs", Context.MODE_PRIVATE) }

    // Check PiP capability on this device
    val isPiPSupported = remember {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                context.packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
    }

    // PiP state from MainActivity
    val isPiPActive by VideoPlayerState.isPiPActive.collectAsStateWithLifecycle()

    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(true) }
    var currentPositionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(video.durationMs.coerceAtLeast(1L)) }
    var videoWidth by remember { mutableIntStateOf(0) }
    var videoHeight by remember { mutableIntStateOf(0) }

    var isControlsVisible by remember { mutableStateOf(true) }
    var controlsTimeout by remember { mutableLongStateOf(System.currentTimeMillis() + 3500L) }

    var isFullscreen by remember { mutableStateOf(false) }
    var isScreenLocked by remember { mutableStateOf(false) }
    var isAudioOnlyMode by remember { mutableStateOf(false) }

    // Aspect Ratio with persistence
    var aspectRatio by remember {
        val saved = prefs.getString("aspect_${video.id}", null)
        val initial = VideoAspectRatio.entries.find { it.name == saved } ?: VideoAspectRatio.FIT
        mutableStateOf(initial)
    }

    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
    var showSpeedMenu by remember { mutableStateOf(false) }

    // Audio Track Selection
    var availableAudioTracks by remember { mutableStateOf<List<AudioTrackInfo>>(emptyList()) }
    var selectedAudioTrackIndex by remember { mutableIntStateOf(-1) }
    var showAudioTrackMenu by remember { mutableStateOf(false) }

    // Gestures HUD feedback
    var gestureHudText by remember { mutableStateOf<String?>(null) }
    var doubleTapSeekFeedback by remember { mutableStateOf<String?>(null) }
    var showResumeToast by remember { mutableStateOf(false) }

    // Video Technical Info & Codec Support
    var showInfoPanelDialog by remember { mutableStateOf(false) }
    var videoQuality by remember(video.id) { mutableStateOf(MediaQualityManager.getVideoQualitySync(video)) }
    var playbackErrorMsg by remember { mutableStateOf<String?>(null) }

    // Subtitle management & customization
    var subtitles by remember { mutableStateOf<List<SubtitleLine>>(emptyList()) }
    var currentSubtitleText by remember { mutableStateOf<String?>(null) }
    var showSubtitleDialog by remember { mutableStateOf(false) }
    var showSubtitleMenu by remember { mutableStateOf(false) }

    var subtitleStyle by remember {
        val size = prefs.getInt("sub_size_${video.id}", 18)
        val colorInt = prefs.getInt("sub_color_${video.id}", 0xFFFFFFFF.toInt())
        val bgInt = prefs.getInt("sub_bg_${video.id}", 0xB3000000.toInt())
        val delayMs = prefs.getLong("sub_delay_${video.id}", 0L)
        mutableStateOf(
            SubtitleStyle(
                textSizeSp = size,
                textColor = Color(colorInt),
                backgroundColor = Color(bgInt),
                delayMs = delayMs
            )
        )
    }

    // Synchronize play state with VideoPlayerState for PiP actions
    LaunchedEffect(isPlaying) {
        VideoPlayerState.notifyPlayStateChanged(isPlaying)
        mainActivity?.updatePiPParams(isPlaying)
    }

    // Listen for remote PiP play/pause toggles
    LaunchedEffect(Unit) {
        VideoPlayerState.pipPlayPauseToggle.collect {
            mediaPlayer?.let { player ->
                if (player.isPlaying) {
                    player.pause()
                    isPlaying = false
                } else {
                    player.start()
                    isPlaying = true
                }
                controlsTimeout = System.currentTimeMillis() + 3500L
            }
        }
    }

    // Codec support check
    LaunchedEffect(video.id) {
        val q = MediaQualityManager.getVideoQuality(context, video)
        videoQuality = q
        if (!q.isSupported) {
            playbackErrorMsg = "Unsupported video codec (${q.videoCodec ?: "Unknown"}). This device does not have a decoder for this video format."
        }
    }

    // Subtitle file picker launcher (.srt and .vtt)
    val subtitlePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            onAttachSubtitle(video.id, uri.toString())
            subtitles = SubtitleParser.parseSrtFromUri(context, uri)
            gestureHudText = "💬 Subtitle Loaded"
        }
    }

    // Auto-detect sibling subtitle file if not explicitly attached
    LaunchedEffect(video.id) {
        if (!video.subtitleUriString.isNullOrBlank()) {
            try {
                subtitles = SubtitleParser.parseSrtFromUri(context, Uri.parse(video.subtitleUriString))
            } catch (_: Exception) {}
        } else {
            withContext(Dispatchers.IO) {
                val detected = SubtitleParser.autoDetectSubtitle(context, video.uriString)
                if (detected != null) {
                    val parsed = SubtitleParser.parseSrtFromUri(context, detected)
                    withContext(Dispatchers.Main) {
                        subtitles = parsed
                        onAttachSubtitle(video.id, detected.toString())
                        gestureHudText = "💬 Subtitle Auto-detected"
                    }
                }
            }
        }
    }

    // Back handler
    BackHandler {
        if (isScreenLocked) {
            isScreenLocked = false
            gestureHudText = "🔓 Controls Unlocked"
        } else if (isFullscreen) {
            toggleFullscreen(activity, false)
            isFullscreen = false
        } else {
            mediaPlayer?.let { onSaveProgress(video.id, it.currentPosition.toLong()) }
            onClose()
        }
    }

    // Auto-hide controls timer
    LaunchedEffect(controlsTimeout, isControlsVisible, isPlaying, isScreenLocked, isPiPActive) {
        if (isPiPActive) {
            isControlsVisible = false
        } else if (isControlsVisible && isPlaying && !isScreenLocked) {
            while (System.currentTimeMillis() < controlsTimeout) {
                delay(500)
            }
            isControlsVisible = false
        }
    }

    // Position ticker and subtitle matching
    LaunchedEffect(isPlaying, subtitleStyle.delayMs) {
        while (true) {
            mediaPlayer?.let { player ->
                if (player.isPlaying) {
                    val pos = player.currentPosition.toLong()
                    currentPositionMs = pos
                    val dur = player.duration.toLong()
                    if (dur > 0) durationMs = dur

                    // Match subtitle respecting custom delay
                    val adjustedPos = pos - subtitleStyle.delayMs
                    val activeSub = subtitles.find { adjustedPos in it.startMs..it.endMs }
                    currentSubtitleText = activeSub?.text
                }
            }
            delay(250)
        }
    }

    // Cleanup when leaving
    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.let { player ->
                try {
                    onSaveProgress(video.id, player.currentPosition.toLong())
                    player.stop()
                    player.release()
                } catch (_: Exception) {}
            }
            mediaPlayer = null
            VideoPlayerState.notifyPlayStateChanged(false)
            activity?.let {
                toggleFullscreen(it, false)
                it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
        }
    }

    Box(
        modifier = modifier
            .testTag("video_player_container")
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Video Surface or Audio-Only Display
        if (isAudioOnlyMode) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0F111A)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .clip(CircleShape)
                            .background(SurfaceHighlightDark)
                            .border(2.dp, PrimaryNeon, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Headphones,
                            contentDescription = "Audio-only Mode",
                            tint = SecondaryCyan,
                            modifier = Modifier.size(48.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = video.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Audio-only playback (Screen display dimmed)",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                    )
                }
            }
        } else {
            // Adaptive SurfaceView sizing based on Aspect Ratio Mode (Fit / Fill / Zoom / Original)
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .clipToBounds(),
                contentAlignment = Alignment.Center
            ) {
                val screenW = maxWidth
                val screenH = maxHeight

                val vW = if (videoWidth > 0) videoWidth.toFloat() else 16f
                val vH = if (videoHeight > 0) videoHeight.toFloat() else 9f
                val videoAspect = vW / vH
                val screenAspect = if (screenH.value > 0) screenW.value / screenH.value else videoAspect

                val surfaceModifier = when (aspectRatio) {
                    VideoAspectRatio.FIT -> {
                        if (screenAspect > videoAspect) {
                            Modifier.size(screenH * videoAspect, screenH)
                        } else {
                            Modifier.size(screenW, screenW / videoAspect)
                        }
                    }
                    VideoAspectRatio.FILL -> {
                        if (screenAspect > videoAspect) {
                            Modifier.size(screenW, screenW / videoAspect)
                        } else {
                            Modifier.size(screenH * videoAspect, screenH)
                        }
                    }
                    VideoAspectRatio.ZOOM -> {
                        val base = if (screenAspect > videoAspect) {
                            Pair(screenH * videoAspect, screenH)
                        } else {
                            Pair(screenW, screenW / videoAspect)
                        }
                        Modifier.size(base.first * 1.35f, base.second * 1.35f)
                    }
                    VideoAspectRatio.ORIGINAL -> {
                        if (videoWidth > 0 && videoHeight > 0) {
                            Modifier.size(videoWidth.dp, videoHeight.dp)
                        } else {
                            Modifier.fillMaxSize()
                        }
                    }
                }

                AndroidView(
                    factory = { ctx ->
                        SurfaceView(ctx).apply {
                            holder.addCallback(object : SurfaceHolder.Callback {
                                override fun surfaceCreated(holder: SurfaceHolder) {
                                    try {
                                        val existing = mediaPlayer
                                        if (existing != null) {
                                            existing.setDisplay(holder)
                                            return
                                        }

                                        val player = MediaPlayer().apply {
                                            setAudioAttributes(
                                                AudioAttributes.Builder()
                                                    .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
                                                    .setUsage(AudioAttributes.USAGE_MEDIA)
                                                    .build()
                                            )
                                            setDisplay(holder)
                                            setDataSource(ctx, Uri.parse(video.uriString))
                                            prepareAsync()
                                            setOnPreparedListener { mp ->
                                                videoWidth = mp.videoWidth
                                                videoHeight = mp.videoHeight

                                                // Auto-rotate to landscape based on aspect ratio if device rotation lock allows
                                                if (mp.videoWidth > mp.videoHeight) {
                                                    try {
                                                        val autoRotateOn = Settings.System.getInt(
                                                            ctx.contentResolver,
                                                            Settings.System.ACCELEROMETER_ROTATION,
                                                            0
                                                        ) == 1
                                                        if (autoRotateOn) {
                                                            activity?.requestedOrientation =
                                                                ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                                                        }
                                                    } catch (_: Exception) {}
                                                }

                                                if (video.lastPositionMs > 5000L && video.lastPositionMs < mp.duration - 5000L) {
                                                    mp.seekTo(video.lastPositionMs.toInt())
                                                    showResumeToast = true
                                                }
                                                mp.start()
                                                isPlaying = true
                                                durationMs = mp.duration.toLong()

                                                // Detect available audio tracks
                                                try {
                                                    val trackInfo = mp.trackInfo
                                                    val audioTracks = mutableListOf<AudioTrackInfo>()
                                                    trackInfo.forEachIndexed { idx, info ->
                                                        if (info.trackType == MediaPlayer.TrackInfo.MEDIA_TRACK_TYPE_AUDIO) {
                                                            val lang = info.language?.takeIf { it.isNotBlank() && it != "und" }
                                                                ?: "Track ${audioTracks.size + 1}"
                                                            audioTracks.add(AudioTrackInfo(idx, "Audio ${audioTracks.size + 1} ($lang)"))
                                                        }
                                                    }
                                                    availableAudioTracks = audioTracks
                                                    if (audioTracks.isNotEmpty()) {
                                                        selectedAudioTrackIndex = audioTracks.first().index
                                                    }
                                                } catch (_: Exception) {}
                                            }
                                            setOnVideoSizeChangedListener { _, width, height ->
                                                videoWidth = width
                                                videoHeight = height
                                            }
                                            setOnCompletionListener {
                                                isPlaying = false
                                                isControlsVisible = true
                                            }
                                            setOnErrorListener { _, what, extra ->
                                                playbackErrorMsg = "Playback error ($what, $extra): Codec or media format cannot be decoded on this device."
                                                true
                                            }
                                        }
                                        mediaPlayer = player
                                    } catch (e: Exception) {
                                        playbackErrorMsg = "Unable to initialize video player: ${e.message}"
                                    }
                                }

                                override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}

                                override fun surfaceDestroyed(holder: SurfaceHolder) {
                                    mediaPlayer?.setDisplay(null)
                                }
                            })
                        }
                    },
                    modifier = surfaceModifier
                )
            }
        }

        // Touch & Gesture Detection Layer (Disabled when locked or in PiP)
        if (!isScreenLocked && !isPiPActive) {
            Row(modifier = Modifier.fillMaxSize()) {
                // Left half: Brightness Drag & Seek -10s
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = {
                                    isControlsVisible = !isControlsVisible
                                    controlsTimeout = System.currentTimeMillis() + 3500L
                                },
                                onDoubleTap = {
                                    mediaPlayer?.let { player ->
                                        val newPos = (player.currentPosition - 10000).coerceAtLeast(0)
                                        player.seekTo(newPos)
                                        currentPositionMs = newPos.toLong()
                                        doubleTapSeekFeedback = "⏪ 10s"
                                        controlsTimeout = System.currentTimeMillis() + 3500L
                                    }
                                }
                            )
                        }
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                activity?.let { act ->
                                    val currentBrightness = act.window.attributes.screenBrightness.let { if (it < 0) 0.5f else it }
                                    val delta = -dragAmount.y / 600f
                                    val newBrightness = (currentBrightness + delta).coerceIn(0.05f, 1.0f)
                                    val lp = act.window.attributes
                                    lp.screenBrightness = newBrightness
                                    act.window.attributes = lp
                                    gestureHudText = "☀️ ${(newBrightness * 100).toInt()}%"
                                    controlsTimeout = System.currentTimeMillis() + 2000L
                                }
                            }
                        }
                )

                // Right half: Volume Drag & Seek +10s
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = {
                                    isControlsVisible = !isControlsVisible
                                    controlsTimeout = System.currentTimeMillis() + 3500L
                                },
                                onDoubleTap = {
                                    mediaPlayer?.let { player ->
                                        val newPos = (player.currentPosition + 10000).coerceAtMost(player.duration)
                                        player.seekTo(newPos)
                                        currentPositionMs = newPos.toLong()
                                        doubleTapSeekFeedback = "10s ⏩"
                                        controlsTimeout = System.currentTimeMillis() + 3500L
                                    }
                                }
                            )
                        }
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                                val currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                                if (dragAmount.y < -15 && currentVol < maxVol) {
                                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, currentVol + 1, 0)
                                    gestureHudText = "🔊 ${((currentVol + 1) * 100 / maxVol)}%"
                                } else if (dragAmount.y > 15 && currentVol > 0) {
                                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, currentVol - 1, 0)
                                    gestureHudText = "🔉 ${((currentVol - 1) * 100 / maxVol)}%"
                                }
                                controlsTimeout = System.currentTimeMillis() + 2000L
                            }
                        }
                )
            }
        } else if (isScreenLocked && !isPiPActive) {
            // Screen Locked: Single tap shows lock icon; accidental touches do nothing
            var showLockIconTemporary by remember { mutableStateOf(false) }

            LaunchedEffect(showLockIconTemporary) {
                if (showLockIconTemporary) {
                    delay(3000)
                    showLockIconTemporary = false
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                showLockIconTemporary = true
                                gestureHudText = "Press & hold lock icon to unlock"
                            }
                        )
                    }
            )

            // Small Floating Lock Icon: Tapping explains; Long-press unlocks
            if (showLockIconTemporary) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .testTag("video_unlock_button")
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.8f))
                            .border(1.5.dp, SecondaryCyan, CircleShape)
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onTap = {
                                        gestureHudText = "Press & hold lock icon to unlock"
                                    },
                                    onLongPress = {
                                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                        isScreenLocked = false
                                        isControlsVisible = true
                                        gestureHudText = "🔓 Controls Unlocked"
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Long-press to Unlock",
                            tint = SecondaryCyan,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }
        }

        // Swipe HUD (Brightness / Volume)
        if (!isPiPActive) {
            LaunchedEffect(gestureHudText) {
                if (gestureHudText != null) {
                    delay(1400)
                    gestureHudText = null
                }
            }

            gestureHudText?.let { hud ->
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black.copy(alpha = 0.8f))
                        .padding(horizontal = 24.dp, vertical = 14.dp)
                ) {
                    Text(
                        text = hud,
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            // Double-tap visual feedback badge
            LaunchedEffect(doubleTapSeekFeedback) {
                if (doubleTapSeekFeedback != null) {
                    delay(800)
                    doubleTapSeekFeedback = null
                }
            }

            doubleTapSeekFeedback?.let { feedback ->
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black.copy(alpha = 0.75f))
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = feedback,
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            // Resume notice toast
            LaunchedEffect(showResumeToast) {
                if (showResumeToast) {
                    delay(3000)
                    showResumeToast = false
                }
            }

            if (showResumeToast) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 80.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(SurfaceDark.copy(alpha = 0.9f))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Resumed from ${formatDuration(video.lastPositionMs)}",
                        color = SecondaryCyan,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
                    )
                }
            }
        }

        // Subtitle Overlay (Formatted according to SubtitleStyle)
        currentSubtitleText?.let { subText ->
            if (!isPiPActive) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = if (isControlsVisible) 95.dp else 28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(subtitleStyle.backgroundColor)
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = subText,
                        color = subtitleStyle.textColor,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = subtitleStyle.textSizeSp.sp,
                            textAlign = TextAlign.Center
                        ),
                        modifier = Modifier.fillMaxWidth(0.9f)
                    )
                }
            } else {
                // In PiP mode, show compact text at bottom
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 6.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.7f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = subText,
                        color = subtitleStyle.textColor,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Controls Overlay (Hidden when locked or in Picture-in-Picture)
        AnimatedVisibility(
            visible = isControlsVisible && !isScreenLocked && !isPiPActive,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
            ) {
                // Top Action Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = {
                            mediaPlayer?.let { onSaveProgress(video.id, it.currentPosition.toLong()) }
                            onClose()
                        },
                        modifier = Modifier
                            .testTag("video_back_button")
                            .minimumInteractiveComponentSize()
                            .size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }

                    Text(
                        text = video.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 6.dp)
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Subtitle (.srt / .vtt) Options Menu
                        Box {
                            IconButton(
                                onClick = { showSubtitleMenu = true },
                                modifier = Modifier
                                    .testTag("video_subtitle_button")
                                    .minimumInteractiveComponentSize()
                                    .size(38.dp)
                            ) {
                                Icon(
                                    imageVector = if (subtitles.isNotEmpty()) Icons.Filled.ClosedCaption else Icons.Outlined.ClosedCaption,
                                    contentDescription = "Subtitles",
                                    tint = if (subtitles.isNotEmpty()) SecondaryCyan else Color.White
                                )
                            }

                            DropdownMenu(
                                expanded = showSubtitleMenu,
                                onDismissRequest = { showSubtitleMenu = false },
                                modifier = Modifier.background(SurfaceHighlightDark)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Load Subtitle (.srt, .vtt)...", color = TextPrimary) },
                                    onClick = {
                                        showSubtitleMenu = false
                                        subtitlePicker.launch(arrayOf("text/*", "application/x-subrip", "text/vtt", "*/*"))
                                    }
                                )
                                if (subtitles.isNotEmpty()) {
                                    DropdownMenuItem(
                                        text = { Text("Subtitle Options & Delay", color = SecondaryCyan) },
                                        onClick = {
                                            showSubtitleMenu = false
                                            showSubtitleDialog = true
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Disable Subtitles", color = Color(0xFFFF5252)) },
                                        onClick = {
                                            showSubtitleMenu = false
                                            subtitles = emptyList()
                                            currentSubtitleText = null
                                            onAttachSubtitle(video.id, null)
                                            gestureHudText = "Subtitles Off"
                                        }
                                    )
                                }
                            }
                        }

                        // Picture-in-Picture (PiP) Button (Hidden if device does not support PiP)
                        if (isPiPSupported) {
                            IconButton(
                                onClick = {
                                    mainActivity?.enterVideoPiP()
                                },
                                modifier = Modifier
                                    .testTag("video_pip_button")
                                    .minimumInteractiveComponentSize()
                                    .size(38.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PictureInPictureAlt,
                                    contentDescription = "Picture in Picture",
                                    tint = Color.White
                                )
                            }
                        }

                        // Screenshot Capture Button
                        IconButton(
                            onClick = {
                                captureVideoScreenshot(context, video.uriString, currentPositionMs) {
                                    gestureHudText = "📸 Screenshot Saved"
                                }
                            },
                            modifier = Modifier
                                .testTag("video_screenshot_button")
                                .minimumInteractiveComponentSize()
                                .size(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Screenshot",
                                tint = Color.White
                            )
                        }

                        // Audio-Only Mode Toggle
                        IconButton(
                            onClick = {
                                isAudioOnlyMode = !isAudioOnlyMode
                                gestureHudText = if (isAudioOnlyMode) "🎧 Audio-Only Mode ON" else "🎬 Video Mode ON"
                            },
                            modifier = Modifier
                                .testTag("video_audio_only_button")
                                .minimumInteractiveComponentSize()
                                .size(38.dp)
                        ) {
                            Icon(
                                imageVector = if (isAudioOnlyMode) Icons.Default.Headphones else Icons.Default.Videocam,
                                contentDescription = "Audio-only toggle",
                                tint = if (isAudioOnlyMode) SecondaryCyan else Color.White
                            )
                        }

                        // Audio Track Selection Menu
                        if (availableAudioTracks.size > 1) {
                            Box {
                                IconButton(
                                    onClick = { showAudioTrackMenu = true },
                                    modifier = Modifier
                                        .testTag("video_audio_track_button")
                                        .minimumInteractiveComponentSize()
                                        .size(38.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.GraphicEq,
                                            contentDescription = "Audio Track Selection",
                                            tint = SecondaryCyan
                                        )
                                    }

                                DropdownMenu(
                                    expanded = showAudioTrackMenu,
                                    onDismissRequest = { showAudioTrackMenu = false },
                                    modifier = Modifier.background(SurfaceHighlightDark)
                                ) {
                                    availableAudioTracks.forEach { track ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = track.label,
                                                    color = if (selectedAudioTrackIndex == track.index) SecondaryCyan else TextPrimary,
                                                    fontWeight = if (selectedAudioTrackIndex == track.index) FontWeight.Bold else FontWeight.Normal
                                                )
                                            },
                                            onClick = {
                                                selectedAudioTrackIndex = track.index
                                                showAudioTrackMenu = false
                                                mediaPlayer?.let { player ->
                                                    try {
                                                        player.selectTrack(track.index)
                                                        gestureHudText = "🔊 ${track.label}"
                                                    } catch (_: Exception) {}
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Aspect Ratio Toggle Button (Cycles Fit -> Fill -> Zoom -> Original)
                        IconButton(
                            onClick = {
                                aspectRatio = when (aspectRatio) {
                                    VideoAspectRatio.FIT -> VideoAspectRatio.FILL
                                    VideoAspectRatio.FILL -> VideoAspectRatio.ZOOM
                                    VideoAspectRatio.ZOOM -> VideoAspectRatio.ORIGINAL
                                    VideoAspectRatio.ORIGINAL -> VideoAspectRatio.FIT
                                }
                                prefs.edit().putString("aspect_${video.id}", aspectRatio.name).apply()
                                gestureHudText = "Aspect: ${aspectRatio.label}"
                            },
                            modifier = Modifier
                                .testTag("video_aspect_ratio_button")
                                .minimumInteractiveComponentSize()
                                .size(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AspectRatio,
                                contentDescription = "Aspect Ratio: ${aspectRatio.label}",
                                tint = Color.White
                            )
                        }

                        // Playback Speed Button
                        Box {
                            IconButton(
                                onClick = { showSpeedMenu = true },
                                modifier = Modifier
                                    .testTag("video_speed_button")
                                    .minimumInteractiveComponentSize()
                                    .size(38.dp)
                            ) {
                                Text(
                                    text = "${playbackSpeed}x",
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                                )
                            }

                            DropdownMenu(
                                expanded = showSpeedMenu,
                                onDismissRequest = { showSpeedMenu = false },
                                modifier = Modifier.background(SurfaceHighlightDark)
                            ) {
                                listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { speed ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = "${speed}x",
                                                color = if (playbackSpeed == speed) SecondaryCyan else TextPrimary,
                                                fontWeight = if (playbackSpeed == speed) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        onClick = {
                                            playbackSpeed = speed
                                            showSpeedMenu = false
                                            mediaPlayer?.let { player ->
                                                try {
                                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                                        player.playbackParams = PlaybackParams().apply { setSpeed(speed) }
                                                    }
                                                } catch (_: Exception) {}
                                            }
                                        }
                                    )
                                }
                            }
                        }

                        // Screen Lock Button (Disables all touches and gestures)
                        IconButton(
                            onClick = {
                                isScreenLocked = true
                                isControlsVisible = false
                                gestureHudText = "🔒 Controls Locked"
                            },
                            modifier = Modifier
                                .testTag("video_lock_button")
                                .minimumInteractiveComponentSize()
                                .size(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LockOpen,
                                contentDescription = "Lock Controls",
                                tint = Color.White
                            )
                        }

                        // Video Technical Info Panel Toggle Button
                        IconButton(
                            onClick = { showInfoPanelDialog = true },
                            modifier = Modifier
                                .testTag("video_info_panel_button")
                                .minimumInteractiveComponentSize()
                                .size(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Video Technical Info",
                                tint = SecondaryCyan
                            )
                        }
                    }
                }

                // Center Play / Pause Action Button
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.65f))
                        .clickable {
                            mediaPlayer?.let { player ->
                                if (player.isPlaying) {
                                    player.pause()
                                    isPlaying = false
                                } else {
                                    player.start()
                                    isPlaying = true
                                }
                                controlsTimeout = System.currentTimeMillis() + 3500L
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color.White,
                        modifier = Modifier
                            .testTag("video_play_pause")
                            .size(36.dp)
                    )
                }

                // Bottom Seekbar & Controls
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    var isSeeking by remember { mutableStateOf(false) }
                    var seekVal by remember { mutableFloatStateOf(0f) }

                    val progress = if (isSeeking) {
                        seekVal
                    } else if (durationMs > 0) {
                        (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
                    } else 0f

                    Slider(
                        value = progress,
                        onValueChange = {
                            isSeeking = true
                            seekVal = it
                            controlsTimeout = System.currentTimeMillis() + 5000L
                        },
                        onValueChangeFinished = {
                            mediaPlayer?.let { player ->
                                val target = (seekVal * durationMs).toLong()
                                player.seekTo(target.toInt())
                                currentPositionMs = target
                            }
                            isSeeking = false
                        },
                        modifier = Modifier
                            .testTag("video_seek_slider")
                            .fillMaxWidth(),
                        colors = SliderDefaults.colors(
                            thumbColor = SecondaryCyan,
                            activeTrackColor = SecondaryCyan,
                            inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${formatDuration(currentPositionMs)} / ${formatDuration(durationMs)}",
                            color = Color.White,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Manual Orientation Toggle Button (Landscape <-> Portrait)
                            IconButton(
                                onClick = {
                                    activity?.let { act ->
                                        val isCurrentlyLandscape = act.requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE ||
                                                act.requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                                        act.requestedOrientation = if (isCurrentlyLandscape) {
                                            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                        } else {
                                            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .testTag("video_rotate_button")
                                    .minimumInteractiveComponentSize()
                                    .size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ScreenRotation,
                                    contentDescription = "Rotate Screen",
                                    tint = Color.White
                                )
                            }

                            // Fullscreen Toggle Button
                            IconButton(
                                onClick = {
                                    isFullscreen = !isFullscreen
                                    toggleFullscreen(activity, isFullscreen)
                                },
                                modifier = Modifier
                                    .testTag("video_fullscreen_button")
                                    .minimumInteractiveComponentSize()
                                    .size(36.dp)
                            ) {
                                Icon(
                                    imageVector = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                    contentDescription = "Fullscreen",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }

        // Subtitle Options & Delay Dialog
        if (showSubtitleDialog) {
            AlertDialog(
                onDismissRequest = { showSubtitleDialog = false },
                containerColor = SurfaceDark,
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.ClosedCaption, contentDescription = null, tint = SecondaryCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Subtitle Options", color = TextPrimary, fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        // Text Size Selection
                        Column {
                            Text("Font Size", color = TextSecondary, style = MaterialTheme.typography.labelMedium)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(14 to "Small", 18 to "Medium", 22 to "Large", 26 to "XL").forEach { (sz, label) ->
                                    val isSel = subtitleStyle.textSizeSp == sz
                                    FilterChip(
                                        selected = isSel,
                                        onClick = {
                                            val updated = subtitleStyle.copy(textSizeSp = sz)
                                            subtitleStyle = updated
                                            prefs.edit().putInt("sub_size_${video.id}", sz).apply()
                                        },
                                        label = { Text(label, fontSize = 11.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = PrimaryViolet,
                                            selectedLabelColor = Color.White
                                        )
                                    )
                                }
                            }
                        }

                        // Text Color Selection
                        Column {
                            Text("Text Color", color = TextSecondary, style = MaterialTheme.typography.labelMedium)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                val colorOptions = listOf(
                                    Color.White to "White",
                                    Color(0xFFFFEB3B) to "Yellow",
                                    Color(0xFF00E5FF) to "Cyan",
                                    Color(0xFF00E676) to "Green"
                                )
                                colorOptions.forEach { (col, name) ->
                                    val isSel = subtitleStyle.textColor == col
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(col)
                                            .border(
                                                width = if (isSel) 3.dp else 1.dp,
                                                color = if (isSel) PrimaryNeon else Color.Gray,
                                                shape = CircleShape
                                            )
                                            .clickable {
                                                val updated = subtitleStyle.copy(textColor = col)
                                                subtitleStyle = updated
                                                prefs.edit().putInt("sub_color_${video.id}", col.value.toInt()).apply()
                                            }
                                    )
                                }
                            }
                        }

                        // Background Opacity Selection
                        Column {
                            Text("Background", color = TextSecondary, style = MaterialTheme.typography.labelMedium)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                val bgOptions = listOf(
                                    Color.Black.copy(alpha = 0.7f) to "Translucent",
                                    Color.Black to "Solid Black",
                                    Color.Transparent to "None"
                                )
                                bgOptions.forEach { (bg, label) ->
                                    val isSel = subtitleStyle.backgroundColor == bg
                                    FilterChip(
                                        selected = isSel,
                                        onClick = {
                                            val updated = subtitleStyle.copy(backgroundColor = bg)
                                            subtitleStyle = updated
                                            prefs.edit().putInt("sub_bg_${video.id}", bg.value.toInt()).apply()
                                        },
                                        label = { Text(label, fontSize = 11.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = PrimaryViolet,
                                            selectedLabelColor = Color.White
                                        )
                                    )
                                }
                            }
                        }

                        // Delay Sync (+/- 0.5s steps)
                        Column {
                            Text("Timing Delay", color = TextSecondary, style = MaterialTheme.typography.labelMedium)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    OutlinedButton(
                                        onClick = {
                                            val newDelay = subtitleStyle.delayMs - 500L
                                            val updated = subtitleStyle.copy(delayMs = newDelay)
                                            subtitleStyle = updated
                                            prefs.edit().putLong("sub_delay_${video.id}", newDelay).apply()
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp)
                                    ) {
                                        Text("-0.5s", color = TextPrimary, fontSize = 12.sp)
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Text(
                                        text = "${(subtitleStyle.delayMs / 1000.0)}s",
                                        color = SecondaryCyan,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                    )

                                    Spacer(modifier = Modifier.width(8.dp))

                                    OutlinedButton(
                                        onClick = {
                                            val newDelay = subtitleStyle.delayMs + 500L
                                            val updated = subtitleStyle.copy(delayMs = newDelay)
                                            subtitleStyle = updated
                                            prefs.edit().putLong("sub_delay_${video.id}", newDelay).apply()
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp)
                                    ) {
                                        Text("+0.5s", color = TextPrimary, fontSize = 12.sp)
                                    }
                                }

                                TextButton(
                                    onClick = {
                                        val updated = subtitleStyle.copy(delayMs = 0L)
                                        subtitleStyle = updated
                                        prefs.edit().putLong("sub_delay_${video.id}", 0L).apply()
                                    }
                                ) {
                                    Text("Reset", color = TextSecondary, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { showSubtitleDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryViolet)
                    ) {
                        Text("Done", color = Color.White)
                    }
                }
            )
        }

        // Technical Video Info Dialog Panel
        if (showInfoPanelDialog) {
            VideoInfoPanelDialog(
                video = video,
                quality = videoQuality,
                onDismiss = { showInfoPanelDialog = false }
            )
        }

        // Unsupported Codec / Media Error Overlay
        if (playbackErrorMsg != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.94f))
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Playback Error",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Codec Not Supported",
                        style = MaterialTheme.typography.titleLarge.copy(color = Color.White, fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = playbackErrorMsg ?: "This device does not support decoding this video format.",
                        style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary, textAlign = TextAlign.Center)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = {
                            mediaPlayer?.let { onSaveProgress(video.id, it.currentPosition.toLong()) }
                            onClose()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryNeon),
                        modifier = Modifier.testTag("unsupported_codec_close_button")
                    ) {
                        Text("Go Back", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

private fun captureVideoScreenshot(
    context: Context,
    uriString: String,
    positionMs: Long,
    onSuccess: () -> Unit
) {
    try {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(context, Uri.parse(uriString))
        val frame = retriever.getFrameAtTime(positionMs * 1000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
        if (frame != null) {
            val screenshotDir = File(context.filesDir, "screenshots")
            if (!screenshotDir.exists()) screenshotDir.mkdirs()
            val file = File(screenshotDir, "screenshot_${System.currentTimeMillis()}.jpg")
            FileOutputStream(file).use { fos ->
                frame.compress(Bitmap.CompressFormat.JPEG, 90, fos)
            }
            onSuccess()
        }
        retriever.release()
    } catch (_: Exception) {}
}

private fun toggleFullscreen(activity: Activity?, isFullscreen: Boolean) {
    activity?.window?.let { window ->
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        if (isFullscreen) {
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }
}

private fun formatDuration(millis: Long): String {
    if (millis <= 0) return "00:00"
    val totalSec = millis / 1000
    val min = totalSec / 60
    val sec = totalSec % 60
    return String.format("%02d:%02d", min, sec)
}
