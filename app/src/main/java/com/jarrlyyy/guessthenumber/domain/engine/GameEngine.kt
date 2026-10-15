package com.jarrlyyy.guessthenumber.domain.engine

import com.jarrlyyy.guessthenumber.shared.domain.FrenzyRules
import com.jarrlyyy.guessthenumber.shared.domain.CriticalChanceRules
import com.jarrlyyy.guessthenumber.shared.domain.GuessRewardRules
import com.jarrlyyy.guessthenumber.shared.domain.GuessPenaltyRules
import com.jarrlyyy.guessthenumber.shared.domain.MutatorRules
import com.jarrlyyy.guessthenumber.shared.domain.ChallengeProgress
import com.jarrlyyy.guessthenumber.shared.domain.ChallengeRewardRules
import com.jarrlyyy.guessthenumber.shared.domain.ProgressionRequirementRules
import com.jarrlyyy.guessthenumber.shared.domain.ProgressionRewardRules
import com.jarrlyyy.guessthenumber.shared.domain.DifficultyRules
import com.jarrlyyy.guessthenumber.shared.rules.GuessStateRules
import com.jarrlyyy.guessthenumber.domain.model.BigNumber
import com.jarrlyyy.guessthenumber.domain.model.Difficulty
import com.jarrlyyy.guessthenumber.domain.model.GameState
import kotlin.random.Random

typealias GuessFeedback = com.jarrlyyy.guessthenumber.shared.rules.GuessFeedback

data class GuessResult(
    val feedback: GuessFeedback,
    val reward: BigNumber,
    val isCritical: Boolean,
    val newStreak: Int,
    val newState: GameState
)

class GameEngine(private val rng: Random = Random.Default) {

    companion object {
        const val FRENZY_THRESHOLD = FrenzyRules.THRESHOLD
        const val FRENZY_BONUS_PER_TIER = FrenzyRules.BONUS_PER_TIER
        const val FRENZY_MAX_MULTIPLIER = FrenzyRules.MAX_MULTIPLIER
    }

    fun frenzyMultiplier(streak: Int): BigNumber =
        if (streak < FrenzyRules.THRESHOLD) BigNumber.ONE
        else BigNumber(FrenzyRules.multiplier(streak))

    fun getInitialRangeMax(difficultyId: String): Long =
        Difficulty.baseRangeMax(difficultyId)

    fun getRangeMax(state: GameState): Long {
        val betterRangeLevel = state.upgradeLevels["better_range"] ?: 0
        val base = Difficulty.rangeMax(state.difficultyId, betterRangeLevel)
        return base * MutatorRules.rangeMultiplier(state.activeMutators)
    }

    fun mutatorRewardMultiplier(state: GameState): BigNumber =
        BigNumber(MutatorRules.rewardMultiplier(state.activeMutators))

    fun validateMutators(mutators: Set<String>): Set<String> =
        MutatorRules.validate(mutators)

    fun applyMutatorSet(state: GameState, mutators: Set<String>): GameState =
        state.copy(activeMutators = validateMutators(mutators))

    fun processGuess(state: GameState, guess: Long): GuessResult {
        val transition = GuessStateRules.applyGuess(
            targetNumber = state.targetNumber,
            guess = guess,
            rangeMin = state.currentRangeMin,
            rangeMax = state.currentRangeMax,
            attempts = state.attempts,
            correctGuesses = state.correctGuesses,
            streak = state.streak,
            bestStreak = state.bestStreak
        )
        val feedback = transition.feedback

        if (feedback == GuessFeedback.CORRECT) {
            val critChance = CriticalChanceRules.chance(state.prestigeUpgradeLevels["critical_precision_boost"] ?: 0)
            val isCrit = rng.nextDouble() < critChance
            val baseReward = BigNumber(100) * BigNumber(state.currentRangeMax)
            val streakMultiplier = BigNumber(GuessRewardRules.streakMultiplier(state.streak))
            val frenzyMult = frenzyMultiplier(state.streak)
            val critMultiplier = BigNumber(GuessRewardRules.criticalMultiplier(isCrit))
            val rewardMultiplierLevel = state.upgradeLevels["reward_multiplier"] ?: 0
            val upgradeMult = BigNumber(GuessRewardRules.upgradeMultiplier(rewardMultiplierLevel))
            val talentMult = if (state.shopPurchases.contains("talent_reward_1") || state.upgradeLevels.containsKey("talent_reward_1")) BigNumber(1.5) else BigNumber.ONE
            val mutatorMult = mutatorRewardMultiplier(state)
            val difficultyMultiplier = BigNumber(Difficulty.rewardMultiplier(state.difficultyId))
            val calculatedReward = baseReward * streakMultiplier * frenzyMult * critMultiplier * upgradeMult * talentMult * mutatorMult * difficultyMultiplier
            val totalReward = maxOf(BigNumber(10), calculatedReward)
            val newMoney = state.money + totalReward
            val newStreak = transition.streak
            val bestStreak = transition.bestStreak

            val newMin = 1L
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
                attempts = transition.attempts,
                correctGuesses = transition.correctGuesses,
                streak = newStreak,
                bestStreak = bestStreak,
                statistics = newStats
            )
            return GuessResult(feedback, totalReward, isCrit, newStreak, updatedState)
        } else {
            val newAttempts = transition.attempts
            val difficultyPenalty = state.money * BigNumber(Difficulty.wrongGuessPenalty(state.difficultyId))
            val taxPenaltyRate = GuessPenaltyRules.taxPenaltyRate(state.activeMutators)
            val taxPenalty = if (taxPenaltyRate > 0.0) state.money * BigNumber(taxPenaltyRate) else BigNumber.ZERO
            val newMoney = maxOf(BigNumber.ZERO, state.money - difficultyPenalty - taxPenalty)
            val newStats = state.statistics.copy(
                totalGuesses = state.statistics.totalGuesses + 1,
                failedGuesses = state.statistics.failedGuesses + 1
            )
            val updatedState = state.copy(
                money = newMoney,
                attempts = newAttempts,
                correctGuesses = transition.correctGuesses,
                streak = transition.streak,
                bestStreak = transition.bestStreak,
                statistics = newStats
            )
            return GuessResult(feedback, BigNumber.ZERO, false, 0, updatedState)
        }
    }

    fun calculatePrestigeRequirement(prestigeCount: Long): BigNumber =
        BigNumber(ProgressionRequirementRules.prestigeRequirement(prestigeCount))

    fun calculateUltraRequirement(ultraCount: Long): BigNumber =
        BigNumber(ProgressionRequirementRules.ultraRequirement(ultraCount))

    fun calculatePrestigeReward(money: BigNumber, prestigeCount: Long): BigNumber {
        val req = calculatePrestigeRequirement(prestigeCount).value.toDouble()
        val m = money.value.toDouble()
        return BigNumber(ProgressionRewardRules.prestigeRewardRatio(m, req)).floor()
    }

    fun calculateUltraReward(money: BigNumber, ultraCount: Long): BigNumber {
        val req = calculateUltraRequirement(ultraCount).value.toDouble()
        val m = money.value.toDouble()
        return BigNumber(ProgressionRewardRules.ultraRewardRatio(m, req)).floor()
    }

    fun claimChallenge(state: GameState, challengeId: String, rewardNebula: Long): Pair<GameState, Boolean> {
        if (challengeId in state.completedChallenges || !isValidChallengeReward(state, challengeId, rewardNebula)) {
            return Pair(state, false)
        }
        return Pair(
            state.copy(
                completedChallenges = state.completedChallenges + challengeId,
                nebula = state.nebula + BigNumber(rewardNebula),
                statistics = state.statistics.copy(nebulaEarned = state.statistics.nebulaEarned + rewardNebula)
            ),
            true
        )
    }

    private fun isValidChallengeReward(state: GameState, challengeId: String, rewardNebula: Long): Boolean =
        ChallengeRewardRules.isValidReward(
            challengeId = challengeId,
            rewardNebula = rewardNebula,
            progress = ChallengeProgress(
                correctGuesses = state.correctGuesses,
                bestStreak = state.bestStreak.toLong(),
                money = state.money.value.toLong(),
                totalGuesses = state.statistics.totalGuesses,
                prestigeCount = state.prestigeCount,
                ultraCount = state.ultraCount,
                playtimeSeconds = state.statistics.playtimeSeconds
            )
        )

    @Deprecated("Legacy overload")
    fun calculatePrestigeReward(money: BigNumber): BigNumber = calculatePrestigeReward(money, 0L)

    @Deprecated("Legacy overload")
    fun calculateUltraReward(money: BigNumber): BigNumber = calculateUltraReward(money, 0L)
}
