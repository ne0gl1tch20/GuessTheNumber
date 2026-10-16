package com.jarrlyyy.guessthenumber.desktop.economy

import com.jarrlyyy.guessthenumber.domain.engine.GameEngine
import com.jarrlyyy.guessthenumber.domain.model.BigNumber
import com.jarrlyyy.guessthenumber.domain.model.GameState
import com.jarrlyyy.guessthenumber.domain.model.UpgradeDef
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

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

        // BigNumber uses BigDecimal internally, whose equality is scale-sensitive.
        // Compare numeric value here because percentage bonuses can produce 250.0.
        assertEquals(0, result.money.compareTo(BigNumber(250)))
        assertEquals(0, result.statistics.moneyEarned.compareTo(BigNumber(250)))
    }

    @Test
    fun stackedProgressionBonusesMatchExpectedMoneyAndNebulaTotals() {
        val before = GameState(
            money = BigNumber(100),
            nebula = BigNumber(10),
            prestigeShopPurchases = setOf("talent_reward_1", "talent_master_1"),
            permanentEventBoosts = 5,
            activeWorldId = "ember_summit",
            worldMasteryLevels = mapOf("ember_summit" to 5),
            equippedRelicIds = setOf("verdant_guardian", "ember_dragon", "nebula_titan"),
            discoveredSecretIds = setOf("ashen_vault"),
            homeBaseLevel = 2,
            statistics = com.jarrlyyy.guessthenumber.domain.model.GameStatistics(
                moneyEarned = BigNumber(100),
                nebulaEarned = 5
            )
        )
        val after = before.copy(
            money = BigNumber(200),
            statistics = before.statistics.copy(moneyEarned = BigNumber(200))
        )

        val result = DesktopEconomyTransactions.applyGuessBonuses(
            before = before,
            after = after,
            correct = true,
            rng = kotlin.random.Random(1234)
        )

        // 50% talent + 10% event + 10% mastery + 10% relic +
        // 25% Ember + 25% Nebula Titan + 20% three-relic set +
        // 4% Home Base + 5% active-world secret = 159% of base earnings.
        assertEquals(0, result.money.compareTo(BigNumber(359)))
        assertEquals(0, result.statistics.moneyEarned.compareTo(BigNumber(359)))
        // Master talent + event tier + Ember relic + active-world secret.
        assertEquals(BigNumber(14), result.nebula)
        assertEquals(9L, result.statistics.nebulaEarned)
    }

    @Test
    fun incorrectGuessDoesNotApplyPostGuessBonuses() {
        val before = GameState(prestigeShopPurchases = setOf("talent_reward_1"))
        val after = before.copy(money = BigNumber(10))

        assertEquals(after, DesktopEconomyTransactions.applyGuessBonuses(before, after, correct = false))
    }

    @Test
    fun correctGuessRecordsDailyProgressAndCommonChestDrop() {
        val before = GameState(
            dailyQuestDate = "2026-10-15",
            dailyQuestGuesses = 9,
            dailyQuestCorrectGuesses = 4,
            dailyQuestBestStreak = 4,
            dailyQuestUpgradePurchases = 3,
            claimedDailyQuests = setOf("daily_guesses_10"),
            lootCommonChests = 2,
            lootDropsFound = 2
        )
        val after = before.copy(money = BigNumber(150), streak = 2)

        val result = DesktopEconomyTransactions.recordGuessProgress(
            before = before,
            after = after,
            correct = true,
            today = "2026-10-16",
            lootRoll = 239
        )

        assertEquals("2026-10-16", result.dailyQuestDate)
        assertEquals(1, result.dailyQuestGuesses)
        assertEquals(1, result.dailyQuestCorrectGuesses)
        assertEquals(2, result.dailyQuestBestStreak)
        assertEquals(0, result.dailyQuestUpgradePurchases)
        assertTrue(result.claimedDailyQuests.isEmpty())
        assertEquals(3, result.lootCommonChests)
        assertEquals(3L, result.lootDropsFound)
        assertEquals("common", result.lastLootDropTier)
        assertEquals(1L, result.lootEventId)
        assertEquals(BigNumber(150), result.money)
    }

    @Test
    fun treasureDropRollBoundariesMatchAndroidRates() {
        fun roll(value: Int) = DesktopEconomyTransactions.recordGuessProgress(
            before = GameState(dailyQuestDate = "2026-10-16"),
            after = GameState(dailyQuestDate = "2026-10-16", streak = 1),
            correct = true,
            today = "2026-10-16",
            lootRoll = value
        )

        assertEquals(1, roll(9).lootEpicChests)
        assertEquals(1, roll(10).lootRareChests)
        assertEquals(1, roll(59).lootRareChests)
        assertEquals(1, roll(60).lootCommonChests)
        assertEquals(1, roll(239).lootCommonChests)
        assertEquals(0L, roll(240).lootDropsFound)
        assertEquals(0L, roll(999).lootDropsFound)
    }

    @Test
    fun incorrectGuessAdvancesDailyGuessCountWithoutDroppingChest() {
        val before = GameState(dailyQuestDate = "2026-10-16")
        val result = DesktopEconomyTransactions.recordGuessProgress(
            before = before,
            after = before.copy(streak = 0),
            correct = false,
            today = "2026-10-16",
            lootRoll = 0
        )

        assertEquals(1, result.dailyQuestGuesses)
        assertEquals(0, result.dailyQuestCorrectGuesses)
        assertEquals(0L, result.lootDropsFound)
        assertEquals(0, result.lootEpicChests)
    }

    @Test
    fun dailyQuestClaimsAwardExactRewardsAndCannotBeClaimedTwice() {
        val today = "2026-10-16"
        var state = GameState(
            dailyQuestDate = today,
            dailyQuestGuesses = 10,
            dailyQuestCorrectGuesses = 5,
            dailyQuestBestStreak = 5,
            dailyQuestUpgradePurchases = 3
        )

        for (questId in listOf("daily_guesses_10", "daily_correct_5", "daily_streak_5", "daily_upgrades_3")) {
            val claim = DesktopEconomyTransactions.claimDailyQuest(state, questId, today)
            assertTrue("Expected $questId to be claimable", claim.accepted)
            state = claim.state
        }

        assertEquals(BigNumber(10_000), state.money)
        assertEquals(BigNumber(110), state.nebula)
        assertEquals(BigNumber(10_000), state.statistics.moneyEarned)
        assertEquals(110L, state.statistics.nebulaEarned)
        assertEquals(4, state.claimedDailyQuests.size)

        val duplicate = DesktopEconomyTransactions.claimDailyQuest(state, "daily_guesses_10", today)
        assertFalse(duplicate.accepted)
        assertEquals(state, duplicate.state)
    }

    @Test
    fun incompleteDailyQuestCannotBeClaimed() {
        val state = GameState(dailyQuestDate = "2026-10-16", dailyQuestGuesses = 9)
        val result = DesktopEconomyTransactions.claimDailyQuest(state, "daily_guesses_10", "2026-10-16")

        assertFalse(result.accepted)
        assertEquals(state, result.state)
    }

    @Test
    fun upgradePurchaseRefreshesDailyQuestBeforeCountingPurchase() {
        val stale = GameState(
            money = BigNumber(1000),
            dailyQuestDate = "2026-10-15",
            dailyQuestUpgradePurchases = 2,
            claimedDailyQuests = setOf("daily_upgrades_3")
        )

        val result = DesktopEconomyTransactions.buyUpgrade(stale, rewardUpgrade)

        assertTrue(result.accepted)
        assertEquals(java.time.LocalDate.now().toString(), result.state.dailyQuestDate)
        assertEquals(1, result.state.dailyQuestUpgradePurchases)
        assertTrue(result.state.claimedDailyQuests.isEmpty())
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
    fun nebulaBoostCanBePurchasedAndUnknownEffectsRemainRejected() {
        val initial = GameState(nebula = BigNumber(100))
        val boost = com.jarrlyyy.guessthenumber.domain.model.ShopItemDef(
            id = "nebula_boost",
            name = "Nebula Crystal",
            description = "Permanently doubles Nebula rewards.",
            nebulaCost = 50,
            category = "boosts",
            isCosmetic = false
        )

        val bought = DesktopEconomyTransactions.buyShopItem(initial, boost)
        assertTrue(bought.accepted)
        assertEquals(BigNumber(50), bought.state.nebula)
        assertTrue("nebula_boost" in bought.state.shopPurchases)

        val unknown = boost.copy(id = "unimplemented_boost")
        val rejected = DesktopEconomyTransactions.buyShopItem(initial, unknown)
        assertFalse(rejected.accepted)
        assertEquals(initial, rejected.state)
    }

    @Test
    fun nebulaBoostDoublesDailyQuestAndAchievementRewardsExactlyOnce() {
        val boosted = GameState(
            nebula = BigNumber(3),
            shopPurchases = setOf("nebula_boost"),
            dailyQuestDate = "2026-10-16",
            dailyQuestCorrectGuesses = 5
        )
        val quest = DesktopEconomyTransactions.claimDailyQuest(boosted, "daily_correct_5", "2026-10-16")
        assertTrue(quest.accepted)
        assertEquals(BigNumber(53), quest.state.nebula)
        assertEquals(50L, quest.state.statistics.nebulaEarned)

        val eligible = quest.state.copy(correctGuesses = 10)
        val rewarded = DesktopEconomyTransactions.evaluateAchievements(eligible)
        assertTrue("correct_10" in rewarded.achievements)
        assertEquals(BigNumber(73), rewarded.nebula)
        assertEquals(70L, rewarded.statistics.nebulaEarned)

        assertEquals(rewarded, DesktopEconomyTransactions.evaluateAchievements(rewarded))
    }

    @Test
    fun goldenGuessPassForcesCriticalRewardOnTenthLifetimeGuess() {
        val state = GameState(
            targetNumber = 42,
            shopPurchases = setOf("golden_guess"),
            statistics = com.jarrlyyy.guessthenumber.domain.model.GameStatistics(totalGuesses = 9)
        )
        val result = GameEngine(kotlin.random.Random(1234)).processGuess(state, 42)
        assertTrue(result.isCritical)
        assertEquals(10L, result.newState.statistics.totalGuesses)
    }

    @Test
    fun nebulaBoostDoublesNebulaGrantedByGuessBonuses() {
        val before = GameState(
            nebula = BigNumber(4),
            shopPurchases = setOf("nebula_boost"),
            prestigeShopPurchases = setOf("talent_master_1"),
            statistics = com.jarrlyyy.guessthenumber.domain.model.GameStatistics(nebulaEarned = 4)
        )
        val after = before.copy(
            money = BigNumber(100),
            statistics = before.statistics.copy(moneyEarned = BigNumber(100))
        )
        val result = DesktopEconomyTransactions.applyGuessBonuses(before, after, correct = true)
        assertEquals(BigNumber(6), result.nebula)
        assertEquals(6L, result.statistics.nebulaEarned)
    }

    @Test
    fun achievementMilestonePaysOnceAndUpdatesNebulaLifetimeStats() {
        val initial = GameState(
            correctGuesses = 10,
            nebula = BigNumber(2),
            statistics = com.jarrlyyy.guessthenumber.domain.model.GameStatistics(nebulaEarned = 2)
        )
        val first = DesktopEconomyTransactions.evaluateAchievements(initial)
        assertTrue("correct_10" in first.achievements)
        assertEquals(BigNumber(12), first.nebula)
        assertEquals(12L, first.statistics.nebulaEarned)
        assertEquals(first, DesktopEconomyTransactions.evaluateAchievements(first))
    }

    @Test
    fun worldUnlockChecksRequirementsAndIsIdempotent() {
        val locked = GameState(
            correctGuesses = 24,
            upgradeLevels = mapOf("a" to 3, "b" to 2),
            defeatedBossIds = setOf("verdant_guardian")
        )
        val denied = DesktopEconomyTransactions.unlockWorld(locked, "crystal_caverns")
        assertFalse(denied.accepted)
        assertEquals(locked, denied.state)

        val ready = locked.copy(correctGuesses = 25)
        val unlocked = DesktopEconomyTransactions.unlockWorld(ready, "crystal_caverns")
        assertTrue(unlocked.accepted)
        assertTrue("crystal_caverns" in unlocked.state.unlockedWorldIds)
        assertEquals("crystal_caverns", unlocked.state.activeWorldId)

        val duplicate = DesktopEconomyTransactions.unlockWorld(unlocked.state, "crystal_caverns")
        assertFalse(duplicate.accepted)
        assertEquals(unlocked.state, duplicate.state)
    }

    @Test
    fun worldSelectionCannotBypassUnlockRequirements() {
        val state = GameState()
        val denied = DesktopEconomyTransactions.selectWorld(state, "nebula_rift")
        assertFalse(denied.accepted)
        assertEquals(state, denied.state)

        val selected = DesktopEconomyTransactions.selectWorld(state, "verdant_grove")
        assertTrue(selected.accepted)
        assertEquals("verdant_grove", selected.state.activeWorldId)
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

    @Test
    fun bulkMoneyUpgradeChargesExactCombinedCostAndCountsOneQuestPurchase() {
        val initial = GameState(money = BigNumber(1_000), buyMultiplier = "10")
        val result = DesktopEconomyTransactions.buyUpgrade(initial, rewardUpgrade)

        assertTrue(result.accepted)
        assertEquals(3, result.state.upgradeLevels[rewardUpgrade.id])
        assertEquals(0, result.state.statistics.moneySpent.compareTo(BigNumber(475)))
        assertEquals(0, result.state.money.compareTo(BigNumber(525)))
        assertEquals(1, result.state.dailyQuestUpgradePurchases)
    }

    @Test
    fun maxUpgradeBuysOnlyLargestAffordablePrefix() {
        val upgrade = rewardUpgrade.copy(maxLevel = 10, baseCost = "100", costMultiplier = 2.0)
        val initial = GameState(money = BigNumber(350), buyMultiplier = "MAX")
        val cost = DesktopEconomyTransactions.upgradeBatchCost(upgrade, 0, "MAX", initial.money)
        val result = DesktopEconomyTransactions.buyUpgrade(initial, upgrade)

        assertEquals(0, cost.compareTo(BigNumber(300)))
        assertTrue(result.accepted)
        assertEquals(2, result.state.upgradeLevels[upgrade.id])
        assertEquals(0, result.state.money.compareTo(BigNumber(50)))
        assertEquals(0, result.state.statistics.moneySpent.compareTo(BigNumber(300)))
    }

    @Test
    fun fixedBulkPurchaseBuysLargestAffordablePrefix() {
        val initial = GameState(money = BigNumber(200), buyMultiplier = "10")
        val cost = DesktopEconomyTransactions.upgradeBatchCost(rewardUpgrade, 0, "10", initial.money)
        val result = DesktopEconomyTransactions.buyUpgrade(initial, rewardUpgrade)

        assertEquals(BigNumber(100), cost)
        assertTrue(result.accepted)
        assertEquals(1, result.state.upgradeLevels[rewardUpgrade.id])
        assertEquals(BigNumber(100), result.state.money)
        assertEquals(BigNumber(100), result.state.statistics.moneySpent)
    }

    @Test
    fun buyMultiplierAcceptsOnlySupportedModes() {
        val initial = GameState()
        assertEquals("MAX", DesktopEconomyTransactions.setBuyMultiplier(initial, "MAX").state.buyMultiplier)
        val rejected = DesktopEconomyTransactions.setBuyMultiplier(initial, "1000")
        assertFalse(rejected.accepted)
        assertEquals(initial, rejected.state)
    }

    @Test
    fun mutatorToggleUsesSharedRulesAndRefreshesRangeAndTarget() {
        val engine = GameEngine()
        val initial = GameState(currentRangeMin = 1, currentRangeMax = 100, targetNumber = 50)
        val result = DesktopEconomyTransactions.setMutatorActive(
            state = initial,
            mutatorId = "mut_hardcore",
            active = true,
            engine = engine,
            rng = Random(7)
        )

        assertTrue(result.accepted)
        assertTrue("mut_hardcore" in result.state.activeMutators)
        assertEquals(200L, result.state.currentRangeMax)
        assertTrue(result.state.targetNumber in 1L..200L)
    }

    @Test
    fun unknownMutatorIsRejectedWithoutChangingState() {
        val initial = GameState()
        val result = DesktopEconomyTransactions.setMutatorActive(
            initial, "mut_not_real", true, GameEngine(), Random(1)
        )

        assertFalse(result.accepted)
        assertEquals(initial, result.state)
    }

    @Test
    fun canonicalTalentPrerequisiteIsEnforcedAndThenAllowsPurchase() {
        val initial = GameState(prestige = BigNumber(1_000))
        val blocked = DesktopEconomyTransactions.buyTalent(
            initial, "talent_speed_2", 500L, "talent_speed_1"
        )
        assertFalse(blocked.accepted)
        assertEquals(initial, blocked.state)

        val root = DesktopEconomyTransactions.buyTalent(
            initial, "talent_speed_1", 100L, null
        )
        assertTrue(root.accepted)
        assertEquals(BigNumber(900), root.state.prestige)

        val child = DesktopEconomyTransactions.buyTalent(
            root.state, "talent_speed_2", 500L, "talent_speed_1"
        )
        assertTrue(child.accepted)
        assertEquals(BigNumber(400), child.state.prestige)
        assertTrue("talent_speed_2" in child.state.prestigeShopPurchases)
    }
}
