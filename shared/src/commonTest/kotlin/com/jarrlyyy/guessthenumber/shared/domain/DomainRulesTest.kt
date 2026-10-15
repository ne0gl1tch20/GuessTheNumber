package com.jarrlyyy.guessthenumber.shared.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SaveProfileRulesTest {
    @Test
    fun validatesAndMapsProfileIcons() {
        assertTrue(SaveProfileRules.isValidIcon("planet"))
        assertFalse(SaveProfileRules.isValidIcon("missing"))
        assertEquals("👑", SaveProfileRules.iconSymbol("crown"))
        assertEquals("🎮", SaveProfileRules.iconSymbol("missing"))
    }
}
