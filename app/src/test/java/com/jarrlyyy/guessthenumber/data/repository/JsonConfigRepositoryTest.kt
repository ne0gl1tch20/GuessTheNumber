package com.jarrlyyy.guessthenumber.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class JsonConfigRepositoryTest {
    private fun repository() = JsonConfigRepository(RuntimeEnvironment.getApplication())

    @Test
    fun versionedBundledGameConfigurationLoads() {
        val config = repository().loadGameConfig()
        assertEquals(1L, config.startingRangeMin)
        assertEquals(100L, config.startingRangeMax)
        assertTrue(config.criticalChance in 0.0..1.0)
    }

    @Test
    fun bundledContentPassesValidationAndLoads() {
        val repo = repository()
        assertFalse(repo.loadUpgrades().isEmpty())
        assertFalse(repo.loadPrestigeUpgrades().isEmpty())
        assertFalse(repo.loadUltraUpgrades().isEmpty())
        assertFalse(repo.loadShopItems().isEmpty())
        assertFalse(repo.loadPrestigeShopItems().isEmpty())
        assertFalse(repo.loadUltraShopItems().isEmpty())
        assertTrue(repo.loadAchievements().size >= 20)
        assertFalse(repo.loadMinigames().isEmpty())
        assertFalse(repo.loadChallenges().isEmpty())
        assertFalse(repo.loadTalents().isEmpty())
        val feedback = repo.loadFeedbackMessages()
        assertEquals(30, feedback.tooLowMessages.size)
        assertEquals(30, feedback.tooHighMessages.size)
        assertEquals(55, feedback.guessTipsAndClues.size)
    }
}
