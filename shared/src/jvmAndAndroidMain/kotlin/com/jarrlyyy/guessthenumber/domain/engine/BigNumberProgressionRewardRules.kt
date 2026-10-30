package com.jarrlyyy.guessthenumber.domain.engine

import com.jarrlyyy.guessthenumber.domain.model.BigNumber
import java.math.BigDecimal
import java.math.BigInteger
import java.math.MathContext
import java.math.RoundingMode
import kotlin.math.floor
import kotlin.math.log10

/**
 * BigNumber-safe Prestige and Ultra reward calculations shared by Android and Windows.
 *
 * Do not convert currencies or requirements to Double: late-game values can exceed
 * Double.MAX_VALUE even though their ratios and resulting rewards are valid.
 */
internal object BigNumberProgressionRewardRules {
    private val LOG_CONTEXT = MathContext.DECIMAL128
    private val TEN = BigInteger.TEN

    fun prestigeReward(money: BigNumber, requirement: BigNumber): BigNumber {
        if (requirement <= BigNumber.ZERO || money < requirement) return BigNumber.ZERO
        val numerator = money.value.unscaledValue()
        val denominator = requirement.value.unscaledValue()
        val scaleDelta = requirement.value.scale() - money.value.scale()
        val scaledNumerator: BigInteger
        val scaledDenominator: BigInteger
        if (scaleDelta >= 0) {
            scaledNumerator = numerator.multiply(TEN.pow(scaleDelta))
            scaledDenominator = denominator
        } else {
            scaledNumerator = numerator
            scaledDenominator = denominator.multiply(TEN.pow(-scaleDelta))
        }
        val integerRatio = scaledNumerator.divide(scaledDenominator)
        return BigNumber(BigDecimal(floorSquareRoot(integerRatio)))
    }

    fun ultraReward(money: BigNumber, requirement: BigNumber): BigNumber {
        if (requirement <= BigNumber.ZERO || money < requirement) return BigNumber.ZERO
        val ratio = money.value.divide(requirement.value, LOG_CONTEXT)
        val exponent = ratio.precision() - ratio.scale() - 1
        if (exponent >= 20) return BigNumber(2_000)

        val mantissa = ratio.movePointLeft(exponent).toDouble()
        val logarithm = log10(mantissa) + exponent.toDouble()
        val reward = floor(logarithm * 100.0).coerceIn(0.0, 2_000.0)
        return BigNumber(BigDecimal.valueOf(reward).setScale(0, RoundingMode.FLOOR))
    }

    /** Exact integer square root, so values just below a square never round up. */
    private fun floorSquareRoot(value: BigInteger): BigInteger {
        if (value.signum() <= 0) return BigInteger.ZERO
        var estimate = BigInteger.ONE.shiftLeft((value.bitLength() + 1) / 2)
        while (true) {
            val next = estimate.add(value.divide(estimate)).shiftRight(1)
            if (next >= estimate) return estimate
            estimate = next
        }
    }
}
