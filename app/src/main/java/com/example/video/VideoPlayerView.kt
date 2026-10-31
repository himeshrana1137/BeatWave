package com.example.video

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.net.Uri
import android.os.Build
import android.util.Rational
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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.outlined.ClosedCaption
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
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
import com.example.model.SubtitleLine
import com.example.model.VideoAspectRatio
import com.example.model.VideoItem
import com.example.ui.theme.PrimaryNeon
import com.example.ui.theme.SecondaryCyan
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceHighlightDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

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
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }

    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(true) }
    var currentPositionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(video.durationMs.coerceAtLeast(1L)) }
    var isControlsVisible by remember { mutableStateOf(true) }
    var controlsTimeout by remember { mutableLongStateOf(System.currentTimeMillis() + 3500L) }

    var isFullscreen by remember { mutableStateOf(false) }
    var isScreenLocked by remember { mutableStateOf(false) }
    var isAudioOnlyMode by remember { mutableStateOf(false) }
    var aspectRatio by remember { mutableStateOf(VideoAspectRatio.FIT) }

    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
    var showSpeedMenu by remember { mutableStateOf(false) }

    // Gestures HUD feedback
    var gestureHudText by remember { mutableStateOf<String?>(null) }
    var doubleTapSeekFeedback by remember { mutableStateOf<String?>(null) }
    var showResumeToast by remember { mutableStateOf(false) }

    // Subtitles
    var subtitles by remember { mutableStateOf<List<SubtitleLine>>(emptyList()) }
    var currentSubtitleText by remember { mutableStateOf<String?>(null) }

    // Subtitle picker launcher
    val subtitlePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            onAttachSubtitle(video.id, uri.toString())
            subtitles = SubtitleParser.parseSrtFromUri(context, uri)
        }
    }

    // Load initial subtitle if present
    LaunchedEffect(video.subtitleUriString) {
        if (!video.subtitleUriString.isNullOrBlank()) {
            try {
                subtitles = SubtitleParser.parseSrtFromUri(context, Uri.parse(video.subtitleUriString))
            } catch (_: Exception) {}
        }
    }

    // Back handler
    BackHandler {
        if (isScreenLocked) {
            isScreenLocked = false
        } else if (isFullscreen) {
            toggleFullscreen(activity, false)
            isFullscreen = false
        } else {
            mediaPlayer?.let { onSaveProgress(video.id, it.currentPosition.toLong()) }
            onClose()
        }
    }

    // Auto-hide controls
    LaunchedEffect(controlsTimeout, isControlsVisible, isPlaying, isScreenLocked) {
        if (isControlsVisible && isPlaying && !isScreenLocked) {
            while (System.currentTimeMillis() < controlsTimeout) {
                delay(500)
            }
            isControlsVisible = false
        }
    }

    // Position updater ticker
    LaunchedEffect(isPlaying) {
        while (true) {
            mediaPlayer?.let { player ->
                if (player.isPlaying) {
                    val pos = player.currentPosition.toLong()
                    currentPositionMs = pos
                    val dur = player.duration.toLong()
                    if (dur > 0) durationMs = dur

                    val activeSub = subtitles.find { pos in it.startMs..it.endMs }
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
            // Audio-Only Battery Saving View
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
            // SurfaceView Video Display with Aspect Ratio Sizing
            val surfaceModifier = when (aspectRatio) {
                VideoAspectRatio.FIT -> Modifier.fillMaxSize()
                VideoAspectRatio.FILL -> Modifier.fillMaxSize()
                VideoAspectRatio.ZOOM -> Modifier.fillMaxSize()
            }

            AndroidView(
                factory = { ctx ->
                    SurfaceView(ctx).apply {
                        holder.addCallback(object : SurfaceHolder.Callback {
                            override fun surfaceCreated(holder: SurfaceHolder) {
                                try {
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
                                            if (video.lastPositionMs > 5000L && video.lastPositionMs < mp.duration - 5000L) {
                                                mp.seekTo(video.lastPositionMs.toInt())
                                                showResumeToast = true
                                            }
                                            mp.start()
                                            isPlaying = true
                                            durationMs = mp.duration.toLong()
                                        }
                                        setOnCompletionListener {
                                            isPlaying = false
                                            isControlsVisible = true
                                        }
                                    }
                                    mediaPlayer = player
                                } catch (e: Exception) {
                                    e.printStackTrace()
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

        // Gesture Detection: Double-Tap (Seek ±10s) and Vertical Swipe (Brightness on left, Volume on right)
        if (!isScreenLocked) {
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
        } else {
            // When screen locked, single tap shows unlock button
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { isControlsVisible = !isControlsVisible }
            )
        }

        // Swipe HUD (Brightness / Volume)
        LaunchedEffect(gestureHudText) {
            if (gestureHudText != null) {
                delay(1200)
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

        // Subtitle Overlay
        currentSubtitleText?.let { subText ->
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = if (isControlsVisible) 95.dp else 28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.75f))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = subText,
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center
                    ),
                    modifier = Modifier.fillMaxWidth(0.9f)
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

        // Child / Screen Lock Floating Unlock Button
        if (isScreenLocked) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(16.dp)
            ) {
                IconButton(
                    onClick = {
                        isScreenLocked = false
                        isControlsVisible = true
                        gestureHudText = "🔓 Screen Unlocked"
                    },
                    modifier = Modifier
                        .testTag("video_unlock_button")
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.7f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Unlock Controls",
                        tint = SecondaryCyan,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }

        // Controls Overlay (Hidden when locked)
        AnimatedVisibility(
            visible = isControlsVisible && !isScreenLocked,
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
                        // Subtitle (.srt) Button
                        IconButton(
                            onClick = { subtitlePicker.launch(arrayOf("*/*")) },
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

                        // Picture-in-Picture (PiP) Button
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            IconButton(
                                onClick = {
                                    try {
                                        val params = PictureInPictureParams.Builder()
                                            .setAspectRatio(Rational(16, 9))
                                            .build()
                                        activity?.enterPictureInPictureMode(params)
                                    } catch (_: Exception) {}
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

                        // Aspect Ratio Toggle (Fit / Fill / Zoom)
                        IconButton(
                            onClick = {
                                aspectRatio = when (aspectRatio) {
                                    VideoAspectRatio.FIT -> VideoAspectRatio.FILL
                                    VideoAspectRatio.FILL -> VideoAspectRatio.ZOOM
                                    VideoAspectRatio.ZOOM -> VideoAspectRatio.FIT
                                }
                                gestureHudText = "Aspect: ${aspectRatio.name}"
                            },
                            modifier = Modifier
                                .testTag("video_aspect_ratio_button")
                                .minimumInteractiveComponentSize()
                                .size(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AspectRatio,
                                contentDescription = "Aspect Ratio",
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

                        // Screen Lock Button (Child Lock)
                        IconButton(
                            onClick = {
                                isScreenLocked = true
                                isControlsVisible = false
                                gestureHudText = "🔒 Screen Locked"
                            },
                            modifier = Modifier
                                .testTag("video_lock_button")
                                .minimumInteractiveComponentSize()
                                .size(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LockOpen,
                                contentDescription = "Lock Screen",
                                tint = Color.White
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
                            // Orientation toggle
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
                                    contentDescription = "Rotate",
                                    tint = Color.White
                                )
                            }

                            // Fullscreen Toggle
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
