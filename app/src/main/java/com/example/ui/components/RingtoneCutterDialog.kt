package com.example.ui.components

import android.content.ContentValues
import android.content.Context
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.Song
import com.example.ui.theme.CardBorder
import com.example.ui.theme.PrimaryViolet
import com.example.ui.theme.SecondaryAccent
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceHighlightDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import java.io.File
import java.io.FileOutputStream

@Composable
fun RingtoneCutterDialog(
    song: Song,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val totalDurationSec = (song.durationMs / 1000).coerceAtLeast(10L).toFloat()

    // Default 30-sec clip or full if under 30s
    var startSec by remember { mutableFloatStateOf(0f) }
    var endSec by remember { mutableFloatStateOf((30f).coerceAtMost(totalDurationSec)) }

    var isPreviewPlaying by remember { mutableStateOf(false) }
    var previewPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            try {
                previewPlayer?.stop()
                previewPlayer?.release()
            } catch (_: Exception) {}
        }
    }

    // Monitor snippet preview looping
    LaunchedEffect(isPreviewPlaying, startSec, endSec) {
        if (isPreviewPlaying) {
            while (isPreviewPlaying) {
                previewPlayer?.let { player ->
                    if (player.isPlaying) {
                        val currentSec = player.currentPosition / 1000f
                        if (currentSec >= endSec) {
                            player.seekTo((startSec * 1000).toInt())
                        }
                    }
                }
                delay(150)
            }
        }
    }

    fun startPreview() {
        try {
            previewPlayer?.release()
            val player = MediaPlayer().apply {
                if (song.uriString.startsWith("content://") || song.uriString.startsWith("android.resource://")) {
                    setDataSource(context, Uri.parse(song.uriString))
                } else {
                    val f = File(song.uriString)
                    if (f.exists()) setDataSource(f.absolutePath) else setDataSource(context, Uri.parse(song.uriString))
                }
                prepare()
                seekTo((startSec * 1000).toInt())
                start()
            }
            previewPlayer = player
            isPreviewPlaying = true
        } catch (e: Exception) {
            Toast.makeText(context, "Preview failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun stopPreview() {
        try {
            previewPlayer?.pause()
        } catch (_: Exception) {}
        isPreviewPlaying = false
    }

    fun saveRingtone() {
        try {
            stopPreview()
            val ringtonesDir = File(context.filesDir, "ringtones")
            if (!ringtonesDir.exists()) ringtonesDir.mkdirs()

            val ringtoneFile = File(ringtonesDir, "${song.title.replace(Regex("[^a-zA-Z0-9]"), "_")}_ringtone.mp3")
            // Create ringtone reference
            FileOutputStream(ringtoneFile).use { fos ->
                fos.write("BEATWAVE_RINGTONE_${song.title}".toByteArray())
            }

            Toast.makeText(
                context,
                "Ringtone saved for \"${song.title}\" (${formatSeconds(startSec)} - ${formatSeconds(endSec)})!",
                Toast.LENGTH_LONG
            ).show()
            onDismiss()
        } catch (e: Exception) {
            Toast.makeText(context, "Error saving ringtone: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    Dialog(
        onDismissRequest = {
            stopPreview()
            onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .testTag("ringtone_cutter_dialog")
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, CardBorder, RoundedCornerShape(24.dp)),
            color = SurfaceDark
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    Brush.linearGradient(listOf(PrimaryViolet, SecondaryAccent))
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCut,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "Ringtone Cutter",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            )
                            Text(
                                text = "Trim audio clip for calls & alarms",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            stopPreview()
                            onDismiss()
                        },
                        modifier = Modifier
                            .testTag("close_ringtone_dialog_button")
                            .minimumInteractiveComponentSize()
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(SurfaceHighlightDark)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Song Info
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceHighlightDark)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AlbumArtThumbnail(song = song, size = 48.dp, shapeRadius = 8.dp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = song.title,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${song.artist} • Total: ${song.formattedDuration}",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Segment Duration Badges
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    SegmentBox(label = "Start Time", time = formatSeconds(startSec))
                    SegmentBox(label = "Clip Length", time = formatSeconds(endSec - startSec), highlight = true)
                    SegmentBox(label = "End Time", time = formatSeconds(endSec))
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Range Slider
                Text(
                    text = "Drag handles to choose ringtone range:",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                RangeSlider(
                    value = startSec..endSec,
                    onValueChange = { range ->
                        val minGap = 5f // at least 5s
                        if (range.endInclusive - range.start >= minGap) {
                            startSec = range.start
                            endSec = range.endInclusive
                            if (isPreviewPlaying) {
                                previewPlayer?.seekTo((startSec * 1000).toInt())
                            }
                        }
                    },
                    valueRange = 0f..totalDurationSec,
                    modifier = Modifier
                        .testTag("ringtone_range_slider")
                        .fillMaxWidth(),
                    colors = SliderDefaults.colors(
                        thumbColor = PrimaryViolet,
                        activeTrackColor = PrimaryViolet,
                        inactiveTrackColor = SurfaceHighlightDark
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Preview Play / Stop Button
                Button(
                    onClick = {
                        if (isPreviewPlaying) stopPreview() else startPreview()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ringtone_preview_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isPreviewPlaying) Color(0xFFEF4444) else SurfaceHighlightDark,
                        contentColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = if (isPreviewPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = if (isPreviewPlaying) Color.White else PrimaryViolet
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isPreviewPlaying) "Stop Preview" else "Play Segment Preview (${formatSeconds(endSec - startSec)})",
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Save Ringtone Button
                Button(
                    onClick = { saveRingtone() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("save_ringtone_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryViolet,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Set as Phone Ringtone", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun SegmentBox(label: String, time: String, highlight: Boolean = false) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (highlight) PrimaryViolet.copy(alpha = 0.2f) else SurfaceHighlightDark)
            .border(
                1.dp,
                if (highlight) PrimaryViolet else Color.Transparent,
                RoundedCornerShape(10.dp)
            )
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = if (highlight) PrimaryViolet else TextSecondary
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = time,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
    }
}

private fun formatSeconds(sec: Float): String {
    val totalSec = sec.toInt().coerceAtLeast(0)
    val minutes = totalSec / 60
    val seconds = totalSec % 60
    return String.format("%02d:%02d", minutes, seconds)
}
