package com.jarrlyyy.guessthenumber.domain.engine

import com.jarrlyyy.guessthenumber.domain.model.GameState
import com.jarrlyyy.guessthenumber.shared.rules.GuessFeedback
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NativeGameplayOptimizerIntegrationTest {
    @Test
    fun correctGuessUsesBoundedNextTargetAndSavedDifficultyRange() {
        val engine = GameEngine(Random(1234))
        val state = GameState(
            difficultyId = "hard",
            targetNumber = 50L,
            currentRangeMin = 1L,
            currentRangeMax = 100L,
            streak = 7,
            attempts = 3L
        )

        val result = engine.processGuess(state, 50L)

        assertEquals(GuessFeedback.CORRECT, result.feedback)
        assertEquals(engine.getRangeMax(state), result.newState.currentRangeMax)
        assertTrue(
            "The optimized target must stay inside the authoritative next range",
            result.newState.targetNumber in result.newState.currentRangeMin..result.newState.currentRangeMax
        )
    }
}
