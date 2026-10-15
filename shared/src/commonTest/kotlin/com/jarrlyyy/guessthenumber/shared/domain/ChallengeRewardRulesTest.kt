package com.jarrlyyy.guessthenumber.shared.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ChallengeRewardRulesTest {
    private val progress = ChallengeProgress(
        correctGuesses = 80,
        bestStreak = 30,
        money = 20_000_000,
        totalGuesses = 300,
        prestigeCount = 2,
        ultraCount = 1,
        playtimeSeconds = 4_000
    )

    @Test
    fun claimEligibilityRejectsCompletedChallengesAndInvalidRewards() {
        assertTrue(ChallengeRewardRules.canClaim(emptySet(), "weekly_correct", 75, progress))
        assertFalse(ChallengeRewardRules.canClaim(setOf("weekly_correct"), "weekly_correct", 75, progress))
        assertFalse(ChallengeRewardRules.canClaim(emptySet(), "weekly_correct", 74, progress))
    }

    @Test
    fun claimDecisionCarriesRewardOnlyForEligibleUnclaimedChallenges() {
        val first = ChallengeClaimRules.resolve(emptySet(), "weekly_correct", 75, progress)
        assertTrue(first.accepted)
        assertEquals("weekly_correct", first.challengeId)
        assertEquals(75L, first.rewardNebula)

        val duplicate = ChallengeClaimRules.resolve(setOf("weekly_correct"), "weekly_correct", 75, progress)
        assertFalse(duplicate.accepted)
        assertEquals("weekly_correct", duplicate.challengeId)
        assertEquals(0L, duplicate.rewardNebula)

        val invalid = ChallengeClaimRules.resolve(emptySet(), "weekly_correct", 74, progress)
        assertFalse(invalid.accepted)
        assertEquals(0L, invalid.rewardNebula)
    }

    @Test
    fun acceptsWeeklyChallengeOnlyWhenRewardAndProgressMatch() {
        assertTrue(ChallengeRewardRules.isValidReward("weekly_correct", 75, progress))
        assertFalse(ChallengeRewardRules.isValidReward("weekly_correct", 74, progress))
        assertFalse(ChallengeRewardRules.isValidReward("weekly_correct", 75, progress.copy(correctGuesses = 74)))
    }

    @Test
    fun validatesWeeklyKindsAndRejectsUnknownKinds() {
        assertTrue(ChallengeRewardRules.isValidReward("weekly_playtime", 95, progress))
        assertTrue(ChallengeRewardRules.isValidReward("weekly_frenzy", 110, progress))
        assertFalse(ChallengeRewardRules.isValidReward("weekly_unknown", 75, progress))
    }

    @Test
    fun validatesBuilderTargetRewardAndMutators() {
        assertTrue(ChallengeRewardRules.isValidReward("builder_correct_75", 50, progress))
        assertTrue(ChallengeRewardRules.isValidReward("builder_correct_75_mut_speed", 100, progress))
        assertFalse(ChallengeRewardRules.isValidReward("builder_correct_75", 100, progress))
        assertFalse(ChallengeRewardRules.isValidReward("builder_correct_0", 50, progress))
        assertFalse(ChallengeRewardRules.isValidReward("builder_correct_75_mut_unknown", 100, progress))
    }

    @Test
    fun rejectsUnknownChallengeTypesAndNonPositiveRewards() {
        assertFalse(ChallengeRewardRules.isValidReward("other_correct", 50, progress))
        assertFalse(ChallengeRewardRules.isValidReward("weekly_correct", 0, progress))
    }
}