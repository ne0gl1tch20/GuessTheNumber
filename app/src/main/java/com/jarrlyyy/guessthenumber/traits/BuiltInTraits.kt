package com.jarrlyyy.guessthenumber.traits

/**
 * Offline-first trait catalog. Stable IDs are save/remote-pack contracts, so existing IDs must
 * never be repurposed. New content is added as data, not new activation code paths.
 */
object BuiltInTraits {
    const val CATALOG_VERSION = 2

    val definitions: List<TraitDefinition> = listOf(
        // Progression
        trait("steady_start", TraitCategory.PROGRESSION, TraitEffect.XP_MULTIPLIER,
            activation = TraitActivationConditions(minimumLevel = 1)),
        trait("quick_learner", TraitCategory.PROGRESSION, TraitEffect.XP_MULTIPLIER,
            activation = TraitActivationConditions(minimumLevel = 3),
            dependencies = setOf("steady_start")),
        trait("first_prestige", TraitCategory.PROGRESSION, TraitEffect.XP_MULTIPLIER,
            activation = TraitActivationConditions(minimumPrestige = 1)),
        trait("veteran_focus", TraitCategory.PROGRESSION, TraitEffect.XP_MULTIPLIER,
            activation = TraitActivationConditions(minimumLevel = 10, minimumCorrectGuesses = 100),
            dependencies = setOf("first_prestige")),

        // Gameplay variants
        trait("streak_guard", TraitCategory.GAMEPLAY, TraitEffect.STREAK_PROTECTION,
            activation = TraitActivationConditions(minimumLevel = 5)),
        trait("focus_streak", TraitCategory.GAMEPLAY, TraitEffect.STREAK_PROTECTION,
            activation = TraitActivationConditions(minimumLevel = 10, minimumCorrectGuesses = 75),
            conflicts = setOf("streak_guard")),

        // Collection
        trait("nebula_finder", TraitCategory.COLLECTION, TraitEffect.NEBULA_BONUS,
            activation = TraitActivationConditions(minimumCorrectGuesses = 25)),
        trait("nebula_hunter", TraitCategory.COLLECTION, TraitEffect.NEBULA_BONUS,
            activation = TraitActivationConditions(minimumCorrectGuesses = 75),
            dependencies = setOf("nebula_finder")),
        trait("comet_collector", TraitCategory.COLLECTION, TraitEffect.NEBULA_BONUS,
            activation = TraitActivationConditions(minimumCorrectGuesses = 150),
            dependencies = setOf("nebula_hunter")),

        // Hidden discoveries
        trait("hidden_night_owl", TraitCategory.COLLECTION, TraitEffect.COSMETIC_BADGE,
            visibility = TraitVisibility.HIDDEN,
            activation = TraitActivationConditions(minimumCorrectGuesses = 100)),
        trait("hidden_century_club", TraitCategory.COLLECTION, TraitEffect.COSMETIC_BADGE,
            visibility = TraitVisibility.HIDDEN,
            activation = TraitActivationConditions(minimumCorrectGuesses = 250),
            dependencies = setOf("hidden_night_owl")),
        trait("hidden_precision_mind", TraitCategory.COLLECTION, TraitEffect.COSMETIC_BADGE,
            visibility = TraitVisibility.HIDDEN,
            activation = TraitActivationConditions(minimumCorrectGuesses = 500),
            dependencies = setOf("hidden_century_club")),

        // Seasonal
        trait("autumn_glow", TraitCategory.SEASONAL, TraitEffect.COSMETIC_BADGE,
            seasonId = "autumn"),
        trait("winter_comet", TraitCategory.SEASONAL, TraitEffect.NEBULA_BONUS,
            seasonId = "winter", activation = TraitActivationConditions(minimumCorrectGuesses = 50)),
        trait("spring_spark", TraitCategory.SEASONAL, TraitEffect.XP_MULTIPLIER,
            seasonId = "spring"),
        trait("summer_bloom", TraitCategory.SEASONAL, TraitEffect.XP_MULTIPLIER,
            seasonId = "summer", activation = TraitActivationConditions(minimumLevel = 5))
    ).also { TraitRegistry(it) }

    fun registry(): TraitRegistry = TraitRegistry(definitions)

    private fun trait(
        id: String,
        category: TraitCategory,
        effect: TraitEffect,
        visibility: TraitVisibility = TraitVisibility.VISIBLE,
        seasonId: String? = null,
        activation: TraitActivationConditions = TraitActivationConditions(),
        dependencies: Set<String> = emptySet(),
        conflicts: Set<String> = emptySet()
    ) = TraitDefinition(
        id = id,
        titleKey = "trait.$id.title",
        descriptionKey = "trait.$id.description",
        category = category,
        visibility = visibility,
        activation = activation,
        dependencies = dependencies,
        conflicts = conflicts,
        effects = setOf(effect),
        seasonId = seasonId
    )
}
