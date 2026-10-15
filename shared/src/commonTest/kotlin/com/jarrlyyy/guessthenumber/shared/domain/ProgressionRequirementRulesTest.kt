package com.jarrlyyy.guessthenumber.shared.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class ProgressionRequirementRulesTest {
    @Test
    fun prestigeRequirementsKeepExistingBaseAndGrowth() {
        assertEquals(50_000_000.0, ProgressionRequirementRules.prestigeRequirement(0))
        assertEquals(125_000_000.0, ProgressionRequirementRules.prestigeRequirement(1))
        assertEquals(312_500_000.0, ProgressionRequirementRules.prestigeRequirement(2))
    }

    @Test
    fun ultraRequirementsKeepExistingBaseAndGrowth() {
        assertEquals(10_000_000_000.0, ProgressionRequirementRules.ultraRequirement(0))
        assertEquals(50_000_000_000.0, ProgressionRequirementRules.ultraRequirement(1))
        assertEquals(250_000_000_000.0, ProgressionRequirementRules.ultraRequirement(2))
    }
}
