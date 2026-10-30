package com.jarrlyyy.guessthenumber.domain.engine

import com.jarrlyyy.guessthenumber.domain.model.BigNumber
import com.jarrlyyy.guessthenumber.domain.model.GameState
import com.jarrlyyy.guessthenumber.desktop.economy.DesktopEconomyTransactions
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BigNumberProgressionRewardRulesTest {
    private val engine = GameEngine()

    @Test
    fun prestigeRewardKeepsPrecisionWhenMoneyAndRequirementExceedDoubleRange() {
        val requirement = engine.calculatePrestigeRequirement(0)
        val money = requirement * BigNumber("1e100")

        val reward = engine.calculatePrestigeReward(money, 0)

        assertEquals(0, reward.compareTo(BigNumber("1e50")))
    }

    @Test
    fun prestigeRewardDoesNotRoundValuesJustBelowPerfectSquaresUp() {
        val justBelowFour = BigNumber("3.999999999999999999999999999999999999")
        val justAboveFour = BigNumber("4.000000000000000000000000000000000001")

        assertEquals(0, BigNumberProgressionRewardRules.prestigeReward(justBelowFour, BigNumber.ONE)
            .compareTo(BigNumber.ONE))
        assertEquals(0, BigNumberProgressionRewardRules.prestigeReward(justAboveFour, BigNumber.ONE)
            .compareTo(BigNumber(2)))
    }

    @Test
    fun ultraRewardUsesRatioExponentWithoutConvertingHugeCurrenciesToDouble() {
        val requirement = engine.calculateUltraRequirement(0)
        val moneyForThousand = requirement * BigNumber("1e10")
        val moneyBeyondCap = requirement * BigNumber("1e100")

        assertEquals(0, engine.calculateUltraReward(moneyForThousand, 0).compareTo(BigNumber(1_000)))
        assertEquals(0, engine.calculateUltraReward(moneyBeyondCap, 0).compareTo(BigNumber(2_000)))
    }

    @Test
    fun ultraRewardDoesNotRoundValuesJustBelowAnIntegerBoundaryUp() {
        val justBelowCap = BigNumber("99999999999999999999")

        assertEquals(
            0,
            BigNumberProgressionRewardRules.ultraReward(justBelowCap, BigNumber.ONE)
                .compareTo(BigNumber(1_999))
        )
        assertEquals(
            0,
            BigNumberProgressionRewardRules.ultraReward(BigNumber("1e20"), BigNumber.ONE)
                .compareTo(BigNumber(2_000))
        )
    }

    @Test
    fun progressionRewardsRejectAmountsBelowRequirementAndHandleZeroRequirement() {
        val requirement = engine.calculatePrestigeRequirement(0)
        val below = requirement - BigNumber.ONE

        assertEquals(BigNumber.ZERO, engine.calculatePrestigeReward(below, 0))
        assertEquals(BigNumber.ZERO, BigNumberProgressionRewardRules.prestigeReward(BigNumber.ONE, BigNumber.ZERO))
        assertEquals(BigNumber.ZERO, BigNumberProgressionRewardRules.ultraReward(BigNumber.ONE, BigNumber.ZERO))
    }

    @Test
    fun hugeCurrencyAndPermanentProgressionLevelsSurviveSaveSerialization() {
        val original = GameState(
            money = BigNumber("1e100"),
            prestige = BigNumber("2.5e80"),
            ultra = BigNumber("3e60"),
            prestigeUpgradeLevels = mapOf("prestige_money_boost" to 4, "prestige_gain_boost" to 5),
            ultraUpgradeLevels = mapOf("ultra_money_boost" to 3, "ultra_gain_boost" to 2)
        )

        val encoded = Json.encodeToString(GameState.serializer(), original)
        val restored = Json.decodeFromString(GameState.serializer(), encoded)

        // BigDecimal scale is not semantically significant for saved currency values.
        assertEquals(0, original.money.compareTo(restored.money))
        assertEquals(0, original.prestige.compareTo(restored.prestige))
        assertEquals(0, original.ultra.compareTo(restored.ultra))
        assertEquals(original.prestigeUpgradeLevels, restored.prestigeUpgradeLevels)
        assertEquals(original.ultraUpgradeLevels, restored.ultraUpgradeLevels)
        assertEquals(
            0,
            engine.calculatePrestigeReward(restored.money, restored.prestigeCount)
                .compareTo(engine.calculatePrestigeReward(original.money, original.prestigeCount))
        )
    }


    @Test
    fun prestigeAndUltraResetResultsRoundTripWithoutLosingPermanentProgress() {
        val prestigeInput = GameState(
            money = engine.calculatePrestigeRequirement(0) * BigNumber("1e100"),
            prestige = BigNumber("7e80"),
            prestigeUpgradeLevels = mapOf(
                "prestige_gain_boost" to 3,
                "prestige_starting_capital" to 2
            ),
            ultraUpgradeLevels = mapOf("ultra_money_boost" to 4),
            shopPurchases = setOf("nebula_boost")
        )
        val prestigeResult = DesktopEconomyTransactions.prestigeReset(prestigeInput, engine)
        assertTrue(prestigeResult.accepted)
        val restoredPrestige = Json.decodeFromString(
            GameState.serializer(),
            Json.encodeToString(GameState.serializer(), prestigeResult.state)
        )
        assertEquals(0, prestigeResult.state.money.compareTo(BigNumber(2_000)))
        assertEquals(0, restoredPrestige.money.compareTo(prestigeResult.state.money))
        assertEquals(0, restoredPrestige.prestige.compareTo(prestigeResult.state.prestige))
        assertEquals(prestigeInput.prestigeUpgradeLevels, restoredPrestige.prestigeUpgradeLevels)
        assertEquals(prestigeInput.ultraUpgradeLevels, restoredPrestige.ultraUpgradeLevels)
        assertEquals(prestigeInput.shopPurchases, restoredPrestige.shopPurchases)
        assertTrue(restoredPrestige.upgradeLevels.isEmpty())

        val ultraInput = GameState(
            money = engine.calculateUltraRequirement(0) * BigNumber("1e100"),
            prestige = BigNumber(1_000),
            prestigeUpgradeLevels = mapOf("prestige_starting_capital" to 3),
            ultraUpgradeLevels = mapOf("ultra_gain_boost" to 2),
            shopPurchases = setOf("nebula_boost")
        )
        val ultraResult = DesktopEconomyTransactions.ultraReset(ultraInput, engine)
        assertTrue(ultraResult.accepted)
        val restoredUltra = Json.decodeFromString(
            GameState.serializer(),
            Json.encodeToString(GameState.serializer(), ultraResult.state)
        )
        assertEquals(0, ultraResult.state.money.compareTo(BigNumber(3_000)))
        assertEquals(BigNumber.ZERO, restoredUltra.prestige)
        assertTrue(restoredUltra.ultra > BigNumber.ZERO)
        assertTrue(restoredUltra.prestigeUpgradeLevels.isEmpty())
        assertEquals(ultraInput.ultraUpgradeLevels, restoredUltra.ultraUpgradeLevels)
        assertEquals(ultraInput.shopPurchases, restoredUltra.shopPurchases)
        assertTrue(restoredUltra.upgradeLevels.isEmpty())
    }

    @Test
    fun bigNumberFormattingFallsBackToScientificNotationBeyondSuffixCatalog() {
        val value = BigNumber("1e100")

        assertEquals("1.00e+100", value.format("Scientific"))
        assertEquals("1.00e+100", value.format("Standard"))
        assertTrue(value.format("Engineering").endsWith("e99"))
    }
}
