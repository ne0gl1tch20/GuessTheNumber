package com.jarrlyyy.guessthenumber.shared.domain

/**
 * Deterministic economy inputs for one guess.
 *
 * This selects the same reward multipliers and penalty rates on every platform.
 * Currency arithmetic, rounding, minimum application and state mutation remain
 * with the platform engine so existing BigNumber behavior is preserved.
 */
data class GuessEconomyDecision(
    val baseRewardPerRangeUnit: Long,
    val minimumGuessReward: Long,
    val rangeUnits: Long,
    val streakMultiplier: Double,
    val frenzyMultiplier: Double,
    val criticalMultiplier: Double,
    val upgradeMultiplier: Double,
    val talentMultiplier: Double,
    val mutatorMultiplier: Double,
    val difficultyMultiplier: Double,
    val difficultyPenaltyRate: Double,
    val taxPenaltyRate: Double
)

object GuessEconomyDecisionRules {
    fun resolve(
        rangeMax: Long,
        streak: Int,
        isCritical: Boolean,
        rewardMultiplierLevel: Int,
        hasTalentReward: Boolean,
        activeMutators: Set<String>,
        difficultyId: String
    ): GuessEconomyDecision = GuessEconomyDecision(
        baseRewardPerRangeUnit = GuessRewardRules.BASE_REWARD_PER_RANGE_UNIT,
        minimumGuessReward = GuessRewardRules.MINIMUM_GUESS_REWARD,
        rangeUnits = rangeMax,
        streakMultiplier = GuessRewardRules.streakMultiplier(streak),
        frenzyMultiplier = FrenzyRules.multiplier(streak),
        criticalMultiplier = GuessRewardRules.criticalMultiplier(isCritical),
        upgradeMultiplier = GuessRewardRules.upgradeMultiplier(rewardMultiplierLevel),
        talentMultiplier = GuessRewardRules.talentMultiplier(hasTalentReward),
        mutatorMultiplier = MutatorRules.rewardMultiplier(activeMutators),
        difficultyMultiplier = DifficultyRules.rewardMultiplier(difficultyId),
        difficultyPenaltyRate = DifficultyRules.wrongGuessPenalty(difficultyId),
        taxPenaltyRate = GuessPenaltyRules.taxPenaltyRate(activeMutators)
    )
}
