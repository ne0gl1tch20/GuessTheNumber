package com.jarrlyyy.guessthenumber.shared.ui

import com.jarrlyyy.guessthenumber.shared.domain.GuessGameState
import com.jarrlyyy.guessthenumber.shared.domain.GuessGameStatus

/**
 * Platform-neutral destinations for the shared core-game experience.
 *
 * These stable route IDs are deliberately limited to the first shared feature
 * slice. Android and desktop can map them to their own navigation systems.
 */
enum class SharedGameDestination(val route: String) {
    PLAY("play"),
    UPGRADES("upgrade"),
    SHOP("shop"),
    MORE("more");

    companion object {
        fun fromRoute(route: String): SharedGameDestination? =
            entries.firstOrNull { it.route == route }
    }
}

/** Intent emitted by shared game UI; platform hosts decide how to execute it. */
sealed interface SharedGameAction {
    data class SubmitGuess(val guess: Long?) : SharedGameAction
    data object StartNewGame : SharedGameAction
    data class Navigate(val destination: SharedGameDestination) : SharedGameAction
}

/**
 * Presentation-ready, UI-neutral snapshot of the shared guessing state.
 * Localized wording and platform services remain outside common domain code.
 */
data class SharedGuessPresentationState(
    val rangeMin: Long,
    val rangeMax: Long,
    val guessCount: Long,
    val status: GuessGameStatus,
    val solved: Boolean
) {
    companion object {
        fun from(state: GuessGameState) = SharedGuessPresentationState(
            rangeMin = state.rangeMin,
            rangeMax = state.rangeMax,
            guessCount = state.attempts,
            status = state.status,
            solved = state.solved
        )
    }
}
