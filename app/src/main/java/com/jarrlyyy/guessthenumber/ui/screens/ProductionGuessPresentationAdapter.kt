package com.jarrlyyy.guessthenumber.ui.screens

import com.jarrlyyy.guessthenumber.domain.model.GameState
import com.jarrlyyy.guessthenumber.shared.domain.GuessGameStatus
import com.jarrlyyy.guessthenumber.shared.ui.SharedGuessPresentationState

/**
 * Maps the Android production state into the shared UI presentation contract.
 * Production gameplay remains continuous, so the shared pilot's terminal
 * solved flag is always false here.
 */
internal object ProductionGuessPresentationAdapter {
    fun from(
        state: GameState,
        status: GuessGameStatus
    ): SharedGuessPresentationState = SharedGuessPresentationState(
        rangeMin = state.currentRangeMin,
        rangeMax = state.currentRangeMax,
        guessCount = state.attempts,
        status = status,
        solved = false
    )
}
