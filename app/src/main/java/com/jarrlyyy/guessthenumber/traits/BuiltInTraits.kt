package com.jarrlyyy.guessthenumber.traits

/** Small initial offline catalog. Expand through data additions, not new code paths. */
object BuiltInTraits {
    val definitions: List<TraitDefinition> = listOf(
        trait("steady_start", TraitCategory.PROGRESSION, TraitEffect.XP_MULTIPLIER,
            activation = TraitActivationConditions(minimumLevel = 1)),
        trait("nebula_finder", TraitCategory.COLLECTION, TraitEffect.NEBULA_BONUS,
            activation = TraitActivationConditions(minimumCorrectGuesses = 25)),
        trait("streak_guard", TraitCategory.GAMEPLAY, TraitEffect.STREAK_PROTECTION,
            activation = TraitActivationConditions(minimumLevel = 5)),
        trait("first_prestige", TraitCategory.PROGRESSION, TraitEffect.XP_MULTIPLIER,
            activation = TraitActivationConditions(minimumPrestige = 1)),
        trait("hidden_night_owl", TraitCategory.COLLECTION, TraitEffect.COSMETIC_BADGE,
            visibility = TraitVisibility.HIDDEN,
            activation = TraitActivationConditions(minimumCorrectGuesses = 100)),
        trait("autumn_glow", TraitCategory.SEASONAL, TraitEffect.COSMETIC_BADGE,
            seasonId = "autumn"),
        trait("winter_comet", TraitCategory.SEASONAL, TraitEffect.NEBULA_BONUS,
            seasonId = "winter", activation = TraitActivationConditions(minimumLevel = 3)),
        trait("spring_spark", TraitCategory.SEASONAL, TraitEffect.XP_MULTIPLIER,
            seasonId = "spring")
    ).also { TraitRegistry(it) }

    fun registry(): TraitRegistry = TraitRegistry(definitions)

    private fun trait(
        id: String,
        category: TraitCategory,
        effect: TraitEffect,
        visibility: TraitVisibility = TraitVisibility.VISIBLE,
        seasonId: String? = null,
        activation: TraitActivationConditions = TraitActivationConditions()
    ) = TraitDefinition(
        id = id,
        titleKey = "trait.$id.title",
        descriptionKey = "trait.$id.description",
        category = category,
        visibility = visibility,
        activation = activation,
        effects = setOf(effect),
        seasonId = seasonId
    )
}
