package com.jarrlyyy.guessthenumber.domain

import com.jarrlyyy.guessthenumber.domain.engine.GameEngine
import com.jarrlyyy.guessthenumber.domain.engine.GuessFeedback
import com.jarrlyyy.guessthenumber.domain.model.BigNumber
import com.jarrlyyy.guessthenumber.domain.model.Difficulty
import com.jarrlyyy.guessthenumber.domain.model.GameState
import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class GameEngineTest {

    @Test
    fun testCorrectGuessRewardAndStreak() {
        val engine = GameEngine(Random(123))
        var state = GameState(targetNumber = 50L)
        
        val result = engine.processGuess(state, 50L)
        assertEquals(GuessFeedback.CORRECT, result.feedback)
        assertTrue(result.reward > BigNumber.ZERO)
        assertEquals(1, result.newStreak)
        assertTrue(result.newState.money > BigNumber.ZERO)
        assertEquals(1L, result.newState.correctGuesses)
    }

    @Test
    fun testWrongGuessResetsStreak() {
        val engine = GameEngine(Random(123))
        val state = GameState(targetNumber = 50L, streak = 5)
        
        val result = engine.processGuess(state, 20L)
        assertEquals(GuessFeedback.TOO_LOW, result.feedback)
        assertEquals(BigNumber.ZERO, result.reward)
        assertEquals(0, result.newStreak)
        assertEquals(0, result.newState.streak)
    }



    @Test
    fun testDifficultyRangeAndWrongGuessPenalty() {
        val engine = GameEngine(Random(123))

        val hardState = GameState(
            difficultyId = Difficulty.HARD,
            money = BigNumber(100),
            targetNumber = 120L,
            currentRangeMax = 150L
        )
        val hardResult = engine.processGuess(hardState, 1L)

        assertEquals(BigNumber(98), hardResult.newState.money)
        assertEquals(150L, hardResult.newState.currentRangeMax)

        val extremeState = GameState(
            difficultyId = Difficulty.EXTREME,
            money = BigNumber(100),
            targetNumber = 200L,
            currentRangeMax = 250L
        )
        val extremeResult = engine.processGuess(extremeState, 1L)

        assertEquals(BigNumber(95), extremeResult.newState.money)
        assertEquals(250L, extremeResult.newState.currentRangeMax)
    }

    @Test
    fun testDifficultyCorrectGuessUsesDifficultyRangeAndReward() {
        val classic = GameEngine(Random(123)).processGuess(
            GameState(targetNumber = 50L, currentRangeMax = 100L),
            50L
        )
        val hard = GameEngine(Random(123)).processGuess(
            GameState(
                difficultyId = Difficulty.HARD,
                targetNumber = 75L,
                currentRangeMax = 150L
            ),
            75L
        )
        val extreme = GameEngine(Random(123)).processGuess(
            GameState(
                difficultyId = Difficulty.EXTREME,
                targetNumber = 125L,
                currentRangeMax = 250L
            ),
            125L
        )

        assertTrue(hard.reward > classic.reward)
        assertTrue(extreme.reward > hard.reward)
        assertEquals(150L, hard.newState.currentRangeMax)
        assertEquals(250L, extreme.newState.currentRangeMax)
    }

    @Test
    fun testComboFrenzyMultiplierScalesAtFiveGuessTiers() {
        val engine = GameEngine(Random(123))

        assertEquals(BigNumber.ONE, engine.frenzyMultiplier(4))
        assertEquals(BigNumber(1.25), engine.frenzyMultiplier(5))
        assertEquals(BigNumber(1.5), engine.frenzyMultiplier(10))
        assertEquals(BigNumber(5.0), engine.frenzyMultiplier(100))
    }

    @Test
    fun testComboFrenzyIncreasesCorrectGuessReward() {
        val normal = GameEngine(Random(123)).processGuess(
            GameState(targetNumber = 50L, currentRangeMax = 100L, streak = 4),
            50L
        )
        val frenzy = GameEngine(Random(123)).processGuess(
            GameState(targetNumber = 50L, currentRangeMax = 100L, streak = 5),
            50L
        )

        assertTrue(frenzy.reward > normal.reward)
        assertEquals(BigNumber(1.25), frenzy.reward / normal.reward)
    }

    @Test
    fun testPrestigeAndUltraCalculations() {
        val engine = GameEngine()
        val moneyForPrestige = BigNumber(60_000_000.0)
        val prestigeReward = engine.calculatePrestigeReward(moneyForPrestige, 0L)
        assertTrue(prestigeReward > BigNumber.ZERO)

        val moneyForUltra = BigNumber(15_000_000_000.0)
        val ultraReward = engine.calculateUltraReward(moneyForUltra, 0L)
        assertTrue(ultraReward > BigNumber.ZERO)
    }

    @Test
    fun testChallengeDuplicateClaims() {
        val engine = GameEngine()
        val state = GameState()
        
        val (state1, claimed1) = engine.claimChallenge(state, "daily_1", 50)
        assertTrue(claimed1)
        assertEquals(BigNumber(50), state1.nebula)
        assertTrue(state1.completedChallenges.contains("daily_1"))

        val (_, claimed2) = engine.claimChallenge(state1, "daily_1", 50)
        assertFalse(claimed2)
    }
    @Test
    fun combinedMutatorsStackTheirRewardMultipliers() {
        val state = GameState(
            currentRangeMax = 100,
            targetNumber = 50,
            activeMutators = setOf("mut_hardcore", "mut_speed", "mut_blind", "mut_tax")
        )
        val multiplier = engine.mutatorRewardMultiplier(state)
        assertEquals(BigNumber(150.0), multiplier)
    }

    @Test
    fun hardcoreMutatorDoublesTheConfiguredRange() {
        val state = GameState(
            difficultyId = Difficulty.CLASSIC,
            activeMutators = setOf("mut_hardcore")
        )
        assertEquals(200L, engine.getRangeMax(state))
    }

}
