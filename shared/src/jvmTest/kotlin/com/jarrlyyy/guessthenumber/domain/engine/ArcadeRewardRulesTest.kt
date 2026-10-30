package com.jarrlyyy.guessthenumber.domain.engine

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
}
