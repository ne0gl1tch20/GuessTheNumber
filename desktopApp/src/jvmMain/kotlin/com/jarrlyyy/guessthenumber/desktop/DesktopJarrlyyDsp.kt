package com.jarrlyyy.guessthenumber.desktop

import java.io.ByteArrayOutputStream
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip
import javax.sound.sampled.FloatControl
import kotlin.math.PI
import kotlin.math.log10
import kotlin.math.sin

/**
 * Always-warm procedural Windows/JVM counterpart to Android JarrlyyDSP.
 *
 * Audio clips are synthesized and opened once on a daemon worker during app startup.
 * Gameplay calls only select an already-open clip and start it, so device discovery,
 * clip allocation and PCM generation never run on the UI/input thread.
 */
internal object DesktopJarrlyyDsp {
    private const val SAMPLE_RATE = 48_000
    private const val VOICES_PER_PRESET = 2
    private const val MAX_PENDING_PLAYS = 12
    private val format = AudioFormat(SAMPLE_RATE.toFloat(), 16, 1, true, false)
    private val lock = Any()
    private val startRequested = AtomicBoolean(false)
    private val initialized = AtomicBoolean(false)
    private val pending = ConcurrentLinkedQueue<Pair<Int, Float>>()
    private val audioWorker = Executors.newSingleThreadExecutor { task ->
        Thread(task, "GTN-JarrlyyDSP").apply { isDaemon = true }
    }
    private val voices = Array(8) { mutableListOf<Clip>() }
    private val nextVoice = IntArray(8)

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

    /** Start device discovery and pre-open the sound pool without blocking the caller. */
    fun start() {
        if (!startRequested.compareAndSet(false, true)) return
        audioWorker.execute {
            val prepared = Array(patterns.size) { mutableListOf<Clip>() }
            for (preset in patterns.indices) {
                val pcm = renderPcm(preset, 1f) ?: continue
                repeat(VOICES_PER_PRESET) {
                    runCatching {
                        AudioSystem.getClip().also { clip ->
                            clip.open(format, pcm, 0, pcm.size)
                            prepared[preset].add(clip)
                        }
                    }
                }
            }
            synchronized(lock) {
                prepared.forEachIndexed { preset, clips -> voices[preset].addAll(clips) }
                initialized.set(true)
                while (true) {
                    val request = pending.poll() ?: break
                    playReady(request.first, request.second)
                }
            }
        }
    }

    /**
     * Queue or start a preset. This method is deliberately non-blocking and safe to call
     * directly from gameplay event handlers.
     */
    fun play(preset: Int, volume: Float): Boolean {
        if (preset !in patterns.indices || !volume.isFinite() || volume <= 0f) return false
        if (!initialized.get()) {
            synchronized(lock) {
                if (pending.size >= MAX_PENDING_PLAYS) pending.poll()
                pending.offer(preset to volume.coerceIn(0f, 1f))
            }
            start()
            return true
        }
        return synchronized(lock) { playReady(preset, volume) }
    }

    private fun playReady(preset: Int, volume: Float): Boolean {
        if (volume <= 0f) return false
        val pool = voices[preset]
        if (pool.isEmpty()) return false
        val clip = pool[nextVoice[preset]++ % pool.size]
        return runCatching {
            if (clip.isRunning) clip.stop()
            clip.framePosition = 0
            runCatching {
                val gain = clip.getControl(FloatControl.Type.MASTER_GAIN) as FloatControl
                val requestedGain = if (volume >= 1f) 0f else 20f * log10(volume.coerceAtLeast(0.001f))
                gain.value = requestedGain.coerceIn(gain.minimum, gain.maximum)
            }
            clip.start()
            true
        }.getOrDefault(false)
    }

    /** Release pre-opened audio resources when the desktop application exits. */
    fun shutdown() {
        if (!startRequested.get()) return
        runCatching {
            audioWorker.execute {
                synchronized(lock) {
                    initialized.set(false)
                    pending.clear()
                    voices.forEach { pool ->
                        pool.toList().forEach { clip ->
                            runCatching { clip.stop() }
                            runCatching { clip.close() }
                        }
                        pool.clear()
                    }
                }
            }
        }
    }

    /** Deterministic PCM renderer used by tests and the prewarmed playback pool. */
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
