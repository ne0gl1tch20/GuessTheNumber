package com.jarrlyyy.guessthenumber.desktop

import androidx.compose.ui.input.key.Key
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DesktopNavigationTest {
    @Test
    fun windowsNavigationCanUseEveryEdgeAndDefaultsToLeft() {
        assertEquals(DesktopNavigationPosition.LEFT, DesktopNavigationPosition.DEFAULT_POSITION)
        assertEquals(listOf("Left", "Right", "Top", "Bottom"), DesktopNavigationPosition.entries.map { it.label })
    }

    @Test
    fun desktopMainNavigationHasTheApprovedQuickAccessOrder() {
        assertEquals(
            listOf(
                DesktopDestination.PLAY,
                DesktopDestination.UPGRADES,
                DesktopDestination.SHOP,
                DesktopDestination.PRESTIGE,
                DesktopDestination.ULTRA,
                DesktopDestination.SETTINGS,
                DesktopDestination.ARCADE,
                DesktopDestination.MUSIC_PLAYER,
                DesktopDestination.SAVE_SLOTS,
                DesktopDestination.STATS,
                DesktopDestination.WORLD_MAP,
                DesktopDestination.CHANGELOG,
                DesktopDestination.MORE
            ),
            DesktopDestination.MAIN_NAVIGATION_ORDER
        )
    }

    @Test
    fun startsOnPlay() {
        val controller = DesktopShellController()

        assertEquals(DesktopDestination.PLAY, controller.state.destination)
        assertNull(controller.state.message)
    }

    @Test
    fun navigationChangesDestinationAndClearsOldMessage() {
        val controller = DesktopShellController()
        controller.showMessage("Could not load a screen")

        controller.navigate(DesktopDestination.ABOUT)

        assertEquals(DesktopDestination.ABOUT, controller.state.destination)
        assertNull(controller.state.message)
    }

    @Test
    fun blankMessagesAreClearedAndMessagesAreTrimmed() {
        val controller = DesktopShellController()

        controller.showMessage("  Saved locally  ")
        assertEquals("Saved locally", controller.state.message)

        controller.showMessage("  ")
        assertNull(controller.state.message)
    }

    @Test
    fun ctrlNumberShortcutsMapToDestinations() {
        assertEquals(DesktopDestination.PLAY, DesktopDestination.fromShortcut(Key.One, ctrlPressed = true))
        assertEquals(DesktopDestination.UPGRADES, DesktopDestination.fromShortcut(Key.Two, ctrlPressed = true))
        assertEquals(DesktopDestination.SHOP, DesktopDestination.fromShortcut(Key.Three, ctrlPressed = true))
        assertEquals(DesktopDestination.MORE, DesktopDestination.fromShortcut(Key.Four, ctrlPressed = true))
        assertEquals(DesktopDestination.ABOUT, DesktopDestination.fromShortcut(Key.Five, ctrlPressed = true))
        assertNull(DesktopDestination.PRESTIGE.shortcutKey)
        assertNull(DesktopDestination.STATS.shortcutKey)
    }

    @Test
    fun settingsDestinationIsReachableAndReturnsToPlay() {
        val controller = DesktopShellController()
        controller.navigate(DesktopDestination.SETTINGS)

        assertEquals(DesktopDestination.SETTINGS, controller.state.destination)
        assertTrue(controller.handleKey(Key.Escape, ctrlPressed = false))
        assertEquals(DesktopDestination.PLAY, controller.state.destination)
    }

    @Test
    fun escapeClearsMessageBeforeReturningToPlay() {
        val controller = DesktopShellController()
        controller.navigate(DesktopDestination.ABOUT)
        controller.showMessage("A recoverable message")

        assertTrue(controller.handleKey(Key.Escape, ctrlPressed = false))
        assertEquals(DesktopDestination.ABOUT, controller.state.destination)
        assertNull(controller.state.message)

        assertTrue(controller.handleKey(Key.Escape, ctrlPressed = false))
        assertEquals(DesktopDestination.PLAY, controller.state.destination)
    }

    @Test
    fun escapeReturnsToPlayAndKeepsNavigationUsable() {
        val controller = DesktopShellController()
        controller.navigate(DesktopDestination.ABOUT)

        assertTrue(controller.handleKey(Key.Escape, ctrlPressed = false))
        assertEquals(DesktopDestination.PLAY, controller.state.destination)
        assertNull(controller.state.message)

        assertTrue(controller.handleKey(Key.Five, ctrlPressed = true))
        assertEquals(DesktopDestination.ABOUT, controller.state.destination)
    }

    @Test
    fun statisticsIsReachableAsAMoreDestinationAndEscapeReturnsToPlay() {
        val controller = DesktopShellController()
        controller.navigate(DesktopDestination.STATS)

        assertEquals(DesktopDestination.STATS, controller.state.destination)
        assertTrue(controller.handleKey(Key.Escape, ctrlPressed = false))
        assertEquals(DesktopDestination.PLAY, controller.state.destination)
    }

    @Test
    fun enterIsNotConsumedByShell() {
        val controller = DesktopShellController()
        assertFalse(controller.handleKey(Key.Enter, ctrlPressed = false))
        assertEquals(DesktopDestination.PLAY, controller.state.destination)
    }

    @Test
    fun shortcutsRequireControlAndIgnoreUnknownKeys() {
        assertNull(DesktopDestination.fromShortcut(Key.One, ctrlPressed = false))
        assertNull(DesktopDestination.fromShortcut(Key.Six, ctrlPressed = true))
    }
}
