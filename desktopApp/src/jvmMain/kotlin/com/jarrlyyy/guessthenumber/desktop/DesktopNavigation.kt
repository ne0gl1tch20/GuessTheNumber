package com.jarrlyyy.guessthenumber.desktop

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import java.util.prefs.Preferences

/**
 * Windows-only navigation placement. Stored separately from GameSettings so Android
 * save files and settings remain unchanged.
 */
enum class DesktopNavigationPosition(val label: String) {
    LEFT("Left"),
    RIGHT("Right"),
    TOP("Top"),
    BOTTOM("Bottom");

    companion object {
        val DEFAULT_POSITION: DesktopNavigationPosition = LEFT

        private val preferences: Preferences =
            Preferences.userNodeForPackage(DesktopNavigationPosition::class.java).node("navigation")

        fun load(): DesktopNavigationPosition =
            runCatching { valueOf(preferences.get("position", DEFAULT_POSITION.name)) }.getOrDefault(LEFT)

        fun save(position: DesktopNavigationPosition) {
            runCatching {
                preferences.put("position", position.name)
                preferences.flush()
            }
        }
    }
}

/**
 * Windows-owned navigation contract. Quick-access destinations may expose
 * partial screens while parity work continues; Android navigation stays platform-owned.
 */
enum class DesktopDestination(val label: String, val shortcutKey: Key?) {
    PLAY("Play", Key.One),
    UPGRADES("Upgrades", Key.Two),
    SHOP("Shop", Key.Three),
    MORE("More", Key.Four),
    ABOUT("About", Key.Five),
    PRESTIGE("Prestige", null),
    ULTRA("Ultra", null),
    TALENT("Talent Web", null),
    STATS("Statistics", null),
    SETTINGS("Settings & Save Management", null),
    ACHIEVEMENTS("Achievements", null),
    WORLD_MAP("World Map", null),
    MUTATORS("Mutators", null),
    SAVE_SLOTS("Save Slots", null),
    RELICS("Relics", null),
    HOME_BASE("Home Base", null),
    CODEX("Codex", null),
    ENDGAME("Endgame", null),
    LUCKY_STAKE("Lucky Stake", null),
    DEVELOPER_SETTINGS("Developer Settings", null),
    DEVELOPER_CONSOLE("Developer Console", null),
    PROCESS_INSPECTOR("Process Inspector", null),
    MUSIC_PLAYER("Music Player", null),
    ARCADE("Arcade", null),
    ACTIVITY_HUB("Activity Hub", null),
    LUA_TESTING_HUB("Lua Testing Hub", null),
    CHANGELOG("Changelog", null),
    LIVE_OPS("Live Ops", null);

    companion object {
        /** Desktop-only quick-access order; Android navigation is unchanged. */
        val MAIN_NAVIGATION_ORDER: List<DesktopDestination> = listOf(
            PLAY,
            UPGRADES,
            SHOP,
            PRESTIGE,
            ULTRA,
            SETTINGS,
            ARCADE,
            MUSIC_PLAYER,
            SAVE_SLOTS,
            STATS,
            WORLD_MAP,
            CHANGELOG,
            MORE
        )

        fun fromShortcut(key: Key, ctrlPressed: Boolean): DesktopDestination? {
            if (!ctrlPressed) return null
            return entries.firstOrNull { it.shortcutKey == key }
        }
    }
}

data class DesktopShellState(
    val destination: DesktopDestination = DesktopDestination.PLAY,
    val message: String? = null
)

/** Small, UI-independent shell controller so navigation transitions are testable. */
class DesktopShellController(
    initialState: DesktopShellState = DesktopShellState()
) {
    var state: DesktopShellState by mutableStateOf(initialState)
        private set

    fun navigate(destination: DesktopDestination) {
        state = state.copy(destination = destination, message = null)
    }

    fun showMessage(message: String) {
        state = state.copy(message = message.trim().takeIf(String::isNotEmpty))
    }

    fun clearMessage() {
        state = state.copy(message = null)
    }

    /** Handles only shell shortcuts and Escape; normal Enter activation stays with focused controls. */
    fun handleKey(key: Key, ctrlPressed: Boolean): Boolean {
        val action = resolveDesktopShellKeyAction(
            key = key,
            ctrlPressed = ctrlPressed,
            destination = state.destination,
            hasMessage = state.message != null
        )
        when (action) {
            DesktopShellKeyAction.NAVIGATE_PLAY -> navigate(DesktopDestination.PLAY)
            DesktopShellKeyAction.NAVIGATE_UPGRADES -> navigate(DesktopDestination.UPGRADES)
            DesktopShellKeyAction.NAVIGATE_SHOP -> navigate(DesktopDestination.SHOP)
            DesktopShellKeyAction.NAVIGATE_MORE -> navigate(DesktopDestination.MORE)
            DesktopShellKeyAction.NAVIGATE_ABOUT -> navigate(DesktopDestination.ABOUT)
            DesktopShellKeyAction.CLEAR_MESSAGE -> clearMessage()
            DesktopShellKeyAction.NONE -> return false
        }
        return true
    }
}
