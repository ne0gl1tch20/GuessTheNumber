package com.jarrlyyy.guessthenumber.shared.domain

import com.jarrlyyy.guessthenumber.shared.rules.GuessFeedback
import com.jarrlyyy.guessthenumber.shared.rules.GuessStateRules

/**
 * UI-neutral state for the small shared guessing-game pilot.
 * Production economy/save state remains in the existing platform engine for now.
 */
data class GuessGameState(
    val targetNumber: Long,
    val rangeMin: Long = 1L,
    val rangeMax: Long = 100L,
    val attempts: Long = 0L,
    val correctGuesses: Long = 0L,
    val streak: Int = 0,
    val bestStreak: Int = 0,
    val status: GuessGameStatus = GuessGameStatus.READY,
    val solved: Boolean = false
)

enum class GuessGameStatus {
    READY,
    INVALID_INPUT,
    TOO_LOW,
    TOO_HIGH,
    CORRECT
}

object GuessGameReducer {
    fun newGame(targetNumber: Long, rangeMin: Long = 1L, rangeMax: Long = 100L): GuessGameState {
        require(rangeMax >= rangeMin) { "rangeMax must be >= rangeMin" }
        require(targetNumber in rangeMin..rangeMax) { "targetNumber must be within the range" }
        return GuessGameState(
            targetNumber = targetNumber,
            rangeMin = rangeMin,
            rangeMax = rangeMax
        )
    }

    fun submitGuess(state: GuessGameState, guess: Long?): GuessGameState {
        if (state.solved) return state
        if (guess == null || guess !in state.rangeMin..state.rangeMax) {
            return state.copy(status = GuessGameStatus.INVALID_INPUT)
        }

        val transition = GuessStateRules.applyGuess(
            targetNumber = state.targetNumber,
            guess = guess,
            rangeMin = state.rangeMin,
            rangeMax = state.rangeMax,
            attempts = state.attempts,
            correctGuesses = state.correctGuesses,
            streak = state.streak,
            bestStreak = state.bestStreak
        )
        val status = when (transition.feedback) {
            GuessFeedback.TOO_LOW -> GuessGameStatus.TOO_LOW
            GuessFeedback.TOO_HIGH -> GuessGameStatus.TOO_HIGH
            GuessFeedback.CORRECT -> GuessGameStatus.CORRECT
        }
        return state.copy(
            attempts = transition.attempts,
            correctGuesses = transition.correctGuesses,
            streak = transition.streak,
            bestStreak = transition.bestStreak,
            status = status,
            solved = transition.feedback == GuessFeedback.CORRECT
        )
    }
}
