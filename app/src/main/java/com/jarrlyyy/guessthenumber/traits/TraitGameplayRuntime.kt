package com.jarrlyyy.guessthenumber.traits

import com.jarrlyyy.guessthenumber.shared.domain.TraitGameplayRules
import com.jarrlyyy.guessthenumber.domain.engine.GuessResult
import com.jarrlyyy.guessthenumber.domain.model.GameState
import java.time.LocalDate

/**
 * Applies approved trait effects to the authoritative guess result.
 *
 * Trait modules cannot change GameState directly. The runtime calculates bounded,
 * deterministic modifiers and returns a new result for the existing save pipeline.
 */
object TraitGameplayRuntime {
    private fun registry(): TraitRegistry = ActiveTraitCatalog.current()

    fun apply(
        stateBefore: GameState,
        guess: Long,
        result: GuessResult,
        activeSeasonId: String = TraitRewardPolicy.currentSeasonId(LocalDate.now())
    ): GuessResult {
        val activeRegistry = registry()
        val activation = TraitActivationEngine(activeRegistry).evaluate(
            TraitActivationContext(
                state = stateBefore,
                playerLevel = 1,
                activeSeasonId = activeSeasonId,
                discoveredTraitIds = stateBefore.discoveredTraitIds
            )
        )
        val active = activation.activeTraitIds
        val activeEffects = active.associateWith { traitId ->
            activeRegistry.find(traitId)?.effects?.mapTo(mutableSetOf()) { it.name }.orEmpty()
        }
        return TraitGameplayRules.apply(stateBefore, guess, result, active, activeEffects)
    }

    /** Validate and persist an arm choice; unknown traits/arms are rejected. */
    fun selectArm(state: GameState, traitId: String, armId: String): GameState {
        val definition = TraitGameplayCatalog.find(traitId) ?: error("Unknown gameplay trait: $traitId.")
        require(definition.arms.isNotEmpty()) { "Trait has no selectable arms." }
        require(TraitGameplayCatalog.allowsArm(traitId, armId)) { "Unsupported arm for $traitId." }
        val active = TraitActivationEngine(registry()).evaluate(
            TraitActivationContext(
                state = state,
                playerLevel = 1,
                activeSeasonId = TraitRewardPolicy.currentSeasonId(LocalDate.now()),
                discoveredTraitIds = state.discoveredTraitIds
            )
        ).activeTraitIds
        require(traitId in active) { "Trait is not currently active." }
        return state.copy(selectedTraitArms = state.selectedTraitArms + (traitId to armId))
    }

}
