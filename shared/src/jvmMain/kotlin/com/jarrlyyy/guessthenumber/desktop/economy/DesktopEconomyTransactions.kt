package com.jarrlyyy.guessthenumber.desktop.economy

import com.jarrlyyy.guessthenumber.domain.engine.GameEngine
import com.jarrlyyy.guessthenumber.domain.model.BigNumber
import com.jarrlyyy.guessthenumber.domain.model.Difficulty
import com.jarrlyyy.guessthenumber.domain.model.GameState
import com.jarrlyyy.guessthenumber.domain.model.ShopItemDef
import com.jarrlyyy.guessthenumber.domain.model.UpgradeDef
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
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

    private data class UpgradeBatch(val count: Int, val cost: BigNumber)

    private fun upgradeBatch(
        upgrade: UpgradeDef,
        currentLevel: Int,
        multiplier: String,
        currency: BigNumber
    ): UpgradeBatch {
        if (currentLevel >= upgrade.maxLevel) return UpgradeBatch(0, BigNumber.ZERO)
        val requested = when (multiplier) {
            "10" -> 10
            "100" -> 100
            "MAX" -> upgrade.maxLevel - currentLevel
            else -> 1
        }.coerceAtMost(upgrade.maxLevel - currentLevel)
        var count = 0
        var total = BigNumber.ZERO
        while (count < requested) {
            val nextCost = upgradeCost(upgrade, currentLevel + count)
            // Android buys the largest affordable prefix for 10, 100 and MAX.
            // Keep the UI estimate and transaction behavior identical.
            if (currency < total + nextCost) break
            total += nextCost
            count++
        }
        return UpgradeBatch(count, total)
    }

    /** Cost shown by the Windows UI; MAX means the largest currently affordable batch. */
    fun upgradeBatchCost(
        upgrade: UpgradeDef,
        currentLevel: Int,
        multiplier: String,
        currency: BigNumber
    ): BigNumber = upgradeBatch(upgrade, currentLevel, multiplier, currency).cost

    fun setBuyMultiplier(state: GameState, multiplier: String): EconomyTransaction {
        if (multiplier !in setOf("1", "10", "100", "MAX")) {
            return rejected(state, "Unsupported buy multiplier.")
        }
        if (state.buyMultiplier == multiplier) return rejected(state, "Buy multiplier is already selected.")
        return accepted(state.copy(buyMultiplier = multiplier), "Buy multiplier set to $multiplier.")
    }

    fun buyUpgrade(initialState: GameState, upgrade: UpgradeDef): EconomyTransaction {
        // Rejected purchases remain atomic, including when a batch is unaffordable.
        val level = initialState.upgradeLevels[upgrade.id] ?: 0
        if (level >= upgrade.maxLevel) return rejected(initialState, "Already at maximum level.")
        val batch = upgradeBatch(upgrade, level, initialState.buyMultiplier, initialState.money)
        if (batch.count == 0) return rejected(initialState, "Not enough Money.")
        if (initialState.money < batch.cost) return rejected(initialState, "Not enough Money.")

        val newLevel = level + batch.count
        val state = refreshDailyQuestDay(initialState, LocalDate.now().toString())
        return accepted(
            state.copy(
                money = state.money - batch.cost,
                upgradeLevels = state.upgradeLevels + (upgrade.id to newLevel),
                statistics = state.statistics.copy(moneySpent = state.statistics.moneySpent + batch.cost),
                dailyQuestUpgradePurchases = state.dailyQuestUpgradePurchases + 1
            ),
            "Purchased ${upgrade.name} (${batch.count} level(s), now level $newLevel)."
        )
    }

    fun buyPrestigeUpgrade(state: GameState, upgrade: UpgradeDef): EconomyTransaction {
        val level = state.prestigeUpgradeLevels[upgrade.id] ?: 0
        if (level >= upgrade.maxLevel) return rejected(state, "Already at maximum level.")
        val batch = upgradeBatch(upgrade, level, state.buyMultiplier, state.prestige)
        if (batch.count == 0 || state.prestige < batch.cost) return rejected(state, "Not enough Prestige for that batch.")
        val newLevel = level + batch.count
        return accepted(
            state.copy(
                prestige = state.prestige - batch.cost,
                prestigeUpgradeLevels = state.prestigeUpgradeLevels + (upgrade.id to newLevel)
            ),
            "Purchased ${upgrade.name} (${batch.count} level(s), now level $newLevel)."
        )
    }

    fun buyUltraUpgrade(state: GameState, upgrade: UpgradeDef): EconomyTransaction {
        val level = state.ultraUpgradeLevels[upgrade.id] ?: 0
        if (level >= upgrade.maxLevel) return rejected(state, "Already at maximum level.")
        val batch = upgradeBatch(upgrade, level, state.buyMultiplier, state.ultra)
        if (batch.count == 0 || state.ultra < batch.cost) return rejected(state, "Not enough Ultra for that batch.")
        val newLevel = level + batch.count
        return accepted(
            state.copy(
                ultra = state.ultra - batch.cost,
                ultraUpgradeLevels = state.ultraUpgradeLevels + (upgrade.id to newLevel)
            ),
            "Purchased ${upgrade.name} (${batch.count} level(s), now level $newLevel)."
        )
    }

    fun prestigeReset(state: GameState, engine: GameEngine): EconomyTransaction {
        val reward = engine.calculatePrestigeReward(state)
        val requirement = engine.calculatePrestigeRequirement(state.prestigeCount)
        if (state.money < requirement || reward <= BigNumber.ZERO) {
            return rejected(state, "Prestige requirement not met.")
        }
        val next = state.copy(
            money = engine.prestigeStartingCapital(state),
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
        val reward = engine.calculateUltraReward(state)
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

    /** Equips an already-owned cosmetic, matching Android's Shop action. */
    fun equipCosmetic(state: GameState, itemId: String): EconomyTransaction {
        if (!itemId.startsWith("cosmetic_") || itemId !in state.shopPurchases) {
            return rejected(state, "Purchase this cosmetic before equipping it.")
        }
        if (state.equippedCosmeticId == itemId) {
            return rejected(state, "This cosmetic is already equipped.")
        }
        return accepted(state.copy(equippedCosmeticId = itemId), "Equipped ${itemId.removePrefix("cosmetic_").replace('_', ' ')}.")
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
        return accepted(state.copy(activeWorldId = worldId, activeBossBattleWorldId = if (state.activeBossBattleWorldId == worldId) worldId else null), "Selected ${WORLD_REQUIREMENTS.first { it.id == worldId }.name}.")
    }

    /** Applies Android's guess-driven boss damage, miss regression and victory rewards. */
    private val CODEX_REQUIRED_ENTRIES = setOf(
        "world:verdant_grove", "boss:verdant_guardian",
        "world:crystal_caverns", "boss:crystal_golem",
        "world:ember_summit", "boss:ember_dragon",
        "world:nebula_rift", "boss:nebula_titan",
        "secret:whispering_hollow", "secret:shard_archive",
        "secret:ashen_vault", "secret:lost_observatory"
    )

    fun settleStake(
        state: GameState,
        stakeAmount: BigNumber,
        payout: BigNumber,
        won: Boolean
    ): EconomyTransaction {
        if (stakeAmount <= BigNumber.ZERO) return rejected(state, "Enter a positive stake.")
        if (payout < BigNumber.ZERO) return rejected(state, "Payout cannot be negative.")
        if (state.money < stakeAmount) return rejected(state, "Insufficient funds for this stake.")
        val updatedMoney = if (won) state.money - stakeAmount + payout else state.money - stakeAmount
        return accepted(
            state.copy(
                money = updatedMoney,
                statistics = state.statistics.copy(
                    moneyEarned = if (won) state.statistics.moneyEarned + payout else state.statistics.moneyEarned,
                    moneySpent = state.statistics.moneySpent + stakeAmount
                )
            ),
            if (won) "Lucky Guess won!" else "Lucky Guess lost."
        )
    }

    fun claimCodexCompletionReward(state: GameState): EconomyTransaction {
        if (state.codexRewardClaimed) return rejected(state, "Codex completion reward is already claimed.")
        if (!state.codexEntries.containsAll(CODEX_REQUIRED_ENTRIES)) {
            return rejected(state, "Discover all 12 Codex entries first.")
        }
        val reward = BigNumber(100)
        return accepted(
            state.copy(
                nebula = state.nebula + reward,
                codexRewardClaimed = true,
                statistics = state.statistics.copy(nebulaEarned = state.statistics.nebulaEarned + 100L)
            ),
            "Codex complete! You earned 100 Nebula."
        )
    }

    fun claimRelic(state: GameState, relicId: String, unlockBossId: String): EconomyTransaction {
        if (relicId.isBlank() || unlockBossId.isBlank()) return rejected(state, "Invalid relic.")
        if (relicId in state.relicInventory) return rejected(state, "Relic is already claimed.")
        if (unlockBossId !in state.defeatedBossIds) return rejected(state, "Defeat the required boss first.")
        return accepted(state.copy(relicInventory = state.relicInventory + relicId), "Relic claimed.")
    }

    fun equipRelic(state: GameState, relicId: String): EconomyTransaction {
        if (relicId !in state.relicInventory) return rejected(state, "Claim the relic first.")
        if (relicId in state.equippedRelicIds) return rejected(state, "Relic is already equipped.")
        if (state.equippedRelicIds.size >= 4) return rejected(state, "You can equip up to four relics.")
        return accepted(state.copy(equippedRelicIds = state.equippedRelicIds + relicId), "Relic equipped.")
    }

    fun unequipRelic(state: GameState, relicId: String): EconomyTransaction {
        if (relicId !in state.equippedRelicIds) return rejected(state, "Relic is not equipped.")
        return accepted(state.copy(equippedRelicIds = state.equippedRelicIds - relicId), "Relic unequipped.")
    }

    fun recordBossGuess(state: GameState, correct: Boolean): GameState {
        val worldId = state.activeBossBattleWorldId ?: return state
        val world = WORLD_REQUIREMENTS.firstOrNull { it.id == worldId } ?: return state
        if (worldId !in state.unlockedWorldIds || (world.bossId in state.defeatedBossIds && !state.endlessRiftActive)) return state

        val bossHp = world.bossHp + if (state.endlessRiftActive) ((state.endlessRiftTier - 1).coerceAtLeast(0) * 2) else 0
        val damage = (state.bossBattleProgress[world.bossId] ?: 0).coerceIn(0, bossHp)
        val mistakes = state.bossBattleMistakes[world.bossId] ?: 0
        if (!correct) {
            val nextMistakes = mistakes + 1
            val phase = ((damage * 3) / bossHp + 1).coerceIn(1, 3)
            val interval = when (phase) { 1 -> 4; 2 -> 3; else -> 2 }
            val nextDamage = if (nextMistakes % interval == 0) (damage - 1).coerceAtLeast(0) else damage
            return state.copy(
                bossBattleMistakes = state.bossBattleMistakes + (world.bossId to nextMistakes),
                bossBattleProgress = state.bossBattleProgress + (world.bossId to nextDamage)
            )
        }

        val nextDamage = damage + 1
        val oldPhase = ((damage * 3) / bossHp + 1).coerceIn(1, 3)
        val newPhase = ((nextDamage * 3) / bossHp + 1).coerceIn(1, 3)
        if (nextDamage < bossHp) {
            return state.copy(
                bossBattleProgress = state.bossBattleProgress + (world.bossId to nextDamage),
                bossBattleMistakes = state.bossBattleMistakes + (world.bossId to if (newPhase != oldPhase) 0 else mistakes)
            )
        }

        val baseMoney = BigNumber(world.baseMoneyReward)
        val (bonusMoney, bonusNebula) = progressionRewardBonus(state, baseMoney)
        val tierMultiplier = if (state.endlessRiftActive) state.endlessRiftTier.coerceAtLeast(1) else 1
        val moneyReward = engine.applyMoneyEarningBoost(state, (baseMoney + bonusMoney) * BigNumber(tierMultiplier.toLong()))
        val nebulaReward = (world.baseNebulaReward + bonusNebula) * tierMultiplier
        val defeatedState = state.copy(
            money = state.money + moneyReward,
            nebula = state.nebula + BigNumber(nebulaReward),
            defeatedBossIds = if (state.endlessRiftActive) state.defeatedBossIds else state.defeatedBossIds + world.bossId,
            worldBossVictories = state.worldBossVictories + 1,
            bossBattleProgress = state.bossBattleProgress + (world.bossId to bossHp),
            activeBossBattleWorldId = null,
            relicInventory = if (state.endlessRiftActive) state.relicInventory else state.relicInventory + world.bossId,
            codexEntries = if (state.endlessRiftActive) state.codexEntries else state.codexEntries + setOf("world:$worldId", "boss:${world.bossId}"),
            worldMasteryLevels = if (state.endlessRiftActive) state.worldMasteryLevels else state.worldMasteryLevels + (worldId to ((state.worldMasteryLevels[worldId] ?: 0) + 1)),
            statistics = state.statistics.copy(
                moneyEarned = state.statistics.moneyEarned + moneyReward,
                nebulaEarned = state.statistics.nebulaEarned + nebulaReward
            )
        )
        if (!state.endlessRiftActive) return defeatedState

        val worldIndex = WORLD_REQUIREMENTS.indexOf(world).coerceAtLeast(0)
        val nextWorld = WORLD_REQUIREMENTS[(worldIndex + 1) % WORLD_REQUIREMENTS.size]
        val nextTier = state.endlessRiftTier + 1
        return defeatedState.copy(
            activeWorldId = nextWorld.id,
            activeBossBattleWorldId = nextWorld.id,
            bossBattleProgress = defeatedState.bossBattleProgress + (nextWorld.bossId to 0),
            bossBattleMistakes = defeatedState.bossBattleMistakes + (nextWorld.bossId to 0),
            endlessRiftTier = nextTier,
            endlessRiftBestTier = maxOf(state.endlessRiftBestTier, state.endlessRiftTier)
        )
    }

    private fun progressionRewardBonus(state: GameState, baseMoney: BigNumber): Pair<BigNumber, Long> {
        val relicCount = state.equippedRelicIds.intersect(
            setOf("verdant_guardian", "crystal_golem", "ember_dragon", "nebula_titan")
        ).size
        val setRate = when (relicCount) { 2 -> 0.10; 3 -> 0.20; 4 -> 0.35; else -> 0.0 }
        val masteryRate = (state.worldMasteryLevels[state.activeWorldId] ?: 0).coerceIn(0, 10) * 0.02
        val sanctuaryRate = (state.homeBaseLevel.coerceAtLeast(0) * 0.02).coerceAtMost(0.40)
        val eventRate = state.permanentEventBoosts.coerceIn(0, 25) * 0.02
        val secretId = mapOf(
            "verdant_grove" to "whispering_hollow",
            "crystal_caverns" to "shard_archive",
            "ember_summit" to "ashen_vault",
            "nebula_rift" to "lost_observatory"
        )[state.activeWorldId]
        val secretRate = if (secretId != null && secretId in state.discoveredSecretIds) 0.05 else 0.0
        val bonusMoney = baseMoney * BigNumber(setRate + masteryRate + sanctuaryRate + secretRate + eventRate)
        val bonusNebula = (if (relicCount == 4) 10L else 0L) +
            (if ((state.worldMasteryLevels[state.activeWorldId] ?: 0) >= 5) 5L else 0L) +
            (if (secretId != null && secretId in state.discoveredSecretIds) 5L else 0L)
        return bonusMoney to bonusNebula
    }

    /** Starts or leaves the Android-defined boss battle for an unlocked world. */
    fun toggleBossBattle(state: GameState, worldId: String): EconomyTransaction {
        val world = WORLD_REQUIREMENTS.firstOrNull { it.id == worldId }
            ?: return rejected(state, "Unknown world.")
        if (worldId !in state.unlockedWorldIds) return rejected(state, "Unlock this world first.")
        if (world.bossId in state.defeatedBossIds && !state.endlessRiftActive) {
            return rejected(state, "This boss has already been defeated.")
        }
        if (state.activeBossBattleWorldId == worldId) {
            return accepted(state.copy(activeBossBattleWorldId = null), "Left the ${world.bossName} battle.")
        }
        val totalUpgradeLevels = state.upgradeLevels.values.sum()
        val canFight = state.endlessRiftActive || (
            state.correctGuesses >= world.fightCorrectGuesses &&
                totalUpgradeLevels >= world.fightUpgradeLevels &&
                state.prestigeCount >= world.fightPrestigeCount &&
                state.ultraCount >= world.fightUltraCount
        )
        if (!canFight) return rejected(state, "Boss battle requirements are not met.")
        return accepted(
            state.copy(activeWorldId = worldId, activeBossBattleWorldId = worldId),
            "Started the ${world.bossName} battle."
        )
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
    fun evaluateAchievements(state: GameState): GameState =
        GameEngine().evaluateAchievements(state)

    private data class WorldRequirement(
        val id: String,
        val name: String,
        val bossId: String,
        val bossName: String,
        val bossHp: Int,
        val baseMoneyReward: String,
        val baseNebulaReward: Long,
        val fightCorrectGuesses: Long,
        val fightUpgradeLevels: Int,
        val fightPrestigeCount: Long,
        val fightUltraCount: Long,
        val requiredBossId: String?,
        val requiredCorrectGuesses: Long,
        val requiredUpgradeLevels: Int,
        val requiredPrestigeCount: Long,
        val requiredUltraCount: Long,
        val requirement: String
    )

    data class DesktopBossBattleStatus(
        val worldId: String,
        val bossId: String,
        val bossName: String,
        val hp: Int,
        val damage: Int,
        val mistakes: Int
    )

    fun activeBossBattleStatus(state: GameState): DesktopBossBattleStatus? {
        val worldId = state.activeBossBattleWorldId ?: return null
        val world = WORLD_REQUIREMENTS.firstOrNull { it.id == worldId } ?: return null
        val hp = world.bossHp + if (state.endlessRiftActive) ((state.endlessRiftTier - 1).coerceAtLeast(0) * 2) else 0
        return DesktopBossBattleStatus(
            worldId = world.id,
            bossId = world.bossId,
            bossName = world.bossName,
            hp = hp,
            damage = (state.bossBattleProgress[world.bossId] ?: 0).coerceIn(0, hp),
            mistakes = state.bossBattleMistakes[world.bossId] ?: 0
        )
    }

    data class DesktopWorldProgress(
        val id: String,
        val name: String,
        val unlocked: Boolean,
        val selected: Boolean,
        val canUnlock: Boolean,
        val requirement: String
    )

    fun setMutatorActive(
        state: GameState,
        mutatorId: String,
        active: Boolean,
        engine: GameEngine,
        rng: Random = Random.Default
    ): EconomyTransaction {
        if (mutatorId !in setOf("mut_hardcore", "mut_speed", "mut_blind", "mut_tax")) {
            return rejected(state, "Unknown mutator.")
        }
        if ((mutatorId in state.activeMutators) == active) {
            return rejected(state, if (active) "Mutator is already active." else "Mutator is already inactive.")
        }
        val selected = state.activeMutators.toMutableSet().apply {
            if (active) add(mutatorId) else remove(mutatorId)
        }.toSet()
        val updated = engine.applyMutatorSet(state, selected)
        val rangeMax = engine.getRangeMax(updated).coerceAtLeast(1L)
        val target = rng.nextLong(1L, rangeMax + 1L)
        return accepted(
            updated.copy(currentRangeMin = 1L, currentRangeMax = rangeMax, targetNumber = target),
            if (active) "Enabled $mutatorId." else "Disabled $mutatorId."
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

    /** Restores one Home Base level using Android's persisted cost/progression rules. */
    fun upgradeHomeBase(state: GameState): EconomyTransaction {
        val level = state.homeBaseLevel
        if (level >= 20) return rejected(state, "The sanctuary is fully restored.")
        if (level < 0) return rejected(state, "Invalid sanctuary level.")
        val cost = BigNumber(50_000L * (level + 1L))
        if (state.money < cost) return rejected(state, "Not enough Money to restore the next level.")
        return accepted(
            state.copy(
                money = state.money - cost,
                homeBaseLevel = level + 1,
                statistics = state.statistics.copy(moneySpent = state.statistics.moneySpent + cost)
            ),
            "Sanctuary restored to level ${level + 1}."
        )
    }

    fun buyTalent(state: GameState, talentId: String, cost: Long): EconomyTransaction {
        val legacyParents = mapOf(
            "talent_speed_2" to "talent_speed_1",
            "talent_crit_2" to "talent_crit_1",
            "talent_reward_1" to "talent_crit_2",
            "talent_master_1" to "talent_reward_1"
        )
        return buyTalent(state, talentId, cost, legacyParents[talentId])
    }

    /** Validates talent purchases against the bundled canonical Android talent catalog. */
    fun buyTalent(
        state: GameState,
        talentId: String,
        cost: Long,
        requiredParentId: String?
    ): EconomyTransaction {
        // Never trust the UI's displayed price or prerequisite. Read the checked-in
        // catalog at runtime so a catalog update cannot silently drift from this rule.
        val canonicalRule = runCatching { canonicalTalentRules[talentId] }.getOrNull()
            ?: return rejected(state, "Talent catalog unavailable or unknown talent.")
        if (cost != canonicalRule.cost || requiredParentId != canonicalRule.requiredParentId) {
            return rejected(state, "Talent definition does not match the canonical catalog.")
        }
        if (talentId in state.prestigeShopPurchases) return rejected(state, "Already unlocked.")
        if (state.prestige < BigNumber(canonicalRule.cost)) return rejected(state, "Not enough Prestige.")
        if (canonicalRule.requiredParentId != null &&
            canonicalRule.requiredParentId !in state.prestigeShopPurchases
        ) {
            return rejected(state, "Unlock the prerequisite talent first.")
        }
        return accepted(
            state.copy(
                prestige = state.prestige - BigNumber(canonicalRule.cost),
                prestigeShopPurchases = state.prestigeShopPurchases + talentId
            ),
            "Talent unlocked."
        )
    }

    private data class CanonicalTalentRule(
        val cost: Long,
        val requiredParentId: String?
    )

    private val canonicalTalentRules: Map<String, CanonicalTalentRule> by lazy { loadCanonicalTalentRules() }

    private fun loadCanonicalTalentRules(): Map<String, CanonicalTalentRule> {
        val stream = DesktopEconomyTransactions::class.java.classLoader
            .getResourceAsStream("game/talents.json")
            ?: error("Missing canonical talent catalog resource: game/talents.json")
        val root = stream.bufferedReader(Charsets.UTF_8).use {
            Json.parseToJsonElement(it.readText()).jsonObject
        }
        val entries = root["talents"] as? JsonArray
            ?: error("Canonical talent catalog has no talents array")
        val rules = linkedMapOf<String, CanonicalTalentRule>()
        entries.forEach { entry ->
            val item = entry as? JsonObject ?: error("Invalid talent catalog entry")
            val id = item["id"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() }
                ?: error("Talent catalog entry has no ID")
            val cost = item["cost"]?.jsonPrimitive?.content?.toLongOrNull()
                ?.takeIf { it >= 0L } ?: error("Talent catalog entry has invalid cost")
            val parentElement = item["requiredParentId"]
                ?: error("Talent catalog entry has no requiredParentId field")
            val parentId = if (parentElement == JsonNull) null else {
                parentElement.jsonPrimitive.content.takeIf { it.isNotBlank() }
                    ?: error("Talent catalog entry has invalid requiredParentId")
            }
            require(rules.put(id, CanonicalTalentRule(cost, parentId)) == null) {
                "Duplicate talent ID in canonical catalog: $id"
            }
        }
        return rules
    }

    private val WORLD_REQUIREMENTS = listOf(
        WorldRequirement("verdant_grove", "Verdant Grove", "verdant_guardian", "Verdant Guardian", 3, "25000", 10L, 10L, 3, 0L, 0L, null, 0L, 0, 0L, 0L, "Starting world"),
        WorldRequirement("crystal_caverns", "Crystal Caverns", "crystal_golem", "Crystal Golem", 5, "100000", 25L, 50L, 10, 0L, 0L, "verdant_guardian", 25L, 5, 0L, 0L, "Defeat Verdant Guardian · 25 correct guesses · 5 upgrade levels"),
        WorldRequirement("ember_summit", "Ember Summit", "ember_dragon", "Ember Dragon", 7, "500000", 75L, 150L, 25, 1L, 0L, "crystal_golem", 100L, 15, 1L, 0L, "Defeat Crystal Golem · 100 correct guesses · 15 upgrade levels · 1 Prestige"),
        WorldRequirement("nebula_rift", "Nebula Rift", "nebula_titan", "Nebula Titan", 10, "5000000", 250L, 500L, 75, 5L, 1L, "ember_dragon", 250L, 40, 3L, 1L, "Defeat Ember Dragon · 250 correct guesses · 40 upgrade levels · 3 Prestige · 1 Ultra")
    )

    private fun accepted(state: GameState, message: String) =
        EconomyTransaction(state, true, message)

    private fun rejected(state: GameState, message: String) =
        EconomyTransaction(state, false, message)
}
