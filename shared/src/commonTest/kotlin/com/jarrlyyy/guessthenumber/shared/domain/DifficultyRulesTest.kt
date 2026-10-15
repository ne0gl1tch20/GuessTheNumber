package com.jarrlyyy.guessthenumber.shared.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DifficultyRulesTest {
    @Test
    fun preservesDifficultyIdentifiersAndValidation() {
        assertEquals(listOf("classic", "hard", "extreme"), DifficultyRules.ids)
        assertTrue(DifficultyRules.isValid(DifficultyRules.CLASSIC))
        assertTrue(DifficultyRules.isValid(DifficultyRules.HARD))
        assertTrue(DifficultyRules.isValid(DifficultyRules.EXTREME))
        assertFalse(DifficultyRules.isValid("unknown"))
    }

    @Test
    fun preservesBaseRangesAndUpgradeScaling() {
        assertEquals(100L, DifficultyRules.baseRangeMax(DifficultyRules.CLASSIC))
        assertEquals(150L, DifficultyRules.baseRangeMax(DifficultyRules.HARD))
        assertEquals(250L, DifficultyRules.baseRangeMax(DifficultyRules.EXTREME))
        assertEquals(95L, DifficultyRules.rangeMax(DifficultyRules.CLASSIC, 1))
        assertEquals(50L, DifficultyRules.rangeMax(DifficultyRules.CLASSIC, 10))
    }

    @Test
    fun preservesRewardMultipliers() {
        assertEquals(1.0, DifficultyRules.rewardMultiplier(DifficultyRules.CLASSIC), 1e-9)
        assertEquals(1.5, DifficultyRules.rewardMultiplier(DifficultyRules.HARD), 1e-9)
        assertEquals(2.5, DifficultyRules.rewardMultiplier(DifficultyRules.EXTREME), 1e-9)
    }

    @Test
    fun preservesWrongGuessPenalties() {
        assertEquals(0.0, DifficultyRules.wrongGuessPenalty(DifficultyRules.CLASSIC), 1e-9)
        assertEquals(0.02, DifficultyRules.wrongGuessPenalty(DifficultyRules.HARD), 1e-9)
        assertEquals(0.05, DifficultyRules.wrongGuessPenalty(DifficultyRules.EXTREME), 1e-9)
    }
}
