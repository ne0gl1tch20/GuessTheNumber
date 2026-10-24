package com.jarrlyyy.guessthenumber.traits

/**
 * Declarative trait contract. Definitions describe capabilities; they never execute code
 * or mutate player state directly.
 */
enum class TraitVisibility { VISIBLE, HIDDEN }
enum class TraitSource { BUILTIN, LIVEOPS }
enum class TraitCategory { PROGRESSION, GAMEPLAY, COLLECTION, SEASONAL }
enum class TraitEffect { XP_MULTIPLIER, NEBULA_BONUS, STREAK_PROTECTION, COSMETIC_BADGE }

data class TraitActivationConditions(
    val minimumLevel: Int = 1,
    val minimumPrestige: Int = 0,
    val minimumCorrectGuesses: Long = 0L
)

data class TraitDefinition(
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val id: String,
    val titleKey: String,
    val descriptionKey: String,
    val category: TraitCategory,
    val visibility: TraitVisibility = TraitVisibility.VISIBLE,
    val source: TraitSource = TraitSource.BUILTIN,
    val definitionVersion: Int = 1,
    val minHostApiVersion: Int = 1,
    val maxHostApiVersion: Int = 1,
    val activation: TraitActivationConditions = TraitActivationConditions(),
    val dependencies: Set<String> = emptySet(),
    val conflicts: Set<String> = emptySet(),
    val effects: Set<TraitEffect>,
    val seasonId: String? = null
) {
    companion object {
        const val CURRENT_SCHEMA_VERSION = 1
        const val CURRENT_HOST_API_VERSION = 1
        const val MAX_ID_LENGTH = 64
        const val MAX_KEY_LENGTH = 100
        const val MAX_DEPENDENCIES = 16
        const val MAX_EFFECTS = 8
    }
}

object TraitDefinitionValidator {
    private val idPattern = Regex("[a-z][a-z0-9_.-]{1,63}")
    private val keyPattern = Regex("[a-zA-Z0-9_.-]{1,100}")

    fun validate(definition: TraitDefinition) {
        require(definition.schemaVersion == TraitDefinition.CURRENT_SCHEMA_VERSION) {
            "Unsupported trait schema version: ${definition.schemaVersion}"
        }
        require(idPattern.matches(definition.id) && definition.id.length <= TraitDefinition.MAX_ID_LENGTH) {
            "Invalid trait ID: ${definition.id}"
        }
        require(keyPattern.matches(definition.titleKey)) { "Invalid title key for ${definition.id}" }
        require(keyPattern.matches(definition.descriptionKey)) { "Invalid description key for ${definition.id}" }
        require(definition.definitionVersion > 0) { "Definition version must be positive." }
        require(definition.minHostApiVersion > 0 &&
            definition.maxHostApiVersion >= definition.minHostApiVersion
        ) { "Invalid host API range for ${definition.id}" }
        require(TraitDefinition.CURRENT_HOST_API_VERSION in
            definition.minHostApiVersion..definition.maxHostApiVersion
        ) { "Trait ${definition.id} is incompatible with this host." }
        require(definition.activation.minimumLevel >= 1) { "Minimum level must be at least 1." }
        require(definition.activation.minimumPrestige >= 0) { "Minimum prestige cannot be negative." }
        require(definition.activation.minimumCorrectGuesses >= 0L) { "Correct guess requirement cannot be negative." }
        require(definition.dependencies.size <= TraitDefinition.MAX_DEPENDENCIES) { "Too many dependencies." }
        require(definition.effects.isNotEmpty() && definition.effects.size <= TraitDefinition.MAX_EFFECTS) {
            "Trait must declare a supported effect."
        }
        require(definition.id !in definition.dependencies && definition.id !in definition.conflicts) {
            "A trait cannot depend on or conflict with itself."
        }
        require(definition.dependencies.all(idPattern::matches)) { "Invalid dependency ID." }
        require(definition.conflicts.all(idPattern::matches)) { "Invalid conflict ID." }
        require(definition.seasonId == null || idPattern.matches(definition.seasonId)) {
            "Invalid season ID."
        }
        require((definition.category == TraitCategory.SEASONAL) == (definition.seasonId != null)) {
            "Seasonal traits must declare a season ID and non-seasonal traits must not."
        }
    }
}

/** Immutable validated registry snapshot. The caller chooses when a new snapshot is activated. */
class TraitRegistry(definitions: Collection<TraitDefinition>) {
    private val byId: Map<String, TraitDefinition>

    init {
        require(definitions.size <= MAX_TRAITS) { "Trait catalog exceeds the supported limit." }
        definitions.forEach(TraitDefinitionValidator::validate)
        require(definitions.map { it.id }.distinct().size == definitions.size) { "Duplicate trait ID." }

        byId = definitions.associateBy { it.id }
        byId.values.forEach { trait ->
            require(trait.dependencies.all(byId::containsKey)) {
                "Trait ${trait.id} references a missing dependency."
            }
            require(trait.conflicts.all(byId::containsKey)) {
                "Trait ${trait.id} references a missing conflict."
            }
            require(trait.id !in trait.dependencies) { "Trait dependency cycle detected." }
        }
        validateDependencyCycles(byId)
    }

    fun all(): List<TraitDefinition> = byId.values.sortedBy { it.id }
    fun find(id: String): TraitDefinition? = byId[id]
    fun visible(): List<TraitDefinition> = all().filter { it.visibility == TraitVisibility.VISIBLE }
    fun hidden(): List<TraitDefinition> = all().filter { it.visibility == TraitVisibility.HIDDEN }
    val size: Int get() = byId.size

    private fun validateDependencyCycles(traits: Map<String, TraitDefinition>) {
        val visiting = mutableSetOf<String>()
        val visited = mutableSetOf<String>()
        fun visit(id: String) {
            if (id in visited) return
            require(visiting.add(id)) { "Trait dependency cycle detected at $id." }
            traits.getValue(id).dependencies.forEach(::visit)
            visiting.remove(id)
            visited.add(id)
        }
        traits.keys.forEach(::visit)
    }

    companion object {
        const val MAX_TRAITS = 500
    }
}
