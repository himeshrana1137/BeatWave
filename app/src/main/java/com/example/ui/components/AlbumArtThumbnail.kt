package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.model.Song
import com.example.ui.theme.PrimaryViolet
import com.example.ui.theme.PrimaryVioletLight
import com.example.ui.theme.SecondaryAccent
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceHighlightDark
import com.example.util.AlbumArtLoader
import java.io.File
import kotlin.math.abs

/**
 * Returns a list of gradient colors for songs without album art.
 * Distinct combinations based on song seed so each track has visual personality.
 */
fun getPlaceholderGradient(songSeed: String?): List<Color> {
    val seed = abs(songSeed.hashCode())
    val palettes = listOf(
        listOf(Color(0xFF8B5CF6), Color(0xFF4C1D95)), // Electric Violet to Deep Indigo
        listOf(Color(0xFF7C3AED), Color(0xFF1E1B4B)), // Vivid Purple to Midnight
        listOf(Color(0xFF06B6D4), Color(0xFF164E63)), // Cyan to Dark Ocean
        listOf(Color(0xFFEC4899), Color(0xFF831843)), // Hot Pink to Deep Berry
        listOf(Color(0xFFA855F7), Color(0xFF3B0764)), // Violet to Dark Violet
        listOf(Color(0xFF6366F1), Color(0xFF312E81))  // Indigo to Midnight Blue
    )
    return palettes[seed % palettes.size]
}

/**
 * Computes a dominant color from album art bitmap, or falls back to song-seeded vibrant color.
 * Uses AlbumArtLoader so color matches the real loaded cover art.
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
    song: Song?,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    shapeRadius: Dp = 10.dp,
    isRound: Boolean = false
) {
    val context = LocalContext.current
    val targetPx = if (size > 140.dp) AlbumArtLoader.SIZE_FULL else AlbumArtLoader.SIZE_THUMBNAIL

    // Synchronously check memory cache first so there's zero flicker for cached images
    var bitmap by remember(song?.id, targetPx) {
        mutableStateOf(AlbumArtLoader.getCachedArt(song?.id, targetPx))
    }

    LaunchedEffect(song?.id, targetPx) {
        if (song == null) {
            bitmap = null
            return@LaunchedEffect
        }
        if (bitmap == null) {
            val loaded = AlbumArtLoader.loadArt(context, song, targetPx)
            bitmap = loaded
        }
    }

    val shape = if (isRound) CircleShape else RoundedCornerShape(shapeRadius)
    val placeholderColors = remember(song?.title) { getPlaceholderGradient(song?.title) }

    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(Brush.linearGradient(placeholderColors))
            .border(1.dp, Color(0x338B5CF6), shape),
        contentAlignment = Alignment.Center
    ) {
        val currentBitmap = bitmap
        if (currentBitmap != null && !currentBitmap.isRecycled) {
            Image(
                bitmap = currentBitmap.asImageBitmap(),
                contentDescription = "${song?.title} Album Artwork",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            // Elegant placeholder gradient with music note icon
            Box(
                modifier = Modifier
                    .size(size * 0.72f)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.size(size * 0.48f)
                )
            }
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
            Color(0xFF38384C),
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
            modifier = modifier.height(16.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            val staticHeights = listOf(0.4f, 0.9f, 0.6f)
            for (i in 0 until barCount) {
                val h = if (isPlaying) staticHeights[i % staticHeights.size] else 0.25f
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height((16 * h).dp.coerceAtLeast(3.dp))
                        .clip(RoundedCornerShape(1.5.dp))
                        .background(color)
                )
            }
        }
        return
    }

    val transition = rememberInfiniteTransition(label = "equalizer")

    val bar1Height by transition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar1"
    )

    val bar2Height by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = if (isPlaying) 0.9f else 0.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(550, delayMillis = 100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar2"
    )

    val bar3Height by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = if (isPlaying) 0.85f else 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(450, delayMillis = 200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar3"
    )

    val heights = listOf(bar1Height, bar2Height, bar3Height)

    Row(
        modifier = modifier.height(16.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        for (i in 0 until barCount) {
            val h = heights.getOrElse(i % heights.size) { 0.5f }
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height((16 * h).dp.coerceAtLeast(3.dp))
                .clip(RoundedCornerShape(1.5.dp))
                .background(color)
            )
        }
    }
}
