package com.jarrlyyy.guessthenumber.shared.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class GuessGameReducerTest {
    @Test
    fun invalidGuessDoesNotIncrementAttempts() {
        val initial = GuessGameReducer.newGame(targetNumber = 50)

        val result = GuessGameReducer.submitGuess(initial, 101L)

        assertEquals(0L, result.attempts)
        assertEquals(GuessGameStatus.INVALID_INPUT, result.status)
        assertFalse(result.solved)
    }

    @Test
    fun lowAndHighGuessesUpdateSharedState() {
        val initial = GuessGameReducer.newGame(targetNumber = 50)

        val low = GuessGameReducer.submitGuess(initial, 20L)
        val high = GuessGameReducer.submitGuess(low, 80L)

        assertEquals(1L, low.attempts)
        assertEquals(0, low.streak)
        assertEquals(2L, high.attempts)
        assertEquals(GuessGameStatus.TOO_HIGH, high.status)
        assertEquals(0L, high.correctGuesses)
    }

    @Test
    fun correctGuessMarksGameSolvedAndFurtherGuessesAreIgnored() {
        val initial = GuessGameReducer.newGame(targetNumber = 50)
        val solved = GuessGameReducer.submitGuess(initial, 50L)

        assertTrue(solved.solved)
        assertEquals(1L, solved.attempts)
        assertEquals(1L, solved.correctGuesses)
        assertEquals(1, solved.streak)
        assertEquals(GuessGameStatus.CORRECT, solved.status)
        assertSame(solved, GuessGameReducer.submitGuess(solved, 20L))
    }

    @Test
    fun newGameResetsAllCountersAndValidatesTarget() {
        val initial = GuessGameReducer.newGame(targetNumber = 50)
        val solved = GuessGameReducer.submitGuess(initial, 50L)
        val next = GuessGameReducer.newGame(targetNumber = 12)

        assertEquals(0L, next.attempts)
        assertEquals(0L, next.correctGuesses)
        assertEquals(0, next.streak)
        assertFalse(next.solved)
        assertEquals(GuessGameStatus.READY, next.status)
    }
}
