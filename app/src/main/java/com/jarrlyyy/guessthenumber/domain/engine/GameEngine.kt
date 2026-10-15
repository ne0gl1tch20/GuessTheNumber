package com.jarrlyyy.guessthenumber.domain.engine

import com.jarrlyyy.guessthenumber.shared.domain.FrenzyRules
import com.jarrlyyy.guessthenumber.shared.domain.CriticalChanceRules
import com.jarrlyyy.guessthenumber.shared.domain.GuessEconomyDecisionRules
import com.jarrlyyy.guessthenumber.shared.domain.MutatorRules
import com.jarrlyyy.guessthenumber.shared.domain.ChallengeProgress
import com.jarrlyyy.guessthenumber.shared.domain.ChallengeRewardRules
import com.jarrlyyy.guessthenumber.shared.domain.ProgressionRequirementRules
import com.jarrlyyy.guessthenumber.shared.domain.ProgressionRewardRules
import com.jarrlyyy.guessthenumber.shared.rules.GuessCoreState
import com.jarrlyyy.guessthenumber.shared.rules.GuessStateRules
import com.jarrlyyy.guessthenumber.shared.rules.GuessTransitionRules
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
        // Map the existing Android save model into the shared guessing-state slice.
        // Keep currency, statistics, upgrades and persistence in the Android engine.
        val transition = GuessStateRules.applyGuess(
            state = GuessCoreState(
                targetNumber = state.targetNumber,
                rangeMin = state.currentRangeMin,
                rangeMax = state.currentRangeMax,
                attempts = state.attempts,
                correctGuesses = state.correctGuesses,
                streak = state.streak,
                bestStreak = state.bestStreak
            ),
            guess = guess
        )
        val feedback = transition.feedback

        if (feedback == GuessFeedback.CORRECT) {
            val critChance = CriticalChanceRules.chance(state.prestigeUpgradeLevels["critical_precision_boost"] ?: 0)
            val criticalRoll = rng.nextDouble()
            val newMin = 1L
            val newMax = getRangeMax(state)
            val generatedNextTarget = rng.nextLong(newMin, newMax + 1)
            val decision = GuessTransitionRules.resolve(
                stateTransition = transition.stateTransition,
                criticalChance = critChance,
                criticalRoll = criticalRoll,
                nextTarget = generatedNextTarget,
                nextRangeMin = newMin,
                nextRangeMax = newMax
            )
            val isCrit = decision.isCritical
            val nextTarget = decision.nextTarget
                ?: error("Correct guess transition must provide a next target")
            val rewardMultiplierLevel = state.upgradeLevels["reward_multiplier"] ?: 0
            val hasTalentReward = state.shopPurchases.contains("talent_reward_1") || state.upgradeLevels.containsKey("talent_reward_1")
            val economyDecision = GuessEconomyDecisionRules.resolve(
                rangeMax = state.currentRangeMax,
                streak = state.streak,
                isCritical = isCrit,
                rewardMultiplierLevel = rewardMultiplierLevel,
                hasTalentReward = hasTalentReward,
                activeMutators = state.activeMutators,
                difficultyId = state.difficultyId
            )
            // Keep BigNumber multiplication order on Android to preserve currency behavior.
            val baseReward = BigNumber(economyDecision.baseRewardPerRangeUnit) * BigNumber(economyDecision.rangeUnits)
            val streakMultiplier = BigNumber(economyDecision.streakMultiplier)
            val frenzyMult = BigNumber(economyDecision.frenzyMultiplier)
            val critMultiplier = BigNumber(economyDecision.criticalMultiplier)
            val upgradeMult = BigNumber(economyDecision.upgradeMultiplier)
            val talentMult = BigNumber(economyDecision.talentMultiplier)
            val mutatorMult = BigNumber(economyDecision.mutatorMultiplier)
            val difficultyMultiplier = BigNumber(economyDecision.difficultyMultiplier)
            val calculatedReward = baseReward * streakMultiplier * frenzyMult * critMultiplier * upgradeMult * talentMult * mutatorMult * difficultyMultiplier
            val totalReward = maxOf(BigNumber(economyDecision.minimumGuessReward), calculatedReward)
            val newMoney = state.money + totalReward
            val newStreak = transition.streak
            val bestStreak = transition.bestStreak

            val newStats = state.statistics.copy(
                totalGuesses = state.statistics.totalGuesses + 1,
                correctGuesses = state.statistics.correctGuesses + 1,
                moneyEarned = state.statistics.moneyEarned + totalReward
            )
            val nextCoreState = transition.nextState.copy(
                targetNumber = nextTarget,
                rangeMin = newMin,
                rangeMax = newMax
            )
            val updatedState = state.copy(
                money = newMoney,
                currentRangeMin = nextCoreState.rangeMin,
                currentRangeMax = nextCoreState.rangeMax,
                targetNumber = nextCoreState.targetNumber,
                attempts = nextCoreState.attempts,
                correctGuesses = nextCoreState.correctGuesses,
                streak = nextCoreState.streak,
                bestStreak = nextCoreState.bestStreak,
                statistics = newStats
            )
            return GuessResult(feedback, totalReward, isCrit, newStreak, updatedState)
        } else {
            val newAttempts = transition.attempts
            val economyDecision = GuessEconomyDecisionRules.resolve(
                rangeMax = state.currentRangeMax,
                streak = state.streak,
                isCritical = false,
                rewardMultiplierLevel = state.upgradeLevels["reward_multiplier"] ?: 0,
                hasTalentReward = state.shopPurchases.contains("talent_reward_1") || state.upgradeLevels.containsKey("talent_reward_1"),
                activeMutators = state.activeMutators,
                difficultyId = state.difficultyId
            )
            val difficultyPenalty = state.money * BigNumber(economyDecision.difficultyPenaltyRate)
            val taxPenaltyRate = economyDecision.taxPenaltyRate
            val taxPenalty = if (taxPenaltyRate > 0.0) state.money * BigNumber(taxPenaltyRate) else BigNumber.ZERO
            val newMoney = maxOf(BigNumber.ZERO, state.money - difficultyPenalty - taxPenalty)
            val newStats = state.statistics.copy(
                totalGuesses = state.statistics.totalGuesses + 1,
                failedGuesses = state.statistics.failedGuesses + 1
            )
            val nextCoreState = transition.nextState
            val updatedState = state.copy(
                money = newMoney,
                targetNumber = nextCoreState.targetNumber,
                currentRangeMin = nextCoreState.rangeMin,
                currentRangeMax = nextCoreState.rangeMax,
                attempts = newAttempts,
                correctGuesses = nextCoreState.correctGuesses,
                streak = nextCoreState.streak,
                bestStreak = nextCoreState.bestStreak,
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
        if (!ChallengeRewardRules.canClaim(
                completedChallenges = state.completedChallenges,
                challengeId = challengeId,
                rewardNebula = rewardNebula,
                progress = challengeProgress(state)
            )
        ) {
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

    private fun challengeProgress(state: GameState): ChallengeProgress =
        ChallengeProgress(
            correctGuesses = state.correctGuesses,
            bestStreak = state.bestStreak.toLong(),
            money = state.money.value.toLong(),
            totalGuesses = state.statistics.totalGuesses,
            prestigeCount = state.prestigeCount,
            ultraCount = state.ultraCount,
            playtimeSeconds = state.statistics.playtimeSeconds
        )

    @Deprecated("Legacy overload")
    fun calculatePrestigeReward(money: BigNumber): BigNumber = calculatePrestigeReward(money, 0L)

    @Deprecated("Legacy overload")
    fun calculateUltraReward(money: BigNumber): BigNumber = calculateUltraReward(money, 0L)
}
