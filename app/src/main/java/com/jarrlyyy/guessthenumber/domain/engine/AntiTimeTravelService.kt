package com.jarrlyyy.guessthenumber.domain.engine

import com.jarrlyyy.guessthenumber.data.logger.GameLogger
import com.jarrlyyy.guessthenumber.data.logger.LoggerCategory
import com.jarrlyyy.guessthenumber.data.logger.LogLevel
import com.jarrlyyy.guessthenumber.domain.model.GameState
import com.jarrlyyy.guessthenumber.shared.domain.TimeTravelPolicy

/**
 * Android logging/system-clock adapter around the shared time-travel policy.
 */
object AntiTimeTravelService {
    data class TimeTravelCheckResult(
        val updatedState: GameState,
        val timeTravelDetected: Boolean,
        val timeDifferenceSeconds: Long
    )

    fun checkTimeTravel(state: GameState): TimeTravelCheckResult {
        val now = System.currentTimeMillis()
        val result = TimeTravelPolicy.check(state, now)
        if (result.timeTravelDetected) {
            GameLogger.log(
                LogLevel.WARN,
                LoggerCategory.GAME,
                "TIME_TRAVEL_BACKWARD",
                "Clock tampering detected! Device time is ${result.timeDifferenceSeconds} seconds behind last save."
            )
        }
        return TimeTravelCheckResult(
            updatedState = result.updatedState,
            timeTravelDetected = result.timeTravelDetected,
            timeDifferenceSeconds = result.timeDifferenceSeconds
        )
    }
}
