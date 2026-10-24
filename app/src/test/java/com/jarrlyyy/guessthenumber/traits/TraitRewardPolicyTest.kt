package com.jarrlyyy.guessthenumber.traits

import com.jarrlyyy.guessthenumber.domain.model.GameState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TraitRewardPolicyTest {
    @Test
    fun hiddenTraitIsPersistentlyDiscoveredAtItsMilestone() {
        val before = GameState(correctGuesses = 99)
        val notYetDiscovered = TraitRewardPolicy.updateDiscoveries(before, "summer")
        assertTrue("Trait should stay hidden before the milestone", "hidden_night_owl" !in notYetDiscovered.discoveredTraitIds)

        val after = TraitRewardPolicy.updateDiscoveries(before.copy(correctGuesses = 100), "summer")
        assertTrue("Discovery should be stored in GameState", "hidden_night_owl" in after.discoveredTraitIds)
    }

    @Test
    fun nebulaFinderAddsOneNebulaToDailyClaimAfterTwentyFiveCorrectGuesses() {
        assertEquals(0L, TraitRewardPolicy.nebulaBonusForClaim(GameState(correctGuesses = 24), "summer"))
        assertEquals(1L, TraitRewardPolicy.nebulaBonusForClaim(GameState(correctGuesses = 25), "summer"))
    }

    @Test
    fun winterCometAddsSeasonalClaimBonusOnlyDuringWinter() {
        val state = GameState(correctGuesses = 50)
        assertEquals(2L, TraitRewardPolicy.nebulaBonusForClaim(state, "winter"))
        assertEquals(1L, TraitRewardPolicy.nebulaBonusForClaim(state, "summer"))
    }

    @Test
    fun hundredConsecutiveGuessesTraitBonusRequiresActualBestStreak() {
        assertEquals(0L, TraitRewardPolicy.nebulaBonusForClaim(GameState(bestStreak = 99), "summer"))
        assertEquals(1L, TraitRewardPolicy.nebulaBonusForClaim(GameState(bestStreak = 100), "summer"))
    }

    @Test
    fun existingDiscoveryIdsArePreserved() {
        val state = GameState(discoveredTraitIds = setOf("custom_trait"))
        val updated = TraitRewardPolicy.updateDiscoveries(state, "summer")
        assertTrue("Existing discovery must survive updates", "custom_trait" in updated.discoveredTraitIds)
    }
    @Test
    fun seasonMappingUsesExplicitDateAndIsStableAcrossTimezoneChanges() {
        assertEquals("winter", TraitRewardPolicy.currentSeasonId(java.time.LocalDate.of(2026, 2, 28)))
        assertEquals("spring", TraitRewardPolicy.currentSeasonId(java.time.LocalDate.of(2026, 3, 1)))
        assertEquals("summer", TraitRewardPolicy.currentSeasonId(java.time.LocalDate.of(2026, 6, 1)))
        assertEquals("autumn", TraitRewardPolicy.currentSeasonId(java.time.LocalDate.of(2026, 9, 1)))
        assertEquals("winter", TraitRewardPolicy.currentSeasonId(java.time.LocalDate.of(2026, 12, 31)))
    }

    @Test
    fun seasonalExpiryPreservesSavedDiscoveryIds() {
        val state = GameState(correctGuesses = 100, discoveredTraitIds = setOf("hidden_night_owl", "custom_seasonal_memory"))
        val winterState = TraitRewardPolicy.updateDiscoveries(state, "winter")
        val summerState = TraitRewardPolicy.updateDiscoveries(winterState, "summer")
        assertTrue("Existing discoveries survive season changes", "hidden_night_owl" in summerState.discoveredTraitIds)
        assertTrue("Unknown saved IDs remain compatible", "custom_seasonal_memory" in summerState.discoveredTraitIds)
        assertEquals(1L, TraitRewardPolicy.nebulaBonusForClaim(summerState, "summer"))
    }

}
