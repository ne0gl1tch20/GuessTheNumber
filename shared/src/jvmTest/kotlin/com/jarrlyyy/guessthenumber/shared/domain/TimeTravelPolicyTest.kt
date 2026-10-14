package com.jarrlyyy.guessthenumber.shared.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TimeTravelPolicyTest {
    @Test
    fun backwardClockAppliesPenaltyAndStopsAutoclicker() {
        val state = GameState(lastSaveTimestamp = 20_000L, autoClickerActive = true)

        val result = TimeTravelPolicy.check(state, nowMillis = 5_000L)

        assertTrue(result.timeTravelDetected)
        assertEquals(15L, result.timeDifferenceSeconds)
        assertFalse(result.updatedState.autoClickerActive)
        assertEquals(5_000L + TimeTravelPolicy.PENALTY_DURATION_MILLIS, result.updatedState.timeTravelPenaltyUntil)
        assertEquals(5_000L, result.updatedState.lastSaveTimestamp)
    }

    @Test
    fun normalClockDoesNotApplyPenalty() {
        val state = GameState(lastSaveTimestamp = 10_000L)

        val result = TimeTravelPolicy.check(state, nowMillis = 11_000L)

        assertFalse(result.timeTravelDetected)
        assertEquals(0L, result.timeDifferenceSeconds)
        assertEquals(11_000L, result.updatedState.lastSaveTimestamp)
    }
}
