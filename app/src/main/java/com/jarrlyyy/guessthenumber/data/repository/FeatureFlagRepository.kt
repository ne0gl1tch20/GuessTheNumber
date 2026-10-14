package com.jarrlyyy.guessthenumber.data.repository

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class FeatureFlagConfig(
    val schemaVersion: Int = 1,
    val flags: Map<String, Boolean> = emptyMap()
)

object DefaultFeatureFlags {
    val values: Map<String, Boolean> = mapOf(
        "live_ops" to true,
        "seasonal_rewards" to true,
        "world_map" to true,
        "challenge_builder" to true,
        "dev_console" to true,
        "lua_scripting" to true
    )
}

/** Bundled, offline feature switches. Missing flags use explicit defaults. */
class FeatureFlagRepository(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true }

    fun load(): Map<String, Boolean> = runCatching {
        val config = context.assets.open("game/feature_flags.json").bufferedReader().use {
            json.decodeFromString<FeatureFlagConfig>(it.readText())
        }
        ContentValidation.requireSupportedSchema(config.schemaVersion)
        DefaultFeatureFlags.values.mapValues { (key, default) -> config.flags[key] ?: default }
    }.getOrElse { DefaultFeatureFlags.values }

    fun isEnabled(flag: String): Boolean = load()[flag] ?: false
}
