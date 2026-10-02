package com.jarrlyyy.guessthenumber.data.store

import android.content.Context
import android.util.Base64
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.jarrlyyy.guessthenumber.data.logger.GameLogger
import com.jarrlyyy.guessthenumber.data.logger.LoggerCategory
import com.jarrlyyy.guessthenumber.data.logger.LogLevel
import com.jarrlyyy.guessthenumber.domain.model.BigNumber
import com.jarrlyyy.guessthenumber.domain.model.GameState
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.math.BigDecimal

val Context.saveDataStore by preferencesDataStore(name = "game_save_prefs")

class SaveManager(private val context: Context) {
    private val saveKey = stringPreferencesKey("game_state_json")
    private val backupKey = stringPreferencesKey("game_state_backup_json")
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    suspend fun saveGame(state: GameState): Boolean {
        return try {
            val jsonString = json.encodeToString(state)
            context.saveDataStore.edit { prefs ->
                val currentState = prefs[saveKey]
                if (currentState != null) {
                    prefs[backupKey] = currentState
                }
                prefs[saveKey] = jsonString
            }
            GameLogger.log(LogLevel.INFO, LoggerCategory.SAVE, "SAVE_SUCCESS", "Game state saved successfully.")
            true
        } catch (e: Exception) {
            GameLogger.log(LogLevel.ERROR, LoggerCategory.SAVE, "SAVE_ERROR", "Failed to save game: ${e.message}")
            false
        }
    }

    suspend fun loadGame(): GameState {
        return try {
            val prefs = context.saveDataStore.data.first()
            val jsonString = prefs[saveKey]
            if (jsonString != null && validateSave(jsonString)) {
                sanitizeLoadedState(json.decodeFromString<GameState>(jsonString))
            } else {
                val backupString = prefs[backupKey]
                if (backupString != null && validateSave(backupString)) {
                    GameLogger.log(LogLevel.WARN, LoggerCategory.SAVE, "RESTORE_BACKUP", "Restored from backup save.")
                    sanitizeLoadedState(json.decodeFromString<GameState>(backupString))
                } else {
                    GameLogger.log(LogLevel.INFO, LoggerCategory.SAVE, "NEW_GAME", "Initializing fresh game state.")
                    GameState()
                }
            }
        } catch (e: Exception) {
            GameLogger.log(LogLevel.ERROR, LoggerCategory.SAVE, "LOAD_ERROR", "Failed to load game, returning fresh state: ${e.message}")
            GameState()
        }
    }

    fun validateSave(jsonString: String): Boolean {
        return try {
            val state = json.decodeFromString<GameState>(jsonString)
            state.money.value >= BigDecimal.ZERO &&
                    state.prestige.value >= BigDecimal.ZERO &&
                    state.ultra.value >= BigDecimal.ZERO &&
                    state.nebula.value >= BigDecimal.ZERO &&
                    state.currentRangeMin <= state.currentRangeMax &&
                    state.attempts >= 0 &&
                    state.correctGuesses >= 0 &&
                    state.streak >= 0 &&
                    state.bestStreak >= 0 &&
                    state.lastSaveTimestamp > 0
        } catch (e: Exception) {
            false
        }
    }

    private fun sanitizeLoadedState(state: GameState): GameState {
        val money = if (state.money.value < BigDecimal.ZERO) BigNumber.ZERO else state.money
        val prestige = if (state.prestige.value < BigDecimal.ZERO) BigNumber.ZERO else state.prestige
        val ultra = if (state.ultra.value < BigDecimal.ZERO) BigNumber.ZERO else state.ultra
        val nebula = if (state.nebula.value < BigDecimal.ZERO) BigNumber.ZERO else state.nebula
        val min = if (state.currentRangeMin > state.currentRangeMax) 1L else state.currentRangeMin
        val max = if (state.currentRangeMax < min) 100L else state.currentRangeMax
        val target = state.targetNumber.coerceIn(min, max)
        val attempts = maxOf(0L, state.attempts)
        val correct = maxOf(0L, state.correctGuesses)
        val streak = maxOf(0, state.streak)
        val bestStreak = maxOf(0, state.bestStreak)

        return state.copy(
            money = money,
            prestige = prestige,
            ultra = ultra,
            nebula = nebula,
            currentRangeMin = min,
            currentRangeMax = max,
            targetNumber = target,
            attempts = attempts,
            correctGuesses = correct,
            streak = streak,
            bestStreak = bestStreak
        )
    }

    private fun encrypt(text: String): String {
        return try {
            val bytes = text.toByteArray(Charsets.UTF_8)
            Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            ""
        }
    }

    private fun decrypt(encoded: String): String {
        return try {
            val bytes = Base64.decode(encoded, Base64.NO_WRAP)
            String(bytes, Charsets.UTF_8)
        } catch (e: Exception) {
            ""
        }
    }

    suspend fun exportSave(): String {
        val prefs = context.saveDataStore.data.first()
        val raw = prefs[saveKey] ?: ""
        return if (raw.isNotEmpty()) encrypt(raw) else ""
    }

    suspend fun importSave(inputString: String): Boolean {
        val jsonString = if (validateSave(inputString)) {
            inputString
        } else {
            val decrypted = decrypt(inputString)
            if (decrypted.isNotEmpty() && validateSave(decrypted)) {
                decrypted
            } else {
                return false
            }
        }

        return try {
            val state = json.decodeFromString<GameState>(jsonString)
            saveGame(state)
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun resetData() {
        context.saveDataStore.edit { prefs ->
            prefs.clear()
        }
        GameLogger.log(LogLevel.WARN, LoggerCategory.SAVE, "RESET_DATA", "All game data reset.")
    }
}
