package com.jarrlyyy.guessthenumber.shared.rules

/**
 * Platform-independent slice of production state required to resolve one guess.
 *
 * Currency, upgrades, persistence and platform services intentionally remain outside
 * this model. Android can map this slice to and from its existing GameState without
 * changing the serialized save format.
 */
data class GuessCoreState(
    val targetNumber: Long,
    val rangeMin: Long,
    val rangeMax: Long,
    val attempts: Long,
    val correctGuesses: Long,
    val streak: Int,
    val bestStreak: Int
)

/** Deterministic guess result plus the updated shared state slice. */
data class GuessCoreTransition(
    val feedback: GuessFeedback,
    val nextState: GuessCoreState,
    val stateTransition: GuessStateTransition
) {
    val attempts: Long get() = nextState.attempts
    val correctGuesses: Long get() = nextState.correctGuesses
    val streak: Int get() = nextState.streak
    val bestStreak: Int get() = nextState.bestStreak
}
