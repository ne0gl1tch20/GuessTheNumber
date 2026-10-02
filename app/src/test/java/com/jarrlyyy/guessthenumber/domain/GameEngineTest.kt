package com.jarrlyyy.guessthenumber.domain

import com.jarrlyyy.guessthenumber.domain.engine.GameEngine
import com.jarrlyyy.guessthenumber.domain.engine.GuessFeedback
import com.jarrlyyy.guessthenumber.domain.model.BigNumber
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
}
