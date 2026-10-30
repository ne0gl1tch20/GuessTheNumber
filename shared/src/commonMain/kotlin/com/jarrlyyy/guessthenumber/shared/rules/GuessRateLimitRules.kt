package com.jarrlyyy.guessthenumber.shared.rules

/**
 * Shared monotonic-clock policy for manual guess submissions.
 *
 * Hosts supply their own monotonic timestamp from platform-specific clocks.
 * This keeps the 50 ms anti-spam rule identical without importing platform APIs
 * into commonMain or relying on wall-clock time adjustments.
 */
object GuessRateLimitRules {
    const val MIN_INTERVAL_NANOS: Long = 50_000_000L

    fun isRateLimited(
        lastSubmissionNanos: Long?,
        nowNanos: Long,
        minimumIntervalNanos: Long = MIN_INTERVAL_NANOS
    ): Boolean {
        if (lastSubmissionNanos == null) return false
        if (minimumIntervalNanos <= 0L) return false
        if (nowNanos < lastSubmissionNanos) return true
        return nowNanos - lastSubmissionNanos < minimumIntervalNanos
    }
}
