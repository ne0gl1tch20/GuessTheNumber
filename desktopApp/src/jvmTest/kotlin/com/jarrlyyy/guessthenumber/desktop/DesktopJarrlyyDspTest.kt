package com.jarrlyyy.guessthenumber.desktop

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DesktopJarrlyyDspTest {
    @Test
    fun rendersAllSharedPresetsIntoNonSilentPcm() {
        for (preset in 0..7) {
            val pcm = DesktopJarrlyyDsp.renderPcm(preset, 1f)
            assertNotNull("Preset $preset should render", pcm)
            assertTrue("Preset $preset should contain audio", pcm!!.any { it.toInt() != 0 })
            assertEquals(0, pcm.size % 2)
        }
    }

    @Test
    fun rejectsInvalidPresetsAndMutedVolume() {
        assertNull(DesktopJarrlyyDsp.renderPcm(-1, 1f))
        assertNull(DesktopJarrlyyDsp.renderPcm(8, 1f))
        assertNull(DesktopJarrlyyDsp.renderPcm(0, 0f))
        assertNull(DesktopJarrlyyDsp.renderPcm(0, Float.NaN))
    }

    @Test
    fun generatedPcmIsBoundedAndVolumeScalesOutput() {
        val quiet = DesktopJarrlyyDsp.renderPcm(0, 0.25f)!!
        val loud = DesktopJarrlyyDsp.renderPcm(0, 1f)!!
        fun peak(bytes: ByteArray): Int = bytes.indices.step(2).maxOf { index ->
            kotlin.math.abs((bytes[index].toInt() and 0xff) or (bytes[index + 1].toInt() shl 8))
        }
        assertTrue(peak(quiet) < peak(loud))
        assertTrue(peak(loud) <= Short.MAX_VALUE.toInt())
    }
}
