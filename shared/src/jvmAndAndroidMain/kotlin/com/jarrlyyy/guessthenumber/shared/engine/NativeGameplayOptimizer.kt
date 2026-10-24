package com.jarrlyyy.guessthenumber.shared.engine

import kotlin.random.Random

/**
 * Narrow bridge for compute-heavy, deterministic gameplay personalization.
 *
 * Kotlin remains authoritative for save state and gameplay rules. Native code only
 * receives bounded scalar inputs and returns a candidate target inside the supplied range.
 * Desktop/JVM builds fall back to Kotlin so the shared engine remains portable.
 */
object NativeGameplayOptimizer {
    private var nativeAvailable: Boolean? = null

    private external fun nativeComputeAdaptiveTarget(
        rangeMin: Long,
        rangeMax: Long,
        difficulty: String,
        streak: Int,
        attempts: Long,
        seed: Long
    ): Long

    private fun ensureNativeLoaded(): Boolean {
        nativeAvailable?.let { return it }
        return try {
            System.loadLibrary("gtn_native")
            nativeAvailable = true
            true
        } catch (_: UnsatisfiedLinkError) {
            nativeAvailable = false
            false
        }
    }

    fun nextTarget(
        rangeMin: Long,
        rangeMax: Long,
        difficulty: String,
        streak: Int,
        attempts: Long,
        rng: Random
    ): Long {
        if (rangeMax < rangeMin) return rangeMin
        if (!ensureNativeLoaded()) return rng.nextLong(rangeMin, rangeMax + 1L)

        val seed = rng.nextLong()
        return runCatching {
            nativeComputeAdaptiveTarget(
                rangeMin = rangeMin,
                rangeMax = rangeMax,
                difficulty = difficulty,
                streak = streak.coerceAtLeast(0),
                attempts = attempts.coerceAtLeast(0L),
                seed = seed
            ).coerceIn(rangeMin, rangeMax)
        }.getOrElse {
            nativeAvailable = false
            rng.nextLong(rangeMin, rangeMax + 1L)
        }
    }
}
