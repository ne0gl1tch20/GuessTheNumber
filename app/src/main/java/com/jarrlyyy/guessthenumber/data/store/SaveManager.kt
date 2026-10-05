package com.jarrlyyy.guessthenumber.data.store

import android.content.Context
import android.util.Base64
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.jarrlyyy.guessthenumber.data.logger.GameLogger
import com.jarrlyyy.guessthenumber.data.logger.LoggerCategory
import com.jarrlyyy.guessthenumber.data.logger.LogLevel
import com.jarrlyyy.guessthenumber.domain.model.BigNumber
import com.jarrlyyy.guessthenumber.domain.model.Difficulty
import com.jarrlyyy.guessthenumber.domain.model.GameState
import com.jarrlyyy.guessthenumber.domain.model.SaveProfile
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.math.BigDecimal

val Context.saveDataStore by preferencesDataStore(name = "game_save_prefs")

const val MAX_SAVE_SLOTS = 10

data class SaveSlotMetadata(
    val slotIndex: Int,
    val difficultyId: String = Difficulty.CLASSIC,
    val profileName: String = "",
    val profileIconId: String = SaveProfile.DEFAULT_ICON,
    val isEmpty: Boolean,
    val money: BigNumber = BigNumber.ZERO,
    val prestige: BigNumber = BigNumber.ZERO,
    val ultra: BigNumber = BigNumber.ZERO,
    val attempts: Long = 0L,
    val correctGuesses: Long = 0L,
    val lastSaveTimestamp: Long = 0L
)

class SaveManager(private val context: Context) {
    private val legacySaveKey = stringPreferencesKey("game_state_json")
    private val legacyBackupKey = stringPreferencesKey("game_state_backup_json")
    private val activeSlotKey = intPreferencesKey("active_save_slot")

    private fun getSaveKey(slot: Int) = stringPreferencesKey("game_state_slot_${slot}_json")
    private fun getBackupKey(slot: Int) = stringPreferencesKey("game_state_slot_${slot}_backup_json")

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    suspend fun getActiveSlot(): Int {
        val prefs = context.saveDataStore.data.first()
        return (prefs[activeSlotKey] ?: 1).coerceIn(1, MAX_SAVE_SLOTS)
    }

    suspend fun setActiveSlot(slot: Int) {
        context.saveDataStore.edit { prefs ->
            prefs[activeSlotKey] = slot.coerceIn(1, MAX_SAVE_SLOTS)
        }
    }

    suspend fun hasLegacySave(): Boolean {
        val prefs = context.saveDataStore.data.first()
        val legacyJson = prefs[legacySaveKey]
        val slot1Json = prefs[getSaveKey(1)]
        return !legacyJson.isNullOrEmpty() && slot1Json.isNullOrEmpty() && validateSave(legacyJson)
    }

    suspend fun migrateLegacySaveToSlot1(): Boolean {
        return try {
            val prefs = context.saveDataStore.data.first()
            val legacyJson = prefs[legacySaveKey]
            val legacyBackup = prefs[legacyBackupKey]
            if (!legacyJson.isNullOrEmpty() && validateSave(legacyJson)) {
                context.saveDataStore.edit { p ->
                    p[getSaveKey(1)] = legacyJson
                    if (!legacyBackup.isNullOrEmpty()) {
                        p[getBackupKey(1)] = legacyBackup
                    }
                    p.remove(legacySaveKey)
                    p.remove(legacyBackupKey)
                }
                GameLogger.log(LogLevel.INFO, LoggerCategory.SAVE, "LEGACY_MIGRATION", "Successfully migrated legacy save to Slot 1.")
                true
            } else {
                false
            }
        } catch (e: Exception) {
            GameLogger.log(LogLevel.ERROR, LoggerCategory.SAVE, "LEGACY_MIGRATION_ERROR", "Failed to migrate legacy save: ${e.message}")
            false
        }
    }

    suspend fun getSlotMetadata(slot: Int): SaveSlotMetadata {
        val s = slot.coerceIn(1, MAX_SAVE_SLOTS)
        return try {
            val prefs = context.saveDataStore.data.first()
            val jsonString = prefs[getSaveKey(s)]
            if (!jsonString.isNullOrEmpty() && validateSave(jsonString)) {
                val state = json.decodeFromString<GameState>(jsonString)
                SaveSlotMetadata(
                    slotIndex = s,
                    isEmpty = false,
                    difficultyId = if (Difficulty.isValid(state.difficultyId)) state.difficultyId else Difficulty.CLASSIC,
                    profileName = state.profileName.take(24),
                    profileIconId = if (SaveProfile.isValidIcon(state.profileIconId)) state.profileIconId else SaveProfile.DEFAULT_ICON,
                    money = state.money,
                    prestige = state.prestige,
                    ultra = state.ultra,
                    attempts = state.attempts,
                    correctGuesses = state.correctGuesses,
                    lastSaveTimestamp = state.lastSaveTimestamp
                )
            } else {
                SaveSlotMetadata(slotIndex = s, isEmpty = true)
            }
        } catch (e: Exception) {
            SaveSlotMetadata(slotIndex = s, isEmpty = true)
        }
    }

    suspend fun saveGame(state: GameState, slot: Int = -1): Boolean {
        val targetSlot = if (slot in 1..MAX_SAVE_SLOTS) slot else getActiveSlot()
        val sKey = getSaveKey(targetSlot)
        val bKey = getBackupKey(targetSlot)
        return try {
            val jsonString = json.encodeToString(state)
            context.saveDataStore.edit { prefs ->
                val currentState = prefs[sKey]
                if (currentState != null) {
                    prefs[bKey] = currentState
                }
                prefs[sKey] = jsonString
            }
            GameLogger.log(LogLevel.INFO, LoggerCategory.SAVE, "SAVE_SUCCESS", "Game state saved successfully to slot $targetSlot.")
            true
        } catch (e: Exception) {
            GameLogger.log(LogLevel.ERROR, LoggerCategory.SAVE, "SAVE_ERROR", "Failed to save game to slot $targetSlot: ${e.message}")
            false
        }
    }

    suspend fun loadGame(slot: Int = -1): GameState {
        val targetSlot = if (slot in 1..MAX_SAVE_SLOTS) slot else getActiveSlot()
        val sKey = getSaveKey(targetSlot)
        val bKey = getBackupKey(targetSlot)
        return try {
            val prefs = context.saveDataStore.data.first()
            val jsonString = prefs[sKey]
            if (jsonString != null && validateSave(jsonString)) {
                sanitizeLoadedState(json.decodeFromString<GameState>(jsonString))
            } else {
                val backupString = prefs[bKey]
                if (backupString != null && validateSave(backupString)) {
                    GameLogger.log(LogLevel.WARN, LoggerCategory.SAVE, "RESTORE_BACKUP", "Restored slot $targetSlot from backup save.")
                    sanitizeLoadedState(json.decodeFromString<GameState>(backupString))
                } else {
                    GameLogger.log(LogLevel.INFO, LoggerCategory.SAVE, "NEW_GAME", "Initializing fresh game state for slot $targetSlot.")
                    GameState()
                }
            }
        } catch (e: Exception) {
            GameLogger.log(LogLevel.ERROR, LoggerCategory.SAVE, "LOAD_ERROR", "Failed to load game for slot $targetSlot, returning fresh state: ${e.message}")
            GameState()
        }
    }

    fun validateSave(jsonString: String): Boolean {
        return try {
            val state = json.decodeFromString<GameState>(jsonString)
            Difficulty.isValid(state.difficultyId) &&
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

        val difficultyId = if (Difficulty.isValid(state.difficultyId)) state.difficultyId else Difficulty.CLASSIC
        val profileName = state.profileName.trim().take(24)
        val profileIconId = if (SaveProfile.isValidIcon(state.profileIconId)) state.profileIconId else SaveProfile.DEFAULT_ICON

        return state.copy(
            difficultyId = difficultyId,
            profileName = profileName,
            profileIconId = profileIconId,
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
            val key = 0x42.toByte() // XOR cipher key combined with Base64 obfuscation & salt
            val salt = "GTN_ENC_2025_".toByteArray(Charsets.UTF_8)
            val textBytes = text.toByteArray(Charsets.UTF_8)
            val combined = ByteArray(salt.size + textBytes.size)
            System.arraycopy(salt, 0, combined, 0, salt.size)
            for (i in textBytes.indices) {
                combined[salt.size + i] = (textBytes[i].toInt() xor key.toInt()).toByte()
            }
            Base64.encodeToString(combined, Base64.NO_WRAP)
        } catch (e: Exception) {
            ""
        }
    }

    private fun decrypt(encoded: String): String {
        return try {
            val decoded = Base64.decode(encoded, Base64.NO_WRAP)
            val salt = "GTN_ENC_2025_".toByteArray(Charsets.UTF_8)
            if (decoded.size <= salt.size) return ""
            for (i in salt.indices) {
                if (decoded[i] != salt[i]) {
                    // Try fallback: plain base64 decode if salt doesn't match
                    val plainBytes = Base64.decode(encoded, Base64.NO_WRAP)
                    return String(plainBytes, Charsets.UTF_8)
                }
            }
            val key = 0x42.toByte()
            val dataSize = decoded.size - salt.size
            val textBytes = ByteArray(dataSize)
            for (i in 0 until dataSize) {
                textBytes[i] = (decoded[salt.size + i].toInt() xor key.toInt()).toByte()
            }
            String(textBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            // Fallback to plain base64 decode
            try {
                val bytes = Base64.decode(encoded, Base64.NO_WRAP)
                String(bytes, Charsets.UTF_8)
            } catch (ex: Exception) {
                ""
            }
        }
    }

    suspend fun exportSave(slot: Int = -1): String {
        val targetSlot = if (slot in 1..MAX_SAVE_SLOTS) slot else getActiveSlot()
        val prefs = context.saveDataStore.data.first()
        val raw = prefs[getSaveKey(targetSlot)] ?: ""
        return if (raw.isNotEmpty()) encrypt(raw) else ""
    }

    suspend fun importSave(inputString: String, slot: Int = -1): Boolean {
        val targetSlot = if (slot in 1..MAX_SAVE_SLOTS) slot else getActiveSlot()
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
            saveGame(state, targetSlot)
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun resetSlot(slot: Int) {
        val s = slot.coerceIn(1, MAX_SAVE_SLOTS)
        context.saveDataStore.edit { prefs ->
            prefs.remove(getSaveKey(s))
            prefs.remove(getBackupKey(s))
        }
        GameLogger.log(LogLevel.WARN, LoggerCategory.SAVE, "RESET_SLOT", "Save slot $s reset.")
    }

    suspend fun resetData() {
        context.saveDataStore.edit { prefs ->
            prefs.clear()
        }
        GameLogger.log(LogLevel.WARN, LoggerCategory.SAVE, "RESET_DATA", "All game data and slots reset.")
    }
}
