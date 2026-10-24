package com.jarrlyyy.guessthenumber.traits

import com.jarrlyyy.guessthenumber.domain.model.GameState

/**
 * Evaluates the validated trait catalog against a read-only snapshot of the saved game.
 *
 * This engine only returns bounded effect intents. It never mutates GameState, awards
 * currency, executes scripts, or persists discoveries. Callers must explicitly persist
 * discovery and apply any future gameplay effect through the Kotlin game engine.
 */
data class TraitActivationContext(
    val state: GameState,
    val playerLevel: Int = 1,
    val activeSeasonId: String? = null,
    val discoveredTraitIds: Set<String> = emptySet()
)

data class TraitEffectSummary(
    val xpMultiplier: Double = 1.0,
    val bonusNebulaPerClaim: Long = 0L,
    val grantsStreakProtection: Boolean = false,
    val cosmeticBadgeTraitIds: Set<String> = emptySet()
)

data class TraitActivationResult(
    val activeTraitIds: Set<String>,
    val discoveredHiddenTraitIds: Set<String>,
    val effects: TraitEffectSummary
) {
    fun isActive(traitId: String): Boolean = traitId in activeTraitIds
}

/** Deterministic, offline-first activation for built-in and validated remote definitions. */
class TraitActivationEngine(
    private val registry: TraitRegistry
) {
    fun evaluate(context: TraitActivationContext): TraitActivationResult {
        val definitions = registry.all().associateBy { it.id }
        val eligible = definitions.values
            .filter { trait -> isEligible(trait, context) }
            .sortedBy { it.id }

        // Resolve conflicts deterministically: lexicographically first eligible trait wins.
        // Dependencies are checked again after conflict resolution so dependent traits cannot
        // remain active when a required trait was excluded.
        val accepted = linkedMapOf<String, TraitDefinition>()
        for (trait in eligible) {
            if (trait.conflicts.any { it in accepted }) continue
            if (accepted.values.any { trait.id in it.conflicts }) continue
            accepted[trait.id] = trait
        }

        var changed: Boolean
        do {
            changed = false
            val removeIds = accepted.values
                .filter { trait -> trait.dependencies.any { it !in accepted } }
                .map { it.id }
            removeIds.forEach { id ->
                accepted.remove(id)
                changed = true
            }
        } while (changed)

        val active = accepted.values
        val xpTraitCount = active.count { TraitEffect.XP_MULTIPLIER in it.effects }
        val nebulaTraitCount = active.count { TraitEffect.NEBULA_BONUS in it.effects }
        val resultEffects = TraitEffectSummary(
            // Intentionally conservative until a gameplay economy contract consumes this value.
            xpMultiplier = (1.0 + xpTraitCount * XP_BONUS_PER_TRAIT).coerceAtMost(MAX_XP_MULTIPLIER),
            bonusNebulaPerClaim = nebulaTraitCount.toLong().coerceAtMost(MAX_NEBULA_BONUS),
            grantsStreakProtection = active.any { TraitEffect.STREAK_PROTECTION in it.effects },
            cosmeticBadgeTraitIds = active
                .filter { TraitEffect.COSMETIC_BADGE in it.effects }
                .mapTo(sortedSetOf()) { it.id }
        )

        return TraitActivationResult(
            activeTraitIds = active.mapTo(sortedSetOf()) { it.id },
            discoveredHiddenTraitIds = registry.hidden()
                .asSequence()
                .filter { it.id in context.discoveredTraitIds }
                .mapTo(sortedSetOf()) { it.id },
            effects = resultEffects
        )
    }

    private fun isEligible(trait: TraitDefinition, context: TraitActivationContext): Boolean {
        if (trait.visibility == TraitVisibility.HIDDEN && trait.id !in context.discoveredTraitIds) {
            return false
        }
        if (trait.category == TraitCategory.SEASONAL && trait.seasonId != context.activeSeasonId) {
            return false
        }
        val requirements = trait.activation
        return context.playerLevel >= requirements.minimumLevel &&
            context.state.prestigeCount >= requirements.minimumPrestige.toLong() &&
            context.state.correctGuesses >= requirements.minimumCorrectGuesses
    }

    companion object {
        const val XP_BONUS_PER_TRAIT = 0.05
        const val MAX_XP_MULTIPLIER = 2.0
        const val MAX_NEBULA_BONUS = 100L
    }
}
