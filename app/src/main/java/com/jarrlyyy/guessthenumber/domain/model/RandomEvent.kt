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

class RandomEventEngine(
    private val rng: Random = Random.Default
) {
    companion object {
        const val TRIGGER_CHANCE = 0.35
    }

    fun roll(state: GameState): RandomEventResult? {
        if (rng.nextDouble() >= TRIGGER_CHANCE) return null

        val type = when (rng.nextInt(100)) {
            in 0..29 -> RandomEventType.CASH_BURST
            in 30..49 -> RandomEventType.NEBULA_RAIN
            in 50..59 -> RandomEventType.JACKPOT
            in 60..69 -> RandomEventType.STREAK_SURGE
            in 70..79 -> RandomEventType.RANGE_SHUFFLE
            in 80..94 -> RandomEventType.TAX_REFUND
            else -> RandomEventType.COSMIC_GIFT
        }

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

        return RandomEventResult(type, updated)
    }
}
