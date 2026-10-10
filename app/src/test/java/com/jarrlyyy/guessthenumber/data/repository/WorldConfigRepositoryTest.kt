package com.jarrlyyy.guessthenumber.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WorldConfigRepositoryTest {
    private fun repository() = WorldConfigRepository(RuntimeEnvironment.getApplication())

    private fun loadWorlds(): List<WorldDefinition> = repository().loadWorlds()

    @Test
    fun bundledWorldsLoadWithExpectedBossStatsAndRewards() {
        val worlds = loadWorlds()

        assertEquals(
            listOf("verdant_grove", "crystal_caverns", "ember_summit", "nebula_rift"),
            worlds.map { it.id }
        )
        assertEquals(listOf(3, 5, 7, 10), worlds.map { it.hp })
        assertEquals(listOf("25000", "100000", "500000", "5000000"), worlds.map { it.baseMoneyReward })
        assertEquals(listOf(10L, 25L, 75L, 250L), worlds.map { it.baseNebulaReward })
    }


    @Test
    fun rejectsInvalidCoordinatesAndMissingBossReferences() {
        val repository = repository()
        val worlds = repository.loadWorlds()

        assertTrue(repository.isValid(worlds))
        assertFalse(repository.isValid(worlds.mapIndexed { index, world ->
            if (index == 0) world.copy(x = 1.5f) else world
        }))
        assertFalse(repository.isValid(worlds.mapIndexed { index, world ->
            if (index == 1) world.copy(unlockBossId = "missing_boss") else world
        }))
        assertFalse(repository.isValid(worlds.mapIndexed { index, world ->
            if (index == 0) world.copy(baseNebulaReward = -1) else world
        }))
    }

    @Test
    fun progressionRequirementsAreLoadedFromContent() {
        val worlds = loadWorlds()
        val crystal = worlds.first { it.id == "crystal_caverns" }
        val ember = worlds.first { it.id == "ember_summit" }
        val nebula = worlds.first { it.id == "nebula_rift" }

        assertEquals("verdant_guardian", crystal.unlockBossId)
        assertEquals(25L, crystal.unlockCorrectGuesses)
        assertEquals(5, crystal.unlockUpgradeCount)
        assertEquals(1, ember.unlockPrestigeCount)
        assertEquals(1, nebula.unlockUltraCount)
        assertEquals(500L, nebula.fightCorrectGuesses)
        assertEquals(75, nebula.fightUpgradeCount)
    }
}
