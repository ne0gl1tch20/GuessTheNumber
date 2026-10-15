package com.jarrlyyy.guessthenumber.shared.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class GuessRewardRulesTest {
    @Test
    fun preservesBaseAndMinimumGuessRewardConstants() {
        assertEquals(100L, GuessRewardRules.BASE_REWARD_PER_RANGE_UNIT)
        assertEquals(10L, GuessRewardRules.MINIMUM_GUESS_REWARD)
    }

    @Test
    fun preservesStreakMultiplierScaling() {
        assertEquals(1.0, GuessRewardRules.streakMultiplier(0), 1e-9)
        assertEquals(1.5, GuessRewardRules.streakMultiplier(5), 1e-9)
    }

    @Test
    fun preservesCriticalRewardMultiplier() {
        assertEquals(1.0, GuessRewardRules.criticalMultiplier(false), 1e-9)
        assertEquals(5.0, GuessRewardRules.criticalMultiplier(true), 1e-9)
    }

    @Test
    fun preservesRewardUpgradeScaling() {
        assertEquals(1.0, GuessRewardRules.upgradeMultiplier(0), 1e-9)
        assertEquals(1.25, GuessRewardRules.upgradeMultiplier(1), 1e-9)
        assertEquals(2.0, GuessRewardRules.upgradeMultiplier(4), 1e-9)
    }

    @Test
    fun preservesTalentRewardMultiplier() {
        assertEquals(1.0, GuessRewardRules.talentMultiplier(false), 1e-9)
        assertEquals(1.5, GuessRewardRules.talentMultiplier(true), 1e-9)
    }
}
