package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.playback.EqualizerState
import com.example.ui.theme.CardBorder
import com.example.ui.theme.PrimaryNeon
import com.example.ui.theme.SecondaryCyan
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceHighlightDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EqualizerScreen(
    equalizerState: EqualizerState,
    onSetEnabled: (Boolean) -> Unit,
    onSetBassBoost: (Int) -> Unit,
    onSetBandLevel: (bandIndex: Int, level: Int) -> Unit,
    onSelectPreset: (String) -> Unit,
    onSetVirtualizer: (Int) -> Unit = {},
    onToggleVolumeNormalization: (Boolean) -> Unit = {},
    sleepTimerSecondsRemaining: Int?,
    onSetSleepTimer: (minutes: Int) -> Unit,
    onCancelSleepTimer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bandLabels = listOf("60 Hz", "230 Hz", "910 Hz", "3.6 kHz", "14 kHz")

    Column(
        modifier = modifier
            .testTag("equalizer_screen")
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Equalizer Header & Switch
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                Brush.linearGradient(listOf(PrimaryNeon, SecondaryCyan))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Audio Equalizer",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        Text(
                            text = if (equalizerState.isEnabled) "Enhancing output frequency" else "Bypassed",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = if (equalizerState.isEnabled) SecondaryCyan else TextMuted
                            )
                        )
                    }
                }

                Switch(
                    checked = equalizerState.isEnabled,
                    onCheckedChange = onSetEnabled,
                    modifier = Modifier.testTag("equalizer_master_switch"),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = PrimaryNeon,
                        uncheckedTrackColor = SurfaceHighlightDark
                    )
                )
            }
        }

        // Presets Chips
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "EQ Presets",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    equalizerState.availablePresets.forEach { preset ->
                        val isSelected = equalizerState.selectedPreset == preset
                        Box(
                            modifier = Modifier
                                .testTag("preset_$preset")
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isSelected) PrimaryNeon else SurfaceHighlightDark)
                                .clickable(enabled = equalizerState.isEnabled) {
                                    onSelectPreset(preset)
                                }
                                .padding(horizontal = 14.dp, vertical = 7.dp)
                        ) {
                            Text(
                                text = preset,
                                color = if (isSelected) Color.White else TextSecondary,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            )
                        }
                    }
                }
            }
        }

        // 5-Band Equalizer Sliders
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Frequency Bands",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                    Text(
                        text = "-15 dB to +15 dB",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                equalizerState.bandLevels.forEachIndexed { index, level ->
                    val label = bandLabels.getOrElse(index) { "Band $index" }
                    val progressFloat = ((level + 1500f) / 3000f).coerceIn(0f, 1f)
                    val dbValue = (level / 100f)

                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                            )
                            Text(
                                text = "${if (dbValue > 0) "+" else ""}${String.format("%.1f", dbValue)} dB",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = if (dbValue != 0f) SecondaryCyan else TextMuted
                                )
                            )
                        }

                        Slider(
                            value = progressFloat,
                            onValueChange = { newVal ->
                                val targetLevel = (newVal * 3000f - 1500f).toInt()
                                onSetBandLevel(index, targetLevel)
                            },
                            enabled = equalizerState.isEnabled,
                            modifier = Modifier
                                .testTag("eq_slider_band_$index")
                                .fillMaxWidth(),
                            colors = SliderDefaults.colors(
                                thumbColor = SecondaryCyan,
                                activeTrackColor = SecondaryCyan,
                                inactiveTrackColor = SurfaceHighlightDark
                            )
                        )
                    }
                }
            }
        }

        // Bass Boost
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Bass Boost Sub-Woofer",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                    Text(
                        text = "${(equalizerState.bassBoostLevel / 10)}%",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = SecondaryCyan,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Slider(
                    value = (equalizerState.bassBoostLevel / 1000f).coerceIn(0f, 1f),
                    onValueChange = { onSetBassBoost((it * 1000).toInt()) },
                    enabled = equalizerState.isEnabled,
                    modifier = Modifier
                        .testTag("bass_boost_slider")
                        .fillMaxWidth(),
                    colors = SliderDefaults.colors(
                        thumbColor = PrimaryNeon,
                        activeTrackColor = PrimaryNeon,
                        inactiveTrackColor = SurfaceHighlightDark
                    )
                )
            }
        }

        // Virtualizer (3D Surround)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "3D Virtualizer Effect",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                    Text(
                        text = "${(equalizerState.virtualizerLevel / 10)}%",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = SecondaryCyan,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Slider(
                    value = (equalizerState.virtualizerLevel / 1000f).coerceIn(0f, 1f),
                    onValueChange = { onSetVirtualizer((it * 1000).toInt()) },
                    enabled = equalizerState.isEnabled,
                    modifier = Modifier
                        .testTag("virtualizer_slider")
                        .fillMaxWidth(),
                    colors = SliderDefaults.colors(
                        thumbColor = SecondaryCyan,
                        activeTrackColor = SecondaryCyan,
                        inactiveTrackColor = SurfaceHighlightDark
                    )
                )
            }
        }

        // Volume Normalization
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Volume Normalization",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Balances loud and quiet tracks for a uniform, comfortable volume.",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Switch(
                    checked = equalizerState.volumeNormalizationEnabled,
                    onCheckedChange = onToggleVolumeNormalization,
                    modifier = Modifier.testTag("volume_normalization_switch"),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = PrimaryNeon,
                        uncheckedTrackColor = SurfaceHighlightDark
                    )
                )
            }
        }

        // Sleep Timer Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Bedtime,
                        contentDescription = null,
                        tint = SecondaryCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Sleep Timer",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (sleepTimerSecondsRemaining != null && sleepTimerSecondsRemaining > 0) {
                    val mins = sleepTimerSecondsRemaining / 60
                    val secs = sleepTimerSecondsRemaining % 60
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(SurfaceHighlightDark)
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Music will pause in",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                            )
                            Text(
                                text = String.format("%02d:%02d", mins, secs),
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = SecondaryCyan
                                )
                            )
                        }

                        Button(
                            onClick = onCancelSleepTimer,
                            modifier = Modifier.testTag("cancel_sleep_timer_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFEF4444)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Turn Off")
                        }
                    }
                } else {
                    Text(
                        text = "Automatically stops playback after selected duration",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(5, 10, 15, 30, 45, 60).forEach { mins ->
                            Box(
                                modifier = Modifier
                                    .testTag("sleep_timer_${mins}m")
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(SurfaceHighlightDark)
                                    .clickable { onSetSleepTimer(mins) }
                                    .padding(horizontal = 14.dp, vertical = 7.dp)
                            ) {
                                Text(
                                    text = "${mins} min",
                                    color = TextPrimary,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
