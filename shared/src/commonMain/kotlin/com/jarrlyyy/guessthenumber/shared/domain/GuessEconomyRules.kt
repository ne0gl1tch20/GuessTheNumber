package com.jarrlyyy.guessthenumber.shared.domain

/**
 * Shared deterministic reward components for one correct guess.
 *
 * Currency multiplication and rounding remain in the platform engine so the
 * existing BigNumber behavior is preserved exactly.
 */
data class GuessRewardDecision(
    val baseRewardPerRangeUnit: Long,
    val rangeMax: Long,
    val streakMultiplier: Double,
    val frenzyMultiplier: Double,
    val criticalMultiplier: Double,
    val upgradeMultiplier: Double,
    val talentMultiplier: Double,
    val mutatorMultiplier: Double,
    val difficultyMultiplier: Double,
    val minimumReward: Long
)

/** Separate penalty rates preserve the existing order of platform BigNumber operations. */
data class GuessPenaltyDecision(
    val difficultyPenaltyRate: Double,
    val mutatorTaxPenaltyRate: Double
)

object GuessEconomyRules {
    fun resolveReward(
        rangeMax: Long,
        streak: Int,
        isCritical: Boolean,
        rewardMultiplierLevel: Int,
        hasTalentReward: Boolean,
        activeMutators: Set<String>,
        difficultyId: String
    ): GuessRewardDecision = GuessRewardDecision(
        baseRewardPerRangeUnit = GuessRewardRules.BASE_REWARD_PER_RANGE_UNIT,
        rangeMax = rangeMax,
        streakMultiplier = GuessRewardRules.streakMultiplier(streak),
        frenzyMultiplier = FrenzyRules.multiplier(streak),
        criticalMultiplier = GuessRewardRules.criticalMultiplier(isCritical),
        upgradeMultiplier = GuessRewardRules.upgradeMultiplier(rewardMultiplierLevel),
        talentMultiplier = GuessRewardRules.talentMultiplier(hasTalentReward),
        mutatorMultiplier = MutatorRules.rewardMultiplier(activeMutators),
        difficultyMultiplier = DifficultyRules.rewardMultiplier(difficultyId),
        minimumReward = GuessRewardRules.MINIMUM_GUESS_REWARD
    )

    fun resolvePenalty(
        difficultyId: String,
        activeMutators: Set<String>
    ): GuessPenaltyDecision = GuessPenaltyDecision(
        difficultyPenaltyRate = DifficultyRules.wrongGuessPenalty(difficultyId),
        mutatorTaxPenaltyRate = GuessPenaltyRules.taxPenaltyRate(activeMutators)
    )
}
