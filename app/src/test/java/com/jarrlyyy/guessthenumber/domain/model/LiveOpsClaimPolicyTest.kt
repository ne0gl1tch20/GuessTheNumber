package com.jarrlyyy.guessthenumber.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Test

class LiveOpsClaimPolicyTest {
    private val event = LiveOpsClaimDefinition(
        id = "test_season",
        maxClaims = 2,
        rewardNebula = 10,
        permanentBoostPerClaim = 1,
        minCorrectGuesses = 100,
        minPrestigeCount = 1,
        minUltraCount = 0
    )

    private val eligibleState = GameState(
        correctGuesses = 100,
        prestigeCount = 1,
        nebula = BigNumber(5),
        permanentEventBoosts = 3
    )

    @Test
    fun validClaimAwardsNebulaAndRecordsProgress() {
        val updated = LiveOpsClaimPolicy.apply(eligibleState, event)

        assertNotNull(updated)
        assertEquals(BigNumber(15), updated!!.nebula)
        assertEquals(1, updated.liveOpsClaims["test_season"])
        assertEquals(4, updated.permanentEventBoosts)
        assertEquals(10L, updated.statistics.nebulaEarned)
    }

    @Test
    fun claimIsRejectedWhenProgressionRequirementsAreNotMet() {
        assertNull(LiveOpsClaimPolicy.apply(eligibleState.copy(correctGuesses = 99), event))
        assertNull(LiveOpsClaimPolicy.apply(eligibleState.copy(prestigeCount = 0), event))
    }

    @Test
    fun claimIsRejectedWhenMaximumClaimsHaveBeenReached() {
        val alreadyClaimed = eligibleState.copy(liveOpsClaims = mapOf("test_season" to 2))
        assertNull(LiveOpsClaimPolicy.apply(alreadyClaimed, event))
    }

    @Test
    fun permanentBonusNeverExceedsLevelTwentyFive() {
        val nearCap = eligibleState.copy(permanentEventBoosts = 25)
        val updated = LiveOpsClaimPolicy.apply(nearCap, event)

        assertNotNull(updated)
        assertEquals(25, updated!!.permanentEventBoosts)
    }

    @Test
    fun rejectsClaimWhenLifetimeNebulaStatisticWouldOverflow() {
        val nearLimit = eligibleState.copy(
            statistics = eligibleState.statistics.copy(nebulaEarned = Long.MAX_VALUE - 5L)
        )

        assertNull(LiveOpsClaimPolicy.apply(nearLimit, event))
    }

    @Test
    fun rejectsClaimWhenLifetimeNebulaStatisticIsAlreadyNegative() {
        val corruptedStatistic = eligibleState.copy(
            statistics = eligibleState.statistics.copy(nebulaEarned = -1L)
        )

        assertNull(LiveOpsClaimPolicy.apply(corruptedStatistic, event))
    }

    @Test
    fun clampsPermanentBoostWithoutIntegerOverflow() {
        val invalidLargeValue = eligibleState.copy(permanentEventBoosts = Int.MAX_VALUE)
        val updated = LiveOpsClaimPolicy.apply(invalidLargeValue, event)

        assertNotNull(updated)
        assertEquals(25, updated!!.permanentEventBoosts)
    }

    @Test
    fun rejectsClaimsWhenStoredProgressionOrBoostStateIsNegative() {
        val corruptedStates = listOf(
            eligibleState.copy(correctGuesses = -1L),
            eligibleState.copy(prestigeCount = -1L),
            eligibleState.copy(ultraCount = -1L),
            eligibleState.copy(permanentEventBoosts = -1)
        )

        corruptedStates.forEach { corrupted ->
            assertNull(LiveOpsClaimPolicy.apply(corrupted, event))
        }
    }

    @Test
    fun invalidClaimDefinitionIsRejected() {
        assertNull(LiveOpsClaimPolicy.apply(eligibleState, event.copy(id = " ")))
        assertNull(LiveOpsClaimPolicy.apply(eligibleState, event.copy(maxClaims = 0)))
        assertNull(LiveOpsClaimPolicy.apply(eligibleState, event.copy(rewardNebula = -1)))
        assertNull(LiveOpsClaimPolicy.apply(eligibleState, event.copy(permanentBoostPerClaim = -1)))
        assertNull(LiveOpsClaimPolicy.apply(eligibleState, event.copy(minCorrectGuesses = -1)))
        assertNull(LiveOpsClaimPolicy.apply(eligibleState, event.copy(minPrestigeCount = -1)))
        assertNull(LiveOpsClaimPolicy.apply(eligibleState, event.copy(minUltraCount = -1)))
    }
}
