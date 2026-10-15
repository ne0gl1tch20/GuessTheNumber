package com.jarrlyyy.guessthenumber.desktop.economy

import com.jarrlyyy.guessthenumber.domain.engine.GameEngine
import com.jarrlyyy.guessthenumber.domain.model.BigNumber
import com.jarrlyyy.guessthenumber.domain.model.GameState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DesktopEconomyRulesTest {
    private val repository = DesktopEconomyRepository()

    @Test
    fun desktopLoadsCanonicalAndroidEconomyDefinitions() {
        assertTrue(repository.upgrades().any { it.id == "reward_multiplier" })
        assertTrue(repository.prestigeUpgrades().any { it.id == "prestige_money_boost" })
        assertTrue(repository.ultraUpgrades().any { it.id == "ultra_money_boost" })
        assertTrue(repository.shopItems().any { it.id == "auto_clicker_basic" })
        assertTrue(repository.prestigeShopItems().isNotEmpty())
        assertTrue(repository.ultraShopItems().isNotEmpty())
        assertTrue(repository.talents().isNotEmpty())
    }

    @Test
    fun moneyUpgradeChargesExactNextLevelCostAndIncrementsLevel() {
        val upgrade = repository.upgrades().first { it.id == "reward_multiplier" }
        val starting = GameState(money = BigNumber(100))
        val next = DesktopEconomyRules.buyMoneyUpgrade(starting, upgrade)

        assertNotNull(next)
        assertEquals(BigNumber.ZERO, next!!.money)
        assertEquals(1, next.upgradeLevels[upgrade.id])
        assertEquals(BigNumber(100), next.statistics.moneySpent)
        assertEquals(1, next.dailyQuestUpgradePurchases)
        assertNull(DesktopEconomyRules.buyMoneyUpgrade(starting.copy(money = BigNumber(99)), upgrade))
    }

    @Test
    fun prestigeResetPreservesPrestigeCurrencyAndClearsMoneyUpgrades() {
        val engine = GameEngine()
        val state = GameState(
            money = BigNumber("1e20"),
            prestige = BigNumber(7),
            upgradeLevels = mapOf("reward_multiplier" to 4),
            prestigeCount = 0
        )

        val next = DesktopEconomyRules.prestigeReset(state, engine)
        assertNotNull(next)
        assertEquals(BigNumber.ZERO, next!!.money)
        assertTrue(next.prestige > state.prestige)
        assertTrue(next.upgradeLevels.isEmpty())
        assertEquals(1L, next.prestigeCount)
        assertEquals(1L, next.statistics.prestigesCount)
        assertNull(DesktopEconomyRules.prestigeReset(GameState(), engine))
    }

    @Test
    fun ultraResetRequiresPrestigeThresholdAndPreservesUltraUpgradeLevels() {
        val engine = GameEngine()
        val underThreshold = GameState(
            money = BigNumber("1e30"),
            prestige = BigNumber(999),
            ultraUpgradeLevels = mapOf("ultra_money_boost" to 2)
        )
        assertNull(DesktopEconomyRules.ultraReset(underThreshold, engine))

        val eligible = underThreshold.copy(prestige = BigNumber(1_000))
        val next = DesktopEconomyRules.ultraReset(eligible, engine)
        assertNotNull(next)
        assertEquals(BigNumber.ZERO, next!!.money)
        assertEquals(BigNumber.ZERO, next.prestige)
        assertTrue(next.ultra > eligible.ultra)
        assertEquals(1L, next.ultraCount)
        assertEquals(2, next.ultraUpgradeLevels["ultra_money_boost"])
    }

    @Test
    fun shopPurchasesCannotBeRepeatedAndTalentParentsAreEnforced() {
        val item = repository.shopItems().first { it.id == "auto_clicker_basic" }
        val purchased = DesktopEconomyRules.buyNebulaShopItem(GameState(nebula = BigNumber(item.nebulaCost)), item)
        assertNotNull(purchased)
        assertTrue(purchased!!.autoClickerActive)
        assertNull(DesktopEconomyRules.buyNebulaShopItem(purchased, item))

        val child = repository.talents().firstOrNull { it.id == "talent_reward_1" }
        if (child != null) {
            val blocked = DesktopEconomyRules.buyTalent(GameState(prestige = BigNumber("1e10")), child)
            assertNull(blocked)
        }
    }
}
