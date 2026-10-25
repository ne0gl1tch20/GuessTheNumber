package com.jarrlyyy.guessthenumber.traits

import com.jarrlyyy.guessthenumber.domain.model.GameState
import java.time.LocalDate

/**
 * Connects validated declarative traits to Kotlin-owned saves and rewards.
 * Remote data can describe supported effects but cannot mutate state or execute code.
 */
object TraitRewardPolicy {
    fun updateDiscoveries(
        state: GameState,
        activeSeasonId: String = currentSeasonId(),
        registry: TraitRegistry = ActiveTraitCatalog.current()
    ): GameState {
        val discovered = state.discoveredTraitIds.toMutableSet()
        var changed: Boolean
        do {
            changed = false
            registry.hidden().forEach { trait ->
                val requirements = trait.activation
                val eligible = trait.id !in discovered &&
                    trait.dependencies.all { it in discovered } &&
                    state.prestigeCount >= requirements.minimumPrestige.toLong() &&
                    state.correctGuesses >= requirements.minimumCorrectGuesses &&
                    state.bestStreak >= requirements.minimumBestStreak &&
                    (trait.category != TraitCategory.SEASONAL || trait.seasonId == activeSeasonId)
                if (eligible && discovered.add(trait.id)) changed = true
            }
        } while (changed)

        return if (discovered == state.discoveredTraitIds) state
        else state.copy(discoveredTraitIds = discovered)
    }

    /** Returns a bounded bonus for one reward claim without mutating saved state. */
    fun nebulaBonusForClaim(
        state: GameState,
        activeSeasonId: String = currentSeasonId(),
        registry: TraitRegistry = ActiveTraitCatalog.current()
    ): Long {
        val stateWithDiscoveries = updateDiscoveries(state, activeSeasonId, registry)
        return TraitActivationEngine(registry).evaluate(
            TraitActivationContext(
                state = stateWithDiscoveries,
                playerLevel = 1,
                activeSeasonId = activeSeasonId,
                discoveredTraitIds = stateWithDiscoveries.discoveredTraitIds
            )
        ).effects.bonusNebulaPerClaim.coerceIn(0L, TraitActivationEngine.MAX_NEBULA_BONUS)
    }

    /** Calendar mapping is deterministic and offline; no network or remote clock is needed. */
    fun currentSeasonId(date: LocalDate = LocalDate.now()): String =
        TraitSeasonPolicy.seasonFor(date).id
}
