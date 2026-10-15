package com.jarrlyyy.guessthenumber.shared.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class GuessPenaltyRulesTest {
    @Test
    fun taxPenaltyOnlyAppliesWhenTaxMutatorIsActive() {
        assertEquals(0.0, GuessPenaltyRules.taxPenaltyRate(emptySet()), 1e-9)
        assertEquals(GuessPenaltyRules.TAX_PENALTY_RATE, GuessPenaltyRules.taxPenaltyRate(setOf(MutatorRules.TAX)), 1e-9)
    }

    @Test
    fun unknownMutatorsDoNotActivateTaxPenalty() {
        assertEquals(0.0, GuessPenaltyRules.taxPenaltyRate(setOf("unknown_mutator")), 1e-9)
    }

    @Test
    fun taxPenaltyUsesExistingFivePercentRate() {
        assertEquals(0.05, GuessPenaltyRules.TAX_PENALTY_RATE, 1e-9)
    }
}
