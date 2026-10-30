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
    fun validatesSignedRemoteEventCatalogWithTheSameRulesAsBundledEvents() {
        val repository = DesktopLiveOpsRepository()
        repository.validateRemoteEvents(
            manifestVersion = 2,
            events = listOf(
                DesktopLiveOpsEvent(
                    id = "winter_event",
                    title = "Winter Event",
                    description = "Seasonal rewards",
                    status = "UPCOMING",
                    startDate = "2026-11-01T00:00:00Z",
                    endDate = "2026-12-31T23:59:59Z",
                    currencyName = "Nebula",
                    maxClaims = 5,
                    rewardNebula = 10,
                    permanentBoostPerClaim = 1,
                    minCorrectGuesses = 100
                )
            )
        )
    }

    @Test
    fun rejectsInvalidSignedRemoteEventDates() {
        val repository = DesktopLiveOpsRepository()
        val error = org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
            repository.validateRemoteEvents(
                manifestVersion = 2,
                events = listOf(
                    DesktopLiveOpsEvent(
                        id = "bad_event",
                        title = "Bad Event",
                        description = "Invalid dates",
                        status = "ACTIVE",
                        startDate = "2026-12-31T00:00:00Z",
                        endDate = "2026-11-01T00:00:00Z",
                        currencyName = "Nebula",
                        maxClaims = 5
                    )
                )
            )
        }
        assertTrue(error.message.orEmpty().contains("dates are invalid"))
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
