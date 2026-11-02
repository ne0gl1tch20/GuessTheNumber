package com.jarrlyyy.guessthenumber.shared.domain

import com.jarrlyyy.guessthenumber.domain.model.GameState

/**
 * Platform-neutral clock rollback policy shared by Android and Windows.
 * Forward clock adjustments are accepted; only a clock earlier than the last
 * persisted timestamp triggers the temporary production pause.
 */
object AntiTimeTravelRules {
    const val PENALTY_MILLIS: Long = 24L * 60L * 60L * 1000L

    data class Result(
        val updatedState: GameState,
        val detected: Boolean,
        val differenceSeconds: Long
    )

    fun check(
        state: GameState,
        nowMillis: Long = System.currentTimeMillis()
    ): Result {
        val lastSave = state.lastSaveTimestamp
        if (nowMillis < lastSave) {
            val difference = (lastSave - nowMillis) / 1_000L
            return Result(
                updatedState = state.copy(
                    autoClickerActive = false,
                    timeTravelPenaltyUntil = nowMillis + PENALTY_MILLIS,
                    lastSaveTimestamp = nowMillis
                ),
                detected = true,
                differenceSeconds = difference
            )
        }
        return Result(
            updatedState = state.copy(lastSaveTimestamp = nowMillis),
            detected = false,
            differenceSeconds = 0L
        )
    }
}
