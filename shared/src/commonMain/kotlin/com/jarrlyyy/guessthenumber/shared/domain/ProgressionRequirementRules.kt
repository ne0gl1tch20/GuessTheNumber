package com.jarrlyyy.guessthenumber.shared.domain

import kotlin.math.pow

/**
 * Platform-independent requirement scaling for prestige and ultra progression.
 *
 * BigNumber conversion and all save/economy mutations remain in the platform engine.
 */
object ProgressionRequirementRules {
    const val PRESTIGE_BASE = 50_000_000.0
    const val PRESTIGE_GROWTH = 2.5
    const val ULTRA_BASE = 10_000_000_000.0
    const val ULTRA_GROWTH = 5.0

    fun prestigeRequirement(prestigeCount: Long): Double =
        PRESTIGE_BASE * PRESTIGE_GROWTH.pow(prestigeCount.toDouble())

    fun ultraRequirement(ultraCount: Long): Double =
        ULTRA_BASE * ULTRA_GROWTH.pow(ultraCount.toDouble())
}
