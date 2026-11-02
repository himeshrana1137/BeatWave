package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.AudioQuality
import com.example.model.AudioQualityTier
import com.example.model.VideoItem
import com.example.model.VideoQuality
import com.example.model.VideoResolutionTier
import com.example.ui.theme.CardBorder
import com.example.ui.theme.PrimaryNeon
import com.example.ui.theme.PrimaryViolet
import com.example.ui.theme.SecondaryAccent
import com.example.ui.theme.SecondaryCyan
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceHighlightDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

/**
 * Small rounded badge for song items in lists and mini player.
 * Examples: "FLAC", "MP3 320", "AAC 256", with golden "HI-RES" badge when applicable.
 */
@Composable
fun AudioQualityBadge(
    quality: AudioQuality,
    compact: Boolean = false,
    modifier: Modifier = Modifier
) {
    val tierColor = when (quality.tier) {
        AudioQualityTier.LOSSLESS -> PrimaryViolet
        AudioQualityTier.HIGH -> SecondaryCyan
        AudioQualityTier.STANDARD -> Color(0xFFA1A1AA)
        AudioQualityTier.LOW -> Color(0xFF71717A)
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Main Format / Bitrate Pill
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(tierColor.copy(alpha = 0.15f))
                .border(0.8.dp, tierColor.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                .padding(horizontal = 4.dp, vertical = 1.dp)
        ) {
            Text(
                text = if (compact) quality.format else quality.badgeText,
                color = tierColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.2.sp
            )
        }

        // Special Hi-Res Golden Badge (24-bit or >48 kHz)
        if (quality.isHiRes) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0x33FFB74D))
                    .border(0.8.dp, Color(0xFFFFB74D), RoundedCornerShape(4.dp))
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            ) {
                Text(
                    text = "HI-RES",
                    color = Color(0xFFFFB74D),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.4.sp
                )
            }
        }
    }
}

/**
 * Detailed audio quality pill strip for the Full-Screen Player.
 * Displays format, bitrate, sample rate, bit depth, channels, and Hi-Res status.
 * Gracefully hides missing or zero fields.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DetailedAudioQualityStrip(
    quality: AudioQuality,
    modifier: Modifier = Modifier
) {
    val tierColor = when (quality.tier) {
        AudioQualityTier.LOSSLESS -> PrimaryViolet
        AudioQualityTier.HIGH -> SecondaryCyan
        AudioQualityTier.STANDARD -> Color(0xFFA1A1AA)
        AudioQualityTier.LOW -> Color(0xFF71717A)
    }

    FlowRow(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceDark.copy(alpha = 0.6f))
            .border(1.dp, CardBorder.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.Center
    ) {
        // Format & Tier Pill
        QualityDetailTag(
            text = "${quality.format} • ${quality.tier.label}",
            color = tierColor,
            isBold = true
        )

        // Hi-Res Special Pill
        if (quality.isHiRes) {
            QualityDetailTag(
                text = "HI-RES AUDIO",
                color = Color(0xFFFFB74D),
                isBold = true
            )
        }

        // Bitrate (if available)
        if (quality.bitrateKbps > 0) {
            QualityDetailTag(text = quality.bitrateFormatted, color = TextPrimary)
        }

        // Sample rate (if available)
        if (quality.sampleRateFormatted.isNotBlank()) {
            QualityDetailTag(text = quality.sampleRateFormatted, color = TextSecondary)
        }

        // Bit depth (if available)
        if (quality.bitDepthFormatted.isNotBlank()) {
            QualityDetailTag(text = quality.bitDepthFormatted, color = TextSecondary)
        }

        // Channels (if available)
        if (quality.channelsFormatted.isNotBlank()) {
            QualityDetailTag(text = quality.channelsFormatted, color = TextMuted)
        }
    }
}

@Composable
private fun QualityDetailTag(
    text: String,
    color: Color,
    isBold: Boolean = false
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 5.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 11.sp,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Medium
        )
    }
}

/**
 * Resolution, HDR and 60fps badges displayed on video thumbnails.
 */
@Composable
fun VideoQualityBadges(
    quality: VideoQuality,
    modifier: Modifier = Modifier
) {
    val (tierText, tierColor) = when (quality.resolutionTier) {
        VideoResolutionTier.UHD_8K -> "8K" to Color(0xFFF43F5E) // Rose/Red
        VideoResolutionTier.UHD_4K -> "4K" to Color(0xFFF59E0B) // Amber
        VideoResolutionTier.QHD_2K -> "2K" to Color(0xFFA855F7) // Purple
        VideoResolutionTier.FHD -> "1080p" to SecondaryCyan     // Cyan
        VideoResolutionTier.HD -> "720p" to Color(0xFF10B981)   // Emerald
        VideoResolutionTier.SD -> "SD" to Color(0xFFA1A1AA)     // Muted
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        // Resolution Badge
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(Color.Black.copy(alpha = 0.82f))
                .border(0.8.dp, tierColor.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
                .padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            Text(
                text = tierText,
                color = tierColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.2.sp
            )
        }

        // 60fps Badge
        if (quality.fps >= 55) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.82f))
                    .border(0.8.dp, PrimaryNeon.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
                .padding(horizontal = 3.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "${quality.fps}fps",
                    color = PrimaryNeon,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // HDR Badge
        if (quality.isHdr) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.82f))
                    .border(0.8.dp, Color(0xFFFFB74D), RoundedCornerShape(4.dp))
                    .padding(horizontal = 3.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "HDR",
                    color = Color(0xFFFFB74D),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}

/**
 * Modal Info Panel for Video Player showing full technical specs and hardware codec support.
 */
@Composable
fun VideoInfoPanelDialog(
    video: VideoItem,
    quality: VideoQuality,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .testTag("video_info_panel_dialog")
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, CardBorder, RoundedCornerShape(20.dp)),
            color = SurfaceDark
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
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
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Brush.linearGradient(listOf(PrimaryViolet, SecondaryCyan))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Video Specifications",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            )
                            Text(
                                text = "Stream & hardware playback info",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .testTag("close_video_info_button")
                            .minimumInteractiveComponentSize()
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(SurfaceHighlightDark)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Video Title
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    ),
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Spec Grid
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SpecRow(
                        icon = Icons.Default.HighQuality,
                        label = "Resolution",
                        value = "${quality.resolutionFormatted} (${quality.resolutionTier.label})"
                    )

                    if (quality.fps > 0) {
                        SpecRow(
                            icon = Icons.Default.Speed,
                            label = "Frame Rate",
                            value = "${quality.fps} fps"
                        )
                    }

                    SpecRow(
                        icon = Icons.Default.Videocam,
                        label = "Video Codec",
                        value = quality.videoCodec
                    )

                    if (quality.videoBitrateKbps > 0) {
                        SpecRow(
                            icon = Icons.Default.Speed,
                            label = "Bitrate",
                            value = "${quality.videoBitrateKbps} kbps"
                        )
                    }

                    SpecRow(
                        icon = Icons.Default.MusicNote,
                        label = "Audio Codec",
                        value = quality.audioCodec
                    )

                    SpecRow(
                        icon = Icons.Default.HighQuality,
                        label = "Color Range",
                        value = if (quality.isHdr) "HDR (High Dynamic Range)" else "SDR (Standard Range)"
                    )

                    if (quality.fileSizeFormatted.isNotBlank()) {
                        SpecRow(
                            icon = Icons.Default.Storage,
                            label = "File Size",
                            value = quality.fileSizeFormatted
                        )
                    }

                    // Hardware decoder support status
                    SpecRow(
                        icon = Icons.Default.Memory,
                        label = "Hardware Decoder",
                        value = if (quality.isSupported) "Supported (Direct Play) ✅" else "Unsupported Codec ⚠️",
                        valueColor = if (quality.isSupported) SecondaryCyan else Color(0xFFF43F5E)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryViolet,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Close", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun SpecRow(
    icon: ImageVector,
    label: String,
    value: String,
    valueColor: Color = TextPrimary
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceHighlightDark)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = SecondaryAccent,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
            )
        }

        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Bold,
                color = valueColor
            )
        )
    }
}
