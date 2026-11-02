package com.jarrlyyy.guessthenumber.domain.engine

import com.jarrlyyy.guessthenumber.data.logger.GameLogger
import com.jarrlyyy.guessthenumber.data.logger.LoggerCategory
import com.jarrlyyy.guessthenumber.data.logger.LogLevel
import com.jarrlyyy.guessthenumber.domain.model.GameState
import com.jarrlyyy.guessthenumber.shared.domain.AntiTimeTravelRules

object AntiTimeTravelService {
    data class TimeTravelCheckResult(
        val updatedState: GameState,
        val timeTravelDetected: Boolean,
        val timeDifferenceSeconds: Long
    )

    fun checkTimeTravel(state: GameState): TimeTravelCheckResult {
        val result = AntiTimeTravelRules.check(state)
        if (result.detected) {
            GameLogger.log(
                LogLevel.WARN,
                LoggerCategory.GAME,
                "TIME_TRAVEL_BACKWARD",
                "Clock rollback detected: device time is ${result.differenceSeconds} seconds behind the last save."
            )
        }
        return TimeTravelCheckResult(
            updatedState = result.updatedState,
            timeTravelDetected = result.detected,
            timeDifferenceSeconds = result.differenceSeconds
        )
    }
}
