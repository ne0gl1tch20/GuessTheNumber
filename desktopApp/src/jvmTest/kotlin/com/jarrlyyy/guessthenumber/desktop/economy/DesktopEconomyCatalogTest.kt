package com.jarrlyyy.guessthenumber.desktop.economy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DesktopEconomyCatalogTest {
    @Test
    fun desktopBundlesTheFullAndroidEconomyCatalogs() {
        val catalog = DesktopEconomyCatalog.load()

        assertEquals(
            listOf(
                "reward_multiplier", "better_range", "critical_boost", "starting_money_boost",
                "auto_efficiency", "prestige_boost", "lucky_multiplier", "critical_multiplier",
                "auto_income_boost", "ultra_efficiency", "discount_bargain"
            ),
            catalog.upgrades.map { it.id }
        )
        assertEquals(3, catalog.prestigeUpgrades.size)
        assertEquals(3, catalog.ultraUpgrades.size)
        assertTrue(catalog.shopItems.any { it.id == "auto_clicker_basic" })
        assertTrue(catalog.shopItems.any { it.id.startsWith("cosmetic_") })
    }

    @Test
    fun repositoryLoadsCanonicalTalentAndCurrencyShopCatalogsFromGameResources() {
        val repository = DesktopEconomyRepository()

        assertEquals(6, repository.talents().size)
        assertEquals(
            listOf("prestige_auto_speed", "prestige_luck_charm"),
            repository.prestigeShopItems().map { it.id }
        )
        assertEquals(
            listOf("ultra_universal_harmony", "ultra_infinite_multiplier", "ultra_flex_badge"),
            repository.ultraShopItems().map { it.id }
        )
    }
}
