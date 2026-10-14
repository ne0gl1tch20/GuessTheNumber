package com.jarrlyyy.guessthenumber.data.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.util.Log
import com.jarrlyyy.guessthenumber.BuildConfig
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

/**
 * Optional, asset-backed sound effects. Music playback has its own manager and volume path.
 *
 * The first call for a valid effect loads it asynchronously; call play again after loading
 * completes to play it. Missing folders/clips are normal and never interrupt gameplay.
 */
class SoundEffectManager private constructor(context: Context) : AutoCloseable {
    enum class PlayResult { PLAYED, LOADING, DISABLED, NOT_FOUND, FAILED }

    companion object {
        private const val TAG = "GTN-SFX"
        private const val MAX_CLIP_BYTES = 10L * 1024L * 1024L
        private val supportedExtensions = setOf("wav", "ogg", "mp3")

        @Volatile private var instance: SoundEffectManager? = null

        fun get(context: Context): SoundEffectManager =
            instance ?: synchronized(this) {
                instance ?: SoundEffectManager(context.applicationContext).also { instance = it }
            }
    }

    private val appContext = context.applicationContext
    private val lock = Any()
    private val soundPool = SoundPool.Builder()
        .setMaxStreams(6)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()
    private val assetPaths: Map<String, String> by lazy { discoverAssets() }
    private val readySounds = mutableMapOf<String, Int>()
    private val pendingSounds = mutableMapOf<Int, String>()
    private val failedSounds = mutableSetOf<String>()

    init {
        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            synchronized(lock) {
                val effectId = pendingSounds.remove(sampleId) ?: return@synchronized
                if (status == 0) {
                    readySounds[effectId] = sampleId
                } else {
                    failedSounds.add(effectId)
                    debugLog("Could not load sound effect '" + effectId + "' (status=" + status + ").")
                }
            }
        }
    }

    /** Returns the discovered effect IDs; an absent assets/sfx directory produces an empty list. */
    fun availableEffects(): Set<String> = assetPaths.keys

    /**
     * Plays an effect if enabled. The supplied volume is independent from background music.
     * First use queues loading and returns LOADING; missing assets return NOT_FOUND.
     */
    fun play(effectId: String, enabled: Boolean, volume: Float): PlayResult {
        if (!enabled || volume.coerceIn(0f, 1f) <= 0f) return PlayResult.DISABLED
        val normalizedId = effectId.trim().lowercase(Locale.ROOT)
        if (!normalizedId.matches(Regex("[a-z0-9_-]{1,80}"))) return PlayResult.NOT_FOUND
        val assetPath = assetPaths[normalizedId] ?: run {
            debugLog("Sound effect '" + normalizedId + "' not found in assets/sfx/.")
            return PlayResult.NOT_FOUND
        }

        synchronized(lock) {
            readySounds[normalizedId]?.let { soundId ->
                val safeVolume = volume.coerceIn(0f, 1f)
                val streamId = runCatching {
                    soundPool.play(soundId, safeVolume, safeVolume, 1, 0, 1f)
                }.getOrDefault(0)
                return if (streamId > 0) PlayResult.PLAYED else PlayResult.FAILED
            }
            if (normalizedId in pendingSounds.values) return PlayResult.LOADING
            if (normalizedId in failedSounds) return PlayResult.FAILED

            return runCatching {
                val cachedFile = copyAssetToCache(assetPath)
                val sampleId = soundPool.load(cachedFile.absolutePath, 1)
                if (sampleId <= 0) {
                    failedSounds.add(normalizedId)
                    PlayResult.FAILED
                } else {
                    pendingSounds[sampleId] = normalizedId
                    PlayResult.LOADING
                }
            }.getOrElse {
                failedSounds.add(normalizedId)
                debugLog("Unable to prepare sound effect '" + normalizedId + "': " + it.javaClass.simpleName)
                PlayResult.FAILED
            }
        }
    }

    private fun discoverAssets(): Map<String, String> {
        val files = runCatching { appContext.assets.list("sfx").orEmpty() }.getOrElse {
            debugLog("Sound-effects folder unavailable; effects remain silent.")
            emptyArray()
        }
        val discovered = files
            .filter { file -> file.substringAfterLast('.', "").lowercase(Locale.ROOT) in supportedExtensions }
            .associateBy(
                keySelector = { it.substringBeforeLast('.').lowercase(Locale.ROOT) },
                valueTransform = { "sfx/" + it }
            )
        if (discovered.isEmpty()) debugLog("No sound-effect clips found; this is supported.")
        return discovered
    }

    private fun copyAssetToCache(assetPath: String): File {
        val cacheDir = File(appContext.cacheDir, "sfx").apply { mkdirs() }
        val outputFile = File(cacheDir, assetPath.substringAfterLast('/'))
        if (outputFile.isFile && outputFile.length() in 1..MAX_CLIP_BYTES) return outputFile

        val temporary = File(cacheDir, outputFile.name + ".tmp")
        var total = 0L
        appContext.assets.open(assetPath).use { input ->
            FileOutputStream(temporary).use { output ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    total += count
                    require(total in 1..MAX_CLIP_BYTES) { "Sound-effect asset exceeds the size limit." }
                    output.write(buffer, 0, count)
                }
            }
        }
        if (!temporary.renameTo(outputFile)) {
            temporary.delete()
            throw IllegalStateException("Could not cache sound-effect asset.")
        }
        return outputFile
    }

    private fun debugLog(message: String) {
        if (BuildConfig.DEBUG) Log.d(TAG, message)
    }

    override fun close() {
        synchronized(lock) {
            runCatching { soundPool.release() }
            readySounds.clear()
            pendingSounds.clear()
            failedSounds.clear()
        }
        synchronized(SoundEffectManager::class.java) {
            if (instance === this) instance = null
        }
    }
}
