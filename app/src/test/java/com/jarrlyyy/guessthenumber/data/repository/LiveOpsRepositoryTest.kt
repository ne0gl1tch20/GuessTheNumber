package com.jarrlyyy.guessthenumber.data.repository

import kotlinx.coroutines.runBlocking
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
class LiveOpsRepositoryTest {
    @Test
    fun bundledManifestHasVersionedValidEventsAndConfiguredRewards() = runBlocking {
        val manifest = LiveOpsRepository(RuntimeEnvironment.getApplication()).loadLiveOpsEvents()
        assertEquals(1, manifest.schemaVersion)
        assertEquals(2, manifest.manifestVersion)
        assertTrue(manifest.events.isNotEmpty())
        assertTrue(manifest.events.all { it.rewardNebula >= 0 && it.maxClaims > 0 })
        assertEquals(100L, manifest.events.first { it.id == "holiday_neon_rush" }.minCorrectGuesses)
    }

    @Test
    fun featureFlagsLoadFromBundledData() {
        val flags = FeatureFlagRepository(RuntimeEnvironment.getApplication()).load()
        assertTrue(flags["live_ops"] == true)
        assertTrue(flags["seasonal_rewards"] == true)
        assertTrue(flags["world_map"] == true)
    }

    @Test
    fun rejectsDuplicateOrInvalidLiveOpsEvents() {
        val event = LiveOpsEvent(
            id = "same", title = "Event", description = "Test", status = "ACTIVE",
            startDate = "2026-01-01T00:00:00Z", endDate = "2026-02-01T00:00:00Z",
            currencyName = "Nebula", maxClaims = 1, rewardNebula = 10
        )
        val duplicate = LiveOpsManifest(manifestVersion = 2, events = listOf(event, event))
        org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
            LiveOpsContentValidator.validate(duplicate)
        }
        val invalidReward = LiveOpsManifest(
            manifestVersion = 2,
            events = listOf(event.copy(id = "bad_reward", rewardNebula = -1))
        )
        org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
            LiveOpsContentValidator.validate(invalidReward)
        }
    }
}
