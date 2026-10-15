package com.jarrlyyy.guessthenumber.desktop.economy

import com.jarrlyyy.guessthenumber.domain.engine.GameEngine
import com.jarrlyyy.guessthenumber.domain.model.BigNumber
import com.jarrlyyy.guessthenumber.domain.model.Difficulty
import com.jarrlyyy.guessthenumber.domain.model.GameState
import com.jarrlyyy.guessthenumber.domain.model.ShopItemDef
import com.jarrlyyy.guessthenumber.domain.model.UpgradeDef

data class EconomyTransaction(
    val state: GameState,
    val accepted: Boolean,
    val message: String
)

/**
 * Immutable economy transactions for the Windows host. Currency math and reset
 * rules deliberately reuse the production BigNumber and GameEngine APIs.
 */
object DesktopEconomyTransactions {
    fun upgradeCost(upgrade: UpgradeDef, currentLevel: Int): BigNumber =
        BigNumber(upgrade.baseCost) * BigNumber(upgrade.costMultiplier).pow(currentLevel)

    fun buyUpgrade(state: GameState, upgrade: UpgradeDef): EconomyTransaction {
        val level = state.upgradeLevels[upgrade.id] ?: 0
        if (level >= upgrade.maxLevel) return rejected(state, "Already at maximum level.")
        val cost = upgradeCost(upgrade, level)
        if (state.money < cost) return rejected(state, "Not enough Money.")
        return accepted(
            state.copy(
                money = state.money - cost,
                upgradeLevels = state.upgradeLevels + (upgrade.id to (level + 1)),
                statistics = state.statistics.copy(moneySpent = state.statistics.moneySpent + cost),
                dailyQuestUpgradePurchases = state.dailyQuestUpgradePurchases + 1
            ),
            "Purchased ${upgrade.name} (level ${level + 1})."
        )
    }

    fun buyPrestigeUpgrade(state: GameState, upgrade: UpgradeDef): EconomyTransaction {
        val level = state.prestigeUpgradeLevels[upgrade.id] ?: 0
        if (level >= upgrade.maxLevel) return rejected(state, "Already at maximum level.")
        val cost = upgradeCost(upgrade, level)
        if (state.prestige < cost) return rejected(state, "Not enough Prestige.")
        return accepted(
            state.copy(
                prestige = state.prestige - cost,
                prestigeUpgradeLevels = state.prestigeUpgradeLevels + (upgrade.id to (level + 1))
            ),
            "Purchased ${upgrade.name} (level ${level + 1})."
        )
    }

    fun buyUltraUpgrade(state: GameState, upgrade: UpgradeDef): EconomyTransaction {
        val level = state.ultraUpgradeLevels[upgrade.id] ?: 0
        if (level >= upgrade.maxLevel) return rejected(state, "Already at maximum level.")
        val cost = upgradeCost(upgrade, level)
        if (state.ultra < cost) return rejected(state, "Not enough Ultra.")
        return accepted(
            state.copy(
                ultra = state.ultra - cost,
                ultraUpgradeLevels = state.ultraUpgradeLevels + (upgrade.id to (level + 1))
            ),
            "Purchased ${upgrade.name} (level ${level + 1})."
        )
    }

    fun prestigeReset(state: GameState, engine: GameEngine): EconomyTransaction {
        val reward = engine.calculatePrestigeReward(state.money, state.prestigeCount)
        val requirement = engine.calculatePrestigeRequirement(state.prestigeCount)
        if (state.money < requirement || reward <= BigNumber.ZERO) {
            return rejected(state, "Prestige requirement not met.")
        }
        val next = state.copy(
            money = BigNumber.ZERO,
            prestige = state.prestige + reward,
            upgradeLevels = emptyMap(),
            currentRangeMin = 1,
            currentRangeMax = Difficulty.rangeMax(state.difficultyId, 0),
            statistics = state.statistics.copy(prestigesCount = state.statistics.prestigesCount + 1),
            prestigeCount = state.prestigeCount + 1
        )
        return accepted(next, "Prestige reset complete. Earned $reward Prestige.")
    }

    fun ultraReset(state: GameState, engine: GameEngine): EconomyTransaction {
        val reward = engine.calculateUltraReward(state.money, state.ultraCount)
        val requirement = engine.calculateUltraRequirement(state.ultraCount)
        if (state.money < requirement || state.prestige < BigNumber(1_000) || reward <= BigNumber.ZERO) {
            return rejected(state, "Ultra requires enough Money and at least 1,000 Prestige.")
        }
        val next = state.copy(
            money = BigNumber.ZERO,
            prestige = BigNumber.ZERO,
            ultra = state.ultra + reward,
            upgradeLevels = emptyMap(),
            prestigeUpgradeLevels = emptyMap(),
            statistics = state.statistics.copy(ultrasCount = state.statistics.ultrasCount + 1),
            ultraCount = state.ultraCount + 1
        )
        return accepted(next, "Ultra reset complete. Earned $reward Ultra.")
    }

    fun buyShopItem(state: GameState, item: ShopItemDef): EconomyTransaction {
        if (item.id in state.shopPurchases) return rejected(state, "Already purchased.")
        val cost = BigNumber(item.nebulaCost)
        if (state.nebula < cost) return rejected(state, "Not enough Nebula.")
        return accepted(
            state.copy(
                nebula = state.nebula - cost,
                shopPurchases = state.shopPurchases + item.id,
                autoClickerActive = if (item.id == "auto_clicker_basic") true else state.autoClickerActive,
                equippedCosmeticId = if (item.id.startsWith("cosmetic_")) item.id else state.equippedCosmeticId
            ),
            "Purchased ${item.name}."
        )
    }

    fun buyTalent(state: GameState, talentId: String, cost: Long): EconomyTransaction {
        val parents = mapOf(
            "talent_speed_2" to "talent_speed_1",
            "talent_crit_2" to "talent_crit_1",
            "talent_reward_1" to "talent_crit_2",
            "talent_master_1" to "talent_reward_1"
        )
        if (talentId in state.prestigeShopPurchases) return rejected(state, "Already unlocked.")
        if (state.prestige < BigNumber(cost)) return rejected(state, "Not enough Prestige.")
        val parent = parents[talentId]
        if (parent != null && parent !in state.prestigeShopPurchases) {
            return rejected(state, "Unlock the prerequisite talent first.")
        }
        return accepted(
            state.copy(
                prestige = state.prestige - BigNumber(cost),
                prestigeShopPurchases = state.prestigeShopPurchases + talentId
            ),
            "Talent unlocked."
        )
    }

    private fun accepted(state: GameState, message: String) =
        EconomyTransaction(state, true, message)

    private fun rejected(state: GameState, message: String) =
        EconomyTransaction(state, false, message)
}
