package com.jarrlyyy.guessthenumber.domain.engine

import com.jarrlyyy.guessthenumber.domain.model.BigNumber
import com.jarrlyyy.guessthenumber.domain.model.GameState
import com.jarrlyyy.guessthenumber.shared.rules.GuessCoreState
import com.jarrlyyy.guessthenumber.shared.rules.GuessStateRules
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Test

class GuessCoreStateParityTest {
    @Test
    fun incorrectGuessMatchesSharedCoreStateWithoutChangingTargetOrRange() {
        val state = GameState(
            money = BigNumber(1000),
            targetNumber = 50,
            currentRangeMin = 1,
            currentRangeMax = 100,
            attempts = 7,
            correctGuesses = 2,
            streak = 3,
            bestStreak = 5
        )
        val shared = GuessStateRules.applyGuess(
            state = state.toGuessCoreState(),
            guess = 51
        ).nextState

        val actual = GameEngine(Random(1234)).processGuess(state, 51).newState

        assertEquals(shared.attempts, actual.attempts)
        assertEquals(shared.correctGuesses, actual.correctGuesses)
        assertEquals(shared.streak, actual.streak)
        assertEquals(shared.bestStreak, actual.bestStreak)
        assertEquals(shared.targetNumber, actual.targetNumber)
        assertEquals(shared.rangeMin, actual.currentRangeMin)
        assertEquals(shared.rangeMax, actual.currentRangeMax)
    }

    @Test
    fun correctGuessMatchesSharedCountersAndUsesGeneratedNextTarget() {
        val state = GameState(
            money = BigNumber(1000),
            targetNumber = 50,
            currentRangeMin = 1,
            currentRangeMax = 100,
            attempts = 7,
            correctGuesses = 2,
            streak = 3,
            bestStreak = 5
        )
        val randomForExpected = Random(5678)
        randomForExpected.nextDouble()
        val expectedTarget = randomForExpected.nextLong(1L, 101L)
        val shared = GuessStateRules.applyGuess(
            state = state.toGuessCoreState(),
            guess = 50
        ).nextState.copy(
            targetNumber = expectedTarget,
            rangeMin = 1,
            rangeMax = 100
        )

        val actual = GameEngine(Random(5678)).processGuess(state, 50).newState

        assertEquals(shared.attempts, actual.attempts)
        assertEquals(shared.correctGuesses, actual.correctGuesses)
        assertEquals(shared.streak, actual.streak)
        assertEquals(shared.bestStreak, actual.bestStreak)
        assertEquals(shared.targetNumber, actual.targetNumber)
        assertEquals(shared.rangeMin, actual.currentRangeMin)
        assertEquals(shared.rangeMax, actual.currentRangeMax)
    }

    private fun GameState.toGuessCoreState() = GuessCoreState(
        targetNumber = targetNumber,
        rangeMin = currentRangeMin,
        rangeMax = currentRangeMax,
        attempts = attempts,
        correctGuesses = correctGuesses,
        streak = streak,
        bestStreak = bestStreak
    )
}
