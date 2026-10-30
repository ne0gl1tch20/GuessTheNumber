package com.jarrlyyy.guessthenumber.shared.rules

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GuessRateLimitRulesTest {
    @Test
    fun firstGuessIsNeverRateLimited() {
        assertFalse(GuessRateLimitRules.isRateLimited(lastSubmissionNanos = 0L, nowNanos = 1L))
    }

    @Test
    fun submissionInsideFiftyMillisecondWindowIsBlocked() {
        assertTrue(
            GuessRateLimitRules.isRateLimited(
                lastSubmissionNanos = 1_000_000_000L,
                nowNanos = 1_049_999_999L
            )
        )
    }

    @Test
    fun submissionAtExactFiftyMillisecondBoundaryIsAllowed() {
        assertFalse(
            GuessRateLimitRules.isRateLimited(
                lastSubmissionNanos = 1_000_000_000L,
                nowNanos = 1_050_000_000L
            )
        )
    }

    @Test
    fun submissionAfterWindowIsAllowed() {
        assertFalse(
            GuessRateLimitRules.isRateLimited(
                lastSubmissionNanos = 1_000_000_000L,
                nowNanos = 1_100_000_000L
            )
        )
    }

    @Test
    fun monotonicClockRegressionFailsClosed() {
        assertTrue(
            GuessRateLimitRules.isRateLimited(
                lastSubmissionNanos = 1_000_000_000L,
                nowNanos = 999_999_999L
            )
        )
    }

    @Test
    fun nonPositiveConfiguredIntervalDisablesLimiter() {
        assertFalse(
            GuessRateLimitRules.isRateLimited(
                lastSubmissionNanos = 1_000_000_000L,
                nowNanos = 1_000_000_001L,
                minimumIntervalNanos = 0L
            )
        )
    }
}
