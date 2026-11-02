package com.example.ui.components

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.FolderOff
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.example.model.AppSettings
import com.example.model.ListeningStats
import com.example.ui.theme.AppThemeMode
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
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter

@Composable
fun SettingsScreen(
    currentTheme: AppThemeMode,
    onSelectTheme: (AppThemeMode) -> Unit,
    isVisualizerEnabled: Boolean,
    onToggleVisualizer: (Boolean) -> Unit,
    appSettings: AppSettings,
    onToggleAutoPause: (Boolean) -> Unit,
    onToggleHeadsetDoublePress: (Boolean) -> Unit,
    onToggleShakeToSkip: (Boolean) -> Unit,
    onToggleFilterShortAudio: (Boolean) -> Unit,
    onSetMinAudioDuration: (Int) -> Unit,
    onOpenFolderBlacklist: () -> Unit,
    onOpenDuplicateFinder: () -> Unit,
    onOpenRingtoneCutter: () -> Unit,
    listeningStats: ListeningStats,
    songCount: Int,
    videoCount: Int,
    playlistCount: Int,
    onExportBackup: suspend () -> String,
    onImportBackup: (String) -> Unit,
    onToggleFadeInOut: ((Boolean) -> Unit)? = null,
    onToggleAutoFetchLyrics: ((Boolean) -> Unit)? = null,
    onToggleBackgroundVideoAudioOnly: ((Boolean) -> Unit)? = null,
    onToggleReduceAnimations: ((Boolean) -> Unit)? = null,
    onToggleFetchOnlineCoverArt: ((Boolean) -> Unit)? = null,
    onToggleShowQualityBadges: ((Boolean) -> Unit)? = null,
    onOpenWrapped: (() -> Unit)? = null,
    onOpenAchievements: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var backupJsonToExport by remember { mutableStateOf<String?>(null) }

    // Export file launcher
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null && backupJsonToExport != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { os ->
                    OutputStreamWriter(os).use { writer ->
                        writer.write(backupJsonToExport)
                    }
                }
                Toast.makeText(context, "Backup exported successfully!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Export error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Import file launcher
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val content = BufferedReader(InputStreamReader(stream)).readText()
                    onImportBackup(content)
                    Toast.makeText(context, "Backup restored successfully!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Restore failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(
        modifier = modifier
            .testTag("settings_screen")
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App Theme Selector
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                Brush.linearGradient(listOf(PrimaryViolet, SecondaryAccent))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Appearance & Theme",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        Text(
                            text = "Spotify-style modern dark aesthetics",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                val themeOptions = listOf(
                    Triple(AppThemeMode.DARK, "Midnight Dark", "Deep slate violet with electric neon accents"),
                    Triple(AppThemeMode.AMOLED, "AMOLED Pure Black", "True #000000 pitch black for OLED battery efficiency"),
                    Triple(AppThemeMode.LIGHT, "Clean Light", "Crisp daytime high-contrast clean theme")
                )

                themeOptions.forEach { (mode, name, desc) ->
                    val isSelected = currentTheme == mode
                    val tag = when (mode) {
                        AppThemeMode.DARK -> "theme_dark"
                        AppThemeMode.AMOLED -> "theme_amoled"
                        AppThemeMode.LIGHT -> "theme_light"
                    }

                    Row(
                        modifier = Modifier
                            .testTag(tag)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) SurfaceHighlightDark else Color.Transparent)
                            .border(
                                1.dp,
                                if (isSelected) PrimaryViolet else Color.Transparent,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { onSelectTheme(mode) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { onSelectTheme(mode) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = PrimaryViolet,
                                unselectedColor = TextMuted
                            )
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = name,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = TextPrimary
                                )
                            )
                            Text(
                                text = desc,
                                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }

        // Section 1: Playback & Headset Controls
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceHighlightDark),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Headphones,
                            contentDescription = null,
                            tint = PrimaryViolet,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Playback & Hardware Controls",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        Text(
                            text = "Calls, headphones, and motion gestures",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Feature 1: Auto-pause on calls & headphone unplug
                SettingToggleItem(
                    title = "Auto-Pause on Calls & Unplug",
                    subtitle = "Automatically pause when receiving a call or unplugging headphones, and resume after call ends",
                    checked = appSettings.autoPauseOnCallAndUnplug,
                    onCheckedChange = onToggleAutoPause,
                    testTag = "setting_autopause_switch"
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Feature 2: Headset double-press
                SettingToggleItem(
                    title = "Headset Double-Press Controls",
                    subtitle = "Double press the play/pause button on wired or Bluetooth headsets to skip to the next track",
                    checked = appSettings.headsetDoublePressEnabled,
                    onCheckedChange = onToggleHeadsetDoublePress,
                    testTag = "setting_headset_double_press_switch"
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Feature 4: Shake to skip
                SettingToggleItem(
                    title = "Shake to Skip",
                    subtitle = "Shake your phone firmly during active playback to skip to the next song (battery-optimized)",
                    checked = appSettings.shakeToSkipEnabled,
                    onCheckedChange = onToggleShakeToSkip,
                    testTag = "setting_shake_to_skip_switch"
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Feature (4): Fade in/out on play and pause
                SettingToggleItem(
                    title = "Fade In / Out on Play & Pause",
                    subtitle = "Smooth volume ramp when starting, resuming, or pausing tracks to prevent audio pops",
                    checked = appSettings.fadeInOutEnabled,
                    onCheckedChange = { onToggleFadeInOut?.invoke(it) },
                    testTag = "setting_fade_in_out_switch"
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Feature (5): Automatic lyrics fetching (LRCLIB)
                SettingToggleItem(
                    title = "Auto-Fetch Synced Lyrics (LRCLIB)",
                    subtitle = "Automatically download and cache synchronized line-by-line .lrc lyrics when connected to internet",
                    checked = appSettings.autoFetchLyrics,
                    onCheckedChange = { onToggleAutoFetchLyrics?.invoke(it) },
                    testTag = "setting_auto_fetch_lyrics_switch"
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Optional Feature: Online Album Art Fetcher
                SettingToggleItem(
                    title = "Online Album Art Fetcher",
                    subtitle = "Search and download missing cover art from iTunes / Deezer over internet (cached in app storage, never modifies audio files)",
                    checked = appSettings.fetchOnlineCoverArt,
                    onCheckedChange = { onToggleFetchOnlineCoverArt?.invoke(it) },
                    testTag = "setting_online_cover_switch"
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Feature (8): Background video audio-only
                SettingToggleItem(
                    title = "Audio-Only Background Video",
                    subtitle = "Keep video audio playing smoothly even when switching apps or locking your phone screen",
                    checked = appSettings.backgroundVideoAudioOnly,
                    onCheckedChange = { onToggleBackgroundVideoAudioOnly?.invoke(it) },
                    testTag = "setting_bg_video_audio_switch"
                )
            }
        }

        // Section 2: Audio Filtering & Media Scanning
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceHighlightDark),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterAlt,
                            contentDescription = null,
                            tint = SecondaryAccent,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Library Filters & Storage",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        Text(
                            text = "Exclude short files, folders, and clean duplicates",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Quality Badges Toggle (default on)
                SettingToggleItem(
                    title = "Show Quality Badges",
                    subtitle = "Show audio format/bitrate (FLAC, Hi-Res, 320k) and video resolution badges (1080p, 4K, 60fps)",
                    checked = appSettings.showQualityBadges,
                    onCheckedChange = { onToggleShowQualityBadges?.invoke(it) },
                    testTag = "setting_quality_badges_switch"
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Feature 5: Hide audio files shorter than 60s
                SettingToggleItem(
                    title = "Hide Short Audio (< ${appSettings.minAudioDurationSec}s)",
                    subtitle = "Exclude ringtones, notifications, and voice memos shorter than threshold from music list",
                    checked = appSettings.filterShortAudioEnabled,
                    onCheckedChange = onToggleFilterShortAudio,
                    testTag = "setting_filter_short_switch"
                )

                if (appSettings.filterShortAudioEnabled) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(SurfaceHighlightDark)
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Minimum Duration Threshold",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = TextSecondary,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                            Text(
                                text = "${appSettings.minAudioDurationSec} seconds",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = PrimaryViolet,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }

                        Slider(
                            value = appSettings.minAudioDurationSec.toFloat(),
                            onValueChange = { onSetMinAudioDuration(it.toInt()) },
                            valueRange = 15f..120f,
                            steps = 6,
                            colors = SliderDefaults.colors(
                                thumbColor = PrimaryViolet,
                                activeTrackColor = PrimaryViolet,
                                inactiveTrackColor = SurfaceDark
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Feature 6: Folder Blacklist Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceHighlightDark)
                        .clickable { onOpenFolderBlacklist() }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOff,
                            contentDescription = null,
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = "Folder Blacklist",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            )
                            Text(
                                text = "${appSettings.blacklistedFolders.size} folders excluded from library",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = onOpenFolderBlacklist,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("Manage", fontSize = 11.sp, color = SecondaryAccent)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Feature 7: Duplicate Song Finder Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceHighlightDark)
                        .clickable { onOpenDuplicateFinder() }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = null,
                            tint = SecondaryCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = "Duplicate Song Finder",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            )
                            Text(
                                text = "Scan and clean duplicate audio copies",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = onOpenDuplicateFinder,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("Scan", fontSize = 11.sp, color = PrimaryViolet)
                    }
                }
            }
        }

        // Section 3: Ringtone Cutter & Home Screen Widget
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceHighlightDark),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCut,
                            contentDescription = null,
                            tint = PrimaryViolet,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Ringtone Cutter & Widget",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        Text(
                            text = "Audio trimmer and home screen mini player",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Feature 10: Ringtone Cutter quick launcher
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceHighlightDark)
                        .clickable { onOpenRingtoneCutter() }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCut,
                            contentDescription = null,
                            tint = SecondaryAccent,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = "Ringtone Cutter",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            )
                            Text(
                                text = "Trim any song to make custom call or alarm ringtones",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = onOpenRingtoneCutter,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("Open", fontSize = 11.sp, color = SecondaryAccent)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Feature 3: Home screen widget info card
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceHighlightDark)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Widgets,
                        contentDescription = null,
                        tint = PrimaryViolet,
                        modifier = Modifier.size(22.dp)
                    )
                    Column {
                        Text(
                            text = "Home Screen Widget Active",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        Text(
                            text = "Add BeatWave to your home screen! Shows live album art with play/pause, prev and next controls.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        )
                    }
                }
            }
        }

        // Performance & Low-RAM Optimization
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                SettingToggleItem(
                    title = "Audio Visualizer FX",
                    subtitle = "Turn off on low-RAM or 32-bit phones for maximum battery life & smooth fps",
                    checked = isVisualizerEnabled,
                    onCheckedChange = onToggleVisualizer,
                    testTag = "setting_visualizer_switch"
                )

                Spacer(modifier = Modifier.height(8.dp))

                SettingToggleItem(
                    title = "Reduce Animations",
                    subtitle = "Disable heavy screen transitions and graphics for low-end or 32-bit devices",
                    checked = appSettings.reduceAnimations,
                    onCheckedChange = { onToggleReduceAnimations?.invoke(it) },
                    testTag = "setting_reduce_animations_switch"
                )
            }
        }

        // Fun Features: Wrapped & Badges
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceHighlightDark),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Celebration,
                            contentDescription = null,
                            tint = PrimaryViolet,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Fun Features & Gamification",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        Text(
                            text = "Yearly stats cards and listening achievements",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // BeatWave Wrapped Action
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceHighlightDark)
                        .clickable { onOpenWrapped?.invoke() }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Celebration,
                            contentDescription = null,
                            tint = PrimaryViolet,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = "BeatWave Wrapped",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            )
                            Text(
                                text = "Your top songs, artists & total time formatted as a shareable card",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = { onOpenWrapped?.invoke() },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("View", fontSize = 11.sp, color = PrimaryViolet)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Badges and Achievements Action
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceHighlightDark)
                        .clickable { onOpenAchievements?.invoke() }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = null,
                            tint = SecondaryAccent,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = "Achievements & Badges",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            )
                            Text(
                                text = "Unlock badges like '100 songs played', 'Night Owl', 'Genre Explorer'",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = { onOpenAchievements?.invoke() },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("Badges", fontSize = 11.sp, color = SecondaryAccent)
                    }
                }
            }
        }

        // Listening Statistics
        Card(
            modifier = Modifier
                .testTag("stats_card")
                .fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceHighlightDark),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Analytics,
                            contentDescription = null,
                            tint = SecondaryCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Text(
                        text = "Listening Statistics",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    StatBox(label = "Total Plays", value = "${listeningStats.totalTracksPlayed}")
                    StatBox(label = "Total Time", value = listeningStats.formattedTotalTime)
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (listeningStats.mostPlayedSong != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(SurfaceHighlightDark)
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Top Song:",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextMuted,
                                fontWeight = FontWeight.Medium
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${listeningStats.mostPlayedSong.title} (${listeningStats.mostPlayedSong.playCount} plays)",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = SecondaryCyan,
                                fontWeight = FontWeight.Bold
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // Backup and Restore (Playlists & Favorites)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceHighlightDark),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Backup,
                            contentDescription = null,
                            tint = SecondaryCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Backup & Restore",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        Text(
                            text = "Save your playlists & favorites as a JSON file",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                backupJsonToExport = onExportBackup()
                                exportLauncher.launch("beatwave_backup.json")
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("export_backup_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryViolet),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Export JSON", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = { importLauncher.launch(arrayOf("application/json", "text/*", "*/*")) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("restore_backup_button"),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SecondaryCyan),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Restore,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Restore JSON", fontSize = 12.sp)
                    }
                }
            }
        }

        // Offline Capability & App Info
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudDone,
                        contentDescription = null,
                        tint = SecondaryCyan,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = "100% Offline Capable & Low-RAM Friendly",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        Text(
                            text = "Optimized for both 32-bit and 64-bit Android devices with ultra-low memory allocations.",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingToggleItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceHighlightDark)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = TextSecondary,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag),
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = PrimaryViolet,
                uncheckedTrackColor = SurfaceDark
            )
        )
    }
}

@Composable
private fun StatBox(label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceHighlightDark)
            .padding(horizontal = 20.dp, vertical = 10.dp)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.ExtraBold,
                color = SecondaryCyan
            )
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
        )
    }
}
