package com.jarrlyyy.guessthenumber.domain.model

/**
 * The bounded, Kotlin-owned reward policy for a seasonal LiveOps claim.
 * Lua event scripts must never bypass this policy or mutate GameState directly.
 */
data class LiveOpsClaimDefinition(
    val id: String,
    val maxClaims: Int,
    val rewardNebula: Long,
    val permanentBoostPerClaim: Int,
    val minCorrectGuesses: Long,
    val minPrestigeCount: Long,
    val minUltraCount: Long
)

object LiveOpsClaimPolicy {
    /**
     * Returns the updated state when a claim is valid, or null when it is not.
     * Date-window and feature-flag checks remain the caller's responsibility.
     */
    fun apply(state: GameState, event: LiveOpsClaimDefinition): GameState? {
        if (event.id.isBlank() || event.maxClaims <= 0 || event.rewardNebula < 0 ||
            event.permanentBoostPerClaim < 0 || event.minCorrectGuesses < 0 ||
            event.minPrestigeCount < 0 || event.minUltraCount < 0
        ) return null

        // Refuse to settle rewards against corrupted progression/boost state. Clamping a
        // negative stored value here could turn an invalid save into a successful claim.
        if (state.correctGuesses < 0L || state.prestigeCount < 0L ||
            state.ultraCount < 0L || state.permanentEventBoosts < 0
        ) return null

        val claims = state.liveOpsClaims[event.id] ?: 0
        if (claims < 0 || claims >= event.maxClaims) return null

        // Long statistics must never wrap into a negative value after a reward.
        val nebulaEarned = state.statistics.nebulaEarned
        if (nebulaEarned < 0L || event.rewardNebula > Long.MAX_VALUE - nebulaEarned) return null

        val eligible = state.correctGuesses >= event.minCorrectGuesses &&
            state.prestigeCount >= event.minPrestigeCount &&
            state.ultraCount >= event.minUltraCount
        if (!eligible) return null

        return state.copy(
            nebula = state.nebula + BigNumber(event.rewardNebula),
            liveOpsClaims = state.liveOpsClaims + (event.id to (claims + 1)),
            permanentEventBoosts = (
                state.permanentEventBoosts.toLong() + event.permanentBoostPerClaim.toLong()
            ).coerceIn(0L, 25L).toInt(),
            statistics = state.statistics.copy(
                nebulaEarned = nebulaEarned + event.rewardNebula
            )
        )
    }
}
