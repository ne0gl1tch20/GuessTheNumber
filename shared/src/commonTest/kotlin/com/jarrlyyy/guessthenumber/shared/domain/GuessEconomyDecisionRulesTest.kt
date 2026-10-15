package com.jarrlyyy.guessthenumber.shared.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class GuessEconomyDecisionRulesTest {
    @Test
    fun resolvesCorrectGuessRewardFactorsWithoutChangingCurrencyArithmetic() {
        val result = GuessEconomyDecisionRules.resolve(
            rangeMax = 150L,
            streak = 10,
            isCritical = true,
            rewardMultiplierLevel = 2,
            hasTalentReward = true,
            activeMutators = setOf(MutatorRules.SPEED, MutatorRules.TAX),
            difficultyId = DifficultyRules.HARD
        )

        assertEquals(GuessRewardRules.BASE_REWARD_PER_RANGE_UNIT, result.baseRewardPerRangeUnit)
        assertEquals(GuessRewardRules.MINIMUM_GUESS_REWARD, result.minimumGuessReward)
        assertEquals(150L, result.rangeUnits)
        assertEquals(2.0, result.streakMultiplier, 1e-9)
        assertEquals(1.5, result.frenzyMultiplier, 1e-9)
        assertEquals(5.0, result.criticalMultiplier, 1e-9)
        assertEquals(1.5, result.upgradeMultiplier, 1e-9)
        assertEquals(1.5, result.talentMultiplier, 1e-9)
        assertEquals(30.0, result.mutatorMultiplier, 1e-9)
        assertEquals(1.5, result.difficultyMultiplier, 1e-9)
        assertEquals(0.02, result.difficultyPenaltyRate, 1e-9)
        assertEquals(0.05, result.taxPenaltyRate, 1e-9)
    }

    @Test
    fun ignoresUnknownMutatorsAndPreservesClassicPenaltyRates() {
        val result = GuessEconomyDecisionRules.resolve(
            rangeMax = 100L,
            streak = 0,
            isCritical = false,
            rewardMultiplierLevel = 0,
            hasTalentReward = false,
            activeMutators = setOf("unknown-mutator"),
            difficultyId = DifficultyRules.CLASSIC
        )

        assertEquals(1.0, result.mutatorMultiplier, 1e-9)
        assertEquals(1.0, result.difficultyMultiplier, 1e-9)
        assertEquals(0.0, result.difficultyPenaltyRate, 1e-9)
        assertEquals(0.0, result.taxPenaltyRate, 1e-9)
        assertEquals(1.0, result.criticalMultiplier, 1e-9)
    }
}
