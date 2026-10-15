package com.jarrlyyy.guessthenumber.shared.domain

/**
 * Pure eligibility and reward validation for weekly and builder challenges.
 * State mutation and Nebula/save handling stay in the platform game engine.
 */
data class ChallengeProgress(
    val correctGuesses: Long,
    val bestStreak: Long,
    val money: Long,
    val totalGuesses: Long,
    val prestigeCount: Long,
    val ultraCount: Long,
    val playtimeSeconds: Long
)

object ChallengeRewardRules {
    fun isValidReward(challengeId: String, rewardNebula: Long, progress: ChallengeProgress): Boolean {
        if (rewardNebula <= 0L) return false

        if (challengeId.startsWith("weekly_")) {
            val kind = challengeId.substringAfterLast('_')
            val expectedReward = when (kind) {
                "correct" -> 75L
                "streak" -> 90L
                "money" -> 100L
                "guesses" -> 80L
                "prestige" -> 125L
                "ultra" -> 175L
                "frenzy" -> 110L
                "playtime" -> 95L
                else -> return false
            }
            if (rewardNebula != expectedReward) return false
            val current = when (kind) {
                "correct" -> progress.correctGuesses
                "streak", "frenzy" -> progress.bestStreak
                "money" -> progress.money
                "guesses" -> progress.totalGuesses
                "prestige" -> progress.prestigeCount
                "ultra" -> progress.ultraCount
                "playtime" -> progress.playtimeSeconds
                else -> return false
            }
            val target = when (kind) {
                "correct" -> 75L
                "streak" -> 15L
                "money" -> 10_000_000L
                "guesses" -> 250L
                "prestige" -> 2L
                "ultra" -> 1L
                "frenzy" -> 25L
                "playtime" -> 3_600L
                else -> return false
            }
            return current >= target
        }

        if (challengeId.startsWith("builder_")) {
            val parts = challengeId.removePrefix("builder_").split('_')
            if (parts.size < 2) return false
            val kind = parts[0]
            val target = parts[1].toLongOrNull() ?: return false
            val mutators = parts.drop(2)
            if (target < 1L || mutators.any { it !in MutatorRules.ids }) return false
            if (rewardNebula != 50L + mutators.size * 50L) return false
            val current = when (kind) {
                "correct" -> progress.correctGuesses
                "streak" -> progress.bestStreak
                "guesses" -> progress.totalGuesses
                else -> progress.money
            }
            return current >= target
        }

        return false
    }
}