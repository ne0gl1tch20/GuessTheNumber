package com.jarrlyyy.guessthenumber.shared.rules

import kotlin.test.Test
import kotlin.test.assertEquals

class GuessRulesTest {
    @Test
    fun guessBelowTargetIsTooLow() {
        assertEquals(
            GuessFeedback.TOO_LOW,
            GuessRules.resolveFeedback(targetNumber = 50, guess = 49, rangeMin = 1, rangeMax = 100)
        )
    }

    @Test
    fun guessAboveTargetIsTooHigh() {
        assertEquals(
            GuessFeedback.TOO_HIGH,
            GuessRules.resolveFeedback(targetNumber = 50, guess = 51, rangeMin = 1, rangeMax = 100)
        )
    }

    @Test
    fun exactTargetIsCorrect() {
        assertEquals(
            GuessFeedback.CORRECT,
            GuessRules.resolveFeedback(targetNumber = 50, guess = 50, rangeMin = 1, rangeMax = 100)
        )
    }

    @Test
    fun guessBelowRangeIsClampedToMinimumBeforeComparison() {
        assertEquals(
            GuessFeedback.TOO_LOW,
            GuessRules.resolveFeedback(targetNumber = 50, guess = -10, rangeMin = 1, rangeMax = 100)
        )
    }

    @Test
    fun guessAboveRangeIsClampedToMaximumBeforeComparison() {
        assertEquals(
            GuessFeedback.TOO_HIGH,
            GuessRules.resolveFeedback(targetNumber = 50, guess = 150, rangeMin = 1, rangeMax = 100)
        )
    }

    @Test
    fun boundaryTargetAtMinimumCanBeCorrect() {
        assertEquals(
            GuessFeedback.CORRECT,
            GuessRules.resolveFeedback(targetNumber = 1, guess = 1, rangeMin = 1, rangeMax = 100)
        )
    }

    @Test
    fun boundaryTargetAtMaximumCanBeCorrect() {
        assertEquals(
            GuessFeedback.CORRECT,
            GuessRules.resolveFeedback(targetNumber = 100, guess = 100, rangeMin = 1, rangeMax = 100)
        )
    }
}
