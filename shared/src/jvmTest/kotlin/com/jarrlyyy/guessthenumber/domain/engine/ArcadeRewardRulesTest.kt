package com.jarrlyyy.guessthenumber.domain.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ArcadeRewardRulesTest {
    @Test
    fun successfulReactionCanClaimReward() {
        assertTrue(ArcadeRewardRules.isReactionTestRewardEligible(250L))
        assertTrue(ArcadeRewardRules.isReactionTestRewardEligible(1_500L))
    }

    @Test
    fun earlyReactionCannotClaimReward() {
        assertFalse(
            ArcadeRewardRules.isReactionTestRewardEligible(
                ArcadeRewardRules.TOO_EARLY_REACTION_SENTINEL
            )
        )
    }

    @Test
    fun negativeReactionTimeCannotClaimReward() {
        assertFalse(ArcadeRewardRules.isReactionTestRewardEligible(-1L))
    }
    @Test
    fun arcadeRewardStartsAtOneNebulaAndScalesWithTimeAndScore() {
        assertEquals(1L, ArcadeRewardRules.calculateNebulaReward(0L, 0))
        assertEquals(3L, ArcadeRewardRules.calculateNebulaReward(60L, 5))
        assertEquals(10L, ArcadeRewardRules.calculateNebulaReward(600L, 100))
    }

    @Test
    fun arcadeMoneyRewardIsBoundedAndNeverNegative() {
        assertEquals(0L, ArcadeRewardRules.calculateMoneyReward(0L, 0))
        assertEquals(3_000L, ArcadeRewardRules.calculateMoneyReward(60L, 5))
        assertEquals(1_000_000L, ArcadeRewardRules.calculateMoneyReward(600L, 10_000))
        assertEquals(0L, ArcadeRewardRules.calculateMoneyReward(-10L, -5))
    }

}
