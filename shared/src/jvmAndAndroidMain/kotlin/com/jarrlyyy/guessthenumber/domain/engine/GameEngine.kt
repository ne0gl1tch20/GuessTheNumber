package com.jarrlyyy.guessthenumber.domain.engine

import com.jarrlyyy.guessthenumber.shared.domain.FrenzyRules
import com.jarrlyyy.guessthenumber.shared.domain.CriticalChanceRules
import com.jarrlyyy.guessthenumber.shared.domain.GuessEconomyDecisionRules
import com.jarrlyyy.guessthenumber.shared.domain.MutatorRules
import com.jarrlyyy.guessthenumber.shared.domain.ChallengeProgress
import com.jarrlyyy.guessthenumber.shared.domain.ChallengeRewardRules
import com.jarrlyyy.guessthenumber.shared.domain.ChallengeClaimRules
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
        const val PRESTIGE_AUTO_SPEED_MULTIPLIER = 1.5
        const val PRESTIGE_LUCK_CHARM_BONUS = 0.05
        const val INFINITE_SINGULARITY_REWARD_MULTIPLIER = 2.0
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

    /**
     * Delay used by Android and Windows for the production Auto Clicker loop.
     * Keep this formula aligned with Android's existing talent and mutator behavior.
     */
    fun autoClickerDelayMillis(state: GameState): Long {
        val talentSpeed =
            (if ("talent_speed_1" in state.prestigeShopPurchases) 1.25 else 1.0) *
                (if ("talent_speed_2" in state.prestigeShopPurchases) 1.5 else 1.0)
        val effectiveSpeed = state.autoClickerSpeed * talentSpeed *
            if ("mut_speed" in state.activeMutators) 3.0 else 1.0
        return if (state.autoClickerActive && effectiveSpeed > 0.0) {
            (1_000.0 / effectiveSpeed).toLong()
        } else {
            1_000L
        }
    }

    /** Effective auto-clicker frequency multiplier from permanent shop effects. */
    fun autoClickerSpeedMultiplier(state: GameState): Double {
        var multiplier = 1.0
        if ("prestige_auto_speed" in state.prestigeShopPurchases) {
            multiplier *= PRESTIGE_AUTO_SPEED_MULTIPLIER
        }
        if ("mut_speed" in state.activeMutators) {
            multiplier *= 3.0
        }
        return multiplier
    }

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
            val baseCritChance = CriticalChanceRules.chance(
                state.prestigeUpgradeLevels["critical_precision_boost"] ?: 0
            )
            val critChance = when {
                "ultra_universal_harmony" in state.ultraShopPurchases -> 1.0
                "golden_guess" in state.shopPurchases &&
                    (state.statistics.totalGuesses + 1L) % 10L == 0L -> 1.0
                "prestige_luck_charm" in state.prestigeShopPurchases ->
                    (baseCritChance + PRESTIGE_LUCK_CHARM_BONUS).coerceAtMost(1.0)
                else -> baseCritChance
            }
            val criticalRoll = rng.nextDouble()
            val newMin = 1L
            val newMax = getRangeMax(state)
            val generatedNextTarget = rng.nextLong(newMin, newMax + 1)
            val decision = GuessTransitionRules.resolve(
                stateTransition = transition.stateTransition,
                currentState = transition.nextState,
                criticalChance = critChance,
                criticalRoll = criticalRoll,
                nextTarget = generatedNextTarget,
                nextRangeMin = newMin,
                nextRangeMax = newMax
            )
            val isCrit = decision.isCritical
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
            val frenzyMult = if (state.streak < FrenzyRules.THRESHOLD) BigNumber.ONE else BigNumber(economyDecision.frenzyMultiplier)
            val critMultiplier = BigNumber(economyDecision.criticalMultiplier)
            val upgradeMult = BigNumber(economyDecision.upgradeMultiplier)
            val talentMult = BigNumber(economyDecision.talentMultiplier)
            val mutatorMult = BigNumber(economyDecision.mutatorMultiplier)
            val difficultyMultiplier = BigNumber(economyDecision.difficultyMultiplier)
            val shopRewardMultiplier = if ("ultra_infinite_multiplier" in state.ultraShopPurchases) {
                BigNumber(INFINITE_SINGULARITY_REWARD_MULTIPLIER)
            } else {
                BigNumber.ONE
            }
            val calculatedReward = baseReward * streakMultiplier * frenzyMult * critMultiplier * upgradeMult * talentMult * mutatorMult * difficultyMultiplier * shopRewardMultiplier
            val totalReward = maxOf(BigNumber(economyDecision.minimumGuessReward), calculatedReward) * moneyEarningMultiplier(state)
            val newMoney = state.money + totalReward
            val newStreak = transition.streak
            val bestStreak = transition.bestStreak

            val newStats = state.statistics.copy(
                totalGuesses = state.statistics.totalGuesses + 1,
                correctGuesses = state.statistics.correctGuesses + 1,
                moneyEarned = state.statistics.moneyEarned + totalReward
            )
            val nextCoreState = decision.nextState
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
            val decision = GuessTransitionRules.resolve(
                stateTransition = transition.stateTransition,
                currentState = transition.nextState,
                criticalChance = 0.0,
                criticalRoll = null,
                nextTarget = null,
                nextRangeMin = state.currentRangeMin,
                nextRangeMax = state.currentRangeMax
            )
            val newAttempts = decision.nextState.attempts
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
            val nextCoreState = decision.nextState
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

    /**
     * Permanent upgrade effects are defined from the canonical upgrade catalog's
     * effectPerLevel values. Keep these shared so Android and Windows cannot drift.
     */
    fun moneyEarningMultiplier(state: GameState): BigNumber {
        val prestigeLevel = (state.prestigeUpgradeLevels["prestige_money_boost"] ?: 0).coerceIn(0, 50)
        val ultraLevel = (state.ultraUpgradeLevels["ultra_money_boost"] ?: 0).coerceIn(0, 20)
        val prestigeMultiplier = BigNumber(1.0 + prestigeLevel * 0.5)
        val ultraMultiplier = BigNumber(1.0 + ultraLevel * 2.0)
        return prestigeMultiplier * ultraMultiplier
    }

    fun applyMoneyEarningBoost(state: GameState, baseReward: BigNumber): BigNumber =
        if (baseReward <= BigNumber.ZERO) baseReward else baseReward * moneyEarningMultiplier(state)

    fun addNebulaEarned(currentTotal: Long, reward: Long): Long {
        val current = currentTotal.coerceAtLeast(0L)
        if (reward <= 0L) return current
        return if (current > Long.MAX_VALUE - reward) Long.MAX_VALUE else current + reward
    }

    fun calculateNebulaReward(state: GameState, baseReward: Long): Long {
        if (baseReward <= 0L) return 0L
        val upgradeLevel = (state.ultraUpgradeLevels["ultra_nebula_boost"] ?: 0).coerceIn(0, 15)
        // effectPerLevel = 1.0, so each level adds one base reward to the payout.
        val multiplier = 1L + upgradeLevel.toLong()
        val shopMultiplier = if ("nebula_boost" in state.shopPurchases) 2L else 1L
        val totalMultiplier = multiplier * shopMultiplier
        return if (baseReward > Long.MAX_VALUE / totalMultiplier) Long.MAX_VALUE else baseReward * totalMultiplier
    }

    fun prestigeStartingCapital(state: GameState): BigNumber {
        val level = (state.prestigeUpgradeLevels["prestige_starting_capital"] ?: 0).coerceIn(0, 30)
        return BigNumber(level.toLong() * 1_000L)
    }

    fun calculatePrestigeReward(money: BigNumber, prestigeCount: Long): BigNumber {
        val req = calculatePrestigeRequirement(prestigeCount).value.toDouble()
        val m = money.value.toDouble()
        return BigNumber(ProgressionRewardRules.prestigeRewardRatio(m, req)).floor()
    }

    fun calculatePrestigeReward(state: GameState): BigNumber {
        val baseReward = calculatePrestigeReward(state.money, state.prestigeCount)
        val level = (state.prestigeUpgradeLevels["prestige_gain_boost"] ?: 0).coerceIn(0, 25)
        return (baseReward * BigNumber(1.0 + level * 0.2)).floor()
    }

    fun calculateUltraReward(money: BigNumber, ultraCount: Long): BigNumber {
        val req = calculateUltraRequirement(ultraCount).value.toDouble()
        val m = money.value.toDouble()
        return BigNumber(ProgressionRewardRules.ultraRewardRatio(m, req)).floor()
    }

    fun calculateUltraReward(state: GameState): BigNumber {
        val baseReward = calculateUltraReward(state.money, state.ultraCount)
        val level = (state.ultraUpgradeLevels["ultra_gain_boost"] ?: 0).coerceIn(0, 10)
        val boosted = (baseReward * BigNumber(1.0 + level * 0.5)).floor()
        // Preserve the catalog's documented 2,000 Ultra reset-reward cap.
        return minOf(boosted, BigNumber(2_000))
    }

    /**
     * Unlocks milestone achievements and pays their Nebula reward once.
     * Shared by Android and Windows so permanent Nebula upgrades have identical
     * behavior regardless of which host detects the milestone.
     */
    fun evaluateAchievements(state: GameState): GameState {
        val milestones = listOf(
            "correct_10" to (state.correctGuesses >= 10),
            "correct_50" to (state.correctGuesses >= 50),
            "correct_100" to (state.correctGuesses >= 100),
            "correct_500" to (state.correctGuesses >= 500),
            "streak_5" to (state.bestStreak >= 5),
            "streak_10" to (state.bestStreak >= 10),
            "streak_25" to (state.bestStreak >= 25),
            "streak_50" to (state.bestStreak >= 50),
            "millionaire" to (state.money >= BigNumber(1_000_000)),
            "ten_millionaire" to (state.money >= BigNumber(10_000_000)),
            "hundred_millionaire" to (state.money >= BigNumber(100_000_000)),
            "billionaire" to (state.money >= BigNumber(1_000_000_000)),
            "prestige_1" to (state.prestigeCount >= 1),
            "prestige_5" to (state.prestigeCount >= 5),
            "prestige_10" to (state.prestigeCount >= 10),
            "prestige_25" to (state.prestigeCount >= 25),
            "ultra_1" to (state.ultraCount >= 1),
            "ultra_3" to (state.ultraCount >= 3),
            "ultra_10" to (state.ultraCount >= 10),
            "ultra_25" to (state.ultraCount >= 25),
            "playtime_1h" to (state.statistics.playtimeSeconds >= 3_600),
            "playtime_10h" to (state.statistics.playtimeSeconds >= 36_000),
            "playtime_50h" to (state.statistics.playtimeSeconds >= 180_000),
            "frenzy_5" to (state.bestStreak >= 5),
            "frenzy_10" to (state.bestStreak >= 10),
            "frenzy_25" to (state.bestStreak >= 25),
            "frenzy_50" to (state.bestStreak >= 50)
        )
        val rewards = mapOf(
            "correct_10" to 10L, "correct_50" to 20L, "correct_100" to 40L, "correct_500" to 100L,
            "streak_5" to 25L, "streak_10" to 50L, "streak_25" to 100L, "streak_50" to 250L,
            "millionaire" to 50L, "ten_millionaire" to 100L, "hundred_millionaire" to 200L, "billionaire" to 500L,
            "prestige_1" to 25L, "prestige_5" to 75L, "prestige_10" to 150L, "prestige_25" to 300L,
            "ultra_1" to 100L, "ultra_3" to 250L, "ultra_10" to 500L, "ultra_25" to 1_000L,
            "playtime_1h" to 25L, "playtime_10h" to 100L, "playtime_50h" to 300L,
            "frenzy_5" to 30L, "frenzy_10" to 75L, "frenzy_25" to 150L, "frenzy_50" to 350L
        )
        val newlyUnlocked = milestones.filter { (id, unlocked) -> unlocked && id !in state.achievements }
        if (newlyUnlocked.isEmpty()) return state
        val baseReward = newlyUnlocked.sumOf { (id, _) -> rewards[id] ?: 0L }
        val reward = calculateNebulaReward(state, baseReward)
        return state.copy(
            achievements = state.achievements + newlyUnlocked.map { it.first },
            nebula = state.nebula + BigNumber(reward),
            statistics = state.statistics.copy(nebulaEarned = addNebulaEarned(state.statistics.nebulaEarned, reward))
        )
    }

    fun claimChallenge(state: GameState, challengeId: String, rewardNebula: Long): Pair<GameState, Boolean> {
        val decision = ChallengeClaimRules.resolve(
            completedChallenges = state.completedChallenges,
            challengeId = challengeId,
            rewardNebula = rewardNebula,
            progress = challengeProgress(state)
        )
        if (!decision.accepted) return Pair(state, false)

        // Keep currency arithmetic and statistics mutation on Android; shared code
        // only decides whether this claim is valid and which reward may be applied.
        return Pair(
            state.copy(
                completedChallenges = state.completedChallenges + decision.challengeId,
                nebula = state.nebula + BigNumber(decision.rewardNebula),
                statistics = state.statistics.copy(
                    nebulaEarned = state.statistics.nebulaEarned + decision.rewardNebula
                )
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
