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
    fun existingDiscoveryIdsArePreserved() {
        val state = GameState(discoveredTraitIds = setOf("custom_trait"))
        val updated = TraitRewardPolicy.updateDiscoveries(state, "summer")
        assertTrue("Existing discovery must survive updates", "custom_trait" in updated.discoveredTraitIds)
    }
}
