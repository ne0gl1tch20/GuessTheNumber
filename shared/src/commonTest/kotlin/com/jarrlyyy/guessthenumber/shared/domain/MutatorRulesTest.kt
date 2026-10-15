package com.jarrlyyy.guessthenumber.shared.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class MutatorRulesTest {
    @Test
    fun validateKeepsOnlySupportedMutators() {
        assertEquals(
            setOf(MutatorRules.HARDCORE, MutatorRules.TAX),
            MutatorRules.validate(setOf(MutatorRules.HARDCORE, MutatorRules.TAX, "unknown"))
        )
    }

    @Test
    fun noMutatorsHaveNeutralMultiplier() {
        assertEquals(1.0, MutatorRules.rewardMultiplier(emptySet()))
    }

    @Test
    fun eachMutatorUsesTheExistingProductionMultiplier() {
        assertEquals(2.0, MutatorRules.rewardMultiplier(setOf(MutatorRules.HARDCORE)))
        assertEquals(3.0, MutatorRules.rewardMultiplier(setOf(MutatorRules.SPEED)))
        assertEquals(5.0, MutatorRules.rewardMultiplier(setOf(MutatorRules.BLIND)))
        assertEquals(10.0, MutatorRules.rewardMultiplier(setOf(MutatorRules.TAX)))
    }

    @Test
    fun combinedMutatorsMultiplyInTheProductionOrder() {
        assertEquals(
            300.0,
            MutatorRules.rewardMultiplier(
                setOf(MutatorRules.HARDCORE, MutatorRules.SPEED, MutatorRules.BLIND, MutatorRules.TAX)
            )
        )
    }

    @Test
    fun unsupportedMutatorsDoNotAffectRewards() {
        assertEquals(1.0, MutatorRules.rewardMultiplier(setOf("unknown")))
    }

    @Test
    fun rangeMultiplierPreservesHardcoreRangeDoubling() {
        assertEquals(1L, MutatorRules.rangeMultiplier(emptySet()))
        assertEquals(2L, MutatorRules.rangeMultiplier(setOf(MutatorRules.HARDCORE)))
        assertEquals(1L, MutatorRules.rangeMultiplier(setOf(MutatorRules.SPEED, MutatorRules.TAX)))
        assertEquals(2L, MutatorRules.rangeMultiplier(setOf(MutatorRules.HARDCORE, "unknown")))
    }
}
