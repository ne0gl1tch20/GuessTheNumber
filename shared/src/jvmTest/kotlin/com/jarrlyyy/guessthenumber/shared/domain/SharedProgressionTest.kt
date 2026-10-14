package com.jarrlyyy.guessthenumber.shared.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SharedProgressionTest {
    @Test
    fun cashBurstAppliesRewardAndTracksEvent() {
        val initial = GameState(money = BigNumber(1_000))
        val result = RandomEventEngine().apply(initial, RandomEventType.CASH_BURST)

        assertEquals(BigNumber(1_100), result.state.money)
        assertEquals(1L, result.state.statistics.randomEventsTriggered)
        assertEquals("cash_burst", result.state.lastRandomEventId)
    }

    @Test
    fun liveOpsClaimIsBoundedAndCanOnlyBeClaimedOnce() {
        val initial = GameState(correctGuesses = 10)
        val definition = LiveOpsClaimDefinition(
            id = "test_event",
            maxClaims = 1,
            rewardNebula = 5,
            permanentBoostPerClaim = 1,
            minCorrectGuesses = 10,
            minPrestigeCount = 0,
            minUltraCount = 0
        )

        val claimed = LiveOpsClaimPolicy.apply(initial, definition)
        assertEquals(BigNumber(5), claimed?.nebula)
        assertEquals(1, claimed?.liveOpsClaims?.get("test_event"))
        assertNull(claimed?.let { LiveOpsClaimPolicy.apply(it, definition) })
    }
}
