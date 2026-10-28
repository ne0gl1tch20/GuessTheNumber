package com.jarrlyyy.guessthenumber.shared.domain

import com.jarrlyyy.guessthenumber.domain.engine.GuessFeedback
import com.jarrlyyy.guessthenumber.domain.engine.GuessResult
import com.jarrlyyy.guessthenumber.domain.model.GameState
import com.jarrlyyy.guessthenumber.domain.model.TraitProfile

/**
 * Deterministic, save-safe brain for the invisible trait system.
 *
 * It observes gameplay and returns bounded helper/reward intents. It never grants currency
 * outside the normal Kotlin gameplay pipeline and never accepts script-provided amounts.
 */
object InvisibleTraitEngine {
    const val HELPER_RANGE_SCOUT = "range_scout"
    const val HELPER_MISTAKE_INSURANCE = "mistake_insurance"
    const val HELPER_STREAK_GUARD = "streak_guard"
    const val HELPER_COMEBACK = "comeback_reward"
    const val HELPER_MOMENTUM = "momentum_reward"

    const val BONUS_FIRST_PRESTIGE = "first_prestige_bonus"
    const val BONUS_FIRST_ULTRA = "first_ultra_bonus"
    const val BONUS_VETERAN = "veteran_bonus"

    data class Decision(
        val helperIds: Set<String> = emptySet(),
        val bonusIds: Set<String> = emptySet()
    )

    fun observeGuess(stateBefore: GameState, result: GuessResult): GameState {
        val profile = stateBefore.traitProfile
        val correct = result.feedback == GuessFeedback.CORRECT
        val failed = !correct
        val nextStreak = result.newStreak
        val nextProfile = profile.copy(
            totalGuessesObserved = (profile.totalGuessesObserved + 1L).coerceAtLeast(profile.totalGuessesObserved),
            correctGuessesObserved = if (correct) profile.correctGuessesObserved + 1L else profile.correctGuessesObserved,
            failedGuessesObserved = if (failed) profile.failedGuessesObserved + 1L else profile.failedGuessesObserved,
            longStreaksObserved = if (nextStreak >= LONG_STREAK_THRESHOLD) profile.longStreaksObserved + 1L else profile.longStreaksObserved,
            moneyEarnedObserved = profile.moneyEarnedObserved + result.reward,
            consistencyScore = smoothScore(profile.consistencyScore, if (correct) 1.0 else 0.0),
            accuracyScore = smoothScore(profile.accuracyScore, if (correct) 1.0 else 0.0),
            riskScore = smoothScore(profile.riskScore, if (failed) 1.0 else 0.0),
            persistenceScore = smoothScore(profile.persistenceScore, 1.0)
        )
        return stateBefore.copy(traitProfile = nextProfile)
    }

    /** Observe a successful prestige/ultra transition without changing the existing counters. */
    fun observeProgression(state: GameState, previousPrestige: Long, previousUltra: Long): GameState {
        val profile = state.traitProfile
        val prestigeDelta = (state.prestigeCount - previousPrestige).coerceAtLeast(0L)
        val ultraDelta = (state.ultraCount - previousUltra).coerceAtLeast(0L)
        if (prestigeDelta == 0L && ultraDelta == 0L) return state
        return state.copy(
            traitProfile = profile.copy(
                prestigesObserved = profile.prestigesObserved + prestigeDelta,
                ultrasObserved = profile.ultrasObserved + ultraDelta,
                progressionScore = smoothScore(profile.progressionScore, 1.0)
            )
        )
    }

    fun observeUpgradePurchase(state: GameState, countDelta: Long = 1L): GameState {
        if (countDelta <= 0L) return state
        return state.copy(
            traitProfile = state.traitProfile.copy(
                upgradesPurchasedObserved = state.traitProfile.upgradesPurchasedObserved + countDelta
            )
        )
    }

    fun evaluate(state: GameState): Decision {
        val p = state.traitProfile
        val helpers = linkedSetOf<String>()
        val bonuses = linkedSetOf<String>()

        // Adaptive helpers: deliberately small and deterministic.
        if (p.failedGuessesObserved >= FAILURE_HELPER_THRESHOLD) helpers += HELPER_MISTAKE_INSURANCE
        if (p.failedGuessesObserved >= RANGE_HELPER_THRESHOLD && state.correctGuesses >= 10L) helpers += HELPER_RANGE_SCOUT
        if (p.longStreaksObserved > 0L) helpers += HELPER_STREAK_GUARD
        if (p.failedGuessesObserved >= COMEBACK_FAILURE_THRESHOLD && p.correctGuessesObserved > 0L) helpers += HELPER_COMEBACK
        if (state.streak >= MOMENTUM_STREAK_THRESHOLD) helpers += HELPER_MOMENTUM

        // One-time progression recognition is ledger-backed and therefore reload-safe.
        if (state.prestigeCount >= 1L && BONUS_FIRST_PRESTIGE !in p.awardedBonuses) bonuses += BONUS_FIRST_PRESTIGE
        if (state.ultraCount >= 1L && BONUS_FIRST_ULTRA !in p.awardedBonuses) bonuses += BONUS_FIRST_ULTRA
        if (state.prestigeCount >= VETERAN_PRESTIGE_THRESHOLD && BONUS_VETERAN !in p.awardedBonuses) bonuses += BONUS_VETERAN
        return Decision(helpers, bonuses)
    }

    fun markBonusesAwarded(state: GameState, bonusIds: Set<String>): GameState {
        if (bonusIds.isEmpty()) return state
        return state.copy(
            traitProfile = state.traitProfile.copy(
                awardedBonuses = state.traitProfile.awardedBonuses + bonusIds
            )
        )
    }

    private fun smoothScore(previous: Double, sample: Double): Double =
        (previous * SCORE_DECAY + sample * (1.0 - SCORE_DECAY)).coerceIn(0.0, 1.0)

    const val LONG_STREAK_THRESHOLD = 10
    const val FAILURE_HELPER_THRESHOLD = 3L
    const val RANGE_HELPER_THRESHOLD = 5L
    const val COMEBACK_FAILURE_THRESHOLD = 6L
    const val MOMENTUM_STREAK_THRESHOLD = 5
    const val VETERAN_PRESTIGE_THRESHOLD = 10L
    const val SCORE_DECAY = 0.92
}
