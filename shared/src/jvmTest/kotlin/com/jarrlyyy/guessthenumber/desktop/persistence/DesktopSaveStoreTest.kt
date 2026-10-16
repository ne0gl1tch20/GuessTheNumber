package com.jarrlyyy.guessthenumber.desktop.persistence

import com.jarrlyyy.guessthenumber.domain.model.GameState
import kotlinx.coroutines.runBlocking
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DesktopSaveStoreTest {
    @Test
    fun saveAndReloadRoundTripPreservesState() = runBlocking {
        val directory = Files.createTempDirectory("gtn-save-roundtrip")
        val store = DesktopSaveStore(directory)
        val expected = GameState(
            targetNumber = 73,
            currentRangeMin = 1,
            currentRangeMax = 100,
            attempts = 4,
            correctGuesses = 1,
            streak = 1,
            bestStreak = 1,
            lastSaveTimestamp = 1234L
        )

        assertTrue(store.save(1, expected))
        val actual = store.load(1)

        assertEquals(expected, actual)
    }

    @Test
    fun resetRemovesPreviousBackupSoOldProgressCannotBeRecovered() = runBlocking {
        val directory = Files.createTempDirectory("gtn-save-reset")
        val store = DesktopSaveStore(directory)
        val oldState = GameState(targetNumber = 50, lastSaveTimestamp = 100L)
        assertTrue(store.save(1, oldState))
        val newState = GameState(targetNumber = 42, lastSaveTimestamp = 200L)

        assertTrue(store.reset(1, newState))
        assertEquals(newState, store.load(1))
        assertFalse(Files.exists(directory.resolve("slot-1.backup.json")))
    }

    @Test
    fun corruptPrimaryRecoversFromBackup() = runBlocking {
        val directory = Files.createTempDirectory("gtn-save-recovery")
        val store = DesktopSaveStore(directory)
        val first = GameState(targetNumber = 20, lastSaveTimestamp = 100L)
        val second = GameState(targetNumber = 80, lastSaveTimestamp = 200L)

        assertTrue(store.save(1, first))
        assertTrue(store.save(1, second))
        Files.writeString(directory.resolve("slot-1.json"), "not-json")

        val recovered = store.load(1)

        assertEquals(first, recovered)
        assertEquals(Files.readString(directory.resolve("slot-1.json")), Files.readString(directory.resolve("slot-1.backup.json")))
    }

    @Test
    fun corruptPrimaryWithoutBackupDoesNotBecomeVisibleState() = runBlocking {
        val directory = Files.createTempDirectory("gtn-save-corrupt")
        Files.createDirectories(directory)
        Files.writeString(directory.resolve("slot-1.json"), "not-json")
        val store = DesktopSaveStore(directory)

        assertEquals(null, store.load(1))
    }


    @Test
    fun slotsSupportCreateSwitchDuplicateAndBackupRestore() = runBlocking {
        val directory = Files.createTempDirectory("gtn-save-slots")
        val store = DesktopSaveStore(directory)
        val first = GameState(targetNumber = 12, lastSaveTimestamp = 100L)
        val second = GameState(targetNumber = 88, lastSaveTimestamp = 200L)
        assertTrue(store.createFreshSlot(1, first))
        assertFalse(store.createFreshSlot(1, second))
        assertTrue(store.setActiveSlot(1))
        assertEquals(1, store.activeSlot())
        assertTrue(store.save(1, second))
        assertTrue(store.restoreBackup(1))
        assertEquals(first, store.load(1))
        assertTrue(store.slotSummaries().any { it.slot == 1 && !it.isEmpty && it.hasBackup })
        assertTrue(store.duplicateSlot(1, 2))
        assertEquals(first.targetNumber, store.load(2)?.targetNumber)
        assertFalse(store.duplicateSlot(1, 2))
    }

    @Test
    fun legacyJsonDefaultsRemainCompatible() = runBlocking {
        val directory = Files.createTempDirectory("gtn-save-legacy")
        Files.createDirectories(directory)
        Files.writeString(
            directory.resolve("slot-1.json"),
            """{"targetNumber":42,"lastSaveTimestamp":1}"""
        )
        val store = DesktopSaveStore(directory)

        val state = store.load(1)

        assertNotNull(state)
        assertEquals(42L, state.targetNumber)
        assertEquals(0L, state.attempts)
        assertEquals("classic", state.difficultyId)
    }

    @Test
    fun failedPrimaryWriteLeavesPreviousStateOnDisk() = runBlocking {
        val directory = Files.createTempDirectory("gtn-save-failure")
        val normal = DesktopSaveStore(directory)
        val first = GameState(targetNumber = 11, lastSaveTimestamp = 100L)
        val second = GameState(targetNumber = 99, lastSaveTimestamp = 200L)
        assertTrue(normal.save(1, first))

        val failingOps = object : DesktopSaveFileOps by DefaultDesktopSaveFileOps {
            override fun moveReplace(source: Path, target: Path) {
                if (target.fileName.toString() == "slot-1.json") {
                    throw IllegalStateException("injected primary write failure")
                }
                DefaultDesktopSaveFileOps.moveReplace(source, target)
            }
        }
        val failingStore = DesktopSaveStore(directory, failingOps)

        assertFalse(failingStore.save(1, second))
        assertEquals(first, normal.load(1))
    }
}
