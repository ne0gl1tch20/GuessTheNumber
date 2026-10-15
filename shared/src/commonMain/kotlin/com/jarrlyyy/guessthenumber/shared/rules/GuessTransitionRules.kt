package com.jarrlyyy.guessthenumber.shared.rules

/**
 * Deterministic outcome of the guess-transition inputs needed by the production engine.
 *
 * The platform supplies random values and retains ownership of currency math and full
 * GameState mutation. Keeping those values explicit makes the decision reproducible in
 * common tests without changing the platform RNG implementation.
 */
data class GuessTransitionDecision(
    val stateTransition: GuessStateTransition,
    val isCritical: Boolean,
    val nextTarget: Long?
)

object GuessTransitionRules {
    /**
     * Resolve the deterministic parts of a guess transition.
     *
     * A correct guess requires the critical roll and next target that the platform
     * generated. Incorrect guesses never consume or require randomness.
     */
    fun resolve(
        stateTransition: GuessStateTransition,
        criticalChance: Double,
        criticalRoll: Double?,
        nextTarget: Long?,
        nextRangeMin: Long,
        nextRangeMax: Long
    ): GuessTransitionDecision {
        if (stateTransition.feedback != GuessFeedback.CORRECT) {
            return GuessTransitionDecision(
                stateTransition = stateTransition,
                isCritical = false,
                nextTarget = null
            )
        }

        require(criticalRoll != null && criticalRoll >= 0.0 && criticalRoll < 1.0) {
            "A correct guess requires a critical roll in [0, 1)"
        }
        require(nextTarget != null &&
            GuessStateRules.isTargetWithinRange(nextTarget, nextRangeMin, nextRangeMax)
        ) {
            "A correct guess requires a next target within the next range"
        }

        return GuessTransitionDecision(
            stateTransition = stateTransition,
            isCritical = criticalRoll < criticalChance,
            nextTarget = nextTarget
        )
    }
}
