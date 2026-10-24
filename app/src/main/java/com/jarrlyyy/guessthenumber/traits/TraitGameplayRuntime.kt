package com.jarrlyyy.guessthenumber.traits

import com.jarrlyyy.guessthenumber.domain.engine.GuessFeedback
import com.jarrlyyy.guessthenumber.domain.engine.GuessResult
import com.jarrlyyy.guessthenumber.domain.model.BigNumber
import com.jarrlyyy.guessthenumber.domain.model.GameState
import java.time.LocalDate

/**
 * Applies approved trait effects to the authoritative guess result.
 *
 * Trait modules cannot change GameState directly. The runtime calculates bounded,
 * deterministic modifiers and returns a new result for the existing save pipeline.
 */
object TraitGameplayRuntime {
    private val registry by lazy { BuiltInTraits.registry() }

    fun apply(
        stateBefore: GameState,
        guess: Long,
        result: GuessResult,
        activeSeasonId: String = TraitRewardPolicy.currentSeasonId(LocalDate.now())
    ): GuessResult {
        if (guess !in stateBefore.currentRangeMin..stateBefore.currentRangeMax) return result

        val active = TraitActivationEngine(registry).evaluate(
            TraitActivationContext(
                state = stateBefore,
                playerLevel = 1,
                activeSeasonId = activeSeasonId,
                discoveredTraitIds = stateBefore.discoveredTraitIds
            )
        ).activeTraitIds
        var nextState = result.newState
        var reward = result.reward

        if (result.feedback == GuessFeedback.CORRECT) {
            var multiplier = 1.0
            if ("combo_momentum" in active) {
                multiplier += (stateBefore.streak * COMBO_BONUS_PER_STREAK).coerceAtMost(MAX_COMBO_BONUS)
            }
            if ("risk_reward" in active) multiplier += RISK_REWARD_SUCCESS_BONUS
            if (multiplier > 1.0) {
                val adjustedReward = reward * BigNumber(multiplier)
                val bonus = adjustedReward - reward
                reward = adjustedReward
                nextState = nextState.copy(
                    money = nextState.money + bonus,
                    statistics = nextState.statistics.copy(
                        moneyEarned = nextState.statistics.moneyEarned + bonus
                    )
                )
            }
        } else {
            val originalPenalty = (stateBefore.money - nextState.money).max(BigNumber.ZERO)
            var refund = BigNumber.ZERO
            if ("mistake_insurance" in active) refund = originalPenalty * BigNumber(MISTAKE_REFUND_RATE)
            var extraPenalty = BigNumber.ZERO
            if ("risk_reward" in active) extraPenalty = originalPenalty * BigNumber(RISK_MISS_PENALTY_RATE)
            if (refund > BigNumber.ZERO || extraPenalty > BigNumber.ZERO) {
                nextState = nextState.copy(
                    money = (nextState.money + refund - extraPenalty).max(BigNumber.ZERO)
                )
            }

            if ("range_scout" in active) {
                nextState = when (result.feedback) {
                    GuessFeedback.TOO_LOW -> if (guess < nextState.currentRangeMax) {
                        nextState.copy(currentRangeMin = maxOf(nextState.currentRangeMin, guess + 1L))
                    } else nextState
                    GuessFeedback.TOO_HIGH -> if (guess > nextState.currentRangeMin) {
                        nextState.copy(currentRangeMax = minOf(nextState.currentRangeMax, guess - 1L))
                    } else nextState
                    else -> nextState
                }
            }
            if ("streak_guard" in active) {
                nextState = nextState.copy(streak = stateBefore.streak)
            }
        }

        return result.copy(
            reward = reward,
            newStreak = nextState.streak,
            newState = nextState
        )
    }

    const val COMBO_BONUS_PER_STREAK = 0.02
    const val MAX_COMBO_BONUS = 0.20
    const val RISK_REWARD_SUCCESS_BONUS = 0.25
    const val RISK_MISS_PENALTY_RATE = 0.10
    const val MISTAKE_REFUND_RATE = 0.50
}
