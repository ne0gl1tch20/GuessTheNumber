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
    fun canClaim(
        completedChallenges: Set<String>,
        challengeId: String,
        rewardNebula: Long,
        progress: ChallengeProgress
    ): Boolean = challengeId !in completedChallenges && isValidReward(challengeId, rewardNebula, progress)

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
            val payload = challengeId.removePrefix("builder_")
            val firstSeparator = payload.indexOf('_')
            if (firstSeparator <= 0) return false
            val secondSeparator = payload.indexOf('_', firstSeparator + 1)
            val kind = payload.substring(0, firstSeparator)
            val targetText = if (secondSeparator < 0) payload.substring(firstSeparator + 1)
                else payload.substring(firstSeparator + 1, secondSeparator)
            val target = targetText.toLongOrNull() ?: return false
            if (target < 1L) return false

            var remaining = if (secondSeparator < 0) "" else payload.substring(secondSeparator + 1)
            val mutators = mutableListOf<String>()
            while (remaining.isNotEmpty()) {
                val matched = MutatorRules.ids.sortedByDescending { it.length }
                    .firstOrNull { remaining == it || remaining.startsWith("${it}_") } ?: return false
                mutators.add(matched)
                remaining = remaining.removePrefix(matched).removePrefix("_")
            }
            if (mutators.distinct().size != mutators.size) return false
            if (rewardNebula != 50L + mutators.size * 50L) return false
            val current = when (kind) {
                "correct" -> progress.correctGuesses
                "streak" -> progress.bestStreak
                "guesses" -> progress.totalGuesses
                "money" -> progress.money
                else -> return false
            }
            return current >= target
        }

        return false
    }
}

/**
 * Shared deterministic decision for a single challenge claim.
 *
 * The platform applies the accepted reward to its currency and statistics models.
 * Rejected or already-completed claims carry no reward to apply.
 */
data class ChallengeClaimDecision(
    val accepted: Boolean,
    val challengeId: String,
    val rewardNebula: Long
)

object ChallengeClaimRules {
    fun resolve(
        completedChallenges: Set<String>,
        challengeId: String,
        rewardNebula: Long,
        progress: ChallengeProgress
    ): ChallengeClaimDecision {
        val accepted = ChallengeRewardRules.canClaim(
            completedChallenges = completedChallenges,
            challengeId = challengeId,
            rewardNebula = rewardNebula,
            progress = progress
        )
        return ChallengeClaimDecision(
            accepted = accepted,
            challengeId = challengeId,
            rewardNebula = if (accepted) rewardNebula else 0L
        )
    }
}
