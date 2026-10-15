package com.jarrlyyy.guessthenumber.shared.domain

/**
 * Platform-independent reward multiplier formulas used by the production guess engine.
 * Keep the existing coefficients unchanged to preserve game balance.
 */
object GuessRewardRules {
    fun streakMultiplier(streak: Int): Double = 1.0 + (streak * 0.1)

    fun criticalMultiplier(isCritical: Boolean): Double = if (isCritical) 5.0 else 1.0

    fun upgradeMultiplier(rewardMultiplierLevel: Int): Double =
        1.0 + (rewardMultiplierLevel * 0.25)

    fun talentMultiplier(hasTalentReward: Boolean): Double =
        if (hasTalentReward) 1.5 else 1.0
}
