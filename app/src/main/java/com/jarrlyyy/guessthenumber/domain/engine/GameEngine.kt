package com.jarrlyyy.guessthenumber.domain.engine

import com.jarrlyyy.guessthenumber.domain.model.BigNumber
import com.jarrlyyy.guessthenumber.domain.model.GameState
import kotlin.math.log10
import kotlin.math.sqrt
import kotlin.random.Random

enum class GuessFeedback {
    TOO_LOW, TOO_HIGH, CORRECT
}

data class GuessResult(
    val feedback: GuessFeedback,
    val reward: BigNumber,
    val isCritical: Boolean,
    val newStreak: Int,
    val newState: GameState
)

class GameEngine(private val rng: Random = Random.Default) {

    fun processGuess(state: GameState, guess: Long): GuessResult {
        val feedback = when {
            guess < state.targetNumber -> GuessFeedback.TOO_LOW
            guess > state.targetNumber -> GuessFeedback.TOO_HIGH
            else -> GuessFeedback.CORRECT
        }

        if (feedback == GuessFeedback.CORRECT) {
            val isCrit = rng.nextDouble() < 0.05 // 5% critical chance
            val baseReward = BigNumber(100) * BigNumber(state.currentRangeMax)
            val streakMultiplier = BigNumber(1.0 + (state.streak * 0.1))
            val critMultiplier = if (isCrit) BigNumber(5) else BigNumber.ONE

            // Multipliers from upgrades
            val rewardMultiplierLevel = state.upgradeLevels["reward_multiplier"] ?: 0
            val upgradeMult = BigNumber(1.0 + (rewardMultiplierLevel * 0.25))

            val calculatedReward = baseReward * streakMultiplier * critMultiplier * upgradeMult
            val totalReward = maxOf(BigNumber(10), calculatedReward)
            val newMoney = state.money + totalReward
            val newStreak = state.streak + 1
            val bestStreak = maxOf(state.bestStreak, newStreak)

            // Generate new target number
            val newMin = 1L
            val betterRangeLevel = state.upgradeLevels["better_range"] ?: 0
            val newMax = maxOf(50L, 100L - (betterRangeLevel * 5L))
            val newTarget = rng.nextLong(newMin, newMax + 1)

            val newStats = state.statistics.copy(
                totalGuesses = state.statistics.totalGuesses + 1,
                correctGuesses = state.statistics.correctGuesses + 1,
                moneyEarned = state.statistics.moneyEarned + totalReward
            )

            val updatedState = state.copy(
                money = newMoney,
                currentRangeMin = newMin,
                currentRangeMax = newMax,
                targetNumber = newTarget,
                attempts = state.attempts + 1,
                correctGuesses = state.correctGuesses + 1,
                streak = newStreak,
                bestStreak = bestStreak,
                statistics = newStats
            )

            return GuessResult(feedback, totalReward, isCrit, newStreak, updatedState)
        } else {
            val newAttempts = state.attempts + 1
            val newStats = state.statistics.copy(
                totalGuesses = state.statistics.totalGuesses + 1,
                failedGuesses = state.statistics.failedGuesses + 1
            )
            val updatedState = state.copy(
                attempts = newAttempts,
                streak = 0,
                statistics = newStats
            )
            return GuessResult(feedback, BigNumber.ZERO, false, 0, updatedState)
        }
    }

    fun calculatePrestigeReward(money: BigNumber): BigNumber {
        // Prestige formula: sqrt(Money / 5e7)
        val m = money.value.toDouble()
        if (m < 50_000_000.0) return BigNumber.ZERO
        val p = sqrt(m / 50_000_000.0)
        return BigNumber(p).floor()
    }

    fun calculateUltraReward(money: BigNumber): BigNumber {
        // Ultra formula: log10(Money / 1e10), capped at 2000 per reset
        val m = money.value.toDouble()
        if (m < 10_000_000_000.0) return BigNumber.ZERO
        val u = log10(m / 10_000_000_000.0) * 100.0
        val capped = minOf(u, 2000.0)
        return BigNumber(capped).floor()
    }
}
