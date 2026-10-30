package com.jarrlyyy.guessthenumber.domain.engine

import com.jarrlyyy.guessthenumber.domain.model.BigNumber
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import kotlin.math.floor
import kotlin.math.log10

/**
 * BigNumber-safe Prestige and Ultra reward calculations shared by Android and Windows.
 *
 * Do not convert the currencies or requirements to Double: late-game values routinely
 * exceed Double.MAX_VALUE even though their ratio and resulting rewards are valid.
 */
internal object BigNumberProgressionRewardRules {
    private val TWO = BigDecimal("2")
    private val LOG_CONTEXT = MathContext.DECIMAL128

    fun prestigeReward(money: BigNumber, requirement: BigNumber): BigNumber {
        val ratio = ratioOrNull(money, requirement) ?: return BigNumber.ZERO
        return BigNumber(floorSquareRoot(ratio))
    }

    fun ultraReward(money: BigNumber, requirement: BigNumber): BigNumber {
        val ratio = ratioOrNull(money, requirement) ?: return BigNumber.ZERO
        val exponent = ratio.precision() - ratio.scale() - 1
        val mantissa = ratio.movePointLeft(exponent).toDouble()
        val logarithm = log10(mantissa) + exponent.toDouble()
        val reward = floor(logarithm * 100.0).coerceAtMost(2_000.0)
        return BigNumber(BigDecimal.valueOf(reward).setScale(0, RoundingMode.FLOOR))
    }

    private fun ratioOrNull(money: BigNumber, requirement: BigNumber): BigDecimal? {
        if (requirement <= BigNumber.ZERO || money < requirement) return null
        return money.value.divide(requirement.value, LOG_CONTEXT)
    }

    /**
     * Newton iteration with enough significant digits to preserve the integer part
     * of the root for the game's supported large-number range. The final exact square
     * comparisons correct a possible one-unit rounding boundary.
     */
    private fun floorSquareRoot(value: BigDecimal): BigDecimal {
        if (value.signum() <= 0) return BigDecimal.ZERO

        val exponent = value.precision() - value.scale() - 1
        val integerDigits = (Math.floorDiv(exponent, 2) + 1).coerceAtLeast(1)
        val precision = (integerDigits + 24).coerceAtMost(4_096)
        val context = MathContext(precision, RoundingMode.HALF_EVEN)
        var estimate = BigDecimal.ONE.scaleByPowerOfTen(Math.floorDiv(exponent, 2))

        repeat(precision.coerceAtMost(256)) {
            val next = estimate.add(value.divide(estimate, context), context)
                .divide(TWO, context)
            if (next.compareTo(estimate) == 0) return@repeat
            estimate = next
        }

        var candidate = estimate.setScale(0, RoundingMode.FLOOR)
        while (candidate.multiply(candidate) > value) {
            candidate = candidate.subtract(BigDecimal.ONE)
        }
        while (candidate.add(BigDecimal.ONE).pow(2) <= value) {
            candidate = candidate.add(BigDecimal.ONE)
        }
        return candidate
    }
}
