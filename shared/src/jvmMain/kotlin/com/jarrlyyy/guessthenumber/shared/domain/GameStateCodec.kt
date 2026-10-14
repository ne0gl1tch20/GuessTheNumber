package com.jarrlyyy.guessthenumber.shared.domain

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Shared JSON contract for game saves.
 *
 * Unknown fields are ignored for forward/backward compatibility and defaults
 * are written explicitly to preserve the Android save representation.
 */
object GameStateCodec {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encode(state: GameState): String = json.encodeToString(state)

    fun decode(serialized: String): GameState =
        json.decodeFromString<GameState>(serialized)

    fun decodeOrNull(serialized: String?): GameState? =
        serialized?.takeIf(String::isNotBlank)?.let { raw ->
            runCatching { decode(raw) }.getOrNull()
        }
}
