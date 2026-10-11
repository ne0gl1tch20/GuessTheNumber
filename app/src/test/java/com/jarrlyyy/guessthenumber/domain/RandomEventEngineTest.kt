package com.jarrlyyy.guessthenumber.domain

import com.jarrlyyy.guessthenumber.domain.model.GameState
import com.jarrlyyy.guessthenumber.domain.model.RandomEventEngine
import com.jarrlyyy.guessthenumber.domain.model.RandomEventType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RandomEventEngineTest {

    @Test
    fun everyLuaApprovedEventTypeAppliesAValidatedStateChange() {
        val engine = RandomEventEngine()
        val initial = GameState()

        RandomEventType.values().forEach { type ->
            val result = engine.apply(initial, type)

            assertEquals(type, result.type)
            assertEquals(1L, result.state.statistics.randomEventsTriggered)
            assertEquals(type.name.lowercase(), result.state.lastRandomEventId)
            assertNotNull(result.state)
        }
    }

    @Test
    fun eventRewardsRemainBoundedByKotlinEventHandlers() {
        val engine = RandomEventEngine()
        val initial = GameState()

        val cosmicGift = engine.apply(initial, RandomEventType.COSMIC_GIFT)
        assertTrue(cosmicGift.state.nebula > initial.nebula)

        val streakSurge = engine.apply(initial, RandomEventType.STREAK_SURGE)
        assertEquals(initial.streak + 2, streakSurge.state.streak)
        assertTrue(streakSurge.state.bestStreak >= streakSurge.state.streak)
    }
}
