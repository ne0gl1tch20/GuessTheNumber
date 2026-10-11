package com.jarrlyyy.guessthenumber.domain.model

import kotlin.random.Random

enum class RandomEventType {
    CASH_BURST,
    NEBULA_RAIN,
    JACKPOT,
    STREAK_SURGE,
    RANGE_SHUFFLE,
    TAX_REFUND,
    COSMIC_GIFT
}

data class RandomEventResult(
    val type: RandomEventType,
    val state: GameState
)

/**
 * Applies a validated event selected by the Lua event script.
 * Lua never receives GameState or writes currencies/progression directly.
 */
class RandomEventEngine(
    private val rng: Random = Random.Default
) {
    fun apply(state: GameState, type: RandomEventType): RandomEventResult {
        val updated = when (type) {
            RandomEventType.CASH_BURST -> {
                val bonus = maxOf(BigNumber(100), state.money * BigNumber(0.10))
                state.copy(
                    money = state.money + bonus,
                    statistics = state.statistics.copy(
                        moneyEarned = state.statistics.moneyEarned + bonus,
                        randomEventsTriggered = state.statistics.randomEventsTriggered + 1
                    )
                )
            }
            RandomEventType.NEBULA_RAIN -> {
                val bonus = rng.nextLong(10L, 51L)
                state.copy(
                    nebula = state.nebula + BigNumber(bonus),
                    statistics = state.statistics.copy(
                        nebulaEarned = state.statistics.nebulaEarned + bonus,
                        randomEventsTriggered = state.statistics.randomEventsTriggered + 1
                    )
                )
            }
            RandomEventType.JACKPOT -> {
                val bonus = BigNumber(1000) * BigNumber(state.currentRangeMax)
                state.copy(
                    money = state.money + bonus,
                    statistics = state.statistics.copy(
                        moneyEarned = state.statistics.moneyEarned + bonus,
                        randomEventsTriggered = state.statistics.randomEventsTriggered + 1
                    )
                )
            }
            RandomEventType.STREAK_SURGE -> {
                val newStreak = state.streak + 2
                state.copy(
                    streak = newStreak,
                    bestStreak = maxOf(state.bestStreak, newStreak),
                    statistics = state.statistics.copy(
                        randomEventsTriggered = state.statistics.randomEventsTriggered + 1
                    )
                )
            }
            RandomEventType.RANGE_SHUFFLE -> {
                val newTarget = rng.nextLong(state.currentRangeMin, state.currentRangeMax + 1)
                state.copy(
                    targetNumber = newTarget,
                    statistics = state.statistics.copy(
                        randomEventsTriggered = state.statistics.randomEventsTriggered + 1
                    )
                )
            }
            RandomEventType.TAX_REFUND -> {
                val refund = maxOf(BigNumber(100), state.statistics.moneySpent * BigNumber(0.05))
                state.copy(
                    money = state.money + refund,
                    statistics = state.statistics.copy(
                        moneyEarned = state.statistics.moneyEarned + refund,
                        randomEventsTriggered = state.statistics.randomEventsTriggered + 1
                    )
                )
            }
            RandomEventType.COSMIC_GIFT -> {
                val bonus = 100L
                state.copy(
                    nebula = state.nebula + BigNumber(bonus),
                    statistics = state.statistics.copy(
                        nebulaEarned = state.statistics.nebulaEarned + bonus,
                        randomEventsTriggered = state.statistics.randomEventsTriggered + 1
                    )
                )
            }
        }

        return RandomEventResult(type, updated.copy(lastRandomEventId = type.name.lowercase()))
    }
}
