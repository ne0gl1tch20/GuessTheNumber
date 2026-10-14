package com.jarrlyyy.guessthenumber.shared.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DifficultyRulesTest {
    @Test fun exposesSupportedDifficulties() {
        assertEquals(listOf("classic", "hard", "extreme"), DifficultyRules.ids)
        assertTrue(DifficultyRules.isValid("hard"))
        assertFalse(DifficultyRules.isValid("unknown"))
    }

    @Test fun difficultyRangesAndRewardsStayConsistent() {
        assertEquals(100L, DifficultyRules.baseRangeMax("classic"))
        assertEquals(150L, DifficultyRules.baseRangeMax("hard"))
        assertEquals(250L, DifficultyRules.baseRangeMax("extreme"))
        assertEquals(50L, DifficultyRules.rangeMax("classic", 100))
        assertEquals(2.5, DifficultyRules.rewardMultiplier("extreme"))
        assertEquals(0.05, DifficultyRules.wrongGuessPenalty("extreme"))
    }
}

class SaveProfileRulesTest {
    @Test fun validatesAndMapsProfileIcons() {
        assertTrue(SaveProfileRules.isValidIcon("planet"))
        assertFalse(SaveProfileRules.isValidIcon("missing"))
        assertEquals("👑", SaveProfileRules.iconSymbol("crown"))
        assertEquals("🎮", SaveProfileRules.iconSymbol("missing"))
    }
}
