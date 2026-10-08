package com.jarrlyyy.guessthenumber.ui

import com.jarrlyyy.guessthenumber.domain.model.DEFAULT_MORE_SCREEN_ORDER
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MoreMenuOrderSmokeTest {
    @Test
    fun defaultMoreMenuOrderContainsUniqueRoutes() {
        assertEquals(DEFAULT_MORE_SCREEN_ORDER.size, DEFAULT_MORE_SCREEN_ORDER.distinct().size)
    }

    @Test
    fun defaultMoreMenuOrderContainsChangelog() {
        assertTrue(DEFAULT_MORE_SCREEN_ORDER.contains("changelog_viewer"))
    }

    @Test
    fun persistedOrderNormalizationRestoresMissingAndRejectsUnknownRoutes() {
        val saved = listOf("changelog_viewer", "not_a_real_route", "settings", "settings")
        val normalized = (saved + DEFAULT_MORE_SCREEN_ORDER)
            .distinct()
            .filter { it in DEFAULT_MORE_SCREEN_ORDER }
            .let { it + DEFAULT_MORE_SCREEN_ORDER.filterNot(it::contains) }

        assertEquals(DEFAULT_MORE_SCREEN_ORDER.toSet(), normalized.toSet())
        assertEquals(normalized.size, normalized.distinct().size)
        assertTrue(normalized.first() == "changelog_viewer")
        assertTrue(normalized[1] == "settings")
    }
}
