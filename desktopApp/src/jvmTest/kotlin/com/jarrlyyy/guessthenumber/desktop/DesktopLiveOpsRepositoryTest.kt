package com.jarrlyyy.guessthenumber.desktop

import com.jarrlyyy.guessthenumber.domain.model.BigNumber
import com.jarrlyyy.guessthenumber.domain.model.GameState
import com.jarrlyyy.guessthenumber.domain.model.LiveOpsClaimDefinition
import com.jarrlyyy.guessthenumber.domain.model.LiveOpsClaimPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DesktopLiveOpsRepositoryTest {
    @Test
    fun loadsCanonicalAndroidEventsAndFeatureFlags() {
        val snapshot = DesktopLiveOpsRepository().load()

        assertTrue(snapshot.error.orEmpty(), snapshot.error == null)
        assertTrue(snapshot.enabled)
        assertTrue(snapshot.events.isNotEmpty())
        assertTrue(snapshot.events.any { it.id == "season_1_cyber_guess" })
        assertTrue(snapshot.events.any { it.id == "holiday_neon_rush" })
    }

    @Test
    fun sharedClaimPolicyAwardsConfiguredEventReward() {
        val event = DesktopLiveOpsRepository().load().events.first { it.id == "season_1_cyber_guess" }
        val eligibleState = GameState(
            correctGuesses = event.minCorrectGuesses,
            prestigeCount = event.minPrestigeCount,
            ultraCount = event.minUltraCount,
            nebula = BigNumber(7)
        )

        val updated = LiveOpsClaimPolicy.apply(
            eligibleState,
            LiveOpsClaimDefinition(
                id = event.id,
                maxClaims = event.maxClaims,
                rewardNebula = event.rewardNebula,
                permanentBoostPerClaim = event.permanentBoostPerClaim,
                minCorrectGuesses = event.minCorrectGuesses,
                minPrestigeCount = event.minPrestigeCount,
                minUltraCount = event.minUltraCount
            )
        )

        assertNotNull(updated)
        assertEquals(BigNumber(7 + event.rewardNebula), updated!!.nebula)
        assertEquals(1, updated.liveOpsClaims[event.id])
        assertEquals(event.permanentBoostPerClaim.coerceAtMost(25), updated.permanentEventBoosts)
    }

    @Test
    fun sharedClaimPolicyRejectsIneligibleEventProgression() {
        val event = DesktopLiveOpsRepository().load().events.first { it.id == "holiday_neon_rush" }
        val ineligibleState = GameState(correctGuesses = event.minCorrectGuesses - 1)

        val updated = LiveOpsClaimPolicy.apply(
            ineligibleState,
            LiveOpsClaimDefinition(
                id = event.id,
                maxClaims = event.maxClaims,
                rewardNebula = event.rewardNebula,
                permanentBoostPerClaim = event.permanentBoostPerClaim,
                minCorrectGuesses = event.minCorrectGuesses,
                minPrestigeCount = event.minPrestigeCount,
                minUltraCount = event.minUltraCount
            )
        )

        assertEquals(null, updated)
    }
}
