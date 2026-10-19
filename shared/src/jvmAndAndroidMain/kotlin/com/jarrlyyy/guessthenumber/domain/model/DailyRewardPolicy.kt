package com.jarrlyyy.guessthenumber.domain.model

import java.time.LocalDate

/** Shared, deterministic daily Nebula reward schedule. A missed day resets the streak. */
object DailyRewardPolicy {
    private val rewards = listOf(5L, 10L, 15L, 20L, 25L, 30L, 50L)

    fun nextStreak(lastClaimDate: String, currentStreak: Int, today: String): Int {
        if (lastClaimDate == today) return currentStreak.coerceIn(1, rewards.size)
        val yesterday = runCatching { LocalDate.parse(today).minusDays(1).toString() }.getOrDefault("")
        return if (lastClaimDate == yesterday) {
            if (currentStreak >= rewards.size) 1 else (currentStreak + 1).coerceAtLeast(1)
        } else 1
    }

    fun rewardForStreak(streak: Int): Long = rewards[streak.coerceIn(1, rewards.size) - 1]

    fun nextReward(lastClaimDate: String, currentStreak: Int, today: String): Long =
        rewardForStreak(nextStreak(lastClaimDate, currentStreak, today))
}
