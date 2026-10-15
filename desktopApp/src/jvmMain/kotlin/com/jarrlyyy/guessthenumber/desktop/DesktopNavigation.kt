package com.jarrlyyy.guessthenumber.desktop

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Windows-owned navigation contract. Destinations are added here as each
 * parity stage implements a real screen; Android navigation stays platform-owned.
 */
enum class DesktopDestination(val label: String) {
    PLAY("Play"),
    ABOUT("About")
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
}
