package com.jarrlyyy.guessthenumber.desktop

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key

/**
 * Windows-owned navigation contract. Destinations are added here as each
 * parity stage implements a real screen; Android navigation stays platform-owned.
 */
enum class DesktopDestination(val label: String, val shortcutKey: Key) {
    PLAY("Play", Key.One),
    UPGRADES("Upgrades", Key.Two),
    SHOP("Shop", Key.Three),
    MORE("More", Key.Four),
    ABOUT("About", Key.Five);

    companion object {
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
