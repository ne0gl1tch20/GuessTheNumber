package com.jarrlyyy.guessthenumber.shared.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class ProgressionRewardRulesTest {
    @Test
    fun prestigeRewardRequiresThresholdAndUsesSquareRootRatio() {
        assertEquals(0.0, ProgressionRewardRules.prestigeRewardRatio(99.0, 100.0))
        assertEquals(1.0, ProgressionRewardRules.prestigeRewardRatio(100.0, 100.0))
        assertEquals(2.0, ProgressionRewardRules.prestigeRewardRatio(400.0, 100.0))
    }

    @Test
    fun ultraRewardRequiresThresholdAndUsesLogarithmicRatio() {
        assertEquals(0.0, ProgressionRewardRules.ultraRewardRatio(99.0, 100.0))
        assertEquals(0.0, ProgressionRewardRules.ultraRewardRatio(100.0, 100.0))
        assertEquals(100.0, ProgressionRewardRules.ultraRewardRatio(1_000.0, 100.0))
    }

    @Test
    fun ultraRewardIsCappedAtTwoThousand() {
        assertEquals(2000.0, ProgressionRewardRules.ultraRewardRatio(1.0e30, 1.0))
    }
}
