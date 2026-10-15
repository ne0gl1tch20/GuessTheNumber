package com.jarrlyyy.guessthenumber.desktop

import com.jarrlyyy.guessthenumber.domain.engine.GuessFeedback

/** Feedback-only binary-search bot; it never reads the hidden target. */
class DesktopGuessingBot {
    private var lowerBound = 1L
    private var upperBound = 100L

    fun reset(min: Long, max: Long) {
        lowerBound = min.coerceAtLeast(1L)
        upperBound = max.coerceAtLeast(lowerBound)
    }

    fun nextGuess(rangeMin: Long, rangeMax: Long): Long {
        if (rangeMin > lowerBound || rangeMax < upperBound) {
            lowerBound = maxOf(rangeMin, lowerBound).coerceAtLeast(1L)
            upperBound = minOf(rangeMax, upperBound).coerceAtLeast(lowerBound)
        }
        if (lowerBound > upperBound) reset(rangeMin, rangeMax)
        return lowerBound + ((upperBound - lowerBound) / 2L)
    }

    fun observeGuess(guess: Long, feedback: GuessFeedback) {
        when (feedback) {
            GuessFeedback.TOO_LOW -> lowerBound = maxOf(lowerBound, guess + 1L)
            GuessFeedback.TOO_HIGH -> upperBound = minOf(upperBound, guess - 1L)
            GuessFeedback.CORRECT -> Unit
        }
        if (lowerBound > upperBound) reset(1L, 100L)
    }
}
