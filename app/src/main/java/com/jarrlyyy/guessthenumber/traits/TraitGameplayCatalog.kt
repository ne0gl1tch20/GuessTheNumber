package com.jarrlyyy.guessthenumber.traits

/** Single source of truth for player-selectable gameplay traits and their arms. */
data class TraitArmDefinition(
    val id: String,
    val labelKey: String,
    val fallbackLabel: String,
    val descriptionKey: String,
    val fallbackDescription: String
)

data class GameplayTraitDefinition(
    val id: String,
    val fallbackTitle: String,
    val fallbackDescription: String,
    val defaultArmId: String? = null,
    val arms: List<TraitArmDefinition> = emptyList()
)

object TraitGameplayCatalog {
    val all: List<GameplayTraitDefinition> = listOf(
        GameplayTraitDefinition("combo_momentum", "Combo Momentum", "Correct-guess payouts scale with your current streak.", "steady", listOf(
            TraitArmDefinition("steady", "trait_arm_steady_title", "Steady", "trait_arm_steady_description", "Up to 20% extra based on streak."),
            TraitArmDefinition("burst", "trait_arm_burst_title", "Burst", "trait_arm_burst_description", "Faster scaling, capped at 25% extra.")
        )),
        GameplayTraitDefinition("range_scout", "Range Scout", "Wrong guesses narrow the visible search range."),
        GameplayTraitDefinition("mistake_insurance", "Mistake Insurance", "Refunds part of the normal money penalty after a miss.", "half_refund", listOf(
            TraitArmDefinition("half_refund", "trait_arm_half_refund_title", "Half refund", "trait_arm_half_refund_description", "Refund 50% of the miss penalty."),
            TraitArmDefinition("careful", "trait_arm_careful_title", "Careful", "trait_arm_careful_description", "Refund 75% of the miss penalty.")
        )),
        GameplayTraitDefinition("risk_reward", "Risk Reward", "Correct guesses pay extra, but wrong guesses cost more.", "balanced", listOf(
            TraitArmDefinition("balanced", "trait_arm_balanced_title", "Balanced", "trait_arm_balanced_description", "25% bonus on hits and 10% extra penalty on misses."),
            TraitArmDefinition("high_risk", "trait_arm_high_risk_title", "High risk", "trait_arm_high_risk_description", "40% bonus on hits and 20% extra penalty on misses.")
        )),
        GameplayTraitDefinition("streak_guard", "Streak Guard", "Keeps your current streak after a wrong guess.")
    )

    private val byId = all.associateBy(GameplayTraitDefinition::id)

    init {
        require(byId.size == all.size) { "Duplicate gameplay trait ID." }
        all.forEach { trait ->
            require(trait.arms.map(TraitArmDefinition::id).distinct().size == trait.arms.size) {
                "Duplicate arm ID in ${trait.id}."
            }
            require(trait.arms.all { it.id.matches(Regex("[a-z][a-z0-9_]{1,47}")) }) {
                "Invalid arm ID in ${trait.id}."
            }
            require(trait.defaultArmId == null || trait.arms.any { it.id == trait.defaultArmId }) {
                "Default arm is not defined for ${trait.id}."
            }
        }
    }

    private val gameplayEffects = setOf(
        TraitEffect.COMBO_REWARD,
        TraitEffect.RANGE_HINT,
        TraitEffect.MISS_PENALTY_REDUCTION,
        TraitEffect.RISK_REWARD,
        TraitEffect.STREAK_PROTECTION
    )

    /**
     * Includes validated remote gameplay modules that declare a supported gameplay effect.
     * Unknown scripts and cosmetic-only traits are never presented as gameplay logic.
     */
    fun forRegistry(registry: TraitRegistry): List<GameplayTraitDefinition> {
        val knownIds = all.mapTo(mutableSetOf()) { it.id }
        val remote = registry.all()
            .filter { trait ->
                trait.source == TraitSource.LIVEOPS &&
                    trait.category == TraitCategory.GAMEPLAY &&
                    trait.effects.any { it in gameplayEffects } &&
                    trait.id !in knownIds
            }
            .map { trait ->
                val effectDescriptions = trait.effects
                    .filter { it in gameplayEffects }
                    .mapNotNull { effect ->
                        when (effect) {
                            TraitEffect.COMBO_REWARD -> "Adds a bounded streak-based bonus to correct-guess payouts."
                            TraitEffect.RANGE_HINT -> "Narrows the search range after an incorrect guess."
                            TraitEffect.MISS_PENALTY_REDUCTION -> "Refunds part of the normal penalty after an incorrect guess."
                            TraitEffect.RISK_REWARD -> "Boosts correct-guess payouts and increases miss penalties."
                            TraitEffect.STREAK_PROTECTION -> "Preserves the current streak after an incorrect guess."
                            else -> null
                        }
                    }.distinct()
                GameplayTraitDefinition(
                    id = trait.id,
                    fallbackTitle = trait.id.split('_', '-', '.').filter(String::isNotBlank)
                        .joinToString(" ") { it.replaceFirstChar(Char::uppercase) },
                    fallbackDescription = effectDescriptions.joinToString(" ")
                        .ifBlank { "A validated gameplay module." }
                )
            }
        return all + remote.sortedBy { it.id }
    }

    fun find(traitId: String): GameplayTraitDefinition? = byId[traitId]
    fun allowsArm(traitId: String, armId: String): Boolean =
        byId[traitId]?.arms?.any { it.id == armId } == true
}
