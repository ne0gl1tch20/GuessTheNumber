package com.jarrlyyy.guessthenumber.desktop

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DesktopCodexSearchTest {
    @Test
    fun emptyQueryReturnsEveryCatalogEntry() {
        assertEquals(12, filterDesktopCodexEntries("").size)
        assertEquals(12, filterDesktopCodexEntries("   ").size)
    }

    @Test
    fun searchMatchesTitlesCaseInsensitively() {
        val matches = filterDesktopCodexEntries("CRYSTAL")
        assertEquals(listOf("Crystal Caverns", "Crystal Golem"), matches.map { it.title })
    }

    @Test
    fun searchMatchesStableEntryIds() {
        val matches = filterDesktopCodexEntries("secret:ashen")
        assertEquals(listOf("secret:ashen_vault"), matches.map { it.id })
    }

    @Test
    fun unknownSearchReturnsNoEntries() {
        assertTrue(filterDesktopCodexEntries("not-a-codex-entry").isEmpty())
    }
}
