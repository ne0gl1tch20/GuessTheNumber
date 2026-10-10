package com.jarrlyyy.guessthenumber.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WorldConfigRepositoryTest {
    private fun loadWorlds(): List<WorldDefinition> =
        WorldConfigRepository(RuntimeEnvironment.getApplication()).loadWorlds()

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
    fun progressionRequirementsAreLoadedFromContent() {
        val worlds = loadWorlds()
        val crystal = worlds.firstOrNull { it.id == "crystal_caverns" }
        val ember = worlds.firstOrNull { it.id == "ember_summit" }
        val nebula = worlds.firstOrNull { it.id == "nebula_rift" }

        assertNotNull(crystal)
        assertNotNull(ember)
        assertNotNull(nebula)
        assertEquals("verdant_guardian", crystal!!.unlockBossId)
        assertEquals(25L, crystal.unlockCorrectGuesses)
        assertEquals(5, crystal.unlockUpgradeCount)
        assertEquals(1, ember!!.unlockPrestigeCount)
        assertEquals(1, nebula!!.unlockUltraCount)
        assertEquals(500L, nebula.fightCorrectGuesses)
        assertEquals(75, nebula.fightUpgradeCount)
    }
}
