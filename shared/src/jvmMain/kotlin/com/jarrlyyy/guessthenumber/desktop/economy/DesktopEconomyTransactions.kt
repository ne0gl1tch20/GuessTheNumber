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
        // Rejected purchases must remain atomic: even a stale daily-quest date
        // must not mutate state when the upgrade is unaffordable or maxed out.
        val level = initialState.upgradeLevels[upgrade.id] ?: 0
        if (level >= upgrade.maxLevel) return rejected(initialState, "Already at maximum level.")
        val cost = upgradeCost(upgrade, level)
        if (initialState.money < cost) return rejected(initialState, "Not enough Money.")

        val state = refreshDailyQuestDay(initialState, LocalDate.now().toString())
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
        val supported = item.id in setOf(
            "auto_clicker_basic", "nebula_boost", "golden_guess"
        ) || (item.isCosmetic && item.id.startsWith("cosmetic_"))
        if (!supported) return rejected(state, "This shop effect is not supported on Windows yet.")
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

    /** Unlocks a world only when its Android-defined progression requirements are met. */
    fun unlockWorld(state: GameState, worldId: String): EconomyTransaction {
        val world = WORLD_REQUIREMENTS.firstOrNull { it.id == worldId }
            ?: return rejected(state, "Unknown world.")
        if (worldId in state.unlockedWorldIds) return rejected(state, "World is already unlocked.")
        val totalUpgradeLevels = state.upgradeLevels.values.sum()
        val eligible = world.requiredBossId?.let { it in state.defeatedBossIds } ?: true
        val meetsRequirements = eligible &&
            state.correctGuesses >= world.requiredCorrectGuesses &&
            totalUpgradeLevels >= world.requiredUpgradeLevels &&
            state.prestigeCount >= world.requiredPrestigeCount &&
            state.ultraCount >= world.requiredUltraCount
        if (!meetsRequirements) return rejected(state, "World requirements are not met.")
        return accepted(
            state.copy(
                unlockedWorldIds = state.unlockedWorldIds + worldId,
                activeWorldId = worldId
            ),
            "Unlocked ${world.name}."
        )
    }

    fun selectWorld(state: GameState, worldId: String): EconomyTransaction {
        if (WORLD_REQUIREMENTS.none { it.id == worldId }) return rejected(state, "Unknown world.")
        if (worldId !in state.unlockedWorldIds) return rejected(state, "Unlock this world first.")
        if (state.endlessRiftActive && worldId != state.activeWorldId) {
            return rejected(state, "World selection is locked during Endless Rift.")
        }
        return accepted(state.copy(activeWorldId = worldId), "Selected ${WORLD_REQUIREMENTS.first { it.id == worldId }.name}.")
    }

    fun worldProgress(state: GameState): List<DesktopWorldProgress> {
        val totalUpgradeLevels = state.upgradeLevels.values.sum()
        return WORLD_REQUIREMENTS.map { world ->
            val bossReady = world.requiredBossId?.let { it in state.defeatedBossIds } ?: true
            DesktopWorldProgress(
                id = world.id,
                name = world.name,
                unlocked = world.id in state.unlockedWorldIds,
                selected = world.id == state.activeWorldId,
                canUnlock = world.id !in state.unlockedWorldIds && bossReady &&
                    state.correctGuesses >= world.requiredCorrectGuesses &&
                    totalUpgradeLevels >= world.requiredUpgradeLevels &&
                    state.prestigeCount >= world.requiredPrestigeCount &&
                    state.ultraCount >= world.requiredUltraCount,
                requirement = world.requirement
            )
        }
    }

    /** Applies Android's achievement milestones after any persisted gameplay/economy transition. */
    fun evaluateAchievements(state: GameState): GameState {
        val milestones = listOf(
            "correct_10" to (state.correctGuesses >= 10),
            "correct_50" to (state.correctGuesses >= 50),
            "correct_100" to (state.correctGuesses >= 100),
            "correct_500" to (state.correctGuesses >= 500),
            "streak_5" to (state.bestStreak >= 5),
            "streak_10" to (state.bestStreak >= 10),
            "streak_25" to (state.bestStreak >= 25),
            "streak_50" to (state.bestStreak >= 50),
            "millionaire" to (state.money >= BigNumber(1_000_000)),
            "ten_millionaire" to (state.money >= BigNumber(10_000_000)),
            "hundred_millionaire" to (state.money >= BigNumber(100_000_000)),
            "billionaire" to (state.money >= BigNumber(1_000_000_000)),
            "prestige_1" to (state.prestigeCount >= 1),
            "prestige_5" to (state.prestigeCount >= 5),
            "prestige_10" to (state.prestigeCount >= 10),
            "prestige_25" to (state.prestigeCount >= 25),
            "ultra_1" to (state.ultraCount >= 1),
            "ultra_3" to (state.ultraCount >= 3),
            "ultra_10" to (state.ultraCount >= 10),
            "ultra_25" to (state.ultraCount >= 25),
            "playtime_1h" to (state.statistics.playtimeSeconds >= 3_600),
            "playtime_10h" to (state.statistics.playtimeSeconds >= 36_000),
            "playtime_50h" to (state.statistics.playtimeSeconds >= 180_000),
            "frenzy_5" to (state.bestStreak >= 5),
            "frenzy_10" to (state.bestStreak >= 10),
            "frenzy_25" to (state.bestStreak >= 25),
            "frenzy_50" to (state.bestStreak >= 50)
        )
        val rewards = mapOf(
            "correct_10" to 10L, "correct_50" to 20L, "correct_100" to 40L, "correct_500" to 100L,
            "streak_5" to 25L, "streak_10" to 50L, "streak_25" to 100L, "streak_50" to 250L,
            "millionaire" to 50L, "ten_millionaire" to 100L, "hundred_millionaire" to 200L, "billionaire" to 500L,
            "prestige_1" to 25L, "prestige_5" to 75L, "prestige_10" to 150L, "prestige_25" to 300L,
            "ultra_1" to 100L, "ultra_3" to 250L, "ultra_10" to 500L, "ultra_25" to 1_000L,
            "playtime_1h" to 25L, "playtime_10h" to 100L, "playtime_50h" to 300L,
            "frenzy_5" to 30L, "frenzy_10" to 75L, "frenzy_25" to 150L, "frenzy_50" to 350L
        )
        val newlyUnlocked = milestones.filter { (id, unlocked) -> unlocked && id !in state.achievements }
        if (newlyUnlocked.isEmpty()) return state
        val baseReward = newlyUnlocked.sumOf { (id, _) -> rewards[id] ?: 0L }
        val reward = if ("nebula_boost" in state.shopPurchases) baseReward * 2L else baseReward
        return state.copy(
            achievements = state.achievements + newlyUnlocked.map { it.first },
            nebula = state.nebula + BigNumber(reward),
            statistics = state.statistics.copy(nebulaEarned = state.statistics.nebulaEarned + reward)
        )
    }

    private data class WorldRequirement(
        val id: String,
        val name: String,
        val requiredBossId: String?,
        val requiredCorrectGuesses: Long,
        val requiredUpgradeLevels: Int,
        val requiredPrestigeCount: Long,
        val requiredUltraCount: Long,
        val requirement: String
    )

    data class DesktopWorldProgress(
        val id: String,
        val name: String,
        val unlocked: Boolean,
        val selected: Boolean,
        val canUnlock: Boolean,
        val requirement: String
    )

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
        if (!correct || lootRoll == null || lootRoll !in 0..999) return progressed

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

    fun claimDailyQuest(state: GameState, questId: String, today: String): EconomyTransaction {
        val daily = refreshDailyQuestDay(state, today)
        val ready = when (questId) {
            "daily_guesses_10" -> daily.dailyQuestGuesses >= 10
            "daily_correct_5" -> daily.dailyQuestCorrectGuesses >= 5
            "daily_streak_5" -> daily.dailyQuestBestStreak >= 5
            "daily_upgrades_3" -> daily.dailyQuestUpgradePurchases >= 3
            else -> false
        }
        if (!ready || questId in daily.claimedDailyQuests) {
            return rejected(daily, "Daily quest is not ready or was already claimed.")
        }

        val nebulaMultiplier = if ("nebula_boost" in daily.shopPurchases) 2L else 1L
        val rewarded = when (questId) {
            "daily_guesses_10" -> daily.copy(
                money = daily.money + BigNumber(10_000),
                statistics = daily.statistics.copy(moneyEarned = daily.statistics.moneyEarned + BigNumber(10_000))
            )
            "daily_correct_5" -> daily.copy(
                nebula = daily.nebula + BigNumber(25L * nebulaMultiplier),
                statistics = daily.statistics.copy(nebulaEarned = daily.statistics.nebulaEarned + 25L * nebulaMultiplier)
            )
            "daily_streak_5" -> daily.copy(
                nebula = daily.nebula + BigNumber(50L * nebulaMultiplier),
                statistics = daily.statistics.copy(nebulaEarned = daily.statistics.nebulaEarned + 50L * nebulaMultiplier)
            )
            else -> daily.copy(
                nebula = daily.nebula + BigNumber(35L * nebulaMultiplier),
                statistics = daily.statistics.copy(nebulaEarned = daily.statistics.nebulaEarned + 35L * nebulaMultiplier)
            )
        }
        return accepted(
            rewarded.copy(claimedDailyQuests = rewarded.claimedDailyQuests + questId),
            "Daily quest claimed."
        )
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
        // Golden Guess Pass guarantees a critical-sized bonus on every tenth lifetime guess.
        if ("golden_guess" in before.shopPurchases && after.statistics.totalGuesses > 0L &&
            after.statistics.totalGuesses % 10L == 0L
        ) bonusMoney += eligibleEarnings
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
        val finalBonusNebula = if ("nebula_boost" in before.shopPurchases) bonusNebula * 2L else bonusNebula
        return after.copy(
            money = after.money + bonusMoney,
            nebula = after.nebula + BigNumber(finalBonusNebula),
            statistics = after.statistics.copy(
                moneyEarned = after.statistics.moneyEarned + bonusMoney,
                nebulaEarned = after.statistics.nebulaEarned + finalBonusNebula
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

    private val WORLD_REQUIREMENTS = listOf(
        WorldRequirement("verdant_grove", "Verdant Grove", null, 0L, 0, 0L, 0L, "Starting world"),
        WorldRequirement("crystal_caverns", "Crystal Caverns", "verdant_guardian", 25L, 5, 0L, 0L, "Defeat Verdant Guardian · 25 correct guesses · 5 upgrade levels"),
        WorldRequirement("ember_summit", "Ember Summit", "crystal_golem", 100L, 15, 1L, 0L, "Defeat Crystal Golem · 100 correct guesses · 15 upgrade levels · 1 Prestige"),
        WorldRequirement("nebula_rift", "Nebula Rift", "ember_dragon", 250L, 40, 3L, 1L, "Defeat Ember Dragon · 250 correct guesses · 40 upgrade levels · 3 Prestige · 1 Ultra")
    )

    private fun accepted(state: GameState, message: String) =
        EconomyTransaction(state, true, message)

    private fun rejected(state: GameState, message: String) =
        EconomyTransaction(state, false, message)
}
