package com.jarrlyyy.guessthenumber.domain.engine

/** Shared arcade outcome rules used by Android and Windows hosts. */
object ArcadeRewardRules {
    const val TOO_EARLY_REACTION_SENTINEL = 9_999L

    fun isReactionTestRewardEligible(reactionTimeMillis: Long): Boolean =
        reactionTimeMillis >= 0 && reactionTimeMillis != TOO_EARLY_REACTION_SENTINEL

    /** One Nebula is the guaranteed completion reward; time and score add capped bonuses. */
    fun calculateNebulaReward(playSeconds: Long, score: Int): Long {
        val timeBonus = (playSeconds.coerceAtLeast(0L) / 30L).coerceAtMost(4L)
        val scoreBonus = (score.coerceAtLeast(0) / 5).coerceAtMost(5).toLong()
        return (1L + timeBonus + scoreBonus).coerceAtMost(10L)
    }

    /** Small Money bonus follows the same validated score/time inputs on both platforms. */
    fun calculateMoneyReward(playSeconds: Long, score: Int): Long =
        (score.coerceAtLeast(0).toLong() * 300L + playSeconds.coerceAtLeast(0L).coerceAtMost(600L) * 25L)
            .coerceAtMost(1_000_000L)
}
