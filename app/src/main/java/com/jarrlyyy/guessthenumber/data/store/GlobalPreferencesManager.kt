package com.jarrlyyy.guessthenumber.data.store

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.jarrlyyy.guessthenumber.domain.model.GameSettings
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

val Context.appPreferencesDataStore by preferencesDataStore(name = "app_preferences")

class GlobalPreferencesManager(private val context: Context) {
    private val settingsKey = stringPreferencesKey("global_app_settings_json")
    private val migrationKey = stringPreferencesKey("settings_migration_completed")
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    suspend fun getSettings(): GameSettings {
        val raw = context.appPreferencesDataStore.data.first()[settingsKey] ?: return GameSettings()
        return runCatching { json.decodeFromString<GameSettings>(raw) }.getOrDefault(GameSettings())
    }

    suspend fun saveSettings(settings: GameSettings) {
        context.appPreferencesDataStore.edit { it[settingsKey] = json.encodeToString(settings) }
    }

    suspend fun isMigrationCompleted(): Boolean =
        context.appPreferencesDataStore.data.first()[migrationKey] == "true"

    suspend fun markMigrationCompleted() {
        context.appPreferencesDataStore.edit { it[migrationKey] = "true" }
    }

    suspend fun markTutorialCompleted() {
        val settings = getSettings()
        saveSettings(settings.copy(tutorialCompleted = true))
    }

    suspend fun clear() {
        context.appPreferencesDataStore.edit { it.clear() }
    }

    fun encode(settings: GameSettings): String = json.encodeToString(settings)
    fun decode(raw: String): GameSettings? = runCatching { json.decodeFromString<GameSettings>(raw) }.getOrNull()
}
