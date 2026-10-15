package com.jarrlyyy.guessthenumber.shared.rules

import kotlin.test.Test
import kotlin.test.assertEquals

class GuessStateRulesTest {
    @Test
    fun validRangeIncludesSingleValueAndNormalRanges() {
        assertEquals(true, GuessStateRules.isValidRange(1, 100))
        assertEquals(true, GuessStateRules.isValidRange(7, 7))
    }

    @Test
    fun reversedRangeIsInvalid() {
        assertEquals(false, GuessStateRules.isValidRange(100, 1))
    }

    @Test
    fun targetMustBeWithinAnOrderedRangeIncludingBoundaries() {
        assertEquals(true, GuessStateRules.isTargetWithinRange(1, 1, 100))
        assertEquals(true, GuessStateRules.isTargetWithinRange(100, 1, 100))
        assertEquals(false, GuessStateRules.isTargetWithinRange(0, 1, 100))
        assertEquals(false, GuessStateRules.isTargetWithinRange(101, 1, 100))
        assertEquals(false, GuessStateRules.isTargetWithinRange(5, 10, 1))
    }

    @Test
    fun correctGuessIncrementsAttemptsCorrectCountAndStreak() {
        val result = GuessStateRules.applyGuess(
            targetNumber = 42,
            guess = 42,
            rangeMin = 1,
            rangeMax = 100,
            attempts = 8,
            correctGuesses = 3,
            streak = 2,
            bestStreak = 5
        )

        assertEquals(GuessFeedback.CORRECT, result.feedback)
        assertEquals(9L, result.attempts)
        assertEquals(4L, result.correctGuesses)
        assertEquals(3, result.streak)
        assertEquals(5, result.bestStreak)
    }

    @Test
    fun correctGuessUpdatesBestStreakWhenNewRecordIsSet() {
        val result = GuessStateRules.applyGuess(
            targetNumber = 1,
            guess = 1,
            rangeMin = 1,
            rangeMax = 100,
            attempts = 0,
            correctGuesses = 0,
            streak = 6,
            bestStreak = 6
        )

        assertEquals(7, result.streak)
        assertEquals(7, result.bestStreak)
    }

    @Test
    fun highGuessIncrementsAttemptsAndResetsStreakWithoutChangingCorrectCount() {
        val result = GuessStateRules.applyGuess(
            targetNumber = 42,
            guess = 43,
            rangeMin = 1,
            rangeMax = 100,
            attempts = 2,
            correctGuesses = 1,
            streak = 3,
            bestStreak = 4
        )

        assertEquals(GuessFeedback.TOO_HIGH, result.feedback)
        assertEquals(3L, result.attempts)
        assertEquals(1L, result.correctGuesses)
        assertEquals(0, result.streak)
        assertEquals(4, result.bestStreak)
    }

    @Test
    fun wrongGuessIncrementsAttemptsAndResetsStreakWithoutChangingCorrectCount() {
        val result = GuessStateRules.applyGuess(
            targetNumber = 42,
            guess = 41,
            rangeMin = 1,
            rangeMax = 100,
            attempts = 8,
            correctGuesses = 3,
            streak = 4,
            bestStreak = 6
        )

        assertEquals(GuessFeedback.TOO_LOW, result.feedback)
        assertEquals(9L, result.attempts)
        assertEquals(3L, result.correctGuesses)
        assertEquals(0, result.streak)
        assertEquals(6, result.bestStreak)
    }
}
