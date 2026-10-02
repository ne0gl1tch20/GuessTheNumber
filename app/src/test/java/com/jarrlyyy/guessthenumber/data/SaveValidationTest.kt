package com.jarrlyyy.guessthenumber.data

import com.jarrlyyy.guessthenumber.domain.engine.AntiCheatService
import com.jarrlyyy.guessthenumber.domain.engine.AntiTimeTravelService
import com.jarrlyyy.guessthenumber.domain.model.BigNumber
import com.jarrlyyy.guessthenumber.domain.model.GameState
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SaveValidationTest {

    @Test
    fun testCurrencySanitization() {
        val corrupted = GameState(money = BigNumber(-5000), prestige = BigNumber(-10))
        val sanitized = AntiCheatService.sanitizeCurrency(corrupted)
        
        assertEquals(BigNumber.ZERO, sanitized.money)
        assertEquals(BigNumber.ZERO, sanitized.prestige)
    }

    @Test
    fun testAntiTimeTravelBackwardClockDetection() {
        val futureTimestamp = System.currentTimeMillis() + 100000L
        val state = GameState(lastSaveTimestamp = futureTimestamp)
        
        val check = AntiTimeTravelService.checkTimeTravel(state)
        assertTrue(check.timeTravelDetected)
        assertFalse(check.updatedState.autoClickerActive)
        assertTrue(check.updatedState.timeTravelPenaltyUntil > System.currentTimeMillis())
    }
}



