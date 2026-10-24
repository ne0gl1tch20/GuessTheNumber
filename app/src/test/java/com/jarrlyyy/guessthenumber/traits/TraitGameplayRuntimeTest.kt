package com.jarrlyyy.guessthenumber.traits

import com.jarrlyyy.guessthenumber.domain.engine.GuessFeedback
import com.jarrlyyy.guessthenumber.domain.engine.GuessResult
import com.jarrlyyy.guessthenumber.domain.model.BigNumber
import com.jarrlyyy.guessthenumber.domain.model.GameState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TraitGameplayRuntimeTest {
    @Test fun comboAndRiskTraitsIncreaseActualCorrectGuessPayout() {
        val before = GameState(
            money = BigNumber(1_000),
            currentRangeMin = 1,
            currentRangeMax = 100,
            targetNumber = 50,
            correctGuesses = 100,
            streak = 5
        )
        val after = before.copy(
            money = BigNumber(1_100),
            correctGuesses = 101,
            streak = 6,
            bestStreak = 6,
            statistics = before.statistics.copy(moneyEarned = BigNumber(100))
        )
        val base = GuessResult(GuessFeedback.CORRECT, BigNumber(100), false, 6, after)
        val result = TraitGameplayRuntime.apply(before, 50, base, "summer")
        assertEquals(135.0, result.reward.value.toDouble(), 0.001)
        assertEquals(1_235.0, result.newState.money.value.toDouble(), 0.001)
        assertEquals(135.0, result.newState.statistics.moneyEarned.value.toDouble(), 0.001)
    }

    @Test fun mistakeInsuranceRefundsHalfOfMissPenaltyAndRangeScoutNarrowsHints() {
        val before = GameState(
            money = BigNumber(1_000),
            currentRangeMin = 1,
            currentRangeMax = 100,
            targetNumber = 50,
            correctGuesses = 50,
            streak = 4
        )
        val after = before.copy(
            money = BigNumber(900),
            attempts = 1,
            streak = 0,
            statistics = before.statistics.copy(totalGuesses = 1, failedGuesses = 1)
        )
        val base = GuessResult(GuessFeedback.TOO_LOW, BigNumber.ZERO, false, 0, after)
        val result = TraitGameplayRuntime.apply(before, 40, base, "summer")
        assertEquals(950.0, result.newState.money.value.toDouble(), 0.001)
        assertEquals(41L, result.newState.currentRangeMin)
        assertEquals(100L, result.newState.currentRangeMax)
        assertEquals(4, result.newState.streak)
    }

    @Test fun traitLogicDoesNotRunForOutOfRangeGuess() {
        val before = GameState(currentRangeMin = 1, currentRangeMax = 100, correctGuesses = 100)
        val after = before.copy(attempts = 1)
        val base = GuessResult(GuessFeedback.TOO_LOW, BigNumber.ZERO, false, 0, after)
        val result = TraitGameplayRuntime.apply(before, 101, base, "summer")
        assertEquals(after, result.newState)
    }

    @Test fun rangeScoutDoesNotMoveRangePastAnExtremeGuess() {
        val before = GameState(currentRangeMin = 1, currentRangeMax = 100, correctGuesses = 10)
        val after = before.copy(attempts = 1)
        val base = GuessResult(GuessFeedback.TOO_LOW, BigNumber.ZERO, false, 0, after)
        val result = TraitGameplayRuntime.apply(before, 100, base, "summer")
        assertTrue(result.newState.currentRangeMin <= result.newState.currentRangeMax)
    }
}
