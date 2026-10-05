package com.jarrlyyy.guessthenumber.domain.engine

import com.jarrlyyy.guessthenumber.domain.model.BigNumber
import com.jarrlyyy.guessthenumber.domain.model.Difficulty
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

    companion object {
        const val FRENZY_THRESHOLD = 5
        const val FRENZY_BONUS_PER_TIER = 0.25
        const val FRENZY_MAX_MULTIPLIER = 5.0
        private const val HARDCORE_MULTIPLIER = 2.0
        private const val SPEED_MULTIPLIER = 3.0
        private const val BLIND_MULTIPLIER = 5.0
        private const val TAX_MULTIPLIER = 10.0
    }

    fun frenzyMultiplier(streak: Int): BigNumber {
        if (streak < FRENZY_THRESHOLD) return BigNumber.ONE
        val tiers = streak / FRENZY_THRESHOLD
        val multiplier = minOf(
            FRENZY_MAX_MULTIPLIER,
            1.0 + (tiers * FRENZY_BONUS_PER_TIER)
        )
        return BigNumber(multiplier)
    }

    fun getInitialRangeMax(difficultyId: String): Long =
        Difficulty.baseRangeMax(difficultyId)

    fun getRangeMax(state: GameState): Long {
        val betterRangeLevel = state.upgradeLevels["better_range"] ?: 0
        val base = Difficulty.rangeMax(state.difficultyId, betterRangeLevel)
        val active = validateMutators(state.activeMutators)
        return if ("mut_hardcore" in active) base * 2L else base
    }

    fun mutatorRewardMultiplier(state: GameState): BigNumber {
        val active = validateMutators(state.activeMutators)
        var multiplier = 1.0
        if ("mut_hardcore" in active) multiplier *= HARDCORE_MULTIPLIER
        if ("mut_speed" in active) multiplier *= SPEED_MULTIPLIER
        if ("mut_blind" in active) multiplier *= BLIND_MULTIPLIER
        if ("mut_tax" in active) multiplier *= TAX_MULTIPLIER
        return BigNumber(multiplier)
    }

    fun validateMutators(mutators: Set<String>): Set<String> =
        mutators.intersect(setOf("mut_hardcore", "mut_speed", "mut_blind", "mut_tax"))

    fun applyMutatorSet(state: GameState, mutators: Set<String>): GameState =
        state.copy(activeMutators = validateMutators(mutators))

    fun processGuess(state: GameState, guess: Long): GuessResult {
        // Validate target range bounds
        val clampedGuess = guess.coerceIn(state.currentRangeMin, state.currentRangeMax)
        val feedback = when {
            clampedGuess < state.targetNumber -> GuessFeedback.TOO_LOW
            clampedGuess > state.targetNumber -> GuessFeedback.TOO_HIGH
            else -> GuessFeedback.CORRECT
        }

        if (feedback == GuessFeedback.CORRECT) {
            val critChance = 0.05 + ((state.prestigeUpgradeLevels["critical_precision_boost"] ?: 0) * 0.01)
            val isCrit = rng.nextDouble() < critChance
            val baseReward = BigNumber(100) * BigNumber(state.currentRangeMax)
            val streakMultiplier = BigNumber(1.0 + (state.streak * 0.1))
            val frenzyMult = frenzyMultiplier(state.streak)
            val critMultiplier = if (isCrit) BigNumber(5) else BigNumber.ONE

            // Multipliers from upgrades
            val rewardMultiplierLevel = state.upgradeLevels["reward_multiplier"] ?: 0
            val upgradeMult = BigNumber(1.0 + (rewardMultiplierLevel * 0.25))

            // Talents & Mutators multiplier integration
            val talentMult = if (state.shopPurchases.contains("talent_reward_1") || state.upgradeLevels.containsKey("talent_reward_1")) BigNumber(1.5) else BigNumber.ONE
            val mutatorMult = mutatorRewardMultiplier(state)

            val difficultyMultiplier = BigNumber(Difficulty.rewardMultiplier(state.difficultyId))
            val calculatedReward = baseReward * streakMultiplier * frenzyMult * critMultiplier * upgradeMult * talentMult * mutatorMult * difficultyMultiplier
            val totalReward = maxOf(BigNumber(10), calculatedReward)
            val newMoney = state.money + totalReward
            val newStreak = state.streak + 1
            val bestStreak = maxOf(state.bestStreak, newStreak)

            // Generate new target number
            val newMin = 1L
            val betterRangeLevel = state.upgradeLevels["better_range"] ?: 0
            val newMax = getRangeMax(state)
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
            val difficultyPenalty = state.money * BigNumber(Difficulty.wrongGuessPenalty(state.difficultyId))
            val taxPenalty = if ("mut_tax" in state.activeMutators) state.money * BigNumber(0.05) else BigNumber.ZERO
            val newMoney = maxOf(BigNumber.ZERO, state.money - difficultyPenalty - taxPenalty)

            val newStats = state.statistics.copy(
                totalGuesses = state.statistics.totalGuesses + 1,
                failedGuesses = state.statistics.failedGuesses + 1
            )
            val updatedState = state.copy(
                money = newMoney,
                attempts = newAttempts,
                streak = 0,
                statistics = newStats
            )
            return GuessResult(feedback, BigNumber.ZERO, false, 0, updatedState)
        }
    }

    fun calculatePrestigeRequirement(prestigeCount: Long): BigNumber {
        val base = 50_000_000.0
        val multiplier = Math.pow(2.5, prestigeCount.toDouble())
        return BigNumber(base * multiplier)
    }

    fun calculateUltraRequirement(ultraCount: Long): BigNumber {
        val base = 10_000_000_000.0
        val multiplier = Math.pow(5.0, ultraCount.toDouble())
        return BigNumber(base * multiplier)
    }

    fun calculatePrestigeReward(money: BigNumber, prestigeCount: Long): BigNumber {
        val req = calculatePrestigeRequirement(prestigeCount).value.toDouble()
        val m = money.value.toDouble()
        if (m < req) return BigNumber.ZERO
        val p = sqrt(m / req)
        return BigNumber(p).floor()
    }

    fun calculateUltraReward(money: BigNumber, ultraCount: Long): BigNumber {
        val req = calculateUltraRequirement(ultraCount).value.toDouble()
        val m = money.value.toDouble()
        if (m < req) return BigNumber.ZERO
        val u = log10(m / req) * 100.0
        val capped = minOf(u, 2000.0)
        return BigNumber(capped).floor()
    }

    fun claimChallenge(state: GameState, challengeId: String, rewardNebula: Long): Pair<GameState, Boolean> {
        if (challengeId in state.completedChallenges || !isValidChallengeReward(state, challengeId, rewardNebula)) {
            return Pair(state, false)
        }
        return Pair(
            state.copy(
                completedChallenges = state.completedChallenges + challengeId,
                nebula = state.nebula + BigNumber(rewardNebula)
            ),
            true
        )
    }

    private fun isValidChallengeReward(state: GameState, challengeId: String, rewardNebula: Long): Boolean {
        if (rewardNebula <= 0L) return false

        if (challengeId.startsWith("weekly_")) {
            val kind = challengeId.substringAfterLast('_')
            val expectedReward = when (kind) {
                "correct" -> 75L
                "streak" -> 90L
                "money" -> 100L
                "guesses" -> 80L
                "prestige" -> 125L
                "ultra" -> 175L
                "frenzy" -> 110L
                "playtime" -> 95L
                else -> return false
            }
            if (rewardNebula != expectedReward) return false
            val progress = when (kind) {
                "correct" -> state.correctGuesses
                "streak" -> state.bestStreak.toLong()
                "money" -> state.money.value.toLong()
                "guesses" -> state.statistics.totalGuesses
                "prestige" -> state.prestigeCount
                "ultra" -> state.ultraCount
                "frenzy" -> state.bestStreak.toLong()
                "playtime" -> state.statistics.playtimeSeconds
                else -> return false
            }
            val target = when (kind) {
                "correct" -> 75L
                "streak" -> 15L
                "money" -> 10_000_000L
                "guesses" -> 250L
                "prestige" -> 2L
                "ultra" -> 1L
                "frenzy" -> 25L
                "playtime" -> 3_600L
                else -> return false
            }
            return progress >= target
        }

        if (challengeId.startsWith("builder_")) {
            val parts = challengeId.removePrefix("builder_").split('_')
            if (parts.size < 2) return false
            val kind = parts[0]
            val target = parts[1].toLongOrNull() ?: return false
            val mutators = parts.drop(2)
            val validMutators = setOf("mut_hardcore", "mut_speed", "mut_blind", "mut_tax")
            if (target < 1L || mutators.any { it !in validMutators }) return false
            if (rewardNebula != 50L + mutators.size * 50L) return false
            val progress = when (kind) {
                "correct" -> state.correctGuesses
                "streak" -> state.bestStreak.toLong()
                "guesses" -> state.statistics.totalGuesses
                else -> state.money.value.toLong()
            }
            return progress >= target
        }

        return false
    }

    @Deprecated("Legacy overload")
    fun calculatePrestigeReward(money: BigNumber): BigNumber = calculatePrestigeReward(money, 0L)

    @Deprecated("Legacy overload")
    fun calculateUltraReward(money: BigNumber): BigNumber = calculateUltraReward(money, 0L)
}
