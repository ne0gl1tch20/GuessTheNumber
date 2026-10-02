package com.jarrlyyy.guessthenumber.domain.engine

import com.jarrlyyy.guessthenumber.data.logger.GameLogger
import com.jarrlyyy.guessthenumber.data.logger.LoggerCategory
import com.jarrlyyy.guessthenumber.data.logger.LogLevel
import com.jarrlyyy.guessthenumber.domain.model.GameState

object AntiTimeTravelService {
    // Tomodachi Life inspired anti-time-travel mechanic:
    // When device time is behind last save or tampered backward, apply 24-hour temporary consequence (pause auto-clicker / production).
    
    data class TimeTravelCheckResult(
        val updatedState: GameState,
        val timeTravelDetected: Boolean,
        val timeDifferenceSeconds: Long
    )

    fun checkTimeTravel(state: GameState): TimeTravelCheckResult {
        val now = System.currentTimeMillis()
        val lastSave = state.lastSaveTimestamp
        
        // 1. Backward tampering check only (device clock set behind last save).
        // Removed forward-jump heuristic checks to completely prevent false positives when device reboots or time syncs normally.
        if (now < lastSave) {
            val diffSeconds = (lastSave - now) / 1000
            GameLogger.log(LogLevel.WARN, LoggerCategory.GAME, "TIME_TRAVEL_BACKWARD", "Clock tampering detected! Device time is $diffSeconds seconds behind last save.")
            
            val penaltyUntil = now + (24 * 60 * 60 * 1000L)
            val penalizedState = state.copy(
                autoClickerActive = false,
                timeTravelPenaltyUntil = penaltyUntil,
                lastSaveTimestamp = now
            )
            return TimeTravelCheckResult(penalizedState, true, diffSeconds)
        }
        
        return TimeTravelCheckResult(state.copy(lastSaveTimestamp = now), false, 0)
    }
}
