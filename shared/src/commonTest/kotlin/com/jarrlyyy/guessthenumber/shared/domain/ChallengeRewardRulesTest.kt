package com.jarrlyyy.guessthenumber.shared.domain

import kotlin.test.Test
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