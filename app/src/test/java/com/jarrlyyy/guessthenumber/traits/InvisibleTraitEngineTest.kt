package com.jarrlyyy.guessthenumber.traits

import com.jarrlyyy.guessthenumber.domain.engine.GuessFeedback
import com.jarrlyyy.guessthenumber.domain.engine.GuessResult
import com.jarrlyyy.guessthenumber.domain.model.GameState
import com.jarrlyyy.guessthenumber.shared.domain.InvisibleTraitEngine
import com.jarrlyyy.guessthenumber.shared.domain.TraitGameplayRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlinx.serialization.json.Json

class InvisibleTraitEngineTest {
    @Test
    fun legacySaveWithoutTraitProfileDeserializesWithSafeDefaults() {
        val state = Json.decodeFromString<GameState>("{}")
        assertEquals(0L, state.traitProfile.totalGuessesObserved)
        assertEquals(0L, state.traitProfile.failedGuessesObserved)
        assertTrue(state.traitProfile.awardedBonuses.isEmpty())
    }

    @Test
    fun repeatedFailuresActivateBoundedHelpers() {
        var state = GameState()
        repeat(6) {
            state = state.copy(
                traitProfile = state.traitProfile.copy(
                    failedGuessesObserved = it.toLong() + 1L,
                    correctGuessesObserved = 10L
                )
            )
        }

        val decision = InvisibleTraitEngine.evaluate(state)
        assertTrue(InvisibleTraitEngine.HELPER_MISTAKE_INSURANCE in decision.helperIds)
        assertTrue(InvisibleTraitEngine.HELPER_RANGE_SCOUT in decision.helperIds)
        assertTrue(InvisibleTraitEngine.HELPER_COMEBACK in decision.helperIds)
        assertTrue(decision.helperIds.size <= 3)
    }

    @Test
    fun gentleTargetTraitActivatesOnlyAfterAConsistentRoughPatch() {
        val profile = GameState().traitProfile.copy(totalGuessesObserved = 8L, failedGuessesObserved = 5L, correctGuessesObserved = 3L, accuracyScore = 0.30)
        val struggling = GameState(traitProfile = profile)
        val confident = struggling.copy(traitProfile = profile.copy(accuracyScore = 0.80))
        assertTrue(InvisibleTraitEngine.HELPER_GENTLE_TARGET in InvisibleTraitEngine.evaluate(struggling).helperIds)
        assertTrue(InvisibleTraitEngine.HELPER_GENTLE_TARGET !in InvisibleTraitEngine.evaluate(confident).helperIds)
    }

    @Test
    fun gentleTargetBiasMovesNextSecretTowardTheMiddleWithoutLeavingTheRange() {
        val profile = GameState().traitProfile.copy(totalGuessesObserved = 8L, failedGuessesObserved = 5L, accuracyScore = 0.30)
        val struggling = GameState(traitProfile = profile)
        val adjusted = InvisibleTraitEngine.adjustNextTarget(struggling, 90L, 1L, 100L)
        assertTrue(adjusted in 1L..100L)
        assertTrue(adjusted < 90L)
        assertTrue(adjusted > 50L)
        assertEquals(90L, InvisibleTraitEngine.adjustNextTarget(GameState(), 90L, 1L, 100L))
    }

    @Test
    fun progressionBonusIsOneTimeAndSurvivesReload() {
        val state = GameState(prestigeCount = 1)
        val decision = InvisibleTraitEngine.evaluate(state)
        assertTrue(InvisibleTraitEngine.BONUS_FIRST_PRESTIGE in decision.bonusIds)

        val awarded = InvisibleTraitEngine.markBonusesAwarded(state, decision.bonusIds)
        val reloaded = awarded.copy()
        assertTrue(InvisibleTraitEngine.BONUS_FIRST_PRESTIGE !in InvisibleTraitEngine.evaluate(reloaded).bonusIds)
    }

    @Test
    fun kotlinRejectsIneligibleScriptIntents() {
        val requested = InvisibleTraitEngine.Decision(
            helperIds = setOf(
                InvisibleTraitEngine.HELPER_MISTAKE_INSURANCE,
                InvisibleTraitEngine.HELPER_RANGE_SCOUT
            ),
            bonusIds = setOf(InvisibleTraitEngine.BONUS_FIRST_PRESTIGE)
        )

        val validated = InvisibleTraitEngine.validateDecision(GameState(), requested)

        assertTrue(validated.helperIds.isEmpty())
        assertTrue(validated.bonusIds.isEmpty())
    }

    @Test
    fun gameplayRulePersistsProfileThroughGuessTransition() {
        val before = GameState(targetNumber = 50L, currentRangeMin = 1L, currentRangeMax = 100L)
        val raw = GuessResult(
            feedback = GuessFeedback.TOO_LOW,
            reward = com.jarrlyyy.guessthenumber.domain.model.BigNumber.ZERO,
            isCritical = false,
            newStreak = 0,
            newState = before
        )
        val after = TraitGameplayRules.apply(before, 10L, raw, emptySet())
        assertEquals(1L, after.newState.traitProfile.totalGuessesObserved)
        assertEquals(1L, after.newState.traitProfile.failedGuessesObserved)
    }
}
