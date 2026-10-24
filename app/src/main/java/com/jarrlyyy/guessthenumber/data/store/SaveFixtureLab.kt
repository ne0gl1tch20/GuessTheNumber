package com.jarrlyyy.guessthenumber.data.store

import com.jarrlyyy.guessthenumber.domain.model.BigNumber
import com.jarrlyyy.guessthenumber.domain.model.Difficulty
import com.jarrlyyy.guessthenumber.domain.model.GameState
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.math.BigDecimal

/**
 * Isolated, in-memory save fixtures for the debug Save Laboratory.
 * No DataStore, file, slot, or active-game mutation is performed here.
 */
object SaveFixtureLab {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    data class Check(val name: String, val passed: Boolean, val detail: String)

    fun encode(state: GameState): String = json.encodeToString(state)

    fun decode(raw: String): GameState? =
        runCatching { json.decodeFromString<GameState>(raw) }.getOrNull()

    /** Mirrors the safety-critical invariants enforced by SaveManager.validateSave. */
    fun validate(raw: String): Check {
        val state = decode(raw) ?: return Check("Decode JSON", false, "Malformed JSON or incompatible schema")
        val valid = Difficulty.isValid(state.difficultyId) &&
            state.money.value >= BigDecimal.ZERO &&
            state.prestige.value >= BigDecimal.ZERO &&
            state.ultra.value >= BigDecimal.ZERO &&
            state.nebula.value >= BigDecimal.ZERO &&
            state.currentRangeMin >= 1 &&
            state.currentRangeMin <= state.currentRangeMax &&
            state.targetNumber in state.currentRangeMin..state.currentRangeMax &&
            state.attempts >= 0 && state.correctGuesses >= 0 &&
            state.streak >= 0 && state.bestStreak >= 0 &&
            state.statistics.moneyEarned.value >= BigDecimal.ZERO &&
            state.statistics.moneySpent.value >= BigDecimal.ZERO &&
            state.upgradeLevels.values.all { it >= 0 } &&
            state.prestigeUpgradeLevels.values.all { it >= 0 } &&
            state.ultraUpgradeLevels.values.all { it >= 0 } &&
            state.lastSaveTimestamp > 0
        return Check("Save invariant validation", valid, if (valid) "Fixture passed core save invariants" else "Rejected by save invariants")
    }

    /** Simulates schema-forward migration in memory using the same tolerant decoder as saves. */
    fun migrateLegacy(raw: String): Check {
        val decoded = decode(raw) ?: return Check("Legacy schema migration", false, "Legacy JSON could not be decoded")
        val migrated = runCatching { json.encodeToString(decoded) }.getOrNull()
            ?: return Check("Legacy schema migration", false, "Could not encode migrated fixture")
        val roundTrip = decode(migrated)
        val passed = roundTrip != null && roundTrip.difficultyId == decoded.difficultyId &&
            roundTrip.currentRangeMin == decoded.currentRangeMin &&
            roundTrip.currentRangeMax == decoded.currentRangeMax
        return Check("Legacy schema migration", passed, if (passed) "Legacy-compatible fields survived in-memory migration" else "State changed during migration round-trip")
    }

    fun runSafetySuite(state: GameState): List<Check> {
        val fixture = encode(state.copy(lastSaveTimestamp = state.lastSaveTimestamp.coerceAtLeast(1L)))
        val current = validate(fixture)
        val corrupted = validate("{ definitely not json")
        val negativeCurrency = validate(encode(state.copy(
            lastSaveTimestamp = state.lastSaveTimestamp.coerceAtLeast(1L),
            money = BigNumber(-999)
        )))
        val legacy = migrateLegacy(fixture.replaceFirst("lastSavedVersion", "legacyUnknownVersionField"))
        return listOf(
            Check("Valid current save", current.passed, current.detail),
            Check("Corrupted JSON rejected", !corrupted.passed, if (!corrupted.passed) "Malformed fixture safely rejected" else "Malformed fixture was accepted"),
            Check("Negative currency rejected", !negativeCurrency.passed, if (!negativeCurrency.passed) "Negative currency fixture safely rejected" else "Negative currency fixture was accepted"),
            legacy
        )
    }
}
