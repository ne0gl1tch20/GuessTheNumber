package com.jarrlyyy.guessthenumber.data

import com.jarrlyyy.guessthenumber.domain.engine.AntiCheatService
import com.jarrlyyy.guessthenumber.domain.engine.AntiTimeTravelService
import com.jarrlyyy.guessthenumber.data.store.SaveManager
import com.jarrlyyy.guessthenumber.data.store.saveDataStore
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.jarrlyyy.guessthenumber.domain.model.BigNumber
import com.jarrlyyy.guessthenumber.domain.model.GameState
import org.junit.Assert.*
import org.robolectric.RuntimeEnvironment
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
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
    fun atomicGameUpdateUsesLatestSavedState() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        val manager = SaveManager(context)
        manager.saveGame(GameState(money = BigNumber(100)), 1)

        val updated = manager.updateGameAtomically(1) { it.copy(money = it.money + BigNumber(50)) }

        assertEquals(BigNumber(150), updated?.money)
        assertEquals(BigNumber(150), manager.loadGame(1).money)
    }

    @Test
    fun corruptPrimaryCannotOverwriteLastKnownGoodBackup() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        val manager = SaveManager(context)
        val primaryKey = stringPreferencesKey("game_state_slot_1_json")

        manager.saveGame(GameState(money = BigNumber(100)), 1)
        manager.saveGame(GameState(money = BigNumber(200)), 1)

        context.saveDataStore.edit { it[primaryKey] = "{corrupt" }
        manager.saveGame(GameState(money = BigNumber(300)), 1)

        // Simulate another interrupted/corrupt write after the new primary was saved.
        context.saveDataStore.edit { it[primaryKey] = "{corrupt-again" }
        assertEquals(BigNumber(100), manager.loadGame(1).money)
    }

    @Test
    fun saveValidationRejectsImpossibleTargetsAndNegativeEconomyCounters() {
        val manager = SaveManager(RuntimeEnvironment.getApplication())
        val json = kotlinx.serialization.json.Json { encodeDefaults = true }

        val impossibleTarget = GameState(
            currentRangeMin = 1,
            currentRangeMax = 100,
            targetNumber = 101
        )
        val negativeUpgrade = GameState(upgradeLevels = mapOf("reward_multiplier" to -1))
        val negativeStatistics = GameState(
            statistics = com.jarrlyyy.guessthenumber.domain.model.GameStatistics(moneySpent = BigNumber(-1))
        )

        assertFalse(manager.validateSave(json.encodeToString(GameState.serializer(), impossibleTarget)))
        assertFalse(manager.validateSave(json.encodeToString(GameState.serializer(), negativeUpgrade)))
        assertFalse(manager.validateSave(json.encodeToString(GameState.serializer(), negativeStatistics)))
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



