package com.jarrlyyy.guessthenumber.desktop.economy

import com.jarrlyyy.guessthenumber.domain.engine.GameEngine
import com.jarrlyyy.guessthenumber.domain.model.BigNumber
import com.jarrlyyy.guessthenumber.domain.model.GameState
import com.jarrlyyy.guessthenumber.domain.model.ShopItemDef
import com.jarrlyyy.guessthenumber.domain.model.UpgradeDef

/**
 * Deterministic, Android-compatible economy mutations for the Windows host.
 * UI and persistence stay outside this class so failed saves never publish a purchase.
 */
object DesktopEconomyRules {
    fun nextUpgradeCost(upgrade: UpgradeDef, currentLevel: Int): BigNumber =
        BigNumber(upgrade.baseCost) * BigNumber(upgrade.costMultiplier).pow(currentLevel)

    fun buyMoneyUpgrade(state: GameState, upgrade: UpgradeDef): GameState? {
        val level = state.upgradeLevels[upgrade.id] ?: 0
        if (level >= upgrade.maxLevel) return null
        val cost = nextUpgradeCost(upgrade, level)
        if (state.money < cost) return null
        return state.copy(
            money = state.money - cost,
            upgradeLevels = state.upgradeLevels + (upgrade.id to (level + 1)),
            statistics = state.statistics.copy(moneySpent = state.statistics.moneySpent + cost),
            dailyQuestUpgradePurchases = state.dailyQuestUpgradePurchases + 1
        )
    }

    fun buyPrestigeUpgrade(state: GameState, upgrade: UpgradeDef): GameState? {
        val level = state.prestigeUpgradeLevels[upgrade.id] ?: 0
        if (level >= upgrade.maxLevel) return null
        val cost = nextUpgradeCost(upgrade, level)
        if (state.prestige < cost) return null
        return state.copy(
            prestige = state.prestige - cost,
            prestigeUpgradeLevels = state.prestigeUpgradeLevels + (upgrade.id to (level + 1))
        )
    }

    fun buyUltraUpgrade(state: GameState, upgrade: UpgradeDef): GameState? {
        val level = state.ultraUpgradeLevels[upgrade.id] ?: 0
        if (level >= upgrade.maxLevel) return null
        val cost = nextUpgradeCost(upgrade, level)
        if (state.ultra < cost) return null
        return state.copy(
            ultra = state.ultra - cost,
            ultraUpgradeLevels = state.ultraUpgradeLevels + (upgrade.id to (level + 1))
        )
    }

    fun buyNebulaShopItem(state: GameState, item: ShopItemDef): GameState? {
        val cost = BigNumber(item.nebulaCost)
        if (item.id in state.shopPurchases || state.nebula < cost) return null
        return state.copy(
            nebula = state.nebula - cost,
            shopPurchases = state.shopPurchases + item.id,
            autoClickerActive = if (item.id == "auto_clicker_basic") true else state.autoClickerActive,
            equippedCosmeticId = if (item.id.startsWith("cosmetic_")) item.id else state.equippedCosmeticId
        )
    }

    fun buyPrestigeShopItem(state: GameState, item: DesktopCurrencyShopItem): GameState? {
        val cost = BigNumber(item.cost)
        if (item.id in state.prestigeShopPurchases || state.prestige < cost) return null
        return state.copy(
            prestige = state.prestige - cost,
            prestigeShopPurchases = state.prestigeShopPurchases + item.id
        )
    }

    fun buyUltraShopItem(state: GameState, item: DesktopCurrencyShopItem): GameState? {
        val cost = BigNumber(item.cost)
        if (item.id in state.ultraShopPurchases || state.ultra < cost) return null
        return state.copy(
            ultra = state.ultra - cost,
            ultraShopPurchases = state.ultraShopPurchases + item.id
        )
    }

    fun buyTalent(state: GameState, talent: DesktopTalentDef): GameState? {
        val parents = mapOf(
            "talent_speed_2" to "talent_speed_1",
            "talent_crit_2" to "talent_crit_1",
            "talent_reward_1" to "talent_crit_2",
            "talent_master_1" to "talent_reward_1"
        )
        if (talent.id in state.prestigeShopPurchases || state.prestige < BigNumber(talent.cost)) return null
        val requiredParent = talent.requiredParentId ?: parents[talent.id]
        if (requiredParent != null && requiredParent !in state.prestigeShopPurchases) return null
        return state.copy(
            prestige = state.prestige - BigNumber(talent.cost),
            prestigeShopPurchases = state.prestigeShopPurchases + talent.id
        )
    }

    fun prestigeReset(state: GameState, engine: GameEngine): GameState? {
        val requirement = engine.calculatePrestigeRequirement(state.prestigeCount)
        val reward = engine.calculatePrestigeReward(state)
        if (state.money < requirement || reward <= BigNumber.ZERO) return null
        return state.copy(
            money = engine.prestigeStartingCapital(state),
            prestige = state.prestige + reward,
            upgradeLevels = emptyMap(),
            currentRangeMin = 1,
            currentRangeMax = com.jarrlyyy.guessthenumber.domain.model.Difficulty.rangeMax(state.difficultyId, 0),
            statistics = state.statistics.copy(prestigesCount = state.statistics.prestigesCount + 1),
            prestigeCount = state.prestigeCount + 1
        )
    }

    fun ultraReset(state: GameState, engine: GameEngine): GameState? {
        val requirement = engine.calculateUltraRequirement(state.ultraCount)
        val reward = engine.calculateUltraReward(state)
        if (state.money < requirement || state.prestige < BigNumber(1_000) || reward <= BigNumber.ZERO) return null
        return state.copy(
            money = engine.prestigeStartingCapital(state),
            prestige = BigNumber.ZERO,
            ultra = state.ultra + reward,
            upgradeLevels = emptyMap(),
            prestigeUpgradeLevels = emptyMap(),
            statistics = state.statistics.copy(ultrasCount = state.statistics.ultrasCount + 1),
            ultraCount = state.ultraCount + 1
        )
    }
}
