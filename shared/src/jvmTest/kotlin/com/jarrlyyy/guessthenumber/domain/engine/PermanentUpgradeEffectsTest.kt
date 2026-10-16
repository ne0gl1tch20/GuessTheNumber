package com.jarrlyyy.guessthenumber.domain.engine

import com.jarrlyyy.guessthenumber.domain.model.BigNumber
import com.jarrlyyy.guessthenumber.domain.model.GameState
import com.jarrlyyy.guessthenumber.desktop.economy.DesktopEconomyTransactions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class PermanentUpgradeEffectsTest {
    private val engine = GameEngine()

    @Test
    fun prestigeAndUltraMoneyBoostsStackAndApplyToRewards() {
        val state = GameState(
            prestigeUpgradeLevels = mapOf("prestige_money_boost" to 2),
            ultraUpgradeLevels = mapOf("ultra_money_boost" to 3)
        )

        assertEquals(0, engine.moneyEarningMultiplier(state).compareTo(BigNumber(14)))
        assertEquals(
            0,
            engine.applyMoneyEarningBoost(state, BigNumber(100)).compareTo(BigNumber(1_400))
        )
    }

    @Test
    fun actualGuessRewardUsesBothPermanentMoneyMultipliers() {
        val baseState = GameState(targetNumber = 50)
        val boostedState = baseState.copy(
            prestigeUpgradeLevels = mapOf("prestige_money_boost" to 2),
            ultraUpgradeLevels = mapOf("ultra_money_boost" to 3)
        )

        val base = GameEngine(Random(42)).processGuess(baseState, 50)
        val boosted = GameEngine(Random(42)).processGuess(boostedState, 50)

        assertEquals(0, (base.reward * BigNumber(14)).compareTo(boosted.reward))
    }

    @Test
    fun prestigeGainBoostIncreasesResetRewardAndStartingCapitalIsGranted() {
        val state = GameState(
            money = BigNumber(100_000_000),
            prestigeUpgradeLevels = mapOf(
                "prestige_gain_boost" to 5,
                "prestige_starting_capital" to 2
            )
        )
        val baseReward = engine.calculatePrestigeReward(state.money, state.prestigeCount)
        val boostedReward = engine.calculatePrestigeReward(state)

        assertEquals(0, (baseReward * BigNumber(2.0)).floor().compareTo(boostedReward))
        assertEquals(0, engine.prestigeStartingCapital(state).compareTo(BigNumber(2_000)))
    }

    @Test
    fun ultraGainBoostIncreasesResetRewardButNeverExceedsCatalogCap() {
        val state = GameState(
            money = BigNumber(100_000_000_000L),
            ultraUpgradeLevels = mapOf("ultra_gain_boost" to 2)
        )
        val baseReward = engine.calculateUltraReward(state.money, state.ultraCount)
        val boostedReward = engine.calculateUltraReward(state)

        assertTrue(baseReward > BigNumber.ZERO)
        assertTrue(boostedReward > baseReward)
        assertTrue(boostedReward <= BigNumber(2_000))

        val maxed = state.copy(
            money = BigNumber("1000000000000000000000000000000000000"),
            ultraUpgradeLevels = mapOf("ultra_gain_boost" to 10)
        )
        assertEquals(0, engine.calculateUltraReward(maxed).compareTo(BigNumber(2_000)))
    }

    @Test
    fun desktopPrestigeAndUltraResetsGrantEndowmentBeforeResettingUpgradeLevels() {
        val prestigeState = GameState(
            money = engine.calculatePrestigeRequirement(0),
            prestigeUpgradeLevels = mapOf("prestige_starting_capital" to 2)
        )
        val prestige = DesktopEconomyTransactions.prestigeReset(prestigeState, engine)
        assertTrue(prestige.accepted)
        assertEquals(0, prestige.state.money.compareTo(BigNumber(2_000)))

        val ultraState = GameState(
            money = engine.calculateUltraRequirement(0) * BigNumber(100),
            prestige = BigNumber(1_000),
            prestigeUpgradeLevels = mapOf("prestige_starting_capital" to 3)
        )
        val ultra = DesktopEconomyTransactions.ultraReset(ultraState, engine)
        assertTrue(ultra.accepted)
        assertEquals(0, ultra.state.money.compareTo(BigNumber(3_000)))
        assertTrue(ultra.state.prestigeUpgradeLevels.isEmpty())
    }

    @Test
    fun nebulaBoostAppliesToAchievementAndArcadeRewardHelperWithSafeSaturation() {
        val state = GameState(
            ultraUpgradeLevels = mapOf("ultra_nebula_boost" to 2)
        )
        assertEquals(30L, engine.calculateNebulaReward(state, 10L))
        assertEquals(
            60L,
            engine.calculateNebulaReward(state.copy(shopPurchases = setOf("nebula_boost")), 10L)
        )
        assertEquals(Long.MAX_VALUE, engine.calculateNebulaReward(
            state.copy(ultraUpgradeLevels = mapOf("ultra_nebula_boost" to 15), shopPurchases = setOf("nebula_boost")),
            Long.MAX_VALUE
        ))
    }

    @Test
    fun achievementNebulaStatisticsSaturateInsteadOfWrappingNegative() {
        val state = GameState(
            correctGuesses = 10,
            ultraUpgradeLevels = mapOf("ultra_nebula_boost" to 15),
            shopPurchases = setOf("nebula_boost"),
            statistics = com.jarrlyyy.guessthenumber.domain.model.GameStatistics(
                nebulaEarned = Long.MAX_VALUE - 5L
            )
        )

        val updated = engine.evaluateAchievements(state)

        assertEquals(Long.MAX_VALUE, updated.statistics.nebulaEarned)
        assertTrue(updated.nebula > state.nebula)
    }

    @Test
    fun desktopArcadeRewardTransactionUsesTheSamePermanentUpgradeRules() {
        val state = GameState(
            ultraUpgradeLevels = mapOf(
                "ultra_money_boost" to 1,
                "ultra_nebula_boost" to 2
            )
        )
        val transaction = DesktopEconomyTransactions.awardArcadeReward(
            state = state,
            nebulaReward = 10L,
            moneyReward = BigNumber(100)
        )

        assertTrue(transaction.accepted)
        assertEquals(0, transaction.state.nebula.compareTo(BigNumber(30)))
        assertEquals(0, transaction.state.money.compareTo(BigNumber(300)))
        assertEquals(30L, transaction.state.statistics.nebulaEarned)
        assertEquals(0, transaction.state.statistics.moneyEarned.compareTo(BigNumber(300)))
    }

    @Test
    fun achievementRewardUsesSharedNebulaBoostAndCannotBeClaimedTwice() {
        val state = GameState(
            correctGuesses = 10,
            ultraUpgradeLevels = mapOf("ultra_nebula_boost" to 2)
        )

        val first = engine.evaluateAchievements(state)
        val second = engine.evaluateAchievements(first)

        assertTrue("correct_10" in first.achievements)
        assertEquals(0, first.nebula.compareTo(BigNumber(30)))
        assertEquals(30L, first.statistics.nebulaEarned)
        assertEquals(first, second)
    }
}
