package com.jarrlyyy.guessthenumber

import com.jarrlyyy.guessthenumber.domain.model.GameState
import com.jarrlyyy.guessthenumber.traits.BuiltInTraits
import com.jarrlyyy.guessthenumber.traits.TraitActivationContext
import com.jarrlyyy.guessthenumber.traits.TraitActivationEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TraitPhase3CatalogTest {
    private val engine = TraitActivationEngine(BuiltInTraits.registry())

    @Test fun catalogIsVersionedAndExpanded() {
        assertEquals(3, BuiltInTraits.CATALOG_VERSION)
        assertTrue(BuiltInTraits.registry().size >= 316)
    }

    @Test fun progressionAndCollectionChainsFollowMilestones() {
        val early = engine.evaluate(TraitActivationContext(GameState(), playerLevel = 2))
        assertFalse(early.isActive("quick_learner"))

        val progressed = engine.evaluate(
            TraitActivationContext(
                GameState(correctGuesses = 75, prestigeCount = 1),
                playerLevel = 10
            )
        )
        assertTrue(progressed.isActive("quick_learner"))
        assertTrue(progressed.isActive("nebula_finder"))
        assertTrue(progressed.isActive("nebula_hunter"))
        assertTrue(progressed.isActive("first_prestige"))
        assertFalse(progressed.isActive("veteran_focus"))

        val veteran = engine.evaluate(
            TraitActivationContext(
                GameState(correctGuesses = 150, prestigeCount = 1),
                playerLevel = 10
            )
        )
        assertTrue(veteran.isActive("veteran_focus"))
        assertTrue(veteran.isActive("comet_collector"))
    }

    @Test fun gameplayVariantsResolveConflictsDeterministically() {
        val result = engine.evaluate(
            TraitActivationContext(GameState(correctGuesses = 100), playerLevel = 10)
        )
        assertTrue(result.isActive("focus_streak"))
        assertFalse(result.isActive("streak_guard"))
        assertTrue(result.effects.grantsStreakProtection)
    }

    @Test fun seasonalCatalogContainsMatchingSeasonRules() {
        val autumn = engine.evaluate(TraitActivationContext(GameState(), activeSeasonId = "autumn"))
        assertTrue(autumn.isActive("autumn_glow"))
        assertFalse(autumn.isActive("winter_comet"))
        assertFalse(autumn.isActive("summer_bloom"))

        val summer = engine.evaluate(
            TraitActivationContext(GameState(), playerLevel = 5, activeSeasonId = "summer")
        )
        assertTrue(summer.isActive("summer_bloom"))
    }

    @Test fun hiddenDiscoveryChainRequiresEarlierDiscoveries() {
        val partial = engine.evaluate(
            TraitActivationContext(
                GameState(correctGuesses = 500),
                playerLevel = 10,
                discoveredTraitIds = setOf("hidden_night_owl", "hidden_precision_mind")
            )
        )
        assertTrue(partial.isActive("hidden_night_owl"))
        assertFalse(partial.isActive("hidden_precision_mind"))

        val complete = engine.evaluate(
            TraitActivationContext(
                GameState(correctGuesses = 500),
                playerLevel = 10,
                discoveredTraitIds = setOf(
                    "hidden_night_owl",
                    "hidden_century_club",
                    "hidden_precision_mind"
                )
            )
        )
        assertTrue(complete.isActive("hidden_century_club"))
        assertTrue(complete.isActive("hidden_precision_mind"))
        assertEquals(
            setOf("hidden_century_club", "hidden_night_owl", "hidden_precision_mind"),
            complete.discoveredHiddenTraitIds
        )
    }

    @Test fun evaluationRemainsReadOnly() {
        val state = GameState(correctGuesses = 75)
        engine.evaluate(TraitActivationContext(state, playerLevel = 10))
        assertEquals(75L, state.correctGuesses)
    }
}
