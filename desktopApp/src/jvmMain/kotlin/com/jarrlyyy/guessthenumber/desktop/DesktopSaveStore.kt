package com.jarrlyyy.guessthenumber.desktop

import com.jarrlyyy.guessthenumber.shared.domain.GameState
import com.jarrlyyy.guessthenumber.shared.domain.GameStateCodec
import java.util.prefs.Preferences

/**
 * Windows/JVM persistence adapter. The shared codec owns the JSON contract;
 * this adapter owns desktop storage and keeps one last-known-good backup.
 */
internal class DesktopSaveStore(
    private val preferences: Preferences = Preferences.userNodeForPackage(DesktopSaveStore::class.java)
) {
    fun loadOrDefault(): GameState {
        val current = GameStateCodec.decodeOrNull(preferences.get(CURRENT_KEY, null))
        if (current != null) return current

        val backup = GameStateCodec.decodeOrNull(preferences.get(BACKUP_KEY, null))
        if (backup != null) {
            save(backup)
            return backup
        }

        return GameState()
    }

    fun save(state: GameState) {
        val previous = preferences.get(CURRENT_KEY, null)
        if (GameStateCodec.decodeOrNull(previous) != null) {
            preferences.put(BACKUP_KEY, previous)
        }

        val timestamped = state.copy(lastSaveTimestamp = System.currentTimeMillis())
        preferences.put(CURRENT_KEY, GameStateCodec.encode(timestamped))
        runCatching { preferences.flush() }
    }

    private companion object {
        const val CURRENT_KEY = "game_state_json"
        const val BACKUP_KEY = "game_state_backup_json"
    }
}
