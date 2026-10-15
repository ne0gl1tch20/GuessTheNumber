package com.jarrlyyy.guessthenumber.shared.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class FrenzyRulesTest {
    @Test
    fun streakBelowThresholdHasNoFrenzyBonus() {
        assertEquals(1.0, FrenzyRules.multiplier(0))
        assertEquals(1.0, FrenzyRules.multiplier(4))
    }

    @Test
    fun eachThresholdAddsOneQuarterMultiplier() {
        assertEquals(1.25, FrenzyRules.multiplier(5))
        assertEquals(1.5, FrenzyRules.multiplier(10))
        assertEquals(1.75, FrenzyRules.multiplier(15))
    }

    @Test
    fun frenzyMultiplierIsCappedAtFive() {
        assertEquals(5.0, FrenzyRules.multiplier(80))
        assertEquals(5.0, FrenzyRules.multiplier(500))
    }
}
