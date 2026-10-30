package com.jarrlyyy.guessthenumber.domain.engine

import com.jarrlyyy.guessthenumber.domain.model.BigNumber
import java.math.BigDecimal
import java.math.BigInteger
import java.math.MathContext
import java.math.RoundingMode

/**
 * BigNumber-safe Prestige and Ultra reward calculations shared by Android and Windows.
 *
 * Do not convert currencies or requirements to Double: late-game values can exceed
 * Double.MAX_VALUE even though their ratios and resulting rewards are valid.
 */
internal object BigNumberProgressionRewardRules {
    private val LOG_CONTEXT = MathContext.DECIMAL128
    private val LOG_PRECISION = MathContext(50, RoundingMode.HALF_EVEN)
    private val TWO = BigDecimal("2")
    private val ONE = BigDecimal.ONE
    private val TEN = BigInteger.TEN
    private val LOG_EPSILON = BigDecimal("1e-48")
    private val LN_TEN = naturalLog(BigDecimal.TEN)

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

        val mantissa = ratio.movePointLeft(exponent)
        val logarithm = naturalLog(mantissa).divide(LN_TEN, LOG_PRECISION)
        val scaledReward = BigDecimal.valueOf(exponent.toLong())
            .add(logarithm, LOG_PRECISION)
            .multiply(BigDecimal("100"), LOG_PRECISION)
        val reward = scaledReward.setScale(0, RoundingMode.FLOOR)
            .min(BigDecimal("2000"))
        return BigNumber(reward)
    }

    /**
     * Natural logarithm for positive values in [1, 10), using
     * ln(x) = 2 * (z + z^3/3 + z^5/5 + ...), z = (x - 1)/(x + 1).
     * This avoids rounding a value just below an integer reward boundary up.
     */
    private fun naturalLog(value: BigDecimal): BigDecimal {
        if (value.compareTo(ONE) == 0) return BigDecimal.ZERO
        val z = value.subtract(ONE, LOG_PRECISION)
            .divide(value.add(ONE, LOG_PRECISION), LOG_PRECISION)
        val zSquared = z.multiply(z, LOG_PRECISION)
        var term = z
        var sum = z
        var divisor = 3L

        repeat(512) {
            term = term.multiply(zSquared, LOG_PRECISION)
            val contribution = term.divide(BigDecimal.valueOf(divisor), LOG_PRECISION)
            if (contribution.abs() < LOG_EPSILON) {
                return sum.multiply(TWO, LOG_PRECISION)
            }
            sum = sum.add(contribution, LOG_PRECISION)
            divisor += 2L
        }
        return sum.multiply(TWO, LOG_PRECISION)
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
