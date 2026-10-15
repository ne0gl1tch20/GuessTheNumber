package com.jarrlyyy.guessthenumber.shared.domain

/**
 * Pure frenzy multiplier formula shared across platforms.
 */
object FrenzyRules {
    const val THRESHOLD = 5
    const val BONUS_PER_TIER = 0.25
    const val MAX_MULTIPLIER = 5.0

    fun multiplier(streak: Int): Double {
        if (streak < THRESHOLD) return 1.0
        val tiers = streak / THRESHOLD
        return minOf(MAX_MULTIPLIER, 1.0 + (tiers * BONUS_PER_TIER))
    }
}
