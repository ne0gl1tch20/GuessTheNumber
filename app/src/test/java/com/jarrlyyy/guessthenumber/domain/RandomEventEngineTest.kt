package com.jarrlyyy.guessthenumber.domain

import com.jarrlyyy.guessthenumber.domain.model.BigNumber
import com.jarrlyyy.guessthenumber.domain.model.GameState
import com.jarrlyyy.guessthenumber.domain.model.RandomEventEngine
import kotlin.random.Random
import org.junit.Assert.assertTrue
import org.junit.Test

class RandomEventEngineTest {

    @Test
    fun randomEventsAreOccasionalAndProduceStateChanges() {
        val engine = RandomEventEngine(Random(1234))
        val state = GameState(money = BigNumber(10_000), streak = 3)
        var triggered = 0

        repeat(100) {
            val result = engine.roll(state)
            if (result != null) {
                triggered++
                assertTrue(result.state.statistics.randomEventsTriggered > 0)
            }
        }

        assertTrue(triggered > 0)
        assertTrue(triggered < 100)
    }

    @Test
    fun randomEventEngineSupportsVariedEventTypes() {
        val engine = RandomEventEngine(Random(9876))
        val state = GameState(money = BigNumber(10_000), currentRangeMax = 100)

        val types = buildSet {
            repeat(500) {
                engine.roll(state)?.let { add(it.type) }
            }
        }

        assertTrue(types.size >= 4)
    }
}
