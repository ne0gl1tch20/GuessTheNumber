package com.jarrlyyy.guessthenumber.audio

/** Reusable facade for the optional native procedural audio engine. */
object JarrlyyDspEngine {
    const val EFFECT_LOWPASS = 1
    const val EFFECT_REVERB = 2
    const val EFFECT_CHORUS = 4
    const val EFFECT_DISTORTION = 8

    data class Preset(val id: Int, val title: String, val description: String)
    val presets = listOf(
        Preset(0, "Tap", "Short, soft sine click"),
        Preset(1, "Confirm", "Two-note confirmation"),
        Preset(2, "Back", "Descending close sound"),
        Preset(3, "Error", "Low, buzzy warning"),
        Preset(4, "Reward", "Four-note reward chime"),
        Preset(5, "Arcade", "Retro square-wave arpeggio"),
        Preset(6, "Guess too low", "Low two-note hint"),
        Preset(7, "Guess too high", "Bright two-note hint")
    )

    private var nativeLoaded: Boolean? = null
    private external fun nativePlayPreset(preset: Int, volume: Float, effects: Int): Boolean
    private external fun nativeStopAll()
    private external fun nativeIsSupported(): Boolean

    private fun ensureLoaded(): Boolean {
        nativeLoaded?.let { return it }
        return try {
            System.loadLibrary("jarrlyyydsp")
            nativeLoaded = true
            true
        } catch (_: UnsatisfiedLinkError) {
            nativeLoaded = false
            false
        } catch (_: SecurityException) {
            nativeLoaded = false
            false
        }
    }

    fun isNativeLibraryAvailable(): Boolean =
        ensureLoaded() && runCatching { nativeIsSupported() }.getOrDefault(false)

    fun playPreset(presetId: Int, volume: Float = 0.18f, effects: Int = 0): Boolean {
        if (presetId !in 0..7 || !volume.isFinite()) return false
        if (!ensureLoaded()) return false
        return runCatching { nativePlayPreset(presetId, volume.coerceIn(0f, 0.45f), effects and 0x0f) }
            .getOrDefault(false)
    }

    fun stopAll(): Boolean {
        if (!ensureLoaded()) return false
        return runCatching { nativeStopAll(); true }.getOrDefault(false)
    }
}
