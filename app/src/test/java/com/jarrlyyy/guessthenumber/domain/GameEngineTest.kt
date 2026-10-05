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
    private val engine = GameEngine()


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

        assertEquals(0, hardResult.newState.money.compareTo(BigNumber(98)))
        assertEquals(150L, hardResult.newState.currentRangeMax)

        val extremeState = GameState(
            difficultyId = Difficulty.EXTREME,
            money = BigNumber(100),
            targetNumber = 200L,
            currentRangeMax = 250L
        )
        val extremeResult = engine.processGuess(extremeState, 1L)

        assertEquals(0, extremeResult.newState.money.compareTo(BigNumber(95)))
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
        assertEquals(0, (frenzy.reward / normal.reward).compareTo(BigNumber(1.25)))
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
        
        val eligible = state.copy(correctGuesses = 75)
        val (state1, claimed1) = engine.claimChallenge(eligible, "weekly_2026_40_correct", 75)
        assertTrue(claimed1)
        assertEquals(BigNumber(75), state1.nebula)
        assertTrue(state1.completedChallenges.contains("weekly_2026_40_correct"))

        val (_, claimed2) = engine.claimChallenge(state1, "weekly_2026_40_correct", 75)
        assertFalse(claimed2)
    }
    @Test
    fun combinedMutatorsStackTheirRewardMultipliers() {
        val engine = GameEngine()
        val state = GameState(
            currentRangeMax = 100,
            targetNumber = 50,
            activeMutators = setOf("mut_hardcore", "mut_speed", "mut_blind", "mut_tax")
        )
        val multiplier = engine.mutatorRewardMultiplier(state)
        assertEquals(BigNumber(300.0), multiplier)
    }

    @Test
    fun hardcoreMutatorDoublesTheConfiguredRange() {
        val state = GameState(
            difficultyId = Difficulty.CLASSIC,
            activeMutators = setOf("mut_hardcore")
        )
        assertEquals(200L, engine.getRangeMax(state))
    }

    @Test
    fun unknownMutatorsAreRemoved() {
        val state = GameState(activeMutators = setOf("mut_hardcore", "unknown_mutator"))
        val sanitized = engine.applyMutatorSet(state, state.activeMutators)
        assertEquals(setOf("mut_hardcore"), sanitized.activeMutators)
    }

    @Test
    fun emptyMutatorSetReturnsBaseMultiplier() {
        assertEquals(0, engine.mutatorRewardMultiplier(GameState()).compareTo(BigNumber.ONE))
    }

    @Test
    fun invalidChallengeRewardCannotBeClaimed() {
        val engine = GameEngine()
        val eligible = GameState(correctGuesses = 75)
        val (updated, claimed) = engine.claimChallenge(
            eligible,
            "weekly_2026_40_correct",
            999999
        )
        assertFalse(claimed)
        assertEquals(eligible, updated)
    }

}
