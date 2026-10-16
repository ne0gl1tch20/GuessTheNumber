package com.jarrlyyy.guessthenumber.desktop

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DesktopWorldCatalogTest {
    @Test
    fun loadsCanonicalAndroidWorldCatalogAndProgressionRules() {
        val worlds = DesktopWorldCatalog.load()
        assertEquals(4, worlds.size)
        assertEquals(listOf("verdant_grove", "crystal_caverns", "ember_summit", "nebula_rift"), worlds.map { it.id })
        assertEquals("verdant_guardian", worlds[1].unlockBossId)
        assertEquals(250L, worlds.last().baseNebulaReward)
    }
}
