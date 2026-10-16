package com.jarrlyyy.guessthenumber.desktop

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DesktopAchievementsCatalogTest {
    @Test
    fun loadsCanonicalAndroidAchievementCatalogAndUniqueIds() {
        val catalog = DesktopAchievementsCatalog.load()
        assertEquals(27, catalog.size)
        assertEquals(catalog.size, catalog.map { it.id }.distinct().size)
        assertTrue(catalog.any { it.id == "correct_10" && it.rewardNebula == 10L })
        assertTrue(catalog.any { it.id == "billionaire" && it.tier == "diamond" })
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsUnsupportedCatalogSchema() {
        DesktopAchievementsCatalog.parse("""{"schemaVersion":2,"achievements":[]}""")
    }
}
