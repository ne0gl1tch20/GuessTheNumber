package com.jarrlyyy.guessthenumber.data.repository

/**
 * Shared validation helpers for bundled, versioned game content.
 * Invalid content fails closed so repositories can log it and use safe defaults.
 */
object ContentValidation {
    const val SUPPORTED_SCHEMA_VERSION = 1

    fun requireSupportedSchema(version: Int) {
        require(version == SUPPORTED_SCHEMA_VERSION) {
            "Unsupported content schema version $version (supported: $SUPPORTED_SCHEMA_VERSION)"
        }
    }

    fun requireUniqueIds(ids: List<String>) {
        require(ids.isNotEmpty()) { "Content list must not be empty" }
        require(ids.all { it.isNotBlank() }) { "Content IDs must not be blank" }
        require(ids.distinct().size == ids.size) { "Content IDs must be unique" }
    }

    fun validateUpgrades(items: List<com.jarrlyyy.guessthenumber.domain.model.UpgradeDef>) {
        requireUniqueIds(items.map { it.id })
        require(items.all {
            it.name.isNotBlank() && it.description.isNotBlank() &&
                it.maxLevel > 0 && it.baseCost.toBigDecimalOrNull()?.signum() != null &&
                it.baseCost.toBigDecimalOrNull()!!.signum() >= 0 &&
                it.costMultiplier >= 1.0 && it.costMultiplier.isFinite() &&
                it.effectPerLevel.isFinite()
        }) { "Invalid upgrade content" }
    }

    fun validateShopItems(items: List<com.jarrlyyy.guessthenumber.domain.model.ShopItemDef>) {
        requireUniqueIds(items.map { it.id })
        require(items.all {
            it.name.isNotBlank() && it.description.isNotBlank() && it.nebulaCost >= 0 &&
                it.category.isNotBlank()
        }) { "Invalid shop item content" }
    }

    fun validateAchievements(items: List<com.jarrlyyy.guessthenumber.domain.model.AchievementDef>) {
        requireUniqueIds(items.map { it.id })
        require(items.all {
            it.name.isNotBlank() && it.description.isNotBlank() && it.rewardNebula >= 0 &&
                it.tier.lowercase() in setOf("bronze", "silver", "gold", "diamond", "platinum", "legendary")
        }) { "Invalid achievement content" }
    }

    fun validateChallenges(items: List<ChallengeDef>) {
        requireUniqueIds(items.map { it.id })
        require(items.all { it.name.isNotBlank() && it.description.isNotBlank() && it.rewardNebula >= 0 }) {
            "Invalid challenge content"
        }
    }

    fun validateMinigames(items: List<MinigameDef>) {
        requireUniqueIds(items.map { it.id })
        require(items.all {
            it.name.isNotBlank() && it.description.isNotBlank() &&
                it.rewardMultiplier.isFinite() && it.rewardMultiplier >= 0.0
        }) { "Invalid minigame content" }
    }

    fun validateTalents(items: List<TalentDef>) {
        requireUniqueIds(items.map { it.id })
        val ids = items.map { it.id }.toSet()
        require(items.all {
            it.name.isNotBlank() && it.description.isNotBlank() && it.cost >= 0 &&
                (it.requiredParentId == null || it.requiredParentId in ids)
        }) { "Invalid talent content or missing parent reference" }
    }
}
