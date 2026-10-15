package com.jarrlyyy.guessthenumber.shared.domain

/**
 * Platform-independent mutator identifiers and reward balancing.
 *
 * Keep validation and multiplier ordering shared so Android and Windows
 * cannot accept different mutator sets or calculate different multipliers.
 */
object MutatorRules {
    const val HARDCORE = "mut_hardcore"
    const val SPEED = "mut_speed"
    const val BLIND = "mut_blind"
    const val TAX = "mut_tax"

    val ids: Set<String> = setOf(HARDCORE, SPEED, BLIND, TAX)

    fun validate(mutators: Set<String>): Set<String> = mutators.intersect(ids)

    fun rewardMultiplier(mutators: Set<String>): Double {
        val active = validate(mutators)
        var multiplier = 1.0
        if (HARDCORE in active) multiplier *= 2.0
        if (SPEED in active) multiplier *= 3.0
        if (BLIND in active) multiplier *= 5.0
        if (TAX in active) multiplier *= 10.0
        return multiplier
    }
}
