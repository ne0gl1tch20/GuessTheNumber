package com.jarrlyyy.guessthenumber.shared.domain

/**
 * Shared policy for the wrong-guess tax mutator.
 * Monetary arithmetic stays in the platform engine to preserve BigNumber behavior.
 */
object GuessPenaltyRules {
    const val TAX_PENALTY_RATE = 0.05

    fun taxPenaltyRate(activeMutators: Set<String>): Double =
        if (MutatorRules.TAX in MutatorRules.validate(activeMutators)) TAX_PENALTY_RATE else 0.0
}
