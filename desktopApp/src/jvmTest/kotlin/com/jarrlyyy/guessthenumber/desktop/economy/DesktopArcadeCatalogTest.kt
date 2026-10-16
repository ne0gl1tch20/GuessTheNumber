package com.jarrlyyy.guessthenumber.desktop.economy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DesktopArcadeCatalogTest {
    @Test
    fun loadsTheCanonicalAndroidMinigameCatalogInOrder() {
        val games = DesktopEconomyRepository().minigames()

        assertEquals(
            listOf("quick_guess", "reaction_test", "lucky_number", "memory_match", "number_rush"),
            games.map { it.id }
        )
        assertEquals("Quick Guess", games.first().name)
        assertTrue(games.all { it.rewardMultiplier > 0.0 })
    }

    @Test
    fun rejectsUnsupportedMinigameCatalogSchemas() {
        val invalid = """{"schemaVersion":2,"minigames":[{"id":"test","name":"Test","description":"Test","rewardMultiplier":1.0}]}"""
        var rejected = false
        try {
            DesktopEconomyJson.minigames(invalid)
        } catch (_: IllegalArgumentException) {
            rejected = true
        }
        assertTrue(rejected)
    }

    @Test
    fun rejectsDuplicateMinigameIds() {
        val invalid = """{"schemaVersion":1,"minigames":[{"id":"same","name":"One","description":"One","rewardMultiplier":1.0},{"id":"same","name":"Two","description":"Two","rewardMultiplier":2.0}]}"""
        var rejected = false
        try {
            DesktopEconomyJson.minigames(invalid)
        } catch (_: IllegalArgumentException) {
            rejected = true
        }
        assertTrue(rejected)
    }
}
