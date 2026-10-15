package com.jarrlyyy.guessthenumber.shared.rules

/**
 * The platform-independent counters affected by one submitted guess.
 *
 * Reward calculation, persistence and target generation remain owned by the
 * existing game engine. This transition centralizes only the deterministic
 * guess feedback and streak/counter rules.
 */
data class GuessStateTransition(
    val feedback: GuessFeedback,
    val attempts: Long,
    val correctGuesses: Long,
    val streak: Int,
    val bestStreak: Int
)

object GuessStateRules {
    /** True when a guessing range is ordered and therefore safe to use. */
    fun isValidRange(rangeMin: Long, rangeMax: Long): Boolean =
        rangeMax >= rangeMin

    /** True when the hidden target is inside an already validated range. */
    fun isTargetWithinRange(targetNumber: Long, rangeMin: Long, rangeMax: Long): Boolean =
        isValidRange(rangeMin, rangeMax) && targetNumber in rangeMin..rangeMax

    fun applyGuess(
        targetNumber: Long,
        guess: Long,
        rangeMin: Long,
        rangeMax: Long,
        attempts: Long,
        correctGuesses: Long,
        streak: Int,
        bestStreak: Int
    ): GuessStateTransition {
        val feedback = GuessRules.resolveFeedback(
            targetNumber = targetNumber,
            guess = guess,
            rangeMin = rangeMin,
            rangeMax = rangeMax
        )
        val nextAttempts = attempts + 1
        return if (feedback == GuessFeedback.CORRECT) {
            val nextStreak = streak + 1
            GuessStateTransition(
                feedback = feedback,
                attempts = nextAttempts,
                correctGuesses = correctGuesses + 1,
                streak = nextStreak,
                bestStreak = maxOf(bestStreak, nextStreak)
            )
        } else {
            GuessStateTransition(
                feedback = feedback,
                attempts = nextAttempts,
                correctGuesses = correctGuesses,
                streak = 0,
                bestStreak = bestStreak
            )
        }
    }
}
