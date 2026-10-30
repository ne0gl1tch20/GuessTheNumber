package com.jarrlyyy.guessthenumber.domain.engine

/** Shared arcade outcome rules used by Android and Windows hosts. */
object ArcadeRewardRules {
    const val TOO_EARLY_REACTION_SENTINEL = 9_999L

    fun isReactionTestRewardEligible(reactionTimeMillis: Long): Boolean =
        reactionTimeMillis >= 0 && reactionTimeMillis != TOO_EARLY_REACTION_SENTINEL
}
