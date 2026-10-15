package com.jarrlyyy.guessthenumber.desktop

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DesktopShellUiPolicyTest {
    @Test
    fun compactNavigationIsUsedBelowBreakpoint() {
        assertTrue(shouldUseCompactNavigation(800.dp))
        assertTrue(shouldUseCompactNavigation(959.dp))
    }

    @Test
    fun expandedNavigationStartsAtBreakpoint() {
        assertFalse(shouldUseCompactNavigation(DesktopWindowMetrics.COMPACT_NAVIGATION_BREAKPOINT))
        assertFalse(shouldUseCompactNavigation(1100.dp))
    }

    @Test
    fun windowMetricsKeepKnownMinimumAndInitialSizes() {
        assertEquals(800.dp, DesktopWindowMetrics.MIN_WIDTH)
        assertEquals(600.dp, DesktopWindowMetrics.MIN_HEIGHT)
        assertEquals(1100.dp, DesktopWindowMetrics.INITIAL_WIDTH)
        assertEquals(760.dp, DesktopWindowMetrics.INITIAL_HEIGHT)
    }

    @Test
    fun controlShortcutsMapToKnownDestinations() {
        assertEquals(
            DesktopShellKeyAction.NAVIGATE_PLAY,
            resolveDesktopShellKeyAction(Key.One, true, DesktopDestination.ABOUT, false)
        )
        assertEquals(
            DesktopShellKeyAction.NAVIGATE_ABOUT,
            resolveDesktopShellKeyAction(Key.Two, true, DesktopDestination.PLAY, false)
        )
    }

    @Test
    fun unsupportedControlShortcutDoesNothing() {
        assertEquals(
            DesktopShellKeyAction.NONE,
            resolveDesktopShellKeyAction(Key.Three, true, DesktopDestination.PLAY, false)
        )
    }

    @Test
    fun escapeClearsMessageBeforeNavigating() {
        assertEquals(
            DesktopShellKeyAction.CLEAR_MESSAGE,
            resolveDesktopShellKeyAction(Key.Escape, false, DesktopDestination.ABOUT, true)
        )
    }

    @Test
    fun escapeReturnsFromAboutToPlay() {
        assertEquals(
            DesktopShellKeyAction.NAVIGATE_PLAY,
            resolveDesktopShellKeyAction(Key.Escape, false, DesktopDestination.ABOUT, false)
        )
    }

    @Test
    fun escapeOnPlayWithoutMessageDoesNothing() {
        assertEquals(
            DesktopShellKeyAction.NONE,
            resolveDesktopShellKeyAction(Key.Escape, false, DesktopDestination.PLAY, false)
        )
    }

    @Test
    fun enterIsLeftForFocusedControls() {
        assertEquals(
            DesktopShellKeyAction.NONE,
            resolveDesktopShellKeyAction(Key.Enter, false, DesktopDestination.PLAY, false)
        )
    }

    @Test
    fun updateStatusDistinguishesLoadingErrorAvailableAndEmpty() {
        assertEquals(
            DesktopUpdateStatusKind.LOADING,
            classifyDesktopUpdateStatus("Checking for updates…", true, false)
        )
        assertEquals(
            DesktopUpdateStatusKind.ERROR,
            classifyDesktopUpdateStatus("Update check failed: offline", false, false)
        )
        assertEquals(
            DesktopUpdateStatusKind.ERROR,
            classifyDesktopUpdateStatus("Update failed: installer error", false, true)
        )
        assertEquals(
            DesktopUpdateStatusKind.AVAILABLE,
            classifyDesktopUpdateStatus("Version 1.2 is ready", false, true)
        )
        assertEquals(
            DesktopUpdateStatusKind.EMPTY,
            classifyDesktopUpdateStatus("You're using the latest build", false, false)
        )
    }

    @Test
    fun busyStateTakesPrecedenceOverOldErrorText() {
        assertEquals(
            DesktopUpdateStatusKind.LOADING,
            classifyDesktopUpdateStatus("Update failed: previous attempt", true, false)
        )
    }
}
