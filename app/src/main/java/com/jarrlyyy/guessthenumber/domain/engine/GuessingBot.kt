package com.jarrlyyy.guessthenumber.domain.engine

import kotlin.math.max
import kotlin.math.min

/**
 * Feedback-driven guessing bot.
 *
 * The bot never reads the target number. It narrows an interval using only
 * TOO_LOW / TOO_HIGH / CORRECT feedback.
 */
class GuessingBot {
    private var lowerBound = 1L
    private var upperBound = 100L

    fun reset(min: Long, max: Long) {
        lowerBound = min.coerceAtLeast(1L)
        upperBound = max.coerceAtLeast(lowerBound)
    }

    fun nextGuess(rangeMin: Long, rangeMax: Long): Long {
        if (rangeMin != lowerBound || rangeMax != upperBound) {
            lowerBound = max(rangeMin, lowerBound).coerceAtLeast(1L)
            upperBound = min(rangeMax, upperBound).coerceAtLeast(lowerBound)
            if (lowerBound > upperBound) reset(rangeMin, rangeMax)
        }
        return lowerBound + ((upperBound - lowerBound) / 2L)
    }

    fun observeGuess(guess: Long, feedback: GuessFeedback) {
        when (feedback) {
            GuessFeedback.TOO_LOW -> lowerBound = max(lowerBound, guess + 1L)
            GuessFeedback.TOO_HIGH -> upperBound = min(upperBound, guess - 1L)
            GuessFeedback.CORRECT -> Unit
        }

        if (lowerBound > upperBound) reset(1L, 100L)
    }
}
