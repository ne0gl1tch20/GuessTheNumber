package com.jarrlyyy.guessthenumber.shared.engine

import com.jarrlyyy.guessthenumber.shared.domain.BigNumber
import com.jarrlyyy.guessthenumber.shared.domain.GameState
import com.jarrlyyy.guessthenumber.shared.rules.GuessFeedback
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GameEngineTest {
    @Test
    fun correctGuessUpdatesProgressionAndReward() {
        val engine = GameEngine(Random(7))
        val initial = GameState(targetNumber = 50, currentRangeMin = 1, currentRangeMax = 100)

        val result = engine.processGuess(initial, 50)

        assertEquals(GuessFeedback.CORRECT, result.feedback)
        assertEquals(1L, result.newState.attempts)
        assertEquals(1L, result.newState.correctGuesses)
        assertEquals(1, result.newState.streak)
        assertTrue(result.reward >= BigNumber(10_000))
        assertEquals(result.reward, result.newState.money)
    }

    @Test
    fun wrongGuessAppliesDifficultyPenaltyAndResetsStreak() {
        val engine = GameEngine(Random(7))
        val initial = GameState(
            difficultyId = "hard",
            money = BigNumber(1_000),
            targetNumber = 50,
            currentRangeMin = 1,
            currentRangeMax = 100,
            streak = 4
        )

        val result = engine.processGuess(initial, 49)

        assertEquals(GuessFeedback.TOO_LOW, result.feedback)
        assertEquals(BigNumber(980), result.newState.money)
        assertEquals(1L, result.newState.attempts)
        assertEquals(0, result.newState.streak)
        assertEquals(BigNumber.ZERO, result.reward)
    }

    @Test
    fun mutatorValidationRejectsUnknownIds() {
        val engine = GameEngine(Random(7))
        assertEquals(setOf("mut_tax"), engine.validateMutators(setOf("mut_tax", "untrusted")))
    }
}
