package com.jarrlyyy.guessthenumber.data.store

import android.content.Context
import android.util.Base64
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.jarrlyyy.guessthenumber.data.logger.GameLogger
import com.jarrlyyy.guessthenumber.data.logger.LoggerCategory
import com.jarrlyyy.guessthenumber.data.logger.LogLevel
import com.jarrlyyy.guessthenumber.domain.model.GameState
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.math.BigDecimal
import java.security.MessageDigest

val Context.dataStore by preferencesDataStore(name = "game_save_prefs")

class SaveManager(private val context: Context) {
    private val saveKey = stringPreferencesKey("game_state_json")
    private val backupKey = stringPreferencesKey("game_state_backup_json")
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    suspend fun saveGame(state: GameState): Boolean {
        return try {
            val jsonString = json.encodeToString(state)
            val checksum = computeChecksum(jsonString)
            context.dataStore.edit { prefs ->
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
            val prefs = context.dataStore.data.first()
            val jsonString = prefs[saveKey]
            if (jsonString != null && validateSave(jsonString)) {
                json.decodeFromString<GameState>(jsonString)
            } else {
                val backupString = prefs[backupKey]
                if (backupString != null && validateSave(backupString)) {
                    GameLogger.log(LogLevel.WARN, LoggerCategory.SAVE, "RESTORE_BACKUP", "Restored from backup save.")
                    json.decodeFromString<GameState>(backupString)
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

    private fun validateSave(jsonString: String): Boolean {
        return try {
            val state = json.decodeFromString<GameState>(jsonString)
            state.money.value >= BigDecimal.ZERO &&
                    state.prestige.value >= BigDecimal.ZERO &&
                    state.ultra.value >= BigDecimal.ZERO
        } catch (e: Exception) {
            false
        }
    }

    private fun computeChecksum(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun encrypt(text: String): String {
        val key = "GuessTheNumberSecureKey2025"
        val bytes = text.toByteArray(Charsets.UTF_8)
        val encrypted = ByteArray(bytes.size)
        for (i in bytes.indices) {
            encrypted[i] = (bytes[i].toInt() xor key[i % key.length].code).toByte()
        }
        return Base64.encodeToString(encrypted, Base64.NO_WRAP)
    }

    private fun decrypt(encoded: String): String {
        return try {
            val key = "GuessTheNumberSecureKey2025"
            val bytes = Base64.decode(encoded, Base64.NO_WRAP)
            val decrypted = ByteArray(bytes.size)
            for (i in bytes.indices) {
                decrypted[i] = (bytes[i].toInt() xor key[i % key.length].code).toByte()
            }
            String(decrypted, Charsets.UTF_8)
        } catch (e: Exception) {
            ""
        }
    }

    suspend fun exportSave(): String {
        val prefs = context.dataStore.data.first()
        val raw = prefs[saveKey] ?: ""
        return if (raw.isNotEmpty()) encrypt(raw) else ""
    }

    suspend fun importSave(inputString: String): Boolean {
        // Try decrypting first, if it fails or isn't base64 encrypted, try treating inputString as raw JSON
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
        context.dataStore.edit { prefs ->
            prefs.clear()
        }
        GameLogger.log(LogLevel.WARN, LoggerCategory.SAVE, "RESET_DATA", "All game data reset.")
    }
}
