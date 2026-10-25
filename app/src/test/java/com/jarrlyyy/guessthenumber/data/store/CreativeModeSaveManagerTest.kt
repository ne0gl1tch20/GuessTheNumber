package com.jarrlyyy.guessthenumber.data.store

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class CreativeModeSaveManagerTest {
    @Test
    fun creativeSavesAreIsolatedBySlotAndResetWithTheirSlot() = runBlocking {
        val manager = SaveManager(RuntimeEnvironment.getApplication())
        manager.resetData()

        val first = CreativeModeSave(
            name = "First slot",
            enabled = true,
            rangeMax = 250,
            effects = listOf(CreativeModeEffect("hints", CreativeEffectType.EXTRA_HINTS, 2))
        )
        val second = CreativeModeSave(
            name = "Second slot",
            enabled = false,
            rangeMax = 500,
            effects = listOf(CreativeModeEffect("attempts", CreativeEffectType.ATTEMPT_LIMIT, 12))
        )

        assertTrue(manager.saveCreativeModeSave(1, first))
        assertTrue(manager.saveCreativeModeSave(2, second))
        assertEquals(first, manager.loadCreativeModeSave(1))
        assertEquals(second, manager.loadCreativeModeSave(2))

        manager.resetSlot(1)
        assertNull(manager.loadCreativeModeSave(1))
        assertEquals(second, manager.loadCreativeModeSave(2))
        manager.resetData()
    }

    @Test
    fun invalidSlotAndInvalidEffectDataAreRejected() = runBlocking {
        val manager = SaveManager(RuntimeEnvironment.getApplication())
        assertTrue(!manager.saveCreativeModeSave(0, CreativeModeSave()))
        assertTrue(!manager.saveCreativeModeSave(1, CreativeModeSave(
            effects = listOf(CreativeModeEffect("too-many-hints", CreativeEffectType.EXTRA_HINTS, 50))
        )))
        assertNull(manager.loadCreativeModeSave(11))
    }
}
