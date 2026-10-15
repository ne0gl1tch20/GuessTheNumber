package com.jarrlyyy.guessthenumber.shared.rules

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class GuessTransitionRulesTest {
    @Test
    fun correctGuessUsesInjectedCriticalRollAndNextTarget() {
        val state = GuessStateRules.applyGuess(
            targetNumber = 42,
            guess = 42,
            rangeMin = 1,
            rangeMax = 100,
            attempts = 3,
            correctGuesses = 2,
            streak = 4,
            bestStreak = 4
        )

        val result = GuessTransitionRules.resolve(
            stateTransition = state,
            criticalChance = 0.25,
            criticalRoll = 0.249,
            nextTarget = 73,
            nextRangeMin = 1,
            nextRangeMax = 100
        )

        assertEquals(state, result.stateTransition)
        assertEquals(true, result.isCritical)
        assertEquals(73L, result.nextTarget)
    }

    @Test
    fun criticalRollAtThresholdIsNotCritical() {
        val state = GuessStateRules.applyGuess(
            targetNumber = 42,
            guess = 42,
            rangeMin = 1,
            rangeMax = 100,
            attempts = 0,
            correctGuesses = 0,
            streak = 0,
            bestStreak = 0
        )

        val result = GuessTransitionRules.resolve(
            stateTransition = state,
            criticalChance = 0.25,
            criticalRoll = 0.25,
            nextTarget = 1,
            nextRangeMin = 1,
            nextRangeMax = 100
        )

        assertEquals(false, result.isCritical)
        assertEquals(1L, result.nextTarget)
    }

    @Test
    fun correctGuessRejectsMissingOrOutOfRangeNextTarget() {
        val state = GuessStateRules.applyGuess(
            targetNumber = 42,
            guess = 42,
            rangeMin = 1,
            rangeMax = 100,
            attempts = 0,
            correctGuesses = 0,
            streak = 0,
            bestStreak = 0
        )

        assertFailsWith<IllegalArgumentException> {
            GuessTransitionRules.resolve(
                stateTransition = state,
                criticalChance = 0.05,
                criticalRoll = 0.0,
                nextTarget = null,
                nextRangeMin = 1,
                nextRangeMax = 100
            )
        }
        assertFailsWith<IllegalArgumentException> {
            GuessTransitionRules.resolve(
                stateTransition = state,
                criticalChance = 0.05,
                criticalRoll = 0.0,
                nextTarget = 101,
                nextRangeMin = 1,
                nextRangeMax = 100
            )
        }
    }

    @Test
    fun incorrectGuessDoesNotRequireRandomInputs() {
        val state = GuessStateRules.applyGuess(
            targetNumber = 42,
            guess = 41,
            rangeMin = 1,
            rangeMax = 100,
            attempts = 7,
            correctGuesses = 2,
            streak = 3,
            bestStreak = 5
        )

        val result = GuessTransitionRules.resolve(
            stateTransition = state,
            criticalChance = 0.05,
            criticalRoll = null,
            nextTarget = null,
            nextRangeMin = 1,
            nextRangeMax = 100
        )

        assertEquals(state, result.stateTransition)
        assertEquals(false, result.isCritical)
        assertEquals(null, result.nextTarget)
    }
}
