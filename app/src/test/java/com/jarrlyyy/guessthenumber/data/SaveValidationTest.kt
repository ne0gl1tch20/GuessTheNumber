package com.jarrlyyy.guessthenumber.data

import com.jarrlyyy.guessthenumber.domain.engine.AntiCheatService
import com.jarrlyyy.guessthenumber.domain.engine.AntiTimeTravelService
import com.jarrlyyy.guessthenumber.data.store.SaveManager
import com.jarrlyyy.guessthenumber.domain.model.BigNumber
import com.jarrlyyy.guessthenumber.domain.model.GameState
import org.junit.Assert.*
import org.robolectric.RuntimeEnvironment
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SaveValidationTest {


    @Test
    fun testFreshSlotDoesNotCloneAnotherSlot() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        val manager = SaveManager(context)

        val populated = GameState(
            difficultyId = "hard",
            money = BigNumber(12345),
            prestige = BigNumber(42),
            attempts = 17,
            correctGuesses = 9
        )
        manager.saveGame(populated, 1)

        assertTrue(manager.createFreshSlot(2, "extreme"))

        val fresh = manager.loadGame(2)
        assertEquals("extreme", fresh.difficultyId)
        assertEquals(BigNumber.ZERO, fresh.money)
        assertEquals(BigNumber.ZERO, fresh.prestige)
        assertEquals(0L, fresh.attempts)
        assertEquals(0L, fresh.correctGuesses)
        assertEquals(2, manager.getActiveSlot())

        val original = manager.loadGame(1)
        assertEquals(BigNumber(12345), original.money)
        assertEquals(BigNumber(42), original.prestige)
        assertEquals(17L, original.attempts)
        assertEquals(9L, original.correctGuesses)
    }

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



