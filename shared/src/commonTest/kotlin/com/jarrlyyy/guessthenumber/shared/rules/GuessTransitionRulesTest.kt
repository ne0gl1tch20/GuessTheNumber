package com.jarrlyyy.guessthenumber.shared.rules

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class GuessTransitionRulesTest {
    @Test
    fun correctGuessUsesInjectedCriticalRollAndReturnsCompleteNextState() {
        val initial = coreState(target = 42, attempts = 3, correct = 2, streak = 4, best = 4)
        val transition = GuessStateRules.applyGuess(state = initial, guess = 42)

        val result = GuessTransitionRules.resolve(
            stateTransition = transition.stateTransition,
            currentState = transition.nextState,
            criticalChance = 0.25,
            criticalRoll = 0.249,
            nextTarget = 73,
            nextRangeMin = 1,
            nextRangeMax = 100
        )

        assertEquals(transition.stateTransition, result.stateTransition)
        assertEquals(transition.nextState.copy(targetNumber = 73), result.nextState)
        assertEquals(true, result.isCritical)
        assertEquals(73L, result.nextTarget)
    }

    @Test
    fun criticalRollAtThresholdIsNotCritical() {
        val transition = GuessStateRules.applyGuess(
            state = coreState(target = 42),
            guess = 42
        )

        val result = GuessTransitionRules.resolve(
            stateTransition = transition.stateTransition,
            currentState = transition.nextState,
            criticalChance = 0.25,
            criticalRoll = 0.25,
            nextTarget = 1,
            nextRangeMin = 1,
            nextRangeMax = 100
        )

        assertEquals(false, result.isCritical)
        assertEquals(1L, result.nextState.targetNumber)
        assertEquals(1L, result.nextTarget)
    }

    @Test
    fun correctGuessRejectsMissingOrOutOfRangeNextTarget() {
        val transition = GuessStateRules.applyGuess(
            state = coreState(target = 42),
            guess = 42
        )

        assertFailsWith<IllegalArgumentException> {
            GuessTransitionRules.resolve(
                stateTransition = transition.stateTransition,
                currentState = transition.nextState,
                criticalChance = 0.05,
                criticalRoll = 0.0,
                nextTarget = null,
                nextRangeMin = 1,
                nextRangeMax = 100
            )
        }
        assertFailsWith<IllegalArgumentException> {
            GuessTransitionRules.resolve(
                stateTransition = transition.stateTransition,
                currentState = transition.nextState,
                criticalChance = 0.05,
                criticalRoll = 0.0,
                nextTarget = 101,
                nextRangeMin = 1,
                nextRangeMax = 100
            )
        }
    }

    @Test
    fun incorrectGuessDoesNotRequireRandomInputsAndPreservesTargetAndRange() {
        val initial = coreState(target = 42, attempts = 7, correct = 2, streak = 3, best = 5)
        val transition = GuessStateRules.applyGuess(state = initial, guess = 41)

        val result = GuessTransitionRules.resolve(
            stateTransition = transition.stateTransition,
            currentState = transition.nextState,
            criticalChance = 0.05,
            criticalRoll = null,
            nextTarget = null,
            nextRangeMin = 1,
            nextRangeMax = 100
        )

        assertEquals(transition.stateTransition, result.stateTransition)
        assertEquals(transition.nextState, result.nextState)
        assertEquals(42L, result.nextState.targetNumber)
        assertEquals(1L, result.nextState.rangeMin)
        assertEquals(100L, result.nextState.rangeMax)
        assertEquals(false, result.isCritical)
        assertEquals(null, result.nextTarget)
    }

    private fun coreState(
        target: Long,
        attempts: Long = 0,
        correct: Long = 0,
        streak: Int = 0,
        best: Int = 0
    ) = GuessCoreState(
        targetNumber = target,
        rangeMin = 1,
        rangeMax = 100,
        attempts = attempts,
        correctGuesses = correct,
        streak = streak,
        bestStreak = best
    )
}
