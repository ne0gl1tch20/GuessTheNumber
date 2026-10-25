package com.jarrlyyy.guessthenumber.data.store

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Creative configuration is separate from GameState and scoped to one save slot.
 * Custom effects can tune bounded play rules, but cannot award currency or edit progression.
 */
@Serializable
data class CreativeModeSave(
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val name: String = "My Creative Mode",
    val enabled: Boolean = false,
    val rangeMin: Long = 1L,
    val rangeMax: Long = 100L,
    val effects: List<CreativeModeEffect> = emptyList(),
    val activeRun: CreativeModeRunSnapshot? = null
) {
    companion object {
        const val CURRENT_SCHEMA_VERSION = 3
        const val MAX_EFFECTS = 16
        const val MAX_RANGE = 1_000_000L
    }
}

@Serializable
data class CreativeModeEffect(
    val id: String,
    val type: CreativeEffectType,
    val value: Int
)

/** Persistable snapshot for a Creative Mode run; it never contains rewards or progression. */
@Serializable
data class CreativeModeRunSnapshot(
    val runId: String,
    val rangeMin: Long,
    val rangeMax: Long,
    val target: Long,
    val attemptsUsed: Int = 0,
    val correctGuesses: Int = 0,
    val hintsUsed: Int = 0,
    val completed: Boolean = false
)

@Serializable
enum class CreativeEffectType {
    RANGE_SCALE,
    EXTRA_HINTS,
    ATTEMPT_LIMIT
}

data class CreativeRuntimeRules(
    val rangeMin: Long,
    val rangeMax: Long,
    val extraHints: Int,
    val attemptLimit: Int?
)

object CreativeModeSaveCodec {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    fun encode(save: CreativeModeSave): String {
        require(validate(save)) { "Invalid creative-mode save." }
        return json.encodeToString(save)
    }

    fun decodeAndMigrate(raw: String): CreativeModeSave? = runCatching {
        val decoded = json.decodeFromString<CreativeModeSave>(raw)
        val migrated = when (decoded.schemaVersion) {
            1 -> decoded.copy(schemaVersion = CreativeModeSave.CURRENT_SCHEMA_VERSION, effects = emptyList(), activeRun = null)
            2 -> decoded.copy(schemaVersion = CreativeModeSave.CURRENT_SCHEMA_VERSION, activeRun = null)
            CreativeModeSave.CURRENT_SCHEMA_VERSION -> decoded
            else -> return null
        }
        migrated.takeIf(::validate)
    }.getOrNull()

    fun validate(save: CreativeModeSave): Boolean {
        if (save.schemaVersion != CreativeModeSave.CURRENT_SCHEMA_VERSION) return false
        if (save.name.isBlank() || save.name.length > 40) return false
        if (save.rangeMin < 1L || save.rangeMax < save.rangeMin ||
            save.rangeMax > CreativeModeSave.MAX_RANGE) return false
        if (save.effects.size > CreativeModeSave.MAX_EFFECTS) return false
        if (save.effects.map { it.id }.toSet().size != save.effects.size) return false
        if (save.activeRun != null && !validateRunSnapshot(save.activeRun)) return false
        return save.effects.all { effect ->
            Regex("[a-z][a-z0-9_-]{1,31}").matches(effect.id) &&
                when (effect.type) {
                    CreativeEffectType.RANGE_SCALE -> effect.value in 1..5
                    CreativeEffectType.EXTRA_HINTS -> effect.value in 0..5
                    CreativeEffectType.ATTEMPT_LIMIT -> effect.value in 1..100
                }
        }
    }

    fun validateRunSnapshot(snapshot: CreativeModeRunSnapshot): Boolean =
        Regex("[a-zA-Z0-9_-]{8,64}").matches(snapshot.runId) &&
            snapshot.rangeMin >= 1L &&
            snapshot.rangeMax >= snapshot.rangeMin &&
            snapshot.rangeMax <= CreativeModeSave.MAX_RANGE &&
            snapshot.target in snapshot.rangeMin..snapshot.rangeMax &&
            snapshot.attemptsUsed >= 0 &&
            snapshot.correctGuesses >= 0 &&
            snapshot.correctGuesses <= snapshot.attemptsUsed &&
            snapshot.hintsUsed in 0..10
}

object CreativeModeEffectPolicy {
    fun resolve(save: CreativeModeSave): CreativeRuntimeRules? {
        if (!CreativeModeSaveCodec.validate(save)) return null
        var min = save.rangeMin
        var max = save.rangeMax
        var hints = 0
        var attempts: Int? = null
        if (save.enabled) {
            save.effects.forEach { effect ->
                when (effect.type) {
                    CreativeEffectType.RANGE_SCALE -> {
                        val width = (max - min + 1L).coerceAtLeast(1L)
                        val scaled = (width * effect.value.toLong()).coerceAtMost(CreativeModeSave.MAX_RANGE)
                        max = (min + scaled - 1L).coerceAtMost(CreativeModeSave.MAX_RANGE)
                    }
                    CreativeEffectType.EXTRA_HINTS -> hints = (hints + effect.value).coerceAtMost(10)
                    CreativeEffectType.ATTEMPT_LIMIT -> attempts = minOf(attempts ?: effect.value, effect.value)
                }
            }
        }
        return CreativeRuntimeRules(min, max, hints, attempts)
    }
}
