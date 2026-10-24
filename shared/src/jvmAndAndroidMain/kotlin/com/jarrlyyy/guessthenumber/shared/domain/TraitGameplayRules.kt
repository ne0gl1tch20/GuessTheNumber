package com.jarrlyyy.guessthenumber.shared.domain

import com.jarrlyyy.guessthenumber.domain.engine.GuessFeedback
import com.jarrlyyy.guessthenumber.domain.engine.GuessResult
import com.jarrlyyy.guessthenumber.domain.model.BigNumber
import com.jarrlyyy.guessthenumber.domain.model.GameState

/**
 * Cross-platform deterministic gameplay effects for active trait definitions.
 * The host supplies effects from its validated registry; unknown effect strings are ignored.
 */
object TraitGameplayRules {
    fun activeGameplayTraitIds(state: GameState): Set<String> = buildSet {
        if (state.correctGuesses >= COMBO_MOMENTUM_UNLOCK) add("combo_momentum")
        if (state.correctGuesses >= RANGE_SCOUT_UNLOCK) add("range_scout")
        if (state.correctGuesses >= MISTAKE_INSURANCE_UNLOCK) add("mistake_insurance")
        if (state.correctGuesses >= RISK_REWARD_UNLOCK) add("risk_reward")
        if (state.correctGuesses >= STREAK_GUARD_UNLOCK) add("streak_guard")
    }

    fun apply(
        stateBefore: GameState,
        guess: Long,
        result: GuessResult,
        activeTraitIds: Set<String>,
        activeEffects: Map<String, Set<String>> = emptyMap()
    ): GuessResult {
        if (guess !in stateBefore.currentRangeMin..stateBefore.currentRangeMax) return result

        val effectTypes = activeTraitIds.flatMapTo(mutableSetOf()) { activeEffects[it].orEmpty() }
        val hasComboReward = "combo_momentum" in activeTraitIds || "COMBO_REWARD" in effectTypes
        val hasRangeHint = "range_scout" in activeTraitIds || "RANGE_HINT" in effectTypes
        val hasMissInsurance = "mistake_insurance" in activeTraitIds || "MISS_PENALTY_REDUCTION" in effectTypes
        val hasRiskReward = "risk_reward" in activeTraitIds || "RISK_REWARD" in effectTypes
        val hasStreakProtection = "streak_guard" in activeTraitIds || "STREAK_PROTECTION" in effectTypes

        var nextState = result.newState
        var reward = result.reward
        if (result.feedback == GuessFeedback.CORRECT) {
            var multiplier = 1.0
            if (hasComboReward) {
                val burst = "combo_momentum" in activeTraitIds && stateBefore.selectedTraitArms["combo_momentum"] == "burst"
                val perStreak = if (burst) BURST_COMBO_BONUS_PER_STREAK else COMBO_BONUS_PER_STREAK
                val cap = if (burst) BURST_MAX_COMBO_BONUS else MAX_COMBO_BONUS
                multiplier += (stateBefore.streak * perStreak).coerceAtMost(cap)
            }
            if (hasRiskReward) {
                multiplier += if ("risk_reward" in activeTraitIds && stateBefore.selectedTraitArms["risk_reward"] == "high_risk") HIGH_RISK_SUCCESS_BONUS else RISK_REWARD_SUCCESS_BONUS
            }
            if (multiplier > 1.0) {
                val adjustedReward = reward * BigNumber(multiplier)
                val bonus = adjustedReward - reward
                reward = adjustedReward
                nextState = nextState.copy(
                    money = nextState.money + bonus,
                    statistics = nextState.statistics.copy(moneyEarned = nextState.statistics.moneyEarned + bonus)
                )
            }
        } else {
            val originalPenalty = (stateBefore.money - nextState.money).max(BigNumber.ZERO)
            val refundRate = if ("mistake_insurance" in activeTraitIds && stateBefore.selectedTraitArms["mistake_insurance"] == "careful") CAREFUL_REFUND_RATE else MISTAKE_REFUND_RATE
            val refund = if (hasMissInsurance) originalPenalty * BigNumber(refundRate) else BigNumber.ZERO
            val penaltyRate = if ("risk_reward" in activeTraitIds && stateBefore.selectedTraitArms["risk_reward"] == "high_risk") HIGH_RISK_MISS_PENALTY_RATE else RISK_MISS_PENALTY_RATE
            val extraPenalty = if (hasRiskReward) originalPenalty * BigNumber(penaltyRate) else BigNumber.ZERO
            if (refund > BigNumber.ZERO || extraPenalty > BigNumber.ZERO) {
                nextState = nextState.copy(money = (nextState.money + refund - extraPenalty).max(BigNumber.ZERO))
            }
            if (hasRangeHint) {
                nextState = when (result.feedback) {
                    GuessFeedback.TOO_LOW -> if (guess < nextState.currentRangeMax) nextState.copy(currentRangeMin = maxOf(nextState.currentRangeMin, guess + 1L)) else nextState
                    GuessFeedback.TOO_HIGH -> if (guess > nextState.currentRangeMin) nextState.copy(currentRangeMax = minOf(nextState.currentRangeMax, guess - 1L)) else nextState
                    else -> nextState
                }
            }
            if (hasStreakProtection) nextState = nextState.copy(streak = stateBefore.streak)
        }
        return result.copy(reward = reward, newStreak = nextState.streak, newState = nextState)
    }

    const val COMBO_MOMENTUM_UNLOCK = 25L
    const val RANGE_SCOUT_UNLOCK = 10L
    const val MISTAKE_INSURANCE_UNLOCK = 50L
    const val RISK_REWARD_UNLOCK = 100L
    const val STREAK_GUARD_UNLOCK = 50L
    const val COMBO_BONUS_PER_STREAK = 0.02
    const val MAX_COMBO_BONUS = 0.20
    const val BURST_COMBO_BONUS_PER_STREAK = 0.03
    const val BURST_MAX_COMBO_BONUS = 0.25
    const val RISK_REWARD_SUCCESS_BONUS = 0.25
    const val RISK_MISS_PENALTY_RATE = 0.10
    const val HIGH_RISK_SUCCESS_BONUS = 0.40
    const val HIGH_RISK_MISS_PENALTY_RATE = 0.20
    const val MISTAKE_REFUND_RATE = 0.50
    const val CAREFUL_REFUND_RATE = 0.75
}
