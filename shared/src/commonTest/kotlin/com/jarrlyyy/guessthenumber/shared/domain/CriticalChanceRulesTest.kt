package com.jarrlyyy.guessthenumber.shared.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class CriticalChanceRulesTest {
    @Test
    fun keepsExistingBaseCriticalChance() {
        assertEquals(0.05, CriticalChanceRules.chance(0), 1e-9)
    }

    @Test
    fun addsOnePercentagePointPerUpgradeLevel() {
        assertEquals(0.06, CriticalChanceRules.chance(1), 1e-9)
        assertEquals(0.15, CriticalChanceRules.chance(10), 1e-9)
    }

    @Test
    fun preservesExistingFormulaForNegativeLevels() {
        assertEquals(0.04, CriticalChanceRules.chance(-1), 1e-9)
    }
}
