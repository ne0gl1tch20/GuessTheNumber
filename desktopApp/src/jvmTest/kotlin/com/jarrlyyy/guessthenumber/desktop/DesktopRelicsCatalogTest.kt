package com.jarrlyyy.guessthenumber.desktop

import com.jarrlyyy.guessthenumber.domain.model.GameState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DesktopRelicsCatalogTest {
    @Test
    fun bundledCatalogHasUniqueRelicsAndConsistentSets() {
        val catalog = DesktopRelicsCatalog.load()
        assertTrue(catalog.size >= 6)
        assertEquals(catalog.size, catalog.map { it.id }.distinct().size)
        assertTrue(catalog.groupBy { it.setId }.values.all { set ->
            set.map { it.setName }.distinct().size == 1 &&
                set.map { it.twoPieceBonus }.distinct().size == 1 &&
                set.map { it.threePieceBonus }.distinct().size == 1
        })
    }

    @Test
    fun invalidCatalogsAreRejected() {
        assertNull(runCatching { DesktopRelicsCatalog.parse("""{"schemaVersion":1,"relics":[]}""") }.getOrNull())
        val duplicate = """{"schemaVersion":1,"relics":[
          {"id":"x","name":"X","description":"D","setId":"s","setName":"Set","rarity":"Common","unlockBossId":"b","unlockBossName":"Boss","twoPieceBonus":"A","threePieceBonus":"B"},
          {"id":"x","name":"Y","description":"D","setId":"s","setName":"Set","rarity":"Common","unlockBossId":"b","unlockBossName":"Boss","twoPieceBonus":"A","threePieceBonus":"B"}
        ]}"""
        assertNull(runCatching { DesktopRelicsCatalog.parse(duplicate) }.getOrNull())
    }

    @Test
    fun equipmentRequiresOwnershipAndHonorsThreeSlotLimit() {
        val owned = GameState(relicInventory = setOf("a", "b", "c", "d"))
        assertNull(DesktopRelicRules.equip(owned, "missing"))
        val first = DesktopRelicRules.equip(owned, "a")!!
        val second = DesktopRelicRules.equip(first, "b")!!
        val third = DesktopRelicRules.equip(second, "c")!!
        assertEquals(setOf("a", "b", "c"), third.equippedRelicIds)
        assertNull(DesktopRelicRules.equip(third, "d"))
    }

    @Test
    fun setBonusesActivateAtTwoAndThreePieces() {
        val catalog = DesktopRelicsCatalog.load()
        val verdant = catalog.filter { it.setId == "verdant" }.map { it.id }
        assertTrue(DesktopRelicRules.activeSetBonuses(catalog, verdant.take(1).toSet()).isEmpty())
        assertTrue(DesktopRelicRules.activeSetBonuses(catalog, verdant.take(2).toSet()).single().contains("2-piece"))
        assertTrue(DesktopRelicRules.activeSetBonuses(catalog, verdant.take(3).toSet()).single().contains("3-piece"))
    }
}
