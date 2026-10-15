package com.example.vybe.playback

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.PresetReverb
import android.media.audiofx.Virtualizer
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class EqualizerState(
    val isEnabled: Boolean = false,
    val bassBoostStrength: Int = 0, // 0 to 1000
    val numBands: Int = 5,
    val bandLevels: Map<Int, Int> = emptyMap(), // Band index -> level in mB (-1500 to +1500)
    val bandCenterFreqs: Map<Int, Int> = emptyMap(), // Band index -> freq in Hz
    val currentPreset: String = "Custom",
    val presets: List<String> = listOf("Custom", "Flat", "Bass Boost", "Rock", "Pop", "Jazz", "Vocal"),
    val isAuraSpatialActive: Boolean = false
)

class EqualizerManager {

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var presetReverb: PresetReverb? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var currentAudioSessionId: Int = 0

    private var isAuraSpatialEnabled: Boolean = false
    private var isHeadsetConnected: Boolean = false

    private val _state = MutableStateFlow(EqualizerState())
    val state: StateFlow<EqualizerState> = _state.asStateFlow()

    // Saved user custom levels to restore when Aura Spatial is turned off
    private var savedUserBandLevels: Map<Int, Int> = emptyMap()

    fun bindAudioSession(audioSessionId: Int) {
        if (audioSessionId <= 0 || audioSessionId == currentAudioSessionId) return
        release()
        currentAudioSessionId = audioSessionId

        try {
            equalizer = Equalizer(0, audioSessionId).apply {
                enabled = _state.value.isEnabled || _state.value.isAuraSpatialActive
            }

            bassBoost = BassBoost(0, audioSessionId).apply {
                val auraActive = _state.value.isAuraSpatialActive
                enabled = _state.value.isEnabled || auraActive
                val targetStrength = if (auraActive) 500 else _state.value.bassBoostStrength
                if (targetStrength > 0) {
                    setStrength(targetStrength.toShort())
                }
            }

            virtualizer = Virtualizer(0, audioSessionId).apply {
                enabled = _state.value.isAuraSpatialActive
                if (_state.value.isAuraSpatialActive) {
                    setStrength(600.toShort())
                }
            }

            try {
                presetReverb = PresetReverb(0, audioSessionId).apply {
                    enabled = _state.value.isAuraSpatialActive
                    if (_state.value.isAuraSpatialActive) {
                        preset = PresetReverb.PRESET_SMALLROOM
                    }
                }
            } catch (e: Exception) {
                // Reverb optional
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                try {
                    loudnessEnhancer = LoudnessEnhancer(audioSessionId).apply {
                        enabled = _state.value.isAuraSpatialActive
                        if (_state.value.isAuraSpatialActive) {
                            setTargetGain(120) // Gentle 1.2 dB clean normalization
                        }
                    }
                } catch (e: Exception) {
                    // LoudnessEnhancer optional
                }
            }

            eqToState()
            applyAuraOrUserCurve()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun eqToState() {
        val eq = equalizer ?: return
        val bands = eq.numberOfBands.toInt()
        val freqs = mutableMapOf<Int, Int>()
        val levels = mutableMapOf<Int, Int>()

        for (i in 0 until bands) {
            freqs[i] = eq.getCenterFreq(i.toShort()) / 1000 // Convert mHz to Hz
            levels[i] = eq.getBandLevel(i.toShort()).toInt()
        }

        _state.value = _state.value.copy(
            numBands = bands,
            bandCenterFreqs = freqs,
            bandLevels = levels
        )
        if (!_state.value.isAuraSpatialActive) {
            savedUserBandLevels = levels
        }
    }

    fun setEnabled(enabled: Boolean) {
        try {
            val auraActive = _state.value.isAuraSpatialActive
            equalizer?.enabled = enabled || auraActive
            bassBoost?.enabled = enabled || auraActive
            _state.value = _state.value.copy(isEnabled = enabled)
            applyAuraOrUserCurve()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setBassBoost(strength: Int) { // 0 to 1000
        val s = strength.coerceIn(0, 1000)
        try {
            _state.value = _state.value.copy(bassBoostStrength = s)
            if (!_state.value.isAuraSpatialActive) {
                bassBoost?.setStrength(s.toShort())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setBandLevel(band: Int, levelMb: Int) {
        try {
            if (!_state.value.isAuraSpatialActive) {
                equalizer?.setBandLevel(band.toShort(), levelMb.toShort())
            }
            val updatedLevels = _state.value.bandLevels.toMutableMap()
            updatedLevels[band] = levelMb
            savedUserBandLevels = updatedLevels
            _state.value = _state.value.copy(bandLevels = updatedLevels, currentPreset = "Custom")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun applyPreset(presetName: String) {
        val eq = equalizer ?: return
        try {
            when (presetName) {
                "Flat" -> {
                    for (i in 0 until eq.numberOfBands) eq.setBandLevel(i.toShort(), 0)
                    setBassBoost(0)
                }
                "Bass Boost" -> {
                    if (eq.numberOfBands >= 2) {
                        eq.setBandLevel(0.toShort(), 800)
                        eq.setBandLevel(1.toShort(), 500)
                    }
                    setBassBoost(800)
                }
                "Rock" -> {
                    if (eq.numberOfBands >= 5) {
                        eq.setBandLevel(0.toShort(), 500)
                        eq.setBandLevel(1.toShort(), 200)
                        eq.setBandLevel(2.toShort(), -100)
                        eq.setBandLevel(3.toShort(), 300)
                        eq.setBandLevel(4.toShort(), 600)
                    }
                }
                "Pop" -> {
                    if (eq.numberOfBands >= 5) {
                        eq.setBandLevel(0.toShort(), -100)
                        eq.setBandLevel(1.toShort(), 200)
                        eq.setBandLevel(2.toShort(), 500)
                        eq.setBandLevel(3.toShort(), 200)
                        eq.setBandLevel(4.toShort(), -100)
                    }
                }
                "Jazz" -> {
                    if (eq.numberOfBands >= 5) {
                        eq.setBandLevel(0.toShort(), 400)
                        eq.setBandLevel(1.toShort(), 200)
                        eq.setBandLevel(2.toShort(), -200)
                        eq.setBandLevel(3.toShort(), 200)
                        eq.setBandLevel(4.toShort(), 400)
                    }
                }
                "Vocal" -> {
                    if (eq.numberOfBands >= 5) {
                        eq.setBandLevel(0.toShort(), -200)
                        eq.setBandLevel(1.toShort(), 0)
                        eq.setBandLevel(2.toShort(), 600)
                        eq.setBandLevel(3.toShort(), 300)
                        eq.setBandLevel(4.toShort(), -100)
                    }
                }
            }
            eqToState()
            _state.value = _state.value.copy(currentPreset = presetName)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun applyAuraOrUserCurve() {
        val eq = equalizer ?: return
        val shouldActivate = _state.value.isAuraSpatialActive

        try {
            if (shouldActivate) {
                // Warm, rich bass + gentle high-frequency roll-off (eliminates piercing ear fatigue)
                val bands = eq.numberOfBands.toInt()
                if (bands >= 5) {
                    eq.setBandLevel(0.toShort(), 300)  // +3.0 dB warm sub-bass
                    eq.setBandLevel(1.toShort(), 150)  // +1.5 dB warm body
                    eq.setBandLevel(2.toShort(), 0)    // 0.0 dB clear vocals
                    eq.setBandLevel(3.toShort(), (-120).toShort()) // -1.2 dB smooth sibilance control
                    eq.setBandLevel(4.toShort(), (-250).toShort()) // -2.5 dB silky high-frequency damping
                }
            } else {
                // Restore user's customized levels
                val bands = eq.numberOfBands.toInt()
                for (i in 0 until bands) {
                    val userLevel = savedUserBandLevels[i] ?: 0
                    eq.setBandLevel(i.toShort(), userLevel.toShort())
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setAuraSpatialAudio(enabled: Boolean, headsetConnected: Boolean) {
        isAuraSpatialEnabled = enabled
        isHeadsetConnected = headsetConnected
        val shouldActivate = enabled && headsetConnected

        _state.value = _state.value.copy(isAuraSpatialActive = shouldActivate)

        try {
            if (shouldActivate) {
                // 1. Enable Equalizer with warm acoustic & smoothed high frequencies
                equalizer?.enabled = true
                applyAuraOrUserCurve()

                // 2. Smooth, rich, warm bass without clipping
                bassBoost?.enabled = true
                bassBoost?.setStrength(500.toShort())

                // 3. Binaural 8D Spatial Virtualizer (natural wide immersion without phase harshness)
                virtualizer?.enabled = true
                virtualizer?.setStrength(600.toShort())

                // 4. Soft acoustic space
                try {
                    presetReverb?.enabled = true
                    presetReverb?.preset = PresetReverb.PRESET_SMALLROOM
                } catch (e: Exception) {}

                // 5. Loudness Enhancer for clean, balanced, non-distorted audio
                try {
                    loudnessEnhancer?.enabled = true
                    loudnessEnhancer?.setTargetGain(120)
                } catch (e: Exception) {}
            } else {
                // Revert to user's standard EQ and effects
                virtualizer?.enabled = false
                try { presetReverb?.enabled = false } catch (e: Exception) {}
                try { loudnessEnhancer?.enabled = false } catch (e: Exception) {}

                val normalEnabled = _state.value.isEnabled
                equalizer?.enabled = normalEnabled
                applyAuraOrUserCurve()

                bassBoost?.enabled = normalEnabled
                bassBoost?.setStrength(_state.value.bassBoostStrength.toShort())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun release() {
        try {
            equalizer?.release()
            bassBoost?.release()
            virtualizer?.release()
            presetReverb?.release()
            loudnessEnhancer?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        equalizer = null
        bassBoost = null
        virtualizer = null
        presetReverb = null
        loudnessEnhancer = null
        currentAudioSessionId = 0
    }
}
