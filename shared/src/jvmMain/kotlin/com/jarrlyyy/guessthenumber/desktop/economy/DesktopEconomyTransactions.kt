package com.jarrlyyy.guessthenumber.desktop.economy

import com.jarrlyyy.guessthenumber.domain.engine.GameEngine
import com.jarrlyyy.guessthenumber.domain.model.BigNumber
import com.jarrlyyy.guessthenumber.domain.model.Difficulty
import com.jarrlyyy.guessthenumber.domain.model.GameState
import com.jarrlyyy.guessthenumber.domain.model.ShopItemDef
import com.jarrlyyy.guessthenumber.domain.model.UpgradeDef
import java.time.LocalDate
import kotlin.random.Random

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

    fun buyUpgrade(initialState: GameState, upgrade: UpgradeDef): EconomyTransaction {
        val state = refreshDailyQuestDay(initialState, LocalDate.now().toString())
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
        if (!item.isCosmetic && item.id != "auto_clicker_basic") return rejected(state, "This shop effect is not wired for Windows yet.")
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

    fun setAutoClickerActive(state: GameState, active: Boolean): EconomyTransaction {
        if ("auto_clicker_basic" !in state.shopPurchases) {
            return rejected(state, "Purchase the Auto Clicker first.")
        }
        return accepted(
            state.copy(autoClickerActive = active),
            if (active) "Auto Clicker resumed." else "Auto Clicker paused."
        )
    }

    /**
     * Mirrors the Android daily-quest progress and correct-guess treasure-drop
     * bookkeeping. A caller supplies the roll so the complete reward path can
     * be tested deterministically without relying on wall-clock time or RNG.
     */
    fun recordGuessProgress(
        before: GameState,
        after: GameState,
        correct: Boolean,
        today: String,
        lootRoll: Int? = null
    ): GameState {
        val daily = refreshDailyQuestDay(before, today)
        val progressed = after.copy(
            dailyQuestDate = today,
            dailyQuestGuesses = daily.dailyQuestGuesses + 1,
            dailyQuestCorrectGuesses = daily.dailyQuestCorrectGuesses + if (correct) 1 else 0,
            dailyQuestBestStreak = maxOf(daily.dailyQuestBestStreak, after.streak),
            dailyQuestUpgradePurchases = daily.dailyQuestUpgradePurchases,
            claimedDailyQuests = daily.claimedDailyQuests
        )
        if (!correct || lootRoll !in 0..999) return progressed

        return when (lootRoll) {
            in 0..9 -> progressed.copy(
                lootEpicChests = progressed.lootEpicChests + 1,
                lootDropsFound = progressed.lootDropsFound + 1,
                lastLootDropTier = "epic",
                lootEventId = progressed.lootEventId + 1
            )
            in 10..59 -> progressed.copy(
                lootRareChests = progressed.lootRareChests + 1,
                lootDropsFound = progressed.lootDropsFound + 1,
                lastLootDropTier = "rare",
                lootEventId = progressed.lootEventId + 1
            )
            in 60..239 -> progressed.copy(
                lootCommonChests = progressed.lootCommonChests + 1,
                lootDropsFound = progressed.lootDropsFound + 1,
                lastLootDropTier = "common",
                lootEventId = progressed.lootEventId + 1
            )
            else -> progressed
        }
    }

    private fun refreshDailyQuestDay(state: GameState, today: String): GameState {
        if (state.dailyQuestDate == today) return state
        return state.copy(
            dailyQuestDate = today,
            dailyQuestGuesses = 0,
            dailyQuestCorrectGuesses = 0,
            dailyQuestBestStreak = 0,
            dailyQuestUpgradePurchases = 0,
            claimedDailyQuests = emptySet()
        )
    }

    /** Mirrors the Android post-guess talent/event/mastery/relic bonus layer. */
    fun applyGuessBonuses(
        before: GameState,
        after: GameState,
        correct: Boolean,
        rng: Random = Random.Default
    ): GameState {
        if (!correct) return after
        val earned = after.money - before.money
        val eligibleEarnings = if (earned > BigNumber.ZERO) earned else BigNumber.ZERO
        var bonusMoney = BigNumber.ZERO
        if ("talent_reward_1" in before.prestigeShopPurchases) bonusMoney += eligibleEarnings * BigNumber(0.5)
        val eventBoostLevel = before.permanentEventBoosts.coerceIn(0, 25)
        if (eventBoostLevel > 0) bonusMoney += eligibleEarnings * BigNumber(eventBoostLevel * 0.02)
        val masteryLevel = (before.worldMasteryLevels[before.activeWorldId] ?: 0).coerceIn(0, 10)
        if (masteryLevel > 0) bonusMoney += eligibleEarnings * BigNumber(masteryLevel * 0.02)
        val criticalChance = when {
            "talent_crit_2" in before.prestigeShopPurchases -> 0.15
            "talent_crit_1" in before.prestigeShopPurchases -> 0.05
            else -> 0.0
        }
        if (criticalChance > 0.0 && rng.nextDouble() < criticalChance) bonusMoney += eligibleEarnings
        if ("verdant_guardian" in before.equippedRelicIds) bonusMoney += eligibleEarnings * BigNumber(0.10)
        if ("crystal_golem" in before.equippedRelicIds && rng.nextDouble() < 0.10) bonusMoney += eligibleEarnings * BigNumber(0.5)
        if ("ember_dragon" in before.equippedRelicIds) bonusMoney += eligibleEarnings * BigNumber(0.25)
        if ("nebula_titan" in before.equippedRelicIds) bonusMoney += eligibleEarnings * BigNumber(0.25)

        val equippedBossRelics = before.equippedRelicIds.intersect(
            setOf("verdant_guardian", "crystal_golem", "ember_dragon", "nebula_titan")
        )
        val relicSetBonus = when (equippedBossRelics.size) {
            2 -> 0.10
            3 -> 0.20
            4 -> 0.35
            else -> 0.0
        }
        if (relicSetBonus > 0.0) bonusMoney += eligibleEarnings * BigNumber(relicSetBonus)
        if (before.homeBaseLevel > 0) bonusMoney += eligibleEarnings * BigNumber((before.homeBaseLevel * 0.02).coerceAtMost(0.40))
        val secretForActiveWorld = mapOf(
            "verdant_grove" to "whispering_hollow",
            "crystal_caverns" to "shard_archive",
            "ember_summit" to "ashen_vault",
            "nebula_rift" to "lost_observatory"
        )[before.activeWorldId]
        if (secretForActiveWorld != null && secretForActiveWorld in before.discoveredSecretIds) {
            bonusMoney += eligibleEarnings * BigNumber(0.05)
        }
        val bonusNebula = (if ("talent_master_1" in before.prestigeShopPurchases) 1L else 0L) +
            (eventBoostLevel / 5).toLong() +
            (if ("ember_dragon" in before.equippedRelicIds) 1L else 0L) +
            (if (equippedBossRelics.size == 4) 1L else 0L) +
            (if (before.discoveredSecretIds.size >= 4) 1L else 0L) +
            (if (before.activeWorldId == "ember_summit" && "ashen_vault" in before.discoveredSecretIds) 1L else 0L)
        return after.copy(
            money = after.money + bonusMoney,
            nebula = after.nebula + BigNumber(bonusNebula),
            statistics = after.statistics.copy(
                moneyEarned = after.statistics.moneyEarned + bonusMoney,
                nebulaEarned = after.statistics.nebulaEarned + bonusNebula
            )
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
