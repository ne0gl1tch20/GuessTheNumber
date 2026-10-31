package com.jarrlyyy.guessthenumber.desktop

import java.util.prefs.Preferences

/**
 * Persists the one-time first-launch gate. The HTML game is not mounted until the
 * player has completed profile setup and explicitly continued from Save Slots.
 */
internal object DesktopStartupSetupStore {
    private val preferences = Preferences.userNodeForPackage(DesktopStartupSetupStore::class.java)
        .node("startup-setup")
    private const val setupCompleteKey = "profile_and_save_slot_setup_complete"

    fun isComplete(): Boolean = preferences.getBoolean(setupCompleteKey, false)

    fun markComplete() {
        preferences.putBoolean(setupCompleteKey, true)
        preferences.flush()
    }
}
