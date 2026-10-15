package com.jarrlyyy.guessthenumber.desktop.economy

import com.jarrlyyy.guessthenumber.domain.engine.GameEngine
import com.jarrlyyy.guessthenumber.domain.model.BigNumber
import com.jarrlyyy.guessthenumber.domain.model.GameState
import com.jarrlyyy.guessthenumber.domain.model.UpgradeDef
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DesktopEconomyTransactionsTest {
    private val rewardUpgrade = UpgradeDef(
        id = "reward_multiplier",
        name = "Reward Multiplier",
        description = "Raises correct-guess rewards.",
        maxLevel = 3,
        baseCost = "100",
        costMultiplier = 1.5,
        effectPerLevel = 0.25
    )

    @Test
    fun buysOneUpgradeWithExactCostAndTracksSpending() {
        val initial = GameState(money = BigNumber(1000))
        val result = DesktopEconomyTransactions.buyUpgrade(initial, rewardUpgrade)

        assertTrue(result.accepted)
        assertEquals(BigNumber(900), result.state.money)
        assertEquals(1, result.state.upgradeLevels["reward_multiplier"])
        assertEquals(BigNumber(100), result.state.statistics.moneySpent)
        assertEquals(1, result.state.dailyQuestUpgradePurchases)
    }

    @Test
    fun unaffordableUpgradeDoesNotMutateState() {
        val initial = GameState(money = BigNumber(99))
        val result = DesktopEconomyTransactions.buyUpgrade(initial, rewardUpgrade)

        assertFalse(result.accepted)
        assertEquals(initial, result.state)
    }

    @Test
    fun maxLevelUpgradeCannotBePurchasedAgain() {
        val initial = GameState(
            money = BigNumber(1000),
            upgradeLevels = mapOf("reward_multiplier" to 3)
        )
        val result = DesktopEconomyTransactions.buyUpgrade(initial, rewardUpgrade)

        assertFalse(result.accepted)
        assertEquals(initial, result.state)
    }

    @Test
    fun prestigeResetUsesProductionRequirementsAndPreservesPermanentProgress() {
        val engine = GameEngine()
        val initial = GameState(
            money = BigNumber("50000000"),
            upgradeLevels = mapOf("reward_multiplier" to 2),
            prestigeUpgradeLevels = mapOf("critical_precision_boost" to 1),
            prestigeShopPurchases = setOf("talent_crit_1"),
            prestigeCount = 0
        )

        val result = DesktopEconomyTransactions.prestigeReset(initial, engine)

        assertTrue(result.accepted)
        assertEquals(BigNumber.ZERO, result.state.money)
        assertTrue(result.state.prestige > BigNumber.ZERO)
        assertTrue(result.state.upgradeLevels.isEmpty())
        assertEquals(initial.prestigeUpgradeLevels, result.state.prestigeUpgradeLevels)
        assertEquals(initial.prestigeShopPurchases, result.state.prestigeShopPurchases)
        assertEquals(1L, result.state.prestigeCount)
        assertEquals(1L, result.state.statistics.prestigesCount)
    }

    @Test
    fun prestigeResetBelowRequirementLeavesStateUntouched() {
        val initial = GameState(money = BigNumber(49_999_999))
        val result = DesktopEconomyTransactions.prestigeReset(initial, GameEngine())

        assertFalse(result.accepted)
        assertEquals(initial, result.state)
    }

    @Test
    fun ultraResetRequiresPrestigeAndPreservesUltraUpgradesAndShopPurchases() {
        val initial = GameState(
            money = BigNumber("1000000000000"),
            prestige = BigNumber(1000),
            ultraUpgradeLevels = mapOf("ultra_efficiency" to 1),
            shopPurchases = setOf("cosmetic_blue"),
            ultraCount = 0
        )

        val result = DesktopEconomyTransactions.ultraReset(initial, GameEngine())

        assertTrue(result.accepted)
        assertEquals(BigNumber.ZERO, result.state.money)
        assertEquals(BigNumber.ZERO, result.state.prestige)
        assertTrue(result.state.ultra > BigNumber.ZERO)
        assertTrue(result.state.upgradeLevels.isEmpty())
        assertTrue(result.state.prestigeUpgradeLevels.isEmpty())
        assertEquals(initial.ultraUpgradeLevels, result.state.ultraUpgradeLevels)
        assertEquals(initial.shopPurchases, result.state.shopPurchases)
        assertEquals(1L, result.state.ultraCount)
        assertEquals(1L, result.state.statistics.ultrasCount)
    }

    @Test
    fun ultraResetWithoutPrestigeDoesNotMutateState() {
        val initial = GameState(money = BigNumber("1000000000000"), prestige = BigNumber(999))
        val result = DesktopEconomyTransactions.ultraReset(initial, GameEngine())

        assertFalse(result.accepted)
        assertEquals(initial, result.state)
    }

    @Test
    fun shopPurchaseIsAtomicAndCannotBeClaimedTwice() {
        val initial = GameState(nebula = BigNumber(5))
        val item = com.jarrlyyy.guessthenumber.domain.model.ShopItemDef(
            id = "cosmetic_blue",
            name = "Blue",
            description = "A cosmetic.",
            nebulaCost = 3,
            isCosmetic = true
        )
        val bought = DesktopEconomyTransactions.buyShopItem(initial, item)

        assertTrue(bought.accepted)
        assertEquals(BigNumber(2), bought.state.nebula)
        assertTrue("cosmetic_blue" in bought.state.shopPurchases)
        assertEquals("cosmetic_blue", bought.state.equippedCosmeticId)

        val duplicate = DesktopEconomyTransactions.buyShopItem(bought.state, item)
        assertFalse(duplicate.accepted)
        assertEquals(bought.state, duplicate.state)
    }

    @Test
    fun rewardTalentAddsMoneyAndStatisticsAfterCorrectGuess() {
        val before = GameState(
            money = BigNumber(100),
            prestigeShopPurchases = setOf("talent_reward_1"),
            statistics = com.jarrlyyy.guessthenumber.domain.model.GameStatistics(
                moneyEarned = BigNumber(100)
            )
        )
        val after = before.copy(
            money = BigNumber(200),
            statistics = before.statistics.copy(moneyEarned = BigNumber(200))
        )

        val result = DesktopEconomyTransactions.applyGuessBonuses(before, after, correct = true)

        assertEquals(BigNumber(250), result.money)
        assertEquals(BigNumber(250), result.statistics.moneyEarned)
    }

    @Test
    fun incorrectGuessDoesNotApplyPostGuessBonuses() {
        val before = GameState(prestigeShopPurchases = setOf("talent_reward_1"))
        val after = before.copy(money = BigNumber(10))

        assertEquals(after, DesktopEconomyTransactions.applyGuessBonuses(before, after, correct = false))
    }

    @Test
    fun autoClickerCanOnlyToggleAfterPurchase() {
        val locked = GameState()
        assertFalse(DesktopEconomyTransactions.setAutoClickerActive(locked, true).accepted)

        val owned = locked.copy(shopPurchases = setOf("auto_clicker_basic"))
        val resumed = DesktopEconomyTransactions.setAutoClickerActive(owned, true)
        assertTrue(resumed.accepted)
        assertTrue(resumed.state.autoClickerActive)

        val paused = DesktopEconomyTransactions.setAutoClickerActive(resumed.state, false)
        assertTrue(paused.accepted)
        assertFalse(paused.state.autoClickerActive)
    }

    @Test
    fun shopEffectWithoutDesktopImplementationIsRejected() {
        val initial = GameState(nebula = BigNumber(100))
        val item = com.jarrlyyy.guessthenumber.domain.model.ShopItemDef(
            id = "auto_clicker_basic",
            name = "Auto Clicker",
            description = "Background automation.",
            nebulaCost = 25,
            category = "automation",
            isCosmetic = false
        )

        val result = DesktopEconomyTransactions.buyShopItem(initial, item)

        assertFalse(result.accepted)
        assertEquals(initial, result.state)
    }

    @Test
    fun talentRequiresParentAndCannotBePurchasedTwice() {
        val locked = GameState(prestige = BigNumber(100))
        val rejected = DesktopEconomyTransactions.buyTalent(locked, "talent_reward_1", 10)
        assertFalse(rejected.accepted)
        assertEquals(locked, rejected.state)

        val ready = locked.copy(prestigeShopPurchases = setOf("talent_crit_2"))
        val bought = DesktopEconomyTransactions.buyTalent(ready, "talent_reward_1", 10)
        assertTrue(bought.accepted)
        assertEquals(BigNumber(90), bought.state.prestige)
        assertTrue("talent_reward_1" in bought.state.prestigeShopPurchases)
    }
}
