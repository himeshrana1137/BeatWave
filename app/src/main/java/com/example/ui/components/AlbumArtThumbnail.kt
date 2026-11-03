package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.model.Song
import com.example.ui.theme.CardBorder
import com.example.ui.theme.PrimaryViolet
import com.example.ui.theme.PrimaryVioletLight
import com.example.ui.theme.SecondaryAccent
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceHighlightDark
import com.example.util.AlbumArtLoader
import java.io.File
import kotlin.math.abs

/**
 * Returns a list of deep, dark, muted gradient colors for songs without album art.
 * Generated from the song title hash with low saturation and deep tones.
 */
fun getPlaceholderGradient(songSeed: String?): List<Color> {
    val seed = abs(songSeed?.hashCode() ?: 0)
    val palettes = listOf(
        listOf(Color(0xFF1E1E28), Color(0xFF14141B)), // Deep Charcoal Slate
        listOf(Color(0xFF221E2C), Color(0xFF14121C)), // Muted Deep Violet / Night
        listOf(Color(0xFF18222A), Color(0xFF10161C)), // Muted Deep Cyan Slate
        listOf(Color(0xFF251C22), Color(0xFF151013)), // Muted Deep Plum
        listOf(Color(0xFF1A2222), Color(0xFF101616)), // Muted Deep Forest
        listOf(Color(0xFF24201C), Color(0xFF151310))  // Muted Warm Charcoal
    )
    return palettes[seed % palettes.size]
}

/**
 * Computes a dominant color from album art bitmap, or falls back to song-seeded deep color.
 */
@Composable
fun rememberDominantColor(song: Song?): Color {
    val context = LocalContext.current
    var dominantColor by remember(song?.id) {
        mutableStateOf(AlbumArtLoader.getCachedDominantColor(song))
    }

    LaunchedEffect(song?.id) {
        if (song != null) {
            val color = AlbumArtLoader.loadDominantColor(context, song)
            dominantColor = color
        } else {
            dominantColor = PrimaryViolet
        }
    }

    return dominantColor
}

@Composable
fun AlbumArtThumbnail(
    song: Song? = null,
    artPath: String? = null,
    title: String? = null,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    shapeRadius: Dp = 8.dp,
    isRound: Boolean = false,
    reduceAnimations: Boolean = false
) {
    val context = LocalContext.current
    val targetPx = if (size > 140.dp) AlbumArtLoader.SIZE_FULL else AlbumArtLoader.SIZE_THUMBNAIL
    val effectiveKey = song?.id ?: artPath ?: title

    // Synchronously check memory cache first so there's zero flicker for cached images
    var bitmap by remember(effectiveKey, targetPx) {
        mutableStateOf(AlbumArtLoader.getCachedArt(song?.id, targetPx))
    }

    LaunchedEffect(effectiveKey, targetPx) {
        if (song != null) {
            if (bitmap == null) {
                bitmap = AlbumArtLoader.loadArt(context, song, targetPx)
            }
        } else if (!artPath.isNullOrBlank()) {
            if (bitmap == null) {
                try {
                    val file = File(artPath)
                    if (file.exists()) {
                        bitmap = BitmapFactory.decodeFile(file.absolutePath)
                    }
                } catch (_: Exception) {}
            }
        } else {
            bitmap = null
        }
    }

    val shape = if (isRound) CircleShape else RoundedCornerShape(shapeRadius)
    val displaySeed = song?.title ?: title ?: song?.album ?: "Music"
    val placeholderColors = remember(displaySeed) { getPlaceholderGradient(displaySeed) }

    val currentBitmap = bitmap
    val crossfadeAlpha by animateFloatAsState(
        targetValue = if (currentBitmap != null) 1f else 0f,
        animationSpec = if (reduceAnimations) tween(0) else tween(durationMillis = 150),
        label = "art_crossfade"
    )

    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(Brush.linearGradient(placeholderColors))
            .border(1.dp, CardBorder, shape),
        contentAlignment = Alignment.Center
    ) {
        // Refined muted placeholder: small, low-opacity music note icon (~30% of tile size)
        Icon(
            imageVector = Icons.Default.MusicNote,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.28f),
            modifier = Modifier.size(size * 0.30f)
        )

        // Loaded bitmap with soft 150ms crossfade
        if (currentBitmap != null && !currentBitmap.isRecycled) {
            Image(
                bitmap = currentBitmap.asImageBitmap(),
                contentDescription = "${song?.title ?: title} Album Artwork",
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = crossfadeAlpha },
                contentScale = ContentScale.Crop
            )
        }
    }
}

/**
 * Shimmer skeleton placeholder for loading states.
 */
@Composable
fun SkeletonPlaceholder(
    modifier: Modifier = Modifier,
    shapeRadius: Dp = 8.dp
) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_anim"
    )

    val brush = Brush.linearGradient(
        colors = listOf(
            SurfaceHighlightDark,
            Color(0xFF282836),
            SurfaceHighlightDark
        ),
        start = Offset.Zero,
        end = Offset(x = translateAnim, y = translateAnim)
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(shapeRadius))
            .background(brush)
    )
}

@Composable
fun MiniEqualizerBars(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    barCount: Int = 3,
    color: Color = PrimaryViolet,
    reduceAnimations: Boolean = false
) {
    if (reduceAnimations || !isPlaying) {
        Row(
            modifier = modifier.height(14.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            val staticHeights = listOf(0.4f, 0.9f, 0.6f)
            for (i in 0 until barCount) {
                val h = if (isPlaying) staticHeights[i % staticHeights.size] else 0.25f
                Box(
                    modifier = Modifier
                        .width(2.5.dp)
                        .height((14 * h).dp.coerceAtLeast(2.5.dp))
                        .clip(RoundedCornerShape(1.25.dp))
                        .background(color)
                )
            }
        }
        return
    }

    val transition = rememberInfiniteTransition(label = "equalizer")

    val bar1Height by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(380, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar1"
    )

    val bar2Height by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = if (isPlaying) 0.85f else 0.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, delayMillis = 80, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar2"
    )

    val bar3Height by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = if (isPlaying) 0.80f else 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(420, delayMillis = 160, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar3"
    )

    val heights = listOf(bar1Height, bar2Height, bar3Height)

    Row(
        modifier = modifier.height(14.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        for (i in 0 until barCount) {
            val h = heights.getOrElse(i % heights.size) { 0.5f }
            Box(
                modifier = Modifier
                    .width(2.5.dp)
                    .height((14 * h).dp.coerceAtLeast(2.5.dp))
                    .clip(RoundedCornerShape(1.25.dp))
                    .background(color)
            )
        }
    }
}
