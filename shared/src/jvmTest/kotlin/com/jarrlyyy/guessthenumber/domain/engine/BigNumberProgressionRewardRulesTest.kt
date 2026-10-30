package com.jarrlyyy.guessthenumber.domain.engine

import com.jarrlyyy.guessthenumber.domain.model.BigNumber
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
    fun ultraRewardUsesRatioExponentWithoutConvertingHugeCurrenciesToDouble() {
        val requirement = engine.calculateUltraRequirement(0)
        val moneyForThousand = requirement * BigNumber("1e10")
        val moneyBeyondCap = requirement * BigNumber("1e100")

        assertEquals(0, engine.calculateUltraReward(moneyForThousand, 0).compareTo(BigNumber(1_000)))
        assertEquals(0, engine.calculateUltraReward(moneyBeyondCap, 0).compareTo(BigNumber(2_000)))
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
    fun bigNumberFormattingFallsBackToScientificNotationBeyondSuffixCatalog() {
        val value = BigNumber("1e100")

        assertEquals("1.00e+100", value.format("Scientific"))
        assertEquals("1.00e+100", value.format("Standard"))
        assertTrue(value.format("Engineering").endsWith("e99"))
    }
}
