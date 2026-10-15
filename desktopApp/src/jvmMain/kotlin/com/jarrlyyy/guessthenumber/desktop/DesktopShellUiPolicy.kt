package com.jarrlyyy.guessthenumber.desktop

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Shared desktop window metrics keep sizing and regression tests in sync. */
internal object DesktopWindowMetrics {
    val INITIAL_WIDTH: Dp = 1100.dp
    val INITIAL_HEIGHT: Dp = 760.dp
    val MIN_WIDTH: Dp = 800.dp
    val MIN_HEIGHT: Dp = 600.dp
    val COMPACT_NAVIGATION_BREAKPOINT: Dp = 960.dp
}

internal fun shouldUseCompactNavigation(width: Dp): Boolean =
    width < DesktopWindowMetrics.COMPACT_NAVIGATION_BREAKPOINT

internal enum class DesktopShellKeyAction {
    NAVIGATE_PLAY,
    NAVIGATE_ABOUT,
    CLEAR_MESSAGE,
    NONE
}

/**
 * Maps shell-level keyboard input only. Enter is deliberately left unconsumed so
 * the focused Compose button or input can perform its normal default action.
 */
internal fun resolveDesktopShellKeyAction(
    key: Key,
    ctrlPressed: Boolean,
    destination: DesktopDestination,
    hasMessage: Boolean
): DesktopShellKeyAction {
    if (ctrlPressed) {
        return when (DesktopDestination.fromShortcut(key, ctrlPressed = true)) {
            DesktopDestination.PLAY -> DesktopShellKeyAction.NAVIGATE_PLAY
            DesktopDestination.ABOUT -> DesktopShellKeyAction.NAVIGATE_ABOUT
            null -> DesktopShellKeyAction.NONE
        }
    }

    if (key != Key.Escape) return DesktopShellKeyAction.NONE
    if (hasMessage) return DesktopShellKeyAction.CLEAR_MESSAGE
    if (destination != DesktopDestination.PLAY) return DesktopShellKeyAction.NAVIGATE_PLAY
    return DesktopShellKeyAction.NONE
}

internal enum class DesktopUpdateStatusKind {
    LOADING,
    ERROR,
    AVAILABLE,
    EMPTY
}

internal fun classifyDesktopUpdateStatus(
    status: String,
    isBusy: Boolean,
    hasUpdate: Boolean
): DesktopUpdateStatusKind = when {
    isBusy -> DesktopUpdateStatusKind.LOADING
    status.startsWith("Update check failed") || status.startsWith("Update failed") ->
        DesktopUpdateStatusKind.ERROR
    hasUpdate -> DesktopUpdateStatusKind.AVAILABLE
    else -> DesktopUpdateStatusKind.EMPTY
}
