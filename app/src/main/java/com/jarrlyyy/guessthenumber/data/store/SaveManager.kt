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
import com.jarrlyyy.guessthenumber.domain.model.GameSettings
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
    val lastSaveTimestamp: Long = 0L,
    val hasBackup: Boolean = false
)

@kotlinx.serialization.Serializable
data class EncryptedBackupBundle(
    val formatVersion: Int = 2,
    val appPreferencesJson: String,
    val activeSlot: Int,
    val slots: Map<Int, String> = emptyMap(),
    val backups: Map<Int, String> = emptyMap()
)

class SaveManager(private val context: Context) {
    private val globalPreferences = GlobalPreferencesManager(context)
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
                    lastSaveTimestamp = state.lastSaveTimestamp,
                    hasBackup = prefs[getBackupKey(s)]?.let { validateSave(it) } == true
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
            val storedState = if (globalPreferences.isMigrationCompleted()) state.copy(settings = GameSettings()) else state
            val jsonString = json.encodeToString(storedState)
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

    suspend fun updateGameAtomically(slot: Int = -1, transform: (GameState) -> GameState): GameState? {
        val targetSlot = if (slot in 1..MAX_SAVE_SLOTS) slot else getActiveSlot()
        val sKey = getSaveKey(targetSlot)
        val bKey = getBackupKey(targetSlot)
        var updatedState: GameState? = null
        return try {
            context.saveDataStore.edit { prefs ->
                val currentJson = prefs[sKey]
                if (currentJson.isNullOrEmpty() || !validateSave(currentJson)) return@edit
                val current = sanitizeLoadedState(json.decodeFromString<GameState>(currentJson))
                val updated = transform(current).copy(lastSaveTimestamp = System.currentTimeMillis())
                val storedUpdated = if (globalPreferences.isMigrationCompleted()) updated.copy(settings = GameSettings()) else updated
                prefs[bKey] = currentJson
                prefs[sKey] = json.encodeToString(storedUpdated)
                updatedState = updated
            }
            updatedState
        } catch (e: Exception) {
            GameLogger.log(LogLevel.ERROR, LoggerCategory.SAVE, "ATOMIC_UPDATE_ERROR", "Failed atomic game update in slot $targetSlot: ${e.message}")
            null
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
                sanitizeLoadedState(json.decodeFromString<GameState>(jsonString)).copy(settings = globalPreferences.getSettings())
            } else {
                val backupString = prefs[bKey]
                if (backupString != null && validateSave(backupString)) {
                    GameLogger.log(LogLevel.WARN, LoggerCategory.SAVE, "RESTORE_BACKUP", "Restored slot $targetSlot from backup save.")
                    sanitizeLoadedState(json.decodeFromString<GameState>(backupString)).copy(settings = globalPreferences.getSettings())
                } else {
                    GameLogger.log(LogLevel.INFO, LoggerCategory.SAVE, "NEW_GAME", "Initializing fresh game state for slot $targetSlot.")
                    GameState(settings = globalPreferences.getSettings())
                }
            }
        } catch (e: Exception) {
            GameLogger.log(LogLevel.ERROR, LoggerCategory.SAVE, "LOAD_ERROR", "Failed to load game for slot $targetSlot, returning fresh state: ${e.message}")
            GameState(settings = globalPreferences.getSettings())
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
        val min = 1L
        val betterRangeLevel = state.upgradeLevels["better_range"] ?: 0
        val difficultyMax = Difficulty.rangeMax(state.difficultyId, betterRangeLevel)
        val max = state.currentRangeMax.coerceAtLeast(difficultyMax).coerceAtLeast(min)
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
        val prefs = context.saveDataStore.data.first()
        val appSettings = globalPreferences.getSettings()
        val slots = buildMap {
            for (index in 1..MAX_SAVE_SLOTS) prefs[getSaveKey(index)]?.let { if (validateSave(it)) put(index, it) }
        }
        val backups = buildMap {
            for (index in 1..MAX_SAVE_SLOTS) prefs[getBackupKey(index)]?.let { if (validateSave(it)) put(index, it) }
        }
        val bundle = EncryptedBackupBundle(
            appPreferencesJson = globalPreferences.encode(appSettings),
            activeSlot = getActiveSlot(),
            slots = slots,
            backups = backups
        )
        return encrypt(json.encodeToString(bundle))
    }

    suspend fun importSave(inputString: String, slot: Int = -1): Boolean {
        val decrypted = decrypt(inputString)
        val bundle = decrypted.takeIf { it.isNotEmpty() }?.let { runCatching { json.decodeFromString<EncryptedBackupBundle>(it) }.getOrNull() }
        if (bundle != null) {
            return try {
                val importedSettings = globalPreferences.decode(bundle.appPreferencesJson) ?: return false
                context.saveDataStore.edit { prefs ->
                    for (index in 1..MAX_SAVE_SLOTS) {
                        prefs.remove(getSaveKey(index))
                        prefs.remove(getBackupKey(index))
                    }
                    bundle.slots.forEach { (index, raw) -> if (index in 1..MAX_SAVE_SLOTS && validateSave(raw)) prefs[getSaveKey(index)] = raw }
                    bundle.backups.forEach { (index, raw) -> if (index in 1..MAX_SAVE_SLOTS && validateSave(raw)) prefs[getBackupKey(index)] = raw }
                    prefs[activeSlotKey] = bundle.activeSlot.coerceIn(1, MAX_SAVE_SLOTS)
                }
                globalPreferences.saveSettings(importedSettings)
                globalPreferences.markMigrationCompleted()
                true
            } catch (_: Exception) { false }
        }

        val jsonString = if (validateSave(inputString)) inputString else decrypted.takeIf { validateSave(it) } ?: return false
        return try {
            val state = json.decodeFromString<GameState>(jsonString)
            saveGame(state, if (slot in 1..MAX_SAVE_SLOTS) slot else getActiveSlot())
            true
        } catch (_: Exception) { false }
    }

    suspend fun needsSettingsMigration(): Boolean {
        if (globalPreferences.isMigrationCompleted()) return false
        val prefs = context.saveDataStore.data.first()
        return (1..MAX_SAVE_SLOTS).any { index ->
            val raw = prefs[getSaveKey(index)]
            !raw.isNullOrEmpty() && runCatching {
                val state = json.decodeFromString<GameState>(raw)
                state.settings != GameSettings()
            }.getOrDefault(false)
        }
    }

    suspend fun migrateSettingsToGlobal(): Boolean {
        if (globalPreferences.isMigrationCompleted()) return true
        return try {
            val prefs = context.saveDataStore.data.first()
            val source = (1..MAX_SAVE_SLOTS).asSequence().mapNotNull { index ->
                prefs[getSaveKey(index)]?.let { raw -> runCatching { json.decodeFromString<GameState>(raw) }.getOrNull() }
            }.firstOrNull()
            val settings = source?.settings ?: GameSettings()
            globalPreferences.saveSettings(settings)
            context.saveDataStore.edit { p ->
                for (index in 1..MAX_SAVE_SLOTS) {
                    p[getSaveKey(index)]?.let { raw ->
                        runCatching { json.decodeFromString<GameState>(raw) }.getOrNull()?.let { state ->
                            p[getSaveKey(index)] = json.encodeToString(state.copy(settings = GameSettings()))
                        }
                    }
                }
            }
            globalPreferences.markMigrationCompleted()
            true
        } catch (_: Exception) { false }
    }

    suspend fun getAppPreferencesJson(): String = globalPreferences.encode(globalPreferences.getSettings())
    suspend fun applyAppPreferencesJson(raw: String): Boolean {
        val settings = globalPreferences.decode(raw) ?: return false
        globalPreferences.saveSettings(settings)
        return true
    }

    suspend fun getSaveJson(slot: Int): String? {
        if (slot !in 1..MAX_SAVE_SLOTS) return null
        return context.saveDataStore.data.first()[getSaveKey(slot)]
    }

    suspend fun applySaveJson(slot: Int, raw: String): Boolean {
        if (slot !in 1..MAX_SAVE_SLOTS || !validateSave(raw)) return false
        return context.saveDataStore.edit { it[getSaveKey(slot)] = raw }.let { true }
    }

    suspend fun createFreshSlot(slot: Int, difficultyId: String): Boolean {
        if (slot !in 1..MAX_SAVE_SLOTS || !Difficulty.isValid(difficultyId)) return false

        val targetSlot = slot
        return try {
            val prefs = context.saveDataStore.data.first()
            val existing = prefs[getSaveKey(targetSlot)]
            if (!existing.isNullOrEmpty() && validateSave(existing)) {
                return false
            }

            val freshState = GameState(
                difficultyId = difficultyId,
                lastSaveTimestamp = System.currentTimeMillis()
            )
            val freshJson = json.encodeToString(freshState)

            context.saveDataStore.edit { p ->
                p.remove(getBackupKey(targetSlot))
                p[getSaveKey(targetSlot)] = freshJson
                p[activeSlotKey] = targetSlot
            }

            GameLogger.log(
                LogLevel.INFO,
                LoggerCategory.SAVE,
                "CREATE_FRESH_SLOT",
                "Created fresh save in slot $targetSlot with difficulty $difficultyId."
            )
            true
        } catch (e: Exception) {
            GameLogger.log(
                LogLevel.ERROR,
                LoggerCategory.SAVE,
                "CREATE_FRESH_SLOT_ERROR",
                "Failed to create fresh save in slot $targetSlot: ${e.message}"
            )
            false
        }
    }

    suspend fun duplicateSlot(sourceSlot: Int, targetSlot: Int): Boolean {
        val source = sourceSlot.coerceIn(1, MAX_SAVE_SLOTS)
        val target = targetSlot.coerceIn(1, MAX_SAVE_SLOTS)
        if (source == target) return false
        return try {
            val prefs = context.saveDataStore.data.first()
            val sourceJson = prefs[getSaveKey(source)] ?: return false
            if (!validateSave(sourceJson)) return false
            if (!prefs[getSaveKey(target)].isNullOrEmpty()) return false
            context.saveDataStore.edit { p ->
                p[getSaveKey(target)] = sourceJson
                prefs[getBackupKey(source)]?.let { p[getBackupKey(target)] = it }
            }
            GameLogger.log(LogLevel.INFO, LoggerCategory.SAVE, "DUPLICATE_SLOT", "Duplicated slot $source to slot $target.")
            true
        } catch (e: Exception) {
            GameLogger.log(LogLevel.ERROR, LoggerCategory.SAVE, "DUPLICATE_SLOT_ERROR", "Failed to duplicate slot $source to slot $target: ${e.message}")
            false
        }
    }

    suspend fun restoreBackup(slot: Int): Boolean {
        val s = slot.coerceIn(1, MAX_SAVE_SLOTS)
        return try {
            val prefs = context.saveDataStore.data.first()
            val current = prefs[getSaveKey(s)] ?: return false
            val backup = prefs[getBackupKey(s)] ?: return false
            if (!validateSave(current) || !validateSave(backup)) return false
            context.saveDataStore.edit { p ->
                p[getSaveKey(s)] = backup
                p[getBackupKey(s)] = current
            }
            GameLogger.log(LogLevel.WARN, LoggerCategory.SAVE, "RESTORE_BACKUP", "Restored slot $s from its backup.")
            true
        } catch (e: Exception) {
            GameLogger.log(LogLevel.ERROR, LoggerCategory.SAVE, "RESTORE_BACKUP_ERROR", "Failed to restore backup for slot $s: ${e.message}")
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
