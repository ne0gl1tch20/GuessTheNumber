package com.jarrlyyy.guessthenumber.shared.domain

/**
 * Platform-independent reward formulas and constants used by the production guess engine.
 * Keep the existing coefficients unchanged to preserve game balance.
 */
object GuessRewardRules {
    const val BASE_REWARD_PER_RANGE_UNIT = 100L
    const val MINIMUM_GUESS_REWARD = 10L

    fun streakMultiplier(streak: Int): Double = 1.0 + (streak * 0.1)

    fun criticalMultiplier(isCritical: Boolean): Double = if (isCritical) 5.0 else 1.0

    fun upgradeMultiplier(rewardMultiplierLevel: Int): Double =
        1.0 + (rewardMultiplierLevel * 0.25)

    fun talentMultiplier(hasTalentReward: Boolean): Double =
        if (hasTalentReward) 1.5 else 1.0
}
