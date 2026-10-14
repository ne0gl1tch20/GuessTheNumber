package com.jarrlyyy.guessthenumber.shared.rules

/**
 * Platform-independent feedback for a submitted number guess.
 *
 * Kept in commonMain so Android and the future Windows desktop app can use
 * the same rule without depending on Android APIs.
 */
enum class GuessFeedback {
    TOO_LOW,
    TOO_HIGH,
    CORRECT
}

object GuessRules {
    fun resolveFeedback(
        targetNumber: Long,
        guess: Long,
        rangeMin: Long,
        rangeMax: Long
    ): GuessFeedback {
        val clampedGuess = guess.coerceIn(rangeMin, rangeMax)
        return when {
            clampedGuess < targetNumber -> GuessFeedback.TOO_LOW
            clampedGuess > targetNumber -> GuessFeedback.TOO_HIGH
            else -> GuessFeedback.CORRECT
        }
    }
}
