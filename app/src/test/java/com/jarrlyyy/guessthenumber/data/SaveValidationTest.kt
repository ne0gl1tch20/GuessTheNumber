package com.jarrlyyy.guessthenumber.data

import com.jarrlyyy.guessthenumber.domain.engine.AntiCheatService
import com.jarrlyyy.guessthenumber.domain.engine.AntiTimeTravelService
import com.jarrlyyy.guessthenumber.domain.engine.GameEngine
import com.jarrlyyy.guessthenumber.data.store.SaveManager
import com.jarrlyyy.guessthenumber.data.store.saveDataStore
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.jarrlyyy.guessthenumber.domain.model.BigNumber
import com.jarrlyyy.guessthenumber.domain.model.GameState
import org.junit.Assert.*
import org.robolectric.RuntimeEnvironment
import kotlinx.coroutines.flow.first
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
    fun legacySaveWithOmittedFieldsRoundTripsWithoutLosingProgression() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        val manager = SaveManager(context)
        val primaryKey = stringPreferencesKey("game_state_slot_1_json")
        val json = kotlinx.serialization.json.Json { encodeDefaults = true }
        val legacyState = GameState(
            difficultyId = "hard",
            money = BigNumber("123456789012345678901234567890"),
            prestige = BigNumber("98765432109876543210"),
            ultra = BigNumber("123456789012345"),
            nebula = BigNumber(425),
            attempts = 321,
            correctGuesses = 210,
            upgradeLevels = mapOf("reward_multiplier" to 7, "better_range" to 3),
            prestigeCount = 12,
            ultraCount = 4,
            activeMutators = setOf("double_or_nothing"),
            completedChallenges = setOf("weekly_correct"),
            statistics = com.jarrlyyy.guessthenumber.domain.model.GameStatistics(
                totalGuesses = 400,
                correctGuesses = 210,
                nebulaEarned = 900
            ),
            unlockedWorldIds = setOf("verdant_grove", "crystal_cavern"),
            activeWorldId = "crystal_cavern",
            homeBaseLevel = 5
        )
        val legacyFields = json.encodeToString(GameState.serializer(), legacyState)
        val legacyObject = json.parseToJsonElement(legacyFields).let {
            it as kotlinx.serialization.json.JsonObject
        }.toMutableMap()

        // Model a save written before these fields existed, plus an unknown future field.
        legacyObject.remove("endlessRiftTier")
        legacyObject.remove("endlessRiftBestTier")
        legacyObject.remove("endlessRiftActive")
        legacyObject.remove("activeBossBattleWorldId")
        legacyObject["futureFieldFromNewerVersion"] =
            kotlinx.serialization.json.JsonPrimitive("ignored by this version")
        val legacyJson = kotlinx.serialization.json.JsonObject(legacyObject).toString()

        assertTrue(manager.validateSave(legacyJson))
        context.saveDataStore.edit { prefs -> prefs[primaryKey] = legacyJson }

        val loaded = manager.loadGame(1)
        assertEquals(BigNumber("123456789012345678901234567890"), loaded.money)
        assertEquals(BigNumber("98765432109876543210"), loaded.prestige)
        assertEquals(BigNumber("123456789012345"), loaded.ultra)
        assertEquals(BigNumber(425), loaded.nebula)
        assertEquals(321L, loaded.attempts)
        assertEquals(210L, loaded.correctGuesses)
        assertEquals(mapOf("reward_multiplier" to 7, "better_range" to 3), loaded.upgradeLevels)
        assertEquals(12L, loaded.prestigeCount)
        assertEquals(4L, loaded.ultraCount)
        assertEquals(setOf("double_or_nothing"), loaded.activeMutators)
        assertEquals(setOf("weekly_correct"), loaded.completedChallenges)
        assertEquals(900L, loaded.statistics.nebulaEarned)
        assertEquals(setOf("verdant_grove", "crystal_cavern"), loaded.unlockedWorldIds)
        assertEquals("crystal_cavern", loaded.activeWorldId)
        assertEquals(5, loaded.homeBaseLevel)
        assertEquals(0, loaded.endlessRiftTier)
        assertEquals(0, loaded.endlessRiftBestTier)
        assertFalse(loaded.endlessRiftActive)
        assertNull(loaded.activeBossBattleWorldId)

        assertTrue(manager.saveGame(loaded, 1))
        val restartedManager = SaveManager(context)
        val reloaded = restartedManager.loadGame(1)
        assertEquals(loaded.money, reloaded.money)
        assertEquals(loaded.prestige, reloaded.prestige)
        assertEquals(loaded.ultra, reloaded.ultra)
        assertEquals(loaded.nebula, reloaded.nebula)
        assertEquals(loaded.upgradeLevels, reloaded.upgradeLevels)
        assertEquals(loaded.prestigeCount, reloaded.prestigeCount)
        assertEquals(loaded.ultraCount, reloaded.ultraCount)
        assertEquals(loaded.activeMutators, reloaded.activeMutators)
        assertEquals(loaded.completedChallenges, reloaded.completedChallenges)
        assertEquals(loaded.statistics.nebulaEarned, reloaded.statistics.nebulaEarned)
        assertEquals(loaded.unlockedWorldIds, reloaded.unlockedWorldIds)
        assertEquals(loaded.activeWorldId, reloaded.activeWorldId)
        assertEquals(loaded.homeBaseLevel, reloaded.homeBaseLevel)
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
    fun atomicChallengeClaimCannotRewardTwiceAfterReload() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        val manager = SaveManager(context)
        val engine = GameEngine()
        val eligibleState = GameState(
            correctGuesses = 80,
            statistics = com.jarrlyyy.guessthenumber.domain.model.GameStatistics(
                totalGuesses = 300,
                correctGuesses = 80
            )
        )
        assertTrue(manager.saveGame(eligibleState, 1))

        val firstClaim = manager.updateGameAtomically(1) { saved ->
            engine.claimChallenge(saved, "weekly_correct", 75L).first
        }
        assertNotNull(firstClaim)
        assertEquals(BigNumber(75), firstClaim?.nebula)
        assertEquals(75L, firstClaim?.statistics?.nebulaEarned)
        assertTrue(firstClaim?.completedChallenges?.contains("weekly_correct") == true)

        // A new manager instance models a process restart reading the durable save.
        val restartedManager = SaveManager(context)
        val reloaded = restartedManager.loadGame(1)
        assertEquals(BigNumber(75), reloaded.nebula)
        assertEquals(75L, reloaded.statistics.nebulaEarned)

        val duplicateClaim = restartedManager.updateGameAtomically(1) { saved ->
            engine.claimChallenge(saved, "weekly_correct", 75L).first
        }
        assertNotNull(duplicateClaim)
        assertEquals(BigNumber(75), duplicateClaim?.nebula)
        assertEquals(75L, duplicateClaim?.statistics?.nebulaEarned)
        assertTrue(duplicateClaim?.completedChallenges?.contains("weekly_correct") == true)
    }

    @Test
    fun atomicGameUpdateRejectsCorruptPrimaryWithoutChangingRecoveryBackup() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        val manager = SaveManager(context)
        val primaryKey = stringPreferencesKey("game_state_slot_1_json")
        val backupKey = stringPreferencesKey("game_state_slot_1_backup_json")
        val json = kotlinx.serialization.json.Json { encodeDefaults = true }
        val recoveryState = GameState(money = BigNumber(250))
        val recoveryJson = json.encodeToString(GameState.serializer(), recoveryState)

        context.saveDataStore.edit { prefs ->
            prefs[primaryKey] = "{corrupt"
            prefs[backupKey] = recoveryJson
        }

        val updated = manager.updateGameAtomically(1) {
            it.copy(money = it.money + BigNumber(100))
        }

        assertNull(updated)
        val afterUpdate = context.saveDataStore.data.first()
        assertEquals("{corrupt", afterUpdate[primaryKey])
        assertEquals(recoveryJson, afterUpdate[backupKey])
    }

    @Test
    fun atomicGameUpdateRejectsInvalidTransformedStateWithoutReplacingSave() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        val manager = SaveManager(context)
        manager.saveGame(GameState(money = BigNumber(100)), 1)

        val updated = manager.updateGameAtomically(1) {
            it.copy(money = BigNumber(-1))
        }

        assertNull(updated)
        assertEquals(BigNumber(100), manager.loadGame(1).money)
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



