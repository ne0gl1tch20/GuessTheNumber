package com.jarrlyyy.guessthenumber.shared.domain

import com.jarrlyyy.guessthenumber.domain.model.GameState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AntiTimeTravelRulesTest {
    @Test
    fun backwardClockPausesAutoClickerAndAppliesTwentyFourHourPenalty() {
        val now = 1_800_000_000_000L
        val state = GameState(
            lastSaveTimestamp = now + 90_000L,
            autoClickerActive = true
        )

        val result = AntiTimeTravelRules.check(state, now)

        assertTrue(result.detected)
        assertEquals(90L, result.differenceSeconds)
        assertFalse(result.updatedState.autoClickerActive)
        assertEquals(now + AntiTimeTravelRules.PENALTY_MILLIS, result.updatedState.timeTravelPenaltyUntil)
        assertEquals(now, result.updatedState.lastSaveTimestamp)
    }

    @Test
    fun forwardClockAdjustmentDoesNotTriggerPenalty() {
        val now = 1_800_000_000_000L
        val state = GameState(
            lastSaveTimestamp = now - 90_000L,
            autoClickerActive = true,
            timeTravelPenaltyUntil = 0L
        )

        val result = AntiTimeTravelRules.check(state, now)

        assertFalse(result.detected)
        assertEquals(0L, result.differenceSeconds)
        assertTrue(result.updatedState.autoClickerActive)
        assertEquals(0L, result.updatedState.timeTravelPenaltyUntil)
        assertEquals(now, result.updatedState.lastSaveTimestamp)
    }
}
