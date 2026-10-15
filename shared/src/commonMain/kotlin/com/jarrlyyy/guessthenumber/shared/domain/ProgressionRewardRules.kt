package com.jarrlyyy.guessthenumber.shared.domain

import kotlin.math.log10
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Pure reward formulas for Prestige and Ultra progression.
 *
 * Requirement lookup, BigNumber conversion, flooring, and state mutation remain
 * in the platform game engine to preserve existing economy behavior.
 */
object ProgressionRewardRules {
    fun prestigeRewardRatio(money: Double, requirement: Double): Double {
        if (money < requirement) return 0.0
        return sqrt(money / requirement)
    }

    fun ultraRewardRatio(money: Double, requirement: Double): Double {
        if (money < requirement) return 0.0
        return min(log10(money / requirement) * 100.0, 2000.0)
    }
}
