package com.jarrlyyy.guessthenumber.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class JarrlyyDspEngineTest {
    @Test fun exposesStablePresetIdsForDeveloperPanel() {
        assertEquals((0..7).toList(), JarrlyyDspEngine.presets.map { it.id })
        assertTrue(JarrlyyDspEngine.presets.all { it.title.isNotBlank() && it.description.isNotBlank() })
    }

    @Test fun effectFlagsAreIndependentBits() {
        val flags = listOf(JarrlyyDspEngine.EFFECT_LOWPASS, JarrlyyDspEngine.EFFECT_REVERB,
            JarrlyyDspEngine.EFFECT_CHORUS, JarrlyyDspEngine.EFFECT_DISTORTION)
        assertEquals(4, flags.distinct().size)
        assertTrue(flags.fold(0) { combined, flag -> combined or flag } == 15)
    }

    @Test fun rejectsInvalidPresetAndNonFiniteVolumeWithoutLoadingNativeCode() {
        assertFalse(JarrlyyDspEngine.playPreset(-1))
        assertFalse(JarrlyyDspEngine.playPreset(8))
        assertFalse(JarrlyyDspEngine.playPreset(0, Float.NaN))
        assertFalse(JarrlyyDspEngine.playPreset(0, Float.POSITIVE_INFINITY))
    }
}
