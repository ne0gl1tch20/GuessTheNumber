package com.jarrlyyy.guessthenumber.shared.domain

/** Result of the platform-neutral backward-clock check. */
data class TimeTravelCheckResult(
    val updatedState: GameState,
    val timeTravelDetected: Boolean,
    val timeDifferenceSeconds: Long
)

/**
 * Pure time-travel policy. The platform supplies the current timestamp and
 * decides how to log/report the result.
 */
object TimeTravelPolicy {
    const val PENALTY_DURATION_MILLIS = 24L * 60L * 60L * 1000L

    fun check(state: GameState, nowMillis: Long): TimeTravelCheckResult {
        val lastSave = state.lastSaveTimestamp
        if (nowMillis < lastSave) {
            val diffSeconds = (lastSave - nowMillis) / 1000L
            val penalizedState = state.copy(
                autoClickerActive = false,
                timeTravelPenaltyUntil = nowMillis + PENALTY_DURATION_MILLIS,
                lastSaveTimestamp = nowMillis
            )
            return TimeTravelCheckResult(penalizedState, true, diffSeconds)
        }

        return TimeTravelCheckResult(
            updatedState = state.copy(lastSaveTimestamp = nowMillis),
            timeTravelDetected = false,
            timeDifferenceSeconds = 0L
        )
    }
}
