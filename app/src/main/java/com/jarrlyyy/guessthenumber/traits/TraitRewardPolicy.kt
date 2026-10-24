package com.jarrlyyy.guessthenumber.traits

import com.jarrlyyy.guessthenumber.domain.model.GameState
import java.time.LocalDate

/**
 * Connects declarative traits to the existing Kotlin-owned save and reward flow.
 * Only the supported Nebula claim bonus is applied in this phase; other effect types
 * remain descriptive until their gameplay systems exist.
 */
object TraitRewardPolicy {
    fun updateDiscoveries(
        state: GameState,
        activeSeasonId: String = currentSeasonId()
    ): GameState {
        val newlyDiscovered = BuiltInTraits.registry().hidden()
            .filter { trait ->
                val requirements = trait.activation
                trait.id !in state.discoveredTraitIds &&
                    state.prestigeCount >= requirements.minimumPrestige.toLong() &&
                    state.correctGuesses >= requirements.minimumCorrectGuesses &&
                    (trait.category != TraitCategory.SEASONAL || trait.seasonId == activeSeasonId)
            }
            .mapTo(sortedSetOf()) { it.id }

        return if (newlyDiscovered.isEmpty()) state
        else state.copy(discoveredTraitIds = state.discoveredTraitIds + newlyDiscovered)
    }

    /** Returns a bounded bonus for one daily reward claim without mutating saved state. */
    fun nebulaBonusForClaim(
        state: GameState,
        activeSeasonId: String = currentSeasonId()
    ): Long {
        val stateWithDiscoveries = updateDiscoveries(state, activeSeasonId)
        return TraitActivationEngine(BuiltInTraits.registry()).evaluate(
            TraitActivationContext(
                state = stateWithDiscoveries,
                playerLevel = 1,
                activeSeasonId = activeSeasonId,
                discoveredTraitIds = stateWithDiscoveries.discoveredTraitIds
            )
        ).effects.bonusNebulaPerClaim.coerceIn(0L, TraitActivationEngine.MAX_NEBULA_BONUS)
    }

    /** Calendar mapping is deterministic and offline; no network or remote clock is needed. */
    fun currentSeasonId(date: LocalDate = LocalDate.now()): String = when (date.monthValue) {
        12, 1, 2 -> "winter"
        in 3..5 -> "spring"
        in 6..8 -> "summer"
        else -> "autumn"
    }
}
