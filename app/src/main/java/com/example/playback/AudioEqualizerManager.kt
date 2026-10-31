package com.example.playback

import android.content.Context
import android.content.SharedPreferences
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class EqualizerState(
    val isEnabled: Boolean = true,
    val bassBoostLevel: Int = 300, // 0 to 1000
    val bandLevels: List<Int> = listOf(0, 0, 0, 0, 0), // millibels (-1500 to +1500)
    val selectedPreset: String = "Flat",
    val availablePresets: List<String> = listOf("Flat", "Bass Boost", "Electronic", "Rock", "Pop", "Vocal")
)

class AudioEqualizerManager(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("harmoniq_equalizer_prefs", Context.MODE_PRIVATE)

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null

    private val _state = MutableStateFlow(loadSavedState())
    val state: StateFlow<EqualizerState> = _state.asStateFlow()

    fun attachAudioSession(audioSessionId: Int) {
        if (audioSessionId == 0) return
        release()
        try {
            equalizer = Equalizer(0, audioSessionId).apply {
                enabled = _state.value.isEnabled
            }
            bassBoost = BassBoost(0, audioSessionId).apply {
                enabled = _state.value.isEnabled
                if (strengthSupported) {
                    setStrength(_state.value.bassBoostLevel.toShort())
                }
            }
            applyBandsToHardware()
        } catch (e: Exception) {
            Log.w("AudioEqualizerManager", "Hardware equalizer unavailable, using fallback: ${e.message}")
        }
    }

    fun setEnabled(enabled: Boolean) {
        _state.value = _state.value.copy(isEnabled = enabled)
        try {
            equalizer?.enabled = enabled
            bassBoost?.enabled = enabled
        } catch (_: Exception) {}
        saveState()
    }

    fun setBassBoost(level: Int) {
        val safeLevel = level.coerceIn(0, 1000)
        _state.value = _state.value.copy(bassBoostLevel = safeLevel)
        try {
            if (bassBoost?.strengthSupported == true) {
                bassBoost?.setStrength(safeLevel.toShort())
            }
        } catch (_: Exception) {}
        saveState()
    }

    fun setBandLevel(bandIndex: Int, level: Int) {
        val bands = _state.value.bandLevels.toMutableList()
        if (bandIndex in bands.indices) {
            bands[bandIndex] = level.coerceIn(-1500, 1500)
            _state.value = _state.value.copy(
                bandLevels = bands,
                selectedPreset = "Custom"
            )
            try {
                equalizer?.setBandLevel(bandIndex.toShort(), level.toShort())
            } catch (_: Exception) {}
            saveState()
        }
    }

    fun selectPreset(presetName: String) {
        val newLevels = when (presetName) {
            "Bass Boost" -> listOf(600, 400, 0, -100, -200)
            "Electronic" -> listOf(450, 300, -150, 200, 400)
            "Rock" -> listOf(500, 250, -200, 300, 500)
            "Pop" -> listOf(-150, 200, 500, 250, -100)
            "Vocal" -> listOf(-300, -100, 600, 400, 100)
            else -> listOf(0, 0, 0, 0, 0) // Flat
        }

        _state.value = _state.value.copy(
            selectedPreset = presetName,
            bandLevels = newLevels
        )
        applyBandsToHardware()
        saveState()
    }

    private fun applyBandsToHardware() {
        try {
            val eq = equalizer ?: return
            val levels = _state.value.bandLevels
            val numBands = eq.numberOfBands.toInt()
            for (i in 0 until minOf(numBands, levels.size)) {
                eq.setBandLevel(i.toShort(), levels[i].toShort())
            }
        } catch (_: Exception) {}
    }

    private fun loadSavedState(): EqualizerState {
        val isEnabled = prefs.getBoolean("eq_enabled", true)
        val bass = prefs.getInt("eq_bass", 300)
        val preset = prefs.getString("eq_preset", "Flat") ?: "Flat"
        val levels = (0..4).map { i -> prefs.getInt("eq_band_$i", 0) }

        return EqualizerState(
            isEnabled = isEnabled,
            bassBoostLevel = bass,
            bandLevels = levels,
            selectedPreset = preset
        )
    }

    private fun saveState() {
        val s = _state.value
        val editor = prefs.edit()
            .putBoolean("eq_enabled", s.isEnabled)
            .putInt("eq_bass", s.bassBoostLevel)
            .putString("eq_preset", s.selectedPreset)
        s.bandLevels.forEachIndexed { index, level ->
            editor.putInt("eq_band_$index", level)
        }
        editor.apply()
    }

    fun release() {
        try {
            equalizer?.release()
            bassBoost?.release()
        } catch (_: Exception) {}
        equalizer = null
        bassBoost = null
    }
}
