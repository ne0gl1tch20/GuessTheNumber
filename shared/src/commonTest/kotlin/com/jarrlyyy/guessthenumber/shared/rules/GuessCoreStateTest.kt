package com.jarrlyyy.guessthenumber.shared.rules

import kotlin.test.Test
import kotlin.test.assertEquals

class GuessCoreStateTest {
    @Test
    fun correctGuessMatchesLegacyScalarTransitionAndUpdatesCoreState() {
        val state = GuessCoreState(
            targetNumber = 42,
            rangeMin = 1,
            rangeMax = 100,
            attempts = 8,
            correctGuesses = 3,
            streak = 2,
            bestStreak = 5
        )
        val scalar = GuessStateRules.applyGuess(
            targetNumber = state.targetNumber,
            guess = 42,
            rangeMin = state.rangeMin,
            rangeMax = state.rangeMax,
            attempts = state.attempts,
            correctGuesses = state.correctGuesses,
            streak = state.streak,
            bestStreak = state.bestStreak
        )

        val result = GuessStateRules.applyGuess(state, guess = 42)

        assertEquals(scalar, result.stateTransition)
        assertEquals(
            state.copy(
                attempts = 9,
                correctGuesses = 4,
                streak = 3,
                bestStreak = 5
            ),
            result.nextState
        )
    }

    @Test
    fun incorrectGuessMatchesLegacyScalarTransitionAndPreservesTargetAndRange() {
        val state = GuessCoreState(
            targetNumber = 42,
            rangeMin = 1,
            rangeMax = 100,
            attempts = 8,
            correctGuesses = 3,
            streak = 4,
            bestStreak = 6
        )
        val scalar = GuessStateRules.applyGuess(
            targetNumber = state.targetNumber,
            guess = 43,
            rangeMin = state.rangeMin,
            rangeMax = state.rangeMax,
            attempts = state.attempts,
            correctGuesses = state.correctGuesses,
            streak = state.streak,
            bestStreak = state.bestStreak
        )

        val result = GuessStateRules.applyGuess(state, guess = 43)

        assertEquals(scalar, result.stateTransition)
        assertEquals(state.copy(attempts = 9, streak = 0), result.nextState)
        assertEquals(GuessFeedback.TOO_HIGH, result.feedback)
    }
}
