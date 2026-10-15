package com.jarrlyyy.guessthenumber.shared.domain

/**
 * Critical-hit probability from the existing prestige upgrade level.
 * Keep the current base chance and per-level increment consistent on all platforms.
 */
object CriticalChanceRules {
    const val BASE_CHANCE = 0.05
    const val BONUS_PER_LEVEL = 0.01

    fun chance(upgradeLevel: Int): Double =
        BASE_CHANCE + (upgradeLevel * BONUS_PER_LEVEL)
}
