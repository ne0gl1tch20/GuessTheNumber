package com.jarrlyyy.guessthenumber

import com.jarrlyyy.guessthenumber.domain.model.GameState
import com.jarrlyyy.guessthenumber.traits.BuiltInTraits
import com.jarrlyyy.guessthenumber.traits.TraitActivationContext
import com.jarrlyyy.guessthenumber.traits.TraitActivationEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TraitActivationEngineTest {
    private val engine = TraitActivationEngine(BuiltInTraits.registry())

    @Test fun starterTraitActivatesOfflineWithDefaultState() {
        val result = engine.evaluate(TraitActivationContext(GameState()))
        assertTrue(result.isActive("steady_start"))
        assertFalse(result.isActive("hidden_night_owl"))
        assertEquals(1.05, result.effects.xpMultiplier, 0.0001)
    }

    @Test fun requirementsGateTraits() {
        val lowProgress = engine.evaluate(TraitActivationContext(GameState(), playerLevel = 1))
        assertFalse(lowProgress.isActive("streak_guard"))
        assertFalse(lowProgress.isActive("nebula_finder"))

        val progressed = engine.evaluate(
            TraitActivationContext(
                GameState(correctGuesses = 25, prestigeCount = 1),
                playerLevel = 5
            )
        )
        assertTrue(progressed.isActive("streak_guard"))
        assertTrue(progressed.isActive("nebula_finder"))
        assertTrue(progressed.isActive("first_prestige"))
    }

    @Test fun seasonalTraitsRequireMatchingActiveSeason() {
        val autumn = engine.evaluate(TraitActivationContext(GameState(), activeSeasonId = "autumn"))
        assertTrue(autumn.isActive("autumn_glow"))
        assertFalse(autumn.isActive("winter_comet"))

        val winter = engine.evaluate(
            TraitActivationContext(
                GameState(correctGuesses = 50),
                playerLevel = 3,
                activeSeasonId = "winter"
            )
        )
        assertTrue(winter.isActive("winter_comet"))
        assertFalse(winter.isActive("autumn_glow"))
    }

    @Test fun hiddenTraitRequiresDiscoveryAndOnlyThenAppearsInDiscoveredList() {
        val undiscovered = engine.evaluate(
            TraitActivationContext(GameState(correctGuesses = 100), playerLevel = 10)
        )
        assertFalse(undiscovered.isActive("hidden_night_owl"))
        assertTrue(undiscovered.discoveredHiddenTraitIds.isEmpty())

        val discovered = engine.evaluate(
            TraitActivationContext(
                GameState(correctGuesses = 100),
                playerLevel = 10,
                discoveredTraitIds = setOf("hidden_night_owl")
            )
        )
        assertTrue(discovered.isActive("hidden_night_owl"))
        assertEquals(setOf("hidden_night_owl"), discovered.discoveredHiddenTraitIds)
    }

    @Test fun hundredConsecutiveGuessesTraitRequiresBestStreakNotLifetimeTotal() {
        val before = engine.evaluate(TraitActivationContext(GameState(correctGuesses = 10_000, bestStreak = 99)))
        assertFalse(before.isActive("hundred_consecutive_guesses"))
        val reached = engine.evaluate(TraitActivationContext(
            GameState(correctGuesses = 100, bestStreak = 100),
            discoveredTraitIds = setOf("hundred_consecutive_guesses")
        ))
        assertTrue(reached.isActive("hundred_consecutive_guesses"))
        assertTrue(reached.effects.bonusNebulaPerClaim >= 1L)
    }

    @Test fun evaluationDoesNotMutateOrPersistGameState() {
        val state = GameState(correctGuesses = 25)
        val result = engine.evaluate(TraitActivationContext(state))
        assertEquals(25L, state.correctGuesses)
        assertTrue(result.activeTraitIds.contains("nebula_finder"))
    }
}
