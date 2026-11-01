package com.jarrlyyy.guessthenumber.desktop

import java.io.ByteArrayOutputStream
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip
import javax.sound.sampled.FloatControl
import kotlin.math.PI
import kotlin.math.log10
import kotlin.math.sin

/**
 * Procedural Windows/JVM counterpart to the Android JarrlyyDSP presets.
 * It synthesizes short PCM clips in memory; no external sound assets are required.
 * Audio-device failures are deliberately silent so they never block gameplay.
 */
internal object DesktopJarrlyyDsp {
    private const val SAMPLE_RATE = 48_000
    private const val MAX_STREAMS = 6
    private val activeClips = mutableListOf<Clip>()
    private val format = AudioFormat(SAMPLE_RATE.toFloat(), 16, 1, true, false)

    private val patterns = arrayOf(
        floatArrayOf(990f),
        floatArrayOf(660f, 880f),
        floatArrayOf(590f, 420f),
        floatArrayOf(235f, 175f),
        floatArrayOf(523.25f, 659.25f, 783.99f, 1046.5f),
        floatArrayOf(392f, 523.25f, 659.25f, 783.99f),
        floatArrayOf(392f, 329.63f),
        floatArrayOf(698.46f, 880f)
    )
    private val noteDurationsMs = intArrayOf(75, 70, 75, 90, 75, 65, 85, 65)

    @Synchronized
    fun play(preset: Int, volume: Float): Boolean {
        val pcm = renderPcm(preset, volume) ?: return false
        return runCatching {
            activeClips.removeAll { clip ->
                if (!clip.isRunning) runCatching { clip.close() }
                !clip.isRunning
            }
            while (activeClips.size >= MAX_STREAMS) {
                val oldest = activeClips.removeAt(0)
                runCatching { oldest.stop(); oldest.close() }
            }
            val clip = AudioSystem.getClip()
            clip.open(format, pcm, 0, pcm.size)
            activeClips.add(clip)
            clip.addLineListener { event ->
                if (event.type == javax.sound.sampled.LineEvent.Type.STOP) {
                    synchronized(this) {
                        activeClips.remove(clip)
                        runCatching { clip.close() }
                    }
                }
            }
            clip.start()
            true
        }.getOrDefault(false)
    }

    /** Deterministic PCM renderer used by tests and the Java Sound playback adapter. */
    internal fun renderPcm(preset: Int, volume: Float): ByteArray? {
        if (preset !in patterns.indices || !volume.isFinite() || volume <= 0f) return null
        val safeVolume = (volume * 0.18f).coerceIn(0f, 0.45f)
        if (safeVolume <= 0f) return null
        val notes = patterns[preset]
        val noteFrames = SAMPLE_RATE * noteDurationsMs[preset] / 1000
        val frames = noteFrames * notes.size
        val output = ByteArrayOutputStream(frames * 2)
        for (frame in 0 until frames) {
            val noteIndex = (frame / noteFrames).coerceAtMost(notes.lastIndex)
            val withinNote = frame % noteFrames
            val attack = (withinNote / (SAMPLE_RATE * 0.004f)).coerceIn(0f, 1f)
            val release = ((noteFrames - withinNote) / (SAMPLE_RATE * 0.025f)).coerceIn(0f, 1f)
            val envelope = minOf(attack, release)
            val phase = 2.0 * PI * notes[noteIndex] * withinNote / SAMPLE_RATE
            val fundamental = sin(phase)
            val harmonic = if (preset == 3 || preset == 5) sin(phase * 2.0) * 0.22 else 0.0
            val sample = ((fundamental + harmonic) * envelope * safeVolume * Short.MAX_VALUE)
                .toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
            output.write(sample and 0xff)
            output.write((sample ushr 8) and 0xff)
        }
        return output.toByteArray()
    }
}
