package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Typeface
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import com.example.model.ListeningStats
import com.example.model.Song
import com.example.ui.theme.AccentPink
import com.example.ui.theme.CardBorder
import com.example.ui.theme.PrimaryNeon
import com.example.ui.theme.PrimaryViolet
import com.example.ui.theme.SecondaryCyan
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceHighlightDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@Composable
fun BeatWaveWrappedDialog(
    stats: ListeningStats,
    allSongs: List<Song>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val topSongs = allSongs.filter { it.playCount > 0 }
        .sortedByDescending { it.playCount }
        .take(5)

    val persona = when {
        stats.totalTracksPlayed > 50 -> "Music Obsessive"
        stats.totalListeningTimeMs > 3600000L -> "Sound Marathoner"
        topSongs.any { it.title.contains("night", ignoreCase = true) || it.title.contains("chill", ignoreCase = true) } -> "Midnight Dreamer"
        else -> "Wave Pioneer"
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .testTag("beatwave_wrapped_dialog")
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            color = SurfaceDark,
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(PrimaryViolet, AccentPink))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "BeatWave Wrapped",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            )
                            Text(
                                text = "Your Listening Year in Review",
                                style = MaterialTheme.typography.bodySmall.copy(color = SecondaryCyan)
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Persona Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = SurfaceHighlightDark),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryViolet.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "YOUR LISTENER PERSONA",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = PrimaryViolet,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.2.sp
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = persona,
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "You explored sound without limits on BeatWave.",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                            )
                        }
                    }

                    // Key Stats Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = SurfaceHighlightDark),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text("Total Time", style = MaterialTheme.typography.bodySmall.copy(color = TextMuted))
                                Text(
                                    text = stats.formattedTotalTime,
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = SecondaryCyan
                                    )
                                )
                            }
                        }

                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = SurfaceHighlightDark),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text("Tracks Played", style = MaterialTheme.typography.bodySmall.copy(color = TextMuted))
                                Text(
                                    text = "${stats.totalTracksPlayed}",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryNeon
                                    )
                                )
                            }
                        }
                    }

                    // Top Artist
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = SurfaceHighlightDark),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Top Artist", style = MaterialTheme.typography.bodySmall.copy(color = TextMuted))
                                Text(
                                    text = stats.mostPlayedArtist ?: "Diverse Explorer",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                )
                            }
                            Text("👑", fontSize = 28.sp)
                        }
                    }

                    // Top 5 Songs
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = SurfaceHighlightDark),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Your Top Songs",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            if (topSongs.isEmpty()) {
                                Text(
                                    text = "Start playing music to generate your top songs chart!",
                                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                                )
                            } else {
                                topSongs.forEachIndexed { index, song ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(
                                                text = "#${index + 1}",
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    color = PrimaryViolet,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            )
                                            Column {
                                                Text(
                                                    text = song.title,
                                                    style = MaterialTheme.typography.bodyMedium.copy(
                                                        color = TextPrimary,
                                                        fontWeight = FontWeight.SemiBold
                                                    ),
                                                    maxLines = 1
                                                )
                                                Text(
                                                    text = song.artist,
                                                    style = MaterialTheme.typography.bodySmall.copy(color = TextMuted),
                                                    maxLines = 1
                                                )
                                            }
                                        }

                                        Text(
                                            text = "${song.playCount} plays",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = SecondaryCyan,
                                                fontWeight = FontWeight.Medium
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Share Wrapped Card Button
                Button(
                    onClick = {
                        scope.launch {
                            shareWrappedImage(context, stats, persona, topSongs)
                        }
                    },
                    modifier = Modifier
                        .testTag("share_wrapped_button")
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryViolet)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share My Wrapped Card", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private suspend fun shareWrappedImage(
    context: Context,
    stats: ListeningStats,
    persona: String,
    topSongs: List<Song>
) = withContext(Dispatchers.IO) {
    try {
        val width = 1080
        val height = 1350
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Background
        val bgPaint = Paint().apply {
            shader = LinearGradient(
                0f, 0f, 0f, height.toFloat(),
                intArrayOf(android.graphics.Color.parseColor("#12121A"), android.graphics.Color.parseColor("#0F0F14")),
                null,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Decorative gradient orb
        val orbPaint = Paint().apply {
            shader = LinearGradient(
                width * 0.2f, height * 0.1f, width * 0.8f, height * 0.5f,
                intArrayOf(android.graphics.Color.parseColor("#558B5CF6"), android.graphics.Color.parseColor("#33EC4899")),
                null,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawCircle(width * 0.5f, height * 0.28f, width * 0.42f, orbPaint)

        // Header Title
        val titlePaint = Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.WHITE
            textSize = 64f
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("BEATWAVE WRAPPED", width * 0.5f, 160f, titlePaint)

        val subPaint = Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.parseColor("#8B5CF6")
            textSize = 34f
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
            letterSpacing = 0.15f
        }
        canvas.drawText("LISTENING PERSONA: $persona", width * 0.5f, 220f, subPaint)

        // Listening Time & Plays
        val statPaint = Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.WHITE
            textSize = 42f
            typeface = Typeface.DEFAULT
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("⏱ ${stats.formattedTotalTime} Listened  •  🎵 ${stats.totalTracksPlayed} Tracks", width * 0.5f, 340f, statPaint)

        // Top Artist
        if (!stats.mostPlayedArtist.isNullOrBlank()) {
            val artistPaint = Paint().apply {
                isAntiAlias = true
                color = android.graphics.Color.parseColor("#06B6D4")
                textSize = 38f
                typeface = Typeface.DEFAULT_BOLD
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("👑 Top Artist: ${stats.mostPlayedArtist}", width * 0.5f, 410f, artistPaint)
        }

        // Top Songs List
        val topTitlePaint = Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.parseColor("#A1A1AA")
            textSize = 32f
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("TOP SONGS", width * 0.5f, 520f, topTitlePaint)

        val songPaint = Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.WHITE
            textSize = 36f
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.LEFT
        }
        val songSubPaint = Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.parseColor("#71717A")
            textSize = 28f
            typeface = Typeface.DEFAULT
            textAlign = Paint.Align.LEFT
        }

        var startY = 600f
        topSongs.take(5).forEachIndexed { i, s ->
            canvas.drawText("${i + 1}. ${s.title.take(28)}", 160f, startY, songPaint)
            canvas.drawText("    ${s.artist.take(30)} • ${s.playCount} plays", 160f, startY + 36f, songSubPaint)
            startY += 90f
        }

        // Footer
        val footerPaint = Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.parseColor("#8B5CF6")
            textSize = 28f
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
            letterSpacing = 0.1f
        }
        canvas.drawText("MY YEAR IN SOUND • BEATWAVE PLAYER", width * 0.5f, 1260f, footerPaint)

        // Save and share
        val shareDir = File(context.cacheDir, "shares").apply { if (!exists()) mkdirs() }
        val shareFile = File(shareDir, "wrapped_${System.currentTimeMillis()}.png")
        FileOutputStream(shareFile).use { fos ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 95, fos)
        }

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", shareFile)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, "Here's my BeatWave Wrapped! Total listening time: ${stats.formattedTotalTime} 🎵")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, "Share BeatWave Wrapped").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })

    } catch (e: Exception) {
        android.util.Log.e("BeatWaveWrapped", "Failed to share wrapped: ${e.message}")
    }
}
